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

    // Velocidad del juego más lenta para dar tiempo a los cálculos
    private final long gameSpeed = 1200; // Milisegundos entre movimientos (más lento para mejor gameplay)

    // TextViews para mostrar información
    private TextView questionTextView;
    private TextView scoreTextView;

    // SONIDOS
    private SoundPool soundPool;
    private int correctSound, errorSound, bonusSound, loseSound;

    // Sprites
    private Bitmap head_up, head_down, head_left, head_right;
    private Bitmap body_vertical, body_horizontal, body_topleft, body_topright, body_bottomleft, body_bottomright;
    private Bitmap tail_up, tail_down, tail_left, tail_right;
    private Bitmap apple, candy, sushi1, sushi2;
    private Bitmap[] foodBitmaps;

    // NUEVOS: sistema de foods correctos/incorrectos
    private class FoodItem {
        Point position;
        int value;        // Número mostrado
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
        thread = new GameThread(getHolder(), this, gameSpeed); // Pasar la velocidad al hilo

        // Hacer la vista focusable para recibir eventos de teclado
        setFocusable(true);
        setFocusableInTouchMode(true);

        try {
            soundPool = new SoundPool.Builder().setMaxStreams(4).build();
            correctSound = soundPool.load(context, R.raw.correct, 1);
            errorSound = soundPool.load(context, R.raw.error, 1);
            bonusSound = soundPool.load(context, R.raw.bonus, 1);
            loseSound = soundPool.load(context, R.raw.lose, 1);

            // Log para verificar que los sonidos se cargaron correctamente
            Log.d("GameView", "Sounds loaded - correct: " + correctSound + ", error: " + errorSound +
                    ", bonus: " + bonusSound + ", lose: " + loseSound);
        } catch (Exception e) {
            Log.e("GameView", "Error loading sounds: " + e.getMessage());
        }

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
        } catch (Exception e) {
            createFallbackBitmaps();
        }

        // Inicializar el juego DESPUÉS de cargar los bitmaps
        restartGame();
    }

    private void createFallbackBitmaps() {
        int size = 70;
        head_up = head_down = head_left = head_right = createColorBitmap(size, Color.GREEN);
        body_vertical = body_horizontal = body_topleft = body_topright = body_bottomleft = body_bottomright = createColorBitmap(size, Color.BLUE);
        tail_up = tail_down = tail_left = tail_right = createColorBitmap(size, Color.CYAN);
        apple = candy = sushi1 = sushi2 = createColorBitmap(size, Color.RED);
        foodBitmaps = new Bitmap[]{apple, candy, sushi1, sushi2};
    }

    private Bitmap createColorBitmap(int size, int color) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawRect(0, 0, size, size, paint);
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

        // Verificar que foodBitmaps no sea null
        if (foodBitmaps == null || foodBitmaps.length == 0) {
            Log.e("GameView", "foodBitmaps is null or empty, creating fallback bitmaps");
            createFallbackBitmaps();
        }

        // Generar posición comestible correcta
        correctFood = new FoodItem(getRandomFreePoint(), correctAnswer, true, foodBitmaps[Math.abs(correctAnswer) % foodBitmaps.length]);

        // Dos alimentos erróneos
        wrongFoods.clear();
        for (int i = 0; i < 2; i++) {
            int wrongVal;
            do {
                wrongVal = correctAnswer + (rand.nextInt(5) - 2);
            } while (wrongVal == correctAnswer);
            wrongFoods.add(new FoodItem(getRandomFreePoint(), wrongVal, false, foodBitmaps[Math.abs(wrongVal) % foodBitmaps.length]));
        }

        // Bonus aleatorio
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

        // Limpiar canvas con fondo negro
        canvas.drawColor(Color.BLACK);

        // Calcular dimensiones de la cuadrícula
        int gridRows = numCells, gridCols = numCells;
        int availableWidth = canvas.getWidth() - 32;
        int availableHeight = canvas.getHeight() - 16;
        int cellSizeDynamic = Math.min(availableWidth / gridCols, availableHeight / gridRows);
        int gridWidth = cellSizeDynamic * gridCols;
        int gridHeight = cellSizeDynamic * gridRows;
        int offsetX = (canvas.getWidth() - gridWidth) / 2;
        int offsetY = (canvas.getHeight() - gridHeight) / 2;

        // Dibujar fondo verde para cada celda de la cuadrícula
        Paint cellPaint = new Paint();
        cellPaint.setColor(Color.rgb(198, 255, 198));
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                int x = offsetX + col * cellSizeDynamic;
                int y = offsetY + row * cellSizeDynamic;
                canvas.drawRect(x, y, x + cellSizeDynamic, y + cellSizeDynamic, cellPaint);
            }
        }

        // --- DIBUJAR CUADRÍCULA ---
        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.BLACK); // Puedes elegir el color que quieras
        gridPaint.setStrokeWidth(2);     // Grosor de línea de 2px

// Líneas verticales (de arriba a abajo)
        for (int i = 0; i <= gridCols; i++) {
            int x = offsetX + i * cellSizeDynamic;
            canvas.drawLine(x, offsetY, x, offsetY + gridHeight, gridPaint);
        }

// Líneas horizontales (de izquierda a derecha)
        for (int i = 0; i <= gridRows; i++) {
            int y = offsetY + i * cellSizeDynamic;
            canvas.drawLine(offsetX, y, offsetX + gridWidth, y, gridPaint);
        }


        // DIBUJAR SERPIENTE
        if (snake != null && !snake.isEmpty()) {
            for (int i = 0; i < snake.size(); i++) {
                Point segment = snake.get(i);
                int x = offsetX + segment.x * cellSizeDynamic;
                int y = offsetY + segment.y * cellSizeDynamic;

                Bitmap segmentBitmap;

                if (i == 0) {
                    // Cabeza de la serpiente
                    switch (direction) {
                        case UP: segmentBitmap = head_up; break;
                        case DOWN: segmentBitmap = head_down; break;
                        case LEFT: segmentBitmap = head_left; break;
                        case RIGHT: segmentBitmap = head_right; break;
                        default: segmentBitmap = head_right; break;
                    }
                } else if (i == snake.size() - 1) {
                    // Cola de la serpiente
                    Point prev = snake.get(i - 1);
                    if (prev.x > segment.x) segmentBitmap = tail_right;
                    else if (prev.x < segment.x) segmentBitmap = tail_left;
                    else if (prev.y > segment.y) segmentBitmap = tail_down;
                    else segmentBitmap = tail_up;
                } else {
                    // Cuerpo de la serpiente
                    Point prev = snake.get(i - 1);
                    Point next = snake.get(i + 1);

                    if ((prev.x == next.x) || (prev.y == next.y)) {
                        // Segmento recto
                        if (prev.x == next.x) segmentBitmap = body_vertical;
                        else segmentBitmap = body_horizontal;
                    } else {
                        // Segmento curvo
                        if ((prev.x < segment.x && next.y < segment.y) || (next.x < segment.x && prev.y < segment.y)) {
                            segmentBitmap = body_topleft;
                        } else if ((prev.x > segment.x && next.y < segment.y) || (next.x > segment.x && prev.y < segment.y)) {
                            segmentBitmap = body_topright;
                        } else if ((prev.x < segment.x && next.y > segment.y) || (next.x < segment.x && prev.y > segment.y)) {
                            segmentBitmap = body_bottomleft;
                        } else {
                            segmentBitmap = body_bottomright;
                        }
                    }
                }

                canvas.drawBitmap(Bitmap.createScaledBitmap(segmentBitmap, cellSizeDynamic, cellSizeDynamic, false), x, y, null);
            }
        }

        // DIBUJAR ALIMENTOS (quiz foods)
        drawFood(canvas, correctFood, offsetX, offsetY, cellSizeDynamic);
        for (FoodItem wf : wrongFoods) drawFood(canvas, wf, offsetX, offsetY, cellSizeDynamic);
        if (bonusFood != null) drawFood(canvas, bonusFood, offsetX, offsetY, cellSizeDynamic);

        // GAME OVER overlay
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
            canvas.drawText("Tap to Restart", getWidth() / 2, getHeight() / 2 + 50, overPaint);
        }
    }

    private void drawFood(Canvas canvas, FoodItem food, int offsetX, int offsetY, int cellSize) {
        if (food == null) return;
        int x = offsetX + food.position.x * cellSize;
        int y = offsetY + food.position.y * cellSize;
        canvas.drawBitmap(Bitmap.createScaledBitmap(food.bitmap, cellSize, cellSize, false), x, y, null);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(cellSize * 0.5f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setShadowLayer(2, 1, 1, Color.BLACK);
        canvas.drawText(String.valueOf(food.value), x + cellSize / 2, y + cellSize / 2 + paint.getTextSize() / 3, paint);
    }

    public void update() {
        if (gameOver) return;

        // Aplicar dirección pendiente si es válida
        if (pendingDirection != null && !direction.isOpposite(pendingDirection)) {
            direction = pendingDirection;
            pendingDirection = null;
        }

        // Calcular nueva posición de la cabeza
        Point head = new Point(snake.get(0));
        switch (direction) {
            case UP: head.y -= 1; break;
            case DOWN: head.y += 1; break;
            case LEFT: head.x -= 1; break;
            case RIGHT: head.x += 1; break;
        }

        // Verificar colisiones con paredes o consigo misma
        if (head.x < 0 || head.y < 0 || head.x >= numCells || head.y >= numCells || snakeContains(head)) {
            gameOver = true;
            if (soundPool != null) soundPool.play(loseSound, 1.0f, 1.0f, 0, 0, 1.0f);
            return;
        }

        // Añadir nueva cabeza
        snake.add(0, head);

        // COLISIONES CON ALIMENTOS
        boolean foodEaten = false;
        boolean wrongFoodEaten = false;

        // Verificar comida correcta
        if (correctFood != null && head.equals(correctFood.position)) {
            score++;
            if (soundPool != null) soundPool.play(correctSound, 1.0f, 1.0f, 0, 0, 1.0f);
            spawnQuizAndFoods();
            updateTextViews();
            foodEaten = true; // La serpiente crece
        }

        // Verificar colisión con comida incorrecta
        if (!foodEaten) { // Solo verificar si no comió la correcta
            for (FoodItem f : wrongFoods) {
                if (head.equals(f.position)) {
                    // Reproducir sonido de error específicamente con logging
                    Log.d("GameView", "Wrong food eaten! Playing error sound");
                    if (soundPool != null && errorSound != 0) {
                        int streamId = soundPool.play(errorSound, 1.0f, 1.0f, 1, 0, 1.0f);
                        Log.d("GameView", "Error sound played with streamId: " + streamId);
                    } else {
                        Log.e("GameView", "Cannot play error sound - soundPool: " + soundPool + ", errorSound: " + errorSound);
                    }

                    wrongFoodEaten = true;
                    spawnQuizAndFoods(); // Generar nueva pregunta después del error
                    updateTextViews();
                    break;
                }
            }
        }

        // Verificar comida bonus
        if (!foodEaten && !wrongFoodEaten && bonusFood != null && head.equals(bonusFood.position)) {
            score += bonusValue;
            if (soundPool != null) soundPool.play(bonusSound, 1.0f, 1.0f, 0, 0, 1.0f);
            bonusFood = null;
            updateTextViews();
            foodEaten = true; // La serpiente crece
        }

        // Manejar el tamaño de la serpiente según lo que comió
        if (wrongFoodEaten) {
            // Comió alimento incorrecto: la serpiente se hace más pequeña
            // Quitar la cola normalmente Y un segmento adicional como castigo
            if (snake.size() > 1) {
                snake.remove(snake.size() - 1); // Quitar cola normal
            }
            if (snake.size() > 1) {
                snake.remove(snake.size() - 1); // Quitar segmento adicional como castigo
                Log.d("GameView", "Snake shrunk due to wrong food. New size: " + snake.size());
            }
        } else if (foodEaten) {
            // Comió alimento correcto o bonus: la serpiente crece (no quitar cola)
            // No hacer nada, la serpiente mantiene todos sus segmentos
        } else {
            // Movimiento normal sin comer nada: quitar la cola
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

                // Añadir umbral mínimo para detectar swipes válidos
                float minSwipeDistance = 50f;

                if (Math.abs(dx) < minSwipeDistance && Math.abs(dy) < minSwipeDistance) {
                    // Movimiento muy pequeño, ignorar
                    break;
                }

                // Determinar dirección basada en el movimiento más grande
                if (Math.abs(dx) > Math.abs(dy)) {
                    // Movimiento horizontal
                    pendingDirection = dx > 0 ? Direction.RIGHT : Direction.LEFT;
                } else {
                    // Movimiento vertical
                    pendingDirection = dy > 0 ? Direction.DOWN : Direction.UP;
                }

                // Debug log para verificar detección de direcciones
                Log.d("GameView", "Swipe detected: dx=" + dx + ", dy=" + dy + ", direction=" + pendingDirection);
                break;
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (gameOver) {
            // Reiniciar con cualquier tecla cuando está en game over
            if (keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_ENTER ||
                    keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                    keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                restartGame();
                return true;
            }
        } else {
            // Control de dirección con teclas de flecha
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_W:
                    pendingDirection = Direction.UP;
                    Log.d("GameView", "Key UP pressed");
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_S:
                    pendingDirection = Direction.DOWN;
                    Log.d("GameView", "Key DOWN pressed");
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_A:
                    pendingDirection = Direction.LEFT;
                    Log.d("GameView", "Key LEFT pressed");
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_D:
                    pendingDirection = Direction.RIGHT;
                    Log.d("GameView", "Key RIGHT pressed");
                    return true;
                case KeyEvent.KEYCODE_SPACE:
                case KeyEvent.KEYCODE_ENTER:
                    // Pausa/resume (opcional)
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Solicitar foco para recibir eventos de teclado
        requestFocus();

        if (thread != null && !thread.isRunning()) {
            thread.setRunning(true);
            try { thread.start(); }
            catch (IllegalThreadStateException e) {
                thread = new GameThread(getHolder(), this, gameSpeed); // Pasar velocidad
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
    }

    public void pause() {
        if (thread != null) {
            thread.setRunning(false);
            try { thread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    public void resume() {
        if (thread != null && !thread.isRunning()) {
            thread = new GameThread(getHolder(), this, gameSpeed); // Pasar velocidad
            thread.setRunning(true);
            try { thread.start(); }
            catch (IllegalThreadStateException e) {}
        }
    }
}