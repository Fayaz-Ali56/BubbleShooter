package com.fayazali.bubbleshooter;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Saves the top five scores and how far the player has unlocked,
 * using SharedPreferences so progress survives closing the app.
 */
public class ScoreManager {

    private static final String PREFS = "BubbleScores";
    private static final int MAX = 5;

    private final SharedPreferences prefs;

    public ScoreManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Adds a score, re-sorts, and keeps only the best five. */
    public void saveScore(int score, int level) {
        List<int[]> existing = new ArrayList<>();
        for (int i = 0; i < MAX; i++) {
            int s = prefs.getInt("score_" + i, -1);
            int l = prefs.getInt("level_" + i, 1);
            if (s >= 0) existing.add(new int[]{s, l});
        }
        existing.add(new int[]{score, level});

        // simple descending sort by score
        for (int i = 0; i < existing.size() - 1; i++) {
            for (int j = i + 1; j < existing.size(); j++) {
                if (existing.get(j)[0] > existing.get(i)[0]) {
                    int[] tmp = existing.get(i);
                    existing.set(i, existing.get(j));
                    existing.set(j, tmp);
                }
            }
        }

        // rewrite the table, keeping the unlock progress intact
        int unlocked = getHighestUnlockedLevel();
        int current = getSavedLevel();

        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        for (int i = 0; i < Math.min(existing.size(), MAX); i++) {
            editor.putInt("score_" + i, existing.get(i)[0]);
            editor.putInt("level_" + i, existing.get(i)[1]);
        }
        editor.putInt("unlocked_level", unlocked);
        editor.putInt("current_level", current);
        editor.apply();
    }

    public int getHighScore() {
        int high = 0;
        for (int i = 0; i < MAX; i++) {
            int s = prefs.getInt("score_" + i, 0);
            if (s > high) high = s;
        }
        return high;
    }

    public void unlockLevel(int level) {
        if (level > getHighestUnlockedLevel()) {
            prefs.edit().putInt("unlocked_level", level).apply();
        }
    }

    public int getHighestUnlockedLevel() {
        return prefs.getInt("unlocked_level", 1);
    }

    public void saveCurrentLevel(int level) {
        prefs.edit().putInt("current_level", level).apply();
    }

    public int getSavedLevel() {
        return prefs.getInt("current_level", 1);
    }

    public List<String> getTopScores() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < MAX; i++) {
            int s = prefs.getInt("score_" + i, -1);
            int l = prefs.getInt("level_" + i, 1);
            if (s >= 0) result.add((i + 1) + ".   " + s + " pts   Lv " + l);
        }
        return result;
    }
}
