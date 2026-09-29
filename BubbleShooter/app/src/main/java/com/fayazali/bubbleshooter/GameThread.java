package com.fayazali.bubbleshooter;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * The game loop. Runs on its own thread, calls update() then draw() on the
 * GameView, and sleeps whatever time is left over to hold roughly 60 FPS.
 */
public class GameThread extends Thread {

    private final SurfaceHolder surfaceHolder;
    private final GameView gameView;
    private boolean running = false;

    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME = 1000 / TARGET_FPS;

    public GameThread(SurfaceHolder holder, GameView view) {
        this.surfaceHolder = holder;
        this.gameView = view;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        while (running) {
            long startTime = System.currentTimeMillis();
            Canvas canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    synchronized (surfaceHolder) {
                        gameView.update();
                        gameView.draw(canvas);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            long sleepTime = FRAME_TIME - (System.currentTimeMillis() - startTime);
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
