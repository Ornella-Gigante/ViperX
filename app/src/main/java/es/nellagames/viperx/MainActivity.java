package es.nellagames.viperx;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private GameView gameView;
    private LinearLayout menuLayer;
    private TextView highScoreLabel;
    private Button playButton, instructionsButton;
    private SharedPreferences prefs;
    private int highScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pantalla completa
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        // Inicializar SharedPreferences para high score
        prefs = getSharedPreferences("ViperXPrefs", MODE_PRIVATE);
        highScore = prefs.getInt("highScore", 0);

        // Encontrar vistas
        gameView = findViewById(R.id.gameView);
        menuLayer = findViewById(R.id.menuLayer);
        highScoreLabel = findViewById(R.id.highScoreLabel);
        playButton = findViewById(R.id.playButton);
        instructionsButton = findViewById(R.id.instructionsButton);

        // Actualizar high score en el menú
        updateHighScoreDisplay();

        // Configurar listeners
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        instructionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInstructions();
            }
        });

        // Inicialmente mostrar menú, ocultar juego
        showMenu();

        Log.d("MainActivity", "Activity created successfully");
    }

    private void startGame() {
        Log.d("MainActivity", "Starting game...");

        // Ocultar menú
        menuLayer.setVisibility(View.GONE);

        // Mostrar y iniciar juego
        gameView.setVisibility(View.VISIBLE);
        gameView.restartGame();
        gameView.resume();

        Log.d("MainActivity", "Game started");
    }

    private void showMenu() {
        Log.d("MainActivity", "Showing menu...");

        // Pausar juego si está corriendo
        if (gameView != null) {
            gameView.pause();
            gameView.setVisibility(View.GONE);
        }

        // Mostrar menú
        menuLayer.setVisibility(View.VISIBLE);
        updateHighScoreDisplay();

        Log.d("MainActivity", "Menu shown");
    }

    private void showInstructions() {
        // Aquí puedes implementar un diálogo o nueva actividad con instrucciones
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Instructions");
        builder.setMessage("🐍 VIPER X - Math Snake Game\n\n" +
                "• Swipe to move the snake\n" +
                "• Eat the food with the correct answer\n" +
                "• Solve math problems to grow\n" +
                "• Avoid hitting walls or yourself\n" +
                "• Collect bonus food for extra points!\n\n" +
                "Good luck!");
        builder.setPositiveButton("Got it!", null);
        builder.show();
    }

    private void updateHighScoreDisplay() {
        highScoreLabel.setText("High Score: " + highScore);
    }

    public void updateHighScore(int newScore) {
        if (newScore > highScore) {
            highScore = newScore;
            prefs.edit().putInt("highScore", highScore).apply();
            updateHighScoreDisplay();
            Log.d("MainActivity", "New high score: " + highScore);
        }
    }

    public void showGameOver(int finalScore) {
        Log.d("MainActivity", "Game over with score: " + finalScore);
        updateHighScore(finalScore);

        // Mostrar diálogo de game over
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Game Over!");
        builder.setMessage("Final Score: " + finalScore +
                (finalScore == highScore ? "\n🎉 NEW HIGH SCORE! 🎉" : ""));
        builder.setPositiveButton("Play Again", (dialog, which) -> startGame());
        builder.setNegativeButton("Menu", (dialog, which) -> showMenu());
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "Activity resumed");
        // No auto-resumir el juego, solo si ya estaba visible
        if (gameView != null && gameView.getVisibility() == View.VISIBLE) {
            gameView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MainActivity", "Activity paused");
        if (gameView != null) {
            gameView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MainActivity", "Activity destroyed");
        if (gameView != null) {
            gameView.pause();
        }
    }

    // Método para que GameView pueda llamar cuando termine el juego
    public void onGameFinished(int score) {
        runOnUiThread(() -> showGameOver(score));
    }

    @Override
    public void onBackPressed() {
        if (gameView != null && gameView.getVisibility() == View.VISIBLE) {
            // Si el juego está visible, volver al menú
            showMenu();
        } else {
            // Si está en el menú, salir de la app
            super.onBackPressed();
        }
    }
}