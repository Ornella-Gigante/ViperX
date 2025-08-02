package es.nellagames.viperx;

import android.content.Context;
import android.graphics.*;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.util.Log;
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
    private final int numCells = 10; // cuadrícula 10x10
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

        try {
            soundPool = new SoundPool.Builder().setMaxStreams(4).build();
            correctSound = soundPool.load(context, R.raw.correct, 1);
            errorSound = soundPool.load(context, R.raw.error, 1);
            bonusSound = soundPool.load(context, R.raw.bonus, 1);
            loseSound = soundPool.load(context, R.raw.lose, 1);
        } catch (Exception e) {
            Log.e("GameView", "Error loading sounds: " + e.getMessage());
        }

        // Load all bitmap assets con manejo de errores
        try {
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
            Log.d("GameView", "All bitmaps loaded successfully");
        } catch (Exception e) {
            Log.e("GameView", "Error loading bitmaps: " + e.getMessage());
            createFallbackBitmaps();
        }
    }

    private void createFallbackBitmaps() {
        int size = 70; // usado solo como fallback
        head_up = head_down = head_left = head_right = createColorBitmap(size, Color.GREEN);
        body_vertical = body_horizontal = body_topleft = body_topright =
                body_bottomleft = body_bottomright = createColorBitmap(size, Color.BLUE);
        tail_up = tail_down = tail_left = tail_right = createColorBitmap(size, Color.CYAN);
        apple = candy = sushi1 = sushi2 = createColorBitmap(size, Color.RED);
        foodBitmaps = new Bitmap[]{apple, candy, sushi1, sushi2};
        Log.d("GameView", "Using fallback bitmaps");
    }

    private Bitmap createColorBitmap(int size, int color) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawRect(0, 0, size, size, paint);
        return bitmap;
    }

    public void restartGame() {
        snake.clear();
        snake.add(new Point(4, 5));
        snake.add(new Point(3, 5));
        direction = Direction.RIGHT;
        pendingDirection = null;
        score = 0;
        gameOver = false;
        showBonus = false;
        spawnMathQuestion();
        bonusFood = null;
        Log.d("GameView", "Game restarted");
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
        if (canvas == null) {
            Log.d("GameView", "Canvas is null!");
            return;
        }

        // ==== CÁLCULOS DINÁMICOS DE GRID/CELDA ====
        int panelHeight = 120; // altura barra superior
        int gridRows = numCells, gridCols = numCells;
        int availableWidth = canvas.getWidth();
        int availableHeight = canvas.getHeight() - panelHeight;
        int cellSizeDynamic = Math.min(availableWidth / gridCols, availableHeight / gridRows);

        int gridWidth = cellSizeDynamic * gridCols;
        int gridHeight = cellSizeDynamic * gridRows;
        int offsetX = (availableWidth - gridWidth) / 2;
        int offsetY = panelHeight + (availableHeight - gridHeight) / 2;

        // Checkerboard fondo extendido y centrado
        int color1 = Color.rgb(170, 215, 81);
        int color2 = Color.rgb(162, 209, 73);
        Paint squarePaint = new Paint();

        for (int y = 0; y < gridRows; y++) {
            for (int x = 0; x < gridCols; x++) {
                squarePaint.setColor(((x + y) % 2 == 0) ? color1 : color2);
                canvas.drawRect(
                        offsetX + x * cellSizeDynamic,
                        offsetY + y * cellSizeDynamic,
                        offsetX + (x + 1) * cellSizeDynamic,
                        offsetY + (y + 1) * cellSizeDynamic,
                        squarePaint
                );
            }
        }

        // Barra superior - pregunta/score
        Paint rectPaint = new Paint();
        rectPaint.setColor(Color.rgb(230, 230, 250));
        canvas.drawRect(0, 0, canvas.getWidth(), panelHeight - 10f, rectPaint);
        canvas.drawText("Q: " + questionA + " " + operation + " " + questionB + " = ?", 25f, 75f, questionPaint);
        canvas.drawText("Score: " + score, 20f, panelHeight + 40f, scorePaint);

        // --- Dibuja la serpiente ---
        for (int i = 0; i < snake.size(); i++) {
            Point p = snake.get(i);
            int x = offsetX + p.x * cellSizeDynamic;
            int y = offsetY + p.y * cellSizeDynamic;
            Bitmap sprite = null;
            if (i == 0) {
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
                Point before = snake.get(i - 1);
                if (before.x == p.x) sprite = (before.y < p.y) ? tail_up : tail_down;
                else if (before.y == p.y) sprite = (before.x < p.x) ? tail_left : tail_right;
            } else {
                Point before = snake.get(i - 1), after = snake.get(i + 1);
                if (before.x == after.x) sprite = body_vertical;
                else if (before.y == after.y) sprite = body_horizontal;
                else {
                    if ((before.x < p.x && after.y < p.y) || (after.x < p.x && before.y < p.y))
                        sprite = body_topleft;
                    else if ((before.x > p.x && after.y < p.y) || (after.x > p.x && before.y < p.y))
                        sprite = body_topright;
                    else if ((before.x < p.x && after.y > p.y) || (after.x < p.x && before.y > p.y))
                        sprite = body_bottomleft;
                    else if ((before.x > p.x && after.y > p.y) || (after.x > p.x && before.y > p.y))
                        sprite = body_bottomright;
                }
            }
            if (sprite != null)
                canvas.drawBitmap(Bitmap.createScaledBitmap(sprite, cellSizeDynamic, cellSizeDynamic, false), x, y, null);
        }

        // --- Dibuja la comida ---
        int fx = offsetX + food.x * cellSizeDynamic + 8;
        int fy = offsetY + food.y * cellSizeDynamic + 8;
        try {
            Bitmap foodBmp = foodBitmaps[Math.abs(correctAnswer) % foodBitmaps.length];
            canvas.drawBitmap(Bitmap.createScaledBitmap(foodBmp, cellSizeDynamic - 16, cellSizeDynamic - 16, false), fx, fy, null);
        } catch (Exception e) {
            Paint foodPaint = new Paint();
            foodPaint.setColor(Color.RED);
            canvas.drawCircle(fx + (cellSizeDynamic - 16) / 2, fy + (cellSizeDynamic - 16) / 2, (cellSizeDynamic - 16) / 2, foodPaint);
        }

        // Número encima de la comida
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(38f);
        textPaint.setFakeBoldText(true);
        canvas.drawText(String.valueOf(correctAnswer), offsetX + food.x * cellSizeDynamic + cellSizeDynamic / 4, offsetY + food.y * cellSizeDynamic + cellSizeDynamic / 2 + 12, textPaint);

        // --- Dibuja el bonus ---
        if (showBonus && bonusFood != null) {
            int bx = offsetX + bonusFood.x * cellSizeDynamic + 8, by = offsetY + bonusFood.y * cellSizeDynamic + 8;
            try {
                Bitmap bmp = foodBitmaps[(score + 1) % foodBitmaps.length];
                canvas.drawBitmap(Bitmap.createScaledBitmap(bmp, cellSizeDynamic - 16, cellSizeDynamic - 16, false), bx, by, null);
            } catch (Exception e) {
                Paint bonusPaint = new Paint();
                bonusPaint.setColor(Color.YELLOW);
                canvas.drawCircle(bx + (cellSizeDynamic - 16) / 2, by + (cellSizeDynamic - 16) / 2, (cellSizeDynamic - 16) / 2, bonusPaint);
            }
            Paint bPaint = new Paint();
            bPaint.setColor(Color.YELLOW);
            bPaint.setTextSize(30f);
            bPaint.setFakeBoldText(true);
            canvas.drawText("+" + bonusValue, offsetX + bonusFood.x * cellSizeDynamic + cellSizeDynamic / 6, offsetY + bonusFood.y * cellSizeDynamic + cellSizeDynamic / 2 + 14, bPaint);
        }

        // --- Game Over ---
        if (gameOver) {
            Paint overPaint = new Paint();
            overPaint.setColor(Color.BLACK);
            overPaint.setTextSize(80f);
            overPaint.setFakeBoldText(true);
            canvas.drawText("GAME OVER", 50, getHeight() / 2, overPaint);
            canvas.drawText("Tap to Restart", 50, getHeight() / 2 + 100, overPaint);
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
            try {
                soundPool.play(loseSound, 1f, 1f, 1, 0, 1f);
            } catch (Exception e) {
                Log.e("GameView", "Error playing lose sound");
            }
        } else {
            snake.add(0, head);
            if (head.equals(food)) {
                try {
                    soundPool.play(correctSound, 1f, 1f, 1, 0, 1f);
                } catch (Exception e) {
                    Log.e("GameView", "Error playing correct sound");
                }
                score++;
                spawnMathQuestion();
            } else if (showBonus && bonusFood != null && head.equals(bonusFood)) {
                try {
                    soundPool.play(bonusSound, 1f, 1f, 1, 0, 1f);
                } catch (Exception e) {
                    Log.e("GameView", "Error playing bonus sound");
                }
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
        Log.d("GameView", "Surface created");
        if (thread != null && !thread.isRunning()) {
            thread.setRunning(true);
            try {
                thread.start();
            } catch (IllegalThreadStateException e) {
                thread = new GameThread(getHolder(), this);
                thread.setRunning(true);
                thread.start();
            }
        }
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("GameView", "Surface destroyed");
        if (thread != null) {
            thread.setRunning(false);
            try {
                thread.join();
            } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    public void pause() {
        Log.d("GameView", "Pausing game");
        if (thread != null) {
            thread.setRunning(false);
            try {
                thread.join();
            } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    public void resume() {
        Log.d("GameView", "Resuming game");
        if (thread != null && !thread.isRunning()) {
            thread = new GameThread(getHolder(), this);
            thread.setRunning(true);
            try {
                thread.start();
            } catch (IllegalThreadStateException e) {
                Log.e("GameView", "Error starting thread: " + e.getMessage());
            }
        }
    }
}
