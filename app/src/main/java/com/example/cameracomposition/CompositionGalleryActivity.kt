package com.example.cameracomposition

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cameracomposition.databinding.ActivityCompositionGalleryBinding
import java.io.File

class CompositionGalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompositionGalleryBinding

    companion object {
        private const val REQUEST_CODE_READ_STORAGE = 21
        private val REQUIRED_PERMISSION =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompositionGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        if (isPermissionGranted()) {
            loadAndDisplayPhotos()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(REQUIRED_PERMISSION), REQUEST_CODE_READ_STORAGE)
        }
    }

    private fun isPermissionGranted() = ContextCompat.checkSelfPermission(
        this, REQUIRED_PERMISSION
    ) == PackageManager.PERMISSION_GRANTED

    private fun loadAndDisplayPhotos() {
        val photoFiles = loadPhotosFromGallery()
        if (photoFiles.isEmpty()) {
            Toast.makeText(this, "No composition photos found.", Toast.LENGTH_SHORT).show()
        }
        setupRecyclerView(photoFiles)
    }

    private fun loadPhotosFromGallery(): List<File> {
        val baseGalleryDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "FormatApp-Images/Composition"
        )

        if (!baseGalleryDir.exists()) {
            return emptyList()
        }

        return baseGalleryDir.listFiles { file ->
            file.isFile && (file.extension.equals("jpg", true) || file.extension.equals("jpeg", true))
        }?.sortedDescending() ?: emptyList()
    }

    private fun setupRecyclerView(photoFiles: List<File>) {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val spanCount = if (isLandscape) 5 else 3
        val layoutManager = GridLayoutManager(this, spanCount)
        binding.photoGridRecyclerview.layoutManager = layoutManager
        val adapter = PhotoAdapter(photoFiles)
        binding.photoGridRecyclerview.adapter = adapter
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_READ_STORAGE) {
            if (isPermissionGranted()) {
                loadAndDisplayPhotos()
            } else {
                Toast.makeText(this, "Storage permission is required to view the gallery.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}