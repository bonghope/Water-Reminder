package com.aqua.water.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.LinearInterpolator;

import com.aqua.water.R;

/** Projects Aqua's rounded model and animates its pose in the pet screen. */
public final class PetView extends View {
    private static final int DODGE = 0;
    private static final int LAUGH = 1;
    private static final int HOP = 2;
    private static final int INK = 0xff14264f;
    private static final float TAU = (float) (2 * Math.PI);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Matrix lightingMatrix = new Matrix();
    private final JellySphere sphere = new JellySphere();
    private final Shader[] water = {
            gloss(0xffe7fbff, 0xff8adfff, 0xff439ae2, 0xff215ba9),
            gloss(0xffe0fff9, 0xff77ebe9, 0xff259fc8, 0xff1b5ca6),
            gloss(0xffe4fcff, 0xff54e2ff, 0xff168fd6, 0xff284da2),
            gloss(0xffe4fbff, 0xff61dcff, 0xff217ace, 0xff383b93)
    };
    private final Shader glass = gloss(0xffe8ffff, 0xff94edff, 0xff37b7e4, 0xff376bc1);
    private final Shader gold = gloss(0xfffff9dd, 0xffffd769, 0xffee9a38, 0xffaa542e);
    private final Shader eyes = gloss(0xff506ca2, 0xff253e72, 0xff13244f, 0xff101b3b);
    private final Shader cheeks = new RadialGradient(0, 0, 1,
            0xcaff879f, 0x00ff91b5, Shader.TileMode.CLAMP);
    private final Shader shadow = new RadialGradient(0, 0, 1,
            0x55144577, 0x00144577, Shader.TileMode.CLAMP);
    private final float touchSlop;

    private ValueAnimator animation;
    private OnPetClickListener listener;
    private int level;
    private int reaction = HOP;
    private int nextReaction;
    private long reactionStarted = -1;
    private long idleStarted;
    private float direction = 1f;
    private float downX;
    private float downY;
    private boolean tapCandidate;
    private boolean rotating;
    private float lastX;
    private float turn;
    private float yawSin;
    private float yawCos = 1;

    public interface OnPetClickListener {
        void onPetClicked(int reaction);
    }

    public PetView(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClickable(true);
        setFocusable(true);
        setLevel(0);
    }

    public void setLevel(int value) {
        level = Math.max(0, Math.min(water.length - 1, value));
        setContentDescription(getResources().getString(R.string.pet_interaction, PetLevels.title(level)));
        invalidate();
    }

    public void setOnPetClickListener(OnPetClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        idleStarted = SystemClock.uptimeMillis();
        updateAnimation();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        updateAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        finishGesture();
        stopAnimation();
        super.onDetachedFromWindow();
    }

    private void updateAnimation() {
        if (!isAttachedToWindow() || getWindowVisibility() != VISIBLE
                || (Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled())) {
            stopAnimation();
            return;
        }
        if (animation == null) {
            animation = ValueAnimator.ofFloat(0f, 1f);
            animation.setDuration(1000);
            animation.setRepeatCount(ValueAnimator.INFINITE);
            animation.setInterpolator(new LinearInterpolator());
            animation.addUpdateListener(value -> invalidate());
            animation.start();
        }
    }

    private void stopAnimation() {
        if (animation != null) {
            animation.cancel();
            animation = null;
        }
        reactionStarted = -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = event.getX();
                downY = event.getY();
                tapCandidate = true;
                rotating = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(event.getX() - downX);
                float dy = Math.abs(event.getY() - downY);
                if (tapCandidate && Math.max(dx, dy) > touchSlop) {
                    tapCandidate = false;
                    rotating = dx > dy;
                    if (rotating && getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                if (rotating) {
                    float fullTurnDistance = Math.max(1f, Math.min(getWidth(), getHeight()) * 0.80f);
                    rotateBy((event.getX() - lastX) * TAU / fullTurnDistance);
                    lastX = event.getX();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (tapCandidate) performClick();
                finishGesture();
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_DOWN:
                finishGesture();
                return true;
            default:
                return true;
        }
    }

    private void finishGesture() {
        if (rotating && getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
        rotating = false;
        tapCandidate = false;
    }

    private void rotateBy(float angle) {
        turn = (turn + angle) % TAU;
        invalidate();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
    }

    @Override
    public boolean performAccessibilityAction(int action, android.os.Bundle arguments) {
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            rotateBy(action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ? TAU / 8 : -TAU / 8);
            return true;
        }
        return super.performAccessibilityAction(action, arguments);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (tapCandidate && Math.abs(downX - getWidth() / 2f) > touchSlop) {
            direction = downX < getWidth() / 2f ? 1f : -1f;
        } else {
            direction = -direction;
        }
        startReaction(nextReaction);
        nextReaction = (nextReaction + 1) % 3;
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        if (listener != null) listener.onPetClicked(reaction);
        return true;
    }

    public void celebratePreview() {
        startReaction(HOP);
    }

    private void startReaction(int value) {
        reaction = value;
        reactionStarted = SystemClock.uptimeMillis();
        updateAnimation();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float unit = Math.min(getWidth(), getHeight()) / 4.2f;
        if (unit <= 0) return;

        long now = SystemClock.uptimeMillis();
        float idle = animation == null ? 0f : (now - idleStarted) / 1000f;
        float t = reactionStarted < 0 ? 1f : Math.min(1f, (now - reactionStarted) / 840f);
        float lift = reaction == LAUGH ? 0.10f * Math.abs(sin(TAU * 1.5f * t)) * wave(t)
                : (reaction == DODGE ? 0.78f : 0.57f) * wave(t);
        float slide = reaction == DODGE ? direction * 0.63f * wave(t) : 0f;
        float squash = reaction == LAUGH ? 0.10f * wave(t) * sin(TAU * 4 * t)
                : 0.10f * wave(t) * sin(TAU * t);
        float yaw = turn + (rotating ? 0f : 0.20f * sin(idle * 1.35f) + slide * 0.9f);
        yawSin = sin(yaw);
        yawCos = (float) Math.cos(yaw);
        float size = 0.91f + 0.028f * level;

        canvas.save();
        canvas.translate(getWidth() / 2f, getHeight() * 0.55f);
        canvas.scale(unit, unit);
        shaded(canvas, slide * 0.64f, 1.18f, 1.15f - lift * 0.36f, 0.25f, shadow);
        drawBubbles(canvas, idle);
        canvas.save();
        canvas.translate(slide, -lift + 0.045f * sin(idle * 2.2f));
        canvas.rotate(slide * 21f + (reaction == LAUGH ? squash * 73f : 0f));
        canvas.scale(size * (1 + squash), size * (1 - squash));
        drawModel(canvas, idle, t);
        canvas.restore();
        drawBurst(canvas, t, slide, lift);
        canvas.restore();
    }

    private void drawModel(Canvas canvas, float idle, float t) {
        Shader body = water[level];
        float armLift = 0.07f * sin(idle * 2.2f) + 0.22f * wave(t);
        drawLimbs(canvas, body, armLift, false);
        if (Build.VERSION.SDK_INT >= 29 || !canvas.isHardwareAccelerated()) {
            sphere.draw(canvas, paint, level);
        } else {
            shaded(canvas, 0, 0, 1.03f, 1.00f, body);
        }
        drawLimbs(canvas, body, armLift, true);

        if (level == 3) {
            // Small crown echoes the final evolution without hiding the face.
            limb(canvas, 0.10f, -0.90f, 0.34f, 0.44f, 0.13f, 0, gold);
            part(canvas, -0.16f, -1.03f, 0.34f, 0.14f, 0.22f, gold);
            part(canvas, 0.10f, -1.12f, 0.34f, 0.16f, 0.29f, gold);
            part(canvas, 0.36f, -1.03f, 0.34f, 0.14f, 0.22f, gold);
            part(canvas, 0.10f, -0.94f, 0.49f, 0.07f, 0.085f, glass);
        } else {
            limb(canvas, 0.08f, -0.87f, 0.13f, 0.21f, 0.35f, 31f, body);
            if (level >= 1) {
                limb(canvas, -0.31f, -0.85f, 0.09f, 0.15f, 0.24f, -35f, body);
            }
            if (level == 2) {
                part(canvas, 0.44f, -1.17f + 0.07f * sin(idle * 2.5f), 0.02f, 0.12f, 0.13f, glass);
            }
        }

        boolean laughing = reaction == LAUGH && t > 0.08f && t < 0.88f;
        boolean startled = reaction == DODGE && t > 0.07f && t < 0.64f;
        float blink = animation != null && idle % 4.6f > 4.46f ? 0.09f : 1f;
        eye(canvas, -0.33f, 0.02f, laughing, blink);
        eye(canvas, 0.33f, 0.02f, laughing, blink);
        drawCheek(canvas, -0.57f);
        drawCheek(canvas, 0.57f);
        if (yawCos > 0f) drawMouth(canvas, laughing, startled);

        canvas.save();
        canvas.rotate(-26f, -0.48f, -0.48f);
        oval(canvas, -0.48f, -0.48f, 0.23f, 0.085f, 0xaaffffff);
        canvas.restore();
        oval(canvas, -0.69f, -0.29f, 0.056f, 0.041f, 0x90ffffff);
        if (level >= 2) {
            oval(canvas, 0.64f, 0.52f, 0.047f, 0.037f, 0x91c7ffff);
            oval(canvas, 0.73f, 0.42f, 0.025f, 0.021f, 0x91c7ffff);
        }
    }

    private void drawLimbs(Canvas canvas, Shader body, float armLift, boolean foreground) {
        // The near arm overlaps the sphere in profile. The far arm stays behind it.
        for (int side = -1; side <= 1; side += 2) {
            float armX = side * 0.91f;
            if ((projectDepth(armX, 0.1f) > 0.32f) == foreground) {
                limb(canvas, armX, 0.22f - armLift, 0.1f, 0.31f, 0.19f, -side * 31f, body);
            }
            if (!foreground) {
                limb(canvas, side * 0.47f, 0.94f, 0.39f, 0.31f, 0.20f, side * 13f, body);
            }
        }
    }

    private void drawCheek(Canvas canvas, float x) {
        float facing = projectDepth(x, 0.78f) / 0.78f;
        if (facing > 0f) {
            part(canvas, x, 0.30f, 0.78f, 0.22f * Math.min(1f, facing), 0.12f, cheeks);
        }
    }

    private void eye(Canvas canvas, float x, float y, boolean laughing, float blink) {
        float facing = projectDepth(x, 0.88f) / 0.88f;
        if (facing <= 0f) return;
        float center = projectX(x, 0.88f);
        float perspective = Math.min(1f, facing);
        float width = 0.145f * perspective;
        if (laughing) {
            paint.setColor(INK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(0.047f);
            canvas.drawArc(center - width, y - 0.06f, center + width, y + 0.13f,
                    207, 126, false, paint);
            paint.setStyle(Paint.Style.FILL);
            return;
        }
        shaded(canvas, center, y, width, 0.204f * blink, eyes);
        if (blink > 0.2f) {
            oval(canvas, center - 0.044f * perspective, y - 0.068f,
                    0.047f * perspective, 0.053f, Color.WHITE);
            oval(canvas, center + 0.048f * perspective, y + 0.058f,
                    0.023f * perspective, 0.025f, 0xffb8eaff);
        }
    }

    private void drawMouth(Canvas canvas, boolean laughing, boolean startled) {
        canvas.save();
        canvas.translate(projectX(0, 0.95f), 0.25f);
        canvas.scale(yawCos, 1f);
        if (startled) {
            oval(canvas, 0, 0.07f, 0.083f, 0.111f, INK);
        } else {
            float depth = laughing ? 0.38f : 0.25f;
            path.reset();
            path.moveTo(-0.19f, 0);
            path.cubicTo(-0.10f, 0.045f, 0.10f, 0.045f, 0.19f, 0);
            path.cubicTo(0.21f, depth, -0.21f, depth, -0.19f, 0);
            path.close();
            paint.setColor(INK);
            canvas.drawPath(path, paint);
            oval(canvas, 0.023f, depth * 0.53f, 0.079f, depth * 0.16f, 0xffff94a7);
        }
        canvas.restore();
    }

    private void drawBubbles(Canvas canvas, float idle) {
        for (int i = 0; i < 5 + level; i++) {
            float side = i % 2 == 0 ? -1f : 1f;
            float x = side * (1.28f + 0.17f * (i % 3)) + 0.06f * sin(idle + i);
            float y = 0.91f - ((idle * 0.24f + i * 0.47f) % 2.65f);
            float radius = 0.040f + 0.022f * (i % 3);
            shaded(canvas, x, y, radius, radius, level == 3 && i % 3 == 0 ? gold : glass);
        }
    }

    private void drawBurst(Canvas canvas, float t, float slide, float lift) {
        if (t >= 1f) return;
        int alpha = (int) (235 * (1 - t));
        for (int i = 0; i < 7; i++) {
            float angle = (i + 0.4f) * TAU / 7;
            float radius = 0.58f + t * 1.02f;
            float x = slide * 0.5f + (float) Math.cos(angle) * radius;
            float y = -0.27f - lift * 0.24f + sin(angle) * radius - 0.23f * t;
            float size = 0.07f + (i % 3) * 0.016f;
            int color = alpha << 24 | (i % 2 == 0 ? 0x00ff91ad : 0x006bcff6);
            if (i % 2 == 0) {
                canvas.save();
                canvas.translate(x, y);
                canvas.scale(size, size);
                path.reset();
                path.moveTo(0, 0.95f);
                path.cubicTo(-2.1f, -0.45f, -0.60f, -1.5f, 0, -0.48f);
                path.cubicTo(0.60f, -1.5f, 2.1f, -0.45f, 0, 0.95f);
                paint.setColor(color);
                canvas.drawPath(path, paint);
                canvas.restore();
            } else {
                oval(canvas, x, y, size * 0.64f, size * 0.64f, color);
            }
        }
    }

    private float projectX(float x, float z) {
        return x * yawCos + z * yawSin;
    }

    private float projectDepth(float x, float z) {
        return z * yawCos - x * yawSin;
    }

    private void part(Canvas canvas, float x, float y, float z, float rx, float ry, Shader shader) {
        shaded(canvas, projectX(x, z), y, rx, ry, shader);
    }

    private void limb(Canvas canvas, float x, float y, float z, float rx, float ry, float tilt, Shader shader) {
        canvas.save();
        canvas.translate(projectX(x, z), y);
        canvas.rotate(tilt);
        shaded(canvas, 0, 0, rx, ry, shader);
        canvas.restore();
    }

    private void shaded(Canvas canvas, float x, float y, float rx, float ry, Shader shader) {
        lightingMatrix.setScale(rx, ry);
        lightingMatrix.postTranslate(x, y);
        shader.setLocalMatrix(lightingMatrix);
        paint.setColor(Color.WHITE);
        paint.setShader(shader);
        canvas.drawOval(x - rx, y - ry, x + rx, y + ry, paint);
        paint.setShader(null);
    }

    private void oval(Canvas canvas, float x, float y, float rx, float ry, int color) {
        paint.setColor(color);
        canvas.drawOval(x - rx, y - ry, x + rx, y + ry, paint);
    }

    private static Shader gloss(int highlight, int light, int mid, int rim) {
        return new RadialGradient(-0.38f, -0.46f, 1.52f, new int[]{highlight, light, mid, rim},
                new float[]{0f, 0.28f, 0.72f, 1f}, Shader.TileMode.CLAMP);
    }

    private static float wave(float t) {
        return t <= 0f || t >= 1f ? 0f : sin((float) Math.PI * t);
    }

    private static float sin(float radians) {
        return (float) Math.sin(radians);
    }

    /** Lit, camera-facing hemisphere of the spherical body. The face is attached at model depth. */
    private static final class JellySphere {
        private static final int RINGS = 14;
        private static final int SLICES = 56;
        private static final int[][] PALETTE = {
                {0xff215ba9, 0xff49a9ef, 0xffa9edff},
                {0xff1b5ca6, 0xff30bfcf, 0xffa0f9f0},
                {0xff284da2, 0xff21acec, 0xff8dedff},
                {0xff383b93, 0xff268ce5, 0xff91e9ff}
        };
        private final float[] vertices = new float[2 * (1 + RINGS * SLICES)];
        private final int[][] colors = new int[PALETTE.length][vertices.length / 2];
        private final short[] triangles = new short[3 * SLICES * (2 * RINGS - 1)];

        JellySphere() {
            setVertex(0, 0, 0, 1);
            int index = 0;
            for (int ring = 1; ring <= RINGS; ring++) {
                float angle = (float) (Math.PI / 2 * ring / RINGS);
                float radius = sin(angle);
                float z = (float) Math.cos(angle);
                for (int slice = 0; slice < SLICES; slice++) {
                    float azimuth = TAU * slice / SLICES;
                    short current = vertex(ring, slice);
                    short next = vertex(ring, (slice + 1) % SLICES);
                    setVertex(current, radius * (float) Math.cos(azimuth), radius * sin(azimuth), z);
                    if (ring == 1) {
                        triangles[index++] = 0;
                    } else {
                        short inner = vertex(ring - 1, slice);
                        short innerNext = vertex(ring - 1, (slice + 1) % SLICES);
                        triangles[index++] = inner;
                        triangles[index++] = next;
                        triangles[index++] = innerNext;
                        triangles[index++] = inner;
                    }
                    triangles[index++] = current;
                    triangles[index++] = next;
                }
            }
        }

        private static short vertex(int ring, int slice) {
            return (short) (1 + (ring - 1) * SLICES + slice);
        }

        private void setVertex(int index, float x, float y, float z) {
            vertices[2 * index] = 1.03f * x;
            vertices[2 * index + 1] = y;
            float diffuse = clamp(-0.42f * x - 0.50f * y + 0.75f * z);
            float reflection = (float) Math.pow(clamp(-0.21f * x - 0.28f * y + 0.936f * z), 26);
            float edgeLight = 0.32f * (1 - z) * clamp(x + y);
            for (int level = 0; level < colors.length; level++) {
                int[] palette = PALETTE[level];
                int base = blend(palette[0], palette[1], diffuse);
                int lit = blend(base, palette[2], 0.39f * diffuse * diffuse + edgeLight);
                colors[level][index] = blend(lit, Color.WHITE, 0.38f * reflection);
            }
        }

        void draw(Canvas canvas, Paint paint, int level) {
            paint.setColor(Color.WHITE);
            canvas.drawVertices(Canvas.VertexMode.TRIANGLES, vertices.length, vertices, 0,
                    null, 0, colors[level], 0, triangles, 0, triangles.length, paint);
        }

        private static float clamp(float value) {
            return Math.min(1f, Math.max(0f, value));
        }

        private static int blend(int a, int b, float t) {
            return Color.rgb(
                    Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                    Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                    Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
        }
    }
}
