package com.example.cameracomposition

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.cameracomposition.databinding.ActivityNotebookBinding

class NotebookActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotebookBinding

    companion object {
        private const val PREFS_NAME = "FormatAppPrefs"
        private const val KEY_NOTEBOOK = "notebookContent"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotebookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        loadNotes()
    }

    override fun onPause() {
        super.onPause()
        saveNotes()
    }

    private fun loadNotes() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedNotes = prefs.getString(KEY_NOTEBOOK, "")
        binding.notebookEditText.setText(savedNotes)
    }

    private fun saveNotes() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentNotes = binding.notebookEditText.text.toString()
        prefs.edit().putString(KEY_NOTEBOOK, currentNotes).apply()
    }
}