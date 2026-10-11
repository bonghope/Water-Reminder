package com.aqua.water.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/** Animated flame with milestone colors, sparks, side flames and orbiting halos. */
public final class FlameView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path flame = new Path();
    private final float[] rainbow = {0, 0.7f, 1};
    private int days, tier;
    private boolean animate;
    public FlameView(Context context) { this(context, null); }
    public FlameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        flame.moveTo(50, 94);
        flame.cubicTo(8, 94, 14, 57, 30, 40);
        flame.cubicTo(25, 58, 41, 51, 48, 8);
        flame.cubicTo(64, 28, 56, 44, 73, 35);
        flame.cubicTo(91, 65, 89, 94, 50, 94);
        flame.close();
        setStreak(0);
    }
    public void setStreak(int days) {
        this.days = Math.max(0, days); tier = StreakFlame.tier(this.days);
        setContentDescription(StreakFlame.name(this.days) + " • " + this.days + " ngày");
        invalidate();
    }
    @Override public void onVisibilityAggregated(boolean visible) {
        super.onVisibilityAggregated(visible); animate = visible; if (visible) invalidate();
    }
    @Override protected void onDetachedFromWindow() { animate = false; super.onDetachedFromWindow(); }
    private void drawFlame(Canvas canvas, float scale, int color) {
        canvas.save(); canvas.translate(50, 92); canvas.scale(scale, scale); canvas.translate(-50, -92);
        paint.setStyle(Paint.Style.FILL); paint.setColor(color); canvas.drawPath(flame, paint);
        canvas.restore();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        canvas.save(); canvas.translate((getWidth()-size)/2f, (getHeight()-size)/2f); canvas.scale(size/100f,size/100f);
        float time = SystemClock.uptimeMillis()/1000f;
        int color = StreakFlame.color(days);
        if (tier == 7 && days > 0) {
            rainbow[0] = (time * 35) % 360;
            color = android.graphics.Color.HSVToColor(rainbow);
        }
        paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setAlpha(days > 0 ? 35 : 15);
        canvas.drawCircle(50,60,34 + (float)Math.sin(time*2)*3,paint);
        if (tier >= 4) {
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.5f); paint.setAlpha(120);
            canvas.drawOval(10,12,90,96,paint);
            if (tier >= 6) canvas.drawOval(4,20,96,87,paint);
        }
        paint.setAlpha(255);
        canvas.save();
        float pulse = days > 0 ? 1 + (float)Math.sin(time*(3+tier))*0.025f*(1+tier/3f) : 1;
        canvas.translate(50,90); canvas.scale(pulse,pulse); canvas.translate(-50,-90);
        if (tier >= 3) {
            canvas.save(); canvas.translate(-23,0); drawFlame(canvas,0.55f,color); canvas.restore();
            canvas.save(); canvas.translate(23,0); drawFlame(canvas,0.55f,color); canvas.restore();
        }
        drawFlame(canvas,0.87f,color);
        drawFlame(canvas,0.5f,days > 0 ? 0xFFFFE082 : 0xFFCBD5E1);
        drawFlame(canvas,0.26f,days > 0 ? 0xFFFFF8E1 : 0xFFE2E8F0);
        canvas.restore();
        if (tier >= 2 && days > 0) {
            paint.setStyle(Paint.Style.FILL);
            for (int i=0;i<tier+1;i++) {
                float phase = (time*(0.45f+tier*0.05f)+i/(float)(tier+1))%1;
                paint.setColor(color); paint.setAlpha((int)(220*(1-phase)));
                canvas.drawCircle(25+(i*17)%53+(float)Math.sin(time+i)*5,80-phase*75,1.8f+(tier>=5?1:0),paint);
            }
        }
        canvas.restore();
        if (animate && days > 0) postInvalidateDelayed(33);
    }
}
