# 🧬 GeneSphere Backend

A robust Java-based backend service for genetic data management and analysis.

## 📋 Overview

GeneSphere Backend is a microservices-based application designed to handle genetic information processing, storage, and retrieval regarding the lung cancer. Built with modern Java technologies, it provides a scalable and efficient solution for managing gene-related data with **JWT authentication** and **Redis caching** for optimal performance and security.

This project is my humble endeavor to bring meaningful change to this world—by empowering researchers, healthcare professionals, and patients with accessible genetic information. 

It will also contain podcasts and relevant information for people who fight this disease, offering hope, knowledge, and community support in their journey.

## ⚠️ Important Notice

**This application uses real, de-identified cancer genomics data from TCGA (The Cancer Genome Atlas) and other public research databases.**

- ✅ **For Research & Education:** Intended for researchers, clinicians, and students
- ❌ **Not for Clinical Use:** NOT for individual patient diagnosis or treatment decisions
- 🔒 **Privacy Compliant:** All data is de-identified per HIPAA standards
- 📚 **Cite Sources:** When publishing, cite TCGA and cBioPortal (see [DATA_SOURCES.md](DATA_SOURCES.md))

**See [DISCLAIMER](gene-service/src/main/resources/static/disclaimer.html) for full terms of use.**


## 🏗️ Architecture

This project follows a microservices architecture:

- **gene-service**: Core service handling genetic data operations with JWT auth and Redis caching
- **gene-mutation**: Mutation service (coming soon)
Other microservices will be added as well.
---

## 🔐 JWT Authentication

### Why JWT?

**Problem it solves:**
- Traditional sessions store user data on the server (memory/database lookup on every request)
- Hard to scale across multiple microservices
- Database bottleneck for high-traffic applications

**How does JWT help:**
- ✅ **Stateless**: No server-side storage needed
- ✅ **Scalable**: Any service can validate tokens independently
- ✅ **Fast**: No database lookup on every request
- ✅ **Microservice-ready**: Same token works across all services

### How does it work?

```
1. User logs in → Server validates credentials
2. Server creates JWT token (signed with secret key)
3. Client stores token and includes it in every request
4. Server validates token signature (no DB lookup needed!)
```

**Token Structure:**
```
header.payload.signature
↓
{"alg":"HS256"}.{"sub":"admin","exp":1234567890}.signature_hash
```

- **Header**: Algorithm used (HS256)
- **Payload**: User info (username, expiration) - Base64 encoded, NOT encrypted
- **Signature**: Prevents tampering (signed with secret key)

⚠️ **Security Note**: Payload is readable! Never put passwords or secrets in tokens.

### Authentication Flow

```
┌──────────┐                           ┌──────────────┐
│  Client  │ POST /auth/login          │ Auth Service │
│          ├──────────────────────────>│              │
│          │ {username, password}      │  Validates   │
│          │<──────────────────────────┤  Returns JWT │
└────┬─────┘  {token, expiresIn}       └──────────────┘
     │
     │ GET /genes (with token in header)
     │
     v
┌─────────────────┐    Validates    ┌──────────────┐
│ JWT Filter      │──────────────────>│ JWT Provider │
│ (every request) │    Signature     │              │
└────┬────────────┘                  └──────────────┘
     │
     v (if valid)
┌─────────────────┐
│ Gene Controller │
└─────────────────┘
```

### Usage Example

**1. Login to get token:**
```bash
POST /auth/login
Body: {"username":"admin","password":"admin123"}

Response:
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**2. Use token in requests:**
```bash
GET /genes/BRCA1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**3. Token validation happens automatically:**
- Extract token from `Authorization` header
- Verify signature with secret key
- Check expiration time
- Extract username from payload
- Allow/deny access

---

## 🚀 Redis Caching

### Why Redis?

**Problem it solves:**
- Database queries for gene data are slow (complex joins, large datasets)
- High traffic causes database bottleneck
- Same data requested repeatedly (e.g., popular genes like BRCA1)

**How does Redis help:**
- ✅ **In-memory storage**: 100x faster than database queries
- ✅ **Reduced DB load**: Fewer queries = better performance
- ✅ **Automatic expiration**: Old data clears automatically (TTL)
- ✅ **Scalable**: Handles thousands of requests per second

### Performance Impact

**Without Redis (Every request hits database):**
```
GET /genes/BRCA1 → Database query (200ms) → Response
GET /genes/BRCA1 → Database query (200ms) → Response
GET /genes/BRCA1 → Database query (200ms) → Response
Total: 600ms for 3 requests
```

**With Redis (First request cached):**
```
GET /genes/BRCA1 → Database query (200ms) → Cache it → Response
GET /genes/BRCA1 → Redis cache (5ms) → Response
GET /genes/BRCA1 → Redis cache (5ms) → Response
Total: 210ms for 3 requests (3x faster!)
```

### How It Works in GeneSphere

```
┌─────────┐                ┌───────┐              ┌──────────┐
│ Client  │  GET /genes    │ Redis │  Cache Miss  │ Database │
│         ├───────────────>│ Cache ├─────────────>│          │
│         │                │       │              │          │
│         │<───────────────┤       │<─────────────┤          │
└─────────┘  Return Data   └───┬───┘  Store       └──────────┘
                               │
                               │ Next request:
                               └─> Cache Hit (5ms, no DB!)
```

**Cache Strategy:**
1. **Request comes in** → Check Redis first
2. **Cache Hit** → Return data immediately (fast!)
3. **Cache Miss** → Query database → Store in Redis → Return data
4. **TTL expires** → Data automatically removed from cache

### Cache Management

**Available operations (Admin only):**
- `GET /cache/status` - Check Redis connection
- `DELETE /cache/genes` - Clear all gene cache
- `DELETE /cache/genes/{symbol}` - Clear specific gene cache

**When to clear cache:**
- After updating gene data
- When data becomes stale
- For testing/debugging

---

## 📊 Real-World Benefits

| Metric | Without Redis | With Redis | Improvement |
|--------|--------------|------------|-------------|
| Response time | 150-200ms | 3-5ms | **40x faster** |
| Database load | 100% | 5-10% | **90% reduction** |
| Requests/second | ~50 | ~2000+ | **40x throughput** |

---

## 🚀 Technologies

- **Language**: Java 17+
- **Framework**: Spring Boot
- **Build Tool**: Maven
- **Authentication**: JWT (JSON Web Tokens)
- **Caching**: Redis
- **Database**: PostgreSQL
- **Architecture**: Microservices

## 📦 Prerequisites

- Java JDK 17 or higher
- Maven 3.8+
- Docker (for Redis)
- PostgreSQL 14+
- Your preferred IDE (IntelliJ IDEA, Eclipse, VS Code)

## 🛠️ Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/Lexaa19/GeneSphere-backend.git
   cd GeneSphere-backend
   ```

2. **Start Redis**
   ```bash
   docker run -d --name redis -p 6379:6379 redis:7-alpine
   ```

3. **Configure application.properties**
   ```bash
   cd gene-service/src/main/resources
   # Edit application.properties with your database and Redis settings
   ```

4. **Build and run**
   ```bash
   cd gene-service
   mvn clean install
   mvn spring-boot:run
   ```

## 📁 Project Structure

```
GeneSphere-backend/
├── gene-service/
│   ├── src/main/java/com/gene/sphere/geneservice/
│   │   ├── config/         # Security & Redis configuration
│   │   ├── controller/     # REST API endpoints
│   │   ├── security/       # JWT provider & authentication filter
│   │   ├── service/        # Business logic
│   │   ├── model/          # Data models (User, Gene, etc.)
│   │   ├── repository/     # Database access
│   │   └── cache/          # Redis cache service
│   └── pom.xml
```

## 🔒 Security & Roles

| Role | Permissions |
|------|-------------|
| **USER** | Read genes, mutations |
| **ADMIN** | Full access + cache management + system monitoring |

## 📚 API Endpoints

### Authentication
- `POST /auth/login` - Get JWT token

### Genes (Requires Authentication)
- `GET /genes/{symbol}` - Get gene by symbol (cached)
- `GET /genes` - List all genes
- `POST /genes` - Create gene (Admin only)
- `PUT /genes/{id}` - Update gene (Admin only)
- `DELETE /genes/{id}` - Delete gene (Admin only)

### Cache Management (Admin Only)
- `GET /cache/status` - Redis connection status
- `DELETE /cache/genes` - Clear all gene cache
- `DELETE /cache/genes/{symbol}` - Clear specific gene

## 🐛 Troubleshooting

### JWT Issues

**Invalid token error:**
- Verify token format: `Authorization: Bearer <token>`
- Token may be expired (login again)
- Check if server secret key is configured

### Redis Issues

**Connection refused:**
```bash
# Check if Redis is running
docker ps | grep redis

# Start Redis if needed
docker start redis

# Test connection
redis-cli ping
```

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## 👤 Author

**Lexaa19**

- GitHub: [@Lexaa19](https://github.com/Lexaa19)

---

**Note**: This is a work in progress project. Stay tuned for updates and new features!

---

*Built with ❤️ for genetic research and education by an atomic and resltless kid*