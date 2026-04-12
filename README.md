# Rate Limiter Service

A high-performance, distributed rate limiting service built with Spring Boot and Redis. Supports multiple rate limiting algorithms with both local and Redis-based implementations.

## 🚀 Features

- **Multiple Algorithms**: Token Bucket and Sliding Window rate limiting algorithms
- **Distributed Support**: Redis-based implementation for distributed systems
- **Local Fallback**: In-memory implementation for single-instance deployments
- **RESTful API**: Clean REST API with OpenAPI/Swagger documentation
- **Monitoring**: Prometheus metrics and Spring Boot Actuator endpoints
- **Docker Support**: Ready-to-use Docker and Docker Compose configuration
- **Health Checks**: Built-in health monitoring for Redis connectivity

## 📋 Requirements

- Java 17+
- Maven 3.6+
- Redis 6.0+ (for distributed mode)
- Docker & Docker Compose (optional)

## 🛠️ Quick Start

### Using Docker Compose (Recommended)

```bash
git clone https://github.com/rohithlovescode/rate-limiter.git
cd rate-limiter
docker-compose up -d
```

The service will be available at `http://localhost:8080`

### Local Development

1. **Start Redis** (if using distributed mode):
```bash
docker run -d -p 6379:6379 redis:7-alpine
```

2. **Build and run the application**:
```bash
mvn clean install
mvn spring-boot:run
```

## 📚 API Documentation

Once the service is running, visit:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## 🔧 API Endpoints

### Check Rate Limit
```http
POST /api/v1/rate-limit/check
Content-Type: application/json

{
  "clientId": "user123",
  "algorithm": "TOKEN_BUCKET",
  "limit": 100,
  "windowMs": 60000,
  "capacity": 100,
  "refillRate": 10,
  "fallbackStrategy": "LOCAL"
}
```

**Response**:
- `200 OK` - Request allowed
- `429 Too Many Requests` - Rate limit exceeded

**Headers**:
- `X-RateLimit-Limit` - Maximum requests allowed
- `X-RateLimit-Remaining` - Remaining requests
- `X-RateLimit-Algorithm` - Algorithm used
- `X-RateLimit-Fallback` - Whether fallback was used
- `Retry-After` - Seconds to wait before retry

### Get Status (Read-only)
```http
GET /api/v1/rate-limit/status/{clientId}?algorithm=TOKEN_BUCKET&limit=100&windowMs=60000
```

### Reset Client
```http
DELETE /api/v1/rate-limit/reset/{clientId}
```

## ⚙️ Configuration

### Application Properties

```yaml
# Redis Configuration
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

# Rate Limiter Configuration
ratelimiter:
  algorithm: SLIDING_WINDOW
  default-limit: 100
  default-window-ms: 60000
  fallback-strategy: LOCAL
  redis:
    key-prefix: "rate-limit:"
    script-timeout: 5000

# Management Endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

## 🎯 Rate Limiting Algorithms

### Token Bucket
- **Description**: Allows bursts up to capacity, then refills at a constant rate
- **Use Case**: APIs that need to handle traffic spikes
- **Parameters**:
  - `capacity`: Maximum tokens in bucket
  - `refillRate`: Tokens added per second

### Sliding Window
- **Description**: Counts requests within a sliding time window
- **Use Case**: Strict rate limiting with precise time windows
- **Parameters**:
  - `limit`: Maximum requests per window
  - `windowMs`: Window size in milliseconds

## 📊 Monitoring

### Prometheus Metrics
Access metrics at: `http://localhost:8080/actuator/prometheus`

Key metrics:
- `rate_limit_requests_total` - Total rate limit checks
- `rate_limit_allowed_total` - Allowed requests
- `rate_limit_blocked_total` - Blocked requests
- `rate_limit_latency_seconds` - Request processing time

### Health Checks
- **Application Health**: `http://localhost:8080/actuator/health`
- **Redis Health**: Included in application health check

## 🧪 Testing

Run the test suite:
```bash
mvn test
```

Run with integration tests:
```bash
mvn test -P integration-test
```

## 📦 Building

Create executable JAR:
```bash
mvn clean package
```

Build Docker image:
```bash
mvn clean package
docker build -t rate-limiter:latest .
```

## 🔧 Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `REDIS_HOST` | localhost | Redis server host |
| `REDIS_PORT` | 6379 | Redis server port |
| `SPRING_PROFILES_ACTIVE` | default | Active Spring profile |
| `RATELIMITER_DEFAULT_LIMIT` | 100 | Default rate limit |
| `RATELIMITER_DEFAULT_WINDOW_MS` | 60000 | Default window size |

## 🚀 Deployment

### Kubernetes
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rate-limiter
spec:
  replicas: 3
  selector:
    matchLabels:
      app: rate-limiter
  template:
    metadata:
      labels:
        app: rate-limiter
    spec:
      containers:
      - name: rate-limiter
        image: rate-limiter:latest
        ports:
        - containerPort: 8080
        env:
        - name: REDIS_HOST
          value: "redis-service"
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Links

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Redis Documentation](https://redis.io/documentation)
- [Docker Documentation](https://docs.docker.com/)

## 📈 Performance

- **Throughput**: 10,000+ requests/second per instance
- **Latency**: <1ms average response time
- **Memory**: ~100MB per instance
- **Redis**: Handles millions of keys efficiently

## 🐛 Troubleshooting

### Common Issues

1. **Redis Connection Failed**
   - Check Redis is running: `redis-cli ping`
   - Verify connection settings in `application.yml`

2. **Rate Limit Not Working**
   - Ensure clientId is consistent across requests
   - Check algorithm parameters are valid

3. **High Memory Usage**
   - Monitor Redis memory usage
   - Consider TTL settings for rate limit keys

### Logs

Enable debug logging:
```yaml
logging:
  level:
    com.ratelimiter: DEBUG
    org.springframework.data.redis: DEBUG
```
### Screenshots
<img width="1889" height="478" alt="image" src="https://github.com/user-attachments/assets/9c414cbb-d61f-4a45-8ac7-fa81355ba8c0" />
<img width="1874" height="841" alt="image" src="https://github.com/user-attachments/assets/a0ea6add-a4f0-495e-9907-3db405be99bf" />

