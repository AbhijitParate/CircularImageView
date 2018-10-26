package com.ap.circularimageview.civ

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.annotation.Dimension
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.ImageView
import kotlin.math.min

/**
 * ImageView for displaying a circular image and a outline around it
 */
class CircularImageView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    AppCompatImageView(context, attrs, defStyleAttr) {

    @Dimension
    private var outlineThickness: Float = 0f
    private var hasOutline: Boolean = false

    private var imageCanvasSize: Int = 0

    private var image: Bitmap? = null
    private var currentDrawable: Drawable? = null
    private var transparentColor: Paint = Paint().apply {
        color = ContextCompat.getColor(context, android.R.color.white)
        isAntiAlias = true
    }
    private var outlineColor: Paint = Paint().apply {
        isAntiAlias = true
    }

    constructor(context: Context) : this(context, null) {
        init(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {
        init(context, attrs, 0)
    }

    init {
        init(context, attrs, defStyleAttr)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        attrs?.let {
            // Load the style attributes from xml
            val attributes = context.obtainStyledAttributes(it, R.styleable.CircularImageView, defStyleAttr, 0)
            if (attributes.getBoolean(R.styleable.CircularImageView_hasOutline, true)) {
                val defaultOutlineSize = dpToPx(context, DEFAULT_OUTLINE_WIDTH)
                setOutlineThickness(
                    attributes.getDimension(
                        R.styleable.CircularImageView_outlineWidth,
                        defaultOutlineSize
                    )
                )

                val borderColorVal =
                    attributes.getInt(R.styleable.CircularImageView_outlineColor, DEFAULT_OUTLINE_COLOR)
                setOutlineColor(borderColorVal)

                val hasOutline = attributes.getBoolean(R.styleable.CircularImageView_hasOutline, DEFAULT_HAS_OUTLINE)
                enableOutline(hasOutline)
            }
            attributes.recycle()
        }
    }

    /**
     * sets outline thinkness in DP
     */
    fun setOutlineThickness(outlineThickness: Int) {
        setOutlineThickness(dpToPx(context, outlineThickness))
    }

    /**
     * sets outline thickness in PX
     */
    fun setOutlineThickness(outlineWidth: Float) {
        this.outlineThickness = outlineWidth
        requestLayout()
        invalidate()
    }

    /**
     * sets outline color
     */
    fun setOutlineColor(outlineColor: Int) {
        this.outlineColor.color = outlineColor
        invalidate()
    }

    /**
     * enables or disables outline
     */
    fun enableOutline(hasOutline: Boolean) {
        this.hasOutline = hasOutline
        invalidate()
    }

    override fun getScaleType(): ImageView.ScaleType {
        return DEFAULT_SCALE_TYPE
    }

    public override fun onDraw(canvas: Canvas) {
        setLayerType(if (canvas.isHardwareAccelerated) View.LAYER_TYPE_HARDWARE else View.LAYER_TYPE_SOFTWARE, null)

        // Load the bitmap
        loadBitmap()

        // Check if image isn't null
        if (image == null) return

        if (!isInEditMode) {
            imageCanvasSize = min(width, height)
        }

        //Find center of the image
        val circularImageCenter = (imageCanvasSize - outlineThickness * 2).toInt() / 2

        if (hasOutline && outlineThickness > 0f) {
            // Add outline
            canvas.drawCircle(
                circularImageCenter + outlineThickness,
                circularImageCenter + outlineThickness,
                circularImageCenter + outlineThickness,
                outlineColor
            )
        }

        // Make hollow part inside the ring center transparent
        canvas.drawCircle(
            circularImageCenter + outlineThickness,
            circularImageCenter + outlineThickness,
            circularImageCenter.toFloat(),
            transparentColor
        )
    }

    private fun loadBitmap() {
        // On redraw if currentDrawable has not changed, no need to re-retrieve it
        if (currentDrawable === drawable) return

        this.currentDrawable = drawable
        this.image = drawableToBitmap(currentDrawable)
        updateShader()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        imageCanvasSize = width
        if (height < imageCanvasSize) imageCanvasSize = height
        image?.let {
            updateShader()
        }
    }

    private fun updateShader() {
        image?.let {
            image = cropBitmap(it)
            val shader = BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val matrix = Matrix()
            matrix.setScale(
                imageCanvasSize.toFloat() / it.width.toFloat(),
                imageCanvasSize.toFloat() / it.height.toFloat()
            )
            shader.setLocalMatrix(matrix)
            transparentColor.shader = shader
        }
    }

    private fun cropBitmap(bitmap: Bitmap): Bitmap {
        val bmp: Bitmap
        if (bitmap.width >= bitmap.height) {
            bmp = Bitmap.createBitmap(
                bitmap,
                bitmap.width / 2 - bitmap.height / 2,
                0,
                bitmap.height,
                bitmap.height
            )
        } else {
            bmp = Bitmap.createBitmap(
                bitmap,
                0,
                bitmap.height / 2 - bitmap.width / 2,
                bitmap.width,
                bitmap.width
            )
        }
        return bmp
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null) {
            return null
        } else if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val intrinsicWidth = drawable.intrinsicWidth
        val intrinsicHeight = drawable.intrinsicHeight

        // Check if image has valid width and height
        if (!(intrinsicWidth > 0 && intrinsicHeight > 0)) return null

        return try {
            // Create Bitmap object from the currentDrawable
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: OutOfMemoryError) {
            // Simply return null of failed bitmap creations
            Log.e(CircularImageView::class.simpleName, "OutOfMemoryError while drawing the image")
            null
        }

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = measureWidth(widthMeasureSpec)
        val height = 2 + measureHeight(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    private fun measureWidth(measureSpec: Int): Int {
        val specMode = View.MeasureSpec.getMode(measureSpec)
        val specSize = View.MeasureSpec.getSize(measureSpec)
        return if (specMode == View.MeasureSpec.EXACTLY || specMode == View.MeasureSpec.AT_MOST) specSize else imageCanvasSize
    }

    private fun measureHeight(measureSpec: Int): Int {
        val specMode = View.MeasureSpec.getMode(measureSpec)
        val specSize = View.MeasureSpec.getSize(measureSpec)
        return if (specMode == View.MeasureSpec.EXACTLY || specMode == View.MeasureSpec.AT_MOST) specSize else imageCanvasSize
    }

    companion object {
        // Default Values
        private const val DEFAULT_OUTLINE_WIDTH = 4
        private const val DEFAULT_HAS_OUTLINE = false
        private const val DEFAULT_OUTLINE_COLOR = android.R.color.transparent
        private val DEFAULT_SCALE_TYPE = ImageView.ScaleType.CENTER_INSIDE

        private fun dpToPx(context: Context, dp: Int): Float {
            val displayMetrics = context.resources.displayMetrics
            return dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)
        }
    }
}