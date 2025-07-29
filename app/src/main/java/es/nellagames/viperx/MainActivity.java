package es.nellagames.viperx;

import android.media.MediaPlayer;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private GameView gameView;
    private MediaPlayer backgroundMusic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameView = new GameView(this);
        setContentView(gameView);

        backgroundMusic = MediaPlayer.create(this, R.raw.main_music);
        backgroundMusic.setLooping(true);
        backgroundMusic.start();
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
