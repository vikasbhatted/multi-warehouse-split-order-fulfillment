package com.fashionhub.fulfillment.service;

import com.fashionhub.fulfillment.camunda.CamundaEngineClient;
import com.fashionhub.fulfillment.camunda.ProcessStarter;
import com.fashionhub.fulfillment.repository.CustomerOrderRepository;
import com.fashionhub.fulfillment.config.AppProperties;
import com.fashionhub.fulfillment.domain.OrderStatus;
import com.fashionhub.fulfillment.dto.OrderViews.OrderDetail;
import com.fashionhub.fulfillment.dto.OrderViews.RefundView;
import com.fashionhub.fulfillment.dto.OrderViews.ShipmentView;
import com.fashionhub.fulfillment.dto.TrackerViews.ElementView;
import com.fashionhub.fulfillment.dto.TrackerViews.EngineView;
import com.fashionhub.fulfillment.dto.TrackerViews.InstanceView;
import com.fashionhub.fulfillment.dto.TrackerViews.Milestone;
import com.fashionhub.fulfillment.dto.TrackerViews.TrackerResponse;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Builds the live status view of an order: business milestones come from our database (always
 * available) and the technical execution path comes from the engine (best effort - search data is
 * eventually consistent, so it can lag by a second).
 */
@Service
public class TrackerService {
    private static final Logger log = LoggerFactory.getLogger(TrackerService.class);

    private final OrderService orderService;
    private final CamundaEngineClient engine;
    private final AppProperties.CamundaApi api;
    private final CustomerOrderRepository orders;

    public TrackerService(OrderService orderService, CamundaEngineClient engine, AppProperties.CamundaApi api,
                          CustomerOrderRepository orders) {
        this.orderService = orderService;
        this.engine = engine;
        this.api = api;
        this.orders = orders;
    }

    public TrackerResponse track(String orderId) {
        OrderDetail order = orderService.getOrder(orderId);
        if (order.processInstanceKey() == null && !order.status().equals("CREATED")) {
            // repair orders whose key was lost (see CustomerOrder @DynamicUpdate) by asking the engine
            try {
                String key = engine.findMainInstanceKey(ProcessStarter.MAIN_PROCESS_ID, orderId);
                if (key != null) {
                    orders.setProcessInstanceKey(orderId, key);
                    order = orderService.getOrder(orderId);
                }
            } catch (RuntimeException e) {
                log.debug("Could not recover process instance key for {}: {}", orderId, e.getMessage());
            }
        }
        List<Milestone> milestones = milestones(order);
        EngineView engineView = engineView(order.processInstanceKey());
        boolean finished = order.status().equals(OrderStatus.COMPLETED.name())
                || order.status().equals(OrderStatus.PARTIALLY_REFUNDED.name())
                || order.status().equals(OrderStatus.REJECTED.name());
        String operate = order.processInstanceKey() == null ? api.operateUrl()
                : api.operateUrl().replaceAll("/+$", "") + "/processes/" + order.processInstanceKey();
        return new TrackerResponse(order, milestones, engineView, operate, finished);
    }

    private List<Milestone> milestones(OrderDetail o) {
        List<Milestone> m = new ArrayList<>();
        boolean rejected = o.status().equals("REJECTED");
        m.add(new Milestone("placed", "Order placed", "DONE", o.id()));
        if (rejected) {
            m.add(new Milestone("validated", "Validation & stock allocation", "FAILED", o.rejectionReason()));
            return m;
        }
        boolean validated = !o.status().equals("CREATED");
        long warehouses = o.items().stream().map(i -> i.warehouseId()).filter(w -> w != null).distinct().count();
        m.add(new Milestone("validated", "Validation & stock allocation", validated ? "DONE" : "ACTIVE",
                validated ? (warehouses > 1 ? "Split into " + warehouses + " parcels" : "Single parcel") : null));
        for (ShipmentView s : o.shipments()) {
            String state = s.status().equals("SHIPPED") ? "DONE" : "ACTIVE";
            String detail = switch (s.status()) {
                case "SHIPPED" -> "Tracking " + s.trackingNumber();
                case "DAMAGE_HANDLING" -> "Damaged item - substitution / refund in progress";
                case "PICKED" -> "Picked - generating carrier label";
                default -> "Waiting for picker";
            };
            m.add(new Milestone("shipment-" + s.warehouseId(), "Parcel from " + s.warehouseName(), state, detail));
        }
        if (o.shipments().isEmpty() && validated) {
            m.add(new Milestone("shipments", "Warehouse sub-workflows", "ACTIVE", "Dispatching to warehouses"));
        }
        for (RefundView r : o.refunds()) {
            m.add(new Milestone("refund-" + r.sku(), "Refund " + r.sku(), "DONE",
                    "₹" + r.amount() + " (" + r.type() + ")"));
        }
        boolean done = o.status().equals("COMPLETED") || o.status().equals("PARTIALLY_REFUNDED");
        m.add(new Milestone("completed", "Order completed", done ? "DONE" : "PENDING",
                done ? "Customer notified per parcel" : null));
        return m;
    }

    private EngineView engineView(String rootKey) {
        if (rootKey == null) {
            return new EngineView(false, "Process instance not started yet", List.of());
        }
        try {
            List<InstanceView> instances = new ArrayList<>();
            collect(rootKey, null, 0, instances);
            return new EngineView(true, null, instances);
        } catch (RuntimeException e) {
            log.debug("Engine view unavailable: {}", e.getMessage());
            return new EngineView(false, "Engine details unavailable: " + e.getMessage(), List.of());
        }
    }

    private void collect(String key, String parentKey, int depth, List<InstanceView> out) {
        if (depth > 3) {
            return;
        }
        JsonNode pi = engine.getProcessInstance(key);
        List<ElementView> elements = new ArrayList<>();
        for (JsonNode e : engine.searchElementInstances(key).path("items")) {
            String type = e.path("type").asText();
            if (type.equals("PROCESS") || type.equals("SEQUENCE_FLOW")) {
                continue;
            }
            elements.add(new ElementView(e.path("elementId").asText(), e.path("elementName").asText(null), type,
                    e.path("state").asText(), e.path("hasIncident").asBoolean(false),
                    e.path("startDate").asText(null), e.path("endDate").asText(null)));
        }
        out.add(new InstanceView(key, pi.path("processDefinitionId").asText(), parentKey,
                pi.path("state").asText(), pi.path("hasIncident").asBoolean(false), elements));
        for (JsonNode child : engine.searchChildInstances(key).path("items")) {
            collect(child.path("processInstanceKey").asText(), key, depth + 1, out);
        }
    }
}
