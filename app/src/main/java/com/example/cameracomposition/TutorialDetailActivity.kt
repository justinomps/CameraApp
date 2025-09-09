package com.example.cameracomposition

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.cameracomposition.databinding.ActivityTutorialDetailBinding

class TutorialDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorialDetailBinding
    private var lesson: TutorialLesson? = null

    companion object {
        const val EXTRA_LESSON = "extra_lesson"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lesson = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_LESSON, TutorialLesson::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_LESSON)
        }

        lesson?.let {
            setupUI(it)
        } ?: finish() // If lesson is null, something went wrong.
    }

    private fun setupUI(lesson: TutorialLesson) {
        binding.titleTextview.text = lesson.title
        binding.explanationTextview.text = lesson.conceptExplanation
        binding.masterNameTextview.text = lesson.masterName
        binding.masterBioTextview.text = lesson.masterBio
        binding.masterImage.setImageResource(lesson.masterImageResId)

        binding.practiceButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_PRACTICE
                putExtra(MainActivity.EXTRA_PRACTICE_GRID_TYPE, lesson.practiceGridType)
            }
            startActivity(intent)
        }

        binding.challengeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_CHALLENGE
                putExtra(MainActivity.EXTRA_CHALLENGE_NAME, lesson.challengeName)
                putExtra(MainActivity.EXTRA_PRACTICE_GRID_TYPE, lesson.practiceGridType)
            }
            startActivity(intent)
        }
    }
}
