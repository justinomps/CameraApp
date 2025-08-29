package com.example.cameracomposition

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.FileProvider
import com.example.cameracomposition.databinding.ActivityPhotoDetailBinding
import java.io.File

class PhotoDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoDetailBinding
    private var photoPath: String? = null
    private var isFavorited = false

    companion object {
        private const val PREFS_NAME = "FormatAppPrefs"
        private const val KEY_FAVORITES = "favoritePhotos"
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
        }
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
            else -> super.onOptionsItemSelected(item)
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
        invalidateOptionsMenu() // This redraws the menu to update the icon
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