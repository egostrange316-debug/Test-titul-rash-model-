package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AnswerKey
import org.json.JSONObject

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("omr_prefs", Context.MODE_PRIVATE)

    fun saveAnswerKey(answerKey: AnswerKey) {
        val json = JSONObject()
        json.put("title", answerKey.title)
        val answersObj = JSONObject()
        for ((q, ans) in answerKey.answers) {
            answersObj.put(q.toString(), ans)
        }
        json.put("answers", answersObj)
        prefs.edit().putString(KEY_ANSWER_MAP, json.toString()).apply()
    }

    fun getAnswerKey(): AnswerKey {
        val saved = prefs.getString(KEY_ANSWER_MAP, null) ?: return AnswerKey()
        return try {
            val json = JSONObject(saved)
            val title = json.optString("title", "Standart Test Kaliti")
            val answersObj = json.getJSONObject("answers")
            val map = mutableMapOf<Int, String>()
            val keys = answersObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k.toInt()] = answersObj.getString(k)
            }
            AnswerKey(title = title, answers = map)
        } catch (e: Exception) {
            AnswerKey()
        }
    }

    companion object {
        private const val KEY_ANSWER_MAP = "key_answer_map"
    }
}
