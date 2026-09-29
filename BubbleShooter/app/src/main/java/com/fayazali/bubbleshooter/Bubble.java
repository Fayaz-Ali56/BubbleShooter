package com.fayazali.bubbleshooter;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * A single bubble. Handles its own drawing, movement and fall animation.
 */
public class Bubble {

    // The six bubble colours used in the game
    public static final int[] COLORS = {
            Color.parseColor("#F44336"), // red
            Color.parseColor("#2196F3"), // blue
            Color.parseColor("#4CAF50"), // green
            Color.parseColor("#FFD700"), // yellow
            Color.parseColor("#9C27B0"), // purple
            Color.parseColor("#FF9800"), // orange
    };

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_BOMB = 1;

    public float x, y;          // centre position on screen
    public float radius;
    public int colorIndex;      // index into COLORS
    public int type;            // normal or bomb
    public boolean isMoving;    // true while this bubble is being shot
    public float vx, vy;        // velocity while moving
    public boolean isAlive;

    // Fields used only by the falling / pop animation
    public boolean isFalling = false;
    public float fallVy = 0;
    public float fallVx = 0;
    public float alpha = 255;

    private final Paint paint;
    private final Paint highlightPaint;
    private final Paint borderPaint;

    public Bubble(float x, float y, float radius, int colorIndex) {
        this(x, y, radius, colorIndex, TYPE_NORMAL);
    }

    public Bubble(float x, float y, float radius, int colorIndex, int type) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.colorIndex = colorIndex;
        this.type = type;
        this.isAlive = true;
        this.isMoving = false;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(Color.argb(80, 255, 255, 255));

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.argb(60, 0, 0, 0));
        borderPaint.setStrokeWidth(2);
    }

    public void draw(Canvas canvas) {
        if (!isAlive) return;

        // alpha is reduced during the fall animation to fade the bubble out
        int a = (int) Math.max(0, Math.min(255, alpha));

        if (type == TYPE_BOMB) {
            drawBomb(canvas, a);
            return;
        }

        int base = COLORS[colorIndex % COLORS.length];
        paint.setColor(Color.argb(a, Color.red(base), Color.green(base), Color.blue(base)));
        canvas.drawCircle(x, y, radius, paint);

        // small white shine in the top-left of the bubble
        highlightPaint.setColor(Color.argb(Math.min(a, 80), 255, 255, 255));
        canvas.drawCircle(x - radius * 0.3f, y - radius * 0.3f, radius * 0.35f, highlightPaint);

        borderPaint.setColor(Color.argb(Math.min(a, 60), 0, 0, 0));
        canvas.drawCircle(x, y, radius - 1, borderPaint);
    }

    private void drawBomb(Canvas canvas, int a) {
        paint.setColor(Color.argb(a, 33, 33, 33));
        canvas.drawCircle(x, y, radius, paint);

        Paint fuse = new Paint(Paint.ANTI_ALIAS_FLAG);
        fuse.setColor(Color.argb(a, 255, 152, 0));
        fuse.setStrokeWidth(4);
        fuse.setStyle(Paint.Style.STROKE);
        canvas.drawLine(x, y - radius, x + radius * 0.5f, y - radius * 1.4f, fuse);

        Paint star = new Paint(Paint.ANTI_ALIAS_FLAG);
        star.setColor(Color.argb(a, 255, 215, 0));
        canvas.drawCircle(x + radius * 0.25f, y - radius * 0.25f, radius * 0.2f, star);

        borderPaint.setColor(Color.argb(Math.min(a, 60), 0, 0, 0));
        canvas.drawCircle(x, y, radius - 1, borderPaint);

        Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
        label.setColor(Color.argb(a, 255, 255, 255));
        label.setTextSize(radius * 0.75f);
        label.setTextAlign(Paint.Align.CENTER);
        label.setFakeBoldText(true);
        canvas.drawText("B", x, y + radius * 0.3f, label);
    }

    /** Moves the bubble one step while it is flying through the air. */
    public void update() {
        if (isMoving) {
            x += vx;
            y += vy;
        }
    }

    /** Moves and fades the bubble while it is falling after being popped. */
    public void updateFall() {
        if (isFalling) {
            fallVy += 1.2f;   // gravity
            x += fallVx;
            y += fallVy;
            alpha -= 8;       // fade out
        }
    }

    public boolean isFallDone() {
        return isFalling && alpha <= 0;
    }

    public int getColor() {
        return COLORS[colorIndex % COLORS.length];
    }
}
