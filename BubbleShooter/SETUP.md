# Setup Guide

Wa Alaikum Assalam Fayaz. This folder has every file for the Bubble Shooter game.

There are two ways to get it running. **Method 1 is the safe one** — use that.

---

## Method 1: Fresh project, then copy the code in (recommended)

This avoids all the Gradle version problems, because Android Studio
generates the build files itself and they will match whatever version
you have installed today.

### Step 1 — Create the project

1. Open Android Studio, click **New Project**
2. Choose **Empty Views Activity**, click **Next**
3. Fill in exactly:

| Field | Value |
|---|---|
| Name | BubbleShooter |
| Package name | `com.fayazali.bubbleshooter` |
| Language | **Java** (not Kotlin) |
| Minimum SDK | **API 23** |

4. Click **Finish** and wait for it to load

The package name must match exactly, or the `package` line at the top of
every Java file will be wrong and nothing will compile.

### Step 2 — Copy the 7 Java files

In the left panel, switch the dropdown at the top to **Android**, then open:

```
app -> java -> com.fayazali.bubbleshooter
```

You will see `MainActivity.java` sitting there. That is the correct folder —
the one you need. Not the one labelled `(androidTest)` and not the one
labelled `(test)`. Those two are for automated testing and code put there
will not compile into your app. This is the mistake that cost you time before.

Now, the easiest way to get the files in:

1. Open File Explorer and go to this folder:
   `BubbleShooter/app/src/main/java/com/fayazali/bubbleshooter/`
2. Select all 7 `.java` files
3. Press **Ctrl + C**
4. Back in Android Studio, click once on the `com.fayazali.bubbleshooter`
   package folder to select it
5. Press **Ctrl + V**
6. Click **OK** / **Overwrite** when it asks

That copies everything in one go, including replacing `MainActivity.java`.

### Step 3 — Replace the manifest

1. In Android Studio open `app -> manifests -> AndroidManifest.xml`
2. Press **Ctrl + A**, then **Delete**
3. Open `app/src/main/AndroidManifest.xml` from this folder in Notepad
4. Copy everything, paste it into Android Studio
5. Press **Ctrl + S**

### Step 4 — Check minSdk is 23

1. Open `Gradle Scripts -> build.gradle.kts (Module :app)`
2. Find the line `minSdk = ` and make sure it says **23**
3. If you changed it, click **Sync Now** in the yellow bar that appears

### Step 5 — Run it

Connect your phone with USB debugging on, or start an emulator, then
press the **Run** button.

---

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
are no missing brackets or cut-off lines this time.

---

## Changing your name in the game

Your name appears in a few places in `GameView.java`. Search for
`Fayaz Ali` in that file and you will find all of them:

- The start screen: `"Created by Fayaz Ali"`
- A faint watermark at the bottom during play: `"by Fayaz Ali"`
- The game over screen: `"by Fayaz Ali"`
- The copyright line: `"(c) Fayaz Ali | All Rights Reserved"`

---

## About the app icon

The icon is not included in this folder, because image files cannot be
shared as text. To add your Sindh flag bubble icon back:

1. Right click the `res` folder in Android Studio
2. **New -> Image Asset**
3. Icon Type: **Launcher Icons (Adaptive and Legacy)**
4. Asset Type: **Image**, then pick your PNG file
5. Adjust the scaling slider, click **Next**, then **Finish**

Until you do that, the app will use Android's default green icon. The game
itself will still run fine.

---

## Uploading to GitHub

The `.gitignore` and `README.md` are already written and included.

1. Install **GitHub Desktop** from desktop.github.com and sign in
2. **File -> Add Local Repository**, choose the BubbleShooter folder
3. It will offer to **create a repository** — click that link
4. Check the file list on the left: `build/` and `local.properties` should
   **not** appear. If they do, the `.gitignore` is not in the right place
5. Write a summary like `Initial commit`, click **Commit to main**
6. Click **Publish repository**

To share the APK with friends without WhatsApp corrupting it, use
**Releases** on your GitHub repo page and attach the `.apk` file there.
