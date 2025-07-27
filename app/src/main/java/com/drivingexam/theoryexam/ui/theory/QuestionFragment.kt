package com.drivingexam.theoryexam.ui.theory

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
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
    private val selectedAnswers = mutableSetOf<String>()

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
            currentQuestion = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                args.getParcelable("question", Question::class.java)
            } else {
                @Suppress("DEPRECATION")
                args.getParcelable("question")
            } ?: throw IllegalArgumentException("Question argument is missing")

            val questionsJson = args.getString("allQuestions")
                ?: throw IllegalArgumentException("Questions JSON is missing")

            val listType = object : TypeToken<List<Question>>() {}.type
            allQuestions = Gson().fromJson(questionsJson, listType)
                ?: throw IllegalStateException("Parsed null questions list")

            currentQuestionIndex = allQuestions.indexOfFirst {
                it.questionId == currentQuestion.questionId
            }.coerceAtLeast(0)
        }
    }

    private fun setupQuestion() {
        with(binding) {
            questionHeader.text = "Питање: ${currentQuestionIndex + 1}/${allQuestions.size}"
            questionText.text = currentQuestion.question
            pointsText.text = "Број поена: ${currentQuestion.points}"

            // Настройка предупреждения о множественных ответах
            if (currentQuestion.correctIds.size > 1) {
                multipleAnswersWarning.visibility = View.VISIBLE
                multipleAnswersWarning.text = "Максимално ${currentQuestion.correctIds.size} одговора"
                choicesGroup.orientation = LinearLayout.VERTICAL
            } else {
                multipleAnswersWarning.visibility = View.GONE
                choicesGroup.orientation = LinearLayout.VERTICAL
            }

            // Очистка предыдущих вариантов
            choicesGroup.removeAllViews()
            selectedAnswers.clear()

            // Создание элементов выбора
            currentQuestion.choices.forEach { choice ->
                val choiceView = if (currentQuestion.correctIds.size > 1) {
                    CheckBox(requireContext()).apply {
                        text = choice.answer
                        id = View.generateViewId()
                        tag = choice.id

                        setOnCheckedChangeListener { buttonView, isChecked ->
                            if (isChecked) {
                                if (selectedAnswers.size < currentQuestion.correctIds.size) {
                                    selectedAnswers.add(choice.id)
                                    updateChoiceViewsState()
                                } else {
                                    buttonView.isChecked = false
                                    showChoiceLimitToast()
                                }
                            } else {
                                selectedAnswers.remove(choice.id)
                                updateChoiceViewsState()
                            }
                        }
                        setTextAppearance(R.style.ChoiceCheckBox)
                    }
                } else {
                    RadioButton(requireContext()).apply {
                        text = choice.answer
                        id = View.generateViewId()
                        setTextAppearance(R.style.ChoiceRadioButton)
                    }
                }

                // Общие настройки для обоих типов
                choiceView.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dpToPx(16)
                    }
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                }

                choicesGroup.addView(choiceView)
            }
            updateChoiceViewsState()
        }
    }

    private fun showChoiceLimitToast() {
        Toast.makeText(
            requireContext(),
            "Максимално ${currentQuestion.correctIds.size} одговора",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateChoiceViewsState() {
        if (currentQuestion.correctIds.size <= 1) return

        for (i in 0 until binding.choicesGroup.childCount) {
            val view = binding.choicesGroup.getChildAt(i)
            if (view is CheckBox) {
                val choiceId = view.tag as? String
                view.isEnabled = selectedAnswers.size < currentQuestion.correctIds.size ||
                        selectedAnswers.contains(choiceId)
                view.alpha = if (view.isEnabled) 1f else 0.5f
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun setupAnswerChecking() {
        binding.checkAnswerButton.setOnClickListener {
            if (currentQuestion.correctIds.size > 1) {
                checkMultipleChoiceAnswer()
            } else {
                checkSingleChoiceAnswer()
            }
        }
    }

    private fun checkSingleChoiceAnswer() {
        val selectedId = binding.choicesGroup.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(requireContext(), "Молимо изаберите одговор", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRadioButton = binding.choicesGroup.findViewById<RadioButton>(selectedId)
        val selectedIndex = binding.choicesGroup.indexOfChild(selectedRadioButton)
        val isCorrect = currentQuestion.choices[selectedIndex].isCorrect

        showAnswerResult(isCorrect)
    }

    private fun checkMultipleChoiceAnswer() {
        if (selectedAnswers.isEmpty()) {
            Toast.makeText(requireContext(), "Молимо изаберите одговоре", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверяем, что выбраны ВСЕ правильные ответы и ничего лишнего
        val isFullyCorrect = selectedAnswers.size == currentQuestion.correctIds.size &&
                selectedAnswers.containsAll(currentQuestion.correctIds)

        showAnswerResult(isFullyCorrect)
    }

    private fun showAnswerResult(isCorrect: Boolean) {
        if (isCorrect) {
            Toast.makeText(requireContext(), "Тачно!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Нетачно", Toast.LENGTH_SHORT).show()
            highlightCorrectAnswers()
        }
    }

    private fun highlightCorrectAnswers() {
        currentQuestion.choices.forEachIndexed { index, choice ->
            if (choice.isCorrect) {
                val answerView = binding.choicesGroup.getChildAt(index)
                answerView.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.green_light))
            }
        }
    }

    private fun setupNavigation() {
        binding.prevButton.setOnClickListener {
            if (currentQuestionIndex > 0) {
                navigateToQuestion(currentQuestionIndex - 1)
            }
        }

        binding.nextButton.setOnClickListener {
            if (currentQuestionIndex < allQuestions.size - 1) {
                navigateToQuestion(currentQuestionIndex + 1)
            }
        }
    }

    private fun navigateToQuestion(index: Int) {
        currentQuestionIndex = index
        currentQuestion = allQuestions[index]
        setupQuestion()
        updateNavigationButtons()
    }

    private fun updateNavigationButtons() {
        binding.prevButton.isEnabled = currentQuestionIndex > 0
        binding.nextButton.isEnabled = currentQuestionIndex < allQuestions.size - 1
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}