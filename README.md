# Bubble Shooter

A 2D bubble shooter game for Android, written from scratch in Java using
Canvas and SurfaceView. No game engine, no third-party game libraries.

**Created by Fayaz Ali**

## Features

- 200 levels with a locked / unlocked progression system
- 3 lives per level, so one mistake does not send you back to level 1
- Reflecting aim line that bounces off the walls for bank shots
- Bubble swap: switch your current and next bubble at any time
- Bomb power-ups gifted every 10 levels, with an area blast
- Falling and fading animation when bubbles pop
- Floating bubbles drop automatically when their support is destroyed
- Top 5 high scores saved on the device
- Built-in "How to Play" guide
- Progress saved with SharedPreferences

## Built With

- Java
- Android SDK (Canvas + SurfaceView)
- A custom game loop running at 60 FPS on its own thread
- SharedPreferences for persistence

## Project Structure

| File | What it does |
|------|--------------|
| `MainActivity.java` | Entry point, sets fullscreen and loads the game view |
| `GameView.java` | All screens, rendering, touch input, game state machine |
| `GameThread.java` | The game loop and frame timing |
| `Bubble.java` | Bubble data, drawing, and the pop / fall animation |
| `Shooter.java` | Aiming, the reflecting aim line, firing, and swapping |
| `GridManager.java` | Hex grid, match detection, floating removal, bomb radius |
| `ScoreManager.java` | High scores and level unlock progress |

## How to Run

1. Clone this repository
2. Open the folder in Android Studio
3. Let Gradle sync (minimum SDK is API 23 / Android 6.0)
4. Press Run with a device connected or an emulator started

## Author

Fayaz Ali
