package com.tetris.game;

import java.util.Random;

public class Tetromino {

    // Each piece defined as 4 (row,col) offsets from pivot, for 4 rotations
    // Index: [piece][rotation][cell][row=0,col=1]
    private static final int[][][][] SHAPES = {
        // I  (cyan)
        {{{0,0},{0,1},{0,2},{0,3}}, {{0,0},{1,0},{2,0},{3,0}},
         {{0,0},{0,1},{0,2},{0,3}}, {{0,0},{1,0},{2,0},{3,0}}},
        // O  (yellow)
        {{{0,0},{0,1},{1,0},{1,1}}, {{0,0},{0,1},{1,0},{1,1}},
         {{0,0},{0,1},{1,0},{1,1}}, {{0,0},{0,1},{1,0},{1,1}}},
        // T  (purple)
        {{{0,1},{1,0},{1,1},{1,2}}, {{0,0},{1,0},{2,0},{1,1}},
         {{1,0},{1,1},{1,2},{0,1}}, {{0,1},{1,1},{2,1},{1,0}}},
        // S  (green)
        {{{0,1},{0,2},{1,0},{1,1}}, {{0,0},{1,0},{1,1},{2,1}},
         {{0,1},{0,2},{1,0},{1,1}}, {{0,0},{1,0},{1,1},{2,1}}},
        // Z  (red)
        {{{0,0},{0,1},{1,1},{1,2}}, {{0,1},{1,0},{1,1},{2,0}},
         {{0,0},{0,1},{1,1},{1,2}}, {{0,1},{1,0},{1,1},{2,0}}},
        // L  (orange)
        {{{0,2},{1,0},{1,1},{1,2}}, {{0,0},{1,0},{2,0},{2,1}},
         {{1,0},{1,1},{1,2},{0,0}}, {{0,0},{0,1},{1,1},{2,1}}},
        // J  (blue)
        {{{0,0},{1,0},{1,1},{1,2}}, {{0,0},{0,1},{1,0},{2,0}},
         {{1,0},{1,1},{1,2},{0,2}}, {{0,1},{1,1},{2,1},{2,0}}}
    };

    // Colours per piece type
    static final int[] COLORS = {
        0xFF00BCD4, // I  cyan
        0xFFFFEB3B, // O  yellow
        0xFF9C27B0, // T  purple
        0xFF4CAF50, // S  green
        0xFFF44336, // Z  red
        0xFFFF9800, // L  orange
        0xFF2196F3  // J  blue
    };

    int type;
    int rotation;
    int row;   // top-left anchor row on board
    int col;   // top-left anchor col on board
    int color;

    Tetromino(int type) {
        this.type = type;
        this.rotation = 0;
        this.color = COLORS[type];
    }

    int[][] cells() { return SHAPES[type][rotation]; }

    static Tetromino random(Random rng) { return new Tetromino(rng.nextInt(SHAPES.length)); }

    Tetromino copy() {
        Tetromino t = new Tetromino(type);
        t.rotation = rotation;
        t.row = row;
        t.col = col;
        return t;
    }

    void rotateRight() { rotation = (rotation + 1) % 4; }
    void rotateLeft()  { rotation = (rotation + 3) % 4; }
}
