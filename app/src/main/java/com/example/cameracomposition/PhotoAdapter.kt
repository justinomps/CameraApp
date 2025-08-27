package com.example.cameracomposition

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class PhotoAdapter(private val photoFiles: List<File>) :
    RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    /**
     * The ViewHolder holds the views for each individual item in the grid.
     */
    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.photo_image_view)
    }

    /**
     * This function is called when the RecyclerView needs a new ViewHolder.
     * It inflates the item_photo.xml layout for each item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    /**
     * This function is called to display the data at a specific position.
     * It takes a photo file and sets it as the image for the ImageView.
     */
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoFile = photoFiles[position]
        holder.imageView.setImageURI(Uri.fromFile(photoFile))
    }

    /**
     * This function returns the total number of items in the list.
     */
    override fun getItemCount(): Int {
        return photoFiles.size
    }
}
