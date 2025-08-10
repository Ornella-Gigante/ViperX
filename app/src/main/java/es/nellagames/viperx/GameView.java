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

    private TextView questionTextView;
    private TextView scoreTextView;

    private SoundPool soundPool;
    private int correctSound, errorSound, bonusSound, loseSound;
    private boolean soundsLoaded = false;

    private Bitmap head_up, head_down, head_left, head_right;
    private Bitmap body_vertical, body_horizontal, body_topleft, body_topright, body_bottomleft, body_bottomright;
    private Bitmap tail_up, tail_down, tail_left, tail_right;
    private Bitmap apple, candy, sushi1, sushi2;
    private Bitmap[] foodBitmaps;
    private Bitmap gridBackground;

    // NUEVO: Interfaz para comunicar con la Activity
    public interface GameEventListener {
        void onBackToMenuPressed();
    }
    private GameEventListener gameEventListener;

    // NUEVO: Rectángulo del botón Back to Menu
    private RectF backToMenuButton = new RectF();

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
        thread = new GameThread(getHolder(), this, gameSpeed);
        setFocusable(true);
        setFocusableInTouchMode(true);
        initializeSounds(context);
        initializeBitmaps();
        restartGame();

        Random rand = new Random();
        for (int i = 0; i < numStars; i++) {
            Star s = new Star();
            s.x = rand.nextFloat() * 1080;
            s.y = rand.nextFloat() * 1920;
            s.alpha = rand.nextFloat();
            s.increasing = rand.nextBoolean();
            stars.add(s);
        }
    }

    // NUEVO: Método para establecer el listener
    public void setGameEventListener(GameEventListener listener) {
        this.gameEventListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        stars.clear();
        Random rand = new Random();
        for (int i = 0; i < numStars; i++) {
            Star s = new Star();
            s.x = rand.nextFloat() * w;
            s.y = rand.nextFloat() * h;
            s.alpha = 0.3f + rand.nextFloat() * 0.7f;
            s.increasing = rand.nextBoolean();
            stars.add(s);
        }
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
        this.questionTextView = null;
        this.scoreTextView = null;
    }

    private void updateTextViews() {
        // Método vacío - no se usan TextViews externos
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
        boolean occupied;
        do {
            p = new Point(rand.nextInt(numCells), rand.nextInt(numCells));
            occupied = snakeContains(p);
            if (!occupied && correctFood != null && p.equals(correctFood.position)) occupied = true;
            if (!occupied) {
                for (FoodItem food : wrongFoods) {
                    if (food.position.equals(p)) {
                        occupied = true;
                        break;
                    }
                }
            }
            if (!occupied && bonusFood != null && p.equals(bonusFood.position)) occupied = true;
        } while (occupied);
        return p;
    }

    private boolean snakeContains(Point p) {
        for (Point s : snake) if (s.equals(p)) return true;
        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        // Fondo azul oscuro espacial
        canvas.drawColor(Color.rgb(15, 25, 45));

        // Dibujar estrellas con variedad
        Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (Star s : stars) {
            int starColor;
            if (s.alpha > 0.8f) {
                starColor = Color.rgb(255, 255, 200);
            } else if (s.alpha > 0.6f) {
                starColor = Color.rgb(220, 220, 255);
            } else {
                starColor = Color.rgb(255, 255, 255);
            }
            starPaint.setColor(starColor);
            starPaint.setAlpha((int) (s.alpha * 255));
            float starSize = s.alpha > 0.7f ? 3f : 2f;
            canvas.drawCircle(s.x, s.y, starSize, starPaint);
        }

        // MODIFICADO: Área de pregunta MÁS ABAJO, CENTRADA y MÁS GRANDE
        drawQuestionArea(canvas);

        // Calcular dimensiones de cuadrícula (ajustada para el cuadro más abajo)
        int availableWidth = canvas.getWidth() - 32;
        int availableHeight = canvas.getHeight() - 380; // Más espacio para el cuadro más grande
        int cellSizeDynamic = Math.min(availableWidth / numCells, availableHeight / numCells);
        int gridWidth = cellSizeDynamic * numCells;
        int gridHeight = cellSizeDynamic * numCells;
        int offsetX = (canvas.getWidth() - gridWidth) / 2;
        int offsetY = ((canvas.getHeight() - gridHeight) / 2) + 190; // Mover más hacia abajo

        if (gridBackground != null) {
            Bitmap scaledGrid = Bitmap.createScaledBitmap(gridBackground, gridWidth, gridHeight, false);
            canvas.drawBitmap(scaledGrid, offsetX, offsetY, null);
        } else {
            Paint cellPaint = new Paint();
            cellPaint.setColor(Color.rgb(198, 255, 198));
            for (int row = 0; row < numCells; row++) {
                for (int col = 0; col < numCells; col++) {
                    int x = offsetX + col * cellSizeDynamic;
                    int y = offsetY + row * cellSizeDynamic;
                    canvas.drawRect(x, y, x + cellSizeDynamic, y + cellSizeDynamic, cellPaint);
                }
            }
        }

        // Dibujar serpiente
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

        // Dibujar alimentos
        drawFood(canvas, correctFood, offsetX, offsetY, cellSizeDynamic);
        for (FoodItem wf : wrongFoods) drawFood(canvas, wf, offsetX, offsetY, cellSizeDynamic);
        if (bonusFood != null) drawFood(canvas, bonusFood, offsetX, offsetY, cellSizeDynamic);

        // GAME OVER con botón de Back to Menu
        if (gameOver) {
            Paint overlayPaint = new Paint();
            overlayPaint.setColor(Color.argb(220, 10, 15, 30));
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

            Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            glowPaint.setColor(Color.argb(100, 255, 100, 100));
            glowPaint.setTextSize(90f);
            glowPaint.setFakeBoldText(true);
            glowPaint.setTextAlign(Paint.Align.CENTER);
            glowPaint.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL));
            canvas.drawText("GAME OVER", getWidth() / 2, getHeight() / 2 - 80, glowPaint);

            Paint gameOverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            gameOverPaint.setColor(Color.rgb(255, 80, 80));
            gameOverPaint.setTextSize(85f);
            gameOverPaint.setFakeBoldText(true);
            gameOverPaint.setTextAlign(Paint.Align.CENTER);
            gameOverPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            gameOverPaint.setShadowLayer(8, 4, 4, Color.argb(200, 0, 0, 0));

            Paint borderPaint = new Paint(gameOverPaint);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(6f);
            borderPaint.setColor(Color.rgb(120, 20, 20));

            canvas.drawText("GAME OVER", getWidth() / 2, getHeight() / 2 - 80, borderPaint);
            canvas.drawText("GAME OVER", getWidth() / 2, getHeight() / 2 - 80, gameOverPaint);

            Paint scorePanelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            scorePanelPaint.setColor(Color.argb(180, 20, 30, 50));
            float panelWidth = 300f;
            float panelHeight = 80f;
            float panelX = (getWidth() - panelWidth) / 2;
            float panelY = getHeight() / 2 - 20;

            canvas.drawRoundRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 20f, 20f, scorePanelPaint);

            Paint panelBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            panelBorderPaint.setStyle(Paint.Style.STROKE);
            panelBorderPaint.setStrokeWidth(3f);
            panelBorderPaint.setColor(Color.rgb(100, 150, 255));
            canvas.drawRoundRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 20f, 20f, panelBorderPaint);

            Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            scorePaint.setColor(Color.rgb(255, 255, 150));
            scorePaint.setTextSize(36f);
            scorePaint.setTextAlign(Paint.Align.CENTER);
            scorePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            scorePaint.setShadowLayer(4, 2, 2, Color.BLACK);
            canvas.drawText("Final Score: " + score, getWidth() / 2, getHeight() / 2 + 25, scorePaint);

            Paint restartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            restartPaint.setColor(Color.rgb(150, 255, 150));
            restartPaint.setTextSize(32f);
            restartPaint.setTextAlign(Paint.Align.CENTER);
            restartPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            restartPaint.setShadowLayer(4, 2, 2, Color.BLACK);

            long time = System.currentTimeMillis();
            float pulse = (float) (0.8f + 0.2f * Math.sin(time * 0.005f));
            restartPaint.setAlpha((int) (255 * pulse));
            canvas.drawText("🎮 TAP TO RESTART 🎮", getWidth() / 2, getHeight() / 2 + 120, restartPaint);

            // NUEVO: Botón Back to Menu
            drawBackToMenuButton(canvas);

            Paint decorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            decorPaint.setColor(Color.rgb(100, 150, 255));
            decorPaint.setStrokeWidth(4f);
            decorPaint.setPathEffect(new DashPathEffect(new float[]{10, 5}, 0));
            canvas.drawLine(50, getHeight() / 2 - 180, getWidth() - 50, getHeight() / 2 - 180, decorPaint);
            canvas.drawLine(50, getHeight() / 2 + 280, getWidth() - 50, getHeight() / 2 + 280, decorPaint);

            Paint starDecorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            starDecorPaint.setColor(Color.rgb(255, 215, 0));
            starDecorPaint.setTextSize(24f);
            starDecorPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("⭐", getWidth() / 2 - 150, getHeight() / 2 - 120, starDecorPaint);
            canvas.drawText("⭐", getWidth() / 2 + 150, getHeight() / 2 - 120, starDecorPaint);
            canvas.drawText("⭐", getWidth() / 2 - 120, getHeight() / 2 + 230, starDecorPaint);
            canvas.drawText("⭐", getWidth() / 2 + 120, getHeight() / 2 + 230, starDecorPaint);
        }
    }

    // MODIFICADO: Área de pregunta MÁS ABAJO, CENTRADA y MÁS GRANDE
    private void drawQuestionArea(Canvas canvas) {
        float questionAreaHeight = 320f; // MÁS GRANDE (era 280f)
        float questionAreaWidth = canvas.getWidth() * 0.9f; // 90% del ancho (era 85%)

        // Calcular posición centrada y MÁS ABAJO
        float centerX = canvas.getWidth() / 2f;
        float centerY = questionAreaHeight / 2f + 120f; // MÁS ABAJO (era 40f)

        float left = centerX - (questionAreaWidth / 2f);
        float top = centerY - (questionAreaHeight / 2f);
        float right = centerX + (questionAreaWidth / 2f);
        float bottom = centerY + (questionAreaHeight / 2f);

        // Fondo opaco elegante - azul oscuro con más transparencia
        Paint questionBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        questionBgPaint.setColor(Color.argb(250, 25, 35, 55)); // MÁS OPACO

        // Gradiente más pronunciado
        LinearGradient gradient = new LinearGradient(
                left, top, left, bottom,
                new int[]{
                        Color.argb(250, 35, 45, 65),
                        Color.argb(250, 20, 30, 50)
                },
                null,
                Shader.TileMode.CLAMP
        );
        questionBgPaint.setShader(gradient);

        RectF rect = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(rect, 35f, 35f, questionBgPaint);

        // Borde elegante más grueso
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(8f); // MÁS GRUESO
        borderPaint.setColor(Color.argb(240, 100, 150, 255));
        canvas.drawRoundRect(rect, 35f, 35f, borderPaint);

        // Borde interno
        Paint innerBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerBorderPaint.setStyle(Paint.Style.STROKE);
        innerBorderPaint.setStrokeWidth(3f);
        innerBorderPaint.setColor(Color.argb(150, 180, 200, 255));
        RectF innerRect = new RectF(left + 4, top + 4, right - 4, bottom - 4);
        canvas.drawRoundRect(innerRect, 31f, 31f, innerBorderPaint);

        // Texto de la pregunta MÁS GRANDE
        Paint questionTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        questionTextPaint.setColor(Color.rgb(255, 255, 230));
        questionTextPaint.setTextSize(64f); // MÁS GRANDE (era 58f)
        questionTextPaint.setTextAlign(Paint.Align.CENTER);
        questionTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        questionTextPaint.setShadowLayer(10, 4, 4, Color.argb(220, 0, 0, 0));

        String questionText = "Q: " + questionA + " " + operation + " " + questionB + " = ?";
        canvas.drawText(questionText, centerX, centerY - 40f, questionTextPaint);

        // Línea separadora decorativa
        Paint separatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        separatorPaint.setColor(Color.argb(180, 100, 150, 255));
        separatorPaint.setStrokeWidth(4f);
        float separatorLeft = centerX - (questionAreaWidth * 0.35f);
        float separatorRight = centerX + (questionAreaWidth * 0.35f);
        canvas.drawLine(separatorLeft, centerY + 15f, separatorRight, centerY + 15f, separatorPaint);

        // Texto del score MÁS GRANDE
        Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scorePaint.setColor(Color.rgb(180, 255, 180));
        scorePaint.setTextSize(48f); // MÁS GRANDE (era 42f)
        scorePaint.setTextAlign(Paint.Align.CENTER);
        scorePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        scorePaint.setShadowLayer(8, 3, 3, Color.argb(200, 0, 0, 0));

        canvas.drawText("Score: " + score, centerX, centerY + 65f, scorePaint);

        // Efectos decorativos mejorados
        Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(Color.argb(60, 255, 255, 255));
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(2f);
        RectF glowRect = new RectF(left + 2, top + 2, right - 2, bottom - 2);
        canvas.drawRoundRect(glowRect, 33f, 33f, glowPaint);

        // Puntos decorativos en las esquinas MÁS GRANDES
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.argb(200, 150, 200, 255));
        float dotSize = 12f; // MÁS GRANDES
        canvas.drawCircle(left + 30f, top + 30f, dotSize, dotPaint);
        canvas.drawCircle(right - 30f, top + 30f, dotSize, dotPaint);
        canvas.drawCircle(left + 30f, bottom - 30f, dotSize, dotPaint);
        canvas.drawCircle(right - 30f, bottom - 30f, dotSize, dotPaint);
    }

    // NUEVO: Dibujar botón Back to Menu
    private void drawBackToMenuButton(Canvas canvas) {
        float buttonWidth = canvas.getWidth() * 0.6f; // 60% del ancho
        float buttonHeight = 70f;
        float buttonLeft = (canvas.getWidth() - buttonWidth) / 2f;
        float buttonTop = (canvas.getHeight() / 2f) + 190f; // Debajo del texto de restart

        // Actualizar rectángulo del botón para detección de toque
        backToMenuButton.set(buttonLeft, buttonTop, buttonLeft + buttonWidth, buttonTop + buttonHeight);

        // Fondo del botón con gradiente
        Paint buttonBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        LinearGradient buttonGradient = new LinearGradient(
                buttonLeft, buttonTop, buttonLeft, buttonTop + buttonHeight,
                new int[]{
                        Color.argb(255, 70, 130, 180),
                        Color.argb(255, 50, 100, 150)
                },
                null,
                Shader.TileMode.CLAMP
        );
        buttonBgPaint.setShader(buttonGradient);
        canvas.drawRoundRect(backToMenuButton, 25f, 25f, buttonBgPaint);

        // Borde del botón
        Paint buttonBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonBorderPaint.setStyle(Paint.Style.STROKE);
        buttonBorderPaint.setStrokeWidth(4f);
        buttonBorderPaint.setColor(Color.argb(255, 100, 150, 200));
        canvas.drawRoundRect(backToMenuButton, 25f, 25f, buttonBorderPaint);

        // Texto del botón
        Paint buttonTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonTextPaint.setColor(Color.WHITE);
        buttonTextPaint.setTextSize(38f);
        buttonTextPaint.setTextAlign(Paint.Align.CENTER);
        buttonTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        buttonTextPaint.setShadowLayer(4, 2, 2, Color.argb(150, 0, 0, 0));

        float textX = backToMenuButton.centerX();
        float textY = backToMenuButton.centerY() + (buttonTextPaint.getTextSize() / 3f);
        canvas.drawText("🏠 BACK TO MENU", textX, textY, buttonTextPaint);

        // Efecto de brillo en el botón
        Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(Color.argb(40, 255, 255, 255));
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(2f);
        RectF glowRect = new RectF(
                backToMenuButton.left + 2,
                backToMenuButton.top + 2,
                backToMenuButton.right - 2,
                backToMenuButton.bottom - 2
        );
        canvas.drawRoundRect(glowRect, 23f, 23f, glowPaint);
    }

    private void drawFood(Canvas canvas, FoodItem food, int offsetX, int offsetY, int cellSize) {
        if (food == null) return;

        int x = offsetX + food.position.x * cellSize;
        int y = offsetY + food.position.y * cellSize;
        int foodPadding = cellSize / 50;
        int foodSize = cellSize - (foodPadding * 2);
        int shadowOffset = cellSize / 40;

        Bitmap scaledFood = Bitmap.createScaledBitmap(food.bitmap, foodSize, foodSize, true);

        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0.3f, 0
        })));
        shadowPaint.setMaskFilter(new BlurMaskFilter(cellSize * 0.05f, BlurMaskFilter.Blur.NORMAL));

        canvas.drawBitmap(scaledFood, x + foodPadding + shadowOffset, y + foodPadding + shadowOffset, shadowPaint);
        canvas.drawBitmap(scaledFood, x + foodPadding, y + foodPadding, null);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (food == bonusFood) {
            bgPaint.setColor(Color.argb(200, 255, 193, 7));
        } else {
            bgPaint.setColor(Color.argb(180, 33, 33, 33));
        }

        float bgWidth = cellSize * 0.4f;
        float bgHeight = cellSize * 0.3f;
        float bgX = x + (cellSize - bgWidth) / 2;
        float bgY = y + (cellSize - bgHeight) / 2;
        float cornerRadius = cellSize * 0.08f;

        canvas.drawRoundRect(bgX, bgY, bgX + bgWidth, bgY + bgHeight, cornerRadius, cornerRadius, bgPaint);

        Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numberPaint.setColor(Color.WHITE);
        numberPaint.setTextSize(cellSize * 0.32f);
        numberPaint.setTextAlign(Paint.Align.CENTER);
        numberPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        numberPaint.setShadowLayer(6, 0, 3, Color.argb(200, 0, 0, 0));

        String numberText = String.valueOf(food.value);
        float textCenterX = bgX + bgWidth / 2;
        float textCenterY = bgY + bgHeight / 2;
        Paint.FontMetrics fm = numberPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        float textY = textCenterY + (textHeight / 2) - fm.descent;

        canvas.drawText(numberText, textCenterX, textY, numberPaint);
    }

    public void update() {
        if (gameOver) return;

        for (Star s : stars) {
            float speed = 0.015f + (s.alpha * 0.01f);
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
            if (snake.size() > 1) snake.remove(snake.size() - 1);
            if (snake.size() > 1) snake.remove(snake.size() - 1);
        } else if (!foodEaten) {
            snake.remove(snake.size() - 1);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver && event.getAction() == MotionEvent.ACTION_UP) {
            // NUEVO: Verificar si el toque fue en el botón Back to Menu
            if (backToMenuButton.contains(event.getX(), event.getY())) {
                if (gameEventListener != null) {
                    gameEventListener.onBackToMenuPressed();
                }
                return true;
            } else {
                // Si no fue en el botón, reiniciar el juego
                restartGame();
                return true;
            }
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
