package com.mithul.aurabank // Make sure this matches your package name

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import kotlin.random.Random

class AuraBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val auraBlobs = mutableListOf<AuraBlob>()
    private val numberOfBlobs = 5
    // NEW: Define a range for random animation durations
    private val minAnimationDuration = 10000L
    private val maxAnimationDuration = 15000L

    private val paint = Paint()

    init {
        for (i in 0 until numberOfBlobs) {
            auraBlobs.add(AuraBlob(
                color = getRandomAuraColor(),
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 0.3f + 0.1f,
                alpha = 0f
            ))
        }
        startAnimation()
    }

    private fun getRandomAuraColor(): Int {
        val colors = listOf(
            ContextCompat.getColor(context, R.color.aura_cyan),
            ContextCompat.getColor(context, R.color.aura_purple),
            ContextCompat.getColor(context, R.color.aura_blue)
        )
        return colors[Random.nextInt(colors.size)]
    }

    private fun startAnimation() {
        auraBlobs.forEach { blob ->
            // Start the "breathing" (alpha) animation, which is still repetitive
            ValueAnimator.ofFloat(0.3f, 0.6f, 0.3f).apply {
                duration = (minAnimationDuration + maxAnimationDuration) / 4
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                addUpdateListener { animator ->
                    blob.alpha = animator.animatedValue as Float
                    // No need to call invalidate() here, the movement animation will handle it
                }
                start()
            }

            // Start the first random movement animation for each blob
            animateBlob(blob)
        }
    }

    // --- NEW: Function for continuous, non-repeating random movement ---
    private fun animateBlob(blob: AuraBlob) {
        val randomDuration = Random.nextLong(minAnimationDuration, maxAnimationDuration)

        // Animate X position to a new random destination
        val animX = ValueAnimator.ofFloat(blob.x, Random.nextFloat()).apply {
            duration = randomDuration
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                blob.x = animator.animatedValue as Float
                invalidate() // Redraw the view on every frame
            }
        }

        // Animate Y position to a new random destination
        val animY = ValueAnimator.ofFloat(blob.y, Random.nextFloat()).apply {
            duration = randomDuration
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                blob.y = animator.animatedValue as Float
            }
        }

        // Add a listener to start the *next* random animation when this one finishes
        animX.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                // When the animation is over, start another one!
                animateBlob(blob)
            }
        })

        animX.start()
        animY.start()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        auraBlobs.forEach { blob ->
            val centerX = blob.x * viewWidth
            val centerY = blob.y * viewHeight
            val currentRadius = blob.radius * viewWidth.coerceAtMost(viewHeight)

            if (currentRadius <= 0) return@forEach

            val gradient = RadialGradient(
                centerX, centerY, currentRadius,
                blob.color,
                0x00000000, // Fully transparent edge
                Shader.TileMode.CLAMP
            )

            paint.apply {
                shader = gradient
                alpha = (blob.alpha * 255).toInt().coerceIn(0, 255)
            }
            canvas.drawCircle(centerX, centerY, currentRadius, paint)
        }
    }

    private data class AuraBlob(
        var color: Int,
        var x: Float,
        var y: Float,
        var radius: Float,
        var alpha: Float
    )
}