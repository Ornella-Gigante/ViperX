package es.nellagames.viperx;

import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

public class GameThread extends Thread {
    private final SurfaceHolder surfaceHolder;
    private final GameView gameView;
    private boolean running = false;

    public GameThread(SurfaceHolder surfaceHolder, GameView gameView, long gameSpeed) {
        this.surfaceHolder = surfaceHolder;
        this.gameView = gameView;
    }

    public void setRunning(boolean run) {
        running = run;
        Log.d("GameThread", "Thread running set to: " + run);
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        Log.d("GameThread", "Game thread started");
        while (running) {
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
                Log.e("GameThread", "Error in game loop: " + e.getMessage());
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        Log.e("GameThread", "Error unlocking canvas: " + e.getMessage());
                    }
                }
            }

            try {
                sleep(200);
            } catch (InterruptedException e) {
                Log.e("GameThread", "Thread interrupted: " + e.getMessage());
                break;
            }
        }
        Log.d("GameThread", "Game thread ended");
    }
}