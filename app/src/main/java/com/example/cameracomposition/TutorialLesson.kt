package com.example.cameracomposition

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class TutorialLesson(
    val title: String,
    val conceptExplanation: String,
    val masterName: String,
    val masterBio: String,
    @DrawableRes val masterImageResId: Int,
    val practiceGridType: OverlayView.GridType,
    val challengeName: String
) : Parcelable
