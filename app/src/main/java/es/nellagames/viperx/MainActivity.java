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
    private LinearLayout menuLayer;
    private View instructionsLayer;
    private TextView highScoreLabel;
    private int highScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Layer references from included layouts
        menuLayer = findViewById(R.id.menuLayer);
        instructionsLayer = findViewById(R.id.instructionsLayer);
        gameView = findViewById(R.id.gameView);
        highScoreLabel = findViewById(R.id.highScoreLabel);

        backgroundMusic = MediaPlayer.create(this, R.raw.main_music);
        backgroundMusic.setLooping(true);
        backgroundMusic.start();

        // PLAY button
        Button playButton = findViewById(R.id.playButton);
        if (playButton != null) {
            playButton.setOnClickListener(v -> {
                menuLayer.setVisibility(View.GONE);
                instructionsLayer.setVisibility(View.GONE);
                gameView.setVisibility(View.VISIBLE);
                gameView.restartGame();
            });
        }

        // INSTRUCTIONS button
        Button instructionsButton = findViewById(R.id.instructionsButton);
        if (instructionsButton != null) {
            instructionsButton.setOnClickListener(v -> {
                menuLayer.setVisibility(View.GONE);
                instructionsLayer.setVisibility(View.VISIBLE);
            });
        }

        // BACK from instructions
        Button backButton = findViewById(R.id.backFromInstructionsButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                instructionsLayer.setVisibility(View.GONE);
                menuLayer.setVisibility(View.VISIBLE);
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (backgroundMusic != null) backgroundMusic.pause();
        if (gameView != null) gameView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (backgroundMusic != null) backgroundMusic.start();
        if (gameView != null) gameView.resume();
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
