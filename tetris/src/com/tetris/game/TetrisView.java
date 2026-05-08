package com.tetris.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

public class TetrisView extends View {

    // ── layout ──────────────────────────────────────────────────────────────
    private float cellSize;
    private float boardLeft, boardTop;
    private float panelLeft;   // right-side info panel x

    // ── game state ──────────────────────────────────────────────────────────
    private enum State { RUNNING, PAUSED, GAME_OVER }

    private final Board board = new Board();
    private final Random rng  = new Random();

    private Tetromino current, next;
    private int score, lines, level;
    private State state;

    // ── timing ───────────────────────────────────────────────────────────────
    private static final long[] SPEEDS = {
        800, 700, 600, 500, 400, 320, 250, 190, 140, 100, 70
    };
    private final Handler handler = new Handler();
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (state == State.RUNNING) {
                dropOne();
                invalidate();
                handler.postDelayed(tick, speed());
            }
        }
    };

    // ── paints ───────────────────────────────────────────────────────────────
    private final Paint pBg      = new Paint();
    private final Paint pBoard   = new Paint();
    private final Paint pGrid    = new Paint();
    private final Paint pCell    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pGhost   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pOverlay = new Paint();
    private final Paint pTitle   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pLabel   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pValue   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBtn     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBtnTxt  = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── touch ────────────────────────────────────────────────────────────────
    private float touchX, touchY;
    private long  touchTime;
    private static final float SWIPE_THRESHOLD = 30f;
    private static final long  TAP_MS          = 200;

    // ── control buttons (portrait, below board) ──────────────────────────────
    // [left]  [rotate]  [right]  [drop]
    private final RectF[] btnRects = new RectF[4];
    private final String[] btnLabels = {"◀", "↻", "▶", "▼"};

    public TetrisView(Context ctx) {
        super(ctx);
        pBg.setColor(0xFF0A0A1A);
        pBoard.setColor(0xFF111128);
        pGrid.setColor(0xFF1A1A35);
        pGrid.setStrokeWidth(1f);
        pCell.setStyle(Paint.Style.FILL);
        pGhost.setStyle(Paint.Style.STROKE);
        pGhost.setStrokeWidth(2f);
        pOverlay.setColor(0xCC000020);
        pTitle.setTypeface(Typeface.DEFAULT_BOLD);
        pTitle.setColor(Color.WHITE);
        pTitle.setTextAlign(Paint.Align.CENTER);
        pLabel.setTypeface(Typeface.DEFAULT);
        pLabel.setColor(0xFFAAAACC);
        pLabel.setTextAlign(Paint.Align.LEFT);
        pValue.setTypeface(Typeface.DEFAULT_BOLD);
        pValue.setColor(Color.WHITE);
        pValue.setTextAlign(Paint.Align.LEFT);
        pBtn.setColor(0xFF22224A);
        pBtn.setStyle(Paint.Style.FILL);
        pBtnTxt.setTypeface(Typeface.DEFAULT_BOLD);
        pBtnTxt.setColor(Color.WHITE);
        pBtnTxt.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < btnRects.length; i++) btnRects[i] = new RectF();
        startGame();
    }

    // ── game lifecycle ───────────────────────────────────────────────────────

    private void startGame() {
        board.clear();
        score = 0; lines = 0; level = 0;
        next = Tetromino.random(rng);
        spawnPiece();
        state = State.RUNNING;
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, speed());
    }

    private void spawnPiece() {
        current = next;
        next = Tetromino.random(rng);
        current.row = 0;
        current.col = Board.COLS / 2 - 2;
        if (!board.fits(current, current.row, current.col)) {
            state = State.GAME_OVER;
            invalidate();
        }
    }

    private long speed() {
        int idx = Math.min(level, SPEEDS.length - 1);
        return SPEEDS[idx];
    }

    // ── game logic ───────────────────────────────────────────────────────────

    private void dropOne() {
        if (board.fits(current, current.row + 1, current.col)) {
            current.row++;
        } else {
            lockAndSpawn();
        }
    }

    private void lockAndSpawn() {
        board.lock(current);
        int cleared = board.clearLines();
        if (cleared > 0) {
            int[] pts = {0, 100, 300, 500, 800};
            score += pts[Math.min(cleared, 4)] * (level + 1);
            lines += cleared;
            level = lines / 10;
        }
        spawnPiece();
    }

    private void hardDrop() {
        while (board.fits(current, current.row + 1, current.col))
            current.row++;
        score += 2;
        lockAndSpawn();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, speed());
        invalidate();
    }

    private void moveLeft() {
        if (board.fits(current, current.row, current.col - 1)) { current.col--; invalidate(); }
    }

    private void moveRight() {
        if (board.fits(current, current.row, current.col + 1)) { current.col++; invalidate(); }
    }

    private void rotate() {
        Tetromino t = current.copy();
        t.rotateRight();
        // wall-kick: try centre, then ±1, ±2
        int[] kicks = {0, -1, 1, -2, 2};
        for (int kick : kicks) {
            if (board.fits(t, t.row, t.col + kick)) {
                current.rotation = t.rotation;
                current.col += kick;
                invalidate();
                return;
            }
        }
    }

    private int ghostRow() {
        int r = current.row;
        while (board.fits(current, r + 1, current.col)) r++;
        return r;
    }

    // ── pause / resume ───────────────────────────────────────────────────────

    public void pause() {
        if (state == State.RUNNING) state = State.PAUSED;
        handler.removeCallbacks(tick);
        invalidate();
    }

    public void resume() {
        if (state == State.PAUSED) state = State.RUNNING;
        if (state == State.RUNNING) handler.postDelayed(tick, speed());
    }

    // ── layout ───────────────────────────────────────────────────────────────

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        // Reserve bottom strip for control buttons (18% of height)
        float btnH    = h * 0.18f;
        float playH   = h - btnH;
        cellSize  = Math.min(w * 0.62f / Board.COLS, playH / Board.ROWS);
        boardLeft = (w * 0.62f - Board.COLS * cellSize) / 2f;
        boardTop  = (playH - Board.ROWS * cellSize) / 2f;
        panelLeft = boardLeft + Board.COLS * cellSize + cellSize * 0.6f;

        // Four equal buttons across the bottom
        float bw = w / 4f;
        float by = playH + btnH * 0.1f;
        float bh = btnH * 0.8f;
        for (int i = 0; i < 4; i++)
            btnRects[i].set(i * bw + bw * 0.05f, by, (i + 1) * bw - bw * 0.05f, by + bh);

        pTitle.setTextSize(cellSize * 1.4f);
        pLabel.setTextSize(cellSize * 0.7f);
        pValue.setTextSize(cellSize * 1.0f);
        pBtnTxt.setTextSize(bh * 0.45f);
    }

    // ── drawing ──────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        int W = getWidth(), H = getHeight();
        canvas.drawRect(0, 0, W, H, pBg);

        drawBoard(canvas);
        drawCurrentAndGhost(canvas);
        drawGrid(canvas);
        drawPanel(canvas);
        drawButtons(canvas);

        if (state != State.RUNNING) drawOverlay(canvas, W, H);
    }

    private void drawBoard(Canvas canvas) {
        float bw = Board.COLS * cellSize, bh = Board.ROWS * cellSize;
        canvas.drawRect(boardLeft, boardTop, boardLeft + bw, boardTop + bh, pBoard);
        for (int r = 0; r < Board.ROWS; r++)
            for (int c = 0; c < Board.COLS; c++)
                if (board.grid[r][c] != 0)
                    drawCell(canvas, r, c, board.grid[r][c], 1f);
    }

    private void drawCurrentAndGhost(Canvas canvas) {
        if (state == State.GAME_OVER || current == null) return;
        int gr = ghostRow();
        // ghost
        pGhost.setColor(current.color & 0x55FFFFFF | 0x55000000);
        for (int[] cell : current.cells()) {
            float l = boardLeft + (current.col + cell[1]) * cellSize + 2;
            float t = boardTop  + (gr              + cell[0]) * cellSize + 2;
            canvas.drawRect(l, t, l + cellSize - 4, t + cellSize - 4, pGhost);
        }
        // current
        for (int[] cell : current.cells())
            drawCell(canvas, current.row + cell[0], current.col + cell[1], current.color, 1f);
    }

    private void drawGrid(Canvas canvas) {
        float bw = Board.COLS * cellSize, bh = Board.ROWS * cellSize;
        for (int c = 0; c <= Board.COLS; c++)
            canvas.drawLine(boardLeft + c * cellSize, boardTop,
                            boardLeft + c * cellSize, boardTop + bh, pGrid);
        for (int r = 0; r <= Board.ROWS; r++)
            canvas.drawLine(boardLeft, boardTop + r * cellSize,
                            boardLeft + bw, boardTop + r * cellSize, pGrid);
    }

    private void drawCell(Canvas canvas, int r, int c, int color, float alpha) {
        float l = boardLeft + c * cellSize + 1;
        float t = boardTop  + r * cellSize + 1;
        float sz = cellSize - 2;
        pCell.setColor(color);
        pCell.setAlpha((int)(alpha * 255));
        RectF rect = new RectF(l, t, l + sz, t + sz);
        canvas.drawRoundRect(rect, cellSize * 0.15f, cellSize * 0.15f, pCell);
        // highlight edge
        pCell.setColor(0x44FFFFFF);
        canvas.drawRoundRect(new RectF(l, t, l + sz, t + sz * 0.25f),
                             cellSize * 0.15f, cellSize * 0.15f, pCell);
        pCell.setAlpha(255);
    }

    private void drawPanel(Canvas canvas) {
        float x = panelLeft;
        float cs = cellSize;

        // Title
        pTitle.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("TETRIS", x, boardTop + cs * 1.2f, pTitle);

        // Score
        float y = boardTop + cs * 3f;
        canvas.drawText("SCORE", x, y, pLabel);
        canvas.drawText(String.valueOf(score), x, y + cs * 1.1f, pValue);

        y += cs * 2.8f;
        canvas.drawText("LINES", x, y, pLabel);
        canvas.drawText(String.valueOf(lines), x, y + cs * 1.1f, pValue);

        y += cs * 2.8f;
        canvas.drawText("LEVEL", x, y, pLabel);
        canvas.drawText(String.valueOf(level + 1), x, y + cs * 1.1f, pValue);

        // Next piece preview
        y += cs * 3.2f;
        canvas.drawText("NEXT", x, y, pLabel);
        if (next != null) {
            float nx = x + cs * 0.2f;
            float ny = y + cs * 0.4f;
            for (int[] cell : next.cells()) {
                float l = nx + cell[1] * cs * 0.85f;
                float t = ny + cell[0] * cs * 0.85f;
                pCell.setColor(next.color);
                RectF rect = new RectF(l, t, l + cs * 0.8f, t + cs * 0.8f);
                canvas.drawRoundRect(rect, cs * 0.12f, cs * 0.12f, pCell);
            }
        }
    }

    private void drawButtons(Canvas canvas) {
        float[] radii = {cellSize * 0.3f};
        for (int i = 0; i < 4; i++) {
            RectF r = btnRects[i];
            canvas.drawRoundRect(r, cellSize * 0.3f, cellSize * 0.3f, pBtn);
            canvas.drawText(btnLabels[i], r.centerX(), r.centerY() + pBtnTxt.getTextSize() * 0.36f, pBtnTxt);
        }
    }

    private void drawOverlay(Canvas canvas, int W, int H) {
        canvas.drawRect(0, 0, W, H, pOverlay);
        Paint big = new Paint(pTitle);
        big.setTextAlign(Paint.Align.CENTER);
        float cy = H * 0.42f;

        if (state == State.GAME_OVER) {
            big.setTextSize(cellSize * 2f);
            canvas.drawText("GAME OVER", W / 2f, cy, big);
            big.setTextSize(cellSize * 1.1f);
            canvas.drawText("Score: " + score, W / 2f, cy + cellSize * 2.4f, big);
            big.setTextSize(cellSize * 0.85f);
            big.setColor(0xFFBBBBCC);
            canvas.drawText("Tap to play again", W / 2f, cy + cellSize * 4f, big);
        } else {
            big.setTextSize(cellSize * 2.2f);
            canvas.drawText("PAUSED", W / 2f, cy, big);
            big.setTextSize(cellSize * 0.85f);
            big.setColor(0xFFBBBBCC);
            canvas.drawText("Tap to continue", W / 2f, cy + cellSize * 2.8f, big);
        }
    }

    // ── touch ─────────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchX = e.getX(); touchY = e.getY();
                touchTime = System.currentTimeMillis();
                return true;

            case MotionEvent.ACTION_UP:
                if (state == State.GAME_OVER) { startGame(); return true; }
                if (state == State.PAUSED)    { resume();    invalidate(); return true; }

                float ex = e.getX(), ey = e.getY();

                // On-screen buttons
                for (int i = 0; i < btnRects.length; i++) {
                    if (btnRects[i].contains(ex, ey)) {
                        handleButton(i); return true;
                    }
                }

                float dx = ex - touchX, dy = ey - touchY;
                long dt = System.currentTimeMillis() - touchTime;
                boolean tap = Math.abs(dx) < SWIPE_THRESHOLD && Math.abs(dy) < SWIPE_THRESHOLD && dt < TAP_MS;

                if (tap) { rotate(); return true; }

                if (Math.abs(dx) > Math.abs(dy)) {
                    if (dx > SWIPE_THRESHOLD)  moveRight();
                    else if (dx < -SWIPE_THRESHOLD) moveLeft();
                } else {
                    if (dy > SWIPE_THRESHOLD)  hardDrop();
                }
                return true;
        }
        return super.onTouchEvent(e);
    }

    private void handleButton(int btn) {
        switch (btn) {
            case 0: moveLeft();  break;
            case 1: rotate();    break;
            case 2: moveRight(); break;
            case 3: hardDrop();  break;
        }
    }
}
