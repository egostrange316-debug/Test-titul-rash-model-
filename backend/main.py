"""
========================================================================
FASTAPI RASCH MODEL 1PL (ITEM RESPONSE THEORY) BACKEND SERVICE
========================================================================
OMR Test Tizimi uchun Rasch tahlili va Supabase integratsiyasi
========================================================================
"""

import os
import logging
from typing import List, Optional, Dict, Any
from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from rasch_engine import RaschEngine

# Optional Supabase client initialization
supabase_client = None
SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_SERVICE_ROLE_KEY = os.getenv("SUPABASE_SERVICE_ROLE_KEY") or os.getenv("SUPABASE_KEY")

if SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY:
    try:
        from supabase import create_client
        supabase_client = create_client(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)
        logging.info("Supabase mijoziga muvaffaqiyatli ulandi.")
    except Exception as e:
        logging.warning(f"Supabase ulanishida ogohlantirish: {e}")

app = FastAPI(
    title="OMR Rasch IRT Psychometrics Service",
    description="OMR test natijalarini Rasch 1PL modeli asosida tahlil qilish, qiyinlik, qobiliyat va moslikni hisoblash API",
    version="1.0.0"
)

# CORS middleware for HTML/JS Frontend integration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ========================================================================
# PYDANTIC SCHEMAS
# ========================================================================
class RaschAnalyzeRequest(BaseModel):
    test_id: str = Field(..., description="Test identifikatori (masalan: FIZIKA-30-2026)", example="MATH-EXAM-01")
    matrix: List[List[int]] = Field(
        ...,
        description="N x M binar javoblar matrisasi (1 - to'g'ri, 0 - noto'g'ri)",
        example=[
            [1, 0, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1],
            [0, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0]
        ]
    )
    student_ids: List[str] = Field(
        ...,
        description="Har bir talabaning ID kodi",
        example=["7712345689", "7798765432"]
    )
    save_to_db: Optional[bool] = Field(
        default=True,
        description="Natijalarni Supabase (PostgreSQL) ma'lumotlar bazasiga avtomatik saqlash"
    )


class QuestionAnalyticsItem(BaseModel):
    question_index: int
    difficulty_b: float
    standard_error: float
    infit_msq: float
    outfit_msq: float
    status: str
    point_biserial: float
    correct_count: int
    correct_percentage: float
    flag_reason: Optional[str] = None


class StudentAbilityItem(BaseModel):
    student_id: str
    raw_score: int
    ability_theta: float
    standard_error: float
    infit_msq: float
    outfit_msq: float
    percentile_rank: float


class SessionSummary(BaseModel):
    total_students: int
    total_questions: int
    cronbach_alpha: float
    person_separation_reliability: float
    item_separation_reliability: float
    mean_student_ability: float
    mean_item_difficulty: float
    convergence_iterations: int


class RaschAnalyzeResponse(BaseModel):
    success: bool
    test_id: str
    session_id: Optional[str] = None
    saved_to_supabase: bool
    session_summary: SessionSummary
    question_analytics: List[QuestionAnalyticsItem]
    student_abilities: List[StudentAbilityItem]


# ========================================================================
# ENDPOINTS
# ========================================================================
@app.get("/")
def read_root():
    return {
        "service": "OMR Rasch IRT Psychometric API",
        "status": "online",
        "model": "1PL Rasch Model (JMLE)",
        "docs": "/docs"
    }


@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "supabase_connected": supabase_client is not None
    }


@app.post(
    "/api/v1/rasch/analyze",
    response_model=RaschAnalyzeResponse,
    status_code=status.HTTP_200_OK,
    summary="OMR test natijalari bo'yicha Rasch 1PL modelini hisoblash"
)
async def analyze_rasch(payload: RaschAnalyzeRequest):
    """
    Ushbu endpoint berilgan N x M javoblar matrisasini qabul qiladi va:
    1. Har bir savol uchun qiyinlik darajasi b_i, Infit MSQ va Outfit MSQ ko'rsatkichlarini hisoblaydi.
    2. Savol sifatini 0.7 - 1.3 oralig'iga ko'ra 'VALID' yoki 'FLAGGED' deb belgilaydi.
    3. Har bir talaba uchun latent qobiliyat theta_n va standart xatolikni hisoblaydi.
    4. Natijalarni Supabase (PostgreSQL) rasch_sessions, question_analytics va student_abilities jadvallariga saqlaydi.
    """
    # 1. Validatsiya
    matrix = payload.matrix
    student_ids = payload.student_ids
    N = len(matrix)

    if N == 0:
        raise HTTPException(status_code=400, detail="Javoblar matrisasi bo'sh bo'lishi mumkin emas!")

    M = len(matrix[0])
    for idx, row in enumerate(matrix):
        if len(row) != M:
            raise HTTPException(
                status_code=400,
                detail=f"{idx + 1}-o'quvchi javoblari soni ({len(row)}) boshqalariga ({M}) teng emas!"
            )
        for val in row:
            if val not in (0, 1):
                raise HTTPException(
                    status_code=400,
                    detail=f"Matritsa elementlari faqat 0 yoki 1 bo'lishi shart! Aniqlangan qiymat: {val}"
                )

    if N != len(student_ids):
        raise HTTPException(
            status_code=400,
            detail=f"Matritsadagi o'quvchilar soni ({N}) student_ids soniga ({len(student_ids)}) mos emas!"
        )

    # 2. Rasch 1PL JMLE hisoblash
    try:
        results = RaschEngine.estimate(matrix, student_ids)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Rasch hisoblashida xatolik: {str(e)}")

    session_summary = results["session_summary"]
    question_analytics = results["question_analytics"]
    student_abilities = results["student_abilities"]

    # 3. Supabase (PostgreSQL) ga saqlash
    saved_to_db = False
    session_id = None

    if payload.save_to_db and supabase_client is not None:
        try:
            # A. rasch_sessions jadvaliga yozish
            session_data = {
                "test_id": payload.test_id,
                "total_students": session_summary["total_students"],
                "total_questions": session_summary["total_questions"],
                "cronbach_alpha": session_summary["cronbach_alpha"],
                "person_separation_reliability": session_summary["person_separation_reliability"],
                "item_separation_reliability": session_summary["item_separation_reliability"],
                "mean_student_ability": session_summary["mean_student_ability"],
                "mean_item_difficulty": session_summary["mean_item_difficulty"],
                "convergence_iterations": session_summary["convergence_iterations"],
                "status": "COMPLETED"
            }
            res_session = supabase_client.table("rasch_sessions").insert(session_data).execute()
            if res_session.data and len(res_session.data) > 0:
                session_id = res_session.data[0]["id"]

                # B. question_analytics jadvaliga yozish
                q_rows = []
                for q in question_analytics:
                    q_rows.append({
                        "session_id": session_id,
                        "question_index": q["question_index"],
                        "difficulty_b": q["difficulty_b"],
                        "standard_error": q["standard_error"],
                        "infit_msq": q["infit_msq"],
                        "outfit_msq": q["outfit_msq"],
                        "status": q["status"],
                        "point_biserial": q["point_biserial"],
                        "correct_count": q["correct_count"],
                        "correct_percentage": q["correct_percentage"],
                        "flag_reason": q["flag_reason"]
                    })
                supabase_client.table("question_analytics").insert(q_rows).execute()

                # C. student_abilities jadvaliga yozish
                s_rows = []
                for s in student_abilities:
                    s_rows.append({
                        "session_id": session_id,
                        "student_id": s["student_id"],
                        "raw_score": s["raw_score"],
                        "ability_theta": s["ability_theta"],
                        "standard_error": s["standard_error"],
                        "infit_msq": s["infit_msq"],
                        "outfit_msq": s["outfit_msq"],
                        "percentile_rank": s["percentile_rank"]
                    })
                supabase_client.table("student_abilities").insert(s_rows).execute()
                saved_to_db = True
        except Exception as db_err:
            logging.error(f"Supabase ga saqlashda xatolik yuz berdi: {db_err}")
            saved_to_db = False

    return {
        "success": True,
        "test_id": payload.test_id,
        "session_id": session_id,
        "saved_to_supabase": saved_to_db,
        "session_summary": session_summary,
        "question_analytics": question_analytics,
        "student_abilities": student_abilities
    }
