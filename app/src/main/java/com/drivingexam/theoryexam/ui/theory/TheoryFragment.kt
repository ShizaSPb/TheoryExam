package com.drivingexam.theoryexam.ui.theory

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.drivingexam.theoryexam.R
import com.drivingexam.theoryexam.data.Question
import com.drivingexam.theoryexam.databinding.FragmentTheoryBinding
import com.drivingexam.theoryexam.ui.theory.adapters.CategoriesAdapter
import com.drivingexam.theoryexam.ui.theory.adapters.SubcategoriesAdapter
import com.google.gson.Gson
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TheoryFragment : Fragment() {
    private var _binding: FragmentTheoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TheoryViewModel by viewModels()
    private var selectedCategory: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTheoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        observeViewModel()
        loadData()
    }

    private fun setupRecyclerViews() {
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSubcategories.layoutManager = LinearLayoutManager(requireContext())

        binding.rvCategories.adapter = CategoriesAdapter(emptyList()) { category ->
            onCategorySelected(category)
        }
        binding.rvSubcategories.adapter = SubcategoriesAdapter(emptyList()) { subcategory ->
            onSubcategorySelected(subcategory)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categories.collectLatest { categories ->
                (binding.rvCategories.adapter as? CategoriesAdapter)?.updateData(categories)
            }
        }
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadQuestions(requireContext())
        }
    }

    private fun onCategorySelected(category: String) {
        selectedCategory = category
        val subcategories = viewModel.getSubcategoriesForCategory(category)
        (binding.rvSubcategories.adapter as? SubcategoriesAdapter)?.updateData(subcategories)
    }

    private fun onSubcategorySelected(subcategory: String) {
        val category = selectedCategory ?: run {
            Toast.makeText(requireContext(), "Ошибка: категория не выбрана", Toast.LENGTH_SHORT).show()
            return
        }

        val questions = try {
            viewModel.getQuestionsForSubcategory(category, subcategory).also {
                Log.d("QUESTIONS_DEBUG", "Loaded ${it.size} questions for $category/$subcategory")
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка загрузки вопросов", Toast.LENGTH_SHORT).show()
            return
        }

        if (questions.isEmpty()) {
            Toast.makeText(requireContext(), "В этой подкатегории пока нет вопросов", Toast.LENGTH_SHORT).show()
            return
        }

        navigateToQuestion(questions.first(), questions)
    }

    private fun navigateToQuestion(question: Question, allQuestions: List<Question>) {
        try {
            // Проверяем данные перед передачей
            if (allQuestions.isEmpty()) {
                throw IllegalStateException("Questions list is empty")
            }

            val args = bundleOf(
                "question" to question,
                "allQuestions" to Gson().toJson(allQuestions)
            )

            Log.d("NAVIGATION", "Navigating to question ${question.questionId}")
            findNavController().navigate(
                R.id.action_theoryFragment_to_questionFragment,
                args
            )
        } catch (e: Exception) {
            Log.e("NAV_ERROR", "Navigation failed", e)
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}