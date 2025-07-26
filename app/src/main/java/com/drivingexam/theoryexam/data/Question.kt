package com.drivingexam.theoryexam.data

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
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
) : Parcelable

@Parcelize
data class Choice(
    val id: String,
    val answer: String,
    @SerializedName("is_correct") val isCorrect: Boolean
) : Parcelable