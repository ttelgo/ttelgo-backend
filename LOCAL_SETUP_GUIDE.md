# TtelGo - Local Development Setup

## Quick Start (No Environment Variables Needed!)

Since you already have `application-dev.yml` configured, you can start immediately:

### 1. Create Database
```bash
# Using createdb command
createdb ttelgo_dev

# OR using psql
psql -U postgres
CREATE DATABASE ttelgo_dev;
\q
```

### 2. Run Application
```bash
# Build and run
mvn clean install
mvn spring-boot:run

# Application will start on http://localhost:8080
```

### 3. Access APIs
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/prometheus

---

## ✅ Your Current Configuration (application-dev.yml)

You already have everything configured:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ttelgo_dev
    username: postgres
    password: Post@gre_2026

esimgo:
  api:
    key: gSAXaGtFYQ3yKoda4A8kwksYBq1E4ZO14XmquhN_

stripe:
  secret:
    key: sk_test_51RISllRx29EprcbC... (your test key)
  publishable:
    key: pk_test_51RISllRx29EprcbC... (your publishable key)
  webhook:
    secret: whsec_914baea8785a68fbbb... (your webhook secret)
```

**These values are perfect for local development!**

---

## 🔧 When to Use Environment Variables

### Local Development
**NOT NEEDED** - Use values in `application-dev.yml`

### Production Deployment
**REQUIRED** - Override with environment variables

**Windows PowerShell:**
```powershell
$env:DB_PASSWORD="prod_password"
$env:STRIPE_SECRET_KEY="sk_live_..."
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

**Linux/Mac:**
```bash
export DB_PASSWORD=prod_password
export STRIPE_SECRET_KEY=sk_live_...
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## 📝 How the Configuration Works

The pattern `${ENV_VAR:default}` means:
1. **First**: Try environment variable `ENV_VAR`
2. **If not found**: Use `default` value

Example:
```yaml
username: ${DB_USERNAME:postgres}
```
- If `DB_USERNAME` env var exists → use it
- If not → use `postgres`

---

## 🎯 Your Next Steps

1. **Start PostgreSQL** (if not running)
   ```bash
   # Check if running
   pg_ctl status
   
   # Start if needed
   pg_ctl start
   ```

2. **Create Database**
   ```bash
   createdb ttelgo_dev
   ```

3. **Run Application**
   ```bash
   mvn spring-boot:run
   ```

4. **Test API**
   ```bash
   # Check health
   curl http://localhost:8080/actuator/health
   
   # View Swagger
   # Open browser: http://localhost:8080/swagger-ui.html
   ```

---

## 🐛 Troubleshooting

### Issue: "Database 'ttelgo_dev' does not exist"
```bash
createdb ttelgo_dev
```

### Issue: "Password authentication failed"
Check your PostgreSQL password:
```yaml
# In application-dev.yml
password: Post@gre_2026  # Make sure this matches your PostgreSQL password
```

Or override with environment variable:
```bash
export DB_PASSWORD=your_actual_postgres_password
```

### Issue: "Port 8080 already in use"
```bash
# Find process using port 8080
netstat -ano | findstr :8080

# Kill the process (Windows)
taskkill /PID <process_id> /F
```

---

## 🔐 Security Notes

### For Development (Current Setup)
✅ **Perfectly fine** to have credentials in `application-dev.yml`
✅ Test API keys are safe to commit
✅ Development database can have simple password

### For Production
❌ **Never** commit production credentials
✅ Use environment variables
✅ Use secret management services
✅ Use live Stripe keys (sk_live_...)

---

## 🎉 You're All Set!

Your configuration is already complete. Just run:

```bash
mvn spring-boot:run
```

No environment variables needed for local development! 🚀

