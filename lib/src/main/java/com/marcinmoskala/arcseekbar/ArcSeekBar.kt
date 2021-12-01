package com.marcinmoskala.arcseekbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.atan2


class ArcSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyle) {

    var onProgressChangedListener: (ProgressListener)? = null
    var onStartTrackingTouch: (ProgressListener)? = null
    var onStopTrackingTouch: (ProgressListener)? = null

    private val a = attrs?.let {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.ArcSeekBar,
            defStyle,
            defStyleRes
        )
    }

    var maxProgress = a.useOrDefault(100) { getInteger(R.styleable.ArcSeekBar_maxProgress, it) }
        set(progress) {
            field = bound(0, progress, Int.MAX_VALUE)
            drawData?.let { drawData = it.copy(maxProgress = progress) }
            invalidate()
        }

    var progressValue: Int =
        a.useOrDefault(0) { getInteger(R.styleable.ArcSeekBar_progressValue, it) }
        set(progress) {
            field = bound(0, progress, maxProgress)
            onProgressChangedListener?.invoke(progress)
            drawData?.let { drawData = it.copy(progressValue = progress) }
            invalidate()
        }

    var progressWidth: Float = a.useOrDefault(4 * context.resources.displayMetrics.density) {
        getDimension(
            R.styleable.ArcSeekBar_progressWidth,
            it
        )
    }
        set(value) {
            field = value
            progressPaint.strokeWidth = value
        }

    var progressBackgroundWidth: Float =
        a.useOrDefault(2F) { getDimension(R.styleable.ArcSeekBar_progressBackgroundWidth, it) }
        set(mArcWidth) {
            field = mArcWidth
            progressBackgroundPaint.strokeWidth = mArcWidth
        }

    var progressColor: Int
        get() = progressPaint.color
        set(color) {
            progressPaint.color = color
            invalidate()
        }

    var progressBackgroundColor: Int
        get() = progressBackgroundPaint.color
        set(color) {
            progressBackgroundPaint.color = color
            invalidate()
        }

    private var thumb: Drawable = a?.getDrawable(R.styleable.ArcSeekBar_thumb)
        ?: ContextCompat.getDrawable(context, R.drawable.thumb)
        ?: ColorDrawable(progressBackgroundColor)

    private var roundedEdges =
        a.useOrDefault(true) { getBoolean(R.styleable.ArcSeekBar_roundEdges, it) }
        set(value) {
            if (value) {
                progressBackgroundPaint.strokeCap = Paint.Cap.ROUND
                progressPaint.strokeCap = Paint.Cap.ROUND
            } else {
                progressBackgroundPaint.strokeCap = Paint.Cap.SQUARE
                progressPaint.strokeCap = Paint.Cap.SQUARE
            }
            field = value
        }

    private var progressBackgroundPaint: Paint = makeProgressPaint(
        color = a.useOrDefault(ContextCompat.getColor(context, android.R.color.darker_gray)) {
            getColor(
                R.styleable.ArcSeekBar_progressBackgroundColor,
                it
            )
        },
        width = progressBackgroundWidth
    )

    private var progressPaint: Paint = makeProgressPaint(
        color = a.useOrDefault(ContextCompat.getColor(context, android.R.color.holo_blue_light)) {
            getColor(
                R.styleable.ArcSeekBar_progressColor,
                it
            )
        },
        width = progressWidth
    )

    private var mEnabled = a?.getBoolean(R.styleable.ArcSeekBar_enabled, true) ?: true

    init {
        a?.recycle()
    }

    private var drawerDataObservers: List<(ArcSeekBarData) -> Unit> = emptyList()

    private fun doWhenDrawerDataAreReady(f: (ArcSeekBarData) -> Unit) {
        if (drawData != null) f(drawData!!) else drawerDataObservers += f
    }

    private var drawData: ArcSeekBarData? = null
        set(value) {
            field = value ?: return
            val temp = drawerDataObservers.toList()
            temp.forEach { it(value) }
            drawerDataObservers -= temp
        }

    override fun onDraw(canvas: Canvas) {
        drawData?.run {
            canvas.drawArc(arcRect, startAngle, sweepAngle, false, progressBackgroundPaint)
            canvas.drawArc(arcRect, startAngle, progressSweepAngle, false, progressPaint)
            if (mEnabled) drawThumb(canvas)
        }
    }

    private fun ArcSeekBarData.drawThumb(canvas: Canvas) {
//        val angleThumb = if (progressValue > maxProgress / 2) {
//            -90F
//        } else {
//            90F
//        }
//        thumb = getRotateDrawable(thumb, angleThumb)

        val thumbHalfHeight = thumb.intrinsicHeight / 2
        val thumbHalfWidth = thumb.intrinsicWidth / 2
        thumb.setBounds(
            thumbX - thumbHalfWidth,
            thumbY - thumbHalfHeight,
            thumbX + thumbHalfWidth,
            thumbY + thumbHalfHeight
        )


//        canvas.rotate(90F)
        /*canvas.rotate(
            angleThumb,
            (thumbHalfWidth / 2).toFloat(),
            (thumbHalfHeight / 2).toFloat()
        )*/
        /*val thumbTemp = ((thumb as? BitmapDrawable?) as? Bitmap?)?.rotate(90)
        val thumbTemp2 = (thumb as? LayerDrawable?)?.rotate(90)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT && thumbTemp != null) {
            thumb = BitmapDrawable(resources, thumbTemp)
        }*/
//        canvas.rotate(angleThumb, thumbHalfWidth.toFloat(), thumbHalfHeight.toFloat())
        thumb.draw(canvas)

    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = View.getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val width = View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val dx = maxOf(thumb.intrinsicWidth.toFloat() / 2, this.progressWidth) + 2
        val dy = maxOf(thumb.intrinsicHeight.toFloat() / 2, this.progressWidth) + 2
        val realWidth = width.toFloat() - 2 * dx - paddingLeft - paddingRight
        val realHeight =
            minOf(height.toFloat() - 2 * dy - paddingTop - paddingBottom, realWidth / 2)
        drawData = ArcSeekBarData(
            dx + paddingLeft,
            dy + paddingTop,
            realWidth,
            realHeight,
            progressValue,
            maxProgress
        )
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mEnabled) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onStartTrackingTouch?.invoke(progressValue)
                    updateOnTouch(event)
                }
                MotionEvent.ACTION_MOVE -> updateOnTouch(event)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    onStopTrackingTouch?.invoke(progressValue)
                    isPressed = false
                }
            }
        }
        return mEnabled
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        if (thumb.isStateful) {
            thumb.state = drawableState
        }
        invalidate()
    }

    fun setProgressBackgroundGradient(vararg colors: Int) {
        setGradient(progressBackgroundPaint, *colors)
    }

    fun setProgressGradient(vararg colors: Int) {
        setGradient(progressPaint, *colors)
    }

    private fun setGradient(paint: Paint, vararg colors: Int) {
        doWhenDrawerDataAreReady {
            paint.shader =
                LinearGradient(it.dx, 0F, it.width, 0F, colors, null, Shader.TileMode.CLAMP)
        }
        invalidate()
    }

    private fun makeProgressPaint(color: Int, width: Float) = Paint().apply {
        this.color = color
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = width
        if (roundedEdges) strokeCap = Paint.Cap.ROUND
    }

    private fun updateOnTouch(event: MotionEvent) {
//        val x = event.x
//        val y = event.y
//        val angle = getAngle(x, y).toFloat()
        val progressFromClick =
            drawData?.progressFromClick(event.x, event.y, thumb.intrinsicHeight) ?: return
        isPressed = true

//        thumb = if (progressFromClick > maxProgress / 2) {
//            getRotateDrawable(thumb, -90F)
//        } else {
//            getRotateDrawable(thumb, 90F)
//        }
        progressValue = progressFromClick
    }

    private fun getAngle(x: Double, y: Double): Double {
        return 1.5 * Math.PI - atan2(
            y,
            x
        ) //note the atan2 call, the order of paramers is y then x
    }

    private fun getAngle(x: Float, y: Float): Double {
        return getAngle(x.toDouble(), y.toDouble())
    }

    private fun getRotateDrawable(bitmap: Bitmap, angle: Float): Drawable {
        return object : BitmapDrawable(resources, bitmap) {
            override fun draw(canvas: Canvas) {
                canvas.save()
                canvas.rotate(angle, (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat())
                super.draw(canvas)
                canvas.restore()
            }
        }
    }

    private fun getRotateDrawable(drawable: Drawable, angle: Float): Drawable {
        val arD = arrayOf(drawable)
        return object : LayerDrawable(arD) {
            override fun draw(canvas: Canvas) {
                canvas.save()
//                canvas.save(Canvas.MATRIX_SAVE_FLAG)
                canvas.rotate(
                    angle,
                    (drawable.bounds.width() / 2).toFloat(),
                    (drawable.bounds.height() / 2).toFloat()
                )
                super.draw(canvas)
                canvas.restore()
            }
        }
    }

    override fun isEnabled(): Boolean = mEnabled

    override fun setEnabled(enabled: Boolean) {
        this.mEnabled = enabled
    }

    fun <T, R> T?.useOrDefault(default: R, usage: T.(R) -> R) =
        if (this == null) default else usage(default)


    infix fun Bitmap.rotate(degrees: Number): Bitmap? {
        return Bitmap.createBitmap(
            this,
            0,
            0,
            width,
            height,
            Matrix().apply { postRotate(degrees.toFloat()) },
            true
        )
    }
}