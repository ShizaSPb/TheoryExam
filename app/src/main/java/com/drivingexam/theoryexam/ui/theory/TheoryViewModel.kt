package com.drivingexam.theoryexam.ui.theory

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivingexam.theoryexam.data.Question
import com.drivingexam.theoryexam.data.QuestionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TheoryViewModel : ViewModel() {
    // Приватные состояния
    private val _categories = MutableStateFlow<List<String>>(emptyList())
    private val _subcategories = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    private val _questions = MutableStateFlow<Map<String, Map<String, List<Question>>>>(emptyMap())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // Публичные интерфейсы для UI
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    fun loadQuestions(context: Context) {
        if (_isLoading.value) return

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val data = QuestionData.loadQuestions(context)
                _questions.value = data
                _categories.value = data.keys.toList()
                // Предзаполняем подкатегории для быстрого доступа
                _subcategories.value = data.mapValues { it.value.keys.toList() }
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки вопросов"
                Log.e("TheoryViewModel", "Error loading questions", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getSubcategoriesFor(category: String): List<String> {
        return _questions.value[category]?.keys?.toList().orEmpty()
    }

    fun getQuestionsFor(category: String, subcategory: String): List<Question> {
        return _questions.value[category]?.get(subcategory).orEmpty().also { questions ->
            if (questions.isEmpty()) {
                Log.d("TheoryVM", "No questions for $category/$subcategory")
            }
            questions.firstOrNull { it.choices.isEmpty() }?.let {
                Log.w("TheoryVM", "Question ${it.questionId} has no choices")
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}