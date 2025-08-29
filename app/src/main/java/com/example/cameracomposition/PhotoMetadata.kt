package com.example.cameracomposition

data class PhotoMetadata(
    val imagePath: String,
    val aspectRatio: String,
    val iso: Int,
    val aperture: Float,
    val shutterSpeed: Long,
    var notes: String = ""
)