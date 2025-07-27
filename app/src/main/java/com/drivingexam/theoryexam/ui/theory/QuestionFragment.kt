package com.drivingexam.theoryexam.ui.theory

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.drivingexam.theoryexam.R
import com.drivingexam.theoryexam.databinding.FragmentQuestionBinding
import com.drivingexam.theoryexam.data.Question
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class QuestionFragment : Fragment() {
    private var _binding: FragmentQuestionBinding? = null
    private val binding get() = _binding!!
    private lateinit var currentQuestion: Question
    private lateinit var allQuestions: List<Question>
    private var currentQuestionIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parseArguments()
        setupQuestion()
        setupNavigation()
        setupAnswerChecking()

        Log.d("QUESTIONS_DEBUG", "Total questions: ${allQuestions.size}")
        Log.d("QUESTIONS_DEBUG", "Current index: $currentQuestionIndex")
    }

    private fun parseArguments() {
        try {
            arguments?.let { args ->
                // Получаем текущий вопрос
                currentQuestion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    args.getParcelable("question", Question::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    args.getParcelable("question")
                } ?: throw IllegalArgumentException("Question argument is missing")

                // Получаем JSON строку
                val questionsJson = args.getString("allQuestions")
                    ?: throw IllegalArgumentException("Questions JSON is missing")

                Log.d("JSON_DEBUG", "Received JSON length: ${questionsJson.length}")

                // Парсим список вопросов
                val listType = object : TypeToken<List<Question>>() {}.type
                allQuestions = Gson().fromJson<List<Question>>(questionsJson, listType)
                    ?: throw IllegalStateException("Parsed null questions list")

                // Находим индекс текущего вопроса
                currentQuestionIndex = allQuestions.indexOfFirst {
                    it.questionId == currentQuestion.questionId
                }.coerceAtLeast(0)

                Log.d("QUESTIONS_LOADED", "Loaded ${allQuestions.size} questions. Current index: $currentQuestionIndex")

            } ?: throw IllegalStateException("Fragment arguments are null")
        } catch (e: Exception) {
            Log.e("PARSE_ERROR", "Failed to parse questions: ${e.javaClass.simpleName}", e)
            showErrorAndGoBack("Error loading questions: ${e.localizedMessage}")
        }
    }

    private fun showErrorAndGoBack(message: String) {
        Toast.makeText(requireContext(), "Error: $message", Toast.LENGTH_LONG).show()
        findNavController().popBackStack()
    }

    private fun setupQuestion() {
        with(binding) {
            questionHeader.text = "Питање: ${currentQuestionIndex + 1}/${allQuestions.size}"
            questionText.text = currentQuestion.question
            pointsText.text = "Број поена: ${currentQuestion.points}"

            if (currentQuestion.correctIds.size > 1) {
                multipleAnswersWarning.visibility = View.VISIBLE
                multipleAnswersWarning.text = "Број потребних одговора: ${currentQuestion.correctIds.size}"
            } else {
                multipleAnswersWarning.visibility = View.GONE
            }

            choicesGroup.removeAllViews()
            currentQuestion.choices.forEach { choice ->
                val radioButton = layoutInflater.inflate(
                    R.layout.item_choice,
                    choicesGroup,
                    false
                ) as RadioButton
                radioButton.text = choice.answer
                radioButton.id = View.generateViewId()
                choicesGroup.addView(radioButton)
            }
        }
    }

    private fun setupNavigation() {
        binding.prevButton.setOnClickListener {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--
                currentQuestion = allQuestions[currentQuestionIndex]
                setupQuestion()
                updateNavigationButtons()
            }
        }

        binding.nextButton.setOnClickListener {
            if (currentQuestionIndex < allQuestions.size - 1) {
                currentQuestionIndex++
                currentQuestion = allQuestions[currentQuestionIndex]
                setupQuestion()
                updateNavigationButtons()
            }
        }

        updateNavigationButtons()
    }

    private fun updateNavigationButtons() {
        binding.prevButton.isEnabled = currentQuestionIndex > 0
        binding.nextButton.isEnabled = currentQuestionIndex < allQuestions.size - 1
    }

    private fun setupAnswerChecking() {
        binding.checkAnswerButton.setOnClickListener {
            checkAnswer()
        }
    }

    private fun checkAnswer() {
        val selectedId = binding.choicesGroup.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(requireContext(), "Молимо изаберите одговор", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRadioButton = binding.choicesGroup.findViewById<RadioButton>(selectedId)
        val selectedIndex = binding.choicesGroup.indexOfChild(selectedRadioButton)
        val isCorrect = currentQuestion.choices[selectedIndex].isCorrect

        if (isCorrect) {
            Toast.makeText(requireContext(), "Тачно!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Нетачно", Toast.LENGTH_SHORT).show()
            // Показываем правильный ответ
            currentQuestion.choices.forEachIndexed { index, choice ->
                if (choice.isCorrect) {
                    val correctButton = binding.choicesGroup.getChildAt(index) as RadioButton
                    correctButton.setBackgroundColor(resources.getColor(R.color.green))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}