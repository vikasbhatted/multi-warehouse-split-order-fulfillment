package com.fashionhub.fulfillment.camunda;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.response.Topology;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Thin wrapper around the Camunda Java client (gRPC) for starting instances and health checks. */
@Component
public class ProcessStarter {

    public static final String MAIN_PROCESS_ID = "split_order_fulfillment";

    private final CamundaClient client;

    public ProcessStarter(CamundaClient client) {
        this.client = client;
    }

    public String startFulfillment(Map<String, Object> variables) {
        ProcessInstanceEvent event = client.newCreateInstanceCommand()
                .bpmnProcessId(MAIN_PROCESS_ID)
                .latestVersion()
                .variables(variables)
                .send()
                .join();
        return String.valueOf(event.getProcessInstanceKey());
    }

    public Topology topology() {
        return client.newTopologyRequest().send().join();
    }
}
