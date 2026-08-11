# VREZER — AI Resume Analyzer & Career Intelligence Platform

VREZER is an enterprise-grade, multi-agent AI resume intelligence platform built with **Spring Boot 3 (Java 17)**, **PostgreSQL / H2**, and **Vite + React 18 + TypeScript**. It extracts candidate attributes strictly from resume evidence, computes deterministic 11-dimension ATS scores, retrieves live job market postings, matches candidate skill matrices against target job roles, and provides personalized career trajectory forecasts.

---

## 🌟 Architecture Overview

```text
               ┌──────────────────────────────────────────────┐
               │    Frontend (Vite + React + TypeScript)      │
               └──────────────────────┬───────────────────────┘
                                      │ REST API / CORS
                                      ▼
               ┌──────────────────────────────────────────────┐
               │  Spring Boot Backend (Port 7000 / Actuator)  │
               └──────┬────────────────┬───────────────┬──────┘
                      │                │               │
                      ▼                ▼               ▼
               ┌────────────┐   ┌────────────┐   ┌────────────┐
               │ PostgreSQL │   │  AI Engine │   │  Live Job  │
               │ / H2 DB    │   │  (Gemini)  │   │  APIs      │
               └────────────┘   └────────────┘   └────────────┘
```

---

## ✨ Features

- **Multi-Format Text Extraction**: High-fidelity parsing for PDF, DOCX, ODT, RTF, and plain text using Apache Tika & PDFBox.
- **Dynamic Candidate Profiling**: Domain detection across 24+ technical and non-technical fields (Software Engineering, Data Science, Marketing, Finance, Healthcare, DevOps, etc.).
- **Deterministic 11-Dimension ATS Engine**: Explainable scoring across Keyword Match, Experience Relevance, Technical Skills, Education, Projects, and formatting markers.
- **Live Job Retrieval & Ranking**: Integrates live job market search (Adzuna, Remotive, Jooble) with similarity matching and deduplication.
- **Hybrid AI & Local Fallback Engine**: Uses Gemini / OpenAI / Groq AI models when keys are configured, gracefully falling back to a deterministic local NLP analysis engine when offline.
- **Production Health Checks**: Native `/actuator/health` endpoint for automated deployment orchestration.

---

## 🛠 Tech Stack

- **Backend**: Java 17, Spring Boot 3.2.3, Spring Data JPA, Spring Security, Spring Actuator
- **Database**: PostgreSQL (Production) / H2 In-Memory (Local Development)
- **Document Parsing**: Apache Tika 2.9.1, Apache PDFBox 2.0.31, Apache POI 5.2.5
- **Frontend**: React 18, TypeScript 5, Vite 5, TailwindCSS 3, Lucide Icons, Recharts
- **Deployment**: Render (Spring Boot Backend), Vercel (React Frontend)

---

## 🔑 Environment Variables Configuration

Copy `.env.example` to `.env` or set environment variables in your deployment environment:

| Variable | Description | Default |
| :--- | :--- | :--- |
| `PORT` | Backend server port | `7000` |
| `SPRING_DATASOURCE_URL` | Database connection URL | `jdbc:h2:mem:vrezer_db` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `sa` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `""` |
| `CORS_ALLOWED_ORIGINS` | Permitted frontend origins | `http://localhost:5173` |
| `GEMINI_API_KEY` | Google Gemini AI Key | *(Optional)* |
| `OPENAI_API_KEY` | OpenAI GPT Key | *(Optional)* |
| `ADZUNA_APP_ID` | Adzuna Job API App ID | *(Optional)* |
| `ADZUNA_APP_KEY` | Adzuna Job API App Key | *(Optional)* |
| `VITE_API_URL` | Frontend API Target Endpoint | `http://localhost:7000` |

---

## 🚀 Local Development Setup

### 1. Backend Setup (Spring Boot)
```bash
cd backend
mvn clean package -DskipTests
mvn spring-boot:run
```
The backend starts at `http://localhost:7000`. Verify health at `http://localhost:7000/actuator/health`.

### 2. Frontend Setup (Vite + React)
```bash
cd frontend
npm install
npm run dev
```
The frontend dev server runs at `http://localhost:5173`.

---

## 🧪 Testing & Verification

Run the full integration test suite covering 5 candidate domain profiles (Java Developer, Python Data Scientist, React Engineer, Performance Marketer, DevOps Architect):

```bash
cd backend
mvn test
```

---

## 🌐 Production Deployment Guide

### Backend Deployment (Render)
1. Link your repository to Render as a **Web Service**.
2. Select **Docker** or **Java** environment using `render.yaml`.
3. Set `healthCheckPath` to `/actuator/health`.
4. Configure standard environment variables (`GEMINI_API_KEY`, `SPRING_DATASOURCE_URL`, etc.).

### Frontend Deployment (Vercel)
1. Import repository into Vercel and set Root Directory to `frontend`.
2. Framework Preset: **Vite**.
3. Build Command: `npm run build`.
4. Output Directory: `dist`.
5. Set `VITE_API_URL` environment variable to your deployed backend URL.

---

## 📄 License
MIT License. Built for production candidate intelligence and market matching.
