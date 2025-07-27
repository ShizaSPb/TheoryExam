package com.drivingexam.theoryexam.ui.theory

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.drivingexam.theoryexam.R
import com.drivingexam.theoryexam.data.Question
import com.drivingexam.theoryexam.databinding.FragmentQuestionBinding
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
    }

    private fun parseArguments() {
        arguments?.let { args ->
            // Получаем текущий вопрос
            currentQuestion = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                args.getParcelable("question", Question::class.java)
            } else {
                @Suppress("DEPRECATION")
                args.getParcelable("question")
            } ?: run {
                showErrorAndGoBack("Question data is missing")
                return@let
            }

            // Получаем JSON строку и десериализуем в список вопросов
            val questionsJson = args.getString("allQuestions") ?: run {
                showErrorAndGoBack("Questions list is missing")
                return@let
            }

            try {
                val type = object : TypeToken<List<Question>>() {}.type
                allQuestions = Gson().fromJson(questionsJson, type) ?: run {
                    showErrorAndGoBack("Failed to parse questions")
                    return@let
                }
            } catch (e: Exception) {
                Log.e("QuestionFragment", "JSON parsing error", e)
                showErrorAndGoBack("Invalid questions data format")
                return@let
            }

            if (allQuestions.isEmpty()) {
                showErrorAndGoBack("Questions list is empty")
                return
            }

            currentQuestionIndex = allQuestions.indexOfFirst { it.question == currentQuestion.question }
                .takeIf { it != -1 } ?: 0
        } ?: showErrorAndGoBack("No arguments provided")
    }

    private fun showErrorAndGoBack(message: String) {
        Log.e("QuestionFragment", message)
        Toast.makeText(requireContext(), "Error: $message", Toast.LENGTH_LONG).show()
        findNavController().popBackStack()
    }

    private fun setupQuestion() {
        with(binding) {
            questionText.text = currentQuestion.question
            pointsText.text = getString(R.string.points_format, currentQuestion.points)

            currentQuestion.image?.let { imageUrl ->
                imageContainer.visibility = View.VISIBLE
                // Здесь должна быть загрузка изображения
            } ?: run {
                imageContainer.visibility = View.GONE
            }

            if (currentQuestion.correctIds.size > 1) {
                multipleAnswersWarning.visibility = View.VISIBLE
                multipleAnswersWarning.text = getString(R.string.multiple_answers_warning)
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
        with(binding) {
            prevButton.isEnabled = currentQuestionIndex > 0
            prevButton.setOnClickListener {
                showQuestion(allQuestions[currentQuestionIndex - 1])
            }

            nextButton.isEnabled = currentQuestionIndex < allQuestions.size - 1
            nextButton.setOnClickListener {
                showQuestion(allQuestions[currentQuestionIndex + 1])
            }
        }
    }

    private fun setupAnswerChecking() {
        binding.checkAnswerButton.setOnClickListener {
            checkAnswer()
        }
    }

    private fun checkAnswer() {
        val selectedId = binding.choicesGroup.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(requireContext(), "Please select an answer", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRadioButton = binding.choicesGroup.findViewById<RadioButton>(selectedId)
        val selectedIndex = binding.choicesGroup.indexOfChild(selectedRadioButton)
        val isCorrect = currentQuestion.choices[selectedIndex].isCorrect

        if (isCorrect) {
            Toast.makeText(requireContext(), "Correct!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Incorrect", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showQuestion(question: Question) {
        try {
            val gson = Gson()
            val args = bundleOf(
                "question" to question,
                "allQuestions" to gson.toJson(allQuestions)
            )

            findNavController().navigate(
                R.id.action_questionFragment_self,
                args
            )
        } catch (e: Exception) {
            Log.e("QuestionFragment", "Navigation error", e)
            Toast.makeText(requireContext(), "Error navigating to question", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}