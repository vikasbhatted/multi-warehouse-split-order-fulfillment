package com.fashionhub.fulfillment.common;

import io.camunda.zeebe.spring.common.exception.ZeebeBpmnError;
import java.util.Map;

/**
 * Central factory for BPMN business errors thrown by job workers.
 *
 * <p>{@link ZeebeBpmnError} is translated by the Camunda Spring Boot Starter into a
 * "throw BPMN error" command, which is caught by the matching Error Boundary Event in the model.
 * (In Camunda 8.8 its non-deprecated parent class is {@code io.camunda.client.exception.BpmnError};
 * both behave identically.)
 */
@SuppressWarnings("removal")
public final class BusinessErrors {

    public static final String ORDER_REJECTED = "ORDER_REJECTED";
    public static final String REFUND_FAILED = "REFUND_FAILED";

    private BusinessErrors() {}

    public static ZeebeBpmnError orderRejected(String reason) {
        return new ZeebeBpmnError(ORDER_REJECTED, reason, Map.of("rejectionReason", reason));
    }

    public static ZeebeBpmnError refundFailed(String reason) {
        return new ZeebeBpmnError(REFUND_FAILED, reason, Map.of("refundFailureReason", reason));
    }
}
