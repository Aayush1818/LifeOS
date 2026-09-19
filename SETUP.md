# LifeOS — Local Development Setup & Prerequisites Guide

This document provides step-by-step instructions to prepare your environment and launch **LifeOS**.

---

## 1. System Requirements & Verified Toolchain

* **Java**: **Java 21 LTS (Eclipse Temurin 21.0.12.1 LTS)** — *Installed and configured*
* **Build Tool**: **Maven Wrapper (`mvnw` / `mvnw.cmd`)** based on Apache Maven 3.9.9
* **Database**: **PostgreSQL 16/18 with native `pgvector` v0.8.6** (configured on port 5433 with `lifeos_db`)
* **Framework**: **Spring Boot 3.3.5** (Spring Framework 6.1.14)
* **Node.js**: v20.x or v22.x LTS (for frontend development in Phase 11)

---

## 2. PostgreSQL & pgvector Configuration

LifeOS runs against a PostgreSQL database with the native `pgvector` extension enabled for HNSW vector cosine search (`vector(1536)`).

* **Default Local Connection**:
  * Host: `localhost` (127.0.0.1)
  * Port: `5433` (configured to prevent conflicts with default service port 5432)
  * Database: `lifeos_db`
  * Username: `postgres`
  * Password: `postgres` (or as defined in `SPRING_DATASOURCE_PASSWORD`)
* **Starting the Database**:
  ```powershell
  & "C:\Users\hp\pg18_custom\bin\pg_ctl.exe" -D "C:\Users\hp\pgdata" -l "C:\Users\hp\pgdata\server.log" start
  ```
* **Stopping the Database**:
  ```powershell
  & "C:\Users\hp\pg18_custom\bin\pg_ctl.exe" -D "C:\Users\hp\pgdata" stop
  ```

---

## 3. Building & Running the Backend

### Using the Bundled Maven Wrapper

#### Run Automated Test Suite:
```powershell
.\mvnw.cmd test
```

#### Package Executable JAR:
```powershell
.\mvnw.cmd package -DskipTests
```

#### Run Spring Boot Application:
```powershell
.\mvnw.cmd spring-boot:run
# Or run the packaged JAR directly:
java -jar target\lifeos-backend-0.1.0-SNAPSHOT.jar
```

---

## 4. Live Verification Endpoints

Once the application is running:
* **System Health Ping**: [`http://localhost:8080/api/v1/health/ping`](http://localhost:8080/api/v1/health/ping)
* **Spring Boot Actuator Health**: [`http://localhost:8080/actuator/health`](http://localhost:8080/actuator/health)
* **OpenAPI 3.0 Specification**: [`http://localhost:8080/v3/api-docs`](http://localhost:8080/v3/api-docs)
* **Interactive Swagger UI**: [`http://localhost:8080/swagger-ui.html`](http://localhost:8080/swagger-ui.html)
