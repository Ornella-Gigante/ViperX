package es.nellagames.viperx;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private GameView gameView;
    private MediaPlayer backgroundMusic;
    private LinearLayout menuLayer, instructionsLayer;
    private TextView highScoreLabel;
    private int highScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        menuLayer = findViewById(R.id.menuLayer);
        instructionsLayer = findViewById(R.id.instructionsLayer);
        gameView = findViewById(R.id.gameView);
        highScoreLabel = findViewById(R.id.highScoreLabel);

        backgroundMusic = MediaPlayer.create(this, R.raw.main_music);
        backgroundMusic.setLooping(true);
        backgroundMusic.start();

        // PLAY
        findViewById(R.id.playButton).setOnClickListener(v -> {
            menuLayer.setVisibility(View.GONE);
            instructionsLayer.setVisibility(View.GONE);
            gameView.setVisibility(View.VISIBLE);
            gameView.restartGame();
        });

        // INSTRUCTIONS
        findViewById(R.id.instructionsButton).setOnClickListener(v -> {
            menuLayer.setVisibility(View.GONE);
            instructionsLayer.setVisibility(View.VISIBLE);
        });

        // BACK from instructions
        findViewById(R.id.backFromInstructionsButton).setOnClickListener(v -> {
            instructionsLayer.setVisibility(View.GONE);
            menuLayer.setVisibility(View.VISIBLE);
        });

        // Opcional: Actualiza high score cuando termina el juego, etc.
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (backgroundMusic != null) backgroundMusic.pause();
        gameView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (backgroundMusic != null) backgroundMusic.start();
        gameView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (backgroundMusic != null) {
            backgroundMusic.release();
            backgroundMusic = null;
        }
    }


}
