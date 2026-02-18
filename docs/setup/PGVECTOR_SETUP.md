# PostgreSQL & PGVector Setup Guide

This guide walks you through setting up PostgreSQL and enabling the `pgvector` extension, which is required for OpenClaw4J's RAG (Retrieval-Augmented Generation) capabilities.

## Prerequisites
- A terminal (Zsh/Bash).
- Administrative access to your machine.

---

## 1. Installation

### Option A: macOS (via Homebrew)
This is the recommended method for Mac users.

1. **Install PostgreSQL**:
   ```bash
   brew install postgresql@18
   ```
2. **Start the Service**:
   ```bash
   brew services start postgresql@18
   ```
3. **Install pgvector**:
   ```bash
   brew install pgvector
   ```

### Option B: Docker (All Platforms)
If you prefer not to install Postgres directly on your OS.

1. **Run a PGVector-enabled image**:
   ```bash
   docker run --name openclaw-pg -e POSTGRES_PASSWORD=mysecretpassword -p 5432:5432 -d pgvector/pgvector:pg18
   ```
   *Note: This image comes with the extension pre-installed.*

---

## 2. Initialize Database & Extension

If you installed via Homebrew (Option A), you need to manually create the database and enable the extension.

1. **Enter the Postgres Shell**:
   ```bash
   psql postgres
   ```
2. **Create the Database**:
   ```sql
   CREATE DATABASE openclaw4j;
   ```
3. **Connect to the Database**:
   ```sql
   \c openclaw4j
   ```
4. **Enable the Extension**:
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```
5. **Verify**:
   ```sql
   SELECT extname FROM pg_extension WHERE extname = 'vector';
   ```
   *You should see `vector` in the result.*

---

## 3. Configure OpenClaw4J

Update your `.env` file or `application.yml` with your database credentials.

### Using `.env` (Recommended)
Add the following variables:
```bash
JDBC_URL=jdbc:postgresql://localhost:5432/openclaw4j
DB_USERNAME=your_postgres_username
DB_PASSWORD=your_postgres_password
```

### Using `application.yml`
```yaml
spring:
  datasource:
    url: ${JDBC_URL:jdbc:postgresql://localhost:5432/openclaw4j}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
```

---

## Troubleshooting

- **Extension not found**: If `CREATE EXTENSION vector` fails, ensure the `pgvector` package was installed correctly (see Step 1). On some systems, you might need to restart the Postgres service after installation.
- **Connection refused**: Check if Postgres is running on the expected port (default: 5432).
- **Password authentication failed**: Double-check the `DB_PASSWORD` in your configuration.
- **Docker conflicts**: If port 5432 is already in use by a local Postgres installation, you may need to stop the local service or map the Docker container to a different port (e.g., `-p 5433:5432`).
