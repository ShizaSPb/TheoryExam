package com.drivingexam.theoryexam.ui.theory.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.drivingexam.theoryexam.R
import com.drivingexam.theoryexam.data.Question

class QuestionsAdapter(
    private val questions: List<Question>,
    private val onQuestionClick: (Question) -> Unit
) : RecyclerView.Adapter<QuestionsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val questionText: TextView = view.findViewById(R.id.tvQuestionText)
        val pointsText: TextView = view.findViewById(R.id.tvPoints)

        fun bind(question: Question) {
            questionText.text = question.question
            pointsText.text = "Баллы: ${question.points}"
            itemView.setOnClickListener { onQuestionClick(question) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(questions[position])
    }

    override fun getItemCount(): Int = questions.size
}