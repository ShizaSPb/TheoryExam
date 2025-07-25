package com.drivingexam.theoryexam.ui.theory

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.drivingexam.theoryexam.data.QuestionData
import com.drivingexam.theoryexam.databinding.FragmentTheoryBinding
import com.drivingexam.theoryexam.ui.theory.adapters.CategoriesAdapter
import com.drivingexam.theoryexam.ui.theory.adapters.SubcategoriesAdapter
import kotlinx.coroutines.launch

class TheoryFragment : Fragment() {
    private var _binding: FragmentTheoryBinding? = null
    private val binding get() = _binding!!

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

        with(binding) {
            rvCategories.layoutManager = LinearLayoutManager(requireContext())
            rvSubcategories.layoutManager = LinearLayoutManager(requireContext())

            lifecycleScope.launch {
                val questionsMap = QuestionData.loadQuestions(requireContext())
                setupAdapters(questionsMap)
            }
        }
    }

    private fun setupAdapters(questionsMap: Map<String, Map<String, List<com.drivingexam.theoryexam.data.Question>>>) {
        binding.rvCategories.adapter = CategoriesAdapter(
            categories = questionsMap.keys.toList(),
            onClick = { category ->
                questionsMap[category]?.let { subcategories ->
                    binding.rvSubcategories.adapter = SubcategoriesAdapter(
                        subcategories = subcategories.keys.toList(),
                        onSubcategoryClick = { /* Обработка выбора */ }
                    )
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}