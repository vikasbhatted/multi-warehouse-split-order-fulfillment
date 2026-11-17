package com.fashionhub.fulfillment.camunda;

import com.fashionhub.fulfillment.common.BadRequestException;
import com.fashionhub.fulfillment.common.ConflictException;
import com.fashionhub.fulfillment.common.EngineUnavailableException;
import com.fashionhub.fulfillment.common.ResourceNotFoundException;
import com.fashionhub.fulfillment.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Client for the Orchestration Cluster REST API v2 (user tasks, process-instance and
 * element-instance search). Used by the custom Tasklist and the live status tracker.
 */
@Component
public class CamundaEngineClient {

    private final RestClient rest;

    public CamundaEngineClient(AppProperties.CamundaApi props, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        String base = props.baseUrl().replaceAll("/+$", "") + "/v2";
        RestClient.Builder b = builder.baseUrl(base)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (props.username() != null && !props.username().isBlank()) {
            b.defaultHeaders(h -> h.setBasicAuth(props.username(), props.password() == null ? "" : props.password()));
        }
        this.rest = b.build();
    }

    // ---------------------------------------------------------------- user tasks
    public JsonNode searchUserTasks(String state) {
        return post("/user-tasks/search", Map.of(
                "filter", Map.of("state", state),
                "sort", List.of(Map.of("field", "creationDate", "order", "DESC")),
                "page", Map.of("limit", 200)));
    }

    public JsonNode getUserTask(String key) {
        return get("/user-tasks/" + key);
    }

    public void assign(String key, String assignee) {
        post("/user-tasks/" + key + "/assignment", Map.of("assignee", assignee, "allowOverride", false));
    }

    public void unassign(String key) {
        call(() -> rest.delete().uri("/user-tasks/{key}/assignee", key).retrieve().toBodilessEntity());
    }

    public void complete(String key, Map<String, Object> variables) {
        post("/user-tasks/" + key + "/completion", Map.of("variables", variables));
    }

    // ---------------------------------------------------------------- process instances
    public JsonNode getProcessInstance(String key) {
        return get("/process-instances/" + key);
    }

    /** Finds the main process instance of an order through its `orderId` process variable (or null). */
    public String findMainInstanceKey(String processId, String orderId) {
        JsonNode res = post("/process-instances/search", Map.of(
                "filter", Map.of("processDefinitionId", processId,
                        "variables", List.of(Map.of("name", "orderId", "value", "\"" + orderId + "\""))),
                "page", Map.of("limit", 1)));
        JsonNode first = res.path("items").path(0);
        return first.isMissingNode() ? null : first.path("processInstanceKey").asText(null);
    }

    public JsonNode searchChildInstances(String parentKey) {
        return post("/process-instances/search", Map.of(
                "filter", Map.of("parentProcessInstanceKey", parentKey),
                "page", Map.of("limit", 100)));
    }

    public JsonNode searchElementInstances(String processInstanceKey) {
        return post("/element-instances/search", Map.of(
                "filter", Map.of("processInstanceKey", processInstanceKey),
                "sort", List.of(Map.of("field", "startDate", "order", "ASC")),
                "page", Map.of("limit", 300)));
    }

    // ---------------------------------------------------------------- plumbing
    private JsonNode get(String path) {
        return call(() -> rest.get().uri(path).retrieve().body(JsonNode.class));
    }

    private JsonNode post(String path, Object body) {
        return call(() -> rest.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(JsonNode.class));
    }

    private <T> T call(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            String detail = extractDetail(e);
            if (code == 404) {
                throw new ResourceNotFoundException(detail);
            } else if (code == 409) {
                throw new ConflictException(detail);
            } else if (code == 400 || code == 422) {
                throw new BadRequestException(detail);
            }
            throw new EngineUnavailableException("Camunda API error " + code + ": " + detail, e);
        } catch (RestClientException e) {
            throw new EngineUnavailableException("Camunda REST API not reachable: " + e.getMessage(), e);
        }
    }

    private String extractDetail(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return e.getStatusText();
        }
        return body.length() > 400 ? body.substring(0, 400) : body;
    }
}
