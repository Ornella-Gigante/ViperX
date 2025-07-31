package es.nellagames.viperx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import java.util.Random;

public class MovingShapesView extends View {

    private static class Bubble {
        float x, y, r, dx, dy;
        int color;
    }
    private Bubble[] bubbles;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Handler handler = new Handler();
    private final int NUM_BUBBLES = 10;
    private Random rand = new Random();

    // Tamaño real de la vista (para evitar crash con nextInt(0))
    private int width = 1, height = 1;

    public MovingShapesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        bubbles = new Bubble[NUM_BUBBLES];
        for (int i = 0; i < NUM_BUBBLES; i++) {
            bubbles[i] = createBubble();
        }
        handler.post(animator);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w > 0 ? w : 1;
        height = h > 0 ? h : 1;
    }

    private Bubble createBubble() {
        Bubble b = new Bubble();
        b.x = rand.nextInt(width > 0 ? width : 1);
        b.y = rand.nextInt(height > 0 ? height : 1);
        b.r = rand.nextInt(25) + 25;
        b.dx = rand.nextFloat() * 3 + 1;
        b.dy = rand.nextFloat() * 2 + 1;
        int[] palette = {
                Color.parseColor("#CDA6F7"), // Lavanda
                Color.parseColor("#A7D2FE"), // Azul pastel
                Color.parseColor("#F5D061"), // Ocre
                Color.parseColor("#B983FF"), // Púrpura
                Color.parseColor("#FFECBA")  // Amarillo pastel
        };
        b.color = palette[rand.nextInt(palette.length)];
        return b;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (Bubble b : bubbles) {
            paint.setColor(b.color);
            paint.setAlpha(140);
            canvas.drawCircle(b.x, b.y, b.r, paint);
        }
    }

    private Runnable animator = new Runnable() {
        @Override
        public void run() {
            for (Bubble b : bubbles) {
                b.x += b.dx;
                b.y += b.dy;
                if (b.x > width + b.r || b.y > height + b.r) {
                    Bubble nb = createBubble();
                    nb.x = -nb.r;
                    nb.y = rand.nextInt(height > 0 ? height : 1); // Siempre seguro
                    b.x = nb.x;
                    b.y = nb.y;
                    b.r = nb.r;
                    b.dx = nb.dx;
                    b.dy = nb.dy;
                    b.color = nb.color;
                }
            }
            invalidate();
            handler.postDelayed(this, 20);
        }
    };
}
