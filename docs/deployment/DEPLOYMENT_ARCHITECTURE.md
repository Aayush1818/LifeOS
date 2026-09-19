# LifeOS — Deployment & Infrastructure Specification

---

## 1. Production Deployment Topology

```mermaid
flowchart TB
    User[End User Web Client] --> ReverseProxy[NGINX Reverse Proxy / TLS 1.3]
    
    subgraph Host_Server["Application Host Environment"]
        ReverseProxy -->|Port 80/443| StaticUI[Angular 18 Frontend Dist]
        ReverseProxy -->|/api/* (Port 8080)| LifeOS_Backend[LifeOS Spring Boot 3.3.5 / Java 21 LTS]
        
        LifeOS_Backend -->|Port 5432| PostgresDB[(PostgreSQL 16 + pgvector)]
        LifeOS_Backend -->|File I/O| PersistentVolume[(Encrypted Storage Volume /uploads)]
    end

    subgraph External_AI["External AI Services"]
        LifeOS_Backend -->|HTTPS API| AI_Provider[OpenAI / Gemini / Self-hosted Ollama]
    end
```

---

## 2. Docker Compose Production Reference

Below is the production container orchestration specification:

```yaml
version: '3.8'

services:
  postgres:
    image: pgvector/pgvector:pg16
    container_name: lifeos-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-lifeos_db}
      POSTGRES_USER: ${POSTGRES_USER:-postgres}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d lifeos_db"]
      interval: 10s
      timeout: 5s
      retries: 5

  lifeos-backend:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: lifeos-backend
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-lifeos_db}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-postgres}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
      JWT_SECRET: ${JWT_SECRET}
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      FILE_STORAGE_LOCATION: /app/uploads
    ports:
      - "8080:8080"
    volumes:
      - uploads_data:/app/uploads

volumes:
  pgdata:
  uploads_data:
```
