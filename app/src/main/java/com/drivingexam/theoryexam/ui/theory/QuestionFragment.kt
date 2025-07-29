package com.drivingexam.theoryexam.ui.theory

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
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
    private var answerChecked = false
    private var isAnswerCorrect = false
    private var shuffledChoices: List<Question.Choice> = emptyList()
    private val selectedAnswers = mutableSetOf<String>()
    private val selectedAnswersMap = mutableMapOf<String, Set<String>>()

    companion object {
        private val QUESTIONS_LIST_TYPE = object : TypeToken<List<Question>>() {}.type
    }

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
        updateNavigationButtons()
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

            allQuestions = Gson().fromJson(questionsJson, QUESTIONS_LIST_TYPE)
                ?: throw IllegalStateException("Parsed null questions list")

            currentQuestionIndex = allQuestions.indexOfFirst {
                it.questionId == currentQuestion.questionId
            }.coerceAtLeast(0)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupQuestion() {
        answerChecked = false
        isAnswerCorrect = false

        with(binding) {
            questionHeader.text = "Питање: ${currentQuestionIndex + 1}/${allQuestions.size}"
            questionText.text = currentQuestion.question
            pointsText.text = "Број поена: ${currentQuestion.points}"

            if (currentQuestion.correctIds.size > 1) {
                multipleAnswersWarning.visibility = View.VISIBLE
                multipleAnswersWarning.text = "Максимално ${currentQuestion.correctIds.size} одговора"
                choicesGroup.orientation = LinearLayout.VERTICAL
            } else {
                multipleAnswersWarning.visibility = View.GONE
                choicesGroup.orientation = LinearLayout.VERTICAL
            }

            choicesGroup.removeAllViews()
            selectedAnswers.clear()
            selectedAnswersMap[currentQuestion.questionId]?.let { savedAnswers ->
                selectedAnswers.addAll(savedAnswers)
            }

            if (!currentQuestion.image.isNullOrEmpty() || !currentQuestion.imageLocal.isNullOrEmpty()) {
                imageContainer.visibility = View.VISIBLE
                questionImage.visibility = View.VISIBLE
                imageProgressBar.visibility = View.VISIBLE
                loadQuestionImage(currentQuestion.imageLocal ?: currentQuestion.image)
            } else {
                imageContainer.visibility = View.GONE
                questionImage.visibility = View.GONE
                imageProgressBar.visibility = View.GONE
            }

            shuffledChoices = currentQuestion.choices.shuffled()

            shuffledChoices.forEach { choice ->
                val choiceView = if (currentQuestion.correctIds.size > 1) {
                    CheckBox(requireContext()).apply {
                        text = choice.answer
                        id = View.generateViewId()
                        tag = choice.id
                        isChecked = selectedAnswers.contains(choice.id)

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
                            saveCurrentSelection()
                        }
                        setTextAppearance(R.style.ChoiceCheckBox)
                    }
                } else {
                    RadioButton(requireContext()).apply {
                        text = choice.answer
                        id = View.generateViewId()
                        tag = choice.id
                        isChecked = selectedAnswers.contains(choice.id)

                        setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                selectedAnswers.clear()
                                selectedAnswers.add(choice.id)
                                saveCurrentSelection()
                            }
                        }
                        setTextAppearance(R.style.ChoiceRadioButton)
                    }
                }

                choiceView.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dpToPx(16) }
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                }

                choicesGroup.addView(choiceView)
            }

            updateNavigationButtons()
        }
    }

    private fun saveCurrentSelection() {
        selectedAnswersMap[currentQuestion.questionId] = selectedAnswers.toSet()
    }

    private fun loadQuestionImage(imagePath: String?) {
        if (imagePath.isNullOrEmpty()) {
            binding.imageContainer.visibility = View.GONE
            return
        }

        try {
            // Нормализуем путь (заменяем обратные слеши и убираем дублирующиеся разделители)
            val normalizedPath = imagePath.replace("\\", "/")
                .replace("//", "/")
                .removePrefix("/")

            // Проверяем, что путь начинается с images/
            val assetPath = if (normalizedPath.startsWith("images/")) {
                normalizedPath
            } else {
                "images/$normalizedPath"
            }

            // Загружаем из assets
            requireContext().assets.open(assetPath).use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                binding.questionImage.setImageBitmap(bitmap)
                binding.imageProgressBar.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e("IMAGE_LOAD", "Error loading image from assets: $imagePath", e)
            showErrorImage()
        }
    }

    private fun showErrorImage() {
        binding.imageProgressBar.visibility = View.GONE
        binding.questionImage.setImageResource(R.drawable.error_image)
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

        binding.choicesGroup.children.forEach { view ->
            if (view is CheckBox) {
                val isSelectable = selectedAnswers.size < currentQuestion.correctIds.size ||
                        selectedAnswers.contains(view.tag)
                view.isEnabled = isSelectable
                view.alpha = if (isSelectable) 1f else 0.5f
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
            highlightCorrectAnswers()
            answerChecked = true
            isAnswerCorrect = selectedAnswers.containsAll(currentQuestion.correctIds)
        }
    }

    private fun checkSingleChoiceAnswer() {
        if (selectedAnswers.isEmpty()) {
            isAnswerCorrect = false
            return
        }

        isAnswerCorrect = currentQuestion.correctIds.contains(selectedAnswers.first())
        showAnswerResult(isAnswerCorrect)
    }

    private fun checkMultipleChoiceAnswer() {
        if (selectedAnswers.isEmpty()) {
            isAnswerCorrect = false
            return
        }

        isAnswerCorrect = selectedAnswers.size == currentQuestion.correctIds.size &&
                selectedAnswers.containsAll(currentQuestion.correctIds)
        showAnswerResult(isAnswerCorrect)
    }

    private fun showAnswerResult(isCorrect: Boolean) {
        Toast.makeText(
            requireContext(),
            if (isCorrect) "Тачно!" else "Нетачно",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun highlightCorrectAnswers() {
        binding.choicesGroup.children.forEachIndexed { index, view ->
            if (index < shuffledChoices.size) {
                val choice = shuffledChoices[index]
                if (choice.isCorrect) {
                    view.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.green_light))
                }
            }
        }
    }

    private fun setupNavigation() {
        binding.prevButton.setOnClickListener {
            saveCurrentSelection()
            if (currentQuestionIndex > 0) {
                navigateToQuestion(currentQuestionIndex - 1)
            }
        }

        binding.nextButton.setOnClickListener {
            saveCurrentSelection()
            if (currentQuestionIndex < allQuestions.size - 1) {
                navigateToQuestion(currentQuestionIndex + 1)
            }
        }
    }

    private fun navigateToQuestion(index: Int) {
        saveCurrentSelection()
        currentQuestionIndex = index
        currentQuestion = allQuestions[index]
        answerChecked = false
        isAnswerCorrect = false
        setupQuestion()
    }

    private fun updateNavigationButtons() {
        val isFirstQuestion = currentQuestionIndex == 0
        val isLastQuestion = currentQuestionIndex == allQuestions.size - 1

        binding.prevButton.apply {
            visibility = if (isFirstQuestion) View.INVISIBLE else View.VISIBLE
            isEnabled = !isFirstQuestion
        }

        binding.nextButton.visibility = if (isLastQuestion) View.INVISIBLE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}