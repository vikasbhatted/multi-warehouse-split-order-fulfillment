# Frontend – Multi-Warehouse Split Order Fulfillment

## Overview

The frontend of the Multi-Warehouse Split Order Fulfillment system is developed using React.js and Vite. It provides a user-friendly interface for customers, warehouse personnel, and administrators to interact with the order fulfillment system.

The frontend communicates with the Spring Boot backend through REST APIs to manage orders, inventory, warehouse tasks, and shipment tracking.

## Technology Stack

- **React.js 18** – Frontend library
- **Vite** – Development server and build tool
- **JavaScript** – Application logic
- **Tailwind CSS** – Styling and responsive UI
- **React Router** – Client-side navigation
- **Fetch API** – Backend communication
- **Nginx** – Production web server and reverse proxy

## Features

- **Place Order:** Create customer orders containing multiple products.
- **Orders:** View and manage customer orders.
- **Inventory Management:** View product availability across warehouses.
- **Warehouse Tasklist:** Manage picking, packing, substitution, and fulfillment tasks.
- **Order Tracker:** Monitor order progress and shipment status.
- **API Integration:** Communicate with backend services using REST APIs.
- **Responsive Interface:** User-friendly application layout.


## Prerequisites

- Node.js
- npm
- Running Spring Boot backend
- Running Camunda 8 instance

## Installation and Execution

### 1. Navigate to the frontend directory

```bash
cd frontend-react
```

### 2. Install dependencies

```bash
npm install
```

### 3. Start the development server

```bash
npm run dev
```

### 4. Open the application

Visit:

http://localhost:5173

## Docker Deployment

The frontend can also be built and deployed using Docker Compose from the project root:

```bash
docker compose --profile app up --build
```

The frontend is available at:

http://localhost:3000

## Backend Communication

The frontend communicates with the Spring Boot backend through REST endpoints. The backend handles business logic, database operations, and Camunda workflow integration.

## Important Notes

- The `node_modules/` directory is not included in GitHub.
- Dependencies can be restored using `npm install`.
- Do not commit environment files containing sensitive credentials.

## Future Enhancements

- Improved dashboard visualizations.
- Enhanced user authentication and authorization.
- Real-time shipment updates.
- Improved mobile responsiveness.
