package com.aqua.water.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import com.aqua.water.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animated Mascot View rendering multi-level water pet evolution (Lv 1 - 4)
 * with idle breathing, floating hovering, ambient particles, golden royal aura,
 * and interactive tap bounce physics.
 */
public final class PetView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private int level = 0;

    private Drawable drawableLv1;
    private Drawable drawableLv2;
    private Drawable drawableLv3;
    private Drawable drawableLv4;

    private ValueAnimator idleAnimator;
    private ValueAnimator bounceAnimator;

    private float idleFraction = 0f;
    private float bounceFraction = 0f;
    private boolean isBouncing = false;

    // Ambient floating bubbles & crystal particles
    private final List<Particle> ambientParticles = new ArrayList<>();
    private final List<BurstParticle> burstParticles = new ArrayList<>();
    private final Random random = new Random();

    private OnPetClickListener petClickListener;

    public interface OnPetClickListener {
        void onPetClicked(int level);
    }

    private static class Particle {
        float xNorm; // 0..1 relative to width
        float yNorm; // 0..1 relative to height
        float radiusDp;
        float speed;
        int color;
        float alpha;
        boolean behind;
    }

    private static class BurstParticle {
        float x;
        float y;
        float vx;
        float vy;
        float life; // 1.0 -> 0.0
        String symbol;
        int color;
    }

    public PetView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        initDrawables();
        initParticles();
        setupAnimators();
    }

    private void initDrawables() {
        try {
            drawableLv1 = getContext().getDrawable(R.drawable.pet_lv1);
            if (drawableLv1 == null) {
                drawableLv1 = getContext().getDrawable(R.drawable.pet);
            }
        } catch (Exception ignored) {
            drawableLv1 = getContext().getDrawable(R.drawable.pet);
        }
        try {
            drawableLv2 = getContext().getDrawable(R.drawable.pet_lv2);
        } catch (Exception ignored) {}
        try {
            drawableLv3 = getContext().getDrawable(R.drawable.pet_lv3);
        } catch (Exception ignored) {}
        try {
            drawableLv4 = getContext().getDrawable(R.drawable.pet_lv4);
        } catch (Exception ignored) {}
    }

    private void initParticles() {
        ambientParticles.clear();
        for (int i = 0; i < 14; i++) {
            Particle p = new Particle();
            p.xNorm = 0.2f + random.nextFloat() * 0.6f;
            p.yNorm = random.nextFloat();
            p.radiusDp = 2f + random.nextFloat() * 3.5f;
            p.speed = 0.003f + random.nextFloat() * 0.005f;
            p.alpha = 0.3f + random.nextFloat() * 0.5f;
            p.behind = random.nextBoolean();
            p.color = random.nextBoolean() ? 0x995ed0ff : 0x99bae6fd;
            ambientParticles.add(p);
        }
    }

    private void setupAnimators() {
        idleAnimator = ValueAnimator.ofFloat(0f, 1f);
        idleAnimator.setDuration(2600);
        idleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        idleAnimator.setRepeatMode(ValueAnimator.REVERSE);
        idleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        idleAnimator.addUpdateListener(anim -> {
            idleFraction = (float) anim.getAnimatedValue();
            updateParticles();
            postInvalidateOnAnimation();
        });

        bounceAnimator = ValueAnimator.ofFloat(0f, 1f);
        bounceAnimator.setDuration(600);
        bounceAnimator.setInterpolator(new OvershootInterpolator(2.2f));
        bounceAnimator.addUpdateListener(anim -> {
            bounceFraction = (float) anim.getAnimatedValue();
            postInvalidateOnAnimation();
        });
        bounceAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                isBouncing = false;
                bounceFraction = 0f;
            }
        });
    }

    private void updateParticles() {
        for (Particle p : ambientParticles) {
            p.yNorm -= p.speed;
            if (p.yNorm < 0.1f) {
                p.yNorm = 0.95f;
                p.xNorm = 0.2f + random.nextFloat() * 0.6f;
            }
        }

        // Update burst particles
        for (int i = burstParticles.size() - 1; i >= 0; i--) {
            BurstParticle bp = burstParticles.get(i);
            bp.x += bp.vx;
            bp.y += bp.vy;
            bp.vy += 0.25f * density; // subtle gravity
            bp.life -= 0.035f;
            if (bp.life <= 0) {
                burstParticles.remove(i);
            }
        }
    }

    public void setLevel(int level) {
        this.level = Math.max(0, Math.min(3, level));
        // Refresh particle colors based on level
        for (Particle p : ambientParticles) {
            if (this.level >= 3) {
                p.color = random.nextBoolean() ? 0xbbf59e0b : 0xbb38bdf8;
            } else if (this.level == 2) {
                p.color = random.nextBoolean() ? 0xaa38bdf8 : 0xaae0f2fe;
            } else if (this.level == 1) {
                p.color = random.nextBoolean() ? 0x9922c55e : 0x995ed0ff;
            } else {
                p.color = 0x8878aeff;
            }
        }
        invalidate();
    }

    public int getLevel() {
        return level;
    }

    public void setOnPetClickListener(OnPetClickListener listener) {
        this.petClickListener = listener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (idleAnimator != null && !idleAnimator.isRunning()) {
            idleAnimator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (idleAnimator != null) {
            idleAnimator.cancel();
        }
        if (bounceAnimator != null) {
            bounceAnimator.cancel();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            triggerTapAnimation(event.getX(), event.getY());
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if (petClickListener != null) {
                petClickListener.onPetClicked(level);
            }
            performClick();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void triggerTapAnimation(float touchX, float touchY) {
        isBouncing = true;
        bounceAnimator.cancel();
        bounceAnimator.start();

        // Spawn joy burst hearts & bubbles
        float cx = getWidth() / 2f;
        float cy = getHeight() * 0.45f;
        for (int i = 0; i < 7; i++) {
            BurstParticle bp = new BurstParticle();
            bp.x = cx + (random.nextFloat() - 0.5f) * 40 * density;
            bp.y = cy + (random.nextFloat() - 0.5f) * 30 * density;
            bp.vx = (random.nextFloat() - 0.5f) * 6 * density;
            bp.vy = (-3f - random.nextFloat() * 5f) * density;
            bp.life = 1.0f;
            bp.symbol = "";
            bp.color = (level >= 3) ? 0xffffb703 : (i % 2 == 0 ? 0xffff4d6d : 0xff38bdf8);
            burstParticles.add(bp);
        }
    }

    private Drawable getDrawableForLevel() {
        switch (level) {
            case 1:
                return drawableLv2 != null ? drawableLv2 : drawableLv1;
            case 2:
                return drawableLv3 != null ? drawableLv3 : (drawableLv2 != null ? drawableLv2 : drawableLv1);
            case 3:
                return drawableLv4 != null ? drawableLv4 : (drawableLv3 != null ? drawableLv3 : drawableLv1);
            default:
                return drawableLv1;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cx = w / 2f;
        float cy = h * 0.48f;

        // Floating hover calculation
        float hoverY = (float) Math.sin(idleFraction * Math.PI) * 9 * density;
        float squishX = 1.0f + 0.032f * (idleFraction - 0.5f);
        float squishY = 1.0f - 0.032f * (idleFraction - 0.5f);

        // Tap bounce physical calculation
        float bounceY = 0f;
        float bounceScaleX = 1f;
        float bounceScaleY = 1f;
        if (isBouncing) {
            float b = bounceFraction; // 0 -> 1
            // Jump up then settle
            bounceY = -30 * density * (float) Math.sin(b * Math.PI);
            bounceScaleX = 1f + 0.12f * (float) Math.sin(b * Math.PI * 2);
            bounceScaleY = 1f - 0.12f * (float) Math.sin(b * Math.PI * 2);
        }

        float baseSize = Math.min(w, h) * 0.78f;
        float levelScale = level == 0 ? 0.85f : level == 1 ? 0.94f : level == 2 ? 1.03f : 1.15f;
        float drawW = baseSize * levelScale * squishX * bounceScaleX;
        float drawH = baseSize * levelScale * squishY * bounceScaleY;

        float mascotCenterY = cy + hoverY + bounceY;

        // 1. Shadow on ground
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(level >= 3 ? 0x33f59e0b : 0x22183e64);
        float shadowScale = 1f - (hoverY + bounceY) / (120f * density);
        float shadowR = (drawW * 0.38f) * Math.max(0.6f, shadowScale);
        canvas.drawOval(cx - shadowR, h * 0.88f, cx + shadowR, h * 0.94f, paint);

        // 2. Ambient particles behind body
        drawAmbientParticles(canvas, true, w, h);

        // 3. Render Mascot VectorDrawable (Clean, no halo ring)
        Drawable mascot = getDrawableForLevel();
        if (mascot != null) {
            int left = (int) (cx - drawW / 2f);
            int top = (int) (mascotCenterY - drawH / 2f);
            int right = (int) (cx + drawW / 2f);
            int bottom = (int) (mascotCenterY + drawH / 2f);
            mascot.setBounds(left, top, right, bottom);
            mascot.draw(canvas);
        }

        // 4. Ambient particles in front of body
        drawAmbientParticles(canvas, false, w, h);

        // 5. Draw burst particles (hearts & stars upon tapping)
        drawBurstParticles(canvas);
    }

    private void drawAmbientParticles(Canvas canvas, boolean behind, int w, int h) {
        paint.setStyle(Paint.Style.FILL);
        for (Particle p : ambientParticles) {
            if (p.behind != behind) continue;
            float px = p.xNorm * w;
            float py = p.yNorm * h;
            paint.setColor(p.color);
            paint.setAlpha((int) (p.alpha * 255));
            canvas.drawCircle(px, py, p.radiusDp * density, paint);
        }
    }

    private void drawBurstParticles(Canvas canvas) {
        if (burstParticles.isEmpty()) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(16 * density);
        paint.setTextAlign(Paint.Align.CENTER);

        for (BurstParticle bp : burstParticles) {
            paint.setColor(bp.color);
            paint.setAlpha((int) (Math.max(0f, Math.min(1f, bp.life)) * 255));
            canvas.drawCircle(bp.x, bp.y, (3f + bp.life * 3.5f) * density, paint);
        }
    }
}
