-- CareerForge AI — PostgreSQL + pgvector Initialization Script
-- This runs automatically on first docker-compose up

-- 1. Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Resume Embeddings Table (for semantic resume-to-job matching & recruiter search)
CREATE TABLE IF NOT EXISTS resume_embeddings (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT,
    resume_hash     VARCHAR(64) NOT NULL UNIQUE,
    embedding       vector(768),            -- Gemini text-embedding-004 dim
    resume_snippet  TEXT,                   -- First 500 chars for display
    candidate_name  VARCHAR(255),
    target_role     VARCHAR(255),
    skills_json     TEXT,                   -- JSON array of extracted skills
    ats_score       FLOAT,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_resume_embeddings_user_id     ON resume_embeddings(user_id);
CREATE INDEX IF NOT EXISTS idx_resume_embeddings_resume_hash ON resume_embeddings(resume_hash);
-- pgvector HNSW index for fast ANN search
CREATE INDEX IF NOT EXISTS idx_resume_embeddings_vector
    ON resume_embeddings USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 3. Job Embeddings Table (for persisting & ranking live job postings)
CREATE TABLE IF NOT EXISTS job_embeddings (
    id              VARCHAR(255) PRIMARY KEY,
    title           TEXT,
    company         TEXT,
    description     TEXT,
    location        TEXT,
    url             TEXT,
    salary          TEXT,
    source          TEXT,
    required_skills TEXT,
    semantic_score  FLOAT,
    embedding       vector(768),
    indexed_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_job_embeddings_vector
    ON job_embeddings USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 4. Log completion
DO $$
BEGIN
    RAISE NOTICE 'CareerForge AI: pgvector initialized successfully. Tables: resume_embeddings, job_embeddings.';
END $$;
