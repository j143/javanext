# JavaNext

A basic Spring Boot application.

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

## Building the Application

```bash
mvn clean compile
```

## Running Tests

```bash
mvn test
```

## Running the Application

```bash
mvn spring-boot:run
```

The application will start on http://localhost:8080

## Available Endpoints

- `GET /` - Returns a welcome message
- `GET /hello` - Returns a hello message

## Testing the Endpoints

```bash
curl http://localhost:8080/
curl http://localhost:8080/hello
```