# 🌿 Dendrite - AI Talent Knowledge Base Scaffold

<p align="center">
  <img src="docs/screenshot.png" alt="Dendrite Dashboard" width="800">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-21-orange" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20AI-1.1.2-blue" alt="Spring AI">
  <img src="https://img.shields.io/badge/pgvector-0.7-purple" alt="pgvector">
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License">
</p>

<p align="center">
  <b>The RuoYi for AI-Powered Knowledge Retrieval</b><br>
  A production-ready scaffold for building semantic search knowledge bases with AI recall capability.
</p>

---

## 🎯 What is Dendrite?

**Dendrite** (树突) is an open-source scaffold for building AI-powered knowledge retrieval systems. Like dendrites in neural networks that receive and transmit information, this system:

1. **Collects** knowledge (evaluations, documents, notes)
2. **Extracts** structured information using AI (skills, tags, summaries)
3. **Stores** as vectors for semantic search (pgvector)
4. **Retrieves** with natural language queries (AI recall)

> 💡 **Use Case**: Build a talent database, document knowledge base, or any system that needs "ask in plain language, get relevant results".

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🤖 **AI Extraction** | Auto-extract skills, tags, and summaries from raw text |
| 🔍 **Semantic Search** | Vector similarity search using pgvector |
| 💬 **AI Recall** | Ask questions in natural language, get AI-recommended results |
| 📊 **Web Dashboard** | Beautiful terminal-inspired UI, zero build required |
| ⚡ **Pipeline Processing** | Async batch processing with Redis queue |
| 💰 **Cost Control** | Token tracking, economy mode, rate limiting |
| 🏗️ **Production Ready** | Connection pooling, thread pools, health checks |

---

## 📸 Screenshots

<table>
<tr>
<td><img src="docs/dashboard.png" alt="Dashboard"></td>
<td><img src="docs/search.png" alt="Search"></td>
</tr>
<tr>
<td align="center"><b>Dashboard</b></td>
<td align="center"><b>AI Search</b></td>
</tr>
</table>

---

## 🚀 Quick Start (5 Minutes)

### Prerequisites

- ☕ Java 21+
- 🐳 Docker & Docker Compose
- 🔑 [Google Gemini API Key](https://makersuite.google.com/app/apikey)

### 1. Clone & Configure

```bash
git clone https://github.com/your-username/dendrite.git
cd dendrite

# Set your API key
export GEMINI_API_KEY=your_api_key_here
```

### 2. Start Infrastructure

```bash
docker compose up -d
# Starts PostgreSQL (with pgvector) and Redis
```

### 3. Run Application

```bash
./mvnw spring-boot:run
```

### 4. Open Browser

```
http://localhost:8080
```

🎉 **Done!** You now have a running AI knowledge base.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Web UI (SPA)                                │
│         Dashboard | Knowledge | Profiles | Search | Monitor         │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
┌────────────────────────────────┼────────────────────────────────────┐
│                           REST API                                   │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌─────────────┐ │
│  │ /evaluate    │ │ /profiles    │ │ /search      │ │ /health     │ │
│  │ /evaluate/*  │ │ /profiles/*  │ │ /ask         │ │ /stats      │ │
│  └──────┬───────┘ └──────────────┘ └──────┬───────┘ └─────────────┘ │
└─────────┼───────────────────────────────────┼───────────────────────┘
          │                                   │
          ▼                                   ▼
┌─────────────────┐              ┌─────────────────────────────────────┐
│   Redis Queue   │              │          SearchService              │
│  (Async Tasks)  │              │  ┌─────────────────────────────────┐ │
└────────┬────────┘              │  │ 1. Query Expansion (AI)        │ │
         │                       │  │ 2. Vector Search (pgvector)    │ │
         ▼                       │  │ 3. AI Recommendation           │ │
┌────────────────────────────────│  └─────────────────────────────────┘ │
│   EvaluationPipeline (5min)    └─────────────────────────────────────┘
│  ┌────────────┐ ┌────────────┐ ┌────────────┐                        
│  │ Extract    │→│ Summarize  │→│ Vectorize  │                        
│  │ (Gemini)   │ │ (Gemini)   │ │ (pgvector) │                        
│  └────────────┘ └────────────┘ └────────────┘                        
└──────────────────────┬───────────────────────                        
                       ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    PostgreSQL + pgvector                             │
│  ┌─────────────┐ ┌─────────────┐ ┌──────────────┐ ┌────────────────┐ │
│  │   Skills    │ │  Profiles   │ │    Tags      │ │  Contributors  │ │
│  │  (vector)   │ │  (vector)   │ │   (vector)   │ │    (scores)    │ │
│  └─────────────┘ └─────────────┘ └──────────────┘ └────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 📡 API Reference

### Knowledge Input

```bash
# Submit single evaluation
curl -X POST "http://localhost:8080/api/v1/gardener/evaluate?employee=Zhang" \
  -H "Content-Type: text/plain" \
  -d "Zhang is proficient in Java and Kubernetes, led the cloud migration project"

# Batch submit
curl -X POST "http://localhost:8080/api/v1/gardener/evaluate/batch" \
  -H "Content-Type: application/json" \
  -d '[{"employee":"Zhang","content":"Expert in microservices"},{"employee":"Li","content":"Frontend specialist"}]'
```

### Semantic Search

```bash
# Vector similarity search
curl "http://localhost:8080/api/v1/gardener/search?query=who%20knows%20kubernetes"

# AI recommendation
curl "http://localhost:8080/api/v1/gardener/ask?query=I%20need%20someone%20for%20database%20optimization"
```

### Profile Management

```bash
# List all profiles
curl "http://localhost:8080/api/v1/profiles"

# Get single profile
curl "http://localhost:8080/api/v1/profiles/Zhang"
```

### System Management

```bash
# Health check
curl "http://localhost:8080/api/v1/health"

# System stats
curl "http://localhost:8080/api/v1/stats"

# Queue status
curl "http://localhost:8080/api/v1/gardener/queue/status"

# Token usage report
curl "http://localhost:8080/api/v1/gardener/token/report"

# Enable economy mode
curl -X POST "http://localhost:8080/api/v1/gardener/cost-mode?economyMode=true"
```

---

## ⚙️ Configuration

### Database Index (Important for Performance)

After first run, add pgvector index for faster search:

```sql
-- Connect to PostgreSQL
docker exec -it dendrite_db psql -U myuser -d dendrite

-- Create HNSW index (recommended)
CREATE INDEX ON skill_records USING hnsw (embedding vector_cosine_ops);
CREATE INDEX ON talent_profiles USING hnsw (summary_embedding vector_cosine_ops);
```

### Concurrency Tuning

Default configuration supports **2000 concurrent search users**:

| Component | Default | Location |
|-----------|---------|----------|
| Tomcat threads | 200 | `application.yml` |
| DB connection pool | 30 | `application.yml` |
| Search thread pool | 50 | `AsyncConfig.java` |
| Rate limit (AI) | 30/min/IP | `RateLimitConfig.java` |

---

## 💰 Cost Optimization

### Model Pricing

| Model | Input | Output | Recommendation |
|-------|-------|--------|----------------|
| gemini-2.5-pro | $1.25/M | $10/M | ❌ Expensive |
| **gemini-2.0-flash** | $0.10/M | $0.40/M | ✅ Default |
| gemini-1.5-flash | $0.075/M | $0.30/M | ✅ Free tier available |

### Cost Control Features

1. **Economy Mode**: Disable query expansion (-50% AI calls)
2. **Token Tracking**: Monitor usage in real-time
3. **Rate Limiting**: 30 AI requests/minute/IP
4. **Query Caching**: Avoid duplicate AI calls

---

## 📁 Project Structure

```
dendrite/
├── src/main/java/com/carter/
│   ├── controller/       # REST APIs + Page routing
│   │   ├── GardenerController.java   # Core business API
│   │   ├── ProfileController.java    # Profile CRUD
│   │   └── HealthController.java     # Health & Stats
│   ├── service/          # Business logic
│   │   ├── SearchService.java        # Vector search + AI
│   │   ├── GardenerService.java      # Skill extraction
│   │   └── SummarizerService.java    # Profile generation
│   ├── pipeline/         # Processing pipeline
│   ├── config/           # Spring configuration
│   ├── entity/           # JPA entities
│   ├── dto/              # Request/Response DTOs
│   ├── common/           # Utils & Constants
│   └── exception/        # Error handling
├── src/main/resources/
│   ├── static/           # Frontend SPA
│   │   └── index.html    # Single-page application
│   └── application.yml   # Configuration
├── compose.yaml          # Docker infrastructure
├── pom.xml               # Maven dependencies
└── README.md             # This file
```

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| Framework | Spring Boot 3.4.0 |
| AI Integration | Spring AI 1.1.2 + Google Gemini |
| Database | PostgreSQL 16 + pgvector |
| Queue | Redis 7 |
| Frontend | Alpine.js + Tailwind CSS |
| Java | 21 (Records, Pattern Matching) |

---

## 🤝 Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Good First Issues

- [ ] Add Swagger/OpenAPI documentation
- [ ] Add unit tests for services
- [ ] Support file upload (PDF/DOCX parsing)
- [ ] Add export feature (Excel/CSV)

---

## 📄 License

MIT License - see [LICENSE](LICENSE)

---

## 🙏 Acknowledgments

- [Spring AI](https://spring.io/projects/spring-ai) - AI integration framework
- [pgvector](https://github.com/pgvector/pgvector) - Vector similarity search
- [Alpine.js](https://alpinejs.dev/) - Lightweight JavaScript framework
- [Tailwind CSS](https://tailwindcss.com/) - Utility-first CSS framework

---

<p align="center">
  <b>🌟 Star this repo if you find it useful!</b><br><br>
  Made with ❤️ by Carter
</p>
