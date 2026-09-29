package com.fayazali.bubbleshooter;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.Random;

/**
 * The launcher at the bottom of the screen. Holds the current and next
 * bubble, draws the reflecting aim line, and fires bubbles.
 */
public class Shooter {

    public float x, y;
    public float angle;
    public Bubble currentBubble;
    public Bubble nextBubble;
    public int bombs = 0;

    private final float bubbleRadius;
    private final int screenWidth;
    private final Random random = new Random();

    private Paint aimPaint, basePaint, swapBtnPaint, swapTextPaint, bombCountPaint;

    public float swapBtnX, swapBtnY, swapBtnRadius;

    /** Y coordinate of the top wall — the aim line and bubbles bounce below it. */
    private static final float TOP_WALL = 130f;

    public Shooter(float x, float y, float bubbleRadius, int screenWidth) {
        this.x = x;
        this.y = y;
        this.bubbleRadius = bubbleRadius;
        this.screenWidth = screenWidth;
        this.angle = (float) (-Math.PI / 2); // straight up by default

        swapBtnX = x + bubbleRadius * 3.5f;
        swapBtnY = y;
        swapBtnRadius = bubbleRadius * 1.2f;

        setupPaints();
        nextBubble = generateBubble(x - bubbleRadius * 3.5f, y);
        loadNext();
    }

    private void setupPaints() {
        aimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        aimPaint.setColor(Color.argb(160, 255, 255, 255));
        aimPaint.setStrokeWidth(3);
        aimPaint.setStyle(Paint.Style.STROKE);
        aimPaint.setPathEffect(new DashPathEffect(new float[]{18, 10}, 0));

        basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        basePaint.setColor(Color.parseColor("#37474F"));

        swapBtnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        swapBtnPaint.setColor(Color.parseColor("#455A64"));

        swapTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        swapTextPaint.setColor(Color.WHITE);
        swapTextPaint.setTextSize(bubbleRadius * 0.6f);
        swapTextPaint.setTextAlign(Paint.Align.CENTER);
        swapTextPaint.setFakeBoldText(true);

        bombCountPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bombCountPaint.setColor(Color.parseColor("#FFD700"));
        bombCountPaint.setTextSize(bubbleRadius * 0.75f);
        bombCountPaint.setTextAlign(Paint.Align.CENTER);
        bombCountPaint.setFakeBoldText(true);
    }

    private Bubble generateBubble(float bx, float by) {
        return new Bubble(bx, by, bubbleRadius, random.nextInt(Bubble.COLORS.length));
    }

    /** Promotes the next bubble to the barrel and generates a new next bubble. */
    public void loadNext() {
        currentBubble = new Bubble(x, y - bubbleRadius * 1.8f,
                bubbleRadius, nextBubble.colorIndex, nextBubble.type);
        nextBubble = generateBubble(x - bubbleRadius * 3.5f, y);
    }

    /** Swaps the colours of the current and next bubble. */
    public void swapBubbles() {
        int tempColor = currentBubble.colorIndex;
        int tempType = currentBubble.type;
        currentBubble.colorIndex = nextBubble.colorIndex;
        currentBubble.type = nextBubble.type;
        nextBubble.colorIndex = tempColor;
        nextBubble.type = tempType;
    }

    /** Turns the loaded bubble into a bomb, if the player has one left. */
    public boolean useBomb() {
        if (bombs > 0) {
            bombs--;
            currentBubble = new Bubble(x, y - bubbleRadius * 1.8f,
                    bubbleRadius, 0, Bubble.TYPE_BOMB);
            return true;
        }
        return false;
    }

    public void updateAim(float touchX, float touchY) {
        if (touchY >= y) return; // never aim downward
        angle = (float) Math.atan2(touchY - y, touchX - x);
    }

    public Bubble shoot() {
        if (currentBubble == null) return null;
        float speed = 38; // higher = faster shot
        currentBubble.vx = (float) (Math.cos(angle) * speed);
        currentBubble.vy = (float) (Math.sin(angle) * speed);
        currentBubble.isMoving = true;
        Bubble shot = currentBubble;
        loadNext();
        return shot;
    }

    public void draw(Canvas canvas) {
        // launcher base
        Path triangle = new Path();
        triangle.moveTo(x, y - bubbleRadius * 1.5f);
        triangle.lineTo(x - bubbleRadius, y + bubbleRadius * 0.5f);
        triangle.lineTo(x + bubbleRadius, y + bubbleRadius * 0.5f);
        triangle.close();
        canvas.drawPath(triangle, basePaint);

        drawReflectingAimLine(canvas);

        if (currentBubble != null) {
            currentBubble.x = x;
            currentBubble.y = y - bubbleRadius * 1.8f;
            currentBubble.draw(canvas);
        }

        if (nextBubble != null) {
            nextBubble.x = x - bubbleRadius * 3.5f;
            nextBubble.y = y;
            nextBubble.draw(canvas);
        }

        Paint nextLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        nextLabel.setColor(Color.parseColor("#AAAAAA"));
        nextLabel.setTextSize(bubbleRadius * 0.6f);
        nextLabel.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("NEXT", x - bubbleRadius * 3.5f, y + bubbleRadius * 1.9f, nextLabel);

        // swap button
        swapBtnX = x + bubbleRadius * 3.5f;
        swapBtnY = y;
        canvas.drawCircle(swapBtnX, swapBtnY, swapBtnRadius, swapBtnPaint);
        canvas.drawText("SWAP", swapBtnX, swapBtnY + swapTextPaint.getTextSize() * 0.35f, swapTextPaint);

        if (bombs > 0) {
            canvas.drawText("B x" + bombs, swapBtnX, y + bubbleRadius * 2.3f, bombCountPaint);
        }
    }

    /**
     * Draws a dashed aim line that bounces off the left and right walls,
     * so the player can plan bank shots.
     */
    private void drawReflectingAimLine(Canvas canvas) {
        float curX = x;
        float curY = y - bubbleRadius * 2;
        float dirX = (float) Math.cos(angle);
        float dirY = (float) Math.sin(angle);

        if (dirY >= 0) return; // only draw when aiming upward

        float remaining = 700f;
        Path path = new Path();
        path.moveTo(curX, curY);

        for (int bounce = 0; bounce < 4 && remaining > 0; bounce++) {
            // distance to each wall along the current direction
            float tLeft = dirX < 0 ? (bubbleRadius - curX) / dirX : Float.MAX_VALUE;
            float tRight = dirX > 0 ? (screenWidth - bubbleRadius - curX) / dirX : Float.MAX_VALUE;
            float tTop = dirY < 0 ? (TOP_WALL + bubbleRadius - curY) / dirY : Float.MAX_VALUE;

            float tWall = Math.min(tLeft, tRight);
            float tHit = Math.min(tWall, tTop);
            float tMove = Math.min(tHit, remaining);

            float nextX = curX + dirX * tMove;
            float nextY = curY + dirY * tMove;
            path.lineTo(nextX, nextY);

            remaining -= tMove;
            curX = nextX;
            curY = nextY;

            // stop at the ceiling, otherwise flip horizontally and keep going
            if (tTop <= tWall) break;
            dirX = -dirX;
        }

        canvas.drawPath(path, aimPaint);
    }

    public boolean isSwapButtonTapped(float tx, float ty) {
        float dx = tx - swapBtnX;
        float dy = ty - swapBtnY;
        return Math.sqrt(dx * dx + dy * dy) < swapBtnRadius * 1.4f;
    }
}
