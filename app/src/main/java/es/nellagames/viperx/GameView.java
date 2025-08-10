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

    // SONIDOS - Añadido flag para verificar que los sonidos están listos
    private SoundPool soundPool;
    private int correctSound, errorSound, bonusSound, loseSound;
    private boolean soundsLoaded = false;

    // Sprites
    private Bitmap head_up, head_down, head_left, head_right;
    private Bitmap body_vertical, body_horizontal, body_topleft, body_topright, body_bottomleft, body_bottomright;
    private Bitmap tail_up, tail_down, tail_left, tail_right;
    private Bitmap apple, candy, sushi1, sushi2;
    private Bitmap[] foodBitmaps;

    // NUEVO: Imagen de fondo de la cuadrícula
    private Bitmap gridBackground;

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

    // ⭐ CAMBIO: estrellas dinámicas
    private class Star {
        float x, y;
        float alpha;
        boolean increasing;
    }
    private List<Star> stars = new ArrayList<>();
    private final int numStars = 180;

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

        initializeSounds(context);
        initializeBitmaps();

        // Inicializar el juego DESPUÉS de cargar los bitmaps
        restartGame();

        // ⭐ CAMBIO: inicializar estrellas
        Random rand = new Random();
        for (int i = 0; i < numStars; i++) {
            Star s = new Star();
            s.x = rand.nextFloat() * 1080; // Ajusta si quieres otra resolución base
            s.y = rand.nextFloat() * 1920;
            s.alpha = rand.nextFloat();
            s.increasing = rand.nextBoolean();
            stars.add(s);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Inicializar estrellas cuando la vista ya tiene dimensiones válidas
        // CORREGIDO: Distribuir estrellas por toda la pantalla
        stars = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < numStars; i++) {
            Star s = new Star();
            s.x = random.nextFloat() * w;  // Usar ancho real de la vista
            s.y = random.nextFloat() * h;  // Usar alto real de la vista
            s.alpha = 0.3f + random.nextFloat() * 0.7f; // Alpha entre 0.3 y 1.0
            s.increasing = random.nextBoolean();
            stars.add(s);
        }
    }


    private void initializeSounds(Context context) {
        try {
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(5)  // Aumentado para más streams simultáneos
                    .build();

            // Cargar sonidos con verificación
            correctSound = soundPool.load(context, R.raw.correct, 1);
            errorSound = soundPool.load(context, R.raw.error, 1);
            bonusSound = soundPool.load(context, R.raw.bonus, 1);
            loseSound = soundPool.load(context, R.raw.lose, 1);

            // Listener para verificar cuando los sonidos están cargados
            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    if (status == 0) { // 0 means success
                        soundsLoaded = true;
                        Log.d("GameView", "Sound loaded successfully - ID: " + sampleId);
                    } else {
                        Log.e("GameView", "Failed to load sound - ID: " + sampleId + ", Status: " + status);
                    }
                }
            });

            Log.d("GameView", "Sounds initialized - correct: " + correctSound + ", error: " + errorSound +
                    ", bonus: " + bonusSound + ", lose: " + loseSound);
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

            // NUEVO: Cargar imagen de fondo de cuadrícula
            gridBackground = BitmapFactory.decodeResource(getResources(), R.drawable.cuadricula);

            foodBitmaps = new Bitmap[]{apple, candy, sushi1, sushi2};
            Log.d("GameView", "Bitmaps loaded successfully");
        } catch (Exception e) {
            Log.e("GameView", "Error loading bitmaps: " + e.getMessage());
            createFallbackBitmaps();
        }
    }

    // Método mejorado para reproducir sonidos
    private void playSound(int soundId, String soundName) {
        if (soundPool != null && soundId > 0) {
            try {
                float volume = 1.0f;
                int priority = 1;
                int loop = 0;
                float rate = 1.0f;

                int streamId = soundPool.play(soundId, volume, volume, priority, loop, rate);
                Log.d("GameView", soundName + " sound played - StreamID: " + streamId + ", SoundID: " + soundId);

                if (streamId == 0) {
                    Log.e("GameView", "Failed to play " + soundName + " sound - StreamID is 0");
                }
            } catch (Exception e) {
                Log.e("GameView", "Exception playing " + soundName + " sound: " + e.getMessage());
            }
        } else {
            Log.e("GameView", "Cannot play " + soundName + " - soundPool: " + soundPool + ", soundId: " + soundId);
        }
    }

    private void createFallbackBitmaps() {
        int size = 70;
        head_up = head_down = head_left = head_right = createColorBitmap(size, Color.GREEN);
        body_vertical = body_horizontal = body_topleft = body_topright = body_bottomleft = body_bottomright = createColorBitmap(size, Color.BLUE);
        tail_up = tail_down = tail_left = tail_right = createColorBitmap(size, Color.CYAN);
        apple = candy = sushi1 = sushi2 = createColorBitmap(size, Color.RED);
        foodBitmaps = new Bitmap[]{apple, candy, sushi1, sushi2};

        // NUEVO: Crear imagen de cuadrícula de respaldo
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

    // NUEVO: Crear imagen de cuadrícula de respaldo
    private Bitmap createGridFallback(int size, int backgroundColor, int lineColor) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Fondo
        Paint bgPaint = new Paint();
        bgPaint.setColor(backgroundColor);
        canvas.drawRect(0, 0, size, size, bgPaint);

        // Líneas de cuadrícula
        Paint gridPaint = new Paint();
        gridPaint.setColor(lineColor);
        gridPaint.setStrokeWidth(2);

        int cellSize = size / numCells;

        // Líneas verticales
        for (int i = 0; i <= numCells; i++) {
            int x = i * cellSize;
            canvas.drawLine(x, 0, x, size, gridPaint);
        }

        // Líneas horizontales
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

        // MEJORADO: Cambiar fondo negro por un azul oscuro espacial más amigable
        canvas.drawColor(Color.rgb(15, 25, 45)); // Azul oscuro espacial

        // ⭐ Dibujar estrellas en todo el canvas con mejor variedad
        Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (Star s : stars) {
            // Crear variaciones en el color de las estrellas
            int starColor;
            if (s.alpha > 0.8f) {
                starColor = Color.rgb(255, 255, 200); // Estrellas brillantes - amarillo claro
            } else if (s.alpha > 0.6f) {
                starColor = Color.rgb(220, 220, 255); // Estrellas medias - azul claro
            } else {
                starColor = Color.rgb(255, 255, 255); // Estrellas normales - blanco
            }

            starPaint.setColor(starColor);
            starPaint.setAlpha((int) (s.alpha * 255));

            // Variar el tamaño de las estrellas según su brillo
            float starSize = s.alpha > 0.7f ? 3f : 2f;
            canvas.drawCircle(s.x, s.y, starSize, starPaint);
        }

        // Calcular dimensiones de la cuadrícula
        int gridRows = numCells, gridCols = numCells;
        int availableWidth = canvas.getWidth() - 32;
        int availableHeight = canvas.getHeight() - 16;
        int cellSizeDynamic = Math.min(availableWidth / gridCols, availableHeight / gridRows);
        int gridWidth = cellSizeDynamic * gridCols;
        int gridHeight = cellSizeDynamic * gridRows;
        int offsetX = (canvas.getWidth() - gridWidth) / 2;
        int offsetY = (canvas.getHeight() - gridHeight) / 2;

        // NUEVO: Dibujar imagen de fondo de cuadrícula en lugar del fondo verde y líneas
        if (gridBackground != null) {
            Bitmap scaledGrid = Bitmap.createScaledBitmap(gridBackground, gridWidth, gridHeight, false);
            canvas.drawBitmap(scaledGrid, offsetX, offsetY, null);
        } else {
            // Fallback: Dibujar fondo verde para cada celda de la cuadrícula
            Paint cellPaint = new Paint();
            cellPaint.setColor(Color.rgb(198, 255, 198));
            for (int row = 0; row < gridRows; row++) {
                for (int col = 0; col < gridCols; col++) {
                    int x = offsetX + col * cellSizeDynamic;
                    int y = offsetY + row * cellSizeDynamic;
                    canvas.drawRect(x, y, x + cellSizeDynamic, y + cellSizeDynamic, cellPaint);
                }
            }

            // Fallback: Dibujar líneas de cuadrícula
            Paint gridPaint = new Paint();
            gridPaint.setColor(Color.BLACK);
            gridPaint.setStrokeWidth(2);

            // Líneas verticales
            for (int i = 0; i <= gridCols; i++) {
                int x = offsetX + i * cellSizeDynamic;
                canvas.drawLine(x, offsetY, x, offsetY + gridHeight, gridPaint);
            }

            // Líneas horizontales
            for (int i = 0; i <= gridRows; i++) {
                int y = offsetY + i * cellSizeDynamic;
                canvas.drawLine(offsetX, y, offsetX + gridWidth, y, gridPaint);
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

        // GAME OVER overlay con diseño mejorado y más grande
        if (gameOver) {
            // Overlay más elegante con gradiente
            Paint overlayPaint = new Paint();
            overlayPaint.setColor(Color.argb(220, 10, 15, 30)); // Más oscuro y opaco
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

            // Añadir efecto de resplandor alrededor del texto
            Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            glowPaint.setColor(Color.argb(100, 255, 100, 100)); // Resplandor rojizo
            glowPaint.setTextSize(90f);
            glowPaint.setFakeBoldText(true);
            glowPaint.setTextAlign(Paint.Align.CENTER);
            glowPaint.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL));

            // Dibujar resplandor del texto principal
            canvas.drawText("GAME OVER", getWidth() / 2, getHeight() / 2 - 80, glowPaint);

            // Texto principal "GAME OVER" más grande y estilizado
            Paint gameOverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            gameOverPaint.setColor(Color.rgb(255, 80, 80)); // Rojo más vibrante
            gameOverPaint.setTextSize(85f); // Más grande
            gameOverPaint.setFakeBoldText(true);
            gameOverPaint.setTextAlign(Paint.Align.CENTER);
            gameOverPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            // Sombra más pronunciada
            gameOverPaint.setShadowLayer(8, 4, 4, Color.argb(200, 0, 0, 0));

            // Añadir borde al texto para mayor contraste
            Paint borderPaint = new Paint(gameOverPaint);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(6f);
            borderPaint.setColor(Color.rgb(120, 20, 20)); // Borde rojo oscuro

            // Dibujar borde del texto
            canvas.drawText("GAME OVER", getWidth() / 2, getHeight() / 2 - 80, borderPaint);
            // Dibujar texto principal
            canvas.drawText("GAME OVER", getWidth() / 2, getHeight() / 2 - 80, gameOverPaint);

            // Panel de información del score con fondo
            Paint scorePanelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            scorePanelPaint.setColor(Color.argb(180, 20, 30, 50)); // Fondo semi-transparente
            float panelWidth = 300f;
            float panelHeight = 80f;
            float panelX = (getWidth() - panelWidth) / 2;
            float panelY = getHeight() / 2 - 20;

            // Dibujar panel con esquinas redondeadas
            canvas.drawRoundRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 20f, 20f, scorePanelPaint);

            // Borde del panel
            Paint panelBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            panelBorderPaint.setStyle(Paint.Style.STROKE);
            panelBorderPaint.setStrokeWidth(3f);
            panelBorderPaint.setColor(Color.rgb(100, 150, 255));
            canvas.drawRoundRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 20f, 20f, panelBorderPaint);

            // Texto del score final
            Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            scorePaint.setColor(Color.rgb(255, 255, 150)); // Amarillo dorado
            scorePaint.setTextSize(36f);
            scorePaint.setTextAlign(Paint.Align.CENTER);
            scorePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            scorePaint.setShadowLayer(4, 2, 2, Color.BLACK);

            canvas.drawText("Final Score: " + score, getWidth() / 2, getHeight() / 2 + 25, scorePaint);

            // Texto de reinicio más atractivo
            Paint restartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            restartPaint.setColor(Color.rgb(150, 255, 150)); // Verde claro
            restartPaint.setTextSize(32f);
            restartPaint.setTextAlign(Paint.Align.CENTER);
            restartPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            restartPaint.setShadowLayer(4, 2, 2, Color.BLACK);

            // Efecto pulsante para el texto de reinicio
            long time = System.currentTimeMillis();
            float pulse = (float) (0.8f + 0.2f * Math.sin(time * 0.005f)); // Pulsación suave
            restartPaint.setAlpha((int) (255 * pulse));

            canvas.drawText("🎮 TAP TO RESTART 🎮", getWidth() / 2, getHeight() / 2 + 120, restartPaint);

            // Añadir decoraciones laterales (líneas estilizadas)
            Paint decorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            decorPaint.setColor(Color.rgb(100, 150, 255));
            decorPaint.setStrokeWidth(4f);
            decorPaint.setPathEffect(new DashPathEffect(new float[]{10, 5}, 0));

            // Líneas decorativas horizontales
            canvas.drawLine(50, getHeight() / 2 - 180, getWidth() - 50, getHeight() / 2 - 180, decorPaint);
            canvas.drawLine(50, getHeight() / 2 + 210, getWidth() - 50, getHeight() / 2 + 210, decorPaint);


            // Añadir pequeñas estrellas decorativas alrededor
            Paint starDecorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            starDecorPaint.setColor(Color.rgb(255, 215, 0)); // Oro
            starDecorPaint.setTextSize(24f);
            starDecorPaint.setTextAlign(Paint.Align.CENTER);

            // Estrellas en las esquinas del área de Game Over
            canvas.drawText("⭐", getWidth() / 2 - 150, getHeight() / 2 - 120, starDecorPaint);
            canvas.drawText("⭐", getWidth() / 2 + 150, getHeight() / 2 - 120, starDecorPaint);
            canvas.drawText("⭐", getWidth() / 2 - 120, getHeight() / 2 + 160, starDecorPaint);
            canvas.drawText("⭐", getWidth() / 2 + 120, getHeight() / 2 + 160, starDecorPaint);
        }
    }

    private void drawFood(Canvas canvas, FoodItem food, int offsetX, int offsetY, int cellSize) {
        if (food == null) return;

        int x = offsetX + food.position.x * cellSize;
        int y = offsetY + food.position.y * cellSize;

        // Add subtle drop shadow for food items
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.argb(60, 0, 0, 0));
        shadowPaint.setMaskFilter(new BlurMaskFilter(cellSize * 0.05f, BlurMaskFilter.Blur.NORMAL));

        // Make food much larger with minimal padding
        int foodPadding = cellSize / 50; // Even less padding for maximum size
        int foodSize = cellSize - (foodPadding * 2);

        // Draw shadow slightly offset
        int shadowOffset = cellSize / 40;
        canvas.drawRoundRect(x + foodPadding + shadowOffset, y + foodPadding + shadowOffset,
                x + foodPadding + foodSize + shadowOffset, y + foodPadding + foodSize + shadowOffset,
                cellSize * 0.1f, cellSize * 0.1f, shadowPaint);

        // Scale and draw the food bitmap
        Bitmap scaledFood = Bitmap.createScaledBitmap(food.bitmap, foodSize, foodSize, true);
        canvas.drawBitmap(scaledFood, x + foodPadding, y + foodPadding, null);

        // Add subtle overlay for better number contrast
        Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setColor(Color.argb(30, 0, 0, 0));
        canvas.drawRoundRect(x + foodPadding, y + foodPadding, x + foodPadding + foodSize, y + foodPadding + foodSize,
                cellSize * 0.05f, cellSize * 0.05f, overlayPaint);

        // Enhanced number styling with better positioning and size
        Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Create semi-transparent background for better readability over food
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (food == bonusFood) {
            // Bonus food - semi-transparent gold
            bgPaint.setColor(Color.argb(200, 255, 193, 7)); // More transparent for overlay
        } else {
            // Quiz foods - semi-transparent dark background
            bgPaint.setColor(Color.argb(180, 33, 33, 33)); // Semi-transparent for overlay effect
        }

        // Center the number background within the food item
        float bgWidth = cellSize * 0.4f;
        float bgHeight = cellSize * 0.3f;
        float bgX = x + (cellSize - bgWidth) / 2;  // Centered horizontally
        float bgY = y + (cellSize - bgHeight) / 2; // Centered vertically
        float cornerRadius = cellSize * 0.08f;

        // Draw background with shadow
        Paint bgShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgShadowPaint.setColor(Color.argb(80, 0, 0, 0));
        bgShadowPaint.setMaskFilter(new BlurMaskFilter(cellSize * 0.02f, BlurMaskFilter.Blur.NORMAL));
        canvas.drawRoundRect(bgX + 2, bgY + 2, bgX + bgWidth + 2, bgY + bgHeight + 2,
                cornerRadius, cornerRadius, bgShadowPaint);

        // Draw main background
        canvas.drawRoundRect(bgX, bgY, bgX + bgWidth, bgY + bgHeight,
                cornerRadius, cornerRadius, bgPaint);

        // Add subtle border
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.argb(100, 255, 255, 255));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(cellSize * 0.015f);
        canvas.drawRoundRect(bgX, bgY, bgX + bgWidth, bgY + bgHeight,
                cornerRadius, cornerRadius, borderPaint);

        // Configure number text with better contrast for overlay
        numberPaint.setColor(Color.WHITE);
        numberPaint.setTextSize(cellSize * 0.32f); // Slightly larger for better visibility
        numberPaint.setTextAlign(Paint.Align.CENTER);
        numberPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Stronger shadow for better readability over food images
        numberPaint.setShadowLayer(6, 0, 3, Color.argb(200, 0, 0, 0));

        // Draw the number centered in background
        String numberText = String.valueOf(food.value);
        float textCenterX = bgX + bgWidth / 2;
        float textCenterY = bgY + bgHeight / 2;

        Paint.FontMetrics fm = numberPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        float textY = textCenterY + (textHeight / 2) - fm.descent;

        canvas.drawText(numberText, textCenterX, textY, numberPaint);

        // Enhanced bonus indicator
        if (food == bonusFood) {
            Paint bonusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bonusPaint.setColor(Color.argb(255, 255, 215, 0));
            bonusPaint.setTextSize(cellSize * 0.15f);
            bonusPaint.setTextAlign(Paint.Align.CENTER);
            bonusPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            bonusPaint.setShadowLayer(3, 1, 1, Color.argb(200, 0, 0, 0));

            // Draw "BONUS" with background
            String bonusText = "BONUS";
            float bonusTextWidth = bonusPaint.measureText(bonusText);
            float bonusBgX = x + (cellSize - bonusTextWidth) / 2 - cellSize * 0.05f;
            float bonusBgY = y + cellSize - cellSize * 0.25f;

            Paint bonusBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bonusBgPaint.setColor(Color.argb(200, 0, 0, 0));
            canvas.drawRoundRect(bonusBgX, bonusBgY, bonusBgX + bonusTextWidth + cellSize * 0.1f,
                    bonusBgY + cellSize * 0.2f, cellSize * 0.03f, cellSize * 0.03f, bonusBgPaint);

            canvas.drawText(bonusText, x + cellSize / 2, y + cellSize - cellSize * 0.08f, bonusPaint);
        }

        // Add modern highlight effect on food edges
        Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(Color.argb(40, 255, 255, 255));
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(cellSize * 0.02f);
        canvas.drawRoundRect(x + foodPadding, y + foodPadding, x + foodPadding + foodSize, y + foodPadding + foodSize,
                cellSize * 0.05f, cellSize * 0.05f, highlightPaint);
    }

    // ⭐ MEJORADO: actualizar estrellas con mejor animación
    public void update() {
        if (gameOver) return;

        // ⭐ Actualizar alpha de las estrellas con velocidad variable
        for (Star s : stars) {
            float speed = 0.015f + (s.alpha * 0.01f); // Estrellas más brillantes parpadean más rápido

            if (s.increasing) {
                s.alpha += speed;
                if (s.alpha >= 1f) {
                    s.alpha = 1f;
                    s.increasing = false;
                }
            } else {
                s.alpha -= speed;
                if (s.alpha <= 0.3f) {
                    s.alpha = 0.3f;
                    s.increasing = true;
                }
            }
        }

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
            playSound(loseSound, "lose");
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
            playSound(correctSound, "correct");
            spawnQuizAndFoods();
            updateTextViews();
            foodEaten = true;
        }

        // Verificar comida incorrecta
        if (!foodEaten) {
            for (FoodItem f : wrongFoods) {
                if (head.equals(f.position)) {
                    Log.d("GameView", "Wrong food eaten! Value: " + f.value + ", Correct answer: " + correctAnswer);
                    playSound(errorSound, "error");
                    wrongFoodEaten = true;
                    spawnQuizAndFoods();
                    updateTextViews();
                    break;
                }
            }
        }

        // Verificar comida bonus
        if (!foodEaten && !wrongFoodEaten && bonusFood != null && head.equals(bonusFood.position)) {
            score += bonusValue;
            playSound(bonusSound, "bonus");
            bonusFood = null;
            updateTextViews();
            foodEaten = true;
        }

        // Manejar tamaño de la serpiente
        if (wrongFoodEaten) {
            if (snake.size() > 1) snake.remove(snake.size() - 1);
            if (snake.size() > 1) {
                snake.remove(snake.size() - 1);
                Log.d("GameView", "Snake shrunk due to wrong food. New size: " + snake.size());
            }
        } else if (!foodEaten) {
            snake.remove(snake.size() - 1); // Movimiento normal
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

        // Limpiar recursos de sonido
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
            thread = new GameThread(getHolder(), this, gameSpeed); // Pasar velocidad
            thread.setRunning(true);
            try { thread.start(); }
            catch (IllegalThreadStateException e) {}
        }
    }
}