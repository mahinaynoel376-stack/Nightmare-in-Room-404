# The project structure — a step-by-step guide

The project grew one feature at a time, and its folders showed it:
- old images nothing used any more
- a package name with capital letters
- one package that mixed the tile engine with the map designs
- compiled files saved in git
- tests that only existed in a temporary folder

This cleanup deleted what nothing uses and moved everything into **the layout most Java projects use**.

**The game itself didn't change.** Every map was rendered before and after the move, and all 33 pictures are pixel-identical.

---

## The project at a glance

```
GameProd/
├── src/
│   ├── main/
│   │   ├── java/                      the game's code
│   │   │   ├── module-info.java
│   │   │   └── nightmare404/
│   │   │       ├── main/      Main, GamePanel           the window, the game loop, the camera, the keys
│   │   │       ├── entity/    Player                    things that move
│   │   │       ├── ui/        BottomUI                  what's drawn on top of the world
│   │   │       ├── tile/      TileSet, TileMap,         the tile engine: loading, drawing, collision
│   │   │       │              MapBuilder, Staircase
│   │   │       ├── map/       Classroom, Hallway, ...   the map designs: what each place looks like
│   │   │       └── world/     World, Floor, Room        the linked lists: how places connect
│   │   └── resources/                 the game's images
│   │       ├── characters/player/     the 12 player sprites
│   │       └── tiles/                 classroom, stairs, library, lab, ground, quad
│   └── test/
│       └── java/nightmare404/
│           ├── checks/        play-through checks (run-checks.bat runs them)
│           └── debug/         tools that save pictures or print measurements
├── docs/                      the devlog, the guides, and their pictures (one folder per guide)
├── tools/art/                 the Python scripts that made the tiles
├── build-jar.bat              builds dist/NightmareInRoom404.jar
├── build-exe.bat              builds dist/NightmareInRoom404-windows.zip (the game plus its own Java, for sharing)
├── run-checks.bat             compiles everything and runs every check
├── README.md                  the front page: what the game is and how to run it
├── .gitignore                 what git should not keep
└── .classpath, .project, .settings/    Eclipse's project settings
```

Folders you'll also see but that are **made by the computer** (and ignored by git):

| Folder | Made by | Holds |
|---|---|---|
| `bin/` | Eclipse and VS Code | the compiled game (`bin/main`) and tests (`bin/test`) |
| `build/` | `build-jar.bat`, `run-checks.bat` | their compiled classes |
| `dist/` | `build-jar.bat` | the finished JAR |

Delete any of them and they'll simply be made again.

---

## Step 1 — Deleting what nothing uses

**How to tell a file is unused:** search every `.java`, `.md` and `.bat` file for its name. If nothing mentions it, nothing loads it.

That found 16 files:

| Files | Why they were there |
|---|---|
| `res/tiles/floor.png`, `bottom wall-export.png`, `left wall-export.png`, `right wall-export.png`, `topt wall-export.png` | the first tiles, from before the classroom set |
| `res/tiles/Classroom/` (7 images) | the first classroom set, replaced by `Classroom2` (now `classroom`) |
| 4 screenshots in `docs/images/` | pictures taken for the guides but never shown in them |

The devlog had listed the old tiles as "can be deleted" since the hallway was built. They were also being packed into every JAR for nothing.

---

## Step 2 — The standard folder layout

Most Java projects put their files in the same places. This layout comes from **Maven**, a popular Java build tool, but it's used far beyond Maven: Gradle, IntelliJ, VS Code and Eclipse all recognise it.

| Folder | What goes in it |
|---|---|
| `src/main/java` | the game's code |
| `src/main/resources` | files the game loads while running: images, and later sounds and fonts |
| `src/test/java` | code that tests the game, and isn't part of it |

**Why bother?**
- Anyone who has seen one Java project knows where everything is in yours. That includes you in six months, a teammate, or a teacher.
- Tools can find things on their own. For example, `build-jar.bat` packs only `src/main`, so the tests never end up in the game players download.
- If you ever switch to Maven or Gradle, the folders are already where they expect.

This project **uses the layout without using Maven**. There's nothing new to install: Eclipse and the two `.bat` files do the building.

### How the game finds its images

`TileSet` loads a tile like this:

```java
private static final String CLASSROOM_DIR = "/tiles/classroom/";
...
getClass().getResourceAsStream(CLASSROOM_DIR + "01_floor.png")
```

`getResourceAsStream` doesn't look in a folder on your disk. It looks on the **classpath**, the set of places Java loads classes from.

The trick is that **the contents of `src/main/resources` are copied to the root of the classpath**:
- Eclipse copies them into `bin/main`.
- `build-jar.bat` copies them into the JAR.

So `src/main/resources/tiles/classroom/01_floor.png` becomes `/tiles/classroom/01_floor.png`. The leading `/` means "from the root".

That's why the code only says `/tiles/...`. The same path works in Eclipse, in VS Code and inside the JAR.

---

## Step 3 — Package names

### The rule: all lowercase

Java's naming convention is:

| Kind of name | Style | Examples |
|---|---|---|
| package | all lowercase | `nightmare404.tile` |
| class | `UpperCamelCase` | `TileMap` |
| method or variable | `lowerCamelCase` | `cameraX()` |
| constant | `UPPER_SNAKE_CASE` | `TILE_SIZE` |

The old package `Nightmareon404` looked like a class name. That's confusing, because `Nightmareon404.Main` reads like "a class called Main inside a class called Nightmareon404".

There's also a practical reason. A package is a folder, and Windows and macOS don't care about upper and lower case in folder names. Two names that differ only in case can collide.

### One root: `nightmare404`

Every package now starts with `nightmare404.`, so the game's code can't clash with anyone else's.

The strictest convention starts from a web address you own, written backwards: `io.github.<your-name>.nightmare404`. For a game that only you build, the short root is plenty.

| Package | Classes | Its job |
|---|---|---|
| `nightmare404.main` | `Main`, `GamePanel` | starts the game; the window, the game loop, the camera, the keys |
| `nightmare404.entity` | `Player` | things that move around the world (enemies will go here too) |
| `nightmare404.ui` | `BottomUI` | what's drawn on top of the world |
| `nightmare404.tile` | `TileSet`, `TileMap`, `MapBuilder`, `Staircase` | **the tile engine**: loading tiles, building maps, drawing them, collision |
| `nightmare404.map` | `Classroom`, `Hallway`, `Stairs`, `Library`, `Lab`, `Office`, `Ground`, `Quadrangle`, `SingleRoom` | **the map designs**: what each place looks like |
| `nightmare404.world` | `World`, `Floor`, `Room` | the linked lists: how the places connect |

### Why `tile` was split in two

The old `tile` package held 13 classes doing two very different jobs.

**The engine** (`TileMap`, `MapBuilder`, ...) is general: it would work for any tile game. **The map designs** (`Classroom`, `Quadrangle`, ...) are this game's content.

Splitting them shows that difference. It also makes a rule visible: **the engine never mentions a map design**. The designs use the engine, and never the other way round.

When you add a new place, it goes in `map`. Only a new *ability* (like the 2D camera or `edgeWallH`) touches `tile`.

### What the split changed in the code

**1. Imports.** Classes in the same package see each other for free. Now that `Quadrangle` (in `map`) and `MapBuilder` (in `tile`) are in different packages, `Quadrangle` has to import it:

```java
package nightmare404.map;

import static nightmare404.tile.TileMap.*;   // TileMap's constants: COLS, ROWS, LAYER_OBJECTS, ...
import static nightmare404.tile.TileSet.*;   // the tile IDs: TREE, FENCE_H, BUS_FIRST, ...

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

import nightmare404.tile.MapBuilder;          // the classes themselves
import nightmare404.tile.TileMap;
import nightmare404.tile.TileSet;
```

A common surprise is that **`import static` and `import` do different jobs**:
- `import static nightmare404.tile.TileSet.*` brings in TileSet's static **members**, so you can write `TREE` instead of `TileSet.TREE`.
- It does **not** bring in the name `TileSet` itself. So a method that takes a `TileSet tiles` parameter also needs the plain `import nightmare404.tile.TileSet;`.

The imports are also in the usual order, with a blank line between groups:
1. static imports
2. Java's own (`java.*`, `javax.*`)
3. the project's own (`nightmare404.*`)

**2. Two fields became `public`.** A field with no `public`/`private` in front is **package-private**: only classes in the same package can use it.

`BottomUI` (now in `ui`) reads the player's health through `gp.flor`, and the checks read `gp.tiles`. So in `GamePanel` those two fields became `public`:

```java
// Entities (public: the UI and the checks in other packages read the player)
public Player flor;
...
public TileSet tiles;
```

Nothing else needed changing. The compiler lists every place that can't see something it needs, so moving packages is safe: if it compiles, nothing was missed.

### The resource folders are lowercase too

`Characters/Player` became `characters/player`, `tiles/Classroom2` became `tiles/classroom`, and so on. Inside a JAR, names are case-sensitive even on Windows, so one consistent style avoids "works in Eclipse, image missing in the JAR" bugs.

---

## Step 4 — The module

`src/main/java/module-info.java` makes the game a **Java module**: a named bundle of packages that says which parts of Java it needs.

```java
module nightmareinroom {
    requires java.desktop;   // Swing and AWT: the window, drawing, images
}
```

Eclipse created this file (named `GameProd`) when the project was made. It was renamed, and the empty comment blocks were removed.

**Why not `nightmare404`?** The Java compiler warns about module names that end in digits, because the digits look like a version number. The packages keep `nightmare404`, since only *module* names get that warning.

---

## Step 5 — Tests next to the code

The play-through checks used to live in a temporary folder that would eventually be deleted. Now they're part of the project, in `src/test/java/nightmare404/checks`:

| Check | What it plays through | Tests |
|---|---|---|
| `WorldWalkCheck` | Floor 4: out of Room 404, along the hallway, every door, both end walls, the linked list's links | 30 |
| `CameraCheck` | walks the whole Floor 4 hallway: the camera never jumps between screens | 5 |
| `StairsEntryCheck` | the stairs can only be entered from their open end | 8 |
| `Floor3Check` | the stairs down to Floor 3, the hallway, the library | 28 |
| `Floor2Check` | the stairs to Floor 2, the hallway, every lab door | 38 |
| `Floor1Check` | the ground floor: stairs, offices, grass, both exits | 33 |
| `ViewCheck` | the view over every railing shows exactly the floor below | 15 |
| `QuadCheck` | the quadrangle: in and out, the camera, all three strongholds, the perimeter | 39 |
| `DoorSafetyCheck` | a door link to a map without a door is refused at start-up | 2 |

That's **198 tests**. A few needed small changes when they moved:
- `CameraCheck` and `DoorSafetyCheck` used to print measurements. Now they say `ok` or `FAIL`.
- The first part of `WorldWalkCheck` compared Room 404 against a file from the very first version that no longer exists, so that part was removed.

`src/test/java/nightmare404/debug` holds the tools that **show** you things instead of passing or failing:

| Tool | What it does |
|---|---|
| `MapPreviewDebug` | prints the floor plan and saves a picture of every map (33 of them) |
| `WalkStripDebug` | renders the down-walk animation into one strip and checks the head never flips |
| `PerfDebug` | measures start-up time and how long one frame takes to draw |

### Running the checks

Double-click `run-checks.bat`. It:
1. compiles the game and the checks into `build/test/classes`
2. copies the images in
3. runs every class in the `checks` folder, one after another

It finishes with a line like:

```
9 checks passed, 0 failed.
```

The script finds the checks by their **file names**, so a new check needs no other setup. Put it in the `checks` folder and the next run includes it.

### How a check reports failure: the exit code

Every program ends with an **exit code**, a number it hands back to whatever started it. `0` means "all good", and anything else means "something failed". Each check ends with:

```java
System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");
System.exit(fails == 0 ? 0 : 1);
```

`run-checks.bat` reads that number after each check:

```bat
for %%f in (src\test\java\nightmare404\checks\*.java) do (
    ...
    java -Djava.awt.headless=true -cp build\test\classes nightmare404.checks.%%~nf
    if errorlevel 1 (set /a failed+=1) else (set /a passed+=1)
)
```

The `%%~nf` gives the file name without `.java`, so `QuadCheck.java` runs `nightmare404.checks.QuadCheck`.

`-Djava.awt.headless=true` tells Java there's no screen, so the checks can't accidentally open a window.

If any check fails, the script itself exits with `1`. Tools like GitHub Actions can use that to tell you a change broke something.

### Running one check or tool on its own

- **In Eclipse:** right-click the file → *Run As → Java Application*.
- **From a terminal**, after `run-checks.bat` has compiled everything:
  ```
  java -Djava.awt.headless=true -cp build/test/classes nightmare404.debug.MapPreviewDebug map_previews
  ```

---

## Step 6 — The build script

`build-jar.bat` works as before, with three changes:
- it compiles only `src\main\java`, so no tests go into the game
- it copies `src\main\resources` (not `res`)
- the entry point is `nightmare404.main.Main`

The JAR is now called **`dist/NightmareInRoom404.jar`**, after the game's title and the GitHub repository.

### Sharing with people who don't have Java: `build-exe.bat`

A JAR only runs if Java 21 is installed. `build-exe.bat` removes that requirement:
1. It builds the JAR first.
2. **`jpackage`**, a tool that comes with the JDK, wraps the JAR with a cut-down Java runtime and a `NightmareInRoom404.exe` launcher.
3. It zips the result.

```bat
jpackage --type app-image --name NightmareInRoom404 --app-version 1.0 ^
    --input build\package\input --main-jar NightmareInRoom404.jar --main-class nightmare404.main.Main ^
    --add-modules java.desktop --dest build\package\out
```

- `--type app-image` makes a **ready-to-run folder**. The other types (`exe`, `msi`) make an installer, but those need the WiX Toolset installed, and a folder in a zip is simpler to hand out.
- `--add-modules java.desktop` keeps only the parts of Java the game uses (Swing and images). That's what keeps the zip at about 25 MB.
- The `^` at the end of a line means "this command continues on the next line".

The result, `dist/NightmareInRoom404-windows.zip`, runs on any Windows PC without Java. [README.md](../README.md) has the instructions to send along with it.

When the game runs, you'll see **two** `NightmareInRoom404` processes in Task Manager: the launcher, and the one with the window. That's normal.

---

## Step 7 — Git: what to keep, what to ignore

Git should keep **what a person made**: code, images, docs, scripts. It shouldn't keep **what a computer can make again**.

| Kept | Ignored |
|---|---|
| `src/`, `docs/`, `tools/` | `bin/`, `build/` (compiled classes) |
| `build-jar.bat`, `run-checks.bat`, `README.md` | `dist/` (the JAR) |
| `.classpath`, `.project`, `.settings/` (so Eclipse can import the project) | `__pycache__/`, `.vscode/`, `Thumbs.db`, ... |

**Why not commit the compiled files?**
- **Noise:** every rebuild changes dozens of `.class` files, so the history fills up with changes nobody made by hand.
- **Size:** git keeps every version forever, and binary files are expensive to keep.
- **Truth:** a `.class` file can be out of date with its `.java` file, and git can't tell.

The **JAR** is the thing to share with players. On GitHub, attach it to a **Release** (*Releases → Draft a new release*) instead of committing it.

### `.gitignore`

`.gitignore` lists what git should pretend isn't there:

```
bin/
build/
*.class
dist/
```

A name ending in `/` means "this folder and everything in it". A `*` matches anything, so `*.class` means every compiled class, wherever it is.

Before this cleanup, `bin/`, `build/` and `dist/` were already being tracked. `.gitignore` only stops git noticing **new** files, so they were also removed from tracking with:

```
git rm -r --cached bin build dist
```

`--cached` means "stop tracking these, but **leave them on my disk**".

### Renames keep their history

Files that git already knew about were moved with `git mv`. When you commit, git shows them as **renamed** rather than deleted and re-added, so `git log --follow` still finds their history.

### Committing the cleanup

Nothing has been committed yet. To review and commit it:

```
git add -A
git status
git commit -m "Reorganise into the standard Java project layout"
```

- `git add -A` stages everything, including the deletions and moves.
- `git status` shows what's about to go in. Check `bin/`, `build/` and `dist/` aren't listed.

---

## Step 8 — The docs and the art scripts

**Docs:**
- Each guide's pictures are now in their own folder: `docs/images/hallway/`, `docs/images/quadrangle/`, and so on.
- Every link in the guides was updated, and a script checked that each one points at a real file.
- The devlog's past entries keep the old paths on purpose. They describe what was true at the time, and the newest entry has a full before → after table.

**Art scripts:** the Python scripts that cut and drew the tiles are in `tools/art/`. Its [README](../tools/art/README.md) gives the exact command for each one. Rerunning them reproduces every tile they made, pixel for pixel.

---

## Step 9 — After the move: Eclipse and VS Code

The IDEs remember the old folders, so give them a nudge.

**Eclipse:**
1. Right-click the project → **Refresh** (or select it and press F5).
2. **Project → Clean...** → Clean.
3. Open `src/main/java/nightmare404/main/Main.java` → right-click → *Run As → Java Application*.

Your old run configuration still points at `Nightmareon404.Main`, so using it gives a "could not find or load main class" error. Delete it in *Run → Run Configurations...*.

**VS Code:** if it shows red errors such as *"The declared package nightmare404.main does not match the expected package main.java.nightmare404.main"*, it's still using the old source folder:
1. Press Ctrl+Shift+P.
2. Run **Java: Clean Java Language Server Workspace**.
3. Choose **Restart**.

---

## How it was checked

| Check | Result |
|---|---|
| All 9 checks, 198 tests (`run-checks.bat`) | ✅ all pass |
| A deliberately failing check | ✅ counted as failed; the script exits with 1 |
| All 33 maps and the walk strip, rendered with **only the new JAR** on the classpath, compared to before the move | ✅ pixel-identical |
| The JAR opens the game window | ✅ no errors |
| The JAR's contents | ✅ 213 tile images, 12 sprites, no test code, entry point `nightmare404.main.Main` |
| The art scripts rerun | ✅ all 177 tiles they make are identical to the game's |
| Every link in the docs | ✅ points at a real file |

---

## Where things are now

| I want to... | Go to |
|---|---|
| change a room's layout | `src/main/java/nightmare404/map/` |
| add a tile | put the PNG in `src/main/resources/tiles/<set>/`, then give it an ID and add its file name in `TileSet` |
| change how the player moves | `src/main/java/nightmare404/entity/Player.java` |
| change the camera | `cameraX()` / `cameraY()` in `src/main/java/nightmare404/main/GamePanel.java` |
| connect a new room | `src/main/java/nightmare404/world/World.java` |
| add a check | a new class with a `main` method in `src/test/java/nightmare404/checks/` that exits with `1` on failure |
| see every map without playing | run `MapPreviewDebug` |
| build the game for players | double-click `build-exe.bat` (no Java needed to play) or `build-jar.bat` (needs Java 21) |
