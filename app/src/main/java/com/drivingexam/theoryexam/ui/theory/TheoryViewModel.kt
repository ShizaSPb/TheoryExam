package com.drivingexam.theoryexam.ui.theory

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivingexam.theoryexam.data.Question
import com.drivingexam.theoryexam.data.QuestionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TheoryViewModel : ViewModel() {
    // Состояния для категорий и подкатегорий
    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories

    private val _subcategories = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val subcategories: StateFlow<Map<String, List<String>>> = _subcategories

    private val _questions = MutableStateFlow<Map<String, Map<String, List<Question>>>>(emptyMap())
    val questions: StateFlow<Map<String, Map<String, List<Question>>>> = _questions

    // Загрузка данных
    fun loadQuestions(context: Context) {
        viewModelScope.launch {
            val data = QuestionData.loadQuestions(context)
            _questions.value = data
            _categories.value = data.keys.toList()
        }
    }

    // Получение подкатегорий для выбранной категории
    fun getSubcategoriesForCategory(category: String): List<String> {
        return _questions.value[category]?.keys?.toList() ?: emptyList()
    }

    // Получение вопросов для подкатегории
    fun getQuestionsForSubcategory(category: String, subcategory: String): List<Question> {
        return _questions.value[category]?.get(subcategory)?.also { questions ->
            Log.d("TheoryVM", "Found ${questions.size} questions for $category/$subcategory")
        } ?: run {
            Log.e("TheoryVM", "No questions found for $category/$subcategory")
            emptyList()
        }
    }
}