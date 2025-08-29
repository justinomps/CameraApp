package com.example.cameracomposition

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // This enables the "Up" button (back arrow) in the app bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Photo Detail" // Optional: Set a title

        photoPath = intent.getStringExtra("photo_path")
        if (photoPath != null) {
            val photoFile = File(photoPath)
            binding.fullScreenImageView.setImageURI(Uri.fromFile(photoFile))
        }
    }

    // This function inflates the menu, adding the share icon to the ActionBar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.photo_detail_menu, menu)
        return true
    }

    // This function handles clicks on ActionBar items, including the share and back buttons
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // This ID is for the standard back arrow
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_share -> {
                // This is the ID of our share item from the menu file
                sharePhoto()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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