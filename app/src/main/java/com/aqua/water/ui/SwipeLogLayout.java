package com.aqua.water.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import com.aqua.water.R;

/** Reveals row actions on a horizontal swipe while allowing vertical scrolling. */
public final class SwipeLogLayout extends FrameLayout {
    private View foreground, actions;
    private float startX, startY, startOffset;
    private boolean dragging;
    private final int slop;

    public SwipeLogLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        foreground = findViewById(R.id.log_foreground);
        actions = findViewById(R.id.log_actions);
        foreground.setOnClickListener(v -> close());
    }

    public void close() { settle(false); }
    private void settle(boolean open) {
        actions.setVisibility(open ? VISIBLE : INVISIBLE);
        foreground.animate().translationX(open ? -actions.getWidth() : 0).setDuration(180).start();
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            startX = event.getX(); startY = event.getY();
            startOffset = foreground.getTranslationX(); dragging = false;
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float dx = event.getX() - startX, dy = event.getY() - startY;
            if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy)) {
                dragging = true;
                foreground.animate().cancel();
                actions.setVisibility(VISIBLE);
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
        }
        return false;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!dragging) return super.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float offset = startOffset + event.getX() - startX;
            foreground.setTranslationX(Math.max(-actions.getWidth(), Math.min(0, offset)));
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            settle(event.getActionMasked() != MotionEvent.ACTION_CANCEL && foreground.getTranslationX() < -actions.getWidth() / 2f);
            dragging = false;
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        return true;
    }
}
