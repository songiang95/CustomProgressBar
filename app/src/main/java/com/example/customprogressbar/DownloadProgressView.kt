package com.example.customprogressbar

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import kotlin.math.PI
import kotlin.math.tan

class DownloadProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val UPDATE_PROGRESS_ANIM_MS: Long = 500
    private val INDETERMINATE_ANIM_MS: Long = 1200
    private var bgColor = 0
    private var maxProgress = 100
    private var progress = 0
    private var drawProgress = 0
    private val bgRectF = RectF()
    private val progressRectF = RectF()
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progressAnimator: ValueAnimator? = null
    private var indeterminateAnimator: ValueAnimator? = null
    private var indeterminate = false
    private val clipRect = RectF()
    private val clipPath = Path()
    private val listStripePath = mutableListOf<Path>()

    init {
        attrs?.let {
            initAttributes(it)
        }
    }

    fun setProgress(progress: Int) {
        if (progress in 0..maxProgress) {
            if (indeterminate) {
                setIndeterminate(false)
            }
            this.progress = progress
            startProgressUpdateAnimation()
        } else {
            throw IllegalArgumentException(
                String.format(
                    "Invalid value '%d' - progress must be an integer in the range 0-%d",
                    progress,
                    maxProgress
                )
            )
        }
    }

    fun setMax(max: Int) {
        if (progress <= max) {
            maxProgress = max
            calcProgressRegion()
            invalidate()
        } else {
            throw IllegalArgumentException(
                String.format(
                    "MaxProgress cant be smaller than the current progress %d<%d",
                    progress,
                    max
                )
            )
        }
    }

    fun setIndeterminate(isEnable: Boolean) {
        indeterminate = isEnable
        if (isEnable) {
            startProgressIndeterminateAnimation()
        } else {
            indeterminateAnimator?.cancel()
            startProgressUpdateAnimation()
        }
    }

    fun setProgressBackground(color: Int) {
        bgColor = color
        bgPaint.color = bgColor
        initStripePaint()
        invalidate()
    }

    private fun initAttributes(attributeSet: AttributeSet) {
        val attribute =
            context.obtainStyledAttributes(attributeSet, R.styleable.DownloadProgressView)

        var progress = 0
        var maxProgress = 0

        try {
            bgColor = attribute.getColor(
                R.styleable.DownloadProgressView_background_color,
                ContextCompat.getColor(context, R.color.progress_bg_color)
            )
            maxProgress = attribute.getInt(R.styleable.DownloadProgressView_max_progress, 100)
            progress = attribute.getInt(R.styleable.DownloadProgressView_download_progress, 0)
        } finally {
            attribute.recycle()
        }

        setProgressBackground(bgColor)
        setProgress(progress)
        setMax(maxProgress)
    }

    private fun setProgressGradient() {
        progressPaint.shader = LinearGradient(
            progressRectF.left,
            progressRectF.bottom,
            progressRectF.right,
            progressRectF.bottom,
            ContextCompat.getColor(context, R.color.progress_start_color),
            ContextCompat.getColor(context, R.color.progress_end_color),
            Shader.TileMode.CLAMP
        )
    }

    private fun calcProgressIndeterminate(value: Float) {
        val indeterminateProgressWidth = bgRectF.width() / 3
        var right = value + indeterminateProgressWidth
        if (right >= bgRectF.right) {
            right = bgRectF.right
        }
        clipRect.set(
            value,
            bgRectF.top,
            right,
            bgRectF.bottom
        )
        clipPath.reset()
        clipPath.addRoundRect(
            clipRect,
            clipRect.height() / 2,
            clipRect.height() / 2,
            Path.Direction.CW
        )
    }

    private fun setProgressRect(width: Int, height: Int) {
        progressRectF.set(0f, 0f, width.toFloat(), height.toFloat())
        setProgressGradient()
    }

    private fun setBackgroundRect(width: Int, height: Int) {
        bgRectF.set(0f, 0f, width.toFloat(), height.toFloat())
        setListStripesPath()
    }

    private fun setListStripesPath() {
        val stripeWidth = bgRectF.height() / 3f
        val stripeLeftCornerOffset = tan(22.5f * PI / 180).toFloat() * height

        val stripePath = Path()
        stripePath.moveTo(bgRectF.left, bgRectF.bottom)
        stripePath.lineTo(stripeLeftCornerOffset, bgRectF.top)
        stripePath.lineTo(stripeLeftCornerOffset + stripeWidth, bgRectF.top)
        stripePath.lineTo(bgRectF.left + stripeWidth, bgRectF.bottom)
        stripePath.close()

        var totalOffset = 0f

        while (totalOffset < (bgRectF.width() - stripeWidth)) {
            val path = Path(stripePath).apply {
                offset(totalOffset + stripeWidth, 0f)
            }
            totalOffset += (stripeWidth + stripeLeftCornerOffset)
            listStripePath.add(path)
        }
    }

    private fun initStripePaint() {
        stripePaint.color = bgColor
        stripePaint.alpha = 56
    }

    private fun calcProgressRegion() {
        val ratio = drawProgress / maxProgress.toFloat()
        val width = bgRectF.width() * ratio
        clipPath.reset()
        clipRect.set(
            progressRectF.left,
            progressRectF.top,
            width,
            progressRectF.bottom
        )

        clipPath.addRoundRect(
            clipRect,
            clipRect.height() / 2f,
            clipRect.height() / 2f,
            Path.Direction.CW
        )
    }

    private fun startProgressUpdateAnimation() {
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofInt(drawProgress, progress).apply {
            duration = UPDATE_PROGRESS_ANIM_MS
            repeatCount = 0
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                drawProgress = (it.animatedValue as Int)
                calcProgressRegion()
                invalidate()
            }
            start()
        }
    }

    private fun startProgressIndeterminateAnimation() {
        indeterminateAnimator?.cancel()
        indeterminateAnimator =
            ValueAnimator.ofFloat(
                0f - (bgRectF.width() / 3),
                bgRectF.width()
            ).apply {
                duration = INDETERMINATE_ANIM_MS
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    calcProgressIndeterminate(it.animatedValue as Float)
                    invalidate()
                }
                start()
            }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setBackgroundRect(w, h)
        setProgressRect(w, h)
    }

    private fun drawBackground(canvas: Canvas?) {
        canvas?.drawRoundRect(bgRectF, bgRectF.height() / 2, bgRectF.height() / 2, bgPaint)
    }

    private fun drawProgress(canvas: Canvas?) {
        canvas?.clipPath(clipPath)

        //draw full linear gradient progress
        canvas?.drawRoundRect(
            progressRectF,
            progressRectF.height() / 2,
            progressRectF.height() / 2,
            progressPaint
        )
    }

    private fun drawStripes(canvas: Canvas?) {
        listStripePath.forEach {
            canvas?.drawPath(it, stripePaint)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawBackground(canvas)
        drawProgress(canvas)
        drawStripes(canvas)
    }

}