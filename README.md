# Digital Twin Service

This project implements a **Digital Twin** for an air conditioning system using **Spring Boot** and **Spring Integration**

It acts as an aggregator that correlates incoming control commands (HTTP) with telemetry data (MQTT) to verify if the physical device (sensor) state matches the desired command state

---

## Tech Stack

- Java 17
- Spring Boot 4.x
- Spring Integration (DSL, MQTT, HTTP)
- Lombok
- JUnit 5 / Mockito (Testing)

## Architecture

The service uses an event-driven architecture based on Spring Integration flows:

```
Tester Service -- HTTP POST  --> HttpInputFlow
Tester Service -- MQTT Topic --> MqttInputFlow

HttpInputFlow --> |Async| AggregatorFlow
MqttInputFlow --> |Async| AggregatorFlow

AggregatorFlow -- Correlation Logic --> HttpOutputFlow
HttpOutputFlow -- HTTP POST Status --> Tester Service
```

## Prerequisites

Before running the application, ensure you have:

- Java 17 (tested with corretto-17)
- MQTT Broker (e.g., Mosquitto)
- Task Simulator to create input data and verify results

## Configuration

The application is configured via `src/main/resources/application.properties`

| Property             | Default Value                         | Description                                                                                                              |
|:---------------------|:--------------------------------------|:-------------------------------------------------------------------------------------------------------------------------|
| `mqtt.url`           | `tcp://localhost:1883`                | The URI of the MQTT Broker to connect to                                                                                 |
| `mqtt.topic`         | `sensor/temperature`                  | The MQTT topic to subscribe to for sensor readings                                                                       |
| `http.input.url`     | `/api/v1/services/air-conditioner`    | The local HTTP endpoint path that accepts conditioner commands                                                           |
| `output.http.url`    | `http://localhost:8080/api/v1/status` | The external URL (Tester Service) where the aggregator sends the final status                                            |
| `aggregator.timeout` | `20000`                               | The aggregation window in milliseconds (20 seconds). If only one message arrives within this time, the group is released |

## Build & Test

This project uses the **Maven Wrapper** (`mvnw`), so you don't need to install Maven manually

### 1. Run Tests
Execute all Unit and Integration tests.
*Note: The tests use mocks (Mockito & Spring Test), so **no external MQTT broker is required** for this step.*

```bash
./mvnw test
```

### 2. Build the Application
Compile the code and package it into an executable JAR file.

```bash
./mvnw clean package
```

## How to Run

### 1. Download and Run Docker MQQT broker service

```bash
docker pull eclipse-mosquitto:2
docker run --name task-mosquitto-broker -p 1883:1883 -v ./src/main/resources/mosquitto.conf:/mosquitto/config/mosquitto.conf:ro --rm eclipse-mosquitto:2
```

### 2. Start the application

```bash
./mvnw spring-boot:run
```

### 3. Start task-simulator

I was staring it from sources inside IntelliJ
