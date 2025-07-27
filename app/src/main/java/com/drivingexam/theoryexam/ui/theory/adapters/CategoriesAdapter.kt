package com.drivingexam.theoryexam.ui.theory.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.drivingexam.theoryexam.R

class CategoriesAdapter(
    private var categories: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<CategoriesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvCategoryTitle)
    }

    fun updateData(newCategories: List<String>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = categories[position]
        holder.itemView.setOnClickListener {
            onClick(categories[position])
        }
    }

    override fun getItemCount() = categories.size
}