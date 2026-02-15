package com.ynab.receiptscanner.ui.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd

/**
 * Utility functions for view animations
 */
object AnimationUtils {
    
    /**
     * Pulse animation for sync indicators
     * Creates a subtle scale pulse effect
     */
    fun View.startPulseAnimation() {
        val scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 1f, 1.1f, 1f)
        
        scaleX.duration = 1000
        scaleY.duration = 1000
        scaleX.repeatCount = ValueAnimator.INFINITE
        scaleY.repeatCount = ValueAnimator.INFINITE
        scaleX.interpolator = AccelerateDecelerateInterpolator()
        scaleY.interpolator = AccelerateDecelerateInterpolator()
        
        scaleX.start()
        scaleY.start()
        
        // Store animators as tags for cleanup
        setTag(com.ynab.receiptscanner.R.id.animator_scale_x, scaleX)
        setTag(com.ynab.receiptscanner.R.id.animator_scale_y, scaleY)
    }
    
    /**
     * Stop pulse animation
     */
    fun View.stopPulseAnimation() {
        (getTag(com.ynab.receiptscanner.R.id.animator_scale_x) as? ObjectAnimator)?.cancel()
        (getTag(com.ynab.receiptscanner.R.id.animator_scale_y) as? ObjectAnimator)?.cancel()
        
        scaleX = 1f
        scaleY = 1f
    }
    
    /**
     * Success checkmark animation
     * Scales in with slight overshoot
     */
    fun View.animateSuccess(onComplete: (() -> Unit)? = null) {
        alpha = 0f
        scaleX = 0f
        scaleY = 0f
        visibility = View.VISIBLE
        
        animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                onComplete?.invoke()
            }
            .start()
    }
    
    /**
     * Fade in view
     */
    fun View.fadeIn(duration: Long = 300) {
        if (visibility == View.VISIBLE && alpha == 1f) return
        
        alpha = 0f
        visibility = View.VISIBLE
        
        animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    /**
     * Fade out view
     */
    fun View.fadeOut(duration: Long = 300, onComplete: (() -> Unit)? = null) {
        if (visibility != View.VISIBLE) {
            onComplete?.invoke()
            return
        }
        
        animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                visibility = View.GONE
                onComplete?.invoke()
            }
            .start()
    }
    
    /**
     * Shake animation for errors
     */
    fun View.shake() {
        val animator = ObjectAnimator.ofFloat(
            this,
            View.TRANSLATION_X,
            0f, -25f, 25f, -25f, 25f, -15f, 15f, -5f, 5f, 0f
        )
        animator.duration = 500
        animator.start()
    }
    
    /**
     * Shimmer effect for loading states
     */
    fun View.startShimmer() {
        val animator = ObjectAnimator.ofFloat(this, View.ALPHA, 1f, 0.3f, 1f)
        animator.duration = 1500
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
        
        setTag(com.ynab.receiptscanner.R.id.animator_shimmer, animator)
    }
    
    /**
     * Stop shimmer effect
     */
    fun View.stopShimmer() {
        (getTag(com.ynab.receiptscanner.R.id.animator_shimmer) as? ObjectAnimator)?.cancel()
        alpha = 1f
    }
    
    /**
     * Bounce animation for buttons
     */
    fun View.bounce() {
        val scaleDown = ObjectAnimator.ofFloat(this, View.SCALE_X, 1f, 0.9f).apply {
            duration = 100
        }
        val scaleDownY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 1f, 0.9f).apply {
            duration = 100
        }
        
        scaleDown.doOnEnd {
            val scaleUp = ObjectAnimator.ofFloat(this, View.SCALE_X, 0.9f, 1f).apply {
                duration = 100
            }
            val scaleUpY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 0.9f, 1f).apply {
                duration = 100
            }
            scaleUp.start()
            scaleUpY.start()
        }
        
        scaleDown.start()
        scaleDownY.start()
    }
}
