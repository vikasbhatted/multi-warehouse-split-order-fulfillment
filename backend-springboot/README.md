# Backend – Multi-Warehouse Split Order Fulfillment

## Overview

The backend of the Multi-Warehouse Split Order Fulfillment system is developed using Java 17 and Spring Boot 3.5.6.

It manages the application's business logic, database operations, REST APIs, and integration with Camunda 8.

The backend coordinates order validation, inventory allocation, parallel warehouse fulfillment, damaged-item substitution, refunds, courier label generation, and shipment tracking.

## Technology Stack

- **Java 17** – Programming language
- **Spring Boot 3.5.6** – Backend framework
- **Maven** – Dependency and build management
- **Camunda 8.8** – Workflow orchestration
- **H2 Database** – Data persistence
- **Spring Data JPA** – Database interaction
- **REST APIs** – Application and courier integration
- **Camunda Java Client** – Workflow and job worker integration
- **Docker** – Containerization

## Features

- Order creation and validation.
- Inventory allocation across multiple warehouses.
- Parallel multi-warehouse fulfillment.
- Shipment registration and management.
- Damaged-item substitution.
- Customer approval and partial refund processing.
- Automatic refunds using timer-based workflow handling.
- Courier label generation through a mock REST API.
- Warehouse task management.
- Order tracking and status updates.
- Exception handling and workflow error management.

## Camunda Integration

The backend integrates with Camunda 8 to execute BPMN workflows.

### BPMN Process IDs

| Process | Process ID |
|---|---|
| Main Fulfillment | `split_order_fulfillment` |
| Warehouse Fulfillment | `warehouse_pick_pack_ship` |
| Carrier Label Generation | `carrier_label_generation` |

### Job Workers

The backend implements the following job workers:

1. `validate-and-allocate`
2. `consolidate-order`
3. `complete-order`
4. `register-shipment`
5. `find-substitutes`
6. `apply-substitution`
7. `issue-refund`
8. `auto-refund-damaged`
9. `mark-shipped`

These workers execute backend business operations when corresponding BPMN service tasks are activated.

## Prerequisites

- Java JDK 17
- Maven
- Camunda 8 Run (version 8.8)
- Docker Desktop (optional)

## Installation and Execution

### 1. Navigate to the backend directory

```bash
cd backend-springboot
```

### 2. Build the application

```bash
mvn clean install
```

### 3. Start Camunda 8 Run

Start the Camunda 8 instance separately.

The backend communicates with Camunda using:

- gRPC: `localhost:26500`
- REST API: `localhost:8080`

### 4. Run the Spring Boot application

```bash
mvn spring-boot:run
```

The backend server runs at:

http://localhost:8081

## REST API Integration

The backend exposes REST endpoints for:

- Order creation and management.
- Product catalog and inventory.
- Warehouse task claiming and completion.
- Shipment tracking.
- System health and status.
- Mock courier label generation.

## Database

The application uses H2 Database for local data persistence.

The database stores business information such as:

- Customer orders.
- Products.
- Warehouses.
- Warehouse stock.
- Shipments.
- Refund records.

## Docker Deployment

The backend can be containerized and started through Docker Compose from the project root:

```bash
docker compose --profile app up --build
```

## Testing and Exception Handling

The backend supports testing of:

- Successful order fulfillment.
- Inventory allocation failures.
- Damaged-item substitution.
- Refund processing failures.
- Invalid delivery addresses.
- Warehouse task execution.
- Courier API responses.

Business exceptions are handled through application-level exception handling and BPMN error handling mechanisms.

## Important Notes

- Camunda 8 Run is configured separately from the Docker Compose application services.
- The courier API is a mock integration for demonstration purposes.
- H2 is used instead of PostgreSQL.
- Do not commit passwords, API keys, or sensitive configuration files.
- Maven-generated `target/` files should not be committed to GitHub.

## Future Enhancements

- PostgreSQL database integration.
- Real courier API integration.
- Production-grade authentication and authorization.
- Improved monitoring and logging.
- Cloud deployment and scalable worker infrastructure.
