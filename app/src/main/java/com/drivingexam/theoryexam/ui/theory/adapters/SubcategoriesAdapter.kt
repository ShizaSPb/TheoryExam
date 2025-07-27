package com.drivingexam.theoryexam.ui.theory.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.drivingexam.theoryexam.R

class SubcategoriesAdapter(
    private var subcategories: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SubcategoriesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvSubcategoryTitle)
    }

    fun updateData(newSubcategories: List<String>) {
        subcategories = newSubcategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subcategory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = subcategories[position]
        holder.itemView.setOnClickListener {
            onClick(subcategories[position])
        }
    }

    override fun getItemCount() = subcategories.size
}