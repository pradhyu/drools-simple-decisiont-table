# Drools Decision Table - Clinical Rules Audit System

This project is a Spring Boot application demonstrating the use of Drools Decision Tables (in CSV format) to define and execute clinical rules. It evaluates `Person` data (age, gender) against a set of rules to generate `Audit` logs.

## Technologies

- **Java 21**: Core language
- **Spring Boot 3.2.1**: Application framework
- **Drools 8.44.0.Final**: Rule engine and Decision Table support
- **Maven**: Dependency management and build tool

## Features

- **CSV Decision Table**: Rules are defined in `src/main/resources/rules/clinical-rules.csv`, making them easy to edit and manage outside of code.
- **REST API**: Exposes an endpoint to submit Person data and receive rule execution results.
- **Automatic DRL Generation**: The application prints the DRL code generated from the CSV decision table to the console on startup for verification and debugging.
- **Rule Control Flow**: Advanced pattern demonstrating how rules can insert control facts to trigger or suppress other rules, implementing "stop after first match" logic. See [RULE_CONTROL_FLOW.md](RULE_CONTROL_FLOW.md) for details.
- **Unit & Integration Tests**: Comprehensive tests covering rule logic and the REST interface.

## Getting Started

### Prerequisites

- JDK 21
- Maven

### Running the Application

To run the application locally:

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.

### Running Tests

To run the unit and integration tests:

```bash
mvn test
```

## API Usage

### Execute Rules

**Endpoint:** `POST /api/rules`

**Request Body:**

```json
{
  "name": "John Doe",
  "age": 30,
  "gender": "Male"
}
```

**Example cURL:**

```bash
curl -X POST http://localhost:8080/api/rules \
     -H "Content-Type: application/json" \
     -d '{"name": "John", "age": 30, "gender": "Male"}'
```

**Response:**

```json
{
  "audits": [
    "Adult Male detected"
  ]
}
```

## Project Structure

- **`rules/clinical-rules.csv`**: The decision table file defining the business rules.
- **`config/DroolsConfig.java`**: Configures the Drools KieContainer and loads the CSV rule file. It also includes logic to print the generated DRL.
- **`service/RulesService.java`**: Service layer that handles the execution of rules against a given `Person` object.
- **`controller/RulesController.java`**: REST controller exposing the rule execution endpoint.
- **`model/`**: Contains simple POJOs (`Person`, `Audit`).
