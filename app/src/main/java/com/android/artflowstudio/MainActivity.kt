package com.android.artflowstudio

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
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

class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    private var imageButtonCurrentPaint: ImageButton? = null
    private var currentIndex: Int? = null

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
                checkPermissionsGranted() -> Toast.makeText(this, "Already permissions granted", Toast.LENGTH_SHORT).show()
                !checkPermissionsGranted() -> showRationaleDialog("Art Flow Studio", "Art Flow Studio needs to Access your External Storage")
                else -> requestPermission(requestPermissions = requestPermissions)
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
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED))
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES))
            else -> requestPermissions.launch(arrayOf(READ_EXTERNAL_STORAGE))
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun checkPermissionsGranted(): Boolean {
        val permissions = arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED, READ_EXTERNAL_STORAGE)

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
}