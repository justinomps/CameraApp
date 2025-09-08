package com.example.cameracomposition

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.TextPaint
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cameracomposition.databinding.ActivityGalleryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private var filmRollId: String? = null
    private var photoFiles: List<File> = emptyList()


    companion object {
        private const val REQUEST_CODE_READ_STORAGE = 20
        private val REQUIRED_PERMISSION =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        private const val PREFS_NAME = "FormatAppPrefs"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.shareButton.setOnClickListener {
            shareContactSheet()
        }

        filmRollId = intent.getStringExtra("filmRollId")
        if (filmRollId == null) {
            Toast.makeText(this, "Film roll not specified.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.galleryTitle.text = "Roll #$filmRollId"

        if (isPermissionGranted()) {
            loadAndDisplayPhotos()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(REQUIRED_PERMISSION), REQUEST_CODE_READ_STORAGE)
        }
    }

    private fun isPermissionGranted() = ContextCompat.checkSelfPermission(
        this, REQUIRED_PERMISSION
    ) == PackageManager.PERMISSION_GRANTED

    private fun loadAndDisplayPhotos() {
        lifecycleScope.launch(Dispatchers.IO) {
            photoFiles = loadPhotosFromGallery()
            withContext(Dispatchers.Main) {
                if (photoFiles.isEmpty()) {
                    Toast.makeText(this@GalleryActivity, "No photos found in this roll.", Toast.LENGTH_SHORT).show()
                }
                setupRecyclerView(photoFiles)
            }
        }
    }

    private fun loadPhotosFromGallery(): List<File> {
        val baseGalleryDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "FormatApp-Images"
        )

        val filmRollDir = File(baseGalleryDir, filmRollId)

        if (!filmRollDir.exists()) {
            return emptyList()
        }

        return filmRollDir.listFiles { file ->
            file.isFile && (file.extension.equals("jpg", true) || file.extension.equals("jpeg", true))
        }?.sortedDescending() ?: emptyList()
    }

    private fun loadFilmRollAspectRatio(): String {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("ratio_$filmRollId", "4:3") ?: "4:3"
    }

    private fun getSpanCount(): Int {
        val aspectRatio = loadFilmRollAspectRatio()
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        return when (aspectRatio) {
            "6x6" -> if (isLandscape) 5 else 3
            "6x7" -> if (isLandscape) 4 else 3
            "6x9" -> if (isLandscape) 3 else 2
            "6x12" -> if (isLandscape) 2 else 1
            else -> if (isLandscape) 5 else 3
        }
    }


    private fun setupRecyclerView(photoFiles: List<File>) {
        val spanCount = getSpanCount()
        val layoutManager = GridLayoutManager(this, spanCount)
        binding.photoGridRecyclerview.layoutManager = layoutManager
        val adapter = PhotoAdapter(photoFiles)
        binding.photoGridRecyclerview.adapter = adapter
    }

    private fun shareContactSheet() {
        if (photoFiles.isEmpty()) {
            Toast.makeText(this, "No photos to share.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            binding.loadingIndicator.visibility = View.VISIBLE
            binding.shareButton.isEnabled = false

            val contentUri = withContext(Dispatchers.IO) {
                createContactSheetImage()
            }

            binding.loadingIndicator.visibility = View.GONE
            binding.shareButton.isEnabled = true

            if (contentUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    type = "image/jpeg"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share contact sheet via"))
            } else {
                Toast.makeText(this@GalleryActivity, "Failed to share contact sheet.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createContactSheetImage(): Uri? {
        // --- Configuration ---
        val cols = getSpanCount()
        val rows = (photoFiles.size + cols - 1) / cols
        val thumbWidth = 200
        val thumbHeight = 200
        val padding = 20
        val headerHeight = 100

        val totalWidth = (thumbWidth * cols) + (padding * (cols + 1))
        val totalHeight = (thumbHeight * rows) + (padding * (rows + 1)) + headerHeight

        return try {
            val resultBitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)
            canvas.drawColor(Color.BLACK)

            // --- Draw Header ---
            val titlePaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 40f
                isAntiAlias = true
            }
            val title = "Roll #$filmRollId"
            val textBounds = Rect()
            titlePaint.getTextBounds(title, 0, title.length, textBounds)
            canvas.drawText(title, (totalWidth - textBounds.width()) / 2f, (headerHeight + textBounds.height()) / 2f, titlePaint)

            // --- Draw Thumbnails ---
            photoFiles.forEachIndexed { index, file ->
                val row = index / cols
                val col = index % cols

                val left = padding + col * (thumbWidth + padding)
                val top = headerHeight + padding + row * (thumbHeight + padding)

                val thumbBitmap = decodeSampledBitmapFromFile(file, thumbWidth, thumbHeight)
                if (thumbBitmap != null) {
                    canvas.drawBitmap(thumbBitmap, left.toFloat(), top.toFloat(), null)
                    thumbBitmap.recycle() // Free memory immediately
                }
            }

            // --- Save to File ---
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "contact_sheet_share.jpg")
            val stream = FileOutputStream(file)
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.close()
            resultBitmap.recycle()
            FileProvider.getUriForFile(this@GalleryActivity, "${applicationContext.packageName}.provider", file)
        } catch (e: Exception) {
            null
        }
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


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_READ_STORAGE) {
            if (isPermissionGranted()) {
                loadAndDisplayPhotos()
            } else {
                Toast.makeText(this, "Storage permission is required to view the gallery.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}

