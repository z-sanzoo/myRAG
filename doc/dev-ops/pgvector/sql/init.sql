CREATE EXTENSION IF NOT EXISTS vector;

DROP TABLE IF EXISTS public.vector_store;

CREATE TABLE public.vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    metadata JSONB,
    embedding VECTOR(1536)
);

SELECT * FROM vector_store;
