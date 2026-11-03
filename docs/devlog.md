# Nightmare in Room 404 — Development log

A running record of how the game is being made: what was built, what went wrong, and why each decision was taken. It's the raw material for a full "making of" documentary once the game is finished, so **add an entry every time you work on the game**, even a short one.

Template for new entries:

```
## YYYY-MM-DD — Title
**Goal:** what you set out to do
**What happened:** what was built or changed (files)
**Problems:** what went wrong, what the cause turned out to be, how it was fixed
**Decisions:** choices made and why (and alternatives you rejected)
**Open issues:** anything left for later
```

---

## Where the project started

- **Language and tools:** Java (Swing), developed in Eclipse and VS Code; pixel art in Aseprite.
- **Screen:** 25 × 19 tiles of 32 px (800 × 608). The width is 800 because 800 ÷ 32 = 25 exactly; the height is 608 rather than the "standard" 600 because 600 ÷ 32 = 18.75 isn't a whole number of tiles.
- **Player:** sprites are 32 × 64 (1 tile wide, 2 tall), with 12 frames (up/down/left/right × idle/walk1/walk2). Speed is 3 px per frame at 60 FPS. The hitbox is a 16 × 16 box at the feet so the player fits through 1-tile aisles.
- **World:** a grid of rooms identified by `(mapX, mapY)`, with one hand-placed classroom (Room 404) and open "plain rooms" everywhere else.

---

## 2026-10-29 — Startup error hunt

**Goal:** find out why the game reported an error.
**What happened:** the code compiled cleanly. Running it without the `res` folder on the classpath crashed with `IllegalArgumentException: input == null!` from `ImageIO.read`: `getResource(...)` returns `null` when an image can't be found.
**Decisions:** confirmed that `res` is registered as a source folder in Eclipse, so the images do load there. Image loading was later changed to check for `null` and report the missing file by name.

## 2026-10-29 — The down-walk animation, part 1: a frame that wasn't

**Goal:** fix the down-walk animation, which seemed to "flip" on `down_walk2`.
**Problems:** a pixel comparison showed `down_walk2.png` was almost a copy of `down_walk1.png` shifted 3 px left: the same leg forward, in a different place. In game, the character slid sideways and repeated one stride.
**What happened:** `down_walk2` was rebuilt from `down_walk1` with the legs (rows 45–63) mirrored around the shirt's centre line, so the other leg steps forward and the head and coat stay put.

## 2026-10-29 — Re-exported sprites

**What happened:** 5 sprites came back from Aseprite with "(1)" in their names (`down_walk1 (1).png` and others), so the game couldn't find them. They were renamed back. The new `down_walk1` had been moved 3 px to line up with `down_idle`, so `down_walk2` was rebuilt again from it.
**Lesson:** when re-exporting, overwrite the existing file instead of saving a copy.

## 2026-10-29 — The down-walk animation, part 2: the head flip

**Goal:** the head and hair still flipped left/right while walking down. Fix it **in code**, without touching the PNGs.
**Problems:** the checklist covered draw-time flipping, the files loaded, the frame sequence and the draw position. None of them had a bug. The cause was the art: `down_idle.png` has a different head (the fringe parts on the opposite side), and the walk cycle `walk1 → idle → walk2 → idle` showed it every other frame.
**What happened:** each direction got its own walk cycle. Down uses `walk1, walk1, walk2, walk2`, skipping the idle frame while keeping the same pace. `WalkStripDebug` was added: it renders the 4 down frames through the real draw code and checks the head is identical (241 differing pixels before, 0 after).
**Open issues:** standing still still shows `down_idle`'s different head. The proper fix is to redraw that head to match the walk frames.

## 2026-10-29 — Room 404 from Aseprite tiles

**Goal:** replace the code-drawn classroom with a layout made from a new 36-tile set (`res/tiles/Classroom2`) and the provided `ClassroomMap` design.
**What happened:** the classroom became 4 layers (floor, vertical walls, horizontal walls, objects) plus a hand-written SOLID grid, loaded from the classpath. Both bottom doors led out at first; then the broken door was made solid on request.
**Decisions:** the layout's 25 × 18 map fits the 25 × 19 screen, leaving a 32 px strip at the bottom. Making the empty space above and below equal (79 px vs 64 px, caused by a 15 px margin in the top wall art) was considered and left as is.

## 2026-10-29 — Floor 4 and the linked list

**Goal:** add two more classrooms, a hallway based on the mockups, stairs at both ends (the east one destroyed), and connect everything with a **linked list**.
**What happened:**
- **Rooms 402 and 403:** 403 faces left (mirrored furniture made in code), and 402 has a different desk layout and different broken doors. All classrooms share one "shell" and differ only in their furniture grid.
- **Hallway:** 3 screens built from the tile set, following the mockups: front wall with doors and windows, pillars between rooms, a corridor, a railing, and the floor below shown darkened.
- **Stairs:** 6 new tiles (landing, step, rail, broken step, hole, rubble) drawn in the same palette. The west stairwell works (no function yet); the east one is caved in.
- **Linked list:** each screen is a `Room` node with `left`, `right` and `door` links; `Floor` is a doubly linked list (`head`, `tail`, `append`); `World` builds it. The game tracks `currentRoom` instead of `mapX`/`mapY`.
- **Collision** is now computed from the tiles instead of hand-written SOLID grids. Room 404's computed collision matched the old grid exactly.
**Problems:** the doors painted on the floor below the railing were being counted as real doors. Fixed: a door only counts if the player can stand on it.
**Decisions:** custom nodes instead of `java.util.LinkedList`, because the game needs each room to know its neighbours directly. Floors will be linked the same way through the stairs (a multilevel linked list).
**Docs:** `docs/hallway-guide.md` explains every step for beginners.

## 2026-10-29 — Fixed screens or a following camera?

**Question:** is it OK that the camera doesn't follow the player?
**Decision:** keep fixed screens for rooms. They're each composed as one screen, they hide what's ahead (good for horror), and they're simpler. But the hallway is one long corridor, so give it a camera that follows the player.
**What happened:** `GamePanel.cameraX()` centres the player but never scrolls toward a `null` link, so classrooms never scroll and the hallway stops at the building's ends. The neighbouring room is drawn through the `left`/`right` link. Crossing between hallway nodes shifts `x` by one screen width, so nothing jumps; tests showed at most 3 px of movement per frame. Collision at a seam checks the neighbour's tiles.

## 2026-10-29 — Health bar moves to a bottom UI bar

**Goal:** remove the health bar floating over the player's head and put it in a UI bar at the bottom of the screen. It's temporary because the UI design isn't settled; the bar can be at most one 32 px tile tall.
**What happened:**
- `Player.draw()` no longer draws the bar; it only draws the sprite.
- New class `BottomUI` draws a dark strip across the bottom row with an "HP" label, a 160 px bar and the value as text (for example "75 / 100"). It's drawn last in `GamePanel.paintComponent()`, after the camera shift is undone, so it stays fixed on screen and nothing covers it.
- `WalkStripDebug` no longer leaves 12 px above the sprite for the old bar.
**Decisions:**
- **Why the bottom row is free:** every map is 18 rows tall, but the screen is 19 rows (608 px). Row 18 (y 576–607) was the "leftover" black strip noticed earlier, and it's exactly the 32 px the UI is allowed. The odd screen height turned out to be useful.
- **Why a separate class:** the UI will grow (items, stamina, messages...). Giving it its own class now keeps `GamePanel` and `Player` from filling up with drawing code.
**Checks:** a rendered screen with health set to 75 shows the bar three-quarters full; the walk-animation and Floor 4 walking tests still pass.
**Open issues:** the room name ("Room 404 | Floor 4") is still a debug label at the top of the screen. It could move into the bottom bar when the UI is designed.

## 2026-10-29 — Stairs: one way in

**Goal:** the stairs should only be enterable from one end, not from the sides.
**What happened:**
- **Choosing the entrance:** the landing (top) touches the back wall, and its left and right are the "sides". So the one open end is the bottom of the two flights. Up one flight, across the landing, down the other: a switchback staircase.
- **Edge walls:** new in `MapBuilder.edgeWall(col, row0, row1)` and `TileMap.collides()`. It's a 4 px invisible wall on the line between two columns. `staircase()` puts one along each outer side, from the landing to the bottom step. The rail between the flights was already solid, so it blocks the middle.
**Problems:** ordinary tile collision can't express "walkable, but not from this direction". A cell is either solid or not, and making the stair cells solid would stop the player walking on them at all. Blocking the *edges* between cells solves it without changing the tiles.
**Checks:** 11 tests on both stairwells. Both sides of the landing and the flights block (from outside and from the landing). Both flights can be climbed from the bottom and walked back down. The ruined east stairs are closed at the sides too. The full Floor 4 walk test still passes.
**Open issues:** the side walls are invisible. If players find that confusing, a visible balustrade tile along the sides would show why they can't step on.

## 2026-10-29 — Is the tile drawing optimized? (No, so now it is)

**Goal:** check whether building and drawing the tiles is efficient, then make it faster in a way that stays easy to understand.
**Measured first:**
- Startup: loading all tiles took 237 ms and building all 8 maps took 33 ms (both once, both fine).
- Every frame: 2.55 ms in a classroom and 3.14 ms in the scrolling hallway, out of a 16.7 ms budget. Fine for now, but wasteful: about 600–700 tiles were redrawn one by one every frame (up to ~1,300 while scrolling), even though the maps never change. Also, a new `Font` object was made every frame.
**What happened:**
- `TileMap.draw()` now draws the whole map **once** into an image (the cache, created the first time a room is shown) and copies that image every frame after.
- The cached image is a **compatible image** (in the screen's own pixel format, opaque), so Java can copy it without converting pixels.
- `clearCache()` was added for the day a map changes during play.
- The room-name font in `GamePanel` is now a constant.
**Decisions:**
- **Planned and dropped:** converting all 57 tile images to the screen format. With the cache, each tile is only drawn once per room, so it would add code without a noticeable gain. Simpler code wins.
- **Cost:** about 1.8 MB of memory per room visited (~15 MB for all 8).
**Results:** 2.55 → **0.41 ms** per frame in a classroom, 3.14 → **0.46 ms** in the hallway (about 6–7× faster), and all 8 rooms pixel-identical to before. All earlier tests still pass. Explained step by step in `hallway-guide.md`, Step 13.

## 2026-10-29 — First playable build: a walking-game JAR

**Goal:** package the game as it is (walk around Floor 4) into a JAR that runs on its own.
**What happened:**
- New script `build-jar.bat` (double-click it). It compiles `src` for **Java 21**, copies `res` in next to the compiled classes, and packs everything into `dist\Nightmareon404.jar` with `Nightmareon404.Main` as the entry point.
- The JAR is about 70 KB.
**Decisions:**
- **Java 21, not 25:** the PC has Java 25, but the Eclipse project targets 21. Building with `--release 21` means the JAR runs on Java 21 *or newer*, so it works for more people.
- **Resources inside the JAR:** the game loads images with `getResourceAsStream("/tiles/...")`, which reads from the classpath. Putting the contents of `res` at the root of the JAR keeps those same paths working, with no code changes. This is the payoff from loading resources this way since the start.
**Checks:** ran the game's own code with **only the JAR** on the classpath (no `res`, no `bin`): every tile, the player sprites and all 8 rooms loaded (`WalkStripDebug` passed, `MapPreviewDebug` rendered all 8 rooms). The manifest names the right main class; the class files are Java 21 (version 65).
**Open issues:**
- Anyone running it needs Java 21 or newer installed. Bundling Java itself (with `jpackage`) would make a real installer later.
- The old unused images in `res/tiles/` are also packed (only a few KB).

## 2026-11-01 — Floor 3, working stairs and the library

**Goal:** a new floor below Floor 4, reached by the west (left wing) stairs. On it, a library that takes up three rooms' width, connected inside but shown screen by screen, with a single way in: a big double door from the hallway. Use a new tile sheet, and make it fit the existing floor.
**What happened:**
- **The sheet (8 × 5 tiles):** library furniture, a bookshelf, a computer, a card catalogue, a hanging lamp, and horror decals (blood, cobwebs, cracks, a handprint, glowing eyes, scratches, "SHHHHH" in blood across three tiles). 31 tiles were cut out into `res/tiles/Library/`.
- **Drawn new:** a big double door (4 × 2 tiles, stone frame like the wall strips, bloody claw marks), a complete QUIET sign, and a LIBRARY plaque, using a hand-typed 5 × 7 pixel font.
- **Floor 3:** same 5-screen hallway layout as Floor 4 (so the stairwells stack), with the library's front wall: pillars only at its ends, and the big door and plaque on the middle screen.
- **The library:** 3 screens written as **character maps** (one character per tile, with a legend) instead of number grids. Its screens are their own small linked list (`addLeftOf` / `addRightOf`), and only the middle one has a door link. A new `Room.fixedScreen` flag stops the camera scrolling there.
- **Working stairs:** `Staircase` stores two zones on the landing and two arrival points. Left flight = down, right flight = up; you arrive at the foot of the flight that leads back. `Floor.connectStairs(floor4, floor3)` links the floors. Flights to floors that don't exist yet are blocked with rubble.
**Problems:**
- **The QUIET signs on the sheet were cut off** by overlapping tiles (one missing its top half, one its bottom). A new complete sign was drawn instead.
- **The palettes:** the sheet's walls and floor turned out to be the same drawings as the Classroom2 tiles, recoloured warm brown. Rather than recolouring, the library keeps its warm interior (the furniture has that floor drawn in) inside the building's grey stone frame, and the hallway stays cool. The two only meet through the door, which cuts to a new screen.
- **Bug: you couldn't leave a library screen.** Keeping the player fully on screen after arriving ran every frame, so the player could never reach the edge. Fixed by doing it only on the frame you arrive. The new tests found it.
**Decisions:**
- **Character maps for the library:** with IDs up to 99, number grids became unreadable. Characters let you see the room in the code, and the loader rejects wrong row lengths or unknown characters with a clear message.
- **Refactored the hallway** into a shared `corridorScreen()`, and checked that every Floor 4 screen stayed pixel-identical (only the intended rubble changed).
- `Floor.addClassroom` was renamed `addRoomBehind`, because it's now used for the library too.
**Checks:** a new simulated play-through of the stairs (both ways, blocked flights), the hallway, the big door, all library screen changes, walls and every link. All passed, along with every earlier test. The guide is `docs/floor3-library-guide.md`.
**Open issues:** the glowing eyes and the locked staff door are just decoration for now. Floor 2 would unblock Floor 3's down flight.

## 2026-11-01 — Floor 2: the labs

**Goal:** a laboratory-themed Floor 2 built from a new lab tile sheet, with only 3 rooms, fitting the building's theme and look.
**What happened:**
- **Reading the sheet:** like the library sheet, the floor and brick are the Classroom2 drawings recoloured, this time cool teal and grey-green, close to the building's own palette. Every horror decal was pixel-identical to the library's, so those tiles are reused.
- **Tiles (`res/tiles/Lab/`, 51):** 33 cut out. *(Corrected on 2026-11-02: this entry first said 4 of them were "recoloured from the library's warm background"; a bug in my comparison code caused that. They already had the lab's background, and the recolour step changed nothing. See the Floor 1 entry.)* 1 hazard sign cut out to a transparent decal for the hallway. 17 drawn: a steel lab door, a sealed one (caution tape), a smashed pod, CHEMISTRY/BIOLOGY/CONTAINMENT plaques, and a crooked LABORATORY sign (drawn straight, then sheared). The sheet's own LABORATORY signs were cut off, like the QUIET signs before.
- **The labs:** Chemistry (benches, stools, reagent shelves, periodic table, one door sealed), Biology (specimen jars, skeleton, dissection tables), Containment (rows of pods). Each is one screen shaped like a classroom.
- **Storytelling:** in Containment one pod is smashed from the inside; glass, slime and bloody footprints lead out of the door, and the trail continues along the hallway towards the collapsed east stairs.
- **Floor 3's stairs now go both ways**; `connectStairs(floor3, floor2)`.
**Refactors:**
- `Library`'s character-map reader moved into `MapBuilder.paint(map, legend, underlay)`, so the labs share it. The legend is now a `Map`, and the underlay an interface (`Lab::underlay`). The library is pixel-identical after the move.
- `TileSet.doorSize(id)`: the one place that knows every kind of door. Adding the lab door was one word.
- The hallway copies doors from any room's finished map (`TileMap.tileAt`) instead of a classroom's number grid.
**Problems:**
- **Bug:** every lab's right-hand door was one column too far left (17–18 instead of 18–19), so the hallway got half a door. On the Containment hallway that meant no working door, and walking out of the lab crashed the game. Fixed the maps.
- **Safety net:** `Floor.addRoomBehind` now refuses to link two rooms by a door unless both maps really have one, so this kind of mistake stops the game at start-up with a clear message.
**Checks:** a 38-check Floor 2 play-through (stairs, hallway, all 6 door positions from both sides, solid furniture, walkable trail, links) passes, plus the safety check and every earlier test. Floors 4 and 3 are pixel-identical apart from the intended rubble removal. The guide is `docs/floor2-lab-guide.md`.
**Open issues:** hazards are decoration only (no damage yet); the sealed doors have no way to open; Floor 1 doesn't exist.

## 2026-11-02 — Floor 1: the ground floor, and a correction

**Goal:** the ground floor, built the same way as the others (3 rooms, from a new sheet), with grass outside, walls still along the bottom, two pillars at each end supporting the building beside two exits, one of them blocked. The quadrangle comes next, so prepare for it.
**What happened:**
- **The sheet:** the same template in a neutral stone-grey palette. New pieces: a cracked support pillar, a reception counter, a cream cabinet, a bench and a fire-alarm bell.
- **Tiles (`res/tiles/Ground/`, 31):** 13 cut; the pillar made 2 tiles tall (cap and base kept, shaft repeated) with a transparent background; night grass (3 tiles, drawn); open and boarded-up exits; FACULTY/PRINCIPAL/RECORDS plaques in wood and brass.
- **Rooms:** Faculty Room (desks in threes), Principal's Office (waiting area, reception counter with one gap, the principal's desk, a locked door), Records Room (rows of filing cabinets). They mix the ground sheet's furniture with the building's standard chairs and desk.
- **Outdoors:** beyond the corridor's low wall there's grass instead of the floor below. The stairwells keep their walls, with an exit in each bottom wall between two pillars. The west exit is boarded up; the east exit is open.
- **Story:** footprints and slime lead from the collapsed east stairs to the open exit. Whatever escaped from Containment went outside.
- **Ready for the quadrangle:** the open exit is a door (`TileSet.doorSize` knows `EXIT_L`); `World.groundExit` remembers its screen. Attaching the quadrangle is one `addRoomBehind` call.
**Decisions:**
- **Which exit is open:** the request said both "the left one is blocked" and "the usable one is on the left". I chose west blocked and east open: it reverses the upper floors ("this time"), makes the player cross the whole ground floor, and matches where the creature's trail goes. Swapping is two words in `World.java`.
- **Shared code, third time:** the one-screen room builder moved from `Lab` into `SingleRoom`, now used by the labs and the offices. The stairwell builders now return an unfinished `MapBuilder`, so the ground-floor versions can add grass and exits before building.
**Problems:**
- **A bug in my checking code, which changed the Floor 2 story.** Pillow's `getbbox()` on images with transparency only looks at the alpha channel, so a tile comparison called two clearly different floor tiles "identical". Rechecking byte by byte: the six see-through decals really are identical across all sheets, but the lamp, table, boxes and bloody door are not. So the Floor 2 entry's "4 recoloured from the library's background" was wrong: those lab tiles already had the lab's background, and the recolour step changed nothing (the files are byte-identical to the lab sheet). **The game was never affected; only the explanation was wrong.** The Floor 2 guide and a `TileSet` comment were corrected.
- **Two test mistakes (not game bugs):** a Floor 2 test still expected Floor 2 to be the bottom floor, and a Floor 1 test placed the player on a bench.
**Checks:** a 34-check Floor 1 play-through, including attaching a stand-in outdoor screen to the exit and walking out and back. All earlier tests pass, and all 24 screens of Floors 4–2 are pixel-identical apart from the intended rubble removal. The guide is `docs/floor1-ground-guide.md`.

## 2026-11-02 — The view over the railing shows the real floor below

**Goal:** make the darkened strip below each hallway's railing (the "hollow view", a teaser of the floor below) accurate. It was a generic picture: classroom doors, pillars between rooms and the same windows on every floor.
**What happened:**
- **The pattern:** every floor's hallway has the same 5 screens in the same order, and the strip (rows 13–17) lines up with the floor below's wall and first corridor row (rows 3–7). So the real picture can be copied, 10 rows lower.
- **`TileMap.View`** (a Java `record`): a rectangle of one map that shows rows of another. Views are **scenery**: always solid and never doors, because the rows copied from below include working doors that must not become a way through.
- **`TileMap.showFloorBelow(below)`** copies every layer (floor, walls, objects) into the views, then clears the cached picture.
- **`World.showFloorsBelow()`** runs after every floor exists. It walks the floor list from the top and, for each floor, walks its hallway list **side by side** with the hallway list of the floor below.
**Results:** Floor 4 now shows the library's big door and LIBRARY plaque (and no pillars in the middle), Floor 3 shows the lab doors, plaques, hazard signs and the start of the creature's trail, and Floor 2 shows the office doors, plaques and fire alarm. The ground floor still shows grass.
**Checks:** a new test compares every view cell with the screen below, tile for tile in every layer (15 screens, 1,215 cells), and checks views are solid with no doors in them. Only the strips changed; everything else is pixel-identical; all earlier tests pass. Explained in `docs/railing-view-guide.md`.

## 2026-11-03 — The quadrangle: the last stand's arena

**Goal:** the courtyard outside the ground floor's exit: the area of 2 × 2 rooms, a camera centred on the player, built from a new sheet (or new tiles that fit), with **3 strategic locations** for a rapid-fire last stand.
**What happened:**
- **Maps of any size:** until now every map was one screen and `ROWS`/`COLS` were used everywhere. `MapBuilder(cols, rows)` and per-map `cols`/`rows` in `TileMap` lifted that. Every existing screen is pixel-identical afterwards.
- **A 2D camera:** `cameraY()` joins `cameraX()`; both centre on the player and clamp to the map's edges. One-screen maps never scroll vertically (they're exactly as tall as the view above the UI bar), so nothing else changed.
- **The map (50 × 36):** two character maps, `GROUND` (grass, paving, asphalt, the building's face) and `THINGS`, laid out by a small script so every row is exactly 50 characters. There's a central plaza with a flagpole, benches and lampposts, a car park, a basketball court, tree-lined paths, a fence all round with three locked gates, and the creature's trail from the doorway to the south gate.
- **Tiles (46):** cut from the sheet; the padlock painted out of the fence by copying the bars 8 px over (they repeat every 8 px); the fence turned 90° for the sides. Drawn: a wrecked school bus (7 × 3, with a walkable roof and a ladder), book barricades, hoops, a flagpole, lampposts, parking lines.
- **The three strongholds:** **the Wrecked Bus** (high ground; the only way onto the roof is the ladder, using invisible walls, including a new horizontal `edgeWallH`); **the Book Barricade** (a U of library books and desks facing the south gate, with two one-tile gaps); **the Cage** (the fenced court: one gate, and the fence stops attackers but not bullets).
- **Prepared for the fighting:** `TileSet.isShootThrough(id)` (fences, gates, benches and tables are solid but don't stop bullets); `Quadrangle.STRONGHOLDS` (name, area, why it helps) and `Quadrangle.SPAWN_GATES`.
**Decisions:**
- **One big map, not four linked screens:** the request was a single area with a centred camera, and the engine's existing left/right links can't express up/down neighbours. Generalising the map size was simpler and helps any future big area.
- **The ground floor now uses the sheet's grass:** walking out of the building should land you on the same ground. Only the grass on Floor 1 changed.
**Problems:** a test showed a lamppost standing directly in front of one of the barricade's gaps, which would have blocked shots through it, so it was moved. Two Floor 1 checks written when the exit led nowhere were updated.
**Checks:** a 39-check quadrangle play-through (entering and leaving, the camera centring and clamping, every way onto the bus roof, the barricade's gaps, the cage's gate, the perimeter, the spawn gates, the data). All earlier tests pass. The guide is `docs/quadrangle-guide.md`. (This entry first said 50 checks by mistake; corrected on 2026-11-03.)
**Open issues:** the last-stand gameplay itself: shooting, enemies, waves from `SPAWN_GATES`, and using `STRONGHOLDS`.

---

## 2026-11-03 — Cleanup: the standard Java project layout

**Goal:** delete the files and pictures nothing uses, and organise the project the way Java projects usually are.
**What happened:**
- **Deleted 16 unused files:**
  - the 12 old tile images from before the classroom set (`res/tiles/*.png` and `res/tiles/Classroom/`)
  - 4 doc screenshots that no guide showed
- **The Maven folder layout** (the standard one, even for projects that don't use Maven):
  - the game's code in `src/main/java`
  - its images in `src/main/resources`
  - the checks and debug tools in `src/test/java`
  - Eclipse's `.classpath` and `build-jar.bat` follow the new folders.
- **Packages:**
  - lowercase, under one root, `nightmare404`: `main`, `entity`, `ui`, `tile`, `map`, `world`
  - the old `tile` package is split into `tile` (the engine: `TileSet`, `TileMap`, `MapBuilder`, `Staircase`) and `map` (the map designs)
  - two `GamePanel` fields (`flor` and `tiles`) became `public`, since classes in other packages now read them
- **Resource folders are lowercase** too: `Classroom2` → `classroom`, `Stairs` → `stairs`, and so on.
- **The checks are saved in the project.** The play-through checks only existed in a temporary folder.
  - Nine are now in `src/test/java/nightmare404/checks`, and `run-checks.bat` runs them all.
  - `CameraCheck` and `DoorSafetyCheck` were made pass/fail.
  - `WorldWalkCheck` lost its first part, which compared against a file from the very first version that no longer exists.
  - `EndWallCheck` was dropped: `WorldWalkCheck` already tests both end walls.
  - `PerfCheck` only prints timings, so it became the debug tool `PerfDebug`.
- **The art scripts are saved** in `tools/art/`, with a README: the 5 tile scripts and the quadrangle layout script. Rerunning them reproduces all 177 tiles they made, pixel for pixel.
- **Git:** a `.gitignore` was added, and git no longer tracks the build output (`bin/`, `build/`, `dist/`).
- **Docs:**
  - the images are grouped into one folder per guide
  - every link in the guides points at the new paths
  - new: `README.md` and `docs/project-structure-guide.md`

| Before | After |
|---|---|
| `src/Nightmareon404/Main.java`, `GamePanel.java` | `src/main/java/nightmare404/main/` |
| `src/Nightmareon404/Player.java` | `src/main/java/nightmare404/entity/` |
| `src/Nightmareon404/BottomUI.java` | `src/main/java/nightmare404/ui/` |
| `src/Nightmareon404/MapPreviewDebug.java`, `WalkStripDebug.java` | `src/test/java/nightmare404/debug/` |
| `src/tile/` `TileSet`, `TileMap`, `MapBuilder`, `Staircase` | `src/main/java/nightmare404/tile/` |
| `src/tile/` `Classroom`, `Hallway`, `Stairs`, `Library`, `Lab`, `Office`, `Ground`, `Quadrangle`, `SingleRoom` | `src/main/java/nightmare404/map/` |
| `src/world/` | `src/main/java/nightmare404/world/` |
| `res/Characters/Player/` | `src/main/resources/characters/player/` |
| `res/tiles/Classroom2/`, `Stairs/`, `Library/`, `Lab/`, `Ground/`, `Quad/` | `src/main/resources/tiles/classroom/`, `stairs/`, `library/`, `lab/`, `ground/`, `quad/` |
| `docs/images/*.png` | `docs/images/<guide>/*.png` |
| `dist/Nightmareon404.jar` | `dist/NightmareInRoom404.jar` |
| module `GameProd` | module `nightmareinroom` |

**Decisions:**
- **Maven's folders, without Maven:** this is the layout most Java projects use. Keeping the Eclipse project and the `.bat` scripts means there's nothing new to install, and building is still a double-click.
- **The root package `nightmare404`,** instead of the reverse-domain style (`io.github.<name>.nightmare404`), which would make every import long.
- **The module is called `nightmareinroom`, not `nightmare404`,** because Java warns about module names that end in digits. The packages keep `nightmare404`.
- **Build output isn't committed.** Anyone can rebuild it from the source. The JAR should be shared through a GitHub Release.
- **Past devlog entries keep the old paths,** because they describe what was true at the time. The guides were updated, because people use them as references.
**Problems:**
- VS Code kept showing old errors in `GamePanel.java` (it expected the package `main.java.nightmare404.main`) until its Java workspace refreshed. It still remembered `src` as the source folder.
- A correction: `QuadCheck` has 39 checks, not 50 as the quadrangle entry first said.
**Checks:**
- `run-checks.bat`: 9 checks with 198 tests, all passing. A deliberately failing check was counted and made the script fail.
- All 33 maps and the walk strip, rendered with only the new JAR on the classpath, are pixel-identical to before the move.
- The JAR opens the game window with no errors. It holds 213 tile images, the 12 sprites, and no test code.
- Every link in the docs resolves.
**Open issues:** commit the cleanup (see the open issues below).

---

## 2026-11-03 — A build groupmates can just double-click

**Goal:** share the game with groupmates without making them install or set up anything.
**What happened:** the new `build-exe.bat` builds the JAR, then uses `jpackage` (part of the JDK) to wrap it with a cut-down Java runtime and a `NightmareInRoom404.exe` launcher. It zips the result into `dist/NightmareInRoom404-windows.zip`: 25 MB zipped, 76 MB unzipped. To play: extract it, then double-click the `.exe`. The README explains how to share it.
**Decisions:**
- **A ready-to-run folder (`--type app-image`), not an installer:** jpackage's `exe`/`msi` installers need the WiX Toolset, and a folder in a zip is easier to hand out.
- **Only the parts of Java the game uses** (`--add-modules java.desktop`), which keeps the download small.
- **Shared by link** (Google Drive or a GitHub Release) instead of committed, like the JAR.
**Problems:**
- In a quick test, the launched `.exe` seemed to show no window. The launcher actually starts a second process that owns the window, and the test was watching the first one. A debug copy built with `--win-console` showed the game was fine.
- `build-exe.bat` calls `build-jar.bat` by its full path (`%~dp0`). Some terminals don't search the current folder for programs.
**Checks:** the zip was extracted into an empty folder, with Java removed from `PATH` and no `JAVA_HOME`. `NightmareInRoom404.exe` opened the "Nightmare in Room 404" window and kept running.
**Open issues:**
- It's Windows-only. Mac users still need the JAR and Java 21.
- The `.exe` isn't digitally signed, so Windows SmartScreen asks "More info → Run anyway" the first time.
- It has no custom icon yet: jpackage's `--icon` takes an `.ico` file.

---

## Open issues and ideas

- Redraw `down_idle`'s head to match the walk frames (then Down can go back to `walk1 → idle → walk2 → idle`).
- ~~Make the west stairs work~~ (done: Floor 3, 2026-11-01). ~~Floor 2~~ (done: the labs, 2026-11-01). ~~Floor 1~~ (done: the ground floor, 2026-11-02). ~~The quadrangle~~ (done: 2026-11-03). Next: the last-stand gameplay.
- Hazard tiles (toxic puddles, slime) that drain health, the first real use of the HP bar.
- A way to open the sealed lab doors (a keycard?).
- ~~Old unused images in `res/tiles/` (outside `Classroom2` and `Stairs`) can be deleted.~~ (done: the cleanup, 2026-11-03)
- Design the real bottom UI (art, layout, what it shows). `BottomUI` is a placeholder.
- Nothing changes `health` yet: it's always 100 until there are hazards or enemies.
- Most of the project, including the 2026-11-03 cleanup, isn't committed to git yet; commit it so the history is safe.
