package com.example.model

data class AnswerKey(
    val title: String = "Standart Test Kaliti",
    val answers: Map<Int, String> = defaultAnswers()
) {
    fun getAnswerFor(questionNumber: Int): String {
        return answers[questionNumber] ?: "A"
    }

    fun toFormattedString(): String {
        return (1..30).joinToString(", ") { "$it:${answers[it] ?: "A"}" }
    }

    fun toCompactString(): String {
        return (1..30).map { answers[it] ?: "A" }.joinToString("")
    }

    companion object {
        val VALID_OPTIONS = listOf("A", "B", "C", "D")

        fun defaultAnswers(): Map<Int, String> {
            val map = mutableMapOf<Int, String>()
            val pattern = listOf("A", "B", "C", "D")
            for (i in 1..30) {
                map[i] = pattern[(i - 1) % pattern.size]
            }
            return map
        }

        fun fromString(input: String, title: String = "Test Kaliti"): AnswerKey {
            val letters = input.uppercase().filter { it in 'A'..'D' }
            val map = mutableMapOf<Int, String>()
            for (i in 1..30) {
                if (i - 1 < letters.length) {
                    map[i] = letters[i - 1].toString()
                } else {
                    map[i] = listOf("A", "B", "C", "D")[(i - 1) % 4]
                }
            }
            return AnswerKey(title = title, answers = map)
        }
    }
}
