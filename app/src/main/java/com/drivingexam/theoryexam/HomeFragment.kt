package com.drivingexam.theoryexam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController


class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Для findViewById нужен импорт android.view.View
        view.findViewById<Button>(R.id.btn_theory).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_theory)
        }

        view.findViewById<Button>(R.id.btn_exam).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_exam)
        }

        return view
    }
}