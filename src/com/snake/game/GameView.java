package com.snake.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import java.util.LinkedList;
import java.util.Random;

public class GameView extends View {

    private static final int COLS = 20;
    private static final int ROWS = 28;
    private static final long GAME_SPEED_MS = 140;

    private enum Direction { UP, DOWN, LEFT, RIGHT }
    private enum State { WAITING, RUNNING, GAME_OVER }

    private LinkedList<int[]> snake;
    private int[] food;
    private Direction dir;
    private Direction nextDir;
    private State state;
    private int score;
    private int highScore;

    private final Paint paintBg      = new Paint();
    private final Paint paintGrid    = new Paint();
    private final Paint paintHead    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBody    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintFood    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintOverlay = new Paint();
    private final Paint paintText    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintSub     = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Handler handler = new Handler();
    private final Random  random  = new Random();

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (state == State.RUNNING) {
                step();
                invalidate();
                handler.postDelayed(tick, GAME_SPEED_MS);
            }
        }
    };

    private float cellW, cellH, offX, offY;
    private float touchX, touchY;

    public GameView(Context ctx) {
        super(ctx);
        paintBg.setColor(0xFF0D0D1A);
        paintGrid.setColor(0xFF16163A);
        paintGrid.setStrokeWidth(1f);
        paintHead.setColor(0xFF1B5E20);
        paintBody.setColor(0xFF43A047);
        paintFood.setColor(0xFFEF5350);
        paintOverlay.setColor(0xCC000000);
        paintText.setColor(Color.WHITE);
        paintText.setTypeface(Typeface.DEFAULT_BOLD);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintSub.setColor(0xFFBBBBBB);
        paintSub.setTypeface(Typeface.DEFAULT);
        paintSub.setTextAlign(Paint.Align.CENTER);
        highScore = 0;
        initGame();
    }

    private void initGame() {
        snake = new LinkedList<int[]>();
        dir = Direction.RIGHT;
        nextDir = Direction.RIGHT;
        score = 0;
        int mx = COLS / 2;
        int my = ROWS / 2;
        snake.addFirst(new int[]{mx,     my});
        snake.addFirst(new int[]{mx + 1, my});
        snake.addFirst(new int[]{mx + 2, my});
        placeFood();
        state = State.RUNNING;
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, GAME_SPEED_MS);
    }

    private void placeFood() {
        int x, y;
        do {
            x = random.nextInt(COLS);
            y = random.nextInt(ROWS);
        } while (occupiedBy(x, y));
        food = new int[]{x, y};
    }

    private boolean occupiedBy(int x, int y) {
        for (int[] s : snake) if (s[0] == x && s[1] == y) return true;
        return false;
    }

    private void step() {
        dir = nextDir;
        int[] head = snake.getFirst();
        int nx = head[0], ny = head[1];
        switch (dir) {
            case UP:    ny--; break;
            case DOWN:  ny++; break;
            case LEFT:  nx--; break;
            case RIGHT: nx++; break;
        }
        if (nx < 0 || nx >= COLS || ny < 0 || ny >= ROWS) { gameOver(); return; }
        // self-collision (exclude tail which will move away)
        for (int i = 0; i < snake.size() - 1; i++) {
            int[] s = snake.get(i);
            if (s[0] == nx && s[1] == ny) { gameOver(); return; }
        }
        snake.addFirst(new int[]{nx, ny});
        if (nx == food[0] && ny == food[1]) {
            score += 10;
            placeFood();
        } else {
            snake.removeLast();
        }
    }

    private void gameOver() {
        if (score > highScore) highScore = score;
        state = State.GAME_OVER;
        invalidate();
    }

    public void pause()  { handler.removeCallbacks(tick); }
    public void resume() { if (state == State.RUNNING) handler.postDelayed(tick, GAME_SPEED_MS); }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        float headerH = h * 0.08f;
        cellW = (float) w / COLS;
        cellH = (h - headerH) / ROWS;
        offX  = 0;
        offY  = headerH;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int W = getWidth(), H = getHeight();

        // background
        canvas.drawRect(0, 0, W, H, paintBg);

        // grid lines
        for (int c = 0; c <= COLS; c++)
            canvas.drawLine(offX + c * cellW, offY, offX + c * cellW, offY + ROWS * cellH, paintGrid);
        for (int r = 0; r <= ROWS; r++)
            canvas.drawLine(offX, offY + r * cellH, offX + COLS * cellW, offY + r * cellH, paintGrid);

        // food — pulsing red circle
        float fc = offX + food[0] * cellW + cellW / 2f;
        float fr = offY + food[1] * cellH + cellH / 2f;
        float fRad = Math.min(cellW, cellH) * 0.4f;
        canvas.drawCircle(fc, fr, fRad, paintFood);

        // snake
        float r = Math.min(cellW, cellH) * 0.18f;
        for (int i = 0; i < snake.size(); i++) {
            int[] s = snake.get(i);
            float l = offX + s[0] * cellW + 2;
            float t = offY + s[1] * cellH + 2;
            RectF rect = new RectF(l, t, l + cellW - 4, t + cellH - 4);
            canvas.drawRoundRect(rect, r, r, i == 0 ? paintHead : paintBody);
        }

        // score header
        float headerH = offY;
        paintText.setTextSize(headerH * 0.55f);
        canvas.drawText("SCORE  " + score + "    BEST  " + highScore, W / 2f, headerH * 0.75f, paintText);

        if (state == State.GAME_OVER) drawGameOver(canvas, W, H);
    }

    private void drawGameOver(Canvas canvas, int W, int H) {
        canvas.drawRect(0, 0, W, H, paintOverlay);
        float cy = H / 2f;
        paintText.setTextSize(W * 0.13f);
        canvas.drawText("GAME OVER", W / 2f, cy - W * 0.12f, paintText);
        paintText.setTextSize(W * 0.07f);
        canvas.drawText("Score: " + score, W / 2f, cy + W * 0.02f, paintText);
        canvas.drawText("Best:  " + highScore, W / 2f, cy + W * 0.11f, paintText);
        paintSub.setTextSize(W * 0.055f);
        canvas.drawText("Tap anywhere to restart", W / 2f, cy + W * 0.22f, paintSub);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchX = e.getX();
                touchY = e.getY();
                return true;
            case MotionEvent.ACTION_UP:
                if (state == State.GAME_OVER) {
                    initGame();
                    return true;
                }
                float dx = e.getX() - touchX;
                float dy = e.getY() - touchY;
                if (Math.abs(dx) >= Math.abs(dy)) {
                    if (dx > 0 && dir != Direction.LEFT)  nextDir = Direction.RIGHT;
                    if (dx < 0 && dir != Direction.RIGHT) nextDir = Direction.LEFT;
                } else {
                    if (dy > 0 && dir != Direction.UP)   nextDir = Direction.DOWN;
                    if (dy < 0 && dir != Direction.DOWN) nextDir = Direction.UP;
                }
                return true;
        }
        return super.onTouchEvent(e);
    }
}
