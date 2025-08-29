package com.example.cameracomposition

import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cameracomposition.databinding.ActivityRollsBinding
import java.io.File

class RollsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRollsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRollsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        val developedRolls = getDevelopedRolls()
        if (developedRolls.isEmpty()) {
            Toast.makeText(this, "No developed rolls found.", Toast.LENGTH_SHORT).show()
        }

        binding.rollsRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.rollsRecyclerview.adapter = RollsAdapter(developedRolls)
    }

    private fun getDevelopedRolls(): List<File> {
        val baseGalleryDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "FormatApp-Images"
        )
        if (!baseGalleryDir.exists()) {
            return emptyList()
        }

        // CHANGE: Filter out the "Composition" directory
        return baseGalleryDir.listFiles { file ->
            file.isDirectory && file.name != "Composition"
        }?.sortedDescending() ?: emptyList()
    }
}