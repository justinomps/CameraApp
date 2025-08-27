package com.example.cameracomposition

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cameracomposition.databinding.ActivityGalleryBinding
import java.io.File

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding

    companion object {
        private const val REQUEST_CODE_READ_STORAGE = 20
        // Select the correct permission based on the Android version
        private val REQUIRED_PERMISSION =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        // Check for storage permission before loading photos.
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
            Toast.makeText(this, "No photos found.", Toast.LENGTH_SHORT).show()
        }
        setupRecyclerView(photoFiles)
    }

    private fun loadPhotosFromGallery(): List<File> {
        val galleryDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "FormatApp-Images"
        )
        if (!galleryDir.exists()) {
            return emptyList()
        }

        return galleryDir.listFiles { file ->
            file.isFile && (file.extension.equals("jpg", true) || file.extension.equals("jpeg", true))
        }?.sortedDescending() ?: emptyList()
    }

    private fun setupRecyclerView(photoFiles: List<File>) {
        val layoutManager = GridLayoutManager(this, 3)
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
                finish() // Close the gallery if permission is denied.
            }
        }
    }
}
