# Setup Guide

This folder has every file for the Bubble Shooter game.

There are two ways to get it running. **Method 1 is the safe one** — use that.

## Method 1: Fresh project, then copy the code in (recommended)
This avoids all the Gradle version problems, because Android Studio
generates the build files itself and they will match whatever version
you have installed.

## Method 2: Open this folder directly

If you want to try this instead: open Android Studio, choose
**Open**, and select the `BubbleShooter` folder.

The build files (`build.gradle.kts`, `settings.gradle.kts`) are included and
set to Android Gradle Plugin 8.7.3. If your Android Studio is newer or
older than that, it will offer to upgrade or complain about a version
mismatch. Let it upgrade if it offers to.

The `gradlew` wrapper files are **not** included here, because one of them is
a binary file that cannot be shared as text. Android Studio regenerates them
automatically the first time it syncs, so this is usually not a problem —
but if the sync fails, fall back to Method 1.

---

## What is in each file

| File | What it does |
|------|--------------|
| `MainActivity.java` | Entry point, goes fullscreen, loads the game view |
| `GameView.java` | All screens, drawing, touch input, game state machine |
| `GameThread.java` | The 60 FPS game loop |
| `Bubble.java` | One bubble: its colour, drawing, and pop animation |
| `Shooter.java` | Aiming, the reflecting aim line, firing, swapping |
| `GridManager.java` | The hex grid, match finding, floating bubbles, bombs |
| `ScoreManager.java` | Saves top 5 scores and which levels are unlocked |

All of this code has been compiled and checked before packaging, so there
are no missing brackets or cut-off lines.

---

## Changing name in the game

My name appears in a few places in `GameView.java`. Search for
`Fayaz Ali` in that file and you will find all of them:

- The start screen: `"Created by Fayaz Ali"`
- A faint watermark at the bottom during play: `"by Fayaz Ali"`
- The game over screen: `"by Fayaz Ali"`
- The copyright line: `"(c) Fayaz Ali | All Rights Reserved"`

---

## About the app icon

The icon is not included in this folder,design icon according to your interest.
1. Right click the `res` folder in Android Studio
2. **New -> Image Asset**
3. Icon Type: **Launcher Icons (Adaptive and Legacy)**
4. Asset Type: **Image**, then pick your PNG file
5. Adjust the scaling slider, click **Next**, then **Finish**

Until you do that, the app will use Android's default green icon. The game
itself will still run fine.
