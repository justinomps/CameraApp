package com.example.cameracomposition

import android.content.Intent
import android.graphics.Bitmap
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
        val bitmap = decodeSampledBitmapFromFile(photoFile, 150, 150) // Load a small thumbnail
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

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun decodeSampledBitmapFromFile(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            BitmapFactory.Options().run {
                inJustDecodeBounds = true
                BitmapFactory.decodeFile(file.absolutePath, this)
                inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.RGB_565
                BitmapFactory.decodeFile(file.absolutePath, this)
            }
        } catch (e: Exception) {
            null
        }
    }
}
