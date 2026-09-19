# LifeOS — Local Development Setup & Prerequisites Guide

This document provides step-by-step instructions to prepare your environment and launch **LifeOS**.

---

## 1. System Requirements

* **Operating System**: Windows 10/11, macOS (Sonoma+), or Linux (Ubuntu 22.04+ / Debian 12+)
* **Java**: **Java 21 LTS (Java SE 21 or OpenJDK 21)** — *Strict requirement. Java 20 or earlier is not supported.*
* **Node.js**: v20.x or v22.x LTS (npm v10+)
* **Database**: PostgreSQL 16+ with `pgvector` extension (v0.6.0+)
* **Build Tool**: Maven 3.9+ (bundled Maven Wrapper `./mvnw` is provided)

---

## 2. Java 21 LTS Installation Guide

LifeOS is built with Java 21 LTS features (Virtual Threads, Pattern Matching, Record Patterns, Sequenced Collections). If your machine currently has Java 20 or older, follow these instructions to install Java 21 LTS:

### Windows (Recommended via Windows Package Manager / winget)
Open PowerShell as Administrator:
```powershell
# Install Eclipse Temurin JDK 21 LTS
winget install EclipseAdoptium.Temurin.21.JDK

# OR install Microsoft OpenJDK 21 LTS
winget install Microsoft.OpenJDK.21
```

After installation, verify in a new terminal window:
```powershell
java -version
```
Expected output:
```text
openjdk version "21.0.x" ...
OpenJDK Runtime Environment Temurin-21.0.x (build 21.0.x+...)
OpenJDK 64-Bit Server VM Temurin-21.0.x (build 21.0.x+..., mixed mode, sharing)
```

Ensure `JAVA_HOME` is set:
```powershell
[System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot\', 'Machine')
```

### macOS
Using Homebrew:
```bash
brew install openjdk@21
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```

### Linux (Ubuntu / Debian)
```bash
sudo apt update
sudo apt install -y openjdk-21-jdk
java -version
```

---

## 3. PostgreSQL 16 + pgvector Setup

LifeOS utilizes PostgreSQL 16 with the `pgvector` extension for storing 1536-dimensional document embeddings.

### Option A: Using Docker (Recommended for quick start)
If Docker Desktop is installed:
```bash
docker run -d \
  --name lifeos-postgres \
  -e POSTGRES_DB=lifeos_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

### Option B: Native Windows Installation
1. Download and run the PostgreSQL 16 installer from [EnterpriseDB](https://www.enterprisedb.com/downloads/postgres-postgresql-downloads).
2. Install `pgvector` for Windows:
   - Download the precompiled binary release from [pgvector/pgvector releases](https://github.com/pgvector/pgvector/releases).
   - Copy `vector.dll` into `C:\Program Files\PostgreSQL\16\lib`.
   - Copy `vector.control` and `vector--*.sql` files into `C:\Program Files\PostgreSQL\16\share\extension`.
3. Open `psql` or pgAdmin:
   ```sql
   CREATE DATABASE lifeos_db;
   \c lifeos_db;
   CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
   CREATE EXTENSION IF NOT EXISTS vector;
   ```

---

## 4. Application Configuration

1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
2. Adjust the settings:
   - `SPRING_DATASOURCE_URL`: PostgreSQL JDBC connection string
   - `JWT_SECRET`: Random 256-bit hexadecimal string
   - `OPENAI_API_KEY`: Required if using OpenAI as the AI provider

---

## 5. Building & Running the Project

### Backend (Spring Boot 3.3.5)
Using the included Maven wrapper:

* On Windows:
  ```cmd
  mvnw.cmd clean install
  mvnw.cmd spring-boot:run
  ```
* On Linux / macOS:
  ```bash
  ./mvnw clean install
  ./mvnw spring-boot:run
  ```

### Frontend (Angular 18)
Navigate to the frontend directory:
```bash
cd frontend
npm install
npm start
```
The application will be accessible at `http://localhost:4200`.
Swagger UI / OpenAPI documentation is available at `http://localhost:8080/swagger-ui.html`.
