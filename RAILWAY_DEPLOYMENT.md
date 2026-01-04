# Railway Deployment Guide for JavaNext

This guide provides step-by-step instructions to deploy JavaNext on Railway.app, a modern PaaS platform for deploying full-stack applications with integrated databases and messaging services.

## Prerequisites

- GitHub account with access to the `javanext` repository
- Railway.app account (free tier available at https://railway.app)
- Docker image built and pushed (Railway handles this automatically)

## Deployment Steps

### 1. Connect GitHub Repository to Railway

1. Log in to [Railway.app](https://railway.app)
2. Click "New Project" → "Deploy from GitHub Repo"
3. Select the `javanext` repository
4. Choose the `feature/railway-deployment` branch (or your target branch)

### 2. Configure Services

Railway will auto-detect:
- **Application Service**: Spring Boot app (from Dockerfile)
- **PostgreSQL Database**: Auto-provisioned with `railway.json`

#### Optional: Add Kafka (requires manual setup)

If you want to add Kafka:
1. In Railway dashboard → "Add Service" → search "Kafka"
2. Select Kafka template
3. Link to the main application service
4. Update `SPRING_KAFKA_BOOTSTRAP_SERVERS` in environment variables

### 3. Environment Variables

Railway automatically provides database credentials:
- `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`

Add additional variables via Railway dashboard:
1. Go to your app service → "Variables"
2. Copy variables from `.env.example`
3. Update `SPRING_PROFILES_ACTIVE=production`
4. Set `JAVA_OPTS=-Xmx512m -Xms256m` for memory constraints

### 4. Build & Deploy

1. Railway automatically detects `Dockerfile` and `railway.json`
2. First build may take 3-5 minutes (building Java 17 image)
3. Once complete, Railway assigns a public URL: `https://<your-app>.up.railway.app`

### 5. Test the Deployment

```bash
# Check health endpoint
curl https://<your-app>.up.railway.app/actuator/health

# Create a bulk order (example)
curl -X POST https://<your-app>.up.railway.app/orders/bulk \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-123" \
  -d '{
    "customerId": "cust-001",
    "items": [{"productId": "prod-001", "quantity": 5}]
  }'

# View metrics
curl https://<your-app>.up.railway.app/actuator/metrics
```

## Configuration Details

### `railway.json`

This file configures Railway deployment:
- **Build**: Uses Dockerfile for image build
- **Deploy**: 1 replica, auto-restart on failure
- **Health Check**: Monitors `/actuator/health` endpoint
- **Environment**: Production profile with 512MB heap JVM

### `.env.example`

Template for all required environment variables:
- Spring Boot configuration (profiles, app name)
- Database connection (auto-provided by Railway)
- Kafka bootstrap servers (if applicable)
- JVM memory tuning for constrained environments
- Actuator endpoints for observability

## Monitoring & Logs

### View Logs

1. Railway dashboard → "Logs" tab
2. Filter by severity, service, or time range
3. Watch for startup errors or Kafka connection issues

### Metrics

Access Prometheus metrics via:
```
https://<your-app>.up.railway.app/actuator/prometheus
```

Import into Grafana or use Railway's built-in observability.

## Troubleshooting

### Build Fails

- **Error**: `Java compilation error`
  - Ensure `pom.xml` is valid and all dependencies are correct
  - Run `mvn clean verify` locally to test

- **Error**: `Dockerfile not found`
  - Ensure `Dockerfile` exists at repo root
  - Railway looks for both `Dockerfile` and `Dockerfile.local`

### Database Connection Fails

- **Error**: `Connection refused`
  - Railway auto-provisions Postgres; wait 1-2 minutes
  - Verify `SPRING_DATASOURCE_URL` uses Railway's variables: `${{PGHOST}}`
  - Check PostgreSQL service is running in Railway dashboard

### Kafka Connection Fails

- **Error**: `Kafka bootstrap server unreachable`
  - Kafka must be manually added (not in `railway.json`)
  - Update `SPRING_KAFKA_BOOTSTRAP_SERVERS` to Railway Kafka URL
  - Ensure Kafka service is linked to app service

### Memory Issues

- **Error**: `OutOfMemoryError` or `killed` (exit code 137)
  - Reduce `JAVA_OPTS` heap size: `-Xmx256m -Xms128m`
  - Review Railway's free tier resource limits
  - Consider upgrading to paid tier for more resources

## Cost Considerations

**Free Tier (Monthly Allowance)**:
- 500 hours compute
- 5 GB persistent storage (Postgres)
- Outbound bandwidth included

**Estimated monthly costs**:
- 1 small app instance: ~$5–10
- PostgreSQL: included in free tier up to 1 GB
- Kafka: ~$10–15/month (if added)

## Next Steps

1. **Add Domain**: Railway allows custom domains; configure DNS
2. **CI/CD**: Enable auto-deploy on branch push
3. **Scaling**: Add replicas for load balancing (requires paid tier)
4. **Observability**: Integrate Datadog or New Relic (optional)
5. **Database Backup**: Set up automatic PostgreSQL snapshots

## References

- [Railway Docs](https://docs.railway.app)
- [Spring Boot on Railway](https://docs.railway.app/guides/spring-boot)
- [Dockerfile Best Practices](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
