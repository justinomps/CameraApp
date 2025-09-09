package com.example.cameracomposition

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cameracomposition.databinding.ItemTutorialBinding

class TutorialAdapter(private val lessons: List<TutorialLesson>) :
    RecyclerView.Adapter<TutorialAdapter.TutorialViewHolder>() {

    inner class TutorialViewHolder(val binding: ItemTutorialBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialViewHolder {
        val binding =
            ItemTutorialBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TutorialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TutorialViewHolder, position: Int) {
        val lesson = lessons[position]
        holder.binding.tutorialTitleTextview.text = lesson.title

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, TutorialDetailActivity::class.java).apply {
                putExtra(TutorialDetailActivity.EXTRA_LESSON, lesson)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = lessons.size
}
