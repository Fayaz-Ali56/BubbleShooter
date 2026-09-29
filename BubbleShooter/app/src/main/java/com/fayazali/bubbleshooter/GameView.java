package com.fayazali.bubbleshooter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

/**
 * The heart of the game. Draws every screen, handles touch input, and owns
 * the grid, the shooter and the list of bubbles currently in the air.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    // ---- screens -------------------------------------------------------
    private static final int STATE_START = 0;
    private static final int STATE_LEVEL_SELECT = 1;
    private static final int STATE_PLAYING = 2;
    private static final int STATE_LEVEL_WIN = 3;
    private static final int STATE_GAMEOVER = 4;
    private static final int STATE_HOW_TO_PLAY = 5;

    private static final int MAX_LEVEL = 200;
    private static final int MAX_LIVES = 3;
    private static final float TOP_WALL = 130f;

    private int state = STATE_START;

    private GameThread gameThread;
    private GridManager gridManager;
    private Shooter shooter;
    private final ScoreManager scoreManager;

    private final List<Bubble> movingBubbles = new ArrayList<>();
    private final List<Bubble> fallingBubbles = new ArrayList<>();

    private int score = 0;
    private int level = 1;
    private int lives = MAX_LIVES;
    private int shotCount = 0;
    private int shotsUntilNewRow = 10;

    private float bubbleRadius;
    private int screenWidth, screenHeight;

    // level select scrolling
    private float levelSelectScrollY = 0;
    private float lastTouchY = 0;

    private Paint bgPaint, textPaint, titlePaint, namePaint;
    private Paint btnPaint, btnTextPaint, overlayPaint, scorePaint;
    private Paint dangerLinePaint, hudBgPaint;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
        scoreManager = new ScoreManager(context);
        setupPaints();
    }

    private void setupPaints() {
        bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#1A1A2E"));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(50);

        titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#FF6B9D"));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTextSize(100);
        titlePaint.setFakeBoldText(true);

        namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(Color.parseColor("#FFD700"));
        namePaint.setTextAlign(Paint.Align.CENTER);
        namePaint.setTextSize(45);
        namePaint.setFakeBoldText(true);

        scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(44);

        btnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        btnTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnTextPaint.setColor(Color.WHITE);
        btnTextPaint.setTextAlign(Paint.Align.CENTER);
        btnTextPaint.setTextSize(52);
        btnTextPaint.setFakeBoldText(true);

        overlayPaint = new Paint();
        overlayPaint.setColor(Color.parseColor("#CC000000"));

        dangerLinePaint = new Paint();
        dangerLinePaint.setColor(Color.parseColor("#55FF0000"));
        dangerLinePaint.setStrokeWidth(3);

        hudBgPaint = new Paint();
        hudBgPaint.setColor(Color.parseColor("#AA000000"));
    }

    // ---- game setup ----------------------------------------------------

    private void initGame() {
        bubbleRadius = screenWidth / (GridManager.COLS * 2.2f);
        float gridStartX = screenWidth / 2f - (GridManager.COLS / 2f) * bubbleRadius * 2.1f;

        gridManager = new GridManager(gridStartX, TOP_WALL, bubbleRadius);
        gridManager.initForLevel(level);

        shooter = new Shooter(screenWidth / 2f, screenHeight - 150, bubbleRadius, screenWidth);

        // two free bombs on every tenth level
        if (level % 10 == 0) shooter.bombs += 2;

        movingBubbles.clear();
        fallingBubbles.clear();
        shotCount = 0;
        shotsUntilNewRow = Math.max(5, 12 - level / 10);
    }

    // ---- update --------------------------------------------------------

    public void update() {
        // falling bubbles keep animating on every screen
        List<Bubble> doneFalling = new ArrayList<>();
        for (Bubble b : fallingBubbles) {
            b.updateFall();
            if (b.isFallDone()) doneFalling.add(b);
        }
        fallingBubbles.removeAll(doneFalling);

        if (state != STATE_PLAYING || gridManager == null) return;

        List<Bubble> toRemove = new ArrayList<>();
        for (Bubble b : movingBubbles) {
            b.update();

            // bounce off the side walls
            if (b.x - b.radius <= 0) {
                b.x = b.radius;
                b.vx = -b.vx;
            } else if (b.x + b.radius >= screenWidth) {
                b.x = screenWidth - b.radius;
                b.vx = -b.vx;
            }

            // reached the ceiling
            if (b.y - b.radius <= TOP_WALL) {
                b.y = TOP_WALL + b.radius;
                snapBubble(b);
                toRemove.add(b);
                continue;
            }

            // hit a bubble already in the grid
            boolean collided = false;
            for (Bubble g : gridManager.getAllBubbles()) {
                float dx = b.x - g.x;
                float dy = b.y - g.y;
                if ((float) Math.sqrt(dx * dx + dy * dy) < b.radius + g.radius - 2) {
                    snapBubble(b);
                    toRemove.add(b);
                    collided = true;
                    break;
                }
            }
            if (collided) continue;

            if (b.y > screenHeight) toRemove.add(b);
        }
        movingBubbles.removeAll(toRemove);

        // level cleared
        if (gridManager.isGridClear()) {
            scoreManager.unlockLevel(level + 1);
            scoreManager.saveCurrentLevel(level);
            state = STATE_LEVEL_WIN;
            return;
        }

        // bubbles crossed the danger line
        if (gridManager.isBubbleAtBottom(screenHeight - 260)) {
            lives--;
            if (lives <= 0) {
                scoreManager.saveScore(score, level);
                state = STATE_GAMEOVER;
            } else {
                initGame(); // same level, one life fewer
            }
        }
    }

    private void snapBubble(Bubble b) {
        int[] cell = gridManager.snapToGrid(b);
        if (cell == null) return;

        int row = cell[0], col = cell[1];
        b.x = gridManager.getBubbleX(row, col);
        b.y = gridManager.getBubbleY(row);
        b.isMoving = false;
        gridManager.grid[row][col] = b;

        if (b.type == Bubble.TYPE_BOMB) {
            for (int[] t : gridManager.findBombTargets(row, col)) {
                startFalling(gridManager.grid[t[0]][t[1]]);
                gridManager.grid[t[0]][t[1]] = null;
                score += 15;
            }
            gridManager.grid[row][col] = null;
        } else {
            List<int[]> matches = gridManager.findMatches(row, col);
            if (matches.size() >= 3) {
                for (int[] m : matches) {
                    startFalling(gridManager.grid[m[0]][m[1]]);
                    gridManager.grid[m[0]][m[1]] = null;
                    score += 10;
                }
                score += matches.size() * 5;

                for (int[] f : gridManager.findFloating()) {
                    startFalling(gridManager.grid[f[0]][f[1]]);
                    gridManager.grid[f[0]][f[1]] = null;
                    score += 15;
                }
            }
        }

        shotCount++;
        if (shotCount % shotsUntilNewRow == 0) gridManager.addRow();
    }

    /** Hands a popped bubble over to the falling animation list. */
    private void startFalling(Bubble b) {
        if (b == null) return;
        b.isFalling = true;
        b.fallVy = -4f + (float) (Math.random() * 3);
        b.fallVx = -3f + (float) (Math.random() * 6);
        b.alpha = 255;
        fallingBubbles.add(b);
    }

    // ---- drawing -------------------------------------------------------

    @Override
    public void draw(Canvas canvas) {
        if (canvas == null) return;
        canvas.drawRect(0, 0, screenWidth, screenHeight, bgPaint);

        for (Bubble b : fallingBubbles) b.draw(canvas);

        switch (state) {
            case STATE_START:
                drawStartScreen(canvas);
                break;
            case STATE_LEVEL_SELECT:
                drawLevelSelect(canvas);
                break;
            case STATE_HOW_TO_PLAY:
                drawHowToPlay(canvas);
                break;
            case STATE_PLAYING:
                drawGameScreen(canvas);
                break;
            case STATE_LEVEL_WIN:
                drawGameScreen(canvas);
                drawLevelWin(canvas);
                break;
            case STATE_GAMEOVER:
                drawGameScreen(canvas);
                drawGameOver(canvas);
                break;
        }
    }

    private void drawStartScreen(Canvas canvas) {
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;

        // faint decorative bubbles behind the menu
        Paint deco = new Paint(Paint.ANTI_ALIAS_FLAG);
        int[] decoColors = {0x22F44336, 0x222196F3, 0x224CAF50, 0x22FFD700, 0x229C27B0};
        float[] dx = {60, 280, 150, 320, 80, 300, 200};
        float[] dy = {200, 160, 400, 340, 600, 540, 750};
        float[] dr = {55, 45, 65, 40, 50, 60, 35};
        for (int i = 0; i < dx.length; i++) {
            deco.setColor(decoColors[i % decoColors.length]);
            canvas.drawCircle(dx[i], dy[i], dr[i], deco);
        }

        titlePaint.setTextSize(105);
        titlePaint.setColor(Color.parseColor("#FF6B9D"));
        canvas.drawText("BUBBLE", cx, cy - 210, titlePaint);
        canvas.drawText("SHOOTER", cx, cy - 90, titlePaint);

        Paint line = new Paint();
        line.setColor(Color.parseColor("#FFD700"));
        line.setStrokeWidth(2);
        canvas.drawLine(cx - 200, cy - 50, cx + 200, cy - 50, line);

        namePaint.setTextSize(45);
        canvas.drawText("Created by Fayaz Ali", cx, cy + 10, namePaint);
        canvas.drawLine(cx - 200, cy + 45, cx + 200, cy + 45, line);

        btnTextPaint.setTextSize(55);

        btnPaint.setColor(Color.parseColor("#FF6B9D"));
        canvas.drawRoundRect(new RectF(cx - 180, cy + 65, cx + 180, cy + 155), 50, 50, btnPaint);
        canvas.drawText("PLAY", cx, cy + 128, btnTextPaint);

        btnPaint.setColor(Color.parseColor("#2196F3"));
        canvas.drawRoundRect(new RectF(cx - 180, cy + 175, cx + 180, cy + 255), 50, 50, btnPaint);
        canvas.drawText("LEVELS", cx, cy + 238, btnTextPaint);

        btnPaint.setColor(Color.parseColor("#4CAF50"));
        canvas.drawRoundRect(new RectF(cx - 180, cy + 275, cx + 180, cy + 355), 50, 50, btnPaint);
        canvas.drawText("HOW TO PLAY", cx, cy + 338, btnTextPaint);

        textPaint.setTextSize(36);
        textPaint.setColor(Color.parseColor("#FFD700"));
        canvas.drawText("Best Score: " + scoreManager.getHighScore(), cx, cy + 400, textPaint);

        textPaint.setColor(Color.parseColor("#4CAF50"));
        canvas.drawText("Reached Level: " + scoreManager.getHighestUnlockedLevel(),
                cx, cy + 450, textPaint);

        textPaint.setTextSize(24);
        textPaint.setColor(Color.parseColor("#555555"));
        canvas.drawText("(c) Fayaz Ali  |  All Rights Reserved", cx, screenHeight - 30, textPaint);
        textPaint.setColor(Color.WHITE);
    }

    private void drawLevelSelect(Canvas canvas) {
        int cx = screenWidth / 2;
        int unlocked = scoreManager.getHighestUnlockedLevel();

        canvas.drawRect(0, 0, screenWidth, 110, hudBgPaint);
        titlePaint.setTextSize(60);
        titlePaint.setColor(Color.parseColor("#FF6B9D"));
        canvas.drawText("SELECT LEVEL", cx, 78, titlePaint);

        Paint back = new Paint(Paint.ANTI_ALIAS_FLAG);
        back.setColor(Color.parseColor("#37474F"));
        canvas.drawRoundRect(new RectF(20, 20, 120, 90), 20, 20, back);
        textPaint.setTextSize(36);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("BACK", 70, 64, textPaint);

        int cols = 5;
        float cellSize = (screenWidth - 40f) / cols;
        float startY = 130 + levelSelectScrollY;

        for (int i = 0; i < MAX_LEVEL; i++) {
            int row = i / cols;
            int col = i % cols;
            float lx = 20 + col * cellSize + cellSize / 2f;
            float ly = startY + row * cellSize + cellSize / 2f;

            // skip anything scrolled off screen
            if (ly + cellSize < 110 || ly - cellSize > screenHeight) continue;

            boolean isUnlocked = (i + 1) <= unlocked;
            RectF cell = new RectF(lx - cellSize * 0.42f, ly - cellSize * 0.42f,
                    lx + cellSize * 0.42f, ly + cellSize * 0.42f);

            if ((i + 1) == unlocked) {
                btnPaint.setColor(Color.parseColor("#FF6B9D")); // next level to play
            } else if (isUnlocked) {
                btnPaint.setColor(Color.parseColor("#2196F3")); // already completed
            } else {
                btnPaint.setColor(Color.parseColor("#37474F")); // still locked
            }
            canvas.drawRoundRect(cell, 16, 16, btnPaint);

            Paint lvl = new Paint(Paint.ANTI_ALIAS_FLAG);
            lvl.setTextAlign(Paint.Align.CENTER);
            lvl.setFakeBoldText(true);

            if (isUnlocked) {
                lvl.setColor(Color.WHITE);
                lvl.setTextSize(cellSize * 0.35f);
                canvas.drawText(String.valueOf(i + 1), lx, ly + cellSize * 0.13f, lvl);

                // tick on levels already beaten
                if ((i + 1) < unlocked) {
                    Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
                    tick.setColor(Color.parseColor("#4CAF50"));
                    tick.setTextSize(cellSize * 0.26f);
                    tick.setTextAlign(Paint.Align.RIGHT);
                    tick.setFakeBoldText(true);
                    canvas.drawText("OK", lx + cellSize * 0.36f, ly - cellSize * 0.20f, tick);
                }
            } else {
                drawPadlock(canvas, lx, ly - cellSize * 0.05f, cellSize * 0.26f);
                lvl.setColor(Color.parseColor("#666666"));
                lvl.setTextSize(cellSize * 0.22f);
                canvas.drawText(String.valueOf(i + 1), lx, ly + cellSize * 0.32f, lvl);
            }
        }
    }

    /** Small padlock drawn with shapes, used for locked levels. */
    private void drawPadlock(Canvas canvas, float cx, float cy, float size) {
        Paint lock = new Paint(Paint.ANTI_ALIAS_FLAG);
        lock.setColor(Color.parseColor("#9E9E9E"));

        // shackle
        lock.setStyle(Paint.Style.STROKE);
        lock.setStrokeWidth(size * 0.18f);
        RectF arc = new RectF(cx - size * 0.32f, cy - size * 0.75f,
                cx + size * 0.32f, cy - size * 0.10f);
        canvas.drawArc(arc, 180, 180, false, lock);

        // body
        lock.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(cx - size * 0.45f, cy - size * 0.20f,
                cx + size * 0.45f, cy + size * 0.50f), size * 0.12f, size * 0.12f, lock);
    }

    private void drawHowToPlay(Canvas canvas) {
        int cx = screenWidth / 2;

        canvas.drawRect(0, 0, screenWidth, 110, hudBgPaint);
        titlePaint.setTextSize(58);
        titlePaint.setColor(Color.parseColor("#FF6B9D"));
        canvas.drawText("HOW TO PLAY", cx, 76, titlePaint);

        Paint back = new Paint(Paint.ANTI_ALIAS_FLAG);
        back.setColor(Color.parseColor("#37474F"));
        canvas.drawRoundRect(new RectF(20, 20, 120, 90), 20, 20, back);
        textPaint.setTextSize(36);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("BACK", 70, 64, textPaint);

        String[] titles = {
                "AIM AND SHOOT",
                "MATCH 3 BUBBLES",
                "SWAP BUBBLES",
                "USE BOMBS",
                "BANK YOUR SHOTS",
                "3 LIVES PER LEVEL",
                "LEVEL PROGRESSION"
        };
        String[] descs = {
                "Drag your finger to aim, then lift it to fire the bubble.",
                "Match 3 or more bubbles of the same colour to pop them.",
                "Tap the SWAP button to switch the current and next bubble.",
                "Bombs destroy every bubble in a wide area around them.",
                "The aim line bounces off walls so you can reach tight spots.",
                "You get 3 lives on each level. Lose all 3 and it is game over.",
                "Beat a level to unlock the next. Replay any level you finished."
        };
        int[] colors = {
                0xFF2196F3, 0xFFFF6B9D, 0xFF4CAF50,
                0xFFFF9800, 0xFF9C27B0, 0xFFF44336, 0xFFFFD700
        };

        float y = 140;
        for (int i = 0; i < titles.length; i++) {
            Paint card = new Paint(Paint.ANTI_ALIAS_FLAG);
            card.setColor(Color.parseColor("#252540"));
            canvas.drawRoundRect(new RectF(20, y, screenWidth - 20, y + 130), 16, 16, card);

            Paint circle = new Paint(Paint.ANTI_ALIAS_FLAG);
            circle.setColor(colors[i]);
            canvas.drawCircle(65, y + 50, 32, circle);

            Paint num = new Paint(Paint.ANTI_ALIAS_FLAG);
            num.setColor(Color.WHITE);
            num.setTextSize(34);
            num.setTextAlign(Paint.Align.CENTER);
            num.setFakeBoldText(true);
            canvas.drawText(String.valueOf(i + 1), 65, y + 62, num);

            Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
            tp.setColor(Color.WHITE);
            tp.setTextSize(32);
            tp.setFakeBoldText(true);
            canvas.drawText(titles[i], 115, y + 40, tp);

            Paint dp = new Paint(Paint.ANTI_ALIAS_FLAG);
            dp.setColor(Color.parseColor("#AAAAAA"));
            dp.setTextSize(25);

            String desc = descs[i];
            if (desc.length() > 44) {
                int split = desc.lastIndexOf(' ', 44);
                if (split < 0) split = 44;
                canvas.drawText(desc.substring(0, split), 115, y + 78, dp);
                canvas.drawText(desc.substring(split + 1), 115, y + 108, dp);
            } else {
                canvas.drawText(desc, 115, y + 78, dp);
            }

            y += 148;
        }
    }

    private void drawGameScreen(Canvas canvas) {
        if (gridManager == null || shooter == null) return;

        canvas.drawRect(0, 0, screenWidth, 120, hudBgPaint);

        scorePaint.setTextAlign(Paint.Align.LEFT);
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(38);
        canvas.drawText("Score: " + score, 20, 70, scorePaint);

        scorePaint.setTextAlign(Paint.Align.CENTER);
        scorePaint.setColor(Color.parseColor("#FF6B9D"));
        scorePaint.setTextSize(46);
        canvas.drawText("Level " + level, screenWidth / 2f, 70, scorePaint);

        // hearts for remaining lives
        drawHearts(canvas);

        canvas.drawLine(0, screenHeight - 260, screenWidth, screenHeight - 260, dangerLinePaint);

        for (Bubble b : gridManager.getAllBubbles()) b.draw(canvas);
        for (Bubble b : movingBubbles) b.draw(canvas);

        shooter.draw(canvas);

        Paint wm = new Paint(Paint.ANTI_ALIAS_FLAG);
        wm.setColor(Color.parseColor("#22FFFFFF"));
        wm.setTextSize(20);
        wm.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("by Fayaz Ali", screenWidth / 2f, screenHeight - 8, wm);
    }

    private void drawHearts(Canvas canvas) {
        Paint heart = new Paint(Paint.ANTI_ALIAS_FLAG);
        heart.setColor(Color.parseColor("#F44336"));
        float size = 14;
        float startX = screenWidth - 30 - (MAX_LIVES - 1) * (size * 2.6f);
        for (int i = 0; i < lives; i++) {
            float hx = startX + i * (size * 2.6f);
            float hy = 58;
            // two circles and a triangle make a simple heart
            canvas.drawCircle(hx - size * 0.45f, hy - size * 0.2f, size * 0.55f, heart);
            canvas.drawCircle(hx + size * 0.45f, hy - size * 0.2f, size * 0.55f, heart);
            android.graphics.Path p = new android.graphics.Path();
            p.moveTo(hx - size, hy);
            p.lineTo(hx + size, hy);
            p.lineTo(hx, hy + size * 1.1f);
            p.close();
            canvas.drawPath(p, heart);
        }
    }

    private void drawLevelWin(Canvas canvas) {
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;

        canvas.drawRect(0, 0, screenWidth, screenHeight, overlayPaint);

        titlePaint.setTextSize(70);
        titlePaint.setColor(Color.parseColor("#FFD700"));
        canvas.drawText("LEVEL " + level, cx, cy - 160, titlePaint);
        titlePaint.setColor(Color.parseColor("#4CAF50"));
        canvas.drawText("COMPLETE!", cx, cy - 75, titlePaint);

        drawStars(canvas, cx, cy - 20);

        textPaint.setTextSize(48);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("Score: " + score, cx, cy + 70, textPaint);

        if ((level + 1) % 10 == 0) {
            textPaint.setTextSize(36);
            textPaint.setColor(Color.parseColor("#FF9800"));
            canvas.drawText("Next level: 2 BOMBS gifted!", cx, cy + 130, textPaint);
        }

        if (level < MAX_LEVEL) {
            btnPaint.setColor(Color.parseColor("#2196F3"));
            canvas.drawRoundRect(new RectF(cx - 180, cy + 160, cx + 180, cy + 250), 50, 50, btnPaint);
            btnTextPaint.setTextSize(50);
            canvas.drawText("NEXT LEVEL", cx, cy + 223, btnTextPaint);
        } else {
            textPaint.setTextSize(42);
            textPaint.setColor(Color.parseColor("#FFD700"));
            canvas.drawText("YOU BEAT ALL 200 LEVELS!", cx, cy + 190, textPaint);
        }
    }

    private void drawStars(Canvas canvas, float cx, float cy) {
        Paint star = new Paint(Paint.ANTI_ALIAS_FLAG);
        star.setColor(Color.parseColor("#FFD700"));
        float[] offsets = {-90, 0, 90};
        for (float off : offsets) {
            android.graphics.Path p = new android.graphics.Path();
            float r = 30;
            for (int i = 0; i < 10; i++) {
                double a = Math.PI / 5 * i - Math.PI / 2;
                float rad = (i % 2 == 0) ? r : r * 0.45f;
                float px = cx + off + (float) (Math.cos(a) * rad);
                float py = cy + (float) (Math.sin(a) * rad);
                if (i == 0) p.moveTo(px, py);
                else p.lineTo(px, py);
            }
            p.close();
            canvas.drawPath(p, star);
        }
    }

    private void drawGameOver(Canvas canvas) {
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;

        canvas.drawRect(0, 0, screenWidth, screenHeight, overlayPaint);

        titlePaint.setTextSize(85);
        titlePaint.setColor(Color.parseColor("#F44336"));
        canvas.drawText("GAME OVER", cx, cy - 220, titlePaint);

        textPaint.setTextSize(48);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("Score: " + score, cx, cy - 155, textPaint);

        textPaint.setTextSize(40);
        textPaint.setColor(Color.parseColor("#FFD700"));
        canvas.drawText("Reached Level: " + level, cx, cy - 100, textPaint);

        Paint line = new Paint();
        line.setColor(Color.parseColor("#444444"));
        line.setStrokeWidth(1);
        canvas.drawLine(cx - 200, cy - 70, cx + 200, cy - 70, line);

        textPaint.setTextSize(32);
        textPaint.setColor(Color.parseColor("#AAAAAA"));
        canvas.drawText("TOP SCORES", cx, cy - 35, textPaint);

        List<String> scores = scoreManager.getTopScores();
        for (int i = 0; i < scores.size(); i++) {
            textPaint.setColor(i == 0 ? Color.parseColor("#FFD700") : Color.parseColor("#888888"));
            canvas.drawText(scores.get(i), cx, cy + 15 + i * 42, textPaint);
        }

        btnPaint.setColor(Color.parseColor("#FF6B9D"));
        canvas.drawRoundRect(new RectF(cx - 180, cy + 235, cx + 180, cy + 325), 50, 50, btnPaint);
        btnTextPaint.setTextSize(48);
        canvas.drawText("MAIN MENU", cx, cy + 298, btnTextPaint);

        namePaint.setTextSize(32);
        canvas.drawText("by Fayaz Ali", cx, screenHeight - 30, namePaint);
    }

    // ---- touch ---------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float tx = event.getX();
        float ty = event.getY();
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchY = ty;
                if (state == STATE_PLAYING && shooter != null) shooter.updateAim(tx, ty);
                break;

            case MotionEvent.ACTION_MOVE:
                if (state == STATE_PLAYING && shooter != null) {
                    shooter.updateAim(tx, ty);
                } else if (state == STATE_LEVEL_SELECT) {
                    levelSelectScrollY += ty - lastTouchY;
                    levelSelectScrollY = Math.min(0, levelSelectScrollY);
                    lastTouchY = ty;
                }
                break;

            case MotionEvent.ACTION_UP:
                handleTap(tx, ty, cx, cy);
                break;
        }
        return true;
    }

    private void handleTap(float tx, float ty, int cx, int cy) {
        if (state == STATE_START) {
            if (inBox(tx, ty, cx - 180, cy + 65, cx + 180, cy + 155)) {
                level = Math.max(1, scoreManager.getSavedLevel());
                lives = MAX_LIVES;
                score = 0;
                state = STATE_PLAYING;
                initGame();
            } else if (inBox(tx, ty, cx - 180, cy + 175, cx + 180, cy + 255)) {
                levelSelectScrollY = 0;
                state = STATE_LEVEL_SELECT;
            } else if (inBox(tx, ty, cx - 180, cy + 275, cx + 180, cy + 355)) {
                state = STATE_HOW_TO_PLAY;
            }

        } else if (state == STATE_LEVEL_SELECT) {
            if (tx < 130 && ty < 100) {
                state = STATE_START;
                return;
            }
            int cols = 5;
            float cellSize = (screenWidth - 40f) / cols;
            int unlocked = scoreManager.getHighestUnlockedLevel();
            for (int i = 0; i < MAX_LEVEL; i++) {
                float lx = 20 + (i % cols) * cellSize + cellSize / 2f;
                float ly = 130 + levelSelectScrollY + (i / cols) * cellSize + cellSize / 2f;
                float dx = tx - lx, dy = ty - ly;
                if (Math.sqrt(dx * dx + dy * dy) < cellSize * 0.42f && (i + 1) <= unlocked) {
                    level = i + 1;
                    lives = MAX_LIVES;
                    score = 0;
                    state = STATE_PLAYING;
                    initGame();
                    return;
                }
            }

        } else if (state == STATE_HOW_TO_PLAY) {
            if (tx < 130 && ty < 100) state = STATE_START;

        } else if (state == STATE_PLAYING) {
            if (shooter == null) return;
            if (shooter.isSwapButtonTapped(tx, ty)) {
                shooter.swapBubbles();
                return;
            }
            if (ty < shooter.y - 50) {
                Bubble shot = shooter.shoot();
                if (shot != null) movingBubbles.add(shot);
            }

        } else if (state == STATE_LEVEL_WIN) {
            if (inBox(tx, ty, cx - 180, cy + 160, cx + 180, cy + 250) && level < MAX_LEVEL) {
                level++;
                lives = MAX_LIVES;
                scoreManager.saveCurrentLevel(level);
                state = STATE_PLAYING;
                initGame();
            }

        } else if (state == STATE_GAMEOVER) {
            if (inBox(tx, ty, cx - 180, cy + 235, cx + 180, cy + 325)) {
                state = STATE_START;
            }
        }
    }

    private boolean inBox(float tx, float ty, float left, float top, float right, float bottom) {
        return tx > left && tx < right && ty > top && ty < bottom;
    }

    // ---- surface lifecycle ---------------------------------------------

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();
        gameThread = new GameThread(holder, this);
        gameThread.setRunning(true);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        screenWidth = w;
        screenHeight = h;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        if (gameThread != null) {
            gameThread.setRunning(false);
            while (retry) {
                try {
                    gameThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
