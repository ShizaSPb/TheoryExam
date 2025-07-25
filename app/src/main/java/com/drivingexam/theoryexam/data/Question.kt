package com.drivingexam.theoryexam.data

import com.google.gson.annotations.SerializedName

data class Question(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("subcategory_id") val subcategoryId: String,
    @SerializedName("subcategory_name") val subcategoryName: String,
    val question: String,
    val points: String,
    val image: String?,
    @SerializedName("image_local") val imageLocal: String?,
    val choices: List<Choice>,
    @SerializedName("correct_ids") val correctIds: List<String>
)

data class Choice(
    val id: String,
    val answer: String,
    @SerializedName("is_correct") val isCorrect: Boolean
)