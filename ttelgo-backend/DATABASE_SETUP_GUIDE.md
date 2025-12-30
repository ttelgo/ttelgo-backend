# TTelGo Database Setup Guide

Complete guide for setting up the PostgreSQL database for TTelGo Backend application.

## Table of Contents
- [Prerequisites](#prerequisites)
- [PostgreSQL Installation](#postgresql-installation)
- [Database Setup](#database-setup)
- [Configuration](#configuration)
- [Running Migrations](#running-migrations)
- [Verification](#verification)
- [Environment-Specific Setup](#environment-specific-setup)
- [Troubleshooting](#troubleshooting)
- [Best Practices](#best-practices)

---

## Prerequisites

### Required Software
- **PostgreSQL 16+** (recommended: 16.11 or later)
- **Java 17+**
- **Maven 3.6+**

### System Requirements
- Minimum 2GB RAM for PostgreSQL
- 10GB+ disk space
- Network access to port 5432

---

## PostgreSQL Installation

### Windows

1. **Download PostgreSQL**
   - Visit: https://www.postgresql.org/download/windows/
   - Download PostgreSQL 16+ installer
   - Run the installer

2. **Installation Steps**
   - Choose installation directory (default: `C:\Program Files\PostgreSQL\16`)
   - Select components: PostgreSQL Server, pgAdmin 4, Command Line Tools
   - Set data directory (default: `C:\Program Files\PostgreSQL\16\data`)
   - **Important:** Remember the superuser (postgres) password you set!
   - Default port: 5432
   - Complete the installation

3. **Verify Installation**
   ```powershell
   # Check PostgreSQL service is running
   Get-Service -Name postgresql*
   
   # Test connection
   psql -U postgres -h localhost
   ```

### macOS

```bash
# Using Homebrew
brew install postgresql@16

# Start PostgreSQL service
brew services start postgresql@16

# Verify installation
psql --version
```

### Linux (Ubuntu/Debian)

```bash
# Add PostgreSQL repository
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -

# Update and install
sudo apt update
sudo apt install postgresql-16 postgresql-contrib-16

# Start service
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Verify
psql --version
```

---

## Database Setup

### Step 1: Access PostgreSQL

**Windows:**
```powershell
# Using Command Prompt or PowerShell
psql -U postgres
```

**macOS/Linux:**
```bash
# If you get permission denied, use:
sudo -u postgres psql
```

### Step 2: Create Database User (Recommended Practice)

Instead of using the `postgres` superuser, create a dedicated application user:

```sql
-- Create application user with password
CREATE USER ttelgo_user WITH PASSWORD 'your_secure_password_here';

-- Grant necessary privileges
ALTER USER ttelgo_user WITH CREATEDB;

-- Optionally, set connection limit
ALTER USER ttelgo_user CONNECTION LIMIT 50;
```

**Security Best Practices for Passwords:**
- Use at least 12 characters
- Include uppercase, lowercase, numbers, and special characters
- Avoid common words or patterns
- Example format: `TtG@2024!Secure#Pass`

### Step 3: Create Databases

```sql
-- Create development database
CREATE DATABASE ttelgo_dev
    WITH 
    OWNER = ttelgo_user
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    TEMPLATE template0;

-- Grant all privileges
GRANT ALL PRIVILEGES ON DATABASE ttelgo_dev TO ttelgo_user;

-- Create test database (for running tests)
CREATE DATABASE ttelgo_test
    WITH 
    OWNER = ttelgo_user
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    TEMPLATE template0;

GRANT ALL PRIVILEGES ON DATABASE ttelgo_test TO ttelgo_user;

-- Create staging database (optional)
CREATE DATABASE ttelgo_staging
    WITH 
    OWNER = ttelgo_user
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    TEMPLATE template0;

GRANT ALL PRIVILEGES ON DATABASE ttelgo_staging TO ttelgo_user;
```

### Step 4: Configure Schema Permissions

```sql
-- Connect to the database
\c ttelgo_dev

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO ttelgo_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ttelgo_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ttelgo_user;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ttelgo_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ttelgo_user;
```

### Step 5: Verify Database Creation

```sql
-- List all databases
\l

-- Connect to your database
\c ttelgo_dev

-- Check current connection info
\conninfo

-- Exit psql
\q
```

---

## Configuration

### Step 1: Create Environment Variables File

Create a `.env` file in the project root (DO NOT commit this to Git):

```bash
# Database Configuration
DB_USERNAME=ttelgo_user
DB_PASSWORD=your_secure_password_here
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ttelgo_dev

# Connection Pool Settings (Optional - use defaults if unsure)
DB_POOL_MAX=10
DB_POOL_MIN_IDLE=2
DB_POOL_CONNECTION_TIMEOUT_MS=30000
DB_POOL_VALIDATION_TIMEOUT_MS=5000
DB_POOL_IDLE_TIMEOUT_MS=600000
DB_POOL_MAX_LIFETIME_MS=1800000

# Application Settings
SPRING_PROFILES_ACTIVE=dev

# eSIMGo API (get from https://www.esim-go.com/)
ESIMGO_API_KEY=your_esimgo_api_key_here

# Stripe API (get from https://dashboard.stripe.com/)
STRIPE_SECRET_KEY=your_stripe_secret_key
STRIPE_PUBLISHABLE_KEY=your_stripe_publishable_key
STRIPE_WEBHOOK_SECRET=your_stripe_webhook_secret

# Frontend URL
FRONTEND_URL=http://localhost:5173
```

### Step 2: Update Application Configuration

The `application-dev.yml` file already uses environment variables with fallbacks:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:ttelgo_dev}
    username: ${DB_USERNAME:ttelgo_user}
    password: ${DB_PASSWORD}
```

**Note:** Never commit real credentials. Use environment variables for sensitive data.

### Step 3: Add .env to .gitignore

Ensure `.env` is in your `.gitignore` file:

```bash
# Add to .gitignore
.env
.env.local
*.env
application-*.yml.local
```

### Step 4: Set Environment Variables

**Windows PowerShell:**
```powershell
# Set for current session
$env:DB_USERNAME="ttelgo_user"
$env:DB_PASSWORD="your_secure_password_here"
$env:DB_NAME="ttelgo_dev"

# Or set permanently (User level)
[System.Environment]::SetEnvironmentVariable('DB_USERNAME', 'ttelgo_user', 'User')
[System.Environment]::SetEnvironmentVariable('DB_PASSWORD', 'your_secure_password_here', 'User')
```

**macOS/Linux Bash:**
```bash
# Add to ~/.bashrc or ~/.zshrc
export DB_USERNAME=ttelgo_user
export DB_PASSWORD=your_secure_password_here
export DB_NAME=ttelgo_dev

# Reload shell configuration
source ~/.bashrc
```

---

## Running Migrations

### About Flyway Migrations

This project uses **Flyway** for database migrations. Migration scripts are in:
```
src/main/resources/db/migration/
```

### Migration Files

Current migrations:
- `V1__init.sql` - Initial schema (users, orders, esims, etc.)
- `V2__create_blog_posts_table.sql` - Blog functionality
- `V3__add_activated_at_to_esims.sql` - eSIM activation tracking
- `V4__update_esims_table_schema.sql` - eSIM schema updates
- `V5__create_faqs_table.sql` - FAQ functionality
- `V6__create_api_keys_and_usage_tracking.sql` - API key management
- `V7__create_payments_table.sql` - Payment tracking
- `V8__stripe_checkout_and_webhooks.sql` - Stripe integration

### Enable Flyway

Update `src/main/resources/application-dev.yml`:

```yaml
spring:
  flyway:
    enabled: true  # Change from false to true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### Run Migrations

Migrations run automatically when you start the application:

```bash
# Build the project
mvn clean install

# Run the application (migrations will execute)
mvn spring-boot:run
```

### Manual Migration Control

```bash
# Run migrations without starting the app
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Validate migrations
mvn flyway:validate

# Repair migration history (use with caution)
mvn flyway:repair
```

### Migration Best Practices

1. **Never modify existing migration files** - Create new ones instead
2. **Always test migrations** on development database first
3. **Backup database** before running migrations in production
4. **Use sequential version numbers** (V1, V2, V3, etc.)
5. **Make migrations idempotent** when possible

---

## Verification

### Step 1: Test Database Connection

```bash
# Test with psql
psql -h localhost -U ttelgo_user -d ttelgo_dev -c "SELECT version();"
```

### Step 2: Start the Application

```bash
# Start with dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Step 3: Check Application Logs

Look for these success indicators:

```
✅ HikariPool-1 - Starting...
✅ HikariPool-1 - Start completed.
✅ Flyway migration completed successfully
✅ Initialized JPA EntityManagerFactory
✅ Tomcat started on port 8080
```

### Step 4: Check Health Endpoint

Open browser or use curl:
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

### Step 5: Verify Tables Created

```sql
-- Connect to database
psql -U ttelgo_user -d ttelgo_dev

-- List all tables
\dt

-- Expected tables:
-- users, orders, esims, payments, plans, blog_posts, faqs, 
-- api_keys, notifications, kyc_documents, sessions, otp_tokens, etc.

-- Check table structure
\d users

-- Check row counts
SELECT 
    schemaname,
    tablename,
    n_live_tup as row_count
FROM pg_stat_user_tables
ORDER BY tablename;

-- Exit
\q
```

---

## Environment-Specific Setup

### Development Environment

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ttelgo_dev
    username: ${DB_USERNAME:ttelgo_user}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate  # Use 'validate' with Flyway
    show-sql: true  # Show SQL queries in logs
  flyway:
    enabled: true
```

### Test Environment

```yaml
# application-test.yml (create this file)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ttelgo_test
    username: ${DB_USERNAME:ttelgo_user}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: create-drop  # Recreate schema for each test run
    show-sql: false
  flyway:
    enabled: false  # Let JPA create schema for tests
```

### Staging Environment

```yaml
# application-staging.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
```

### Production Environment

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20  # Increase for production
      minimum-idle: 5
  jpa:
    hibernate:
      ddl-auto: validate  # NEVER use 'update' or 'create' in production
    show-sql: false
  flyway:
    enabled: true
    validate-on-migrate: true
```

---

## Troubleshooting

### Issue 1: Connection Refused

**Error:**
```
Connection refused: connect
```

**Solutions:**
1. Check if PostgreSQL is running:
   ```bash
   # Windows
   Get-Service -Name postgresql*
   
   # macOS
   brew services list
   
   # Linux
   sudo systemctl status postgresql
   ```

2. Verify port 5432 is not blocked:
   ```bash
   # Windows
   netstat -ano | findstr :5432
   
   # macOS/Linux
   netstat -an | grep 5432
   ```

3. Check PostgreSQL configuration:
   - Edit `postgresql.conf`
   - Set: `listen_addresses = 'localhost'` or `'*'`
   - Restart PostgreSQL

### Issue 2: Authentication Failed

**Error:**
```
FATAL: password authentication failed for user "ttelgo_user"
```

**Solutions:**
1. Verify credentials:
   ```bash
   psql -U ttelgo_user -d ttelgo_dev
   ```

2. Reset password:
   ```sql
   ALTER USER ttelgo_user WITH PASSWORD 'new_secure_password';
   ```

3. Check `pg_hba.conf`:
   ```
   # Add or modify this line
   host    all             all             127.0.0.1/32            md5
   ```

4. Restart PostgreSQL

### Issue 3: Database Does Not Exist

**Error:**
```
FATAL: database "ttelgo_dev" does not exist
```

**Solution:**
```sql
-- Connect as postgres
psql -U postgres

-- Create database
CREATE DATABASE ttelgo_dev OWNER ttelgo_user;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE ttelgo_dev TO ttelgo_user;
```

### Issue 4: Flyway Migration Failures

**Error:**
```
FlywayException: Validate failed: Detected applied migration not resolved locally
```

**Solutions:**

1. **Check migration history:**
   ```sql
   \c ttelgo_dev
   SELECT * FROM flyway_schema_history ORDER BY installed_rank;
   ```

2. **Repair migration history:**
   ```bash
   mvn flyway:repair
   ```

3. **Clean and re-migrate (ONLY for development):**
   ```bash
   mvn flyway:clean
   mvn flyway:migrate
   ```

   ⚠️ **WARNING:** `flyway:clean` deletes all database objects!

### Issue 5: Port Already in Use

**Error:**
```
Port 5432 is already in use
```

**Solutions:**
1. Find process using port:
   ```bash
   # Windows
   netstat -ano | findstr :5432
   taskkill /PID <process_id> /F
   
   # macOS/Linux
   lsof -i :5432
   kill -9 <process_id>
   ```

2. Change PostgreSQL port in `postgresql.conf`

### Issue 6: Out of Connections

**Error:**
```
FATAL: sorry, too many clients already
```

**Solutions:**
1. Check current connections:
   ```sql
   SELECT count(*) FROM pg_stat_activity;
   ```

2. Increase max connections:
   ```sql
   ALTER SYSTEM SET max_connections = 200;
   ```

3. Restart PostgreSQL

4. Reduce connection pool size in application:
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 5
   ```

---

## Best Practices

### Security

1. **Never use default passwords** in production
2. **Use environment variables** for all sensitive data
3. **Create dedicated database users** - don't use `postgres` superuser
4. **Limit user permissions** - grant only necessary privileges
5. **Use SSL/TLS connections** in production:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://host:5432/db?sslmode=require
   ```

### Performance

1. **Configure connection pooling properly:**
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 10  # Adjust based on load
         minimum-idle: 2
         connection-timeout: 30000
         idle-timeout: 600000
         max-lifetime: 1800000
   ```

2. **Create indexes** for frequently queried columns
3. **Monitor slow queries** - enable log_min_duration_statement
4. **Regular VACUUM and ANALYZE:**
   ```sql
   VACUUM ANALYZE;
   ```

### Backup and Recovery

1. **Regular backups:**
   ```bash
   # Backup database
   pg_dump -U ttelgo_user -d ttelgo_dev -F c -f backup_$(date +%Y%m%d).dump
   
   # Restore database
   pg_restore -U ttelgo_user -d ttelgo_dev backup_20241230.dump
   ```

2. **Automated backup script:**
   ```bash
   # Create backup script
   #!/bin/bash
   DATE=$(date +%Y%m%d_%H%M%S)
   pg_dump -U ttelgo_user -d ttelgo_dev -F c -f /backups/ttelgo_$DATE.dump
   
   # Keep only last 7 days
   find /backups -name "ttelgo_*.dump" -mtime +7 -delete
   ```

3. **Schedule with cron (Linux/macOS):**
   ```bash
   # Daily backup at 2 AM
   0 2 * * * /path/to/backup-script.sh
   ```

### Monitoring

1. **Enable query logging:**
   ```sql
   ALTER SYSTEM SET log_statement = 'all';
   ALTER SYSTEM SET log_duration = on;
   ```

2. **Monitor active connections:**
   ```sql
   SELECT 
       datname,
       usename,
       application_name,
       client_addr,
       state,
       query
   FROM pg_stat_activity
   WHERE state = 'active';
   ```

3. **Check database size:**
   ```sql
   SELECT 
       pg_database.datname,
       pg_size_pretty(pg_database_size(pg_database.datname)) AS size
   FROM pg_database
   ORDER BY pg_database_size(pg_database.datname) DESC;
   ```

### Development Workflow

1. **Use separate databases** for dev, test, and staging
2. **Test migrations locally** before applying to shared environments
3. **Use database seeding** for development data
4. **Document schema changes** in migration files
5. **Review and optimize queries** regularly

---

## Quick Reference Commands

### PostgreSQL Commands

```bash
# Start PostgreSQL
# Windows: Services > postgresql-x64-16 > Start
brew services start postgresql@16  # macOS
sudo systemctl start postgresql    # Linux

# Stop PostgreSQL
brew services stop postgresql@16   # macOS
sudo systemctl stop postgresql     # Linux

# Restart PostgreSQL
brew services restart postgresql@16  # macOS
sudo systemctl restart postgresql    # Linux

# Check status
brew services list                    # macOS
sudo systemctl status postgresql      # Linux
```

### psql Commands

```sql
\l              -- List all databases
\c dbname       -- Connect to database
\dt             -- List all tables
\d tablename    -- Describe table structure
\du             -- List all users
\dn             -- List all schemas
\df             -- List all functions
\dv             -- List all views
\di             -- List all indexes
\q              -- Quit psql
\?              -- Help on psql commands
\h              -- Help on SQL commands
```

### Maven Flyway Commands

```bash
mvn flyway:migrate    -- Run migrations
mvn flyway:info       -- Show migration status
mvn flyway:validate   -- Validate migrations
mvn flyway:repair     -- Repair migration history
mvn flyway:clean      -- Drop all objects (careful!)
```

---

## Additional Resources

### Official Documentation
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Spring Boot Data JPA](https://spring.io/guides/gs/accessing-data-jpa/)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)

### Useful Tools
- **pgAdmin 4** - GUI for PostgreSQL management
- **DBeaver** - Universal database tool
- **DataGrip** - JetBrains database IDE
- **Azure Data Studio** - Cross-platform database tool

### Contact & Support

For database-related issues:
1. Check this guide first
2. Review application logs
3. Test database connection independently
4. Contact the development team

---

**Version:** 1.0.0  
**Last Updated:** December 2024  
**Maintained By:** TTelGo Development Team

