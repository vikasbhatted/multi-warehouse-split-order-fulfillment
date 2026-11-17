package com.fashionhub.fulfillment.worker;

import com.fashionhub.fulfillment.common.BusinessErrors;
import com.fashionhub.fulfillment.common.OrderRejectedException;
import com.fashionhub.fulfillment.common.PaymentGatewayException;
import com.fashionhub.fulfillment.dto.DamagedItem;
import com.fashionhub.fulfillment.dto.ShipmentResult;
import com.fashionhub.fulfillment.service.FulfillmentService;
import com.fashionhub.fulfillment.service.FulfillmentService.ValidationResult;
import com.fashionhub.fulfillment.service.OrderService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.VariablesAsType;
import io.camunda.client.api.response.ActivatedJob;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * All job workers of the fulfilment solution. Returned maps become process variables.
 * Business errors are thrown as BPMN errors (caught by Error Boundary Events); any other
 * exception fails the job and is retried by the engine (incident when retries are exhausted).
 */
@Component
public class FulfillmentWorkers {
    private static final Logger log = LoggerFactory.getLogger(FulfillmentWorkers.class);

    private final FulfillmentService fulfillment;
    private final OrderService orderService;

    public FulfillmentWorkers(FulfillmentService fulfillment, OrderService orderService) {
        this.fulfillment = fulfillment;
        this.orderService = orderService;
    }

    // ------------------------------------------------------------ main process
    @JobWorker(type = "validate-and-allocate")
    public Map<String, Object> validateAndAllocate(@VariablesAsType WorkerVariables.Validate v) {
        log.info("[validate-and-allocate] order {}", v.orderId());
        try {
            ValidationResult r = fulfillment.validateAndAllocate(v.orderId());
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("warehouseGroups", r.groups());
            out.put("warehouseCount", r.groups().size());
            out.put("orderTotal", r.total());
            return out;
        } catch (OrderRejectedException e) {
            orderService.markRejected(v.orderId(), e.getMessage());
            throw BusinessErrors.orderRejected(e.getMessage());
        }
    }

    @JobWorker(type = "consolidate-order")
    public Map<String, Object> consolidate(@VariablesAsType WorkerVariables.Consolidate v) {
        log.info("[consolidate-order] order {} with {} parcel(s)", v.orderId(), v.shipmentResults().size());
        return fulfillment.consolidate(v.orderId(), v.shipmentResults());
    }

    @JobWorker(type = "complete-order")
    public Map<String, Object> completeOrder(@VariablesAsType WorkerVariables.Complete v) {
        log.info("[complete-order] order {}", v.orderId());
        return fulfillment.completeOrder(v.orderId());
    }

    // ------------------------------------------------------------ warehouse process
    @JobWorker(type = "register-shipment")
    public Map<String, Object> registerShipment(@VariablesAsType WorkerVariables.Register v, ActivatedJob job) {
        String childInstanceKey = String.valueOf(job.getProcessInstanceKey());
        log.info("[register-shipment] order {} warehouse {} (instance {})", v.orderId(),
                v.warehouseGroup().warehouseId(), childInstanceKey);
        return fulfillment.registerShipment(v.orderId(), v.warehouseGroup(), childInstanceKey);
    }

    @JobWorker(type = "find-substitutes")
    public Map<String, Object> findSubstitutes(@VariablesAsType WorkerVariables.FindSubstitutes v) {
        log.info("[find-substitutes] order {} warehouse {} damaged={}", v.orderId(), v.warehouseId(),
                v.damagedItems().size());
        return fulfillment.findSubstitutes(v.orderId(), v.warehouseId(), v.damagedItems());
    }

    @JobWorker(type = "apply-substitution")
    public void applySubstitution(@VariablesAsType WorkerVariables.ApplySubstitution v) {
        log.info("[apply-substitution] order {}", v.orderId());
        fulfillment.applySubstitution(v.orderId(), v.substitutes());
    }

    /** Runs once per damaged item (sequential multi-instance). */
    @JobWorker(type = "issue-refund")
    public Map<String, Object> issueRefund(@VariablesAsType WorkerVariables.Refund v) {
        DamagedItem item = v.refundItem();
        log.info("[issue-refund] order {} sku {}", v.orderId(), item.sku());
        try {
            return fulfillment.issueRefund(v.orderId(), v.warehouseId(), item,
                    Boolean.TRUE.equals(v.simulateRefundFailure()));
        } catch (PaymentGatewayException e) {
            throw BusinessErrors.refundFailed(e.getMessage());
        }
    }

    @JobWorker(type = "auto-refund-damaged")
    public Map<String, Object> autoRefund(@VariablesAsType WorkerVariables.AutoRefund v) {
        log.warn("[auto-refund-damaged] substitution timed out for order {} - refunding automatically", v.orderId());
        return fulfillment.autoRefundDamaged(v.orderId(), v.warehouseId(), v.damagedItems());
    }

    @JobWorker(type = "mark-shipped")
    public Map<String, Object> markShipped(@VariablesAsType WorkerVariables.MarkShipped v) {
        log.info("[mark-shipped] order {} warehouse {}", v.orderId(), v.warehouseId());
        ShipmentResult result = fulfillment.markShipped(v.orderId(), v.warehouseId(), v.labelResult(),
                v.resolution());
        return Map.of("shipmentResult", result);
    }
}
