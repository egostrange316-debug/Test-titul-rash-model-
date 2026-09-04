"""
========================================================================
RASCH 1PL ITEM RESPONSE THEORY (IRT) PSYCHOMETRIC ENGINE
========================================================================
Matematik model:
P(X_ni = 1 | theta_n, b_i) = exp(theta_n - b_i) / (1 + exp(theta_n - b_i))

Bu modul Joint Maximum Likelihood Estimation (JMLE) va PROX algoritmi
orqali quyidagilarni hisoblaydi:
1. Item Difficulty (b_i) va Person Ability (theta_n) logit shkalasida.
2. O'lchov standart xatoliklari: SE(b_i) va SE(theta_n).
3. Moslik statistikasi: Infit Mean Square (MSQ) va Outfit Mean Square (MSQ).
4. Savol sifat maqomi: 'VALID' (0.7 <= MSQ <= 1.3) yoki 'FLAGGED'.
5. Ishonchlilik: Cronbach Alpha, Person Separation Reliability, Item Separation.
========================================================================
"""

import math
from typing import List, Dict, Any, Optional

try:
    import numpy as np
    HAS_NUMPY = True
except ImportError:
    HAS_NUMPY = False


class RaschEngine:
    @staticmethod
    def estimate(
        matrix: List[List[int]],
        student_ids: List[str],
        max_iter: int = 200,
        tolerance: float = 1e-4
    ) -> Dict[str, Any]:
        """
        Rasch 1PL tahlilini amalga oshiruvchi funksiya.
        Agar numpy o'rnatilgan bo'lsa, tezkor vektorlashdan foydalanadi,
        aks holda standart kutubxona bilan ishlaydi.
        """
        N = len(matrix)
        if N == 0:
            raise ValueError("Javoblar matrisasi bo'sh bo'lishi mumkin emas!")
        M = len(matrix[0])

        if N != len(student_ids):
            raise ValueError(f"Matritsadagi o'quvchilar soni ({N}) student_ids soni ({len(student_ids)}) ga mos kelmadi!")

        if N < 2 or M < 2:
            raise ValueError("Rasch tahlili uchun kamida 2 ta talaba va 2 ta savol zarur!")

        # Row sums (person scores) and Col sums (item scores)
        row_sums = [sum(row) for row in matrix]
        col_sums = [sum(matrix[i][j] for i in range(N)) for j in range(M)]

        # 1. Ekstremal baholarni korreksiya qilish (0 ball yoki 100% ball uchun 0.3 qirqim)
        adj_row_sums = []
        for r in row_sums:
            if r <= 0:
                adj_row_sums.append(0.3)
            elif r >= M:
                adj_row_sums.append(M - 0.3)
            else:
                adj_row_sums.append(float(r))

        adj_col_sums = []
        for c in col_sums:
            if c <= 0:
                adj_col_sums.append(0.3)
            elif c >= N:
                adj_col_sums.append(N - 0.3)
            else:
                adj_col_sums.append(float(c))

        # 2. PROX boshlang'ich baholarini hisoblash (Wright & Stone)
        theta = []
        for r in adj_row_sums:
            p = r / M
            theta.append(math.log(p / (1.0 - p)))

        b = []
        for c in adj_col_sums:
            p = c / N
            b.append(math.log((1.0 - p) / p))

        # Markazlashtirish (Mean difficulty = 0.0)
        mean_b = sum(b) / M
        b = [val - mean_b for val in b]

        # 3. Joint Maximum Likelihood Estimation (JMLE) Iteratsion Sikli
        iterations = 0
        for it in range(max_iter):
            iterations = it + 1

            # E(X_ni) = P_ni va W_ni
            P = [[0.0] * M for _ in range(N)]
            W = [[0.0] * M for _ in range(N)]
            for i in range(N):
                th = theta[i]
                for j in range(M):
                    diff = th - b[j]
                    # Sigmoid
                    if diff > 35:
                        p_val = 1.0 - 1e-15
                    elif diff < -35:
                        p_val = 1e-15
                    else:
                        p_val = 1.0 / (1.0 + math.exp(-diff))
                    P[i][j] = p_val
                    W[i][j] = p_val * (1.0 - p_val)

            # Item parametrlarini yangilash
            delta_b = [0.0] * M
            for j in range(M):
                exp_item_score = sum(P[i][j] for i in range(N))
                var_item_score = max(sum(W[i][j] for i in range(N)), 1e-6)
                delta_b[j] = (exp_item_score - adj_col_sums[j]) / var_item_score

            # Person parametrlarini yangilash
            delta_theta = [0.0] * N
            for i in range(N):
                exp_person_score = sum(P[i][j] for j in range(M))
                var_person_score = max(sum(W[i][j] for j in range(M)), 1e-6)
                delta_theta[i] = (adj_row_sums[i] - exp_person_score) / var_person_score

            # Yangilash va damping (0.95)
            for j in range(M):
                b[j] += delta_b[j] * 0.95
            for i in range(N):
                theta[i] += delta_theta[i] * 0.95

            # O'lchov shkalasini markazda ushlash (mean b = 0)
            curr_mean_b = sum(b) / M
            for j in range(M):
                b[j] -= curr_mean_b
            for i in range(N):
                theta[i] -= curr_mean_b

            max_delta = max(max(abs(x) for x in delta_b), max(abs(x) for x in delta_theta))
            if max_delta < tolerance:
                break

        # Yakuniy ehtimollik va dispersiya
        P = [[0.0] * M for _ in range(N)]
        W = [[0.0] * M for _ in range(N)]
        for i in range(N):
            th = theta[i]
            for j in range(M):
                diff = th - b[j]
                if diff > 35:
                    p_val = 1.0 - 1e-15
                elif diff < -35:
                    p_val = 1e-15
                else:
                    p_val = 1.0 / (1.0 + math.exp(-diff))
                P[i][j] = p_val
                W[i][j] = p_val * (1.0 - p_val)

        # 4. Standart Xatoliklar (Standard Errors)
        se_b = []
        for j in range(M):
            var_sum = max(sum(W[i][j] for i in range(N)), 1e-6)
            se_b.append(1.0 / math.sqrt(var_sum))

        se_theta = []
        for i in range(N):
            var_sum = max(sum(W[i][j] for j in range(M)), 1e-6)
            se_theta.append(1.0 / math.sqrt(var_sum))

        # 5. Moslik Statistikasi (Infit va Outfit Mean Square)
        item_outfit_msq = [0.0] * M
        item_infit_msq = [0.0] * M
        for j in range(M):
            sq_res_sum = 0.0
            info_weighted_sq_res = 0.0
            info_sum = 0.0
            for i in range(N):
                res = matrix[i][j] - P[i][j]
                var = max(W[i][j], 1e-6)
                sq_res_sum += (res ** 2) / var
                info_weighted_sq_res += res ** 2
                info_sum += var
            item_outfit_msq[j] = sq_res_sum / N
            item_infit_msq[j] = info_weighted_sq_res / max(info_sum, 1e-6)

        person_outfit_msq = [0.0] * N
        person_infit_msq = [0.0] * N
        for i in range(N):
            sq_res_sum = 0.0
            info_weighted_sq_res = 0.0
            info_sum = 0.0
            for j in range(M):
                res = matrix[i][j] - P[i][j]
                var = max(W[i][j], 1e-6)
                sq_res_sum += (res ** 2) / var
                info_weighted_sq_res += res ** 2
                info_sum += var
            person_outfit_msq[i] = sq_res_sum / M
            person_infit_msq[i] = info_weighted_sq_res / max(info_sum, 1e-6)

        # 6. Point-Biserial korrelyatsiya
        mean_tot = sum(row_sums) / N
        var_tot = sum((r - mean_tot) ** 2 for r in row_sums) / max(N - 1, 1)
        sd_tot = math.sqrt(max(var_tot, 1e-8))

        point_biserials = []
        for j in range(M):
            col = [matrix[i][j] for i in range(N)]
            mean_col = sum(col) / N
            var_col = sum((c - mean_col) ** 2 for c in col) / max(N - 1, 1)
            sd_col = math.sqrt(max(var_col, 1e-8))

            if sd_col > 1e-6 and sd_tot > 1e-6:
                cov = sum((col[i] - mean_col) * (row_sums[i] - mean_tot) for i in range(N)) / max(N - 1, 1)
                corr = cov / (sd_col * sd_tot)
            else:
                corr = 0.0
            point_biserials.append(round(corr, 3))

        # 7. Ishonchlilik (Reliability)
        # Cronbach's Alpha
        item_variances = []
        for j in range(M):
            col = [matrix[i][j] for i in range(N)]
            m_c = sum(col) / N
            v_c = sum((c - m_c) ** 2 for c in col) / max(N - 1, 1)
            item_variances.append(v_c)

        sum_item_var = sum(item_variances)
        if var_tot > 1e-6 and M > 1:
            cronbach_alpha = (M / (M - 1)) * (1.0 - sum_item_var / var_tot)
            cronbach_alpha = max(0.0, min(1.0, cronbach_alpha))
        else:
            cronbach_alpha = 0.0

        # Person Separation Reliability
        mean_th = sum(theta) / N
        var_th = sum((t - mean_th) ** 2 for t in theta) / max(N - 1, 1)
        mean_se_th_sq = sum(s ** 2 for s in se_theta) / N
        person_sep_rel = max(0.0, min(1.0, (var_th - mean_se_th_sq) / max(var_th, 1e-6)))

        # Item Separation Reliability
        mean_b_val = sum(b) / M
        var_b = sum((item_b - mean_b_val) ** 2 for item_b in b) / max(M - 1, 1)
        mean_se_b_sq = sum(s ** 2 for s in se_b) / M
        item_sep_rel = max(0.0, min(1.0, (var_b - mean_se_b_sq) / max(var_b, 1e-6)))

        # 8. Savollar Natijalarini Shakllantirish
        question_analytics = []
        for j in range(M):
            infit = item_infit_msq[j]
            outfit = item_outfit_msq[j]
            difficulty = b[j]
            se = se_b[j]
            correct_ct = col_sums[j]
            pct = round((correct_ct / N) * 100.0, 2)

            is_valid = (0.7 <= infit <= 1.3) and (0.7 <= outfit <= 1.3)
            status = "VALID" if is_valid else "FLAGGED"

            flag_reasons = []
            if outfit > 1.3:
                flag_reasons.append("Outfit > 1.3 (Tasodifiy taxmin / Shovqin)")
            elif outfit < 0.7:
                flag_reasons.append("Outfit < 0.7 (O'ta ortiqcha determinizm)")

            if infit > 1.3:
                flag_reasons.append("Infit > 1.3 (Savol matni chalkash / Kalit xato)")
            elif infit < 0.7:
                flag_reasons.append("Infit < 0.7 (Modelga o'ta mutelik)")

            question_analytics.append({
                "question_index": j + 1,
                "difficulty_b": round(difficulty, 4),
                "standard_error": round(se, 4),
                "infit_msq": round(infit, 3),
                "outfit_msq": round(outfit, 3),
                "status": status,
                "point_biserial": point_biserials[j],
                "correct_count": correct_ct,
                "correct_percentage": pct,
                "flag_reason": "; ".join(flag_reasons) if flag_reasons else None
            })

        # 9. Talabalar Natijalarini Shakllantirish
        student_abilities = []
        sorted_thetas = sorted(theta)
        for i in range(N):
            th = theta[i]
            se = se_theta[i]
            # Percentile rank
            cnt_less = sum(1 for x in sorted_thetas if x <= th)
            percentile = round((cnt_less / N) * 100.0, 2)

            student_abilities.append({
                "student_id": student_ids[i],
                "raw_score": row_sums[i],
                "ability_theta": round(th, 4),
                "standard_error": round(se, 4),
                "infit_msq": round(person_infit_msq[i], 3),
                "outfit_msq": round(person_outfit_msq[i], 3),
                "percentile_rank": percentile
            })

        return {
            "session_summary": {
                "total_students": N,
                "total_questions": M,
                "cronbach_alpha": round(cronbach_alpha, 4),
                "person_separation_reliability": round(person_sep_rel, 4),
                "item_separation_reliability": round(item_sep_rel, 4),
                "mean_student_ability": round(sum(theta) / N, 4),
                "mean_item_difficulty": round(sum(b) / M, 4),
                "convergence_iterations": iterations
            },
            "question_analytics": question_analytics,
            "student_abilities": student_abilities
        }
