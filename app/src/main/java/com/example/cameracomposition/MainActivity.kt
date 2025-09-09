package com.example.cameracomposition

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.example.cameracomposition.databinding.ActivityMainBinding
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private enum class ShootingMode {
        PRACTICE, FILM, CHALLENGE
    }

    private var currentMode = ShootingMode.PRACTICE
    private var shotsTaken = 0
    private var rollCapacity = 12
    private var lastDevelopedRollId: String? = null
    private var challengeName: String? = null


    private val aspectRatios = OverlayView.AspectRatio.values()
    private var currentRatioIndex = 0
    private val gridTypes = OverlayView.GridType.values()
    private var currentGridIndex = 1

    companion object {
        private const val TAG = "FormatApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            } else {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        private const val PREFS_NAME = "FormatAppPrefs"
        private const val KEY_SHOTS_TAKEN = "shotsTaken"
        private const val KEY_SHOOTING_MODE = "shootingMode"
        private const val KEY_LAST_ROLL_ID = "lastDevelopedRollId"
        private const val KEY_RATIO_INDEX = "ratioIndex"
        private const val KEY_METADATA_PREFIX = "metadata_"

        const val ACTION_PRACTICE = "com.example.cameracomposition.ACTION_PRACTICE"
        const val ACTION_CHALLENGE = "com.example.cameracomposition.ACTION_CHALLENGE"
        const val EXTRA_PRACTICE_GRID_TYPE = "extra_practice_grid_type"
        const val EXTRA_CHALLENGE_NAME = "extra_challenge_name"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        loadState()

        val savedRatio = aspectRatios[currentRatioIndex]
        viewBinding.overlayView.setAspectRatio(savedRatio)
        viewBinding.switchFormatButton.text = savedRatio.displayName

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Standard listeners
        viewBinding.shutterButton.setOnClickListener { takePhoto() }
        viewBinding.switchFormatButton.setOnClickListener { switchFormat() }
        viewBinding.gridButton.setOnClickListener { switchGrid() }
        viewBinding.developButton.setOnClickListener { developRoll() }
        viewBinding.tutorialButton.setOnClickListener {
            val intent = Intent(this, TutorialHubActivity::class.java)
            startActivity(intent)
        }
        viewBinding.galleryButton.setOnClickListener {
            val intent = if (currentMode == ShootingMode.PRACTICE) {
                Intent(this, CompositionGalleryActivity::class.java)
            } else {
                Intent(this, RollsActivity::class.java)
            }
            startActivity(intent)
        }

        viewBinding.portfolioButton.setOnClickListener {
            val intent = Intent(this, PortfolioActivity::class.java)
            startActivity(intent)
        }
        viewBinding.modeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val newMode = if (isChecked) ShootingMode.FILM else ShootingMode.PRACTICE
            if (newMode == currentMode) {
                return@setOnCheckedChangeListener
            }
            currentMode = newMode
            saveState()
            updateUiForMode()
        }
        viewBinding.filmCounterText.setOnLongClickListener {
            if (shotsTaken > 0) {
                showAbandonRollDialog()
            }
            true
        }


        handleIntent(intent)
        updateUiForMode()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_PRACTICE -> {
                val gridType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra(EXTRA_PRACTICE_GRID_TYPE, OverlayView.GridType::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra(EXTRA_PRACTICE_GRID_TYPE) as? OverlayView.GridType
                }
                gridType?.let {
                    currentMode = ShootingMode.PRACTICE // Ensure we're in practice mode
                    viewBinding.overlayView.setGridType(it)
                }
            }
            ACTION_CHALLENGE -> {
                challengeName = intent.getStringExtra(EXTRA_CHALLENGE_NAME)
                val gridType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra(EXTRA_PRACTICE_GRID_TYPE, OverlayView.GridType::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra(EXTRA_PRACTICE_GRID_TYPE) as? OverlayView.GridType
                }
                if (challengeName != null && gridType != null) {
                    currentMode = ShootingMode.CHALLENGE
                    shotsTaken = 0
                    viewBinding.overlayView.setGridType(gridType)
                }
            }
            else -> {
                // Default behavior, no special mode
                challengeName = null
            }
        }
    }


    private fun updateUiForMode() {
        viewBinding.modeSwitch.isChecked = currentMode == ShootingMode.FILM

        // Hide tutorial-specific UI by default
        viewBinding.tutorialPromptText.visibility = View.GONE
        viewBinding.tutorialButton.visibility = View.VISIBLE

        when (currentMode) {
            ShootingMode.PRACTICE -> {
                viewBinding.modeSwitch.text = "Composition Practice"
                viewBinding.galleryButton.visibility = View.VISIBLE
                viewBinding.galleryButton.setImageResource(R.drawable.ic_composition_gallery)
                viewBinding.filmCounterText.visibility = View.GONE
                viewBinding.developButton.visibility = View.GONE
                viewBinding.shutterButton.visibility = View.VISIBLE
                viewBinding.switchFormatButton.isEnabled = true
                viewBinding.gridButton.isEnabled = true
                viewBinding.modeSwitch.visibility = View.VISIBLE
                viewBinding.portfolioButton.visibility = View.VISIBLE
            }

            ShootingMode.FILM -> {
                viewBinding.modeSwitch.text = "Deliberate Shooting"
                viewBinding.galleryButton.visibility = View.VISIBLE
                viewBinding.galleryButton.setImageResource(R.drawable.ic_gallery)
                viewBinding.filmCounterText.visibility = View.VISIBLE
                viewBinding.filmCounterText.text = "$shotsTaken / $rollCapacity"
                viewBinding.tutorialButton.visibility = View.GONE
                viewBinding.modeSwitch.visibility = View.VISIBLE
                viewBinding.portfolioButton.visibility = View.VISIBLE

                val isRollStarted = shotsTaken > 0
                viewBinding.switchFormatButton.isEnabled = !isRollStarted
                viewBinding.gridButton.isEnabled = true

                if (shotsTaken >= rollCapacity) {
                    viewBinding.developButton.visibility = View.VISIBLE
                    viewBinding.shutterButton.visibility = View.GONE
                } else {
                    viewBinding.developButton.visibility = View.GONE
                    viewBinding.shutterButton.visibility = View.VISIBLE
                }
            }
            ShootingMode.CHALLENGE -> {
                viewBinding.filmCounterText.visibility = View.VISIBLE
                viewBinding.filmCounterText.text = "$challengeName Challenge: $shotsTaken / $rollCapacity"
                // Hide most controls during a challenge to keep the user focused
                viewBinding.switchFormatButton.isEnabled = false
                viewBinding.gridButton.isEnabled = false
                viewBinding.modeSwitch.visibility = View.GONE
                viewBinding.galleryButton.visibility = View.GONE
                viewBinding.portfolioButton.visibility = View.GONE
                viewBinding.tutorialButton.visibility = View.GONE
                viewBinding.developButton.visibility = View.GONE
                viewBinding.shutterButton.visibility = View.VISIBLE

                if (shotsTaken >= rollCapacity) {
                    // Challenge is over
                    Toast.makeText(this, "$challengeName Challenge Complete!", Toast.LENGTH_LONG).show()
                    finish() // Exit back to the detail screen
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewBinding.cameraPreview.surfaceProvider)
            }
            val targetSize =
                Size(viewBinding.cameraPreview.width, viewBinding.cameraPreview.height)
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetResolution(targetSize)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun switchFormat() {
        currentRatioIndex = (currentRatioIndex + 1) % aspectRatios.size
        val newRatio = aspectRatios[currentRatioIndex]
        viewBinding.overlayView.setAspectRatio(newRatio)
        viewBinding.switchFormatButton.text = newRatio.displayName
        saveState()
    }

    private fun switchGrid() {
        currentGridIndex = (currentGridIndex + 1) % gridTypes.size
        val newGridType = gridTypes[currentGridIndex]
        viewBinding.overlayView.setGridType(newGridType)
        viewBinding.gridButton.text = newGridType.displayName
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        triggerHapticFeedback()
        // playShutterSound() // Consider removing for less distraction in tutorial
        animateShutterFlash()

        val fileName = "photo_${System.currentTimeMillis()}.jpg"

        val outputFile = when (currentMode) {
            ShootingMode.FILM -> File(getDir("film_roll", Context.MODE_PRIVATE), fileName)
            ShootingMode.CHALLENGE -> {
                val mediaDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "FormatApp-Images/Challenges/$challengeName"
                )
                mediaDir.mkdirs()
                File(mediaDir, fileName)
            }
            else -> { // PRACTICE
                val mediaDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "FormatApp-Images/Composition"
                )
                mediaDir.mkdirs()
                File(mediaDir, fileName)
            }
        }


        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    savePhotoMetadata(outputFile.absolutePath)

                    val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
                    val rotatedBitmap = rotateBitmap(bitmap, outputFile.absolutePath)
                    val croppedBitmap = cropBitmap(rotatedBitmap)

                    try {
                        val out = FileOutputStream(outputFile)
                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                        out.flush()
                        out.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error overwriting file", e)
                    }

                    when (currentMode) {
                        ShootingMode.FILM -> {
                            shotsTaken++
                            saveState()
                            updateUiForMode()
                            Toast.makeText(baseContext, "Shot $shotsTaken taken.", Toast.LENGTH_SHORT).show()
                        }
                        ShootingMode.PRACTICE -> {
                            notifyMediaStore(outputFile, "Pictures/FormatApp-Images/Composition")
                            Toast.makeText(baseContext, "Practice shot saved.", Toast.LENGTH_SHORT).show()
                        }
                        ShootingMode.CHALLENGE -> {
                            shotsTaken++
                            notifyMediaStore(outputFile, "Pictures/FormatApp-Images/Challenges/$challengeName")
                            updateUiForMode()
                        }
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

    private fun notifyMediaStore(file: File, relativePath: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            } else {
                put(MediaStore.Images.Media.DATA, file.absolutePath)
            }
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    private fun savePhotoMetadata(filePath: String) {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val gson = Gson()

            val exif = ExifInterface(filePath)
            val iso = exif.getAttributeInt(ExifInterface.TAG_ISO_SPEED_RATINGS, 0)

            val shutterSpeedString = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
            val shutterSpeedSeconds = shutterSpeedString?.toFloatOrNull() ?: 0f
            val shutterSpeedNanos = (shutterSpeedSeconds * 1_000_000_000).toLong()

            val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.toFloatOrNull() ?: 0f

            val metadata = PhotoMetadata(
                imagePath = filePath,
                aspectRatio = aspectRatios[currentRatioIndex].displayName,
                iso = iso,
                aperture = aperture,
                shutterSpeed = shutterSpeedNanos
            )

            val json = gson.toJson(metadata)
            editor.putString("$KEY_METADATA_PREFIX$filePath", json)
            editor.apply()

        } catch (e: Exception) {
            Log.e(TAG, "Error reading or saving metadata", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun triggerHapticFeedback() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun playShutterSound() {
        try {
            val mediaPlayer = MediaPlayer.create(this, R.raw.shutter_sound)
            mediaPlayer?.setOnCompletionListener { it.release() }
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing shutter sound", e)
        }
    }

    private fun animateShutterFlash() {
        viewBinding.shutterFlash.apply {
            alpha = 1f
            visibility = View.VISIBLE
            animate()
                .alpha(0f)
                .setDuration(300)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        visibility = View.GONE
                    }
                })
        }
    }

    private fun developRoll() {
        val appSpecificDirectory = getDir("film_roll", Context.MODE_PRIVATE)
        val imageFiles = appSpecificDirectory.listFiles()

        if (imageFiles.isNullOrEmpty()) {
            Toast.makeText(this, "No photos to develop.", Toast.LENGTH_SHORT).show()
            return
        }

        val rollId =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        lastDevelopedRollId = rollId

        val currentRatioName = aspectRatios[currentRatioIndex].displayName
        saveMetadataForRoll(rollId, currentRatioName)

        cameraExecutor.execute {
            var developedCount = 0
            imageFiles.forEach { file ->
                try {
                    val bitmap = BitmapFactory.decodeStream(FileInputStream(file))
                    saveBitmapToGallery(bitmap, rollId, file.name)
                    file.delete()
                    developedCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to develop image: ${file.name}", e)
                }
            }

            runOnUiThread {
                Toast.makeText(this, "$developedCount photos developed.", Toast.LENGTH_SHORT)
                    .show()
                shotsTaken = 0
                currentMode = ShootingMode.PRACTICE
                saveState()
                updateUiForMode()
            }
        }
    }

    private fun saveMetadataForRoll(rollId: String, format: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("ratio_$rollId", format).apply()
    }


    private fun showAbandonRollDialog() {
        AlertDialog.Builder(this)
            .setTitle("Abandon Roll?")
            .setMessage("Are you sure? All $shotsTaken shots on this roll will be permanently deleted.")
            .setPositiveButton("Abandon") { _, _ ->
                abandonRoll(true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun abandonRoll(showToast: Boolean) {
        val appSpecificDirectory = getDir("film_roll", Context.MODE_PRIVATE)
        val imageFiles = appSpecificDirectory.listFiles()
        imageFiles?.forEach { it.delete() }
        shotsTaken = 0
        saveState()
        if (showToast) {
            Toast.makeText(this, "Roll abandoned.", Toast.LENGTH_SHORT).show()
        }
        updateUiForMode()
    }

    private fun saveState() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_SHOTS_TAKEN, shotsTaken)
            .putString(KEY_SHOOTING_MODE, currentMode.name)
            .putString(KEY_LAST_ROLL_ID, lastDevelopedRollId)
            .putInt(KEY_RATIO_INDEX, currentRatioIndex)
            .apply()
    }

    private fun loadState() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        shotsTaken = prefs.getInt(KEY_SHOTS_TAKEN, 0)
        val savedMode = prefs.getString(KEY_SHOOTING_MODE, ShootingMode.PRACTICE.name)
        currentMode = ShootingMode.valueOf(savedMode ?: ShootingMode.PRACTICE.name)
        lastDevelopedRollId = prefs.getString(KEY_LAST_ROLL_ID, null)
        currentRatioIndex = prefs.getInt(KEY_RATIO_INDEX, 0)
    }

    private fun rotateBitmap(bitmap: Bitmap, path: String): Bitmap {
        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun cropBitmap(source: Bitmap): Bitmap {
        val currentRatio = aspectRatios[currentRatioIndex].ratio
        val sourceWidth = source.width
        val sourceHeight = source.height
        val targetWidth: Int
        val targetHeight: Int
        if (sourceWidth.toFloat() / sourceHeight > currentRatio) {
            targetHeight = sourceHeight
            targetWidth = (sourceHeight * currentRatio).toInt()
        } else {
            targetWidth = sourceWidth
            targetHeight = (sourceWidth / currentRatio).toInt()
        }
        val x = (sourceWidth - targetWidth) / 2
        val y = (sourceHeight - targetHeight) / 2
        return Bitmap.createBitmap(source, x, y, targetWidth, targetHeight)
    }

    private fun saveBitmapToGallery(
        bitmap: Bitmap,
        subfolder: String?,
        fileName: String
    ): File? {
        val mediaDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "FormatApp-Images/$subfolder"
        )
        mediaDir.mkdirs()
        val imageFile = File(mediaDir, fileName)

        try {
            val outputStream: OutputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()

            notifyMediaStore(imageFile, "Pictures/FormatApp-Images/$subfolder")
            return imageFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bitmap", e)
            return null
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

