package es.nellagames.viperx;

import android.content.Context;
import android.graphics.*;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.core.content.ContextCompat;
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

    // Bonus variables
    private Point bonusFood = null;
    private int bonusValue = 5;
    private boolean showBonus = false;

    private Paint questionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // SOUND EFFECTS
    private SoundPool soundPool;
    private int correctSound, errorSound, bonusSound, loseSound;

    // Bitmap assets
    private Bitmap head_up, head_down, head_left, head_right;
    private Bitmap body_vertical, body_horizontal, body_topleft, body_topright, body_bottomleft, body_bottomright;
    private Bitmap tail_up, tail_down, tail_left, tail_right;
    private Bitmap apple, candy, sushi1, sushi2;

    // Used for food randomization
    private Bitmap[] foodBitmaps;

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

        // Load all bitmap assets
        head_up = BitmapFactory.decodeResource(getResources(), R.drawable.head_up);
        head_down = BitmapFactory.decodeResource(getResources(), R.drawable.head_down);
        head_left = BitmapFactory.decodeResource(getResources(), R.drawable.head_left);
        head_right = BitmapFactory.decodeResource(getResources(), R.drawable.head_right);

        body_vertical = BitmapFactory.decodeResource(getResources(), R.drawable.body_vertical);
        body_horizontal = BitmapFactory.decodeResource(getResources(), R.drawable.body_horizontal);
        body_topleft = BitmapFactory.decodeResource(getResources(), R.drawable.body_topleft);
        body_topright = BitmapFactory.decodeResource(getResources(), R.drawable.body_topright);
        body_bottomleft = BitmapFactory.decodeResource(getResources(), R.drawable.body_bottomleft);
        body_bottomright = BitmapFactory.decodeResource(getResources(), R.drawable.body_bottomright);

        tail_up = BitmapFactory.decodeResource(getResources(), R.drawable.tail_up);
        tail_down = BitmapFactory.decodeResource(getResources(), R.drawable.tail_down);
        tail_left = BitmapFactory.decodeResource(getResources(), R.drawable.tail_left);
        tail_right = BitmapFactory.decodeResource(getResources(), R.drawable.tail_right);

        apple = BitmapFactory.decodeResource(getResources(), R.drawable.apple);
        candy = BitmapFactory.decodeResource(getResources(), R.drawable.candy);
        sushi1 = BitmapFactory.decodeResource(getResources(), R.drawable.sushi1);
        sushi2 = BitmapFactory.decodeResource(getResources(), R.drawable.sushi2);

        foodBitmaps = new Bitmap[]{apple, candy, sushi1, sushi2};
    }

    public void restartGame() {
        snake.clear();
        snake.add(new Point(4, 5));
        snake.add(new Point(3, 5));
        direction = Direction.RIGHT;
        score = 0;
        gameOver = false;
        showBonus = false;
        spawnMathQuestion();
        bonusFood = null;
    }

    // Randomizes next question and food position
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
        // 20% chance bonus
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

        // Fondo suave
        canvas.drawColor(Color.rgb(250, 250, 200));

        // Barra superior con pregunta y marcador
        Paint rectPaint = new Paint();
        rectPaint.setColor(Color.rgb(230, 230, 250));
        canvas.drawRect(0, 0, getWidth(), 110f, rectPaint);

        canvas.drawText("Q: " + questionA + " " + operation + " " + questionB + " = ?", 25f, 75f, questionPaint);
        canvas.drawText("Score: " + score, 20f, 160f, scorePaint);

        // Dibuja la serpiente usando los sprites
        for (int i = 0; i < snake.size(); i++) {
            Point p = snake.get(i);
            int x = p.x * cellSize;
            int y = p.y * cellSize + 120;
            Bitmap sprite = null;

            if (i == 0) {
                // Cabeza
                Direction headDir = direction;
                if (snake.size() > 1) {
                    Point after = snake.get(1);
                    if (after.x < p.x) headDir = Direction.RIGHT;
                    if (after.x > p.x) headDir = Direction.LEFT;
                    if (after.y < p.y) headDir = Direction.DOWN;
                    if (after.y > p.y) headDir = Direction.UP;
                }
                switch (headDir) {
                    case UP:    sprite = head_up; break;
                    case DOWN:  sprite = head_down; break;
                    case LEFT:  sprite = head_left; break;
                    case RIGHT: sprite = head_right; break;
                }
            } else if (i == snake.size() - 1) {
                // Cola
                Point before = snake.get(i - 1);
                if (before.x == p.x) {
                    sprite = (before.y < p.y) ? tail_up : tail_down;
                } else if (before.y == p.y) {
                    sprite = (before.x < p.x) ? tail_left : tail_right;
                }
            } else {
                // Cuerpo o esquina
                Point before = snake.get(i - 1), after = snake.get(i + 1);
                if (before.x == after.x) {
                    sprite = body_vertical;
                } else if (before.y == after.y) {
                    sprite = body_horizontal;
                } else {
                    if ((before.x < p.x && after.y < p.y) || (after.x < p.x && before.y < p.y)) {
                        sprite = body_topleft;
                    } else if ((before.x > p.x && after.y < p.y) || (after.x > p.x && before.y < p.y)) {
                        sprite = body_topright;
                    } else if ((before.x < p.x && after.y > p.y) || (after.x < p.x && before.y > p.y)) {
                        sprite = body_bottomleft;
                    } else if ((before.x > p.x && after.y > p.y) || (after.x > p.x && before.y > p.y)) {
                        sprite = body_bottomright;
                    }
                }
            }

            if (sprite != null)
                canvas.drawBitmap(Bitmap.createScaledBitmap(sprite, cellSize, cellSize, false), x, y, null);
        }

        // Dibuja la comida (selección aleatoria)
        int fx = food.x * cellSize + 8;
        int fy = food.y * cellSize + 128;
        Bitmap foodBmp = foodBitmaps[Math.abs(correctAnswer) % foodBitmaps.length];
        canvas.drawBitmap(Bitmap.createScaledBitmap(foodBmp, cellSize - 16, cellSize - 16, false), fx, fy, null);

        // Número encima de la comida (respuesta correcta)
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(38f);
        textPaint.setFakeBoldText(true);
        canvas.drawText(
                String.valueOf(correctAnswer),
                food.x * cellSize + cellSize / 4,
                food.y * cellSize + 128 + cellSize / 2 + 12,
                textPaint);

        // Dibuja el bonus (si aparece)
        if (showBonus && bonusFood != null) {
            int bx = bonusFood.x * cellSize + 8, by = bonusFood.y * cellSize + 128;
            Bitmap bmp = foodBitmaps[(score + 1) % foodBitmaps.length];
            canvas.drawBitmap(Bitmap.createScaledBitmap(bmp, cellSize - 16, cellSize - 16, false), bx, by, null);
            Paint bPaint = new Paint();
            bPaint.setColor(Color.YELLOW);
            bPaint.setTextSize(30f);
            bPaint.setFakeBoldText(true);
            canvas.drawText("+" + bonusValue,
                    bonusFood.x * cellSize + cellSize / 6,
                    bonusFood.y * cellSize + 128 + cellSize / 2 + 14,
                    bPaint);
        }

        // Game Over
        if (gameOver) {
            Paint overPaint = new Paint();
            overPaint.setColor(Color.BLACK);
            overPaint.setTextSize(80f);
            overPaint.setTextAlign(Paint.Align.LEFT);
            overPaint.setFakeBoldText(true);
            canvas.drawText("GAME OVER", 50f, getHeight() / 2f, overPaint);
            canvas.drawText("Tap to Restart", 50f, getHeight() / 2f + 100, overPaint);
        }
    }

    public void update() {
        if (gameOver) return;

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

        if (head.x < 0 || head.y < 0 || head.x >= numCells || head.y >= numCells || snakeContains(head)) {
            gameOver = true;
            soundPool.play(loseSound, 1f, 1f, 1, 0, 1f);
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
        try { thread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    public void pause() { thread.setRunning(false); }
    public void resume() {
        if (!thread.isRunning()) {
            thread.setRunning(true);
            thread.start();
        }
    }
}
