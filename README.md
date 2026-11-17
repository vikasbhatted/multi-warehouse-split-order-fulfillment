# Multi-Warehouse Split Order Fulfillment

**Domain:** Retail, E-Commerce & Logistics  
**Source idea number:** 16  
**Team size:** 1 student  
**Project type:** Full-Stack Workflow Automation System

## Team members

- **Student 1:** Vikas Bhatted

## Project overview

Multi-Warehouse Split Order Fulfillment is a full-stack workflow automation system inspired by real-world e-commerce platforms such as Zalando.

The system automates the fulfillment of customer orders containing multiple products stored across different warehouses. When products are distributed among multiple locations, the order is split into separate shipments and processed in parallel.

The project uses **Camunda 8 and BPMN** to orchestrate business workflows, **Spring Boot** for backend services and job workers, and **React.js** for the frontend interface.

Key workflow features include:

- Order validation and inventory allocation.
- Parallel multi-warehouse order fulfillment using Multi-Instance activities.
- Damaged-item handling through substitution or partial refunds.
- Customer approval and timer-based automatic refunds.
- Courier label generation through REST integration.
- Automated email notifications.
- Order tracking and warehouse task management.
- Exception handling and recovery mechanisms.

**Expected outcome:** A working full-stack application that demonstrates how BPMN-based workflow automation can improve order processing, warehouse coordination, exception handling, and shipment management.

## Responsibilities

**Vikas Bhatted – Individual Project Contributor**

Responsible for the complete development and integration of the project, including:

- BPMN process modelling and Camunda 8 integration.
- Multi-Instance, Sub-Process and Call Activity implementation.
- Spring Boot backend development and job workers.
- REST API development and courier integration.
- React.js frontend development.
- Database design and persistence using H2.
- Docker configuration and application deployment.
- Workflow testing, debugging and documentation.

## Folder structure

```text
multi-warehouse-split-order-fulfillment/
│
├── bpmn-models/
│
├── backend-springboot/
│   ├── src/
│   ├── data/
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend-react/
│   ├── src/
│   ├── package.json
│   ├── package-lock.json
│   └── Dockerfile
│
├── docker-compose.yml
└── README.md
```

## Technology stack

| Component | Technology |
|---|---|
| Frontend | React.js, Vite, Tailwind CSS |
| Backend | Java 17, Spring Boot |
| Workflow Engine | Camunda 8.8 |
| Process Modelling | BPMN 2.0 |
| Database | H2 Database |
| API Integration | REST API |
| Email Testing | Mailpit |
| Containerization | Docker, Docker Compose |
| Build Tool | Maven, npm |

## Setup and execution

### Prerequisites

Install the following:

- Java JDK 17
- Maven
- Node.js and npm
- Docker Desktop
- Camunda 8 Run (version 8.8)

### 1. Clone the repository

```bash
git clone https://github.com/vikashbhatted/multi-warehouse-split-order-fulfillment.git
cd multi-warehouse-split-order-fulfillment
```

### 2. Start Camunda 8 Run

Configure and start Camunda 8 Run separately.

The application connects to Camunda through:

- gRPC: `localhost:26500`
- REST API: `localhost:8080`

### 3. Start the backend

```bash
cd backend-springboot
mvn spring-boot:run
```

### 4. Start the frontend

Open another terminal:

```bash
cd frontend-react
npm install
npm run dev
```

### 5. Docker deployment

From the project root:

```bash
docker compose --profile app up --build
```

Mailpit can also be started separately:

```bash
docker compose up -d mailpit
```

### Application URLs

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend | http://localhost:8081 |
| Mailpit | http://localhost:8025 |
| Camunda Operate | http://localhost:8080/operate |

*Camunda 8 Run must be running separately when using the Docker Compose application profile.*

## Testing and evidence

The application supports multiple demonstration scenarios:

1. Successful order fulfillment across multiple warehouses.
2. Single-warehouse order fulfillment.
3. Out-of-stock and damaged-item substitution.
4. Customer rejection and partial refund.
5. Customer approval timeout and automatic refund.
6. Invalid delivery address and correction.
7. Refund gateway failure and manual finance review.
8. Shipment tracking and customer notifications.

Testing evidence can include:

- BPMN workflow diagrams.
- Camunda Operate process execution screenshots.
- Frontend application screenshots.
- Warehouse Tasklist demonstrations.
- Order tracking results.
- REST API responses and application logs.

## Important notes

- The courier integration uses a mock API for demonstration.
- Mailpit captures emails locally for testing.
- H2 is used for application data persistence.
- Never commit API keys, passwords, personal customer information, or other sensitive credentials.

## Future enhancements

- Integration with real courier services.
- Production-grade authentication and role-based authorization.
- Migration to PostgreSQL.
- Real-time shipment tracking.
- Cloud deployment and scalable warehouse integration.

## Conclusion

This project demonstrates the practical application of BPMN-based workflow automation in e-commerce logistics. By combining Camunda 8, Spring Boot, React.js, and Docker, it provides an integrated solution for managing distributed inventory, parallel fulfillment, shipment processing, and business exceptions.
