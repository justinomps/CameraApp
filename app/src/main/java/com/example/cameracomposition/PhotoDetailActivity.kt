package com.example.cameracomposition

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.example.cameracomposition.databinding.ActivityPhotoDetailBinding
import java.io.File

class PhotoDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // This line enables the "Up" button (back arrow) in the app bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val photoPath = intent.getStringExtra("photo_path")
        if (photoPath != null) {
            val photoFile = File(photoPath)
            binding.fullScreenImageView.setImageURI(Uri.fromFile(photoFile))
        }

        // The back button in your layout is now redundant, but we'll leave it for now.
        // The new app bar back button is the standard way to handle this.
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    // This function handles clicks on the app bar, including the back button.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
