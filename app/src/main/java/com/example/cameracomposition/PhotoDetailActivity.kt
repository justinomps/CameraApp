package com.example.cameracomposition

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.FileProvider
import com.example.cameracomposition.databinding.ActivityPhotoDetailBinding
import com.google.gson.Gson
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class PhotoDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoDetailBinding
    private var photoPath: String? = null
    private var isFavorited = false
    private var metadata: PhotoMetadata? = null

    companion object {
        private const val PREFS_NAME = "FormatAppPrefs"
        private const val KEY_FAVORITES = "favoritePhotos"
        private const val KEY_METADATA_PREFIX = "metadata_"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Photo Detail"

        photoPath = intent.getStringExtra("photo_path")
        if (photoPath != null) {
            val photoFile = File(photoPath)
            binding.fullScreenImageView.setImageURI(Uri.fromFile(photoFile))
            checkIfFavorited()
            loadMetadata()
        }
    }

    override fun onStop() {
        super.onStop()
        saveNotes()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.photo_detail_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val favoriteItem = menu?.findItem(R.id.action_favorite)
        if (isFavorited) {
            favoriteItem?.setIcon(R.drawable.ic_star_filled)
        } else {
            favoriteItem?.setIcon(R.drawable.ic_star_outline)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_share -> {
                sharePhoto()
                true
            }
            R.id.action_favorite -> {
                toggleFavorite()
                true
            }
            R.id.action_notebook -> {
                toggleNotebookVisibility()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadMetadata() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString("$KEY_METADATA_PREFIX$photoPath", null)
        if (json != null) {
            metadata = gson.fromJson(json, PhotoMetadata::class.java)
        }
        displayMetadata()
    }

    private fun displayMetadata() {
        val iso = metadata?.iso ?: 0
        val aperture = metadata?.aperture ?: 0f
        val shutterSpeed = metadata?.shutterSpeed ?: 0L
        val aspectRatio = metadata?.aspectRatio ?: "N/A"

        val isoString = if (iso > 0) "ISO: $iso" else "ISO: N/A"
        val apertureFormatted = if (aperture > 0) "f/$aperture" else "f/N/A"
        val shutterSpeedFormatted = formatShutterSpeed(shutterSpeed)

        val metadataString = "Format: $aspectRatio • $isoString • $apertureFormatted • $shutterSpeedFormatted"
        binding.metadataText.text = metadataString
        binding.notesEditText.setText(metadata?.notes ?: "")
    }

    private fun saveNotes() {
        val newNotes = binding.notesEditText.text.toString()

        // If metadata doesn't exist, create a default one to save the notes
        if (metadata == null && photoPath != null) {
            metadata = PhotoMetadata(photoPath!!, "N/A", 0, 0f, 0L)
        }

        metadata?.let {
            it.notes = newNotes

            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val gson = Gson()
            val json = gson.toJson(it)
            editor.putString("$KEY_METADATA_PREFIX${it.imagePath}", json)
            editor.apply()
        }
    }

    private fun formatShutterSpeed(nanos: Long): String {
        if (nanos <= 0L) return "N/A"
        val seconds = nanos / 1_000_000_000.0
        return if (seconds >= 1.0) {
            "${seconds}\""
        } else {
            "1/${(1.0 / seconds).roundToInt()}"
        }
    }

    private fun toggleNotebookVisibility() {
        binding.notebookLayout.visibility = if (binding.notebookLayout.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun checkIfFavorited() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val favorites = prefs.getStringSet(KEY_FAVORITES, setOf()) ?: setOf()
        isFavorited = favorites.contains(photoPath)
        invalidateOptionsMenu()
    }

    private fun toggleFavorite() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val favorites = prefs.getStringSet(KEY_FAVORITES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        if (isFavorited) {
            favorites.remove(photoPath)
        } else {
            photoPath?.let { favorites.add(it) }
        }

        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
        isFavorited = !isFavorited
        invalidateOptionsMenu()
    }

    private fun sharePhoto() {
        photoPath?.let { path ->
            val photoFile = File(path)
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                photoFile
            )

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, photoURI)
                type = "image/jpeg"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share photo via"))
        }
    }
}