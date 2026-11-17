# BPMN Models – Multi-Warehouse Split Order Fulfillment

## Overview

This directory contains the BPMN 2.0 process models designed for the Multi-Warehouse Split Order Fulfillment project.

The workflows are modelled using Camunda Modeler and executed using Camunda 8. They automate the complete order fulfillment lifecycle, from order validation and warehouse allocation to shipment creation and delivery processing.

## BPMN Process Models

The project consists of three major BPMN processes:

### 1. Split Order Fulfillment

**Process ID:** `split_order_fulfillment`

This is the main orchestration process responsible for managing customer orders.

Key activities:
- Validate customer orders.
- Allocate products across available warehouses.
- Determine whether an order requires single or multiple shipments.
- Trigger parallel warehouse fulfillment processes.
- Collect shipment results.
- Send customer notifications.
- Complete the fulfillment process.

### 2. Warehouse Pick, Pack and Ship

**Process ID:** `warehouse_pick_pack_ship`

This process manages individual warehouse fulfillment activities.

Key activities:
- Register warehouse shipments.
- Perform picking and packing tasks.
- Identify damaged or unavailable products.
- Trigger the substitution and partial refund subprocess.
- Invoke the carrier label generation process.
- Update shipment status.

### 3. Carrier Label Generation

**Process ID:** `carrier_label_generation`

This reusable process handles courier label generation through a REST API integration.

Key activities:
- Submit shipment and address details.
- Generate courier labels.
- Retrieve tracking information.
- Handle invalid delivery addresses.
- Allow address correction and retry.

## BPMN Concepts Implemented

The process models demonstrate the following BPMN constructs:

| BPMN Construct | Purpose |
|---|---|
| Multi-Instance Activity | Executes warehouse fulfillment processes in parallel. |
| Embedded Sub-Process | Handles damaged-item substitution and partial refunds. |
| Call Activity | Invokes reusable warehouse and carrier processes. |
| Exclusive Gateway | Routes orders based on conditions. |
| Parallel Gateway | Coordinates parallel workflow paths. |
| Timer Boundary Event | Handles customer approval timeouts. |
| Error Boundary Event | Handles business and technical exceptions. |
| User Task | Represents manual warehouse and customer support activities. |
| Service Task | Executes automated backend operations. |
| REST Connector | Integrates with the mock courier API. |
| Email Connector | Sends automated customer notifications. |

## Exception Handling

The BPMN models include exception-handling mechanisms for:

- Damaged or unavailable products.
- Customer rejection of substitutions.
- Customer approval timeouts.
- Refund processing failures.
- Invalid delivery addresses.
- Technical service failures.

These mechanisms help maintain workflow continuity and provide appropriate recovery paths.

*The BPMN files should be placed in this directory if maintained separately from the backend resources.*

## Tools Used

- **Camunda Modeler:** BPMN process modelling.
- **Camunda 8:** Workflow execution and orchestration.
- **BPMN 2.0:** Standard process modelling notation.

## Deployment and Execution

1. Open the BPMN files using Camunda Modeler.
2. Verify the process IDs and service task configurations.
3. Start the Camunda 8 Run instance.
4. Deploy the BPMN models to Camunda 8.
5. Start the Spring Boot backend and its job workers.
6. Execute workflows through the React frontend.
7. Monitor process instances and execution details using Camunda Operate.

## Testing and Verification

The BPMN models can be verified using the following scenarios:

- Successful single-warehouse fulfillment.
- Successful multi-warehouse fulfillment.
- Parallel warehouse execution.
- Damaged-item substitution.
- Partial refund processing.
- Customer approval timeout.
- Invalid address correction.
- Refund failure and manual finance review.


## Conclusion

These BPMN models form the workflow orchestration layer of the Multi-Warehouse Split Order Fulfillment system. They demonstrate how business process automation can coordinate distributed warehouse operations, integrate backend services, and manage exceptions in an e-commerce fulfillment environment.
