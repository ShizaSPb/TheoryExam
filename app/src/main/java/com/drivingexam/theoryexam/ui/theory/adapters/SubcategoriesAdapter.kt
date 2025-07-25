package com.drivingexam.theoryexam.ui.theory.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.drivingexam.theoryexam.R

class SubcategoriesAdapter(
    private val subcategories: List<String>,
    private val onSubcategoryClick: (String) -> Unit
) : RecyclerView.Adapter<SubcategoriesAdapter.ViewHolder>() {

    // ViewHolder для кэширования элементов представления
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvSubcategoryTitle)
    }

    // Создание новых ViewHolder'ов
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subcategory, parent, false)
        return ViewHolder(view)
    }

    // Заполнение данных в элементы
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = subcategories[position]
        holder.itemView.setOnClickListener {
            onSubcategoryClick(subcategories[position])
        }
    }

    // Общее количество элементов
    override fun getItemCount(): Int = subcategories.size
}