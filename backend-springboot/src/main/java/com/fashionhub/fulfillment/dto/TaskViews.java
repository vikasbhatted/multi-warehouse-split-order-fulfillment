package com.fashionhub.fulfillment.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public final class TaskViews {
    private TaskViews() {}

    public record TaskView(String taskKey, String name, String elementId, String state, String assignee,
                           List<String> candidateGroups, String processInstanceKey, String processDefinitionId,
                           String creationDate, Map<String, Object> context) {
    }

    public record ClaimRequest(@NotBlank String assignee) {
    }

    public record CompleteTaskRequest(@NotBlank String assignee, Map<String, Object> variables) {
    }
}
