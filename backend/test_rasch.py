"""
Unit test for Rasch Engine (1PL Item Response Theory)
"""

import unittest
from rasch_engine import RaschEngine


class TestRaschEngine(unittest.TestCase):
    def test_basic_rasch_estimation(self):
        # 5 students x 6 questions matrix
        # S1: High ability, S5: Low ability
        matrix = [
            [1, 1, 1, 1, 1, 0], # S1 (5/6)
            [1, 1, 1, 1, 0, 0], # S2 (4/6)
            [1, 1, 1, 0, 0, 0], # S3 (3/6)
            [1, 1, 0, 0, 0, 0], # S4 (2/6)
            [1, 0, 0, 0, 0, 0], # S5 (1/6)
        ]
        student_ids = ["ID_001", "ID_002", "ID_003", "ID_004", "ID_005"]

        res = RaschEngine.estimate(matrix, student_ids)

        self.assertIn("session_summary", res)
        self.assertIn("question_analytics", res)
        self.assertIn("student_abilities", res)

        # Questions check
        questions = res["question_analytics"]
        self.assertEqual(len(questions), 6)

        # Question 1 is easiest (all got 1), Question 6 is hardest (only S1 got 0, others 0)
        diff_q1 = questions[0]["difficulty_b"]
        diff_q6 = questions[5]["difficulty_b"]
        self.assertLess(diff_q1, diff_q6, "1-savol 6-savoldan osonroq (b1 < b6) bo'lishi kerak!")

        # Students check
        students = res["student_abilities"]
        self.assertEqual(len(students), 5)
        # S1 should have higher theta than S5
        self.assertGreater(students[0]["ability_theta"], students[4]["ability_theta"])

        # Check status field
        for q in questions:
            self.assertIn(q["status"], ["VALID", "FLAGGED"])
            self.assertGreater(q["infit_msq"], 0)
            self.assertGreater(q["outfit_msq"], 0)

        print("Rasch Engine Unit Test muvaffaqiyatli o'tdi!")


if __name__ == "__main__":
    unittest.main()
