package com.drivingexam.theoryexam.data

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Question(
    @SerializedName("question_id") val questionId: String,
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("subcategory_id") val subcategoryId: String,
    @SerializedName("subcategory_name") val subcategoryName: String,
    val question: String,
    val points: String,
    val image: String? = null,
    var isAnswered: Boolean = false,
    @SerializedName("image_local") val imageLocal: String? = null,
    val choices: List<Choice>,
    @SerializedName("correct_ids") val correctIds: List<String> = emptyList()
    ) : Parcelable {
    fun getShuffledChoices(): List<Choice> {
        return choices.shuffled()
    }

    @Parcelize
    data class Choice(
        val id: String,
        val answer: String,
        @SerializedName("is_correct") val isCorrect: Boolean = false
    ) : Parcelable
}