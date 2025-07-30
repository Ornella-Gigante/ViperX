package es.nellagames.viperx;

import android.content.Context;
import android.graphics.*;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread thread;
    private List<Point> snake = new ArrayList<>();
    private Direction direction = Direction.RIGHT;
    private Direction pendingDirection = null;
    private Point food = new Point(5, 5);
    private int correctAnswer = 0;
    private int questionA = 1, questionB = 1;
    private String operation = "+";
    private int score = 0;
    private boolean gameOver = false;
    private final int cellSize = 70;
    private final int numCells = 10;
    private float startX, startY;

    // Bonus variables (adapt as needed)
    private Point bonusFood = null;
    private int bonusValue = 5;
    private boolean showBonus = false;

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint questionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // SOUND EFFECTS
    private SoundPool soundPool;
    private int correctSound, errorSound, bonusSound, loseSound;

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);

        getHolder().addCallback(this);
        thread = new GameThread(getHolder(), this);
        restartGame();

        questionPaint.setColor(Color.rgb(33, 150, 243));
        questionPaint.setTextSize(65f);
        questionPaint.setTypeface(Typeface.DEFAULT_BOLD);

        scorePaint.setColor(Color.rgb(50, 50, 50));
        scorePaint.setTextSize(40f);
        scorePaint.setTypeface(Typeface.DEFAULT);

        soundPool = new SoundPool.Builder().setMaxStreams(4).build();
        correctSound = soundPool.load(context, R.raw.correct, 1);
        errorSound = soundPool.load(context, R.raw.error, 1);
        bonusSound = soundPool.load(context, R.raw.bonus, 1);
        loseSound = soundPool.load(context, R.raw.lose, 1);
    }

    public void restartGame() {
        snake.clear();
        snake.add(new Point(4, 5));
        direction = Direction.RIGHT;
        score = 0;
        gameOver = false;
        showBonus = false;
        spawnMathQuestion();
        bonusFood = null;
    }

    private void spawnMathQuestion() {
        Random rand = new Random();
        questionA = rand.nextInt(9) + 1;
        questionB = rand.nextInt(9) + 1;
        operation = rand.nextBoolean() ? "+" : "-";
        correctAnswer = operation.equals("+") ? questionA + questionB : questionA - questionB;
        int x = rand.nextInt(numCells);
        int y = rand.nextInt(numCells);
        food = new Point(x, y);
        while (snakeContains(food)) {
            food = new Point(rand.nextInt(numCells), rand.nextInt(numCells));
        }
        // Aparece un bonus extra aleatorio a veces (20% de chance)
        showBonus = rand.nextInt(5) == 0;
        if (showBonus) {
            do {
                bonusFood = new Point(rand.nextInt(numCells), rand.nextInt(numCells));
            } while (snakeContains(bonusFood) || bonusFood.equals(food));
        } else {
            bonusFood = null;
        }
    }

    private boolean snakeContains(Point p) {
        for (Point s : snake) if (s.equals(p)) return true;
        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;
        canvas.drawColor(Color.rgb(250, 250, 200));

        // Question bar background
        Paint rectPaint = new Paint();
        rectPaint.setColor(Color.rgb(230, 230, 250));
        canvas.drawRect(0, 0, getWidth(), 110f, rectPaint);

        // Math question and score text
        canvas.drawText("Q: " + questionA + " " + operation + " " + questionB + " = ?", 25f, 75f, questionPaint);
        canvas.drawText("Score: " + score, 20f, 160f, scorePaint);

        // Draw snake (head green, body lighter green)
        for (int i = 0; i < snake.size(); i++) {
            Point p = snake.get(i);
            paint.setColor(i == 0 ? Color.rgb(76, 175, 80) : Color.rgb(139, 195, 74));
            canvas.drawRect(
                    p.x * cellSize,
                    p.y * cellSize + 120,
                    (p.x + 1) * cellSize,
                    (p.y + 1) * cellSize + 120,
                    paint);
        }

        // Draw normal food (correct answer) — orange oval with number
        paint.setColor(Color.rgb(255, 87, 34));
        canvas.drawOval(
                food.x * cellSize + 8,
                food.y * cellSize + 128,
                food.x * cellSize + cellSize - 8,
                food.y * cellSize + cellSize + 112,
                paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(40f);
        canvas.drawText(
                String.valueOf(correctAnswer),
                food.x * cellSize + 20,
                food.y * cellSize + 150,
                paint);

        // Draw bonus food (yellow oval with +bonusValue)
        if (showBonus && bonusFood != null) {
            paint.setColor(Color.YELLOW);
            canvas.drawOval(
                    bonusFood.x * cellSize + 8,
                    bonusFood.y * cellSize + 128,
                    bonusFood.x * cellSize + cellSize - 8,
                    bonusFood.y * cellSize + cellSize + 112,
                    paint);
            paint.setColor(Color.BLACK);
            paint.setTextSize(32f);
            canvas.drawText(
                    "+" + bonusValue,
                    bonusFood.x * cellSize + 16,
                    bonusFood.y * cellSize + 154,
                    paint);
        }

        // Draw Game Over text centered if game over
        if (gameOver) {
            paint.setColor(Color.BLACK);
            paint.setTextSize(80f);
            canvas.drawText("GAME OVER", 50f, getHeight() / 2f, paint);
            canvas.drawText("Tap to Restart", 50f, getHeight() / 2f + 100, paint);
        }
    }

    public void update() {
        if (gameOver) return;

        // Update snake direction if pending and not opposite
        if (pendingDirection != null && !direction.isOpposite(pendingDirection)) {
            direction = pendingDirection;
            pendingDirection = null;
        }

        Point head = new Point(snake.get(0));
        switch (direction) {
            case UP:    head.y -= 1; break;
            case DOWN:  head.y += 1; break;
            case LEFT:  head.x -= 1; break;
            case RIGHT: head.x += 1; break;
        }

        // Check collision with wall or self
        if (head.x < 0 || head.y < 0 || head.x >= numCells || head.y >= numCells || snakeContains(head)) {
            gameOver = true;
            soundPool.play(loseSound, 1f, 1f, 1, 0, 1f);  // Play lose sound
        } else {
            snake.add(0, head);
            if (head.equals(food)) {
                soundPool.play(correctSound, 1f, 1f, 1, 0, 1f);
                score++;
                spawnMathQuestion();
            } else if (showBonus && bonusFood != null && head.equals(bonusFood)) {
                soundPool.play(bonusSound, 1f, 1f, 1, 0, 1f);
                score += bonusValue;
                showBonus = false;
                bonusFood = null;
            } else {
                snake.remove(snake.size() - 1);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver && event.getAction() == MotionEvent.ACTION_UP) {
            restartGame();
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                float dx = event.getX() - startX;
                float dy = event.getY() - startY;
                if (Math.abs(dx) > Math.abs(dy)) {
                    pendingDirection = dx > 0 ? Direction.RIGHT : Direction.LEFT;
                } else {
                    pendingDirection = dy > 0 ? Direction.DOWN : Direction.UP;
                }
                break;
        }
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread.setRunning(true);
        thread.start();
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        thread.setRunning(false);
        try {
            thread.join();
        } catch (InterruptedException e) { e.printStackTrace(); }
    }

    public void pause() {
        thread.setRunning(false);
    }

    public void resume() {
        if (!thread.isRunning()) {
            thread.setRunning(true);
            thread.start();
        }
    }
}
