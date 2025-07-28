package com.drivingexam.theoryexam.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QuestionData {
    // Кэшированные вопросы (категория -> подкатегория -> список вопросов)
    private var cachedQuestions: Map<String, Map<String, List<Question>>>? = null

    // Кэшированный TypeToken для эффективного парсинга
    private val questionsListType = object : TypeToken<List<Question>>() {}.type

    suspend fun loadQuestions(context: Context): Map<String, Map<String, List<Question>>> {
        return cachedQuestions ?: withContext(Dispatchers.IO) {
            try {
                // Чтение JSON из assets
                val json = context.assets.open("QuestionsNew.json")
                    .bufferedReader()
                    .use { it.readText() }

                // Парсинг JSON с кэшированным TypeToken
                val questions = Gson().fromJson<List<Question>>(json, questionsListType)
                    ?: throw IllegalStateException("Failed to parse questions")

                // Валидация данных
                validateQuestions(questions)

                // Группировка вопросов
                val groupedQuestions = questions.groupBy { it.categoryName }
                    .mapValues { it.value.groupBy { q -> q.subcategoryName } }

                // Сохраняем в кэш
                cachedQuestions = groupedQuestions
                groupedQuestions
            } catch (e: Exception) {
                Log.e("QuestionData", "Error loading questions", e)
                throw e
            }
        }
    }

    private fun validateQuestions(questions: List<Question>) {
        questions.forEach { q ->
            require(q.questionId.isNotBlank()) { "Question ID is empty for question: ${q.question}" }
            require(q.question.isNotBlank()) { "Question text is empty for ID: ${q.questionId}" }
            require(q.choices.isNotEmpty()) { "No choices for question: ${q.questionId}" }
        }
    }

    // Очистка кэша (например, при logout или смене пользователя)
    fun clearCache() {
        cachedQuestions = null
    }
}