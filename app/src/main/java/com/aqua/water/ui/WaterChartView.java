package com.aqua.water.ui;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

/** XML-hosted chart. The Activity supplies the data rather than querying inside draw. */
public final class WaterChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean dark;
    private final float density;
    private int[] values = new int[0];
    private String[] labels = new String[0];
    private int goal = 2000;

    public WaterChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        dark = context.getSharedPreferences("AppConfig", 0).getBoolean("THEME_DARK", false);
    }

    public void setData(int[] values, String[] labels, int goal) {
        this.values = values.clone();
        this.labels = labels.clone();
        this.goal = Math.max(1, goal);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        if (values.length == 0) return;
        float left = 42 * density, top = 20 * density, bottom = getHeight() - 32 * density;
        float width = (getWidth() - left - 8 * density) / values.length;
        int max = goal;
        for (int value : values) max = Math.max(max, value);
        max = Math.max(1, (int) (max * 1.15f));
        int muted = dark ? 0xffa2bfd0 : 0xff7393aa;
        paint.setTextSize(10 * density);
        for (int j = 0; j <= 4; j++) {
            float y = bottom - (bottom - top) * j / 4;
            paint.setColor(muted);
            canvas.drawText("" + (max * j / 4), 0, y, paint);
            paint.setColor(dark ? 0xff254354 : 0xffe2eff6);
            canvas.drawLine(left, y, getWidth(), y, paint);
        }
        paint.setColor(0xff00a6d7);
        paint.setPathEffect(new DashPathEffect(new float[]{8, 6}, 0));
        float goalY = bottom - (bottom - top) * goal / max;
        canvas.drawLine(left, goalY, getWidth(), goalY, paint);
        paint.setPathEffect(null);
        for (int i = 0; i < values.length; i++) {
            float x = left + i * width;
            paint.setColor(0xff00a6d7);
            canvas.drawRoundRect(x + width * .17f, bottom - (bottom - top) * values[i] / max,
                    x + width * .83f, bottom, 3 * density, 3 * density, paint);
            if (values.length == 7 || i % 5 == 0) {
                paint.setColor(muted);
                paint.setTextSize(9 * density);
                canvas.drawText(labels[i], x, bottom + 19 * density, paint);
            }
        }
    }
}
