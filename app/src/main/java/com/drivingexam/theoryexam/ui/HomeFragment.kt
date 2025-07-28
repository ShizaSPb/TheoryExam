package com.drivingexam.theoryexam.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.drivingexam.theoryexam.R
import com.drivingexam.theoryexam.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!! // Получаем доступ к binding только когда view существует

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnTheory.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_theory)
        }

        binding.btnExam.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_exam)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Важно обнулять binding при уничтожении view
    }
}