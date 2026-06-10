# 🚀 Deployment Guide - Customer Feedback Analyzer

This guide provides step-by-step instructions for deploying the Customer Feedback Analyzer to various environments.

## 📋 Table of Contents

1. [Local Development with Docker](#local-development-with-docker)
2. [Kubernetes Deployment](#kubernetes-deployment)
3. [Production Checklist](#production-checklist)
4. [Monitoring & Observability](#monitoring--observability)
5. [Troubleshooting](#troubleshooting)

---

## 🐳 Local Development with Docker

### Prerequisites

- Docker (v20.10+)
- Docker Compose (v2.0+)
- 4GB RAM minimum

### Quick Start

1. **Clone and navigate to project**
   ```bash
   cd /path/to/NLP Customer feedback analyzer
   ```

2. **Create environment file**
   ```bash
   cat > .env << EOF
   DB_NAME=feedback_analyzer
   DB_USER=feedback_user
   DB_PASSWORD=secure_password_here
   NLP_BASE_URL=http://nlp-service:8000
   EOF
   ```

3. **Start services**
   ```bash
   docker-compose up -d
   ```

4. **Verify deployment**
   ```bash
   # Check containers are running
   docker-compose ps
   
   # Check health
   curl http://localhost:8080/actuator/health
   ```

5. **Access services**
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Database: localhost:5432

### Stopping Services

```bash
docker-compose down
docker-compose down -v  # Also remove volumes
```

---

## ☸️ Kubernetes Deployment

### Prerequisites

- kubectl (v1.24+)
- Kubernetes cluster (1.20+)
- Container registry access (Docker Hub, ECR, GCR, etc.)
- Helm (optional, for package management)

### Pre-Deployment Steps

1. **Build and push Docker image**
   ```bash
   # Build image
   docker build -t your-registry/feedback-analyzer:1.0.0 .
   
   # Push to registry
   docker push your-registry/feedback-analyzer:1.0.0
   ```

2. **Update image reference in k8s manifests**
   ```bash
   # Edit k8s/deployment.yaml
   sed -i 's|your-registry/feedback-analyzer:1.0.0|YOUR_ACTUAL_IMAGE|g' k8s/deployment.yaml
   ```

3. **Update production secrets**
   ```bash
   # Edit k8s/secret.yaml with actual credentials
   nano k8s/secret.yaml
   ```

### Step 1: Create Namespace and RBAC

```bash
kubectl apply -f k8s/namespace-rbac.yaml
```

Verify:
```bash
kubectl get namespaces | grep feedback-system
kubectl get serviceaccount -n feedback-system
```

### Step 2: Create ConfigMap and Secrets

```bash
# Create ConfigMap
kubectl apply -f k8s/configmap.yaml

# Create Secrets (IMPORTANT: Update with real values first!)
kubectl apply -f k8s/secret.yaml

# Verify
kubectl get configmap -n feedback-system
kubectl get secrets -n feedback-system
```

### Step 3: Deploy Database (PostgreSQL)

If not using a managed database:

```bash
# Create PersistentVolume (if YAML available)
kubectl apply -f k8s/postgres-pv.yaml

# Create StatefulSet/Deployment for PostgreSQL
kubectl apply -f k8s/postgres-deployment.yaml
```

Or use a managed service (AWS RDS, Azure Database, GCP Cloud SQL):
```bash
# Update DB_URL in ConfigMap to point to managed service
kubectl patch configmap feedback-analyzer-config -n feedback-system \
  --type merge -p '{"data":{"db.url":"jdbc:postgresql://your-managed-db:5432/feedback_analyzer"}}'
```

### Step 4: Deploy Application

```bash
# Deploy application
kubectl apply -f k8s/deployment.yaml

# Create service
kubectl apply -f k8s/service.yaml

# Verify deployment
kubectl get deployment -n feedback-system
kubectl get pods -n feedback-system
kubectl get svc -n feedback-system
```

### Step 5: Setup Autoscaling

```bash
# Apply HPA
kubectl apply -f k8s/hpa.yaml

# Monitor scaling
kubectl get hpa -n feedback-system -w
```

### Step 6: Setup Ingress

```bash
# Install ingress controller (if not already installed)
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml

# Create ingress
kubectl apply -f k8s/ingress.yaml

# Verify ingress
kubectl get ingress -n feedback-system
```

### Step 7: Setup TLS/SSL Certificates

Using cert-manager with Let's Encrypt:

```bash
# Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Create ClusterIssuer
kubectl apply -f - << EOF
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: you@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF
```

---

## ✅ Production Checklist

Before deploying to production, ensure:

### Security
- [ ] All secrets are managed securely (not in git/manifests)
- [ ] Database credentials are strong (20+ characters, mixed case, numbers, symbols)
- [ ] TLS/SSL certificates are valid
- [ ] CORS is configured correctly
- [ ] API rate limiting is enabled (Resilience4j)
- [ ] Authentication & authorization implemented
- [ ] Ingress is HTTPS-only

### Configuration
- [ ] SPRING_PROFILES_ACTIVE=prod
- [ ] DDL_AUTO=validate (not create-drop)
- [ ] Logging level set appropriately
- [ ] Database connection pooling configured
- [ ] Circuit breaker thresholds tuned

### Database
- [ ] PostgreSQL 12+ (not H2)
- [ ] Automated backups enabled
- [ ] Read replicas configured (if needed)
- [ ] Connection pooling configured (HikariCP: 20-30 connections)
- [ ] Flyway migrations applied

### Monitoring
- [ ] Prometheus metrics exposed (/actuator/metrics)
- [ ] Centralized logging setup (ELK/Splunk/Datadog)
- [ ] Alerting rules configured
- [ ] APM tool integrated (DataDog, New Relic, etc.)
- [ ] Health check endpoints monitored

### Performance
- [ ] Load testing completed
- [ ] Caching strategy implemented (Redis)
- [ ] Database queries optimized
- [ ] API response time targets met
- [ ] Batch processing optimized

### Reliability
- [ ] Circuit breaker tested
- [ ] Retry logic verified
- [ ] Failover tested
- [ ] Disaster recovery plan documented
- [ ] Multi-region deployment (if required)

---

## 📊 Monitoring & Observability

### Health Check Endpoints

```bash
# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Readiness probe
curl http://localhost:8080/actuator/health/readiness

# Full health check
curl http://localhost:8080/actuator/health

# Database health
curl http://localhost:8080/actuator/health/db

# NLP service health
curl http://localhost:8080/actuator/health/NlpServiceHealthIndicator
```

### Metrics

```bash
# All available metrics
curl http://localhost:8080/actuator/metrics

# HTTP requests
curl http://localhost:8080/actuator/metrics/http.server.requests

# Circuit breaker status
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state

# Database connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections
```

### Prometheus Integration

```yaml
# Add to prometheus.yml
scrape_configs:
  - job_name: 'feedback-analyzer'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['feedback-analyzer:8080']
```

### Log Aggregation

```bash
# View logs from pod
kubectl logs -n feedback-system deployment/feedback-analyzer

# Stream logs
kubectl logs -n feedback-system deployment/feedback-analyzer -f

# Tail last 100 lines
kubectl logs -n feedback-system deployment/feedback-analyzer --tail=100
```

---

## 🔧 Troubleshooting

### Common Issues

#### 1. Pods not starting

```bash
# Check pod events
kubectl describe pod <pod-name> -n feedback-system

# Check logs
kubectl logs <pod-name> -n feedback-system

# Common causes:
# - Image not found: Update image reference in deployment.yaml
# - Resource quota exceeded: Check cluster capacity
# - Secret/ConfigMap missing: Apply k8s/secret.yaml and k8s/configmap.yaml
```

#### 2. Database connection failed

```bash
# Test database connectivity from pod
kubectl exec -it <pod-name> -n feedback-system -- \
  sh -c 'apt-get update && apt-get install -y postgresql-client && \
  psql -h feedback-postgres-service -U feedback_user -d feedback_analyzer'

# Check database credentials in Secret
kubectl get secret feedback-analyzer-secrets -n feedback-system -o yaml
```

#### 3. High CPU/Memory usage

```bash
# Check resource usage
kubectl top pod <pod-name> -n feedback-system

# Check HPA autoscaling
kubectl get hpa -n feedback-system -w

# Adjust resource limits
kubectl set resources deployment feedback-analyzer \
  -n feedback-system \
  --limits=memory=1Gi,cpu=1000m \
  --requests=memory=512Mi,cpu=500m
```

#### 4. Circuit breaker open (NLP service unavailable)

```bash
# Check metrics
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state

# Check NLP service health
kubectl logs -n feedback-system deployment/nlp-service

# Reset circuit breaker (if available through admin endpoint)
curl -X POST http://localhost:8080/admin/circuitbreaker/reset
```

#### 5. Slow API responses

```bash
# Check request latency
curl http://localhost:8080/actuator/metrics/http.server.requests

# Check database slow queries
kubectl exec -it postgres-pod -- \
  psql -U feedback_user -d feedback_analyzer \
  -c "SELECT * FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"

# Analyze query plans
EXPLAIN ANALYZE SELECT * FROM feedbacks WHERE sentiment='NEGATIVE';
```

### Scaling Down / Cleanup

```bash
# Delete all resources
kubectl delete -f k8s/

# Delete namespace (and all resources in it)
kubectl delete namespace feedback-system

# Verify cleanup
kubectl get po -n feedback-system  # Should be empty
```

---

## 🆘 Getting Help

- **Documentation**: `README.md`, `pom.xml` (dependencies)
- **API Docs**: http://localhost:8080/swagger-ui.html
- **Logs**: Check pod logs with kubectl
- **GitHub Issues**: Report issues with reproduction steps

---

## 📚 Additional Resources

- [Spring Boot Deployment](https://spring.io/guides/gs/spring-boot/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Docker Documentation](https://docs.docker.com/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)

---

**Last Updated:** June 2026
**Maintainer:** Maze Tesfa

