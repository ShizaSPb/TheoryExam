package com.drivingexam.theoryexam.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QuestionData {
    suspend fun loadQuestions(context: Context): Map<String, Map<String, List<Question>>> {
        return withContext(Dispatchers.IO) {
            val json = context.assets.open("QuestionsNew.json")
                .bufferedReader()
                .use { it.readText() }

            val type = TypeToken.getParameterized(List::class.java, Question::class.java).type
            val questions = Gson().fromJson<List<Question>>(json, type)
                ?: throw IllegalStateException("Failed to parse questions")

            // Валидация данных
            questions.forEach { q ->
                require(q.questionId.isNotBlank()) { "Question ID is empty" }
                require(q.question.isNotBlank()) { "Question text is empty" }
            }

            questions.groupBy { it.categoryName }
                .mapValues { it.value.groupBy { q -> q.subcategoryName } }
        }
    }
}