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

    public PetView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        ink = context.getSharedPreferences("AppConfig", 0).getBoolean("THEME_DARK", false) ? 0xffe4f5ff : 0xff183e64;
    }

    @Override protected void onDraw(Canvas canvas) {
        float x = getWidth() / 2f, y = getHeight() * .48f, r = Math.min(getWidth(), getHeight()) * .25f;
        paint.setColor(0xffd9f7ff);
        canvas.drawOval(x-r*1.5f, y+r*.8f, x+r*1.5f, y+r*1.2f, paint);
        Path drop = new Path();
        drop.moveTo(x, y-r*1.5f);
        drop.cubicTo(x+r*2, y+r*.2f, x+r, y+r, x, y+r);
        drop.cubicTo(x-r, y+r, x-r*2, y+r*.2f, x, y-r*1.5f);
        paint.setColor(0xff72dff3);
        canvas.drawPath(drop, paint);
        paint.setColor(ink);
        canvas.drawCircle(x-r*.35f, y, 4*density, paint);
        canvas.drawCircle(x+r*.35f, y, 4*density, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3*density);
        canvas.drawArc(x-r*.22f, y, x+r*.22f, y+r*.35f, 10, 160, false, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xffffacc2);
        canvas.drawCircle(x-r*.6f, y+r*.15f, 6*density, paint);
        canvas.drawCircle(x+r*.6f, y+r*.15f, 6*density, paint);
    }
}
