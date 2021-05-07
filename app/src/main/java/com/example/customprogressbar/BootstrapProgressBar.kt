package com.example.customprogressbar

import android.animation.Animator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat


class BootstrapProgressBar : View, Animator.AnimatorListener, AnimatorUpdateListener {
    private var progress: Int = 0
    private var progressPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var stripePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var bgPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var userProgress = 0
    private var drawnProgress = 0
    private var maxProgress = 0
    private var striped = false
    private var animated = false
    private var rounded = false
    private var color = Color.WHITE

    var cornerRoundingLeft = true
    var cornerRoundingRight = true
    private var progressAnimator: ValueAnimator? = null
    private var tilePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val baselineHeight = this.dp2px(20f)
    private var progressCanvas: Canvas? = null
    private var progressBitmap: Bitmap? = null
    private var stripeTile: Bitmap? = null
    private var bootstrapSize = 1f

    constructor(context: Context?) : super(context) {
        initialise(null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initialise(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialise(attrs)
    }

    private fun initialise(attrs: AttributeSet?) {
        ValueAnimator.setFrameDelay(15) // attempt 60fps
        progressPaint.style = Paint.Style.FILL
        stripePaint = Paint()
        stripePaint.style = Paint.Style.FILL
        bgPaint.style = Paint.Style.FILL

        // get attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.BootstrapProgressBar)
        bgPaint.color = a.getColor(
            R.styleable.BootstrapProgressBar_myBGColor,
            ContextCompat.getColor(context, R.color.item_catch_progress_color)
        )
        color = a.getColor(R.styleable.BootstrapProgressBar_myFGColor, Color.WHITE)
        try {
            animated = a.getBoolean(R.styleable.BootstrapProgressBar_animated, false)
            rounded = a.getBoolean(R.styleable.BootstrapProgressBar_roundedCorners, false)
            striped = a.getBoolean(R.styleable.BootstrapProgressBar_striped, false)
            userProgress = a.getInt(R.styleable.BootstrapProgressBar_bootstrapProgress, 0)
            maxProgress = a.getInt(R.styleable.BootstrapProgressBar_bootstrapMaxProgress, 100)
            drawnProgress = userProgress
        } finally {
            a.recycle()
        }
        updateBootstrapState()
        progress = userProgress
        setMaxProgress(maxProgress)
    }

    fun setBgPaintColor(bgColor: Int) {
        this.bgPaint.color = bgColor
        invalidate()
    }

    fun setFGPaintColor(FgColor: Int) {
        this.color = FgColor
        invalidate()
    }

    private fun startProgressUpdateAnimation() {
        clearAnimation()
        progressAnimator = ValueAnimator.ofFloat(drawnProgress.toFloat(), userProgress.toFloat())
        progressAnimator?.let {
            it.duration = UPDATE_ANIM_MS
            it.repeatCount = 0
            it.repeatMode = ValueAnimator.RESTART
            it.interpolator = DecelerateInterpolator()
            it.addUpdateListener(this)

            it.addListener(this)
            it.start()
        }
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        drawnProgress = (animation.animatedValue as Float).toInt()
        invalidate()
    }

    override fun onAnimationStart(animation: Animator) {}
    override fun onAnimationEnd(animation: Animator) {
        startStripedAnimationIfNeeded()
    }

    override fun onAnimationCancel(animation: Animator) {}
    override fun onAnimationRepeat(animation: Animator) {}

    /**
     * Starts an infinite animation cycle which provides the visual effect of stripes moving
     * backwards. The current system time is used to offset tiled bitmaps of the progress background,
     * producing the effect that the stripes are moving backwards.
     */
    private fun startStripedAnimationIfNeeded() {
        if (!striped || !animated) {
            return
        }
        clearAnimation()
        progressAnimator = ValueAnimator.ofFloat(0f, 0f)
        progressAnimator?.let {
            it.duration = UPDATE_ANIM_MS
            it.repeatCount = ValueAnimator.INFINITE
            it.repeatMode = ValueAnimator.RESTART
            it.interpolator = LinearInterpolator()
            it.addUpdateListener { invalidate() }
            it.start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        when (heightMode) {
            MeasureSpec.EXACTLY -> {
            }
            MeasureSpec.AT_MOST -> {
                val desiredHeight = baselineHeight * bootstrapSize
                height = if (height > desiredHeight) desiredHeight.toInt() else height
            }
            else -> height = (baselineHeight * bootstrapSize).toInt()
        }
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (h != oldh) {
            stripeTile = null
        }
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) {
            return
        }
        if (progressBitmap == null) {
            progressBitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        }
        if (progressCanvas == null) {
            progressCanvas = Canvas(progressBitmap!!)
        }
        progressCanvas!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val ratio = drawnProgress / maxProgress.toFloat()
        val lineEnd = (w * ratio).toInt()
        var offset = 0f
        val offsetFactor = System.currentTimeMillis() % STRIPE_CYCLE_MS / STRIPE_CYCLE_MS.toFloat()
        if (striped && animated) { // determine offset for current animation frame of progress bar
            offset = h * 2 * offsetFactor
        }
        if (striped) { // draw a regular striped bar
            if (stripeTile == null) {
                stripeTile = createTile(h, stripePaint, progressPaint)
            }
            var start = 0 - offset
            while (start < lineEnd) {
                progressCanvas!!.drawBitmap(stripeTile!!, start, 0f, tilePaint)
                start += stripeTile!!.width.toFloat()
            }
        } else { // draw a filled bar
            progressCanvas!!.drawRect(0f, 0f, lineEnd.toFloat(), h, progressPaint)
        }
        progressCanvas!!.drawRect(lineEnd.toFloat(), 0f, w, h, bgPaint) // draw bg
        val corners: Float = if (rounded) h / 2f else 0f
        val round =
            createRoundedBitmap(progressBitmap, corners, cornerRoundingRight, cornerRoundingLeft)
        canvas.drawBitmap(round, 0f, 0f, tilePaint)
    }

    private fun updateBootstrapState() {
        progressPaint.color = color
        stripePaint.color =
            Color.argb(STRIPE_ALPHA, Color.red(color), Color.green(color), Color.blue(color))
        invalidateDrawCache()
        invalidate()
    }

    private fun invalidateDrawCache() {
        stripeTile = null
        progressBitmap = null
        progressCanvas = null
    }

    fun setProgress(progress: Int) {
        require(!(progress < 0 || progress > maxProgress)) {
            String.format(
                "Invalid value '%d' - progress must be an integer in the range 0-%d",
                progress,
                maxProgress
            )
        }
        userProgress = progress
        if (animated) {
            startProgressUpdateAnimation()
        } else {
            drawnProgress = progress
            invalidate()
        }
    }

    fun getProgress(): Int {
        return userProgress
    }

    fun setStriped(striped: Boolean) {
        this.striped = striped
        invalidate()
        startStripedAnimationIfNeeded()
    }

    fun isStriped(): Boolean {
        return striped
    }

    fun setAnimated(animated: Boolean) {
        this.animated = animated
        invalidate()
        startStripedAnimationIfNeeded()
    }

    fun isAnimated(): Boolean {
        return animated
    }

    fun setRounded(rounded: Boolean) {
        this.rounded = rounded
        updateBootstrapState()
    }

    fun isRounded(): Boolean {
        return rounded
    }

    fun getBootstrapSize(): Float {
        return bootstrapSize
    }

    fun setBootstrapSize(bootstrapSize: Float) {
        this.bootstrapSize = bootstrapSize
        requestLayout()
        updateBootstrapState()
    }

    fun getMaxProgress(): Int {
        return maxProgress
    }

    fun setMaxProgress(newMaxProgress: Int) {
        maxProgress = if (progress <= newMaxProgress) {
            newMaxProgress
        } else {
            throw IllegalArgumentException(
                String.format(
                    "MaxProgress cant be smaller than the current progress %d<%d",
                    progress,
                    newMaxProgress
                )
            )
        }
        invalidate()
    }

    fun setCornerRounding(left: Boolean, right: Boolean) {
        cornerRoundingLeft = left
        cornerRoundingRight = right
    }

    companion object {
        private const val UPDATE_ANIM_MS: Long = 1200
        private const val STRIPE_ALPHA = 150
        private const val STRIPE_CYCLE_MS: Long = 1500

        /**
         * Creates a Bitmap which is a tile of the progress bar background
         *
         * @param h the view height
         * @return a bitmap of the progress bar background
         */
        private fun createTile(h: Float, stripePaint: Paint?, progressPaint: Paint?): Bitmap {
            val bm = Bitmap.createBitmap(h.toInt() * 2, h.toInt(), Bitmap.Config.ARGB_8888)
            val tile = Canvas(bm)
            var x = 0f
            val path = Path()
            path.moveTo(x, 0f)
            path.lineTo(x, h)
            path.lineTo(h, h)
            tile.drawPath(path, stripePaint!!) // draw striped triangle
            path.reset()
            path.moveTo(x, 0f)
            path.lineTo(x + h, h)
            path.lineTo(x + h * 2, h)
            path.lineTo(x + h, 0f)
            tile.drawPath(path, progressPaint!!) // draw progress parallelogram
            x += h
            path.reset()
            path.moveTo(x, 0f)
            path.lineTo(x + h, 0f)
            path.lineTo(x + h, h)
            tile.drawPath(path, stripePaint) // draw striped triangle (completing tile)
            return bm
        }

        private fun createRoundedBitmap(
            bitmap: Bitmap?,
            cornerRadius: Float,
            roundRight: Boolean,
            roundLeft: Boolean
        ): Bitmap {
            val roundedBitmap =
                Bitmap.createBitmap(bitmap!!.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(roundedBitmap)
            val paint = Paint()
            val frame = Rect(0, 0, bitmap.width, bitmap.height)

            val leftRect = Rect(0, 0, bitmap.width / 2, bitmap.height)
            val rightRect = Rect(bitmap.width / 2, 0, bitmap.width, bitmap.height)

            // prepare canvas for transfer
            paint.isAntiAlias = true
            paint.color = -0x1
            paint.style = Paint.Style.FILL
            canvas.drawARGB(0, 0, 0, 0)
            canvas.drawRoundRect(RectF(frame), cornerRadius, cornerRadius, paint)
            if (!roundLeft) {
                canvas.drawRect(leftRect, paint)
            }
            if (!roundRight) {
                canvas.drawRect(rightRect, paint)
            }
            // draw bitmap
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, frame, frame, paint)
            return roundedBitmap
        }
    }
}