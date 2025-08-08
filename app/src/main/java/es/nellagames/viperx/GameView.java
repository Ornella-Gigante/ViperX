package es.nellagames.viperx;

import android.content.Context;
import android.graphics.*;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread thread;
    private List<Point> snake = new ArrayList<>();
    private Direction direction = Direction.RIGHT;
    private Direction pendingDirection = null;
    private int correctAnswer = 0;
    private int questionA = 1, questionB = 1;
    private String operation = "+";
    private int score = 0;
    private boolean gameOver = false;
    private final int numCells = 10;
    private float startX, startY;

    private int bonusValue = 5;
    private final long gameSpeed = 1200;

    // TextViews para mostrar información
    private TextView questionTextView;
    private TextView scoreTextView;

    // SONIDOS
    private SoundPool soundPool;
    private int correctSound, errorSound, bonusSound, loseSound;
    private boolean soundsLoaded = false;

    // Sprites
    private Bitmap head_up, head_down, head_left, head_right;
    private Bitmap body_vertical, body_horizontal, body_topleft, body_topright, body_bottomleft, body_bottomright;
    private Bitmap tail_up, tail_down, tail_left, tail_right;
    private Bitmap apple, candy, sushi1, sushi2;
    private Bitmap[] foodBitmaps;
    private Bitmap gridBackground;

    // NUEVOS: sistema de foods correctos/incorrectos
    private class FoodItem {
        Point position;
        int value;
        boolean isCorrect;
        Bitmap bitmap;
        FoodItem(Point position, int value, boolean isCorrect, Bitmap bitmap) {
            this.position = position;
            this.value = value;
            this.isCorrect = isCorrect;
            this.bitmap = bitmap;
        }
    }
    private FoodItem correctFood;
    private List<FoodItem> wrongFoods = new ArrayList<>();
    private FoodItem bonusFood;

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        thread = new GameThread(getHolder(), this, gameSpeed);
        setFocusable(true);
        setFocusableInTouchMode(true);
        initializeSounds(context);
        initializeBitmaps();
        restartGame();
    }

    private void initializeSounds(Context context) {
        try {
            soundPool = new SoundPool.Builder().setMaxStreams(5).build();
            correctSound = soundPool.load(context, R.raw.correct, 1);
            errorSound = soundPool.load(context, R.raw.error, 1);
            bonusSound = soundPool.load(context, R.raw.bonus, 1);
            loseSound = soundPool.load(context, R.raw.lose, 1);
            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    if (status == 0) {
                        soundsLoaded = true;
                        Log.d("GameView", "Sound loaded successfully - ID: " + sampleId);
                    }
                }
            });
        } catch (Exception e) {
            Log.e("GameView", "Error initializing sounds: " + e.getMessage());
            soundPool = null;
        }
    }

    private void initializeBitmaps() {
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
            gridBackground = BitmapFactory.decodeResource(getResources(), R.drawable.cuadricula);
            foodBitmaps = new Bitmap[]{apple, candy, sushi1, sushi2};
        } catch (Exception e) {
            createFallbackBitmaps();
        }
    }

    private void playSound(int soundId, String soundName) {
        if (soundPool != null && soundId > 0) {
            try {
                soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
            } catch (Exception e) {
                Log.e("GameView", "Exception playing " + soundName + " sound: " + e.getMessage());
            }
        }
    }

    private void createFallbackBitmaps() {
        int size = 70;
        head_up = head_down = head_left = head_right = createColorBitmap(size, Color.GREEN);
        body_vertical = body_horizontal = body_topleft = body_topright = body_bottomleft = body_bottomright = createColorBitmap(size, Color.BLUE);
        tail_up = tail_down = tail_left = tail_right = createColorBitmap(size, Color.CYAN);
        apple = candy = sushi1 = sushi2 = createColorBitmap(size, Color.RED);
        foodBitmaps = new Bitmap[]{apple, candy, sushi1, sushi2};
        gridBackground = createGridFallback(700, Color.rgb(198, 255, 198), Color.BLACK);
    }

    private Bitmap createColorBitmap(int size, int color) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawRect(0, 0, size, size, paint);
        return bitmap;
    }

    private Bitmap createGridFallback(int size, int backgroundColor, int lineColor) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint bgPaint = new Paint();
        bgPaint.setColor(backgroundColor);
        canvas.drawRect(0, 0, size, size, bgPaint);
        Paint gridPaint = new Paint();
        gridPaint.setColor(lineColor);
        gridPaint.setStrokeWidth(2);
        int cellSize = size / numCells;
        for (int i = 0; i <= numCells; i++) {
            int x = i * cellSize;
            canvas.drawLine(x, 0, x, size, gridPaint);
        }
        for (int i = 0; i <= numCells; i++) {
            int y = i * cellSize;
            canvas.drawLine(0, y, size, y, gridPaint);
        }
        return bitmap;
    }

    public void setTextViews(TextView questionText, TextView scoreText) {
        this.questionTextView = questionText;
        this.scoreTextView = scoreText;
        updateTextViews();
    }

    private void updateTextViews() {
        if (questionTextView != null) questionTextView.post(() -> questionTextView.setText("Q: " + questionA + " " + operation + " " + questionB + " = ?"));
        if (scoreTextView != null) scoreTextView.post(() -> scoreTextView.setText("Score: " + score));
    }

    public void restartGame() {
        snake.clear();
        snake.add(new Point(4, 5));
        snake.add(new Point(3, 5));
        snake.add(new Point(2, 5));
        snake.add(new Point(1, 5));
        direction = Direction.RIGHT;
        pendingDirection = null;
        score = 0;
        gameOver = false;
        spawnQuizAndFoods();
        updateTextViews();
    }

    private void spawnQuizAndFoods() {
        Random rand = new Random();
        questionA = rand.nextInt(9) + 1;
        questionB = rand.nextInt(9) + 1;
        operation = rand.nextBoolean() ? "+" : "-";
        correctAnswer = operation.equals("+") ? questionA + questionB : questionA - questionB;

        if (foodBitmaps == null || foodBitmaps.length == 0) {
            createFallbackBitmaps();
        }

        correctFood = new FoodItem(getRandomFreePoint(), correctAnswer, true, foodBitmaps[Math.abs(correctAnswer) % foodBitmaps.length]);

        wrongFoods.clear();
        for (int i = 0; i < 2; i++) {
            int wrongVal;
            do {
                wrongVal = correctAnswer + (rand.nextInt(5) - 2);
            } while (wrongVal == correctAnswer);
            wrongFoods.add(new FoodItem(getRandomFreePoint(), wrongVal, false, foodBitmaps[Math.abs(wrongVal) % foodBitmaps.length]));
        }

        if (rand.nextInt(5) == 0) {
            bonusFood = new FoodItem(getRandomFreePoint(), bonusValue, true, foodBitmaps[rand.nextInt(foodBitmaps.length)]);
        } else {
            bonusFood = null;
        }
    }

    private Point getRandomFreePoint() {
        Random rand = new Random();
        Point p;
        do {
            p = new Point(rand.nextInt(numCells), rand.nextInt(numCells));
        } while (isPositionOccupied(p));
        return p;
    }

    private boolean isPositionOccupied(Point p) {
        return snakeContains(p)
                || (correctFood != null && p.equals(correctFood.position))
                || wrongFoods.stream().anyMatch(f -> f.position.equals(p))
                || (bonusFood != null && p.equals(bonusFood.position));
    }

    private boolean snakeContains(Point p) {
        for (Point s : snake) if (s.equals(p)) return true;
        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        canvas.drawColor(Color.BLACK);

        int gridRows = numCells, gridCols = numCells;
        int availableWidth = canvas.getWidth() - 32;
        int availableHeight = canvas.getHeight() - 16;
        int cellSizeDynamic = Math.min(availableWidth / gridCols, availableHeight / gridRows);
        int gridWidth = cellSizeDynamic * gridCols;
        int gridHeight = cellSizeDynamic * gridRows;
        int offsetX = (canvas.getWidth() - gridWidth) / 2;
        int offsetY = (canvas.getHeight() - gridHeight) / 2;

        if (gridBackground != null) {
            Bitmap scaledGrid = Bitmap.createScaledBitmap(gridBackground, gridWidth, gridHeight, false);
            canvas.drawBitmap(scaledGrid, offsetX, offsetY, null);
        } else {
            // Fallback grid drawing
            Paint cellPaint = new Paint();
            cellPaint.setColor(Color.rgb(198, 255, 198));
            for (int row = 0; row < gridRows; row++) {
                for (int col = 0; col < gridCols; col++) {
                    int x = offsetX + col * cellSizeDynamic;
                    int y = offsetY + row * cellSizeDynamic;
                    canvas.drawRect(x, y, x + cellSizeDynamic, y + cellSizeDynamic, cellPaint);
                }
            }
        }

        // DIBUJAR SERPIENTE
        if (snake != null && !snake.isEmpty()) {
            for (int i = 0; i < snake.size(); i++) {
                Point segment = snake.get(i);
                int x = offsetX + segment.x * cellSizeDynamic;
                int y = offsetY + segment.y * cellSizeDynamic;

                Bitmap segmentBitmap;
                if (i == 0) {
                    switch (direction) {
                        case UP: segmentBitmap = head_up; break;
                        case DOWN: segmentBitmap = head_down; break;
                        case LEFT: segmentBitmap = head_left; break;
                        case RIGHT: segmentBitmap = head_right; break;
                        default: segmentBitmap = head_right; break;
                    }
                } else if (i == snake.size() - 1) {
                    Point prev = snake.get(i - 1);
                    if (prev.x > segment.x) segmentBitmap = tail_right;
                    else if (prev.x < segment.x) segmentBitmap = tail_left;
                    else if (prev.y > segment.y) segmentBitmap = tail_down;
                    else segmentBitmap = tail_up;
                } else {
                    Point prev = snake.get(i - 1);
                    Point next = snake.get(i + 1);
                    if (prev.x == next.x) segmentBitmap = body_vertical;
                    else if (prev.y == next.y) segmentBitmap = body_horizontal;
                    else if ((prev.x < segment.x && next.y < segment.y) || (next.x < segment.x && prev.y < segment.y))
                        segmentBitmap = body_topleft;
                    else if ((prev.x > segment.x && next.y < segment.y) || (next.x > segment.x && prev.y < segment.y))
                        segmentBitmap = body_topright;
                    else if ((prev.x < segment.x && next.y > segment.y) || (next.x < segment.x && prev.y > segment.y))
                        segmentBitmap = body_bottomleft;
                    else segmentBitmap = body_bottomright;
                }
                canvas.drawBitmap(Bitmap.createScaledBitmap(segmentBitmap, cellSizeDynamic, cellSizeDynamic, false), x, y, null);
            }
        }

        // DIBUJAR ALIMENTOS con estética mejorada
        drawFood(canvas, correctFood, offsetX, offsetY, cellSizeDynamic, true);
        for (FoodItem wf : wrongFoods) drawFood(canvas, wf, offsetX, offsetY, cellSizeDynamic, false);
        if (bonusFood != null) drawFood(canvas, bonusFood, offsetX, offsetY, cellSizeDynamic, true);

        if (gameOver) {
            Paint overlayPaint = new Paint();
            overlayPaint.setColor(Color.argb(180, 0, 0, 0));
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

            Paint overPaint = new Paint();
            overPaint.setColor(Color.WHITE);
            overPaint.setTextSize(60f);
            overPaint.setFakeBoldText(true);
            overPaint.setTextAlign(Paint.Align.CENTER);
            overPaint.setShadowLayer(4, 2, 2, Color.BLACK);
            canvas.drawText("GAME OVER", getWidth() / 2, getHeight() / 2 - 50, overPaint);
            overPaint.setTextSize(40f);
            canvas.drawText("Tap to Restart", getWidth() / 2, getWidth() / 2 + 50, overPaint);
        }
    }

    // MÉTODO MEJORADO para dibujar alimentos con mejor estética
    private void drawFood(Canvas canvas, FoodItem food, int offsetX, int offsetY, int cellSize, boolean isCorrectOrBonus) {
        if (food == null) return;

        int x = offsetX + food.position.x * cellSize;
        int y = offsetY + food.position.y * cellSize;

        // Dibujar el bitmap de comida con efecto de brillo sutil
        Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (isCorrectOrBonus) {
            // Añadir un ligero brillo dorado para respuestas correctas y bonus
            bitmapPaint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{
                    1.2f, 0.1f, 0.1f, 0, 20,    // Red
                    0.1f, 1.2f, 0.1f, 0, 20,    // Green
                    0.1f, 0.1f, 1.0f, 0, 10,    // Blue
                    0, 0, 0, 1, 0               // Alpha
            })));
        }
        canvas.drawBitmap(Bitmap.createScaledBitmap(food.bitmap, cellSize, cellSize, false), x, y, bitmapPaint);

        // Crear fondo circular elegante para el número
        int circleRadius = Math.min(cellSize / 3, 25);
        int centerX = x + cellSize / 2;
        int centerY = y + cellSize / 2;

        // Fondo del círculo con gradiente
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (isCorrectOrBonus) {
            // Respuesta correcta o bonus: verde brillante con gradiente
            circlePaint.setShader(new RadialGradient(centerX, centerY, circleRadius,
                    Color.argb(240, 76, 175, 80),   // Verde central
                    Color.argb(200, 56, 142, 60),   // Verde más oscuro en borde
                    Shader.TileMode.CLAMP));
        } else {
            // Respuesta incorrecta: rojo elegante con gradiente
            circlePaint.setShader(new RadialGradient(centerX, centerY, circleRadius,
                    Color.argb(240, 244, 67, 54),   // Rojo central
                    Color.argb(200, 183, 28, 28),   // Rojo más oscuro en borde
                    Shader.TileMode.CLAMP));
        }
        canvas.drawCircle(centerX, centerY, circleRadius, circlePaint);

        // Borde del círculo con efecto brillante
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setShadowLayer(4, 0, 0, Color.argb(100, 255, 255, 255));
        canvas.drawCircle(centerX, centerY, circleRadius - 1, borderPaint);

        // Texto del número con tipografía mejorada
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Math.max(16f, cellSize * 0.35f));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Sombra elegante para el texto
        textPaint.setShadowLayer(3, 1, 1, Color.argb(150, 0, 0, 0));

        // Dibujar el número
        String numberText = String.valueOf(food.value);
        if (food == bonusFood) {
            numberText = "+" + food.value;
        }

        // Calcular posición Y para centrar el texto verticalmente
        Rect textBounds = new Rect();
        textPaint.getTextBounds(numberText, 0, numberText.length(), textBounds);
        float textY = centerY + textBounds.height() / 2f;

        canvas.drawText(numberText, centerX, textY, textPaint);

        // Efecto de partículas brillantes para bonus
        if (food == bonusFood) {
            drawSparkleEffect(canvas, centerX, centerY, cellSize);
        }
    }

    // Efecto de partículas brillantes para bonus
    private void drawSparkleEffect(Canvas canvas, int centerX, int centerY, int cellSize) {
        Paint sparklePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sparklePaint.setColor(Color.argb(180, 255, 215, 0));
        sparklePaint.setStyle(Paint.Style.FILL);

        int sparkleRadius = cellSize / 2;
        Random sparkleRandom = new Random(System.currentTimeMillis() / 1000); // Cambio lento

        for (int i = 0; i < 4; i++) {
            float angle = (i * 90 + sparkleRandom.nextFloat() * 20 - 10) * (float)Math.PI / 180;
            float distance = sparkleRadius * (0.7f + sparkleRandom.nextFloat() * 0.3f);
            float sparkleX = centerX + (float)Math.cos(angle) * distance;
            float sparkleY = centerY + (float)Math.sin(angle) * distance;

            canvas.drawCircle(sparkleX, sparkleY, 2, sparklePaint);
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
            case UP: head.y -= 1; break;
            case DOWN: head.y += 1; break;
            case LEFT: head.x -= 1; break;
            case RIGHT: head.x += 1; break;
        }

        if (head.x < 0 || head.y < 0 || head.x >= numCells || head.y >= numCells || snakeContains(head)) {
            gameOver = true;
            playSound(loseSound, "lose");
            return;
        }

        snake.add(0, head);

        boolean foodEaten = false;
        boolean wrongFoodEaten = false;

        if (correctFood != null && head.equals(correctFood.position)) {
            score++;
            playSound(correctSound, "correct");
            spawnQuizAndFoods();
            updateTextViews();
            foodEaten = true;
        }

        if (!foodEaten) {
            for (FoodItem f : wrongFoods) {
                if (head.equals(f.position)) {
                    playSound(errorSound, "error");
                    wrongFoodEaten = true;
                    spawnQuizAndFoods();
                    updateTextViews();
                    break;
                }
            }
        }

        if (!foodEaten && !wrongFoodEaten && bonusFood != null && head.equals(bonusFood.position)) {
            score += bonusValue;
            playSound(bonusSound, "bonus");
            bonusFood = null;
            updateTextViews();
            foodEaten = true;
        }

        if (wrongFoodEaten) {
            if (snake.size() > 1) {
                snake.remove(snake.size() - 1);
            }
            if (snake.size() > 1) {
                snake.remove(snake.size() - 1);
            }
        } else if (foodEaten) {
            // La serpiente crece
        } else {
            snake.remove(snake.size() - 1);
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
                float minSwipeDistance = 50f;
                if (Math.abs(dx) < minSwipeDistance && Math.abs(dy) < minSwipeDistance) break;
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (gameOver) {
            if (keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_ENTER ||
                    keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                    keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                restartGame();
                return true;
            }
        } else {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_W:
                    pendingDirection = Direction.UP;
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_S:
                    pendingDirection = Direction.DOWN;
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_A:
                    pendingDirection = Direction.LEFT;
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_D:
                    pendingDirection = Direction.RIGHT;
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        requestFocus();
        if (thread != null && !thread.isRunning()) {
            thread.setRunning(true);
            try { thread.start(); }
            catch (IllegalThreadStateException e) {
                thread = new GameThread(getHolder(), this, gameSpeed);
                thread.setRunning(true);
                thread.start();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (thread != null) {
            thread.setRunning(false);
            try { thread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    public void pause() {
        if (thread != null) {
            thread.setRunning(false);
            try { thread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    public void resume() {
        if (thread != null && !thread.isRunning()) {
            thread = new GameThread(getHolder(), this, gameSpeed);
            thread.setRunning(true);
            try { thread.start(); }
            catch (IllegalThreadStateException e) {}
        }
    }
}
