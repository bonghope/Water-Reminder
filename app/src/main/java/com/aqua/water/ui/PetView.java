package com.aqua.water.ui;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

/** The mascot is drawn inside the size and position declared in screen_pet.xml. */
public final class PetView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final int ink;
    private int level = 0;

    public PetView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        ink = context.getSharedPreferences("AppConfig", 0).getBoolean("THEME_DARK", false) ? 0xffe4f5ff : 0xff183e64;
    }

    public void setLevel(int level) {
        this.level = Math.max(0, Math.min(3, level));
        invalidate();
    }

    public int getLevel() {
        return level;
    }

    @Override protected void onDraw(Canvas canvas) {
        float x = getWidth() / 2f, y = getHeight() * .48f;
        float baseR = Math.min(getWidth(), getHeight()) * .25f;
        float scale = level == 0 ? 0.8f : level == 1 ? 0.95f : level == 2 ? 1.05f : 1.18f;
        float r = baseR * scale;

        // Vòng hào quang hoàng kim khi đạt cấp 4 (Level 3 - hoàn thành 100%)
        if (level >= 3) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4 * density);
            paint.setColor(0x80FFD700);
            canvas.drawCircle(x, y, r * 1.35f, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        // Đáy bóng nước
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(level >= 3 ? 0xfffff3c4 : 0xffd9f7ff);
        canvas.drawOval(x-r*1.5f, y+r*.8f, x+r*1.5f, y+r*1.2f, paint);

        // Thân giọt nước
        Path drop = new Path();
        drop.moveTo(x, y-r*1.5f);
        drop.cubicTo(x+r*2, y+r*.2f, x+r, y+r, x, y+r);
        drop.cubicTo(x-r, y+r, x-r*2, y+r*.2f, x, y-r*1.5f);
        paint.setColor(level == 0 ? 0xff9cebf8 : level == 1 ? 0xff72dff3 : level == 2 ? 0xff48cde8 : 0xff25bde0);
        canvas.drawPath(drop, paint);

        // Mắt
        paint.setColor(ink);
        float eyeSize = (level == 0 ? 3.5f : level == 3 ? 4.5f : 4f) * density;
        canvas.drawCircle(x-r*.35f, y, eyeSize, paint);
        canvas.drawCircle(x+r*.35f, y, eyeSize, paint);

        // Miệng cười
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3*density);
        if (level == 0) {
            canvas.drawArc(x-r*.15f, y+r*.05f, x+r*.15f, y+r*.25f, 20, 140, false, paint);
        } else if (level == 1) {
            canvas.drawArc(x-r*.22f, y, x+r*.22f, y+r*.35f, 10, 160, false, paint);
        } else {
            canvas.drawArc(x-r*.28f, y-r*.05f, x+r*.28f, y+r*.4f, 10, 160, false, paint);
        }

        // Má hồng
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(level >= 3 ? 0xffff8da1 : 0xffffacc2);
        float blushR = (level == 0 ? 4.5f : level == 3 ? 7.5f : 6f) * density;
        canvas.drawCircle(x-r*.6f, y+r*.15f, blushR, paint);
        canvas.drawCircle(x+r*.6f, y+r*.15f, blushR, paint);
    }
}