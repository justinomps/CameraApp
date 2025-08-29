package com.example.cameracomposition

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cameracomposition.databinding.ActivityGalleryBinding
import java.io.File

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private var filmRollId: String? = null
    private var photoFiles: List<File> = emptyList()


    companion object {
        private const val REQUEST_CODE_READ_STORAGE = 20
        private val REQUIRED_PERMISSION =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        private const val PREFS_NAME = "FormatAppPrefs"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.shareButton.setOnClickListener {
            shareContactSheet()
        }

        filmRollId = intent.getStringExtra("filmRollId")
        if (filmRollId == null) {
            Toast.makeText(this, "Film roll not specified.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.galleryTitle.text = "Roll #$filmRollId"

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
        photoFiles = loadPhotosFromGallery()
        if (photoFiles.isEmpty()) {
            Toast.makeText(this, "No photos found in this roll.", Toast.LENGTH_SHORT).show()
        }
        setupRecyclerView(photoFiles)
    }

    private fun loadPhotosFromGallery(): List<File> {
        val baseGalleryDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "FormatApp-Images"
        )

        val filmRollDir = File(baseGalleryDir, filmRollId)

        if (!filmRollDir.exists()) {
            return emptyList()
        }

        return filmRollDir.listFiles { file ->
            file.isFile && (file.extension.equals("jpg", true) || file.extension.equals("jpeg", true))
        }?.sortedDescending() ?: emptyList()
    }

    private fun loadFilmRollAspectRatio(): String {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("ratio_$filmRollId", "4:3") ?: "4:3"
    }

    private fun setupRecyclerView(photoFiles: List<File>) {
        val aspectRatio = loadFilmRollAspectRatio()
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val spanCount = when (aspectRatio) {
            "6x6" -> if (isLandscape) 5 else 3
            "6x7" -> if (isLandscape) 4 else 3
            "6x9" -> if (isLandscape) 3 else 2
            "6x12" -> if (isLandscape) 2 else 1
            else -> if (isLandscape) 5 else 3
        }

        val layoutManager = GridLayoutManager(this, spanCount)
        binding.photoGridRecyclerview.layoutManager = layoutManager
        val adapter = PhotoAdapter(photoFiles)
        binding.photoGridRecyclerview.adapter = adapter
    }

    private fun shareContactSheet() {
        if (photoFiles.isEmpty()) {
            Toast.makeText(this, "No photos to share.", Toast.LENGTH_SHORT).show()
            return
        }

        val imageUris = ArrayList<Uri>()
        for (file in photoFiles) {
            val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
            imageUris.add(uri)
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share contact sheet via"))
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