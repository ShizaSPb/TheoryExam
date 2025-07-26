package com.drivingexam.theoryexam.ui.theory

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.drivingexam.theoryexam.R
import com.drivingexam.theoryexam.data.Question
import com.drivingexam.theoryexam.databinding.FragmentTheoryBinding
import com.drivingexam.theoryexam.ui.theory.adapters.CategoriesAdapter
import com.drivingexam.theoryexam.ui.theory.adapters.SubcategoriesAdapter
import kotlinx.coroutines.launch
import com.drivingexam.theoryexam.data.QuestionData

class TheoryFragment : Fragment() {
    private var _binding: FragmentTheoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var questionsMap: Map<String, Map<String, List<Question>>>

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
                questionsMap = QuestionData.loadQuestions(requireContext())
                setupAdapters()
            }
        }
    }

    private fun setupAdapters() {
        binding.rvCategories.adapter = CategoriesAdapter(
            categories = questionsMap.keys.toList(),
            onClick = { category ->
                questionsMap[category]?.let { subcategories ->
                    binding.rvSubcategories.adapter = SubcategoriesAdapter(
                        subcategories = subcategories.keys.toList(),
                        onSubcategoryClick = { subcategory ->
                            val questions = questionsMap[category]?.get(subcategory) ?: emptyList()
                            navigateToQuestionList(questions)
                        }
                    )
                }
            }
        )
    }

    private fun navigateToQuestionList(questions: List<Question>) {
        val bundle = Bundle().apply {
            putParcelableArray("questions", questions.toTypedArray())
        }
        findNavController().navigate(
            R.id.action_theoryFragment_to_questionListFragment,
            bundle
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}