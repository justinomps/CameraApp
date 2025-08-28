package com.example.cameracomposition

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class RollsAdapter(private val rolls: List<File>) :
    RecyclerView.Adapter<RollsAdapter.RollViewHolder>() {

    class RollViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rollIdTextView: TextView = itemView.findViewById(R.id.roll_id_textview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RollViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_roll, parent, false)
        return RollViewHolder(view)
    }

    override fun onBindViewHolder(holder: RollViewHolder, position: Int) {
        val rollDirectory = rolls[position]
        holder.rollIdTextView.text = "Roll #${rollDirectory.name}"

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, GalleryActivity::class.java).apply {
                putExtra("filmRollId", rollDirectory.name)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = rolls.size
}
