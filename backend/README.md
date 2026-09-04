# OMR Rasch 1PL Item Response Theory (IRT) Backend & Supabase

Ushbu loyiha OMR (Optical Mark Recognition) test tizimiga zamonaviy psixometrik **Rasch 1PL (Item Response Theory)** tahlilini integratsiya qilish uchun ishlab chiqilgan.

---

## 1. Nazariy Asos (Rasch 1PL Modeli)

Rasch modeli har bir savolning qiyinlik darajasi ($b_i$) va har bir talabaning latent qobiliyati ($\theta_n$) o'rtasidagi munosabatni quyidagi logistik ehtimollik formulasi orqali modellashtiradi:

$$P(X_{ni} = 1 \mid \theta_n, b_i) = \frac{\exp(\theta_n - b_i)}{1 + \exp(\theta_n - b_i)}$$

### Asosiy Ko'rsatkichlar:
1. **Savol Qiyinligi ($b_i$)**: Logit shkalasida. 0.0 — o'rtacha qiyinlik. Manfiy ($b_i < 0$) bo'lsa oson, musbat ($b_i > 0$) bo'lsa qiyin.
2. **Talaba Qobiliyati ($\theta_n$)**: Logit shkalasida. Talabaning haqiqiy bilim darajasini aks ettiradi.
3. **Standart Xatoliklar ($SE$)**: Har bir parametrning o'lchov aniqligi.
4. **Outfit Mean Square (MSQ)**: O'lchovsiz (unweighted) qoldiq kvadratlarining o'rtachasi. Tasodifiy shovqin yoki oson savolda kuchli talabaning chalg'ishini aniqlaydi.
5. **Infit Mean Square (MSQ)**: Axborot bilan og'irlangan (information-weighted) moslik statistikasi. Savol matnidagi chalkashlik yoki kalit xatoligiga o'ta sezgir.
6. **Sifat Maqomi**:
   - **VALID**: $0.7 \le Infit \le 1.3$ va $0.7 \le Outfit \le 1.3$ (Wright & Linacre qoidasi).
   - **FLAGGED**: Ushbu oraliqdan tashqaridagi savollar (qayta ko'rib chiqish yoki testdan chiqarish tavsiya etiladi).

---

## 2. PostgreSQL / Supabase Arxitekturasi

`schema.sql` faylida quyidagi jadvallar tayyorlangan:
- **`rasch_sessions`**: Har bir o'tkazilgan test sessiyasi, umumiy talabalar, savollar soni, Cronbach Alfa va Person/Item Separation ishonchliligi.
- **`question_analytics`**: Har bir savolning $b_i$, $SE$, Infit MSQ, Outfit MSQ, Point-Biserial korrelyatsiyasi va maqomi (`VALID` yoki `FLAGGED`).
- **`student_abilities`**: Har bir talabaning latent qobiliyati ($\theta_n$), standart xatosi, shaxsiy Infit/Outfit mosligi va persentil o'rni.
- **`omr_scan_results`**: Android ilova tomonidan har bir skanerlangan varaq **to'g'ridan-to'g'ri qurilmadan** (backend'ni chetlab o'tib) yozadigan jadval. `device_id` + `device_scan_id` bo'yicha upsert qilinadi, shu sababli qurilma qayta sinxronlasa ham dublikat yuzaga kelmaydi.

### O'rnatish (Supabase SQL Editor orqali):
1. Supabase Dashboard -> **SQL Editor** bo'limiga kiring.
2. `backend/schema.sql` fayli mazmunini nusxalang va **Run** tugmasini bosing.
3. Jadvallar, indekslar va RLS xavfsizlik qoidalari avtomatik shakllanadi.

### Android ilovani Supabase'ga ulash:
1. Supabase Dashboard -> **Project Settings -> API** bo'limidan `Project URL` va `anon public` kalitni oling (⚠️ `service_role` kalitini ilovaga qo'ymang — u faqat backend uchun).
2. Repo ildizidagi `.env.example` asosida `.env` fayl yarating (agar hali yo'q bo'lsa) va quyidagilarni to'ldiring:
   ```
   SUPABASE_URL=https://xxxxxxxxxxxx.supabase.co
   SUPABASE_ANON_KEY=eyJhbGciOi...
   ```
3. Ilovani qayta build qiling. `app/src/main/java/com/example/service/SupabaseSyncService.kt` har bir yangi skanerlash natijasini avtomatik ravishda (fon rejimida) `omr_scan_results` jadvaliga yuboradi; "Tarix" (History) ekranidagi bulut ikonkasi orqali qo'lda ham sinxronlash mumkin.

---

## 3. Python FastAPI Servisini Ishga Tushirish

### Lokal ishga tushirish:
```bash
cd backend
pip install -r requirements.txt
export SUPABASE_URL="https://your-project.supabase.co"
export SUPABASE_KEY="your-service-role-key"
uvicorn main:app --reload --port 8000
```

### Docker orqali ishga tushirish:
```bash
cd backend
docker build -t omr-rasch-service .
docker run -p 8000:8000 omr-rasch-service
```

---

## 4. API Foydalanish (POST /api/v1/rasch/analyze)

### So'rov yuborish (cURL):
```bash
curl -X POST "http://localhost:8000/api/v1/rasch/analyze" \
     -H "Content-Type: application/json" \
     -d '{
       "test_id": "MATEMATIKA-30-2026",
       "student_ids": ["7712345689", "7798765432", "7755512345"],
       "matrix": [
         [1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1],
         [1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0],
         [0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0]
       ],
       "save_to_db": true
     }'
```

### Javob (Response):
```json
{
  "success": true,
  "test_id": "MATEMATIKA-30-2026",
  "saved_to_supabase": true,
  "session_summary": {
    "total_students": 3,
    "total_questions": 30,
    "cronbach_alpha": 0.8421,
    "person_separation_reliability": 0.8105,
    "item_separation_reliability": 0.8652,
    "mean_student_ability": 0.0,
    "mean_item_difficulty": 0.0,
    "convergence_iterations": 12
  },
  "question_analytics": [
    {
      "question_index": 1,
      "difficulty_b": -0.8452,
      "standard_error": 0.3541,
      "infit_msq": 0.982,
      "outfit_msq": 0.945,
      "status": "VALID",
      "point_biserial": 0.582,
      "correct_count": 2,
      "correct_percentage": 66.67,
      "flag_reason": null
    }
  ],
  "student_abilities": [
    {
      "student_id": "7712345689",
      "raw_score": 19,
      "ability_theta": 0.7812,
      "standard_error": 0.412,
      "infit_msq": 1.021,
      "outfit_msq": 0.985,
      "percentile_rank": 100.0
    }
  ]
}
```
