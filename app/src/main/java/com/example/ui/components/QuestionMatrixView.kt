package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.QuestionResult

@Composable
fun QuestionMatrixView(
    questionDetails: List<QuestionResult>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Savollar kesimida tahlil (1 - 30):",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Split into 3 columns (1-10, 11-20, 21-30)
            val chunk1 = questionDetails.filter { it.questionNumber in 1..10 }
            val chunk2 = questionDetails.filter { it.questionNumber in 11..20 }
            val chunk3 = questionDetails.filter { it.questionNumber in 21..30 }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuestionColumn(chunk1, Modifier.weight(1f))
                QuestionColumn(chunk2, Modifier.weight(1f))
                QuestionColumn(chunk3, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuestionColumn(
    questions: List<QuestionResult>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (q in questions) {
            QuestionRowItem(q)
        }
    }
}

@Composable
private fun QuestionRowItem(item: QuestionResult) {
    val isUnmarked = item.studentAnswer == "BELGILANMAGAN"
    val (bgColor, icon, tint) = when {
        item.isCorrect -> Triple(Color(0xFFD1FAE5), Icons.Default.Check, Color(0xFF059669))
        isUnmarked -> Triple(Color(0xFFFEF3C7), Icons.Default.Remove, Color(0xFFD97706))
        else -> Triple(Color(0xFFFEE2E2), Icons.Default.Close, Color(0xFFDC2626))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${item.questionNumber}.",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (item.studentAnswer == "BELGILANMAGAN") "-" else item.studentAnswer,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tint
            )
            if (!item.isCorrect && !isUnmarked && item.correctAnswer.isNotEmpty()) {
                Text(
                    text = "(${item.correctAnswer})",
                    fontSize = 9.5.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
    }
}
