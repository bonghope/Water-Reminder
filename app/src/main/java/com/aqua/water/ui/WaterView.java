package com.aqua.water.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;
import android.view.View;
import android.util.AttributeSet;


public final class WaterView extends View implements SensorEventListener {
    public WaterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0, context.getSharedPreferences("AppConfig", 0)
                .getBoolean("THEME_DARK", false));
    }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path water = new Path();
    private final SensorManager sensors;
    private final Sensor accelerometer;
    private final float density;
    private final boolean dark;
    private final float[] gravity = new float[3];
    private LinearGradient gradient;
    private boolean active, running, gravityReady;
    private long lastFrame, lastSensor;
    private float targetLevel, level, targetAngle, angle, velocity, energy, phase;

    public WaterView(Context context, float progress, boolean dark) {
        this(context, null, progress, dark);
    }

    private WaterView(Context context, AttributeSet attrs, float progress, boolean dark) {
        super(context, attrs);
        this.dark = dark;
        density = getResources().getDisplayMetrics().density;
        targetLevel = Math.max(0, Math.min(1, progress));
        level = targetLevel;
        sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);
    }

    public void setProgress(float progress) {
        targetLevel = Math.max(0, Math.min(1, progress));
    }

    public void setActive(boolean active) {
        this.active = active;
        updateRunning();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateRunning();
    }

    @Override protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        updateRunning();
    }

    private void updateRunning() {
        if (active && isAttachedToWindow() && getWindowVisibility() == VISIBLE) {
            if (running) return;
            running = true;
            lastFrame = lastSensor = 0;
            gravityReady = false;
            if (accelerometer != null) sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            postOnAnimation(frame);
        } else {
            stop();
        }
    }

    private void stop() {
        running = false;
        removeCallbacks(frame);
        if (sensors != null) sensors.unregisterListener(this);
    }

    private final Runnable frame = new Runnable() {
        @Override public void run() {
            if (!running) return;
            long now = System.nanoTime();
            float dt = lastFrame == 0 ? 1f / 60 : Math.min(.04f, (now - lastFrame) / 1_000_000_000f);
            lastFrame = now;
            // Damped spring: water overshoots after a shake, then settles.
            float angleDelta = (float) Math.IEEEremainder(targetAngle - angle, 360.0);
            velocity += (angleDelta * 22f - velocity * 7f) * dt;
            angle = (float) Math.IEEEremainder(angle + velocity * dt, 360.0);
            energy *= (float) Math.exp(-1.8f * dt);
            level += (targetLevel - level) * (1f - (float) Math.exp(-3f * dt));
            phase = (phase + dt * (1.4f + energy * 2f)) % ((float) Math.PI * 2);
            invalidate();
            postOnAnimation(this);
        }
    };

    @Override public void onSensorChanged(SensorEvent event) {
        if (!running || event.values.length < 3) return;
        if (!gravityReady) {
            System.arraycopy(event.values, 0, gravity, 0, 3);
            gravityReady = true;
            lastSensor = event.timestamp;
        }
        float dt = Math.max(.001f, Math.min(.1f, (event.timestamp - lastSensor) / 1_000_000_000f));
        lastSensor = event.timestamp;
        float smoothing = 1f - (float) Math.exp(-dt / .18f);
        float shakeSquared = 0;
        for (int i = 0; i < 3; i++) {
            gravity[i] += smoothing * (event.values[i] - gravity[i]);
            float impulse = event.values[i] - gravity[i];
            shakeSquared += impulse * impulse;
        }
        float gx = gravity[0], gy = gravity[1];
        int rotation = getDisplay() == null ? Surface.ROTATION_0 : getDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_90: gx = -gravity[1]; gy = gravity[0]; break;
            case Surface.ROTATION_180: gx = -gravity[0]; gy = -gravity[1]; break;
            case Surface.ROTATION_270: gx = gravity[1]; gy = -gravity[0]; break;
        }

        if (Math.hypot(gx, gy) > 2f) {
            targetAngle = (float) Math.toDegrees(Math.atan2(gx, gy));
        }
        float shake = (float) Math.sqrt(shakeSquared);
        if (shake > .8f) {
            energy = Math.min(1.5f, energy + (shake - .8f) * dt * .7f);

        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        gradient = new LinearGradient(0, 0, 0, Math.max(1, h),
                dark ? 0xff227ca1 : 0xff83e7f5, dark ? 0xff0b4169 : 0xff079ccc, Shader.TileMode.CLAMP);
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w == 0 || h == 0) return;

        float cx = w / 2f;
        float cy = h / 2f;
        double radians = Math.toRadians(angle);
        float sin = (float) Math.abs(Math.sin(radians));
        float cos = (float) Math.abs(Math.cos(radians));

        // Kích thước khung nhìn chiếu lên trục vuông góc với mặt nước.
        float projectedWidth = w * sin;
        float projectedHeight = h * cos;
        float shortSpan = Math.min(projectedWidth, projectedHeight);
        float longSpan = Math.max(projectedWidth, projectedHeight);
        float halfDepth = (shortSpan + longSpan) / 2f;

        float visibleLevel = Math.max(24f * density / h, Math.min(.97f, level));
        visibleLevel = Math.min(.97f, visibleLevel);
        float tail = Math.min(visibleLevel, 1f - visibleLevel);
        float triangleFraction = shortSpan / (2f * longSpan);

        float depthFromEdge;
        if (tail < triangleFraction) {
            depthFromEdge = (float) Math.sqrt(2f * tail * shortSpan * longSpan);
        } else {
            depthFromEdge = tail * longSpan + shortSpan / 2f;
        }

        float offset = halfDepth - depthFromEdge;
        float surfaceY = cy + (visibleLevel <= .5f ? offset : -offset);
        float halfReach = (w * cos + h * sin) / 2f + 8f * density;
        float left = cx - halfReach;
        float right = cx + halfReach;
        float bottom = cy + halfDepth + 64f * density;
        float amplitude = density * (5f + energy * 20f);

        canvas.save();
        canvas.clipRect(0f, 0f, w, h);
        canvas.rotate(angle, cx, cy);

        for (int layer = 0; layer < 2; layer++) {
            water.reset();
            for (float x = left; ; x += 8f * density) {
                float px = Math.min(x, right);
                float wave = (float) Math.sin(px / w * Math.PI * 2 + phase + layer * 1.7f);
                float ripple = (float) Math.sin(px / w * Math.PI * 4 - phase * 1.3f);
                float py = surfaceY + wave * amplitude
                        + ripple * amplitude * .25f + layer * 9f * density;

                if (x == left) {
                    water.moveTo(px, py);
                } else {
                    water.lineTo(px, py);
                }
                if (px == right) break;
            }
            water.lineTo(right, bottom);
            water.lineTo(left, bottom);
            water.close();

            paint.setShader(gradient);
            paint.setAlpha(layer == 0 ? 100 : 220);
            canvas.drawPath(water, paint);
        }

        paint.setShader(null);
        paint.setColor(dark ? 0x305bd7e8 : 0x55ffffff);
        float visibleBottom = cy + halfDepth;
        float span = Math.max(1f, visibleBottom - surfaceY);
        for (int i = 0; i < 10; i++) {
            float x = left + (right - left) * (i + .5f) / 10f;
            x += (float) Math.sin(phase + i) * density * 8f;
            float travel = (phase / ((float) Math.PI * 2f) + i * .137f) % 1f;
            float bubbleY = visibleBottom - travel * span;
            if (bubbleY > surfaceY + amplitude * 2f) {
                canvas.drawCircle(x, bubbleY, density * (2 + i % 3), paint);
            }
        }

        canvas.restore();
    }
}
