package com.drivingexam.theoryexam.ui.theory

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.RadioButton
import android.widget.Toast
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
            // Устанавливаем заголовок с номером вопроса
            questionHeader.text = "Питање: ${currentQuestionIndex + 1}/${allQuestions.size}"

            questionText.text = currentQuestion.question
            pointsText.text = "Број поена: ${currentQuestion.points}"

            currentQuestion.image?.let { imageUrl ->
                imageContainer.visibility = View.VISIBLE
                // Загрузка изображения
            } ?: run {
                imageContainer.visibility = View.GONE
            }

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
        with(binding) {
            prevButton.apply {
                isEnabled = currentQuestionIndex > 0
                setOnClickListener {
                    showQuestion(allQuestions[currentQuestionIndex - 1])
                }
            }

            nextButton.apply {
                isEnabled = currentQuestionIndex < allQuestions.size - 1
                setOnClickListener {
                    showQuestion(allQuestions[currentQuestionIndex + 1])
                }
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
        // Создаем анимацию для контента
        val slideOut = AnimationUtils.loadAnimation(requireContext(),
            if (question == allQuestions[currentQuestionIndex + 1]) R.anim.slide_out_left
            else R.anim.slide_out_right)

        val slideIn = AnimationUtils.loadAnimation(requireContext(),
            if (question == allQuestions[currentQuestionIndex + 1]) R.anim.slide_in_right
            else R.anim.slide_in_left)

        // Применяем анимацию только к контейнеру с контентом
        binding.questionContentContainer.startAnimation(slideOut)

        slideOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                // После завершения анимации скрытия, обновляем данные
                currentQuestion = question
                currentQuestionIndex = allQuestions.indexOfFirst { it.question == currentQuestion.question }
                    .takeIf { it != -1 } ?: 0

                setupQuestion()

                // Запускаем анимацию появления нового контента
                binding.questionContentContainer.startAnimation(slideIn)

                // Обновляем состояние кнопок
                binding.prevButton.isEnabled = currentQuestionIndex > 0
                binding.nextButton.isEnabled = currentQuestionIndex < allQuestions.size - 1
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}