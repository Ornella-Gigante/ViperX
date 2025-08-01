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

public class MainActivity extends Activity {

    private GameView gameView;
    private LinearLayout menuLayer;
    private View instructionsLayer;
    private TextView highScoreLabel;
    private Button playButton, instructionsButton, backFromInstructionsButton;
    private SharedPreferences prefs;
    private int highScore = 0;
    private MediaPlayer backgroundMusic;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pantalla completa, opcional
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        backgroundMusic = MediaPlayer.create(this, R.raw.main_music); // Usa el nombre que tengas: main_music.mp3
        backgroundMusic.setLooping(true);
        backgroundMusic.start();


        // Inicializar SharedPreferences para high score
        prefs = getSharedPreferences("ViperXPrefs", MODE_PRIVATE);
        highScore = prefs.getInt("highScore", 0);

        // Encontrar vistas
        gameView = findViewById(R.id.gameView);
        menuLayer = findViewById(R.id.menuLayer);
        instructionsLayer = findViewById(R.id.instructionsLayer);
        highScoreLabel = findViewById(R.id.highScoreLabel);
        playButton = findViewById(R.id.playButton);
        instructionsButton = findViewById(R.id.instructionsButton);
        backFromInstructionsButton = findViewById(R.id.backFromInstructionsButton);

        updateHighScoreDisplay();

        // PLAY -- mostrar el juego
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuLayer.setVisibility(View.GONE);
                instructionsLayer.setVisibility(View.GONE);
                gameView.setVisibility(View.VISIBLE);
                gameView.restartGame();
                gameView.resume();
            }
        });

        // INSTRUCTIONS -- mostrar pantalla de instrucciones animada
        instructionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuLayer.setVisibility(View.GONE);
                gameView.setVisibility(View.GONE);
                instructionsLayer.setVisibility(View.VISIBLE);
            }
        });

        // BACK en instrucciones -- volver al menú principal
        backFromInstructionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                instructionsLayer.setVisibility(View.GONE);
                menuLayer.setVisibility(View.VISIBLE);
            }
        });

        // Inicialmente mostrar menú, ocultar juego e instrucciones
        showMenu();
        Log.d("MainActivity", "Activity started");
    }

    private void showMenu() {
        if (gameView != null) {
            gameView.pause();
            gameView.setVisibility(View.GONE);
        }
        if (instructionsLayer != null) instructionsLayer.setVisibility(View.GONE);
        menuLayer.setVisibility(View.VISIBLE);
        updateHighScoreDisplay();
    }

    private void updateHighScoreDisplay() {
        if (highScoreLabel != null)
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
        updateHighScore(finalScore);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Game Over!");
        builder.setMessage("Final Score: " + finalScore +
                (finalScore == highScore ? "\n🎉 NEW HIGH SCORE! 🎉" : ""));
        builder.setPositiveButton("Play Again", (dialog, which) -> {
            menuLayer.setVisibility(View.GONE);
            instructionsLayer.setVisibility(View.GONE);
            gameView.setVisibility(View.VISIBLE);
            gameView.restartGame();
            gameView.resume();
        });
        builder.setNegativeButton("Menu", (dialog, which) -> showMenu());
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (backgroundMusic != null && backgroundMusic.isPlaying()) backgroundMusic.pause();
        if (gameView != null) gameView.pause();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) backgroundMusic.start();
        if (gameView != null && gameView.getVisibility() == View.VISIBLE) gameView.resume();
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


    @Override
    public void onBackPressed() {
        if (gameView != null && gameView.getVisibility() == View.VISIBLE) {
            showMenu();
        } else {
            super.onBackPressed();
        }
    }
}
