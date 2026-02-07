## TtelGo Deployment Guide

## Live server: Stripe and deployment

**Stripe keys are never stored in config files.** Set them as environment variables on the live server.

### 1. Set these environment variables on the live server

On the server (e.g. in your systemd service file, `.env` file, or AWS/hosting env config), set:

```bash
STRIPE_SECRET_KEY=sk_test_...          # or sk_live_... for production
STRIPE_PUBLISHABLE_KEY=pk_test_...     # or pk_live_... for production
STRIPE_WEBHOOK_SECRET=whsec_...
```

Also ensure the server uses the **prod** profile:

```bash
SPRING_PROFILES_ACTIVE=prod
```

(Plus your existing DB, JWT, and other env vars as needed.)

### 2. Deploy backend to live server

From your **local machine** (push code to GitHub):

```bash
git add -A
git commit -m "Backend: Stripe via env vars, ready for live"
git push origin <your-branch-name>
```

On the **live server** (SSH in, then):

```bash
cd /path/to/ttelgo-backend
git pull origin <your-branch-name>
# Build (if using JAR)
./mvnw clean package -DskipTests
# Restart the app (example: systemd)
sudo systemctl restart ttelgo-backend
```

If you use Docker, AWS, or another platform, set the same `STRIPE_*` and `SPRING_PROFILES_ACTIVE=prod` in that platform’s environment configuration, then deploy as usual.

---

## Prerequisites

- Java 17 or higher
- PostgreSQL 14 or higher
- Maven 3.8+
- Environment variables configured
- eSIM Go API credentials
- Stripe API credentials

## Environment Variables

Create a `.env` file or set system environment variables:

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/ttelgo
DB_USERNAME=postgres
DB_PASSWORD=your_password

# eSIM Go API
ESIMGO_API_URL=https://api.esim-go.com/v2.4
ESIMGO_API_KEY=your_esimgo_api_key

# Stripe
STRIPE_SECRET_KEY=sk_test_your_stripe_key
STRIPE_PUBLISHABLE_KEY=pk_test_your_publishable_key
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret

# Application
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_TIKTEL=DEBUG
```

## Local Development

### 1. Database Setup

```bash
# Create database
createdb ttelgo

# Or using psql
psql -U postgres
CREATE DATABASE ttelgo;
```

### 2. Run Application

```bash
# Using Maven
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or using JAR
mvn clean package
java -jar target/ttelgo-backend-1.0.0.jar --spring.profiles.active=dev
```

### 3. Access APIs

- Base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health Check: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/prometheus`

## Production Deployment

### Option 1: Traditional Server Deployment

```bash
# 1. Build production JAR
mvn clean package -DskipTests -Pprod

# 2. Transfer JAR to server
scp target/ttelgo-backend-1.0.0.jar user@server:/opt/ttelgo/

# 3. Create systemd service
sudo nano /etc/systemd/system/ttelgo.service
```

**ttelgo.service:**
```ini
[Unit]
Description=TtelGo eSIM Reseller Platform
After=network.target

[Service]
Type=simple
User=ttelgo
WorkingDirectory=/opt/ttelgo
ExecStart=/usr/bin/java -jar /opt/ttelgo/ttelgo-backend-1.0.0.jar
EnvironmentFile=/opt/ttelgo/.env
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# 4. Start service
sudo systemctl daemon-reload
sudo systemctl enable ttelgo
sudo systemctl start ttelgo
sudo systemctl status ttelgo
```

### Option 2: Docker Deployment

**Dockerfile:**
```dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY target/ttelgo-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Build image
docker build -t ttelgo-backend:1.0.0 .

# Run container
docker run -d \
  --name ttelgo \
  -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://db:5432/ttelgo \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=password \
  -e ESIMGO_API_KEY=your_key \
  -e STRIPE_SECRET_KEY=your_key \
  ttelgo-backend:1.0.0
```

**docker-compose.yml:**
```yaml
version: '3.8'

services:
  db:
    image: postgres:14
    environment:
      POSTGRES_DB: ttelgo
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:postgresql://db:5432/ttelgo
      DB_USERNAME: postgres
      DB_PASSWORD: password
      ESIMGO_API_KEY: ${ESIMGO_API_KEY}
      STRIPE_SECRET_KEY: ${STRIPE_SECRET_KEY}
      STRIPE_WEBHOOK_SECRET: ${STRIPE_WEBHOOK_SECRET}
    depends_on:
      - db

volumes:
  postgres_data:
```

```bash
# Deploy with docker-compose
docker-compose up -d
```

### Option 3: Kubernetes Deployment

**kubernetes/deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ttelgo-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ttelgo-backend
  template:
    metadata:
      labels:
        app: ttelgo-backend
    spec:
      containers:
      - name: ttelgo
        image: ttelgo-backend:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: DB_URL
          valueFrom:
            secretKeyRef:
              name: ttelgo-secrets
              key: db-url
        - name: ESIMGO_API_KEY
          valueFrom:
            secretKeyRef:
              name: ttelgo-secrets
              key: esimgo-api-key
        - name: STRIPE_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: ttelgo-secrets
              key: stripe-secret-key
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: ttelgo-service
spec:
  type: LoadBalancer
  selector:
    app: ttelgo-backend
  ports:
  - port: 80
    targetPort: 8080
```

```bash
# Deploy to Kubernetes
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/secrets.yaml
```

## Database Migration

Flyway automatically runs migrations on startup. To manually manage:

```bash
# Run migrations
mvn flyway:migrate

# Check status
mvn flyway:info

# Rollback (careful!)
mvn flyway:undo
```

## Nginx Reverse Proxy

**nginx.conf:**
```nginx
server {
    listen 80;
    server_name api.ttelgo.com;

    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl http2;
    server_name api.ttelgo.com;

    ssl_certificate /etc/ssl/certs/ttelgo.crt;
    ssl_certificate_key /etc/ssl/private/ttelgo.key;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Monitoring Setup

### Prometheus Configuration

**prometheus.yml:**
```yaml
scrape_configs:
  - job_name: 'ttelgo'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

### Grafana Dashboard

Import Spring Boot dashboard:
- Dashboard ID: 11378
- Data source: Prometheus

## Backup Strategy

### Database Backup

```bash
# Daily backup script
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/backups/ttelgo
pg_dump -U postgres ttelgo | gzip > $BACKUP_DIR/ttelgo_$DATE.sql.gz

# Keep last 7 days
find $BACKUP_DIR -name "ttelgo_*.sql.gz" -mtime +7 -delete
```

```bash
# Add to crontab
0 2 * * * /opt/scripts/backup_ttelgo.sh
```

## Scaling

### Horizontal Scaling

```bash
# Scale with systemd (multiple ports)
sudo systemctl start ttelgo@8081
sudo systemctl start ttelgo@8082

# Scale with Docker
docker-compose up --scale app=3

# Scale with Kubernetes
kubectl scale deployment ttelgo-backend --replicas=5
```

### Database Replication

Set up PostgreSQL replication for read scaling:
1. Primary database for writes
2. Read replicas for queries
3. Use Spring Boot's `@Transactional(readOnly=true)` for read replicas

## Security Checklist

- [ ] HTTPS enabled with valid SSL certificate
- [ ] Firewall configured (only 80/443 open)
- [ ] Database credentials secured
- [ ] API keys in environment variables (not code)
- [ ] Rate limiting enabled
- [ ] Security headers configured
- [ ] CORS configured for frontend domains
- [ ] Regular security updates applied

## Troubleshooting

### Check Application Logs

```bash
# Systemd logs
sudo journalctl -u ttelgo -f

# Docker logs
docker logs -f ttelgo

# Kubernetes logs
kubectl logs -f deployment/ttelgo-backend
```

### Common Issues

**Database Connection Failed:**
```bash
# Check database is running
systemctl status postgresql

# Test connection
psql -h localhost -U postgres -d ttelgo
```

**Application Won't Start:**
```bash
# Check Java version
java -version

# Check port availability
netstat -an | grep 8080

# Check environment variables
printenv | grep DB_
```

**High Memory Usage:**
```bash
# Set JVM memory limits
java -Xms512m -Xmx2048m -jar app.jar
```

## Health Checks

```bash
# Basic health
curl http://localhost:8080/actuator/health

# Detailed health
curl http://localhost:8080/actuator/health?show-details=always

# Database health
curl http://localhost:8080/actuator/health/db

# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Readiness probe
curl http://localhost:8080/actuator/health/readiness
```

## Performance Tuning

### JVM Settings

```bash
java \
  -Xms1g \
  -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/ttelgo/heapdump.hprof \
  -jar app.jar
```

### Database Connection Pool

**application-prod.yml:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

## Support

For issues or questions:
- Check logs first
- Review health endpoints
- Consult ARCHITECTURE.md for design details
- Check NFR_COMPLIANCE_MATRIX.md for requirements

## Maintenance

### Regular Tasks

- Daily: Check logs and metrics
- Weekly: Review database performance
- Monthly: Security patches and updates
- Quarterly: Load testing and capacity planning

