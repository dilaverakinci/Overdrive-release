package com.overdrive.app.ui.component

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

/**
 * A robust [FrameLayout] container hosting a [ComposeView] that guards against infinite/unbounded
 * height constraints during measure passes from legacy parent ViewGroups (e.g. [android.widget.LinearLayout]
 * with `layout_weight`, or [androidx.coordinatorlayout.widget.CoordinatorLayout]).
 *
 * Background:
 * Android's [android.widget.LinearLayout] executes a preliminary measure pass for weighted children
 * using `MeasureSpec.UNSPECIFIED` (infinite constraints). Jetpack Compose's vertically scrollable
 * components (`Modifier.verticalScroll` or `LazyColumn`) disallow unbounded height constraints and
 * throw [IllegalStateException].
 *
 * Solution:
 * When [MeasureSpec.UNSPECIFIED] is supplied during the parent's preliminary pass,
 * [OverdriveComposeContainer] clamps the constraint to [MeasureSpec.AT_MOST] with the screen dimensions,
 * allowing Compose to measure cleanly. The parent ViewGroup subsequently finalizes the exact dimensions
 * in its second pass (`MeasureSpec.EXACTLY`).
 */
class OverdriveComposeContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val composeView: ComposeView = ComposeView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    init {
        addView(composeView)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val safeHeightSpec = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            MeasureSpec.makeMeasureSpec(resources.displayMetrics.heightPixels, MeasureSpec.AT_MOST)
        } else {
            heightMeasureSpec
        }

        val safeWidthSpec = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels, MeasureSpec.AT_MOST)
        } else {
            widthMeasureSpec
        }

        super.onMeasure(safeWidthSpec, safeHeightSpec)
    }

    fun setContent(content: @Composable () -> Unit) {
        composeView.setContent(content)
    }

    fun setViewCompositionStrategy(strategy: ViewCompositionStrategy) {
        composeView.setViewCompositionStrategy(strategy)
    }
}
