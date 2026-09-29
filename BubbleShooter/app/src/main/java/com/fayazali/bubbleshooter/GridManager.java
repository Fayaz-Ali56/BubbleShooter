package com.fayazali.bubbleshooter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 * Holds the hexagonal grid of bubbles and all the logic that operates on it:
 * snapping a fired bubble into place, finding colour matches, finding
 * bubbles that are no longer attached to the ceiling, and bomb blast radius.
 */
public class GridManager {

    public static final int COLS = 8;
    public static final int MAX_ROWS = 20;

    public Bubble[][] grid;
    public float bubbleRadius;
    public float startX, startY;
    public float horizontalSpacing;
    public float verticalSpacing;

    private final Random random = new Random();
    private int colorsForLevel = 3;

    public GridManager(float startX, float startY, float bubbleRadius) {
        this.startX = startX;
        this.startY = startY;
        this.bubbleRadius = bubbleRadius;
        this.horizontalSpacing = bubbleRadius * 2.1f;
        this.verticalSpacing = bubbleRadius * 1.85f;
        grid = new Bubble[MAX_ROWS][COLS];
    }

    /** Builds the starting layout for a level. Higher levels are harder. */
    public void initForLevel(int level) {
        // more colours and more rows as the player progresses
        colorsForLevel = Math.min(2 + (level / 10), Bubble.COLORS.length);
        int rows = Math.min(3 + (level / 5), 10);

        for (int r = 0; r < MAX_ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = null;
            }
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < COLS; c++) {
                // leave gaps in the layout at high levels to vary the shape
                if (level > 50 && random.nextInt(10) < 2) continue;
                grid[r][c] = new Bubble(getBubbleX(r, c), getBubbleY(r),
                        bubbleRadius, random.nextInt(colorsForLevel));
            }
        }
    }

    /** Pushes every row down and adds a fresh row of bubbles at the top. */
    public void addRow() {
        for (int r = MAX_ROWS - 1; r > 0; r--) {
            grid[r] = grid[r - 1];
        }
        grid[0] = new Bubble[COLS];
        for (int c = 0; c < COLS; c++) {
            grid[0][c] = new Bubble(getBubbleX(0, c), getBubbleY(0),
                    bubbleRadius, random.nextInt(colorsForLevel));
        }
        updatePositions();
    }

    private void updatePositions() {
        for (int r = 0; r < MAX_ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != null) {
                    grid[r][c].x = getBubbleX(r, c);
                    grid[r][c].y = getBubbleY(r);
                }
            }
        }
    }

    /** Odd rows are shifted half a bubble across, which makes the hex layout. */
    public float getBubbleX(int row, int col) {
        float offset = (row % 2 == 0) ? 0 : bubbleRadius;
        return startX + bubbleRadius + col * horizontalSpacing + offset;
    }

    public float getBubbleY(int row) {
        return startY + bubbleRadius + row * verticalSpacing;
    }

    /** Finds the closest free cell to a flying bubble, or null if none is near. */
    public int[] snapToGrid(Bubble bubble) {
        int bestRow = -1, bestCol = -1;
        float bestDist = Float.MAX_VALUE;

        for (int r = 0; r < MAX_ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != null) continue;
                float gx = getBubbleX(r, c);
                float gy = getBubbleY(r);
                float dist = (float) Math.sqrt(
                        (bubble.x - gx) * (bubble.x - gx) + (bubble.y - gy) * (bubble.y - gy));
                if (dist < bestDist) {
                    bestDist = dist;
                    bestRow = r;
                    bestCol = c;
                }
            }
        }
        if (bestDist < bubbleRadius * 2.5f && bestRow >= 0) {
            return new int[]{bestRow, bestCol};
        }
        return null;
    }

    /** Breadth-first search for all touching bubbles of the same colour. */
    public List<int[]> findMatches(int row, int col) {
        List<int[]> matches = new ArrayList<>();
        if (grid[row][col] == null) return matches;

        int targetColor = grid[row][col].colorIndex;
        boolean[][] visited = new boolean[MAX_ROWS][COLS];
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{row, col});
        visited[row][col] = true;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            matches.add(curr);
            for (int[] n : getNeighbors(curr[0], curr[1])) {
                int nr = n[0], nc = n[1];
                if (nr >= 0 && nr < MAX_ROWS && nc >= 0 && nc < COLS
                        && !visited[nr][nc] && grid[nr][nc] != null
                        && grid[nr][nc].colorIndex == targetColor) {
                    visited[nr][nc] = true;
                    queue.add(n);
                }
            }
        }
        return matches;
    }

    /** Every bubble inside the blast radius of a bomb. */
    public List<int[]> findBombTargets(int row, int col) {
        List<int[]> targets = new ArrayList<>();
        float bx = getBubbleX(row, col);
        float by = getBubbleY(row);
        float blast = bubbleRadius * 4.5f;

        for (int r = 0; r < MAX_ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] == null) continue;
                float dx = grid[r][c].x - bx;
                float dy = grid[r][c].y - by;
                if ((float) Math.sqrt(dx * dx + dy * dy) <= blast) {
                    targets.add(new int[]{r, c});
                }
            }
        }
        return targets;
    }

    /** Bubbles no longer connected to the top row fall on their own. */
    public List<int[]> findFloating() {
        boolean[][] connected = new boolean[MAX_ROWS][COLS];
        Queue<int[]> queue = new LinkedList<>();

        for (int c = 0; c < COLS; c++) {
            if (grid[0][c] != null) {
                connected[0][c] = true;
                queue.add(new int[]{0, c});
            }
        }
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            for (int[] n : getNeighbors(curr[0], curr[1])) {
                int nr = n[0], nc = n[1];
                if (nr >= 0 && nr < MAX_ROWS && nc >= 0 && nc < COLS
                        && !connected[nr][nc] && grid[nr][nc] != null) {
                    connected[nr][nc] = true;
                    queue.add(n);
                }
            }
        }

        List<int[]> floating = new ArrayList<>();
        for (int r = 0; r < MAX_ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != null && !connected[r][c]) {
                    floating.add(new int[]{r, c});
                }
            }
        }
        return floating;
    }

    /** The six touching cells in a hex grid. */
    public List<int[]> getNeighbors(int row, int col) {
        List<int[]> neighbors = new ArrayList<>();
        neighbors.add(new int[]{row, col - 1});
        neighbors.add(new int[]{row, col + 1});
        int offset = (row % 2 == 0) ? -1 : 0;
        neighbors.add(new int[]{row - 1, col + offset});
        neighbors.add(new int[]{row - 1, col + offset + 1});
        neighbors.add(new int[]{row + 1, col + offset});
        neighbors.add(new int[]{row + 1, col + offset + 1});
        return neighbors;
    }

    public boolean isBubbleAtBottom(float dangerY) {
        for (int r = 0; r < MAX_ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != null && grid[r][c].y + bubbleRadius > dangerY) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isGridClear() {
        for (int r = 0; r < MAX_ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != null) return false;
            }
        }
        return true;
    }

    public List<Bubble> getAllBubbles() {
        List<Bubble> list = new ArrayList<>();
        for (int r = 0; r < MAX_ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != null) list.add(grid[r][c]);
            }
        }
        return list;
    }
}
