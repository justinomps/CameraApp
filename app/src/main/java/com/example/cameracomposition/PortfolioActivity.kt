package com.example.cameracomposition

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cameracomposition.databinding.ActivityPortfolioBinding
import java.io.File

class PortfolioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPortfolioBinding

    companion object {
        private const val PREFS_NAME = "FormatAppPrefs"
        private const val KEY_FAVORITES = "favoritePhotos"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPortfolioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadAndDisplayPhotos()
    }

    private fun loadAndDisplayPhotos() {
        val photoFiles = loadFavoritePhotos()
        if (photoFiles.isEmpty()) {
            Toast.makeText(this, "No favorite photos yet.", Toast.LENGTH_SHORT).show()
        }
        setupRecyclerView(photoFiles)
    }

    private fun loadFavoritePhotos(): List<File> {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val favorites = prefs.getStringSet(KEY_FAVORITES, setOf()) ?: setOf()
        return favorites.map { File(it) }.filter { it.exists() }.sortedDescending()
    }

    private fun setupRecyclerView(photoFiles: List<File>) {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val spanCount = if (isLandscape) 4 else 2 // Different layout for portfolio
        val layoutManager = GridLayoutManager(this, spanCount)
        binding.photoGridRecyclerview.layoutManager = layoutManager
        val adapter = PhotoAdapter(photoFiles)
        binding.photoGridRecyclerview.adapter = adapter
    }
}