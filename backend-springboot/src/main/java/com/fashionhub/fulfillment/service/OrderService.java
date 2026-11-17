package com.fashionhub.fulfillment.service;

import com.fashionhub.fulfillment.common.BadRequestException;
import com.fashionhub.fulfillment.common.EngineUnavailableException;
import com.fashionhub.fulfillment.common.ResourceNotFoundException;
import com.fashionhub.fulfillment.config.AppProperties;
import com.fashionhub.fulfillment.domain.CustomerOrder;
import com.fashionhub.fulfillment.domain.OrderItem;
import com.fashionhub.fulfillment.domain.OrderStatus;
import com.fashionhub.fulfillment.domain.Product;
import com.fashionhub.fulfillment.dto.CreateOrderRequest;
import com.fashionhub.fulfillment.dto.OrderViews.OrderDetail;
import com.fashionhub.fulfillment.dto.OrderViews.OrderSummary;
import com.fashionhub.fulfillment.camunda.ProcessStarter;
import com.fashionhub.fulfillment.repository.CustomerOrderRepository;
import com.fashionhub.fulfillment.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final CustomerOrderRepository orders;
    private final ProductRepository products;
    private final OrderMapper mapper;
    private final ProcessStarter processStarter;
    private final AppProperties.Fulfillment fulfillmentProps;
    private final TransactionTemplate tx;

    public OrderService(CustomerOrderRepository orders, ProductRepository products, OrderMapper mapper,
                        ProcessStarter processStarter, AppProperties.Fulfillment fulfillmentProps,
                        PlatformTransactionManager txManager) {
        this.orders = orders;
        this.products = products;
        this.mapper = mapper;
        this.processStarter = processStarter;
        this.fulfillmentProps = fulfillmentProps;
        this.tx = new TransactionTemplate(txManager);
    }

    /** Persists the order, then starts the Camunda process instance with the order as variables. */
    public OrderSummary createOrder(CreateOrderRequest req) {
        Map<String, Integer> qtyBySku = new LinkedHashMap<>();
        for (CreateOrderRequest.ItemRequest it : req.items()) {
            qtyBySku.merge(it.sku().trim(), it.quantity(), Integer::sum);
        }
        qtyBySku.forEach((sku, q) -> {
            if (q > 5) {
                throw new BadRequestException("Maximum 5 units per SKU (" + sku + ")");
            }
        });

        CustomerOrder saved = tx.execute(status -> {
            CustomerOrder o = new CustomerOrder();
            o.setId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            o.setCustomerName(req.customerName().trim());
            o.setCustomerEmail(req.customerEmail().trim());
            o.setShippingLine1(req.shippingAddress().line1().trim());
            o.setShippingCity(req.shippingAddress().city().trim());
            o.setShippingPostalCode(req.shippingAddress().postalCode().trim());
            o.setShippingCountry(req.shippingAddress().country().trim().toUpperCase());
            o.setSimulateRefundFailure(req.simulateRefundFailure());
            BigDecimal total = BigDecimal.ZERO;
            for (Map.Entry<String, Integer> e : qtyBySku.entrySet()) {
                Product p = products.findById(e.getKey())
                        .orElseThrow(() -> new BadRequestException("Unknown SKU: " + e.getKey()));
                OrderItem item = new OrderItem();
                item.setSku(p.getSku());
                item.setName(p.getName());
                item.setQuantity(e.getValue());
                item.setUnitPrice(p.getPrice());
                o.addItem(item);
                total = total.add(item.lineTotal());
            }
            o.setTotalAmount(total);
            return orders.save(o);
        });

        // ---- process variables = the data contract of the main BPMN process
        List<Map<String, Object>> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> e : qtyBySku.entrySet()) {
            Product p = products.findById(e.getKey()).orElseThrow();
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("sku", p.getSku());
            line.put("name", p.getName());
            line.put("quantity", e.getValue());
            line.put("unitPrice", p.getPrice());
            lines.add(line);
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("orderId", saved.getId());
        variables.put("customer", Map.of("name", saved.getCustomerName(), "email", saved.getCustomerEmail()));
        variables.put("shippingAddress", Map.of(
                "line1", saved.getShippingLine1(),
                "city", saved.getShippingCity(),
                "postalCode", saved.getShippingPostalCode(),
                "country", saved.getShippingCountry()));
        variables.put("items", lines);
        variables.put("simulateRefundFailure", saved.isSimulateRefundFailure());
        variables.put("substitutionTimeout", fulfillmentProps.substitutionTimeout());

        String processInstanceKey;
        try {
            processInstanceKey = processStarter.startFulfillment(variables);
        } catch (RuntimeException ex) {
            log.error("Could not start process for {}: {}", saved.getId(), ex.getMessage());
            orders.deleteById(saved.getId());
            throw new EngineUnavailableException(
                    "Camunda engine is not reachable - is c8run started and are the BPMN models deployed?", ex);
        }

        // The validate-and-allocate worker may already be updating this order -> targeted single-column update.
        orders.setProcessInstanceKey(saved.getId(), processInstanceKey);
        log.info("Order {} started as process instance {}", saved.getId(), processInstanceKey);
        return tx.execute(status -> mapper.toSummary(orders.findById(saved.getId()).orElseThrow()));
    }

    @Transactional(readOnly = true)
    public List<OrderSummary> list() {
        return orders.findAllByOrderByCreatedAtDesc().stream().map(mapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public OrderDetail getOrder(String id) {
        CustomerOrder o = orders.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        return mapper.toDetail(o);
    }

    /** Recorded in its own transaction because the allocation transaction has been rolled back. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRejected(String orderId, String reason) {
        orders.findById(orderId).ifPresent(o -> {
            o.setStatus(OrderStatus.REJECTED);
            o.setRejectionReason(reason);
            o.touch();
        });
    }
}
