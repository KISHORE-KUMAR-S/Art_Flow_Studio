package com.android.artflowstudio

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    private var imageButtonCurrentPaint: ImageButton? = null
    private var currentIndex: Int? = null

    private val openGalleryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == RESULT_OK && result.data != null) {
            val imageBackground: ImageView = findViewById(R.id.iv_background)
            imageBackground.setImageURI(result.data!!.data) // Getting the location of the data
        }
    }

    @SuppressLint("NewApi")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val grantedPermissions = permissions.filterValues { it }

            when {
                grantedPermissions.isEmpty() -> Log.i("Permissions", "Permission Denied.")
                else -> if (grantedPermissions.keys.intersect(setOf(READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED, READ_EXTERNAL_STORAGE)).isNotEmpty()) Log.i("Permission", "Permission Granted")
            }
        }

        drawingView = findViewById(R.id.drawing_view)

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.paint_colors)
        imageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            drawingView?.setSizeForBrush(20.toFloat())
        }

        val ibBrush: ImageButton = findViewById(R.id.ib_brush)
        val ibEraser: ImageButton = findViewById(R.id.ib_eraser)
        val ibGallery: ImageButton = findViewById(R.id.ib_add_image)
        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        val ibRedo: ImageButton = findViewById(R.id.ib_redo)
        val ibSave: ImageButton = findViewById(R.id.ib_save)

        val frameDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)

        ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
            imageButtonCurrentPaint = when (currentIndex) {
                null -> linearLayoutPaintColors[1] as ImageButton
                else -> linearLayoutPaintColors[currentIndex!!] as ImageButton
            }

            drawingView!!.setColor(imageButtonCurrentPaint!!.tag.toString())
            imageButtonCurrentPaint?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.color_palette_selected))
        }

        ibEraser.setOnClickListener {
            showBrushSizeChooserDialog()
            currentIndex = linearLayoutPaintColors.indexOfChild(imageButtonCurrentPaint)

            drawingView!!.setColor(ibEraser.tag.toString())

            for (index in 0 until linearLayoutPaintColors.childCount) {
                val button = linearLayoutPaintColors.getChildAt(index) as? ImageButton
                button?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.color_palette_unselected))
            }
        }

        ibGallery.setOnClickListener {
            when {
                checkPermissionsGranted() -> {
                    val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }
                !checkPermissionsGranted() -> showRationaleDialog("Art Flow Studio", "Art Flow Studio needs to Access your External Storage")
                else -> requestPermission(requestPermissions = requestPermissions)
            }
        }

        ibUndo.setOnClickListener { drawingView?.onClickUndo() }

        ibRedo.setOnClickListener { drawingView?.onClickRedo() }

        ibSave.setOnClickListener {
            lifecycleScope.launch {
                saveBitmapFile(
                    bitmap = getBitmapFromView(
                        view = frameDrawingView
                    )
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size")

        val sizes = listOf(10f, 20f, 30f)
        val smallButton = brushDialog.findViewById<ImageView>(R.id.ib_small_brush)
        val mediumButton = brushDialog.findViewById<ImageView>(R.id.ib_medium_brush)
        val largeButton = brushDialog.findViewById<ImageView>(R.id.ib_large_brush)
        val buttons = listOf(smallButton, mediumButton, largeButton)

        buttons.forEachIndexed { index, button ->
            button.setOnClickListener {
                drawingView?.setSizeForBrush(sizes[index])
                brushDialog.dismiss()
            }
        }

        brushDialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun paintClicked(view: View) {
        if (view != imageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView!!.setColor(colorTag)

            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.color_palette_selected))
            imageButtonCurrentPaint?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.color_palette_unselected))
            imageButtonCurrentPaint = view
        }
    }

    private fun requestPermission(requestPermissions: ActivityResultLauncher<Array<String>>) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED, WRITE_EXTERNAL_STORAGE))
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES, WRITE_EXTERNAL_STORAGE))
            else -> requestPermissions.launch(arrayOf(READ_EXTERNAL_STORAGE))
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun checkPermissionsGranted(): Boolean {
        val permissions = arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)

        return permissions.any { check ->
            ContextCompat.checkSelfPermission(
                this,
                check
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showRationaleDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder
            .setMessage(message)
            .setTitle(title)
            .setPositiveButton("Dismiss") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val backgroundDrawable = view.background

        when {
            backgroundDrawable != null -> backgroundDrawable.draw(canvas)
            else -> canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun saveBitmapFile(bitmap: Bitmap?): String {
        var result = ""

        withContext(Dispatchers.IO) {
            if(bitmap != null) {
                try {
                    val storageManager = getSystemService(STORAGE_SERVICE) as StorageManager
                    val storageVolume = storageManager.storageVolumes[0]
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                    val bytesArray = byteArrayOutputStream.toByteArray()

                    val file = File(storageVolume.directory?.path + "/Download/Art_Flow_Studio_" + System.currentTimeMillis() + ".jpg")
                    val fos = FileOutputStream(file)
                    fos.write(bytesArray)
                    fos.close()

                    result = file.absolutePath

                    runOnUiThread {
                        when {
                            result.isNotEmpty() -> Toast.makeText(this@MainActivity, "File saved successfully", Toast.LENGTH_SHORT).show()
                            else -> Toast.makeText(this@MainActivity, "Something went wrong while saving the file", Toast.LENGTH_SHORT).show()
                        }

                        Log.i("Stored: ", result)
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }

        return result
    }
}