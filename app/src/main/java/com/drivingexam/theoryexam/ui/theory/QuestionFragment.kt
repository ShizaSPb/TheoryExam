package com.drivingexam.theoryexam.ui.theory

import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import android.graphics.drawable.Drawable
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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
    private val selectedAnswers = mutableSetOf<String>()
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

    private fun setupQuestion() {
        answerChecked = false
        isAnswerCorrect = false

        with(binding) {
            // Установка основных данных вопроса
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

            // Загрузка изображения вопроса (если есть)
            if (!currentQuestion.image.isNullOrEmpty() || !currentQuestion.imageLocal.isNullOrEmpty()) {
                imageContainer.visibility = View.VISIBLE
                questionImage.visibility = View.VISIBLE
                imageProgressBar.visibility = View.VISIBLE

                val imageUrl = currentQuestion.imageLocal ?: currentQuestion.image
                loadQuestionImage(imageUrl)
            } else {
                imageContainer.visibility = View.GONE
                questionImage.visibility = View.GONE
                imageProgressBar.visibility = View.GONE
            }

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

                // Общие настройки для вариантов ответа
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

            updateNavigationButtons()
        }
    }

    private fun loadQuestionImage(imageUrl: String?) {
        if (imageUrl.isNullOrEmpty()) {
            binding.imageContainer.visibility = View.GONE
            return
        }

        try {
            when {
                imageUrl.startsWith("http") -> {
                    Glide.with(requireContext().applicationContext)
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                binding.imageProgressBar.visibility = View.GONE
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                binding.imageProgressBar.visibility = View.GONE
                                return false
                            }
                        })
                        .into(binding.questionImage)
                }

                else -> {
                    // Обработка локальных изображений
                    val localPath = imageUrl.replace("\\", "/") // Заменяем обратные слэши
                    try {
                        // Пробуем загрузить из assets
                        val inputStream = requireContext().assets.open(localPath)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        binding.questionImage.setImageBitmap(bitmap)
                        binding.imageProgressBar.visibility = View.GONE
                    } catch (e: Exception) {
                        // Пробуем загрузить как ресурс
                        val resId = resources.getIdentifier(
                            localPath.substringBeforeLast("."), // Удаляем расширение
                            "drawable",
                            requireContext().packageName
                        )
                        if (resId != 0) {
                            binding.questionImage.setImageResource(resId)
                            binding.imageProgressBar.visibility = View.GONE
                        } else {
                            showErrorImage()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IMAGE_LOAD", "Error loading image: $imageUrl", e)
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

        currentQuestion.choices.forEach { choice ->
            getChoiceView(choice.id)?.let { view ->
                val isSelectable = selectedAnswers.size < currentQuestion.correctIds.size ||
                        selectedAnswers.contains(choice.id)

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

            // Всегда подсвечиваем правильные ответы при нажатии "Прикажи одговор"
            highlightCorrectAnswers()
            answerChecked = true
            updateNavigationButtons()
        }
    }

    private fun checkSingleChoiceAnswer() {
        val selectedId = binding.choicesGroup.checkedRadioButtonId
        if (selectedId == -1) {
            // Ничего не выбрано - просто показываем правильные ответы
            isAnswerCorrect = false
            return
        }

        val selectedRadioButton = binding.choicesGroup.findViewById<RadioButton>(selectedId) ?: return
        val selectedIndex = binding.choicesGroup.indexOfChild(selectedRadioButton).takeIf { it >= 0 } ?: return

        if (selectedIndex >= currentQuestion.choices.size) return

        isAnswerCorrect = currentQuestion.choices[selectedIndex].isCorrect
        showAnswerResult(isAnswerCorrect)
    }

    private fun checkMultipleChoiceAnswer() {
        if (selectedAnswers.isEmpty()) {
            // Ничего не выбрано - просто показываем правильные ответы
            isAnswerCorrect = false
            return
        }

        isAnswerCorrect = selectedAnswers.size == currentQuestion.correctIds.size &&
                selectedAnswers.containsAll(currentQuestion.correctIds)

        showAnswerResult(isAnswerCorrect)
    }

    private fun showAnswerResult(isCorrect: Boolean) {
        if (isCorrect) {
            Toast.makeText(requireContext(), "Тачно!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Нетачно", Toast.LENGTH_SHORT).show()
        }
    }

    private fun highlightCorrectAnswers() {
        currentQuestion.choices.forEachIndexed { index, choice ->
            val answerView = binding.choicesGroup.getChildAt(index)
            if (choice.isCorrect) {
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
            if (!answerChecked && hasUserSelectedAnswer()) {
                // Если ответ не проверен и есть выбор - проверяем
                checkAnswer()
                answerChecked = true

                if (!isAnswerCorrect) {
                    // Если ответ неверный - подсвечиваем правильные
                    highlightCorrectAnswers()
                    updateNavigationButtons()
                    return@setOnClickListener
                }
                // Если ответ верный - продолжаем выполнение (переход)
            }

            // Переход к следующему вопросу
            if (currentQuestionIndex < allQuestions.size - 1) {
                navigateToQuestion(currentQuestionIndex + 1)
            }
        }
    }

    private fun hasUserSelectedAnswer(): Boolean {
        return if (currentQuestion.correctIds.size > 1) {
            selectedAnswers.isNotEmpty()
        } else {
            binding.choicesGroup.checkedRadioButtonId != -1
        }
    }

    private fun checkAnswer() {
        if (currentQuestion.correctIds.size > 1) {
            checkMultipleChoiceAnswer()
        } else {
            checkSingleChoiceAnswer()
        }
    }

    private fun navigateToQuestion(index: Int) {
        currentQuestionIndex = index
        currentQuestion = allQuestions[index]
        answerChecked = false // Сбрасываем флаг проверки ответа
        isAnswerCorrect = false
        selectedAnswers.clear()
        setupQuestion()
        updateNavigationButtons()
    }

    private fun updateNavigationButtons() {
        val isFirstQuestion = currentQuestionIndex == 0
        val isLastQuestion = currentQuestionIndex == allQuestions.size - 1

        // Кнопка "Назад"
        binding.prevButton.apply {
            visibility = if (isFirstQuestion) View.INVISIBLE else View.VISIBLE
            isEnabled = !isFirstQuestion
        }

        // Кнопка "Напред" - просто скрываем на последнем вопросе
        binding.nextButton.visibility = if (isLastQuestion) View.INVISIBLE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        selectedAnswers.clear()
    }

    private fun getChoiceView(choiceId: String): CheckBox? {
        return binding.choicesGroup.children
            .filterIsInstance<CheckBox>()
            .firstOrNull { it.tag == choiceId }
    }
}