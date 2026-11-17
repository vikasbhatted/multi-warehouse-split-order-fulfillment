package com.fashionhub.fulfillment.web;

import com.fashionhub.fulfillment.camunda.ProcessStarter;
import io.camunda.client.api.response.Topology;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final ProcessStarter starter;

    public SystemController(ProcessStarter starter) {
        this.starter = starter;
    }

    /** Lets the UI show whether the Camunda engine (c8run) is reachable. */
    @GetMapping("/engine")
    public Map<String, Object> engine() {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            Topology t = starter.topology();
            out.put("connected", true);
            out.put("gatewayVersion", t.getGatewayVersion());
            out.put("clusterSize", t.getClusterSize());
        } catch (RuntimeException e) {
            out.put("connected", false);
            out.put("message", "Camunda engine not reachable. Start it with: c8run.exe start");
        }
        return out;
    }
}
