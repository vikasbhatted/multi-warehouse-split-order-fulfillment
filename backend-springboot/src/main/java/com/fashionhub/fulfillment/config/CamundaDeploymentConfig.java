package com.fashionhub.fulfillment.config;

import io.camunda.client.annotation.Deployment;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-deploys all BPMN models and Camunda Forms (packaged from /bpmn-models) to the engine
 * when the application starts. The call-activity children are deployed in the same deployment
 * as the main process.
 */
@Configuration
@Deployment(resources = {"classpath*:/bpmn/*.bpmn", "classpath*:/bpmn/forms/*.form"})
public class CamundaDeploymentConfig {
}
