# Distributed Movie Ticket Booking System

An event-driven backend application engine designed to manage movie ticket reservations securely across microservices. This system handles asynchronous event streams, distributed state transitions, and high-volume transactions reliably.

## 🛠️ Architecture & Core Design Patterns

The backend architecture is structured around **data isolation per service** to prevent database coupling and maximize availability.

### 1. Choreography Saga Pattern
Distributed operations (e.g., placing an order, verifying availability, and processing processing charges) coordinate by listening to and emitting domain events over **Apache Kafka**. No single orchestrator directs the flow; instead, individual microservices react independently to events, executing positive workflows or launching rollback events (compensating transactions) if a component downstream encounters a failure.

### 2. Transactional Outbox Pattern
To prevent distributed dual-write inconsistencies (where a database update succeeds but a matching message broker write fails), this project leverages an **Outbox Pattern**. Changes to a microservice's local data and the generation of its outbound event are written into the same atomic database transaction. A separate database polling mechanism or log miner subsequently reads these records and safely publishes them to Apache Kafka.

---

## 🗂️ Microservice Boundaries

The project contains three decoupled microservices, each managing its own isolated MySQL schema:

*   **Order Microservice (`ordermicroservice`)**: Manages the initiation of ticket bookings, status lifecycles (`PENDING`, `CONFIRMED`, `CANCELLED`), and transaction rollbacks if subsequent validation checkpoints fail.
*   **Seat Microservice (`seat_microservice`)**: Handles real-time inventory management, lock allocations, and idempotent seat releases during customer cancellations.
*   **Payment Microservice (`payment_microservice`)**: Manages billing profiles, interactions with external transaction systems, and payment confirmation sequences.

---

## 🚀 Getting Started

### Prerequisites
Ensure the following developer tools are installed locally:
*   **Java 17** or higher
*   **Maven 3.8+**
*   **Docker & Docker Compose** (for running Kafka and MySQL containers simultaneously)

### Installation & Local Setup

1. **Clone the Repository**
   ```bash
   git clone https://github.com
   cd distributed-movie-booking-system
   ```

2. **Spin up Infrastructure Containers**
   Create a basic container setup file to launch your backing services:
   ```bash
   docker-compose up -d
   ```
   *(Ensure local instance mappings are exposed on port `9092` for Kafka and `3306` for MySQL)*

3. **Build the Source Code**
   Run the standard compilation lifecycle from the root package:
   ```bash
   mvn clean install
   ```

4. **Launch the Applications**
   Boot each microservice executable individually within separate terminal environments:
   ```bash
   # Terminal 1: Seat Management Engine
   cd seat_microservice && mvn spring-boot:run
   
   # Terminal 2: Payment Gateway Interface
   cd payment_microservice && mvn spring-boot:run
   
   # Terminal 3: Booking Coordination Engine
   cd ordermicroservice && mvn spring-boot:run
   ```

---

## 🛰️ Kafka Messaging Fabric Topics

The following stream routing channels must be provisioned inside your cluster:

| Topic Name | Publishing Source | Main Subscribing Component(s) | Operational Purpose |
| :--- | :--- | :--- | :--- |
| `order-created-events` | `ordermicroservice` | `seat_microservice` | Allocates and holds candidate theater seats temporarily. |
| `seat-reserved-events` | `seat_microservice` | `payment_microservice` | Triggers customer credit collection after inventory validation. |
| `seat-reservation-failed` | `seat_microservice` | `ordermicroservice` | Aborts the order path due to concurrent seat selection collisions. |
| `payment-success-events` | `payment_microservice` | `ordermicroservice` | Confirms booking state and emits final customer tickets. |
| `payment-failed-events` | `payment_microservice` | `seat_microservice`, `ordermicroservice` | Launches compensation events to unlock seats and mark orders failed. |
