package com.fashionhub.fulfillment.service;

import com.fashionhub.fulfillment.camunda.CamundaEngineClient;
import com.fashionhub.fulfillment.common.BadRequestException;
import com.fashionhub.fulfillment.common.ConflictException;
import com.fashionhub.fulfillment.common.ResourceNotFoundException;
import com.fashionhub.fulfillment.domain.CustomerOrder;
import com.fashionhub.fulfillment.domain.ItemStatus;
import com.fashionhub.fulfillment.domain.OrderItem;
import com.fashionhub.fulfillment.domain.Product;
import com.fashionhub.fulfillment.domain.Shipment;
import com.fashionhub.fulfillment.domain.ShipmentStatus;
import com.fashionhub.fulfillment.dto.DamagedItem;
import com.fashionhub.fulfillment.dto.TaskViews.TaskView;
import com.fashionhub.fulfillment.repository.CustomerOrderRepository;
import com.fashionhub.fulfillment.repository.ProductRepository;
import com.fashionhub.fulfillment.repository.ShipmentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom Tasklist backend: lists / claims / completes Camunda user tasks through the
 * Orchestration Cluster REST API and enriches every task with business context from the database.
 */
@Service
public class TaskService {

    public static final String UT_PICK = "UT_Pick";
    public static final String UT_APPROVE = "UT_ApproveSubstitution";
    public static final String UT_ADDRESS = "UT_CorrectAddress";
    public static final String UT_MANUAL_REFUND = "UT_ManualRefund";

    private final CamundaEngineClient engine;
    private final CustomerOrderRepository orders;
    private final ShipmentRepository shipments;
    private final ProductRepository products;
    private final ObjectMapper mapper;

    public TaskService(CamundaEngineClient engine, CustomerOrderRepository orders, ShipmentRepository shipments,
                       ProductRepository products, ObjectMapper mapper) {
        this.engine = engine;
        this.orders = orders;
        this.shipments = shipments;
        this.products = products;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------------ queries
    @Transactional(readOnly = true)
    public List<TaskView> list(String group, String assignee, boolean unassignedOnly) {
        JsonNode result = engine.searchUserTasks("CREATED");
        List<TaskView> tasks = new ArrayList<>();
        for (JsonNode n : result.path("items")) {
            TaskView t = toView(n, false);
            if (group != null && !group.isBlank() && !t.candidateGroups().contains(group)) {
                continue;
            }
            if (assignee != null && !assignee.isBlank() && !assignee.equals(t.assignee())) {
                continue;
            }
            if (unassignedOnly && t.assignee() != null) {
                continue;
            }
            tasks.add(toView(n, true));
        }
        return tasks;
    }

    @Transactional(readOnly = true)
    public TaskView get(String key) {
        return toView(engine.getUserTask(key), true);
    }

    // ------------------------------------------------------------------ commands
    public void claim(String key, String assignee) {
        JsonNode task = engine.getUserTask(key);
        String current = text(task, "assignee");
        if (current != null && !current.equals(assignee)) {
            throw new ConflictException("Task is already assigned to " + current);
        }
        if (current == null) {
            engine.assign(key, assignee);
        }
    }

    public void unclaim(String key, String assignee) {
        JsonNode task = engine.getUserTask(key);
        String current = text(task, "assignee");
        if (current != null && !current.equals(assignee)) {
            throw new ConflictException("Task is assigned to " + current);
        }
        if (current != null) {
            engine.unassign(key);
        }
    }

    @Transactional
    public void complete(String key, String assignee, Map<String, Object> input) {
        JsonNode task = engine.getUserTask(key);
        if (!"CREATED".equals(text(task, "state"))) {
            throw new ConflictException("Task is not active (state " + text(task, "state") + ")");
        }
        String current = text(task, "assignee");
        if (current != null && !current.equals(assignee)) {
            throw new ConflictException("Task is assigned to " + current);
        }
        if (current == null) {
            engine.assign(key, assignee);
        }
        String elementId = text(task, "elementId");
        String instanceKey = text(task, "processInstanceKey");
        Map<String, Object> in = input == null ? Map.of() : input;
        Resolved ctx = resolve(instanceKey);

        Map<String, Object> variables = switch (elementId == null ? "" : elementId) {
            case UT_PICK -> completePick(ctx, in);
            case UT_APPROVE -> Map.of("substitutionApproved", Boolean.TRUE.equals(in.get("substitutionApproved")));
            case UT_ADDRESS -> completeAddress(ctx, in);
            case UT_MANUAL_REFUND -> {
                Object note = in.get("manualRefundNote");
                if (note == null || note.toString().isBlank()) {
                    throw new BadRequestException("A refund note / gateway reference is required");
                }
                yield Map.of("manualRefundNote", note.toString().trim());
            }
            default -> throw new BadRequestException("Unsupported task element: " + elementId);
        };
        engine.complete(key, variables);
    }

    // ------------------------------------------------------------------ completion helpers
    private Map<String, Object> completePick(Resolved ctx, Map<String, Object> in) {
        if (ctx.shipment == null) {
            throw new ResourceNotFoundException("Shipment for this task is not registered yet");
        }
        List<DamagedItem> damaged = new ArrayList<>();
        Object raw = in.get("damagedItems");
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                DamagedItem d = mapper.convertValue(o, DamagedItem.class);
                if (d.sku() == null || d.sku().isBlank() || d.reason() == null || d.reason().isBlank()) {
                    throw new BadRequestException("Every damaged item needs a SKU and a reason");
                }
                damaged.add(d);
            }
        }
        Set<String> seen = new HashSet<>();
        for (DamagedItem d : damaged) {
            if (!seen.add(d.sku())) {
                throw new BadRequestException("Duplicate damaged SKU " + d.sku());
            }
            boolean inParcel = ctx.order.getItems().stream().anyMatch(
                    i -> i.getSku().equals(d.sku()) && ctx.shipment.getWarehouseId().equals(i.getWarehouseId()));
            if (!inParcel) {
                throw new BadRequestException("SKU " + d.sku() + " is not part of this parcel");
            }
        }
        // persist picking outcome BEFORE the engine continues (find-substitutes reads it)
        for (OrderItem it : ctx.order.getItems()) {
            if (!ctx.shipment.getWarehouseId().equals(it.getWarehouseId())) {
                continue;
            }
            Optional<DamagedItem> hit = damaged.stream().filter(d -> d.sku().equals(it.getSku())).findFirst();
            if (hit.isPresent()) {
                it.setStatus(ItemStatus.DAMAGED);
                it.setDamageReason(hit.get().reason());
            } else if (it.getStatus() == ItemStatus.ALLOCATED) {
                it.setStatus(ItemStatus.PICKED);
            }
        }
        ctx.shipment.setStatus(damaged.isEmpty() ? ShipmentStatus.PICKED : ShipmentStatus.DAMAGE_HANDLING);
        ctx.order.touch();
        return Map.of("damagedItems", damaged);
    }

    private Map<String, Object> completeAddress(Resolved ctx, Map<String, Object> in) {
        Object raw = in.get("shippingAddress");
        if (!(raw instanceof Map<?, ?> m)) {
            throw new BadRequestException("shippingAddress is required");
        }
        String line1 = str(m.get("line1"));
        String city = str(m.get("city"));
        String postal = str(m.get("postalCode"));
        String country = str(m.get("country")).toUpperCase();
        if (line1.isBlank() || city.isBlank() || postal.isBlank() || country.length() != 2) {
            throw new BadRequestException("Complete address (line1, city, postalCode, 2-letter country) is required");
        }
        if (postal.equals("00000")) {
            throw new BadRequestException("Postal code 00000 is not deliverable");
        }
        if (ctx.order != null) {
            ctx.order.setShippingLine1(line1);
            ctx.order.setShippingCity(city);
            ctx.order.setShippingPostalCode(postal);
            ctx.order.setShippingCountry(country);
            ctx.order.touch();
        }
        Map<String, Object> addr = new LinkedHashMap<>();
        addr.put("line1", line1);
        addr.put("city", city);
        addr.put("postalCode", postal);
        addr.put("country", country);
        return Map.of("shippingAddress", addr);
    }

    // ------------------------------------------------------------------ mapping + context
    private TaskView toView(JsonNode n, boolean withContext) {
        List<String> groups = new ArrayList<>();
        n.path("candidateGroups").forEach(g -> groups.add(g.asText()));
        String instanceKey = text(n, "processInstanceKey");
        Map<String, Object> context = withContext ? buildContext(instanceKey, text(n, "elementId")) : Map.of();
        return new TaskView(text(n, "userTaskKey"), text(n, "name"), text(n, "elementId"), text(n, "state"),
                text(n, "assignee"), groups, instanceKey, text(n, "processDefinitionId"),
                text(n, "creationDate"), context);
    }

    private record Resolved(CustomerOrder order, Shipment shipment) {
    }

    /** Walks up the call-activity hierarchy (label process -> warehouse process -> main process). */
    private Resolved resolve(String instanceKey) {
        String current = instanceKey;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            Optional<Shipment> s = shipments.findByProcessInstanceKey(current);
            if (s.isPresent()) {
                return new Resolved(orders.findById(s.get().getOrderId()).orElse(null), s.get());
            }
            Optional<CustomerOrder> o = orders.findByProcessInstanceKey(current);
            if (o.isPresent()) {
                return new Resolved(o.get(), null);
            }
            try {
                current = text(engine.getProcessInstance(current), "parentProcessInstanceKey");
            } catch (RuntimeException e) {
                return new Resolved(null, null);
            }
        }
        return new Resolved(null, null);
    }

    private Map<String, Object> buildContext(String instanceKey, String elementId) {
        Resolved r = resolve(instanceKey);
        Map<String, Object> ctx = new LinkedHashMap<>();
        if (r.order == null) {
            return ctx;
        }
        CustomerOrder o = r.order;
        ctx.put("orderId", o.getId());
        ctx.put("customerName", o.getCustomerName());
        ctx.put("customerEmail", o.getCustomerEmail());
        Map<String, Object> addr = new LinkedHashMap<>();
        addr.put("line1", o.getShippingLine1());
        addr.put("city", o.getShippingCity());
        addr.put("postalCode", o.getShippingPostalCode());
        addr.put("country", o.getShippingCountry());
        ctx.put("shippingAddress", addr);
        ctx.put("simulateRefundFailure", o.isSimulateRefundFailure());
        if (r.shipment != null) {
            ctx.put("warehouseId", r.shipment.getWarehouseId());
            ctx.put("warehouseName", r.shipment.getWarehouseName());
            ctx.put("parcelWeightKg", r.shipment.getParcelWeightKg());
            List<Map<String, Object>> items = new ArrayList<>();
            for (OrderItem it : o.getItems()) {
                if (!r.shipment.getWarehouseId().equals(it.getWarehouseId())) {
                    continue;
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("sku", it.getSku());
                m.put("name", it.getName());
                m.put("quantity", it.getQuantity());
                m.put("unitPrice", it.getUnitPrice());
                m.put("status", it.getStatus().name());
                m.put("damageReason", it.getDamageReason());
                m.put("proposedSubstituteSku", it.getProposedSubstituteSku());
                if (it.getProposedSubstituteSku() != null) {
                    m.put("proposedSubstituteName", products.findById(it.getProposedSubstituteSku())
                            .map(Product::getName).orElse(it.getProposedSubstituteSku()));
                }
                items.add(m);
            }
            ctx.put("items", items);
        }
        return ctx;
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isMissingNode() || v.isNull() || v.asText().isBlank() ? null : v.asText();
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}
