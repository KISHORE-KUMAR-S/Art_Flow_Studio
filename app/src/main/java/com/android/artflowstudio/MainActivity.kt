package com.android.artflowstudio

import android.app.Dialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get

class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    private var imageButtonCurrentPaint: ImageButton? = null
    private var currentIndex: Int? = null

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

        drawingView = findViewById(R.id.drawing_view)

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.paint_colors)
        imageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            drawingView?.setSizeForBrush(20.toFloat())
        }

        val ibBrush: ImageButton = findViewById(R.id.ib_brush)
        val ibEraser: ImageButton = findViewById(R.id.ib_eraser)

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
        if(view != imageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView!!.setColor(colorTag)

            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.color_palette_selected))
            imageButtonCurrentPaint?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.color_palette_unselected))
            imageButtonCurrentPaint = view
        }
    }
}