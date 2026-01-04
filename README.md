# JavaNext

An Order-Payment-Inventory microservices system built with Spring Boot 3, demonstrating event-driven architecture with the outbox pattern and saga orchestration.

## Overview

This application implements a realistic e-commerce backend with three bounded contexts:
- **Order Service**: Handles order creation with bulk support and idempotency
- **Payment Service**: Simulates payment processing with random delays and failures
- **Inventory Service**: Manages product inventory with reservation logic

### Key Features

- **Outbox Pattern**: Reliable event publishing from Order Service to Kafka
- **Saga Orchestration**: Event-driven choreography for order fulfillment
- **Idempotency**: Request-level idempotency using idempotency keys
- **Batch Processing**: JPA batch inserts for bulk order creation
- **OpenAPI Documentation**: Interactive API documentation with Swagger UI

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Docker and Docker Compose (for running PostgreSQL and Kafka)

## Quick Start

### 1. Start Infrastructure Services

Start PostgreSQL and Kafka using Docker Compose:

```bash
docker-compose up -d
```

Wait for services to be healthy:
```bash
docker-compose ps
```

### 2. Build the Application

```bash
mvn clean compile
```

### 3. Run Tests

```bash
mvn test
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

The application will start on http://localhost:8080

## API Documentation

Once the application is running, access:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs

## Available Endpoints

### Order Service

- `POST /orders/bulk` - Create bulk orders with idempotency
  - Header: `Idempotency-Key` (required)
  - Body: BulkOrderRequest (customer ID and list of orders)
  - Response: List of created orders with IDs and statuses

- `GET /orders/{id}` - Get order by ID
  - Response: Order details including status

### Sample Products

The application initializes with sample products:
- LAPTOP-001: Gaming Laptop ($1,299.99) - 100 units
- MOUSE-001: Wireless Mouse ($29.99) - 500 units
- KEYBOARD-001: Mechanical Keyboard ($89.99) - 250 units
- MONITOR-001: 4K Monitor ($499.99) - 75 units
- HEADSET-001: Gaming Headset ($79.99) - 200 units

## Example Usage

### Create Bulk Orders

```bash
curl -X POST http://localhost:8080/orders/bulk \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-key-123" \
  -d '{
    "customerId": "550e8400-e29b-41d4-a716-446655440000",
    "orders": [
      {
        "items": [
          {
            "productId": "<LAPTOP_PRODUCT_ID>",
            "quantity": 1,
            "unitPrice": 1299.99
          }
        ]
      }
    ]
  }'
```

### Get Order Status

```bash
curl http://localhost:8080/orders/{order-id}
```

## Architecture

### Order Flow (Saga Pattern)

1. Client creates order via REST API
2. Order Service saves order (status: PENDING) and creates outbox event
3. OutboxRelay publishes ORDER_CREATED event to Kafka
4. Inventory Service reserves stock:
   - Success → publishes INVENTORY_RESERVED
   - Failure → publishes INVENTORY_REJECTED (order → CANCELLED)
5. Payment Service processes payment (1-3s delay, 20% random failure):
   - Success → publishes PAYMENT_COMPLETED (order → PAID)
   - Failure → publishes PAYMENT_FAILED (order → FAILED)

### Event Topics

- `orders.events`: Order domain events (ORDER_CREATED)
- `payments.events`: Payment events (PAYMENT_COMPLETED, PAYMENT_FAILED)
- `inventory.events`: Inventory events (INVENTORY_RESERVED, INVENTORY_REJECTED)

## Configuration

Key application properties:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/javanext

# Kafka
spring.kafka.bootstrap-servers=localhost:9092

# JPA Batch Processing
spring.jpa.properties.hibernate.jdbc.batch_size=50

# Outbox Relay
outbox.relay.batch-size=100
outbox.relay.fixed-delay=5000
```

## Development

### Project Structure

```
src/main/java/com/javanext/
├── config/              # Configuration classes
├── order/               # Order bounded context
│   ├── domain/         # Entities (Order, OrderItem, OutboxEvent, IdempotencyKey)
│   ├── dto/            # Request/Response DTOs
│   ├── repository/     # JPA repositories
│   ├── service/        # Business logic (OrderService, OutboxRelayService, OrderSagaService)
│   ├── controller/     # REST controllers
│   └── event/          # Event classes
├── payment/            # Payment bounded context
│   ├── domain/         # Entities (Payment)
│   ├── repository/     # JPA repositories
│   ├── service/        # Business logic (PaymentService)
│   └── event/          # Event classes
└── inventory/          # Inventory bounded context
    ├── domain/         # Entities (Product)
    ├── repository/     # JPA repositories
    ├── service/        # Business logic (InventoryService)
    └── event/          # Event classes
```

## Stopping Services

Stop the application: `Ctrl+C`

Stop infrastructure:
```bash
docker-compose down
```

Clean up volumes:
```bash
docker-compose down -v
```

## Troubleshooting

**Connection refused to PostgreSQL**:
- Ensure Docker Compose is running: `docker-compose ps`
- Check PostgreSQL logs: `docker-compose logs postgres`

**Kafka connection issues**:
- Ensure Kafka is running: `docker-compose ps`
- Check Kafka logs: `docker-compose logs kafka`

**Orders stuck in PENDING**:
- Check Kafka consumer logs for errors
- Verify OutboxRelay is running (check logs for "Processing X outbox events")
- Check inventory - may be out of stock

## License

This project is for educational purposes.