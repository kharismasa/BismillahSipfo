package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.adapter.PeminjamanPagerAdapter
import com.example.bismillahsipfo.data.repository.SharedPeminjamanViewModel

class PeminjamanActivity : AppCompatActivity() {

    private val sharedViewModel: SharedPeminjamanViewModel by viewModels()
    lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: PeminjamanPagerAdapter

    // Progress Step Views
    private lateinit var step1: TextView
    private lateinit var step2: TextView
    private lateinit var step3: TextView
    private lateinit var tvStep1a: TextView
    private lateinit var tvStep1b: TextView
    private lateinit var tvStep2a: TextView
    private lateinit var tvStep2b: TextView
    private lateinit var tvStep3a: TextView
    private lateinit var tvStep3b: TextView

    // Progress Lines
    private lateinit var lineStep12: View
    private lateinit var lineStep23: View

    // Colors for animation
    private val activeColor by lazy { ContextCompat.getColor(this, R.color.dark_blue) }
    private val inactiveColor by lazy { ContextCompat.getColor(this, R.color.light_gray) }
    private val activeTextColor by lazy { ContextCompat.getColor(this, R.color.dark_blue) }
    private val inactiveTextColor by lazy { ContextCompat.getColor(this, R.color.gray) }
    private val whiteColor by lazy { ContextCompat.getColor(this, android.R.color.white) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peminjaman)

        initViews()
        setupViewPager()

        // Set initial step
        updateProgressStep(1)

        Log.d("PeminjamanActivity", "Activity created with responsive progress steps")
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)

        // Initialize progress step views
        step1 = findViewById(R.id.step1)
        step2 = findViewById(R.id.step2)
        step3 = findViewById(R.id.step3)
        tvStep1a = findViewById(R.id.tvStep1a)
        tvStep1b = findViewById(R.id.tvStep1b)
        tvStep2a = findViewById(R.id.tvStep2a)
        tvStep2b = findViewById(R.id.tvStep2b)
        tvStep3a = findViewById(R.id.tvStep3a)
        tvStep3b = findViewById(R.id.tvStep3b)

        // Initialize progress lines
        lineStep12 = findViewById(R.id.line_step_1_2)
        lineStep23 = findViewById(R.id.line_step_2_3)
    }

    private fun setupViewPager() {
        pagerAdapter = PeminjamanPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        // Disable swipe
        viewPager.isUserInputEnabled = false

        // Listen to page changes untuk update progress secara otomatis
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val currentStep = position + 1
                Log.d("PeminjamanActivity", "Page changed to: $position, updating to step: $currentStep")
                updateProgressStep(currentStep)
            }
        })
    }

    /**
     * Update progress step indicator dengan animasi
     * @param currentStep Current step (1, 2, or 3)
     */
    private fun updateProgressStep(currentStep: Int) {
        Log.d("PeminjamanActivity", "Updating progress to step: $currentStep")

        when (currentStep) {
            1 -> {
                // Step 1: Active
                // Step 2, 3: Inactive
                // Lines: Inactive
                setStepActive(step1, tvStep1a, tvStep1b, animate = true)
                setStepInactive(step2, tvStep2a, tvStep2b, animate = true)
                setStepInactive(step3, tvStep3a, tvStep3b, animate = true)
                setLineInactive(lineStep12, animate = true)
                setLineInactive(lineStep23, animate = true)
            }
            2 -> {
                // Step 1: Active
                // Step 2: Active
                // Step 3: Inactive
                // Line 1-2: Active
                // Line 2-3: Inactive
                setStepActive(step1, tvStep1a, tvStep1b, animate = false) // Already active
                setStepActive(step2, tvStep2a, tvStep2b, animate = true)
                setStepInactive(step3, tvStep3a, tvStep3b, animate = false) // Already inactive
                setLineActive(lineStep12, animate = true)
                setLineInactive(lineStep23, animate = true)
            }
            3 -> {
                // Step 1, 2, 3: Active
                // Line 1-2, 2-3: Active
                setStepActive(step1, tvStep1a, tvStep1b, animate = false) // Already active
                setStepActive(step2, tvStep2a, tvStep2b, animate = false) // Already active
                setStepActive(step3, tvStep3a, tvStep3b, animate = true)
                setLineActive(lineStep12, animate = false) // Already active
                setLineActive(lineStep23, animate = true)
            }
        }
    }

    private fun setStepActive(stepCircle: TextView, stepLabel1: TextView, stepLabel2: TextView, animate: Boolean = true) {
        if (animate) {
            // Animate background color
            animateStepBackground(stepCircle, R.drawable.circle_active)

            // Animate text color
            animateTextColor(stepCircle, whiteColor)
            animateTextColor(stepLabel1, activeTextColor)
            animateTextColor(stepLabel2, activeTextColor)
        } else {
            // Set immediately without animation
            stepCircle.background = ContextCompat.getDrawable(this, R.drawable.circle_active)
            stepCircle.setTextColor(whiteColor)
            stepLabel1.setTextColor(activeTextColor)
            stepLabel2.setTextColor(activeTextColor)
        }

        Log.d("PeminjamanActivity", "Step ${stepCircle.text} set to active")
    }

    private fun setStepInactive(stepCircle: TextView, stepLabel1: TextView, stepLabel2: TextView, animate: Boolean = true) {
        if (animate) {
            // Animate background color
            animateStepBackground(stepCircle, R.drawable.circle_inactive)

            // Animate text color
            animateTextColor(stepCircle, inactiveTextColor)
            animateTextColor(stepLabel1, inactiveTextColor)
            animateTextColor(stepLabel2, inactiveTextColor)
        } else {
            // Set immediately without animation
            stepCircle.background = ContextCompat.getDrawable(this, R.drawable.circle_inactive)
            stepCircle.setTextColor(inactiveTextColor)
            stepLabel1.setTextColor(inactiveTextColor)
            stepLabel2.setTextColor(inactiveTextColor)
        }

        Log.d("PeminjamanActivity", "Step ${stepCircle.text} set to inactive")
    }

    private fun setLineActive(line: View, animate: Boolean = true) {
        if (animate) {
            animateLineColor(line, activeColor)
        } else {
            line.setBackgroundColor(activeColor)
        }

        Log.d("PeminjamanActivity", "Line set to active (dark_blue)")
    }

    private fun setLineInactive(line: View, animate: Boolean = true) {
        if (animate) {
            animateLineColor(line, inactiveColor)
        } else {
            line.setBackgroundColor(inactiveColor)
        }

        Log.d("PeminjamanActivity", "Line set to inactive (light_gray)")
    }

    private fun animateStepBackground(stepCircle: TextView, drawableRes: Int) {
        // Simple background change with slight scale animation
        stepCircle.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(150)
            .withEndAction {
                stepCircle.background = ContextCompat.getDrawable(this, drawableRes)
                stepCircle.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun animateTextColor(textView: TextView, targetColor: Int) {
        val currentColor = textView.currentTextColor

        if (currentColor != targetColor) {
            val colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), currentColor, targetColor)
            colorAnimator.duration = 300
            colorAnimator.addUpdateListener { animator ->
                textView.setTextColor(animator.animatedValue as Int)
            }
            colorAnimator.start()
        }
    }

    private fun animateLineColor(line: View, targetColor: Int) {
        // Get current background color (default to inactive color if can't get)
        val currentColor = try {
            (line.background as? android.graphics.drawable.ColorDrawable)?.color ?: inactiveColor
        } catch (e: Exception) {
            inactiveColor
        }

        if (currentColor != targetColor) {
            val colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), currentColor, targetColor)
            colorAnimator.duration = 300
            colorAnimator.addUpdateListener { animator ->
                line.setBackgroundColor(animator.animatedValue as Int)
            }
            colorAnimator.start()
        }
    }

    fun navigateToNextPage(data: Bundle) {
        // Update shared data in adapter
        pagerAdapter.updateSharedData(data)

        // Navigate to next page
        val nextItem = viewPager.currentItem + 1
        if (nextItem < pagerAdapter.itemCount) {
            viewPager.setCurrentItem(nextItem, true)

            // Progress will be updated automatically by page change callback
            Log.d("PeminjamanActivity", "Navigating to page: ${nextItem + 1}")
        }
    }

    fun navigateToPreviousPage() {
        val previousItem = viewPager.currentItem - 1
        if (previousItem >= 0) {
            viewPager.setCurrentItem(previousItem, true)

            // Progress will be updated automatically by page change callback
            Log.d("PeminjamanActivity", "Navigating back to page: ${previousItem + 1}")
        }
    }

    /**
     * Get current step number (1-based)
     */
    fun getCurrentStep(): Int {
        return viewPager.currentItem + 1
    }

    /**
     * Navigate to specific step with progress update
     * @param step Step number (1, 2, or 3)
     */
    fun navigateToStep(step: Int) {
        val targetPosition = step - 1
        if (targetPosition >= 0 && targetPosition < pagerAdapter.itemCount) {
            viewPager.setCurrentItem(targetPosition, true)
            // Progress will be updated automatically by page change callback
        }
    }

    /**
     * Check if we can proceed to next step
     */
    fun canProceedToNextStep(): Boolean {
        return viewPager.currentItem < pagerAdapter.itemCount - 1
    }

    /**
     * Check if we can go back to previous step
     */
    fun canGoBackToPreviousStep(): Boolean {
        return viewPager.currentItem > 0
    }

    /**
     * Manually update progress (for testing or special cases)
     */
    fun updateProgress(step: Int) {
        updateProgressStep(step)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clear SharedViewModel data saat activity selesai
        if (isFinishing) {
            Log.d("PeminjamanActivity", "Activity finishing, clearing SharedViewModel data")
            sharedViewModel.clearData()
        }
    }
}