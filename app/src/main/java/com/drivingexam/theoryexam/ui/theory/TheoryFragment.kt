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

        // Инициализация с пустыми списками
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
        // 1. Проверяем, что категория выбрана
        val category = selectedCategory ?: run {
            Log.e("Navigation", "No category selected when choosing subcategory")
            Toast.makeText(
                requireContext(),
                "Ошибка: категория не выбрана",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 2. Получаем вопросы для выбранной подкатегории
        val questions = try {
            viewModel.getQuestionsForSubcategory(category, subcategory)
        } catch (e: Exception) {
            Log.e("Navigation", "Error getting questions: ${e.message}")
            Toast.makeText(
                requireContext(),
                "Ошибка загрузки вопросов",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 3. Проверяем, что список вопросов не пустой
        if (questions.isEmpty()) {
            Log.e("Navigation", "Empty questions list for $category/$subcategory")
            Toast.makeText(
                requireContext(),
                "В этой подкатегории пока нет вопросов",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 4. Навигируем к первому вопросу
        try {
            navigateToQuestion(questions.first(), questions)
        } catch (e: Exception) {
            Log.e("Navigation", "Failed to navigate to question: ${e.message}")
            Toast.makeText(
                requireContext(),
                "Ошибка перехода к вопросу",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun navigateToQuestion(question: Question, allQuestions: List<Question>) {
        try {
            val gson = Gson()
            val args = bundleOf(
                "question" to question,
                "allQuestions" to gson.toJson(allQuestions)
            )
            findNavController().navigate(
                R.id.action_theoryFragment_to_questionFragment,
                args
            )
        } catch (e: Exception) {
            Log.e("NAV_DEBUG", "Navigation failed", e)
            Toast.makeText(
                requireContext(),
                "Ошибка перехода: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}