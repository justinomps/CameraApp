package com.example.cameracomposition

import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cameracomposition.databinding.ItemPhotoBinding
import java.io.File

class PhotoAdapter(private val photoFiles: List<File>) :
    RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoFile = photoFiles[position]
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        holder.binding.photoImageView.setImageBitmap(bitmap)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, PhotoDetailActivity::class.java).apply {
                putExtra("photo_path", photoFile.absolutePath)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return photoFiles.size
    }
}
