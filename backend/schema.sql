-- ========================================================================
-- RASCH MODEL (1PL IRT) POSTGRESQL / SUPABASE DATABASE SCHEMA
-- OMR Test Tekshiruv Tizimi va Psixometrik Tahlil Arxitekturasi
-- ========================================================================

-- Enable UUID extension if not enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. RASCH_SESSIONS
-- Tahlil sessiyalari haqidagi metama'lumotlar va umumiy test ishonchliligi
CREATE TABLE IF NOT EXISTS rasch_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    total_students INT NOT NULL CHECK (total_students > 0),
    total_questions INT NOT NULL CHECK (total_questions > 0),
    cronbach_alpha NUMERIC(5, 4),
    person_separation_reliability NUMERIC(5, 4),
    item_separation_reliability NUMERIC(5, 4),
    mean_student_ability NUMERIC(7, 4) DEFAULT 0.0,
    mean_item_difficulty NUMERIC(7, 4) DEFAULT 0.0,
    convergence_iterations INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED'
);

-- 2. QUESTION_ANALYTICS
-- Har bir savolning Rasch ko'rsatkichlari (qiyinlik, infit, outfit va sifat maqomi)
CREATE TABLE IF NOT EXISTS question_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES rasch_sessions(id) ON DELETE CASCADE,
    question_index INT NOT NULL CHECK (question_index >= 1),
    difficulty_b NUMERIC(7, 4) NOT NULL,          -- Savol qiyinlik darajasi (logit)
    standard_error NUMERIC(7, 4) NOT NULL,        -- Standart xatolik SE(b)
    infit_msq NUMERIC(6, 3) NOT NULL,             -- Information-weighted Mean Square
    outfit_msq NUMERIC(6, 3) NOT NULL,            -- Unweighted Mean Square
    status VARCHAR(10) NOT NULL CHECK (status IN ('VALID', 'FLAGGED')),
    point_biserial NUMERIC(6, 3),                 -- Diskriminatsiya (klassik test nazariyasi)
    correct_count INT DEFAULT 0,
    correct_percentage NUMERIC(5, 2) DEFAULT 0.0,
    flag_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(session_id, question_index)
);

-- 3. STUDENT_ABILITIES
-- Har bir talabaning latent qobiliyati (theta) va o'lchov aniqligi
CREATE TABLE IF NOT EXISTS student_abilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES rasch_sessions(id) ON DELETE CASCADE,
    student_id VARCHAR(50) NOT NULL,              -- Talabaning 10 xonali OMR kodi yoki ID
    raw_score INT NOT NULL DEFAULT 0,             -- To'g'ri javoblar soni (0..M)
    ability_theta NUMERIC(7, 4) NOT NULL,         -- Talaba qobiliyati theta (logit)
    standard_error NUMERIC(7, 4) NOT NULL,        -- Standart xatolik SE(theta)
    infit_msq NUMERIC(6, 3),                      -- Talaba javoblarining infit mosligi
    outfit_msq NUMERIC(6, 3),                     -- Talaba javoblarining outfit mosligi
    percentile_rank NUMERIC(5, 2),                -- Guruh ichidagi foizli o'rni
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(session_id, student_id)
);

-- ========================================================================
-- INDEXES FOR HIGH-SPEED LOOKUP
-- ========================================================================
CREATE INDEX IF NOT EXISTS idx_rasch_sessions_test_id ON rasch_sessions(test_id);
CREATE INDEX IF NOT EXISTS idx_rasch_sessions_created_at ON rasch_sessions(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_question_analytics_session_id ON question_analytics(session_id);
CREATE INDEX IF NOT EXISTS idx_question_analytics_status ON question_analytics(status);
CREATE INDEX IF NOT EXISTS idx_question_analytics_difficulty ON question_analytics(difficulty_b);

CREATE INDEX IF NOT EXISTS idx_student_abilities_session_id ON student_abilities(session_id);
CREATE INDEX IF NOT EXISTS idx_student_abilities_student_id ON student_abilities(student_id);
CREATE INDEX IF NOT EXISTS idx_student_abilities_ability ON student_abilities(ability_theta DESC);

-- ========================================================================
-- ROW LEVEL SECURITY (RLS) FOR SUPABASE
-- ========================================================================
ALTER TABLE rasch_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE question_analytics ENABLE ROW LEVEL SECURITY;
ALTER TABLE student_abilities ENABLE ROW LEVEL SECURITY;

-- Allow read access to authenticated users and anon (if public dashboard is enabled)
CREATE POLICY "Public read for rasch_sessions" ON rasch_sessions FOR SELECT USING (true);
CREATE POLICY "Public insert for rasch_sessions" ON rasch_sessions FOR INSERT WITH CHECK (true);

CREATE POLICY "Public read for question_analytics" ON question_analytics FOR SELECT USING (true);
CREATE POLICY "Public insert for question_analytics" ON question_analytics FOR INSERT WITH CHECK (true);

CREATE POLICY "Public read for student_abilities" ON student_abilities FOR SELECT USING (true);
CREATE POLICY "Public insert for student_abilities" ON student_abilities FOR INSERT WITH CHECK (true);

-- ========================================================================
-- HELPER VIEW: ITEM DIFFICULTY SPECTRUM (WRIGHT MAP SUMMARY)
-- ========================================================================
CREATE OR REPLACE VIEW v_rasch_item_summary AS
SELECT 
    s.test_id,
    s.id AS session_id,
    q.question_index,
    q.difficulty_b,
    q.infit_msq,
    q.outfit_msq,
    q.status,
    q.correct_percentage,
    CASE 
        WHEN q.difficulty_b < -1.0 THEN 'Juda Oson'
        WHEN q.difficulty_b BETWEEN -1.0 AND 0.0 THEN 'Oson'
        WHEN q.difficulty_b BETWEEN 0.0 AND 1.0 THEN 'O''rtacha'
        WHEN q.difficulty_b BETWEEN 1.0 AND 2.0 THEN 'Qiyin'
        ELSE 'Juda Qiyin'
    END AS difficulty_category
FROM question_analytics q
JOIN rasch_sessions s ON s.id = q.session_id
ORDER BY q.difficulty_b ASC;
