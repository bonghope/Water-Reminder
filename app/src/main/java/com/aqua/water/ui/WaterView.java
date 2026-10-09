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

/** Full-window water backdrop, driven by gravity and short acceleration impulses. */
public final class WaterView extends View implements SensorEventListener {
    public WaterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0, context.getSharedPreferences("AppConfig", 0).getBoolean("THEME_DARK", false));
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
    private float targetLevel, level, targetSlope, slope, velocity, energy, phase;

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
            velocity += ((targetSlope - slope) * 22f - velocity * 7f) * dt;
            slope = Math.max(-.65f, Math.min(.65f, slope + velocity * dt));
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
        // Flat on a table: avoid amplifying noisy horizontal gravity readings.
        targetSlope = Math.max(-.6f, Math.min(.6f, -gx / Math.max(4f, Math.abs(gy))));
        float shake = (float) Math.sqrt(shakeSquared);
        if (shake > .8f) {
            energy = Math.min(1.5f, energy + (shake - .8f) * dt * .7f);
            float impulseX = event.values[0] - gravity[0];
            if (rotation == Surface.ROTATION_90) impulseX = -(event.values[1] - gravity[1]);
            else if (rotation == Surface.ROTATION_180) impulseX = -impulseX;
            else if (rotation == Surface.ROTATION_270) impulseX = event.values[1] - gravity[1];
            velocity = Math.max(-3f, Math.min(3f, velocity - impulseX * dt * .6f));
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        gradient = new LinearGradient(0, 0, 0, Math.max(1, h),
                dark ? 0xff227ca1 : 0xff83e7f5, dark ? 0xff0b4169 : 0xff079ccc, Shader.TileMode.CLAMP);
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        // Small idle ripple remains visible at zero; the main level follows intake.
        float visibleLevel = Math.max(24 * density / h, Math.min(.97f, level));
        float y = h * (1f - visibleLevel);
        float amplitude = density * (5f + energy * 20f);
        for (int layer = 0; layer < 2; layer++) {
            water.reset();
            for (float x = 0; x <= w + 8 * density; x += 8 * density) {
                float px = Math.min(w, x);
                float wave = (float) Math.sin(px / w * Math.PI * 2 + phase + layer * 1.7f);
                float ripple = (float) Math.sin(px / w * Math.PI * 4 - phase * 1.3f);
                float py = y + (px - w / 2) * slope + wave * amplitude + ripple * amplitude * .25f + layer * 9 * density;
                if (x == 0) water.moveTo(px, py); else water.lineTo(px, py);
            }
            water.lineTo(w, h); water.lineTo(0, h); water.close();
            paint.setShader(gradient);
            paint.setAlpha(layer == 0 ? 100 : 220);
            canvas.drawPath(water, paint);
        }
        paint.setShader(null);
        paint.setColor(dark ? 0x305bd7e8 : 0x55ffffff);
        for (int i = 0; i < 10; i++) {
            float x = w * (i + .5f) / 10 + (float) Math.sin(phase + i) * density * 8;
            float span = Math.max(1, h - y);
            float by = h - ((phase / ((float) Math.PI * 2) + i * .137f) % 1f) * span;
            if (by > y + (x - w / 2) * slope + amplitude * 2)
                canvas.drawCircle(x, by, density * (2 + i % 3), paint);
        }
    }
}
