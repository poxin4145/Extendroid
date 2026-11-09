package dev.legendsayantan.extendroid.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.card.MaterialCardView
import dev.legendsayantan.extendroid.R
import dev.legendsayantan.extendroid.Utils.Companion.dpToPx
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.get
import dev.legendsayantan.extendroid.Utils
import dev.legendsayantan.extendroid.lib.Logging
import java.util.Timer
import kotlin.concurrent.timerTask

/**
 * @author legendsayantan
 */
@SuppressLint("ClickableViewAccessibility")
class PopupWindow(
    val ctx: Context,
    val widthPixels: Int,
    val heightPixels: Int,
    val spawnLocation: Pair<Int, Int>,
    val color: Int,
    val dimValue: Float = 0f
) : FrameLayout(ctx) {

    var onSurfaceAvailable: (Surface) -> Unit = {}

    /** Called when the window is restored from the bubble, to re-attach the display surface. */
    var onSurfaceRefresh: (Surface) -> Unit = {}
    var onSurfaceChanged: (Surface, Int, Int) -> Unit = { s, w, h -> }

    var onDeleteRequest: (PopupManager.PopupDeleteReason) -> Unit = {}
    var onKeyEventCallback: (Int, Int) -> Unit = { kCode, action -> }
    var onTouchEventCallback: (MotionEvent) -> Unit = { }

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    var positionParams = WindowManager.LayoutParams().apply {
        val originalRatio = widthPixels.toFloat()/heightPixels
        val optimised = optimiseSizes(ctx, widthPixels, heightPixels)
        val newRatio = optimised.first.toFloat()/optimised.second
        println(originalRatio)
        println(newRatio)
        width = optimised.first
        height = optimised.second
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                if(dimValue!=0f) WindowManager.LayoutParams.FLAG_DIM_BEHIND else 0
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.TOP or Gravity.START
        dimAmount = dimValue
        x = spawnLocation.first
        y = spawnLocation.second
    }

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private val touchSlop = ViewConfiguration.get(ctx).scaledTouchSlop

    var resizeInitialWidth = 0
    var resizeInitialHeight = 0
    var resizeInitialX = 0
    var resizeInitialY = 0
    var resizeTouchStartX = 0f
    var resizeTouchStartY = 0f

    var controlTimer = Timer()

    val themedCtx by lazy { ContextThemeWrapper(ctx, R.style.Base_Theme_Extendroid) }
    val root by lazy { LayoutInflater.from(themedCtx).inflate(R.layout.layout_app_window, this, true) }
    val surfaceParent by lazy { root.findViewById<LinearLayout>(R.id.surfaceParent) }
    val dragHandle by lazy { root.findViewById<LinearLayout>(R.id.handle) }
    val handleCard by lazy { root.findViewById<MaterialCardView>(R.id.handleCard) }
    val surfaceContainer by lazy { root.findViewById<SurfaceView>(R.id.surfaceView) }
    val leftResizer by lazy { root.findViewById<LinearLayout>(R.id.leftResizer) }
    val rightResizer by lazy { root.findViewById<LinearLayout>(R.id.rightResizer) }

    val controls by lazy { root.findViewById<LinearLayout>(R.id.controls) }
    val bubbleView by lazy { root.findViewById<MaterialCardView>(R.id.bubbleView) }
    val hideBtn by lazy { root.findViewById<MaterialCardView>(R.id.hideBtn) }
    val fullBtn by lazy { root.findViewById<MaterialCardView>(R.id.fullBtn) }
    val backBtn by lazy { root.findViewById<MaterialCardView>(R.id.backBtn) }
    val closeBtn by lazy { root.findViewById<MaterialCardView>(R.id.actionBtn) }

    init {
        surfaceParent.background = color.toDrawable()
        handleCard.setCardBackgroundColor(ColorStateList.valueOf(color))
        val colors = arrayOf(
            Utils.mixColors(color, resources.getColor(R.color.white,null), 0.5f),
            Utils.mixColors(color, resources.getColor(R.color.black,null), 0.5f)
        )
        if(Utils.isDarkModeEnabled(ctx)) colors.reverse()
        arrayOf(
            hideBtn,
            fullBtn,
            backBtn,
            closeBtn
        ).forEach {
            it.setCardBackgroundColor(colors[0])
            it.strokeColor = colors[1]
            (it[0] as AppCompatImageView).imageTintList = ColorStateList.valueOf(colors[1])
        }
        if (surfaceContainer is SurfaceView) {
            surfaceContainer.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    onSurfaceAvailable(holder.surface)
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                    onSurfaceChanged(holder.surface, width, height)
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {}

            })
        }

        surfaceContainer.setOnTouchListener { view, event ->
            onTouchEventCallback(event)
            true
        }

        // Drag & Click handling on dragHandle
        dragHandle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // Record initial positions
                    initialX = positionParams.x
                    initialY = positionParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (!isDragging && (dx * dx + dy * dy) > touchSlop * touchSlop) {
                        isDragging = true
                    }
                    if (isDragging) {
                        // Update position
                        positionParams.x = initialX + dx
                        positionParams.y = initialY + dy
                        wm.updateViewLayout(this, positionParams)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Treat as click: toggle controls
                        if (controls.translationY == 0f) {
                            hideControls()
                        } else {
                            showControls()
                        }
                    }
                    true
                }

                else -> false
            }
        }

        // LEFT resizer (bottom‑left corner)
        // LEFT resizer (bottom‑left corner)
        leftResizer.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    resizeInitialWidth = positionParams.width
                    resizeInitialHeight = positionParams.height
                    resizeInitialX = positionParams.x
                    resizeTouchStartX = event.rawX
                    resizeTouchStartY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - resizeTouchStartX).toInt()
                    val dy = (event.rawY - resizeTouchStartY).toInt()

                    // Compute new raw size
                    val rawWidth = resizeInitialWidth - dx
                    val rawHeight = resizeInitialHeight + dy

                    // Clamp to minimums
                    val (newW, newH) = coerceSizes(context,rawWidth, rawHeight)

                    // Here’s the only change: fix the right edge at its original X + width
                    val initialRightX = resizeInitialX + resizeInitialWidth
                    positionParams.x = initialRightX - newW

                    positionParams.width = newW
                    positionParams.height = newH

                    wm.updateViewLayout(this, positionParams)
                    true
                }

                else -> false
            }
        }


// RIGHT resizer (bottom‑right corner)
        rightResizer.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    resizeInitialWidth = positionParams.width
                    resizeInitialHeight = positionParams.height
                    resizeTouchStartX = event.rawX
                    resizeTouchStartY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - resizeTouchStartX).toInt()
                    val dy = (event.rawY - resizeTouchStartY).toInt()

                    // dragging right handle: dx > 0 expands right, dx < 0 shrinks
                    val rawWidth = resizeInitialWidth + dx
                    val rawHeight = resizeInitialHeight + dy

                    val (newW, newH) = coerceSizes(context,rawWidth, rawHeight)

                    positionParams.width = newW
                    positionParams.height = newH
                    wm.updateViewLayout(this, positionParams)
                    true
                }

                else -> false
            }
        }


        controls.visibility = GONE

        // Resize instead of closing: removing the window (and its virtual display) looked like
        // the app had crashed. Long press keeps the old "back to preview" behaviour.
        hideBtn.setOnClickListener { shrinkOrRestore() }
        hideBtn.setOnLongClickListener {
            onDeleteRequest(PopupManager.PopupDeleteReason.MINIMIZE)
            true
        }
        fullBtn.setOnClickListener { maximizeOrRestore() }
        backBtn.setOnTouchListener { view, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> onKeyEventCallback(
                    KeyEvent.KEYCODE_BACK,
                    KeyEvent.ACTION_DOWN
                )

                MotionEvent.ACTION_UP -> onKeyEventCallback(
                    KeyEvent.KEYCODE_BACK,
                    KeyEvent.ACTION_UP
                )
            }
            true
        }
        closeBtn.setOnClickListener { onDeleteRequest(PopupManager.PopupDeleteReason.TERMINATE) }
    }

    fun addDim(dimValue: Float){
        positionParams = positionParams.apply {
            flags += WindowManager.LayoutParams.FLAG_DIM_BEHIND
            dimAmount = dimValue
        }
        wm.updateViewLayout(this,positionParams)
    }

    fun showControls() {
        controls.translationY = ctx.dpToPx(-100f)
        controls.visibility = VISIBLE
        controls.animate().translationY(0f).alpha(1f)
        try {
            controlTimer.cancel()
        } catch (_: Exception) {
        }
        controlTimer = Timer()
        controlTimer.schedule(timerTask {
            handler.post { hideControls() }
        }, 3000)
    }

    fun hideControls() {
        try {
            controls.animate().translationY(ctx.dpToPx(-100f)).alpha(0f)
            handler.postDelayed({
                controls.visibility = GONE
            }, 250)
        } catch (_: Exception) {
        }
    }

    private var savedBounds: IntArray? = null

    /** Fills the screen with this window, or restores the previous bounds. */
    fun maximizeOrRestore() {
        if (bubbleMode) return
        val metrics = resources.displayMetrics
        val saved = savedBounds
        if (saved == null) {
            savedBounds = intArrayOf(positionParams.x, positionParams.y, positionParams.width, positionParams.height)
            applyBounds(0, 0, metrics.widthPixels, metrics.heightPixels)
        } else {
            applyBounds(saved[0], saved[1], saved[2], saved[3])
            savedBounds = null
        }
    }

    /** Bounds of the compact window, kept so the bubble can expand back into it. */
    private var compactBounds: IntArray? = null
    private var bubbleMode = false

    private val bubbleSize: Int
        get() = ctx.dpToPx(58f).toInt()

    /**
     * Cycles full window -> compact window -> round edge bubble -> compact window.
     * The bubble shows no content, only the app icon, and expands when tapped.
     */
    fun shrinkOrRestore() {
        if (bubbleMode) {
            expandFromBubble()
            return
        }
        val metrics = resources.displayMetrics
        val saved = savedBounds
        if (saved == null) {
            savedBounds = intArrayOf(positionParams.x, positionParams.y, positionParams.width, positionParams.height)
            val w = (metrics.widthPixels * 0.55f).toInt()
            val h = (metrics.heightPixels * 0.45f).toInt()
            compactBounds = intArrayOf(
                ((metrics.widthPixels - w) / 2).coerceAtLeast(0),
                ((metrics.heightPixels - h) / 3).coerceAtLeast(0),
                w,
                h
            )
            applyBounds(compactBounds!![0], compactBounds!![1], w, h)
        } else {
            // Second press: collapse to a bubble hugging the screen edge.
            compactBounds = intArrayOf(positionParams.x, positionParams.y, positionParams.width, positionParams.height)
            collapseToBubble()
        }
    }

    private fun collapseToBubble() {
        if (bubbleMode) return
        bubbleMode = true
        // Keep the SurfaceView alive: destroying its surface detaches the virtual display and
        // the app came back as a black window. It just becomes tiny underneath the bubble.
        dragHandle.visibility = View.GONE
        controls.visibility = View.GONE
        leftResizer.visibility = View.GONE
        rightResizer.visibility = View.GONE
        bubbleView.visibility = View.VISIBLE
        bubbleView.setOnClickListener { expandFromBubble() }

        val metrics = resources.displayMetrics
        val size = bubbleSize
        val saved = compactBounds ?: intArrayOf(positionParams.x, positionParams.y, 0, 0)
        positionParams.width = size
        positionParams.height = size
        positionParams.x = if (saved[0] + saved[2] / 2 > metrics.widthPixels / 2) {
            (metrics.widthPixels - size).coerceAtLeast(0)
        } else {
            0
        }
        positionParams.y = saved[1].coerceIn(0, (metrics.heightPixels - size).coerceAtLeast(0))
        try {
            wm.updateViewLayout(this, positionParams)
        } catch (t: Throwable) {
            Logging(ctx).e(t as? Exception ?: Exception(t), "PopupWindow")
        }
    }

    private fun expandFromBubble() {
        if (!bubbleMode) return
        bubbleMode = false
        bubbleView.visibility = View.GONE
        dragHandle.visibility = View.VISIBLE
        leftResizer.visibility = View.VISIBLE
        rightResizer.visibility = View.VISIBLE
        val bounds = compactBounds ?: savedBounds
        if (bounds != null) {
            applyBounds(bounds[0], bounds[1], bounds[2], bounds[3])
        }
        // Re-attach the (still alive) surface to the virtual display after the resize.
        bubbleView.postDelayed({
            try {
                onSurfaceRefresh(surfaceContainer.holder.surface)
            } catch (t: Throwable) {
                Logging(ctx).e(t as? Exception ?: Exception(t), "PopupWindow")
            }
        }, 60)
    }

    private fun applyBounds(x: Int, y: Int, width: Int, height: Int) {
        positionParams.x = x
        positionParams.y = y
        positionParams.width = width.coerceAtLeast(getLimit(ctx).toInt())
        positionParams.height = height.coerceAtLeast(getLimit(ctx).toInt())
        try {
            wm.updateViewLayout(this, positionParams)
        } catch (t: Throwable) {
            Logging(ctx).e(t as? Exception ?: Exception(t), "PopupWindow")
        }
    }

    /** True once the window was really added to the window manager. */
    var isAddedToWindow = false
        private set

    fun show(): PopupWindow {
        if (parent == null) {
            try {
                wm.addView(this, positionParams)
                isAddedToWindow = true
                alpha = 0f
                animate().alpha(1f).scaleY(1f).setDuration(250).start()
            } catch (t: Throwable) {
                // Usually a missing "display over other apps" permission. Report it instead of
                // letting BadTokenException kill the app.
                isAddedToWindow = false
                Logging(ctx).e(t as? Exception ?: Exception(t), "PopupWindow")
                Toast.makeText(ctx, R.string.popup_failed, Toast.LENGTH_LONG).show()
            }
        }
        return this
    }

    fun remove() {
        if (parent != null) {
            animate().alpha(0f).scaleY(0f).setDuration(250).start()
            controlTimer.cancel()
            handler.postDelayed({
                try {
                    wm.removeView(this)
                } catch (t: Throwable) {
                    Logging(ctx).e(t as? Exception ?: Exception(t), "PopupWindow")
                }
            },250)
        }
    }

    companion object {

        fun getLimit(context: Context): Float{
            return context.dpToPx(175f)
        }
        // Helper to clamp to your minimums
        fun coerceSizes(context: Context,width: Int, height: Int): Pair<Int, Int> {
            val w = width.coerceAtLeast(getLimit(context).toInt())
            val h = height.coerceAtLeast(getLimit(context).toInt())
            return w to h
        }

        //keeping the aspect ratio same and ensuring neither width nor height is under limit
        fun optimiseSizes(context: Context,width:Int,height: Int): Pair<Int, Int>{
            val limit = getLimit(context)
            if (width >= limit && height >= limit) {
                return width to height
            }

            val scaleW = limit  / width.toFloat()
            val scaleH = limit / height.toFloat()

            val scale = maxOf(scaleW, scaleH)

            val newW = kotlin.math.ceil(width  * scale).toInt()
            val newH = kotlin.math.ceil(height * scale).toInt()
            return newW to newH
        }
    }
}