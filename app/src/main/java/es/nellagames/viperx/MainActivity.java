package es.nellagames.viperx;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.util.TypedValue;
import android.view.ViewOutlineProvider;

public class MainActivity extends Activity {

    private GameView gameView;
    private LinearLayout menuLayer;
    private FrameLayout instructionsLayer;
    private FrameLayout gameLayer;
    private TextView highScoreLabel;
    private Button playButton, instructionsButton, backFromInstructionsButton;
    private SharedPreferences prefs;
    private int highScore = 0;
    private MediaPlayer backgroundMusic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pantalla completa
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        // Inicializar música de fondo
        backgroundMusic = MediaPlayer.create(this, R.raw.main_music);
        backgroundMusic.setLooping(true);
        backgroundMusic.start();

        // Inicializar SharedPreferences para high score
        prefs = getSharedPreferences("ViperXPrefs", MODE_PRIVATE);
        highScore = prefs.getInt("highScore", 0);

        // Encontrar vistas
        gameView = findViewById(R.id.gameView);
        menuLayer = findViewById(R.id.menuLayer);
        instructionsLayer = findViewById(R.id.instructionsLayer);
        gameLayer = findViewById(R.id.gameLayer);
        highScoreLabel = findViewById(R.id.highScoreLabel);
        playButton = findViewById(R.id.playButton);
        instructionsButton = findViewById(R.id.instructionsButton);
        backFromInstructionsButton = findViewById(R.id.backFromInstructionsButton);

        updateHighScoreDisplay();

        // === Bordes redondeados para los botones usando OutlineProvider ===
        applyRoundedCorners(playButton, "#B983FF");
        applyRoundedCorners(instructionsButton, "#577590");
        applyRoundedCorners(backFromInstructionsButton, "#B983FF");

        // PLAY -- mostrar el juego
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGameLayer();
            }
        });

        // INSTRUCTIONS -- mostrar pantalla de instrucciones animada
        instructionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuLayer.setVisibility(View.GONE);
                gameLayer.setVisibility(View.GONE);
                instructionsLayer.setVisibility(View.VISIBLE);
            }
        });

        // BACK en instrucciones -- volver al menú principal
        backFromInstructionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu();
            }
        });

        // Inicialmente mostrar menú, ocultar juego e instrucciones
        showMenu();
        Log.d("MainActivity", "Activity started");
    }

    // Aplica esquinas redondeadas a un botón por código (sin shape!)
    private void applyRoundedCorners(final Button button, final String colorHex) {
        if (button == null) return;

        button.setStateListAnimator(null);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorHex)));
        button.setClipToOutline(true);

        final float radiusPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 18, button.getResources().getDisplayMetrics()
        );

        button.post(() -> {
            button.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radiusPx);
                }
            });
        });
    }

    private void showMenu() {
        if (gameView != null) {
            gameView.pause();
        }
        if (gameLayer != null) gameLayer.setVisibility(View.GONE);
        if (instructionsLayer != null) instructionsLayer.setVisibility(View.GONE);
        menuLayer.setVisibility(View.VISIBLE);
        updateHighScoreDisplay();
    }

    private void updateHighScoreDisplay() {
        if (highScoreLabel != null)
            highScoreLabel.setText("High Score: " + highScore);
    }

    // CORREGIDO: Método para actualizar el high score y guardarlo
    public void updateHighScore(int newScore) {
        if (newScore > highScore) {
            highScore = newScore;
            prefs.edit().putInt("highScore", highScore).apply();
            updateHighScoreDisplay();
            Log.d("MainActivity", "New high score: " + highScore);
        }
    }

    public void showGameOver(int finalScore) {
        updateHighScore(finalScore);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Game Over!");
        builder.setMessage("Final Score: " + finalScore +
                (finalScore == highScore ? "\n🎉 NEW HIGH SCORE! 🎉" : ""));
        builder.setPositiveButton("Play Again", (dialog, which) -> {
            showGameLayer();
        });
        builder.setNegativeButton("Menu", (dialog, which) -> showMenu());
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (backgroundMusic != null && !backgroundMusic.isPlaying())
            backgroundMusic.start();
        if (gameView != null && gameLayer != null && gameLayer.getVisibility() == View.VISIBLE) {
            gameView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (backgroundMusic != null && backgroundMusic.isPlaying())
            backgroundMusic.pause();
        if (gameView != null) gameView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (backgroundMusic != null) {
            backgroundMusic.release();
            backgroundMusic = null;
        }
        if (gameView != null) gameView.pause();
    }

    // ELIMINADO: No necesario ya que el GameView maneja todo internamente
    // public void onGameFinished(int score) { ... }

    private void showGameLayer() {
        // CORREGIDO: Configurar el listener del GameView correctamente
        gameView.setGameEventListener(new GameView.GameEventListener() {
            @Override
            public void onBackToMenuPressed() {
                // Actualizar high score antes de volver al menú
                updateHighScore(gameView.getCurrentScore());
                showMenu();
            }

            @Override
            public void onGameOver(int finalScore) {
                // Actualizar high score cuando el juego termine
                updateHighScore(finalScore);
            }
        });

        // Pasar el high score al GameView
        gameView.setHighScore(highScore);

        // Cambiar visibilidad
        gameLayer.setVisibility(View.VISIBLE);
        menuLayer.setVisibility(View.GONE);
        instructionsLayer.setVisibility(View.GONE);

        // Solicitar foco
        gameView.post(new Runnable() {
            @Override
            public void run() {
                gameView.requestFocus();
                gameView.requestFocusFromTouch();
            }
        });

        // Reiniciar y resumir el juego
        gameView.restartGame();
        gameView.resume();
    }

    @Override
    public void onBackPressed() {
        if (gameLayer != null && gameLayer.getVisibility() == View.VISIBLE) {
            // CORREGIDO: Actualizar high score antes de salir del juego
            updateHighScore(gameView.getCurrentScore());
            showMenu();
        } else {
            super.onBackPressed();
        }
    }
}