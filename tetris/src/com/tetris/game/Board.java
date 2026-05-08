package com.tetris.game;

public class Board {

    static final int COLS = 10;
    static final int ROWS = 20;

    final int[][] grid = new int[ROWS][COLS]; // 0 = empty, else color

    void clear() {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                grid[r][c] = 0;
    }

    boolean fits(Tetromino t, int row, int col) {
        for (int[] cell : t.cells()) {
            int r = row + cell[0];
            int c = col + cell[1];
            if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return false;
            if (grid[r][c] != 0) return false;
        }
        return true;
    }

    void lock(Tetromino t) {
        for (int[] cell : t.cells())
            grid[t.row + cell[0]][t.col + cell[1]] = t.color;
    }

    // Returns number of lines cleared
    int clearLines() {
        int cleared = 0;
        for (int r = ROWS - 1; r >= 0; r--) {
            if (isFullRow(r)) {
                removeRow(r);
                r++; // recheck same index
                cleared++;
            }
        }
        return cleared;
    }

    private boolean isFullRow(int r) {
        for (int c = 0; c < COLS; c++) if (grid[r][c] == 0) return false;
        return true;
    }

    private void removeRow(int row) {
        for (int r = row; r > 0; r--)
            System.arraycopy(grid[r - 1], 0, grid[r], 0, COLS);
        for (int c = 0; c < COLS; c++) grid[0][c] = 0;
    }
}
