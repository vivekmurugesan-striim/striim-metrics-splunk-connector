# Production Deployment Guide

This guide covers deploying the Striim Splunk Connector to production environments.

## Pre-Deployment Checklist

- [ ] All code merged to main branch
- [ ] Tests pass (backend and frontend)
- [ ] Security review completed
- [ ] Dependencies updated and scanned for vulnerabilities
- [ ] Environment variables configured
- [ ] Database backup plan in place
- [ ] Rollback procedure documented
- [ ] Monitoring and alerting configured
- [ ] Documentation updated

## Environment Configuration

### Production Environment Variables

Create a `.env.production` file with:

```env
# Database
DB_USERNAME=<production-db-user>
DB_PASSWORD=<strong-secure-password>

# JWT
JWT_SECRET=<long-random-secret-key>

# Striim Configuration
# These can be configured via UI after deployment
# Or set here for initial setup
# STRIIM_URL=https://striim.prod.company.com
# STRIIM_TOKEN=<encrypted-token>

# Splunk Configuration
# SPLUNK_HEC_URL=https://splunk.prod.company.com:8088/services/collector
# SPLUNK_TOKEN=<hec-token>

# Collection Settings
COLLECTION_INTERVAL=60
```

### Security Configuration

1. **Generate strong JWT secret**:
```bash
openssl rand -base64 32
```

2. **Database credentials**:
   - Use strong, unique passwords
   - Store in secure vault (AWS Secrets Manager, HashiCorp Vault, etc.)
   - Rotate regularly

3. **API tokens**:
   - Store Striim and Splunk tokens in secure vault
   - Use IAM roles instead of static credentials where possible
   - Rotate on schedule (quarterly minimum)

## Docker Deployment

### Building Production Images

1. **Build images**:
```bash
# Backend
docker build -t striim-splunk-connector:1.0.0 .

# Frontend
docker build -t striim-splunk-connector-ui:1.0.0 -f frontend/Dockerfile .

# Tag for registry
docker tag striim-splunk-connector:1.0.0 \
  registry.company.com/striim-splunk-connector:1.0.0
docker tag striim-splunk-connector-ui:1.0.0 \
  registry.company.com/striim-splunk-connector-ui:1.0.0
```

2. **Push to registry**:
```bash
docker push registry.company.com/striim-splunk-connector:1.0.0
docker push registry.company.com/striim-splunk-connector-ui:1.0.0
```

### Docker Compose Production Deployment

Create `docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: striim-connector-db
    restart: always
    environment:
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_DB: striim_connector
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backups:/backups
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - striim-network
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

  backend:
    image: registry.company.com/striim-splunk-connector:1.0.0
    container_name: striim-connector-backend
    restart: always
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/striim_connector
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      STRIIM_METRICS_COLLECTION_INTERVAL_SECONDS: ${COLLECTION_INTERVAL}
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - striim-network
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "10"

  frontend:
    image: registry.company.com/striim-splunk-connector-ui:1.0.0
    container_name: striim-connector-frontend
    restart: always
    ports:
      - "3000:3000"
    depends_on:
      - backend
    networks:
      - striim-network
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

volumes:
  postgres_data:
    driver: local

networks:
  striim-network:
    driver: bridge
```

Deploy with:
```bash
docker-compose -f docker-compose.prod.yml up -d
```

## Kubernetes Deployment

### Prerequisites

- Kubernetes cluster (1.24+)
- kubectl configured
- Docker images pushed to registry
- PostgreSQL external service or StatefulSet

### Kubernetes Manifests

Create `k8s/namespace.yaml`:
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: striim-connector
```

Create `k8s/configmap.yaml`:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: striim-connector-config
  namespace: striim-connector
data:
  COLLECTION_INTERVAL: "60"
  SPRING_JPA_HIBERNATE_DDL_AUTO: "validate"
```

Create `k8s/secret.yaml`:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: striim-connector-secret
  namespace: striim-connector
type: Opaque
stringData:
  DB_USERNAME: <username>
  DB_PASSWORD: <password>
  JWT_SECRET: <secret>
```

Create `k8s/backend-deployment.yaml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: striim-connector-backend
  namespace: striim-connector
spec:
  replicas: 2
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
      - name: backend
        image: registry.company.com/striim-splunk-connector:1.0.0
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: striim-connector-config
        - secretRef:
            name: striim-connector-secret
        livenessProbe:
          httpGet:
            path: /api/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /api/health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 10
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: backend-service
  namespace: striim-connector
spec:
  type: ClusterIP
  selector:
    app: backend
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
```

Create `k8s/frontend-deployment.yaml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: striim-connector-frontend
  namespace: striim-connector
spec:
  replicas: 2
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
      - name: frontend
        image: registry.company.com/striim-splunk-connector-ui:1.0.0
        imagePullPolicy: Always
        ports:
        - containerPort: 3000
        livenessProbe:
          httpGet:
            path: /
            port: 3000
          initialDelaySeconds: 30
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /
            port: 3000
          initialDelaySeconds: 10
          periodSeconds: 10
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "250m"
---
apiVersion: v1
kind: Service
metadata:
  name: frontend-service
  namespace: striim-connector
spec:
  type: LoadBalancer
  selector:
    app: frontend
  ports:
  - protocol: TCP
    port: 80
    targetPort: 3000
```

Deploy to Kubernetes:
```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/backend-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml
```

## Database Management

### Initial Setup

```bash
# Create database
createdb -U postgres striim_connector

# Apply migrations (automatic with Spring Boot)
# Or manual:
psql -U postgres -d striim_connector < src/main/resources/db/migration/V1__initial_schema.sql
```

### Backup Strategy

```bash
# Daily backup
0 2 * * * pg_dump -U postgres striim_connector > /backups/striim_$(date +\%Y\%m\%d).sql

# Backup to S3
0 2 * * * pg_dump -U postgres striim_connector | aws s3 cp - s3://backups/striim/$(date +\%Y\%m\%d).sql
```

### Restore from Backup

```bash
# Restore database
psql -U postgres striim_connector < /backups/striim_20260901.sql
```

## Monitoring & Alerting

### Prometheus Metrics

Enable Spring Boot Actuator:

```yaml
spring:
  boot:
    admin:
      client:
        url: http://prometheus:9090
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### Key Metrics to Monitor

- `striim_metrics_collection_duration_seconds` - Collection execution time
- `striim_metrics_published_total` - Metrics published count
- `striim_metrics_errors_total` - Collection errors
- `striim_api_calls_duration_seconds` - API call latency
- `spring_db_connections_max` - Database connection pool status

### Alert Rules

```yaml
groups:
- name: striim_connector
  rules:
  - alert: CollectionFailures
    expr: rate(striim_metrics_errors_total[5m]) > 0
    for: 5m
    annotations:
      summary: "Metrics collection failures detected"

  - alert: HighLatency
    expr: striim_metrics_collection_duration_seconds > 30
    for: 5m
    annotations:
      summary: "Metrics collection latency is high"

  - alert: DatabaseConnections
    expr: spring_db_connections_max == spring_db_connections
    for: 2m
    annotations:
      summary: "Database connection pool exhausted"

  - alert: SplunkPublishFailures
    expr: rate(striim_metrics_errors_total{type="splunk"}[5m]) > 0
    for: 5m
    annotations:
      summary: "Failed to publish metrics to Splunk"
```

## SSL/TLS Configuration

### Generate Self-Signed Certificate

```bash
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes
```

### Configure Nginx for HTTPS

```nginx
server {
    listen 443 ssl;
    server_name striim-connector.company.com;

    ssl_certificate /etc/nginx/certs/cert.pem;
    ssl_certificate_key /etc/nginx/certs/key.pem;

    # ... rest of configuration
}

server {
    listen 80;
    server_name striim-connector.company.com;
    return 301 https://$server_name$request_uri;
}
```

## Scaling Considerations

### Horizontal Scaling

1. **Multiple Backend Instances**:
   - Use load balancer (HAProxy, nginx, AWS ELB)
   - Ensure stateless design
   - All instances share same database

2. **Database Scaling**:
   - Use PostgreSQL replication for read scaling
   - Implement connection pooling (PgBouncer)
   - Consider read replicas for reporting

### Vertical Scaling

- Increase JVM heap size: `-Xmx2g -Xms1g`
- Increase database connections: `hikari.maximum-pool-size`
- Adjust collection interval based on load

## Disaster Recovery

### Backup Plan

- **Frequency**: Daily
- **Retention**: 30 days
- **Location**: Off-site (S3, GCS, etc.)
- **Testing**: Monthly restore tests

### Failover Plan

1. Database failover: Use PostgreSQL streaming replication
2. Application failover: Use load balancer with health checks
3. Data recovery: From latest backup

### RTO/RPO Targets

- **RTO** (Recovery Time Objective): 1 hour
- **RPO** (Recovery Point Objective): 15 minutes

## Post-Deployment

### Verification Steps

```bash
# Check services
curl http://localhost:8080/api/health
curl http://localhost:3000

# Check database
psql -U postgres -d striim_connector -c "\dt"

# Check logs
docker-compose logs -f

# Verify configuration
curl http://localhost:8080/api/v1/config
```

### Performance Baselines

Document initial metrics:
- Response time latencies
- Throughput (requests/second)
- Database query times
- Memory usage

## Rollback Procedure

If deployment has issues:

```bash
# Stop current version
docker-compose down

# Pull previous image
docker pull registry.company.com/striim-splunk-connector:1.0.0-prev

# Update docker-compose.yml with previous version
# Then restart
docker-compose up -d
```

## Maintenance Windows

Schedule regular maintenance:

- **Weekly**: Database statistics update
- **Monthly**: Security patches and updates
- **Quarterly**: Major version upgrades, token rotation

## Support and Escalation

- **L1 Support**: Application logs, basic troubleshooting
- **L2 Support**: Database optimization, performance tuning
- **L3 Support**: Code debugging, infrastructure changes

## Documentation Updates

After deployment:

- [ ] Update runbooks
- [ ] Document any configuration changes
- [ ] Update disaster recovery procedures
- [ ] Update monitoring dashboards
- [ ] Document performance baselines
