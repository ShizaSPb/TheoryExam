package com.drivingexam.theoryexam.data

import android.content.Context
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

            val type = object : TypeToken<List<Question>>() {}.type
            val questions = Gson().fromJson<List<Question>>(json, type)

            questions.groupBy { it.categoryName }
                .mapValues { it.value.groupBy { q -> q.subcategoryName } }
        }
    }
}