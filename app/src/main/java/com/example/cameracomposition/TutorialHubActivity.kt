package com.example.cameracomposition

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cameracomposition.databinding.ActivityTutorialHubBinding

class TutorialHubActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorialHubBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialHubBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val lessons = listOf(
            TutorialLesson(
                title = "Tutorial 1: The Rule of Thirds",
                conceptExplanation = "The Rule of Thirds is a fundamental principle of composition. Imagine your frame is divided into nine equal segments by two vertical and two horizontal lines. The rule suggests that placing key elements of your scene along these lines or at their intersections creates a more balanced, dynamic, and engaging photograph than simply centering the subject.",
                masterName = "Henri Cartier-Bresson",
                masterBio = "A French humanist photographer, Cartier-Bresson is considered a master of candid photography. He was a pioneer of street photography and famously used the Rule of Thirds to create impeccably composed, decisive moments.",
                masterImageResId = R.drawable.henri_cartier_bresson,
                practiceGridType = OverlayView.GridType.THIRDS_INTERSECTIONS,
                challengeName = "Rule of Thirds"
            )
            // Future lessons will be added here
        )

        binding.tutorialsRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.tutorialsRecyclerview.adapter = TutorialAdapter(lessons)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
