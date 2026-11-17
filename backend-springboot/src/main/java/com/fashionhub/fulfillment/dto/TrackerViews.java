package com.fashionhub.fulfillment.dto;

import java.util.List;

/** Live process status tracker: business milestones (DB) + technical steps (engine). */
public final class TrackerViews {
    private TrackerViews() {}

    public record Milestone(String key, String label, String state, String detail) {
    }

    public record ElementView(String elementId, String name, String type, String state, boolean hasIncident,
                              String startDate, String endDate) {
    }

    public record InstanceView(String key, String processDefinitionId, String parentKey, String state,
                               boolean hasIncident, List<ElementView> elements) {
    }

    public record EngineView(boolean available, String message, List<InstanceView> instances) {
    }

    public record TrackerResponse(OrderViews.OrderDetail order, List<Milestone> milestones, EngineView engine,
                                  String operateUrl, boolean finished) {
    }
}
