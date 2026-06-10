# 🚀 Quick Reference Guide - Production Features

## 📌 Table of Contents
1. [Local Development](#local-development)
2. [Docker Deployment](#docker-deployment)
3. [Kubernetes Deployment](#kubernetes-deployment)
4. [Monitoring APIs](#monitoring-apis)
5. [Troubleshooting](#troubleshooting)

---

## 🏠 Local Development

### Start Local Environment
```bash
# Navigate to project
cd ~/IdeaProjects/NLP\ Customer\ feedback\ analyzer

# Create .env file
cat > .env << EOF
DB_NAME=feedback_analyzer
DB_USER=feedback_user
DB_PASSWORD=secure_password
NLP_BASE_URL=http://nlp-service:8000
EOF

# Start services
docker-compose up -d

# Verify
docker-compose ps
```

### Access Services
```
API:        http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui.html
Database:   localhost:5432
```

### Stop Services
```bash
docker-compose down          # Keep data
docker-compose down -v       # Remove data
```

---

## 🐳 Docker Deployment

### Build Image
```bash
docker build -t feedback-analyzer:1.0.0 .
docker tag feedback-analyzer:1.0.0 your-registry/feedback-analyzer:1.0.0
docker push your-registry/feedback-analyzer:1.0.0
```

### Run Container
```bash
docker run -d \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://postgres:5432/feedback_analyzer \
  -e DB_USER=feedback_user \
  -e DB_PASSWORD=secure_password \
  -p 8080:8080 \
  feedback-analyzer:1.0.0
```

### Check Health
```bash
curl http://localhost:8080/actuator/health
```

---

## ☸️ Kubernetes Deployment

### Full Deployment (Recommended)
```bash
# 1. Setup namespace and RBAC
kubectl apply -f k8s/namespace-rbac.yaml

# 2. Create configuration
kubectl apply -f k8s/configmap.yaml

# 3. Create secrets (EDIT FIRST!)
nano k8s/secret.yaml
kubectl apply -f k8s/secret.yaml

# 4. Deploy application
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# 5. Setup autoscaling
kubectl apply -f k8s/hpa.yaml

# 6. Setup ingress (requires ingress-nginx controller)
kubectl apply -f k8s/ingress.yaml
```

### Check Deployment Status
```bash
# Check pods
kubectl get pods -n feedback-system
kubectl describe pod <pod-name> -n feedback-system

# Check deployment
kubectl get deployment -n feedback-system
kubectl rollout status deployment/feedback-analyzer -n feedback-system

# Check service
kubectl get svc -n feedback-system

# Check HPA
kubectl get hpa -n feedback-system
```

### View Logs
```bash
# Latest logs
kubectl logs -n feedback-system deployment/feedback-analyzer

# Stream logs
kubectl logs -n feedback-system deployment/feedback-analyzer -f

# Logs from specific pod
kubectl logs -n feedback-system <pod-name>
```

### Scale Manually
```bash
# Scale to 5 replicas
kubectl scale deployment feedback-analyzer --replicas=5 -n feedback-system

# View current replicas
kubectl get deployment feedback-analyzer -n feedback-system
```

---

## 📊 Monitoring APIs

### Health Checks
```bash
# Overall health
curl http://localhost:8080/actuator/health

# Kubernetes liveness probe
curl http://localhost:8080/actuator/health/liveness

# Kubernetes readiness probe
curl http://localhost:8080/actuator/health/readiness

# Database health
curl http://localhost:8080/actuator/health/db

# NLP service health
curl http://localhost:8080/actuator/health/NlpServiceHealthIndicator
```

### Metrics
```bash
# All available metrics
curl http://localhost:8080/actuator/metrics | jq .

# HTTP server requests
curl http://localhost:8080/actuator/metrics/http.server.requests | jq .

# Circuit breaker state
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state | jq .

# Database connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections | jq .

# Prometheus format (for scraping)
curl http://localhost:8080/actuator/metrics/prometheus
```

### API Endpoints
```bash
# Single feedback analysis
curl -X POST http://localhost:8080/api/v1/feedback/analyze \
  -H "Content-Type: application/json" \
  -d '{"text":"Product is great!","feedbackType":"PRODUCT_REVIEW","source":"website"}'

# Batch analysis (async)
curl -X POST http://localhost:8080/api/v1/feedback/analyze/batch \
  -H "Content-Type: application/json" \
  -d '{
    "feedbacks":[
      {"text":"Great product","feedbackType":"PRODUCT_REVIEW","source":"website"},
      {"text":"Poor service","feedbackType":"SUPPORT_TICKET","source":"email"}
    ]
  }'

# Get all feedbacks (paginated)
curl "http://localhost:8080/api/v1/feedback?page=0&size=20"

# Get feedback by ID
curl http://localhost:8080/api/v1/feedback/1

# Get insights
curl http://localhost:8080/api/v1/feedback/insights

# Filter by sentiment
curl "http://localhost:8080/api/v1/feedback/sentiment/POSITIVE?page=0&size=20"

# Filter by type
curl "http://localhost:8080/api/v1/feedback/type/PRODUCT_REVIEW?page=0&size=20"
```

---

## 🔧 Troubleshooting

### Pods Not Starting

```bash
# Check pod status
kubectl describe pod <pod-name> -n feedback-system

# Common fixes:
# 1. Image not found
kubectl set image deployment/feedback-analyzer \
  feedback-analyzer=your-registry/feedback-analyzer:1.0.0 \
  -n feedback-system

# 2. Check events
kubectl get events -n feedback-system --sort-by='.lastTimestamp'

# 3. Check configuration
kubectl get configmap -n feedback-system
kubectl describe configmap feedback-analyzer-config -n feedback-system
```

### Database Connection Issues

```bash
# Test from pod
kubectl exec -it <pod-name> -n feedback-system -- bash

# Inside pod, test connection
apt-get update && apt-get install -y postgresql-client
psql -h feedback-postgres-service -U feedback_user -d feedback_analyzer

# Check secrets
kubectl get secret feedback-analyzer-secrets -n feedback-system -o yaml
```

### Circuit Breaker Open

```bash
# Check metrics
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state

# View detailed state
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state | jq '.measurements'

# Wait for automatic recovery (60 seconds default)
# Or check NLP service health and fix the issue
```

### High Resource Usage

```bash
# Check resource consumption
kubectl top pod <pod-name> -n feedback-system

# Check HPA status
kubectl get hpa -n feedback-system -w

# View HPA events
kubectl describe hpa feedback-analyzer-hpa -n feedback-system

# Manually adjust resource limits
kubectl set resources deployment feedback-analyzer \
  --limits=memory=1Gi,cpu=1000m \
  --requests=memory=512Mi,cpu=500m \
  -n feedback-system
```

### Access Denied / RBAC Issues

```bash
# Check RBAC
kubectl get rolebinding -n feedback-system
kubectl describe role feedback-analyzer-role -n feedback-system

# Check service account
kubectl get sa -n feedback-system
kubectl describe sa feedback-analyzer -n feedback-system
```

### Slow API Responses

```bash
# Check request latency
curl http://localhost:8080/actuator/metrics/http.server.requests | jq '.measurements'

# Check database performance
kubectl exec -it postgres-pod -- psql -U feedback_user -d feedback_analyzer
SELECT * FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;

# Check if batch processing is running asynchronously
# Look for "ANALYZING" status feedbacks in database
```

---

## 📈 Performance Tuning

### Database Connection Pool (HikariCP)
Located in `application-prod.yml`:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20    # Increase if high concurrency
      minimum-idle: 5          # Idle connections kept
      connection-timeout: 30s  # Wait time for connection
```

### Async Thread Pool
Located in `application.yml`:
```yaml
spring:
  task:
    execution:
      pool:
        core-size: 2        # Min threads
        max-size: 10        # Max threads
        queue-capacity: 100 # Queue size
```

### Circuit Breaker Thresholds
Located in `application-prod.yml`:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      nlpService:
        failureRateThreshold: 50        # Fail if 50% requests fail
        slidingWindowSize: 100          # Evaluate last 100 calls
        waitDurationInOpenState: 60000  # Wait 60s before retry
```

---

## 🔒 Security Checklist

Before production deployment:

```bash
# ✅ Update secrets
kubectl set secret generic feedback-analyzer-secrets \
  --from-literal=db.password=YOUR_STRONG_PASSWORD \
  -n feedback-system

# ✅ Update ingress
# Edit k8s/ingress.yaml with your domain name
# Ensure HTTPS is configured

# ✅ Test rate limiting
for i in {1..150}; do 
  curl -s http://localhost:8080/api/v1/feedback/insights > /dev/null
  echo "Request $i"
done

# ✅ Check audit logs
kubectl exec -it postgres-pod -- psql -U feedback_user -d feedback_analyzer
SELECT * FROM audit_logs LIMIT 10;

# ✅ Verify CORS
curl -H "Origin: http://unauthorized-domain.com" http://localhost:8080/api/v1/feedback
```

---

## 📞 Support Resources

- **Documentation:** `DEPLOYMENT.md`
- **Implementation Summary:** `IMPLEMENTATION_SUMMARY.md`
- **Main README:** `README.md`
- **API Docs:** http://localhost:8080/swagger-ui.html
- **Configuration:** `application-prod.yml`

---

**Version:** 1.0.0  
**Last Updated:** June 10, 2026  
**Maintainer:** Maze Tesfa

