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

## 2026-11-10 — A shorter walk: faster steps, a sprint key, narrower hallways

**Goal:** a groupmate found the walking area too big.
**What changed:**
- **Speed:** walking went from 3 to 4 px per frame, and holding **Shift** sprints at 6 px (`Player.WALK_SPEED` / `SPRINT_SPEED`). The walk animation speeds up to match, so the feet don't slide. Swing key strokes include the modifiers held at the time ("pressed W" doesn't fire while Shift is down), so WASD is bound both with and without Shift.
- **Pillars:** each hallway screen used to draw its own pillars at columns 2 and 22, so every seam had two pillars 4 tiles apart. Now there's one pillar per seam, split in half across it (`Hallway.seamPillars`), and none next to the stairwells, which have their own wall.
- **Narrower hallway screens:** the three room-front screens on each floor are 17 columns instead of 25, so a floor is 101 tiles long instead of 125 (about 19% shorter). The rooms behind keep their size. Because the doors are no longer in the same columns on both sides, going through a door now matches doors **by order** (left door to left door) instead of by nearest column, and `Floor.addRoomBehind` checks both sides have the same number of working doors. The camera now draws neighbours on both sides, since a 544 px screen is narrower than the 800 px view.
- **Checks:** `StairsEntryCheck` held Up for a fixed 200 ticks, which at a different speed walks down a floor and straight back up; it now stops when the floor changes. The other checks use the new door columns (`Hallway.HALL_DOOR_COLS` / `ROOM_DOOR_COLS`) and measure positions by screen widths instead of `800`.

---

## 2026-11-12 — New player sprites

**What changed:** the main character has new art: a grey jacket over a white shirt, black trousers and messy black hair. Still 32 × 64, the same 12 frames, so they're straight replacements for the files in `src/main/resources/characters/player/`.
- The new `down_idle` has the same head as the walk frames, so Down uses the normal walk cycle again (`walk1 → idle → walk2 → idle`) instead of holding each stride for two steps.
- The walk frames bob the body 1 px lower than the idle frame. `WalkStripDebug` compared heads pixel for pixel and reported that as a change, so it now allows up to 2 px of up-and-down bob.

---

## 2026-11-12 — Narrower stairwells, and the broken stairs alternate

**What changed:**
- **Stairwells the same width as the hallway:** the West and East Stairs screens were still 25 columns, so the two ends of each floor looked stretched next to the 17-column room fronts. They're now 17 columns too, so a floor is 85 tiles long (it was 125 before the hallways were shortened). Both ends come from one builder, `Stairs.build(..., Side, Flight down, Flight up)`; the east stairwell is the west one mirrored.
- **The broken stairs alternate:** before, the west stairs worked on every floor and the east ones were always destroyed, so you could go straight down without crossing a floor. Now the way down switches ends on each floor: Floor 4 west, Floor 3 east, Floor 2 west, then Floor 1 east to the exit. A flight joins two floors, so each middle stairwell has one working flight and one that has caved in (holes and broken steps). The east stairwells on Floors 4 and 1 work in neither direction, so they're the full ruin with rubble everywhere.
- **Code:** each flight is `OPEN`, `BLOCKED` (no floor that way) or `COLLAPSED`. A flight that doesn't work has no zone in its `Staircase` (null), so it can't take you anywhere. `Floor.stairs` is replaced by `Floor.upStairs()` / `downStairs()`, which find the right stairwell along the hallway list, and `Floor.connectStairs` refuses at start-up to link two floors whose flights aren't at the same end of the building. The LABORATORY sign moved to Floor 2's east stairs, where you now arrive.
- **Checks:** the stair checks now test the alternation (collapsed flights block, working ones lead to the right stairwell) and read the arrival spots from each `Staircase` instead of hard-coding pixels. A scripted play-through using only key presses gets from Room 404 to the quadrangle in about 43 s of walking.

---

## 2026-11-12 — A staircase with guard rails and depth

**Goal:** the stairs were a flat block (a light landing, two striped flights and a thin rail), with nothing showing that one flight goes down and the other goes up.
**What changed:**
- **One detailed picture:** the staircase is now drawn as a single 7 × 7-tile picture by `tools/art/make_stair_tiles.py` and cut into tiles in reading order (`TileSet.STAIRCASE_FIRST`, like the bus): a balustrade, the down flight, a divider, the up flight, a balustrade.
- **Guard rails:** each balustrade is a stone wall with an iron handrail on top: a thin baluster at every step, a thicker post at every block, and a heavy newel post at the bottom end, with the wall's front face showing below it. They're solid, so they also replace the invisible side walls the stairs used before. The staircase grew from 5 to 7 columns, but the flights are in the same columns as before, so walking on them hasn't changed.
- **Depth:** the down flight is a pit: its inner walls widen and everything in it darkens as it goes down, and the back wall carries on below floor level to a dark lower landing. The up flight shows the risers facing you and its steps get lighter as they climb to a raised, lit landing. The balustrades cast shadows onto the flights.
- **Collapsed flights:** the cracks (`STAIR_STEP_BROKEN`) and holes (`STAIR_HOLE`) are now see-through overlays on the objects layer, so they sit on top of the new steps instead of replacing them. The old landing, step and rail tiles aren't used any more; the script still makes them, pixel for pixel the same.

---

## 2026-11-12 — The staircase as a box, and a main entrance in the middle

**What changed:**
- **The staircase is a complete box:** the stairwells now rise two rows higher than the hallway (their back wall starts at row 1), which leaves floor between the back wall and the staircase. A back balustrade closes the staircase at the top. It runs left to right, so its railing shows its posts from the side, and corner posts join it to the side balustrades. A stone threshold along the bottom finishes the frame without blocking the way onto the flights. The staircase picture is now 7 × 8 tiles. You can walk round behind it, but not onto it from there.
- **A main entrance in the middle:** the way out of the building used to be the exit at the east end. Now both side exits are boarded up, and the main entrance is in the middle of the ground floor: a doorway 4 tiles wide (twice a side exit) in the corridor's low front wall, in front of the Principal's Office, with pillars either side and a stone path across the grass. In the quadrangle it comes out in the middle of the building's face, with a paved path down to the plaza; the old side doorway there is boarded up too. The book barricade now stands just in front of the main entrance. The art comes from a new script, `tools/art/make_entrance_tiles.py`.
- **Doors are linked one by one:** that middle hallway screen now has two kinds of doors: the office's (in its back wall) and the main entrance (in its front wall). So each screen now knows, for each of its doors, which room it leads to and which door you come out of (`Room.doorLeadsTo` / `doorArrivesAt`). `Floor.addRoomBehind` links the doors in a hallway's back wall; the new `Floor.addOutside` links the ones in its front wall. `Room.door` is still the room behind a hallway screen.
- **Checks:** `Floor1Check` and `QuadCheck` test the boarded-up side exits and the way in and out through the main entrance; `StairsEntryCheck` tests the floor behind the staircase. The key-only play-through now takes about 37 s from Room 404 to the quadrangle.

---

## 2026-11-12 — No more locked bloody doors; side gates at both ends

**What changed:**
- **The locked bloody doors are gone:** the four decorative doors that led nowhere (in the Chemistry Lab, the Library entrance, the Principal's Office and on the quadrangle's face) are now plain wall. Their tiles and legend letters still exist, so one can be put back by writing an `X` in a map.
- **Side gates at both ends:** on the quadrangle's face, the boarded-up side doorway moved from beside the main entrance (columns 36-37) out to the east end (43-44), and a matching one was added at the west end (5-6), so the face is symmetrical: a boarded side exit at each end, the main entrance in the middle, just like inside. Each has a paved path to the nearest gate path, framed by lampposts; the creature's trail now starts at the east one. Made with `tools/art/make_quad_maps.py`, so the arrays still match the script.

---

## 2026-11-12 — New hand-drawn props

**What changed:** seven new drawings, kept in `docs/images/props` and cut into tiles by `tools/art/make_prop_tiles.py` (into `tiles/props`):
- **Windows:** the plain window and the one with red curtains are now the windows of every room in the building: the classrooms, labs, library, offices, hallways (and the floor below seen over the railing), stairwells and the quadrangle's face, so they all match. They're drawn a little wider than a tile, so to keep each one whole in a single tile the script takes out a few columns where nothing changes (plain glass, plain curtain). (A first try made them 2 tiles wide, but the classrooms place their windows by tile number, so they showed half a window.) The area windows (`LAB_WINDOW`, `LIB_WINDOW`, `G_WINDOW`, `Q_WINDOW`) are no longer used.
- **Trees:** the small round trees (36 of them, which looked like bushes) are now 13 big 2 x 2 trees, round the edges and on the lawns. Their leafy top can be walked past behind; the trunk blocks.
- **New things in the quadrangle:** 8 bushes either side of each doorway and of the path south (they block walking but not shots), 4 mossy rocks as cover on the lawns, and 4 overgrown trash cans by the benches, the car park and the court.
- **The bus:** the new bus replaces the wrecked one. It's drawn the size of a bush, so it's scaled up 3 times to be bus-sized (its pixels are bigger than everything else's; a full-size redraw, 160 x 96, would look crisper). It's 5 x 3 tiles: the roof is the first 4 columns, the ladder (column 2) is still the only way up, and the lower front (column 4) is solid.
- **Layout:** big things in the quadrangle's THINGS map are written as their top-left letter with `+` on the rest of their tiles (`Quadrangle.PROPS` fills them in); `make_quad_maps.py` checks each one is on grass and overlaps nothing. The facade's windows and lamps are now placed symmetrically around the main entrance. The old tree and bus tiles are no longer used (their IDs are kept so nothing after them moves).

---

## 2026-11-14 — Updated player sprite sheet

**What changed:** all 12 player frames now come from the new sprite sheet (rows: down, right, left, up; in each row the middle frame is the standing pose and the outer two are the steps). The front and back walks are new: the arms swing and the body sways a pixel or two sideways with each step. The side views are the same as the previous sheet's.
- `WalkStripDebug` now allows that sideways sway as well as the up-and-down bob, so it only reports a head that's actually drawn differently. It does report one: in `down_walk2` and `up_walk2` the hair strands are drawn differently from the other frames (they look mirrored), so the hair detail changes slightly every other step. Left as drawn for now.

---

## 2026-11-15 — Monsters and word battles

**What changed:** the battle screens from the `Battle_Screens` folder are in the game. Touching a monster opens its word battle in the game's window; winning removes it for good.
- **The battle code** (`nightmare404.battle`): the 12 battle screens were the same engine 12 times, with different monsters and backgrounds. So the engine is in one class, `BattleScreen` (the original code, with its inner `GamePanel` renamed `BattlePanel` so it isn't confused with the game's), the monsters are in four files by area (`FourthFloorEnemies`, `LibraryEnemies`, `LaboratoryEnemies`, `LobbyEnemies`) and the six backgrounds are `Backdrop` classes holding the original `drawRoom` code. The changes to the original code: files load from `src/main/resources/battle` (so they're found inside the JAR), each `EnemyDef` names its background, the room signs name the game's rooms, the hero is the game's own player sprite, and `fight(...)` opens one battle in an existing window. `BattleScreen.main` still runs a fight on its own.
- **Where the monsters are** (`nightmare404.world.Encounters`): Room 404 is safe; every other room has 2 or 3 monsters (23 in all), and each floor has a boss: the Brute at Floor 4's West Stairs, the Whisperer in Library (East), Test Subject X in the Containment Lab and the Principal in his office. Each floor's way down (on Floor 1, the main entrance) stays sealed until its boss is beaten.
- **How they move** (`nightmare404.entity.Monster`): each walks a straight patrol along an aisle; within 3 tiles it chases you (slower than you walk), gives up 6 tiles away and walks back the way it came. Bosses stand guard. They have no walking frames, so they bob.
- **Winning and losing:** health, first aid kits and locker items carry from battle to battle (you start with 3 kits and one of each item). Giving up after losing wakes you where you came onto the floor, with full health; that room's monsters go back to their posts.
- **Checks:** `EncounterCheck` tests the placements, patrols, chasing, touching, the sealed exits, winning and losing, and that every battle's sprites, words and screen load. The other checks start with `gp.peaceful()` (every monster beaten), since they only test walking. `BattleScreensDebug` saves a picture of every battle screen; `MapPreviewDebug` now draws the monsters too.

## 2026-11-15 — The quadrangle from Floor 1, bosses that guard, and keys

**Goal:** four things: show the quadrangle below Floor 1's railing, put the Brute where nobody can get past, make giving up undo only what happened on the floor, and give the bosses a reward that opens the next floor.
**What happened:**
- **The quadrangle from Floor 1** (`TileMap.showOutside`, `World.showOutside`, `Hallway.corridorScreen`): the upper floors already show the floor below over the railing. Floor 1's hallway now shows the strip of the quadrangle in front of the building (its walkway, bushes, trees and lamps), shifted so the entrance path lines up under the main entrance. `ViewCheck` compares those cells with the quadrangle map.
- **The Brute on the stairs** (`Encounters`, `Monster.nudge`, `Monster.reach`): it stands in the middle of Floor 4's way down, between the two columns of the flight, and a boss's reach is 2 tiles wide, so climbing at either edge still runs into it. The other bosses stay in their lairs (the Whisperer at the far end of the library, Test Subject X among the pods, the Principal behind his counter); the keys below make each of them a fight you can't skip.
- **Loot and keys** (`Loot`, `Floor.exitKey`/`lockedNote`/`locked`): a beaten boss leaves a glinting key where it stood; standing on it picks it up with some supplies. The Brute drops the janitor's West Stairwell Key (+1 kit, +1 Red Pen); the Whisperer, the East Stairwell Key in a hollowed-out book (+1 kit, +1 Textbook, +1 Hall Pass); Test Subject X, a Security Keycard that overrides the lab lockdown on the west stairwell (+2 kits, +1 Red Pen, +1 Hall Pass); the Principal, the school's Master Key to the front doors (+2 kits, one of each item). Without the key, the way down (or out) shows a note saying what's missing. The bottom bar now shows every item and, in gold, the floor's key once you have it. Notices can run onto two lines.
- **Giving up** (`Player.setCheckpoint`/`respawn`): arriving on a floor takes a snapshot: health, kits, items, keys, and which monsters on the floor were already beaten. Giving up restores exactly that, and the monsters beaten on this floor since come back (a boss with its key). Other floors aren't touched.
**Decisions:** giving up restores kits, items and keys too, not only health; otherwise you could use up kits, give up and keep the boss's loot, or lose a key you'd need. Loot is picked up after the 1-second pause that follows a battle, so the key is seen glinting first.
**Checks:** `EncounterCheck` now climbs the stairs at the middle and both edges of the flight into the Brute, picks up its key, goes down, gives up on Floor 3 with 80 HP (and gets 80 back, not 100), and tries every locked way out with and without its key.

## 2026-11-15 — Side exits in the middle of the stairwells, not outside

**What changed:** the boarded-up side exits (with their two pillars) now sit in the middle of each Floor 1 stairwell's bottom wall, right below the staircase (`Ground.boardedExit`, `Stairs.exitMiddle`: column 7 west, 9 east). A stairwell is 11 tiles wide, an odd number, so a 2-tile doorway could never be exactly centred: the boarded doorway is now 3 tiles wide, with a new middle tile (`EXIT_BLOCKED_M`, ID 313, drawn by `tools/art/make_ground_tiles.py` with the same planks). The east stairwell's footprint trail now leads to it, and the rubble that would have sat in front of it moved aside.
The quadrangle's building face lost its two side exits and their pillars, so they're gone from the view over Floor 1's railing too. The paths that led from them stay, as garden paths off the walkway, and the gaps in the wall got a window each (columns 6 and 43, keeping the windows symmetric). `Floor1Check` and `QuadCheck` check the new layout.

## 2026-11-16 — Hallway monsters, clutter, flickering lights, dimmer rooms

**What changed:**
- **Monsters in the hallways** (`Encounters.hallway`): 8 hallway screens have one monster walking up and down the middle of the corridor (rows 9-10), 2 a floor: a Crawler and the Hall Monitor on Floor 4, an Undead and a Bookworm by the library, a Flask Slime and Specimen 7 by the labs, a Shadow Stalker and the Substitute by the offices. The hallway outside Room 404, the library's entrance and the main entrance stay safe. Their battles' signs say "4F HALLWAY" and so on. Because the hallway screens are narrower than the view, the monsters on the screens beside you are in sight, so they keep walking their rounds (`Monster.update(null)`); only the ones on your screen can notice you, and the ones you leave behind stop chasing.
- **Clutter** (`Hallway.scatter`): every room-front screen has 3 things on the floor you can walk over and 2 that block, in its floor's style: broken chairs and desks by the classrooms, books by the library, glass, slime and rubble by the labs, chairs and rubble by the offices. The blocking ones stay against the walls (rows 7 and 11), never in front of a door or the main entrance, so the middle of the corridor is always clear. Places are random but seeded, so they're the same every game.
- **Light** (`ui.Lighting`, drawn by `GamePanel` over the world and under the text and bottom bar): the hallways and stairwells are slightly dark, and about every 5 seconds their lights stutter off and on for a moment (about 3% of the time in all). The rooms behind the doors are dimmer, darkest at the edges of the view and a little lighter around the player. The quadrangle is unchanged. The numbers are at the top of `Lighting`.
**Checks:** `EncounterCheck` now expects one monster on 8 hallway screens (not outside Room 404 or at the two entrances) with "nF HALLWAY" signs. `Floor1Check`'s low-wall test moved to column 15, where clutter never goes.

## 2026-11-19 — The quadrangle's trees come alive, and a typing race

**Goal:** make the quadrangle's trees its enemies. A short while after the player comes outside they pop out of the ground and chase them, and they're beaten with a fast typing challenge instead of the word grid. Fewer trees if there were too many.
**What happened:**
- **Fewer trees:** 13 → 6, spread round the edges and lawns, none right by the main entrance (`Quadrangle.THINGS`, and `make_quad_maps.py` to match). `Quadrangle.trees()` lists them.
- **The trees** (`entity.TreeMonster`, a kind of `Monster`): asleep, a tree is just the map's own tree tiles. When the player comes outside (`Monster.playerArrived`) each one counts down: 1.5 s, plus a little more for each tile further away, so they wake one after another (the nearest after about 2.5 s). Popping out takes a second: the tree's tiles leave the map (`TileMap.setTile`, which redraws only that cell of the cached picture) and the monster shivers, opens red eyes, tears its roots up out of the ground in a spray of dirt and rises up on them, leaving torn earth. Then it hunts the player across the whole quadrangle at 3 px a frame (the player walks at 4), sliding along obstacles and stepping round them when stuck. Leaving and coming back puts every tree still standing back to sleep in its place. A beaten one leaves a stump; giving up brings back the ones beaten since Floor 1 began.
- **The Hollow Oak** (`battle.QuadrangleEnemies`, sprites by `tools/art/make_treant.py` from the tree tiles: eyes, a jagged mouth, branch arms, root legs; standing, attacking and hurt poses). Lore: the groundskeeper planted a tree for every student who never went home.
- **The typing race** (`battle.TypingBattle`): a word and a timer. Type it in time to hit the tree (2.5 per letter, ×1.5 if quick, +10% per word in a streak, up to +50%); run out of time and it hits you; a wrong key costs a quarter of a second and the streak. As the tree weakens, words get longer and the timer shorter. Keys 1-4 use a First Aid Kit, Old Textbook (halves the next hit), Red Pen (doubles the next word) or Hall Pass (skips a word). Losing offers Try Again (Enter) or Give Up (Esc), like the word battle. The rules (`Duel`) are separate from the drawing, so the checks play whole fights. Its background is the quadrangle at night (`QuadrangleBackdrop`). The words are in `battle/typing_words.txt`. `GamePanel.openBattle` sends the trees to it and everyone else to the word battle.
**Checks:** `EncounterCheck` sections 8-9: the 6 trees sleep, wake one after another, pop out (tiles off the map, torn ground), hunt and catch the player; winning, leaving and coming back, giving up; and the typing race's rules (damage, quick bonus, mistakes, time-outs, every item, winning, losing, Try Again). `BattleScreensDebug` saves the typing screen too.

## 2026-11-19 — A turn timer in the word battle

**What changed:** the player's turn in the word battle now has a time limit (`BattleScreen.TURN_TIMER`, `turnSeconds`). When it runs out, the word being spelled is dropped ("Too slow!") and the monster takes its turn; then the player gets a fresh clock. It goes by the monster's health: 45 seconds for 80 HP or less (the 4th floor's beginners), 5 seconds less for every 5 HP more, never under 15. So: Crawler, Desk Mimic, Hall Monitor, Flask Slime 45 s; Chalk Wraith, Undead Scholar, Bookworm, Anatomy Model, Lab Assistant 40 s; the Brute and the other 90 HP monsters 35 s; Substitute, Shadow Stalker 30 s; Whisperer, Test Subject X 25 s; the Principal 15 s. The clock only runs on the player's turn (not during attacks or with the menu open); Shuffle and items are free and don't stop it. It's drawn as a burning fuse with a clock on the floor between the fighters, just above the letters: green, amber, then red and flashing for the last 5 seconds.
**Decisions:** health was used as the measure of strength because it already rises floor by floor and boss by boss, so there's no new number to keep in step in 21 monster definitions. The hallway copies of the monsters get the same time as the originals. The quadrangle's typing race has its own per-word timer and is unchanged.
**Checks:** `EncounterCheck` section 11 prints every monster's time, checks a stronger monster never gives more time than a weaker one, and idles through fights (`BattleScreen.enemyTurnsWhileIdle`) to check the Desk Mimic waits its 45 seconds and the Principal attacks every 15.

## 2026-11-23 — A story, quests, a dialogue box, and the fight outside on the map

**Goal:** give the player a reason to go into the side rooms instead of running straight to each floor's boss, tell a story, and show it in a dialogue box. In the quadrangle, keep the trees that wake up and hunt you, but make the fight more engaging than one battle screen after another, and add the final boss: the monster that escaped from Floor 2's labs, asleep and easy to miss, waking only when every tree is down.
**What happened:**
- **The story** (`story/Story.java`, every line in one file): Flor stayed late in Room 404 and fell asleep. Anyone still in the school after midnight gets forgotten: their names fade off every register. The monsters are what the forgotten became, and each boss was someone. It builds on the battles' incident reports (the janitor's logbook, "never marked absent", the groundskeeper's trees).
- **The dialogue box** (`ui/DialogueBox`): a box above the bottom bar. Five kinds of line (`story/Line`): someone speaking (name and face), Flor thinking (italics), something written (lined paper), narration, and how-to-play tips (gold). The text types itself out; E, Space or Enter shows the whole line, then goes on. The world waits while it's open. A conversation is a **queue** of lines (`ArrayDeque`).
- **When things are said** (`story/Director`): the intro in Room 404, the first time on each floor and outside, before each floor's boss (what Flor says depends on how many of that floor's clues were found), picking up a boss's key, the trees falling, Subject Zero waking, and the ending. Each scene plays once.
- **The quests** (`story/Quest`, `story/Quests`, `entity/Clue`): two clues per floor in the side rooms (the janitor's logbook pages, library checkout cards, Project REMEMBER lab reports, a staff memo and the Room 404 register). Walking over one picks it up: the player reads it, gets a supply, and the floor's boss starts its battle 20% weaker per clue (`BattleScreen.fight` takes the foe's starting health; Try Again keeps it). A tracker in the top right shows the quest, and **J** opens a journal to read the clues again.
- **The fight outside, on the map** (`battle/LastStand`): the trees still wake one after another and hunt the player (unchanged, see 2026-11-19), but they no longer open the typing race. Each hunting monster shows a **word** over its head. **Space** focuses: the world slows to 30%, the player stands still, and the keyboard types. Typing a word (its first letter picks the monster) sends it flying as a **strike**: 3.5 damage a letter, +10% for every word in a row without a mistake. Focus lasts 5 seconds, comes back while you move, and each word gives some back, so it's a rhythm of running and striking. Strikes fly in straight lines: walls, the bus and trees stop them, fences, benches and bushes don't (`TileMap.stopsShots`), and the tile right beside you is cleared, so you can strike out from behind the barricade, the bus or the court's fence. A monster that reaches you hits you, knocks you back, breaks your focus and reels for a moment. Keys **1-4** use a First Aid Kit, an Old Textbook (halves the next hit), a Red Pen (doubles the next strike) or a Hall Pass (everything stands still for 3 seconds). Each tree felled leaves its stump, gives back 10 HP and saves progress.
- **Subject Zero** (`entity/SubjectZero`, art by `tools/art/make_quest_art.py` from Test Subject X's sprites): the subject that broke out of pod 2, asleep at the end of the slime trail by the south gate as a heap of dead leaves; walking right past it only gets a notice that something is breathing. When the last tree falls it wakes and walks after you. Its words are longer; every few seconds the ground cracks round you and roots burst up (get out of the way: stop focusing and run); hurt to two thirds and to one third, it calls two **saplings** through the gates. Beating it ends the game: **REMEMBERED** if all 8 clues were found, **FORGOTTEN** if not. Enter starts again.
- **Code:** the trees' chasing moved, unchanged, from `TreeMonster` into a new `Hunter` class that `SubjectZero` and `Sapling` share. Going outside through the main entrance moves the checkpoint there; going down outside brings you back to it with at least 50 HP and the trees felled so far still down.
**Decisions:**
- **Clues make the boss weaker; they don't unlock it.** Nothing is locked behind them, so a skilled player can still run straight to the boss.
- **Clues are undone by giving up, like keys**, so a clue's supply can't be kept after a lost battle. Pages already read show one line when picked up again.
- **Focus instead of typing all the time:** W, A, S and D are letters too, so the keyboard either walks or types. Focus makes that a choice, and slowing the world while you type keeps it fair.
- **The typing race isn't deleted.** It's no longer opened by the game (the trees are fought on the map), but its rules and word list are reused, and `EncounterCheck` still tests them.
- **No screen, no waiting:** with no display (the checks), the dialogue box closes as soon as it opens (`DialogueBox.autoSkip`) and logs its lines.
**Problems:**
- The fight's lists (strikes, words, roots) are changed by the game loop while Swing draws them on another thread, which could crash a frame mid-draw. `LastStand`'s public methods are synchronized. A first key test in a real window found it.
- The focus meter covered the room's name; it moved right and its hint got shorter.
**Checks:** the new `QuestCheck` (69 tests): the dialogue box, every clue's place, clues weakening the Brute (90, 72, 54), giving up putting a clue back, the Brute's lines, the journal; outside: focusing (the world slows, the player stands still), words with different first letters, a wrong key, a strike landing for exactly its damage, the bus stopping a strike and the cover beside you not, every item, being hit, felling trees, Subject Zero asleep then awake, its roots hitting and missing, the saplings, and both endings. `EncounterCheck` section 8 now expects trees to hit the player on the map, not open a battle, and going down outside to keep the felled trees down. All 11 checks pass (542 tests). The keys were also tried in a real window with the built JAR.
**Open issues:** the shooting-and-waves last stand from the quadrangle's first design still isn't there; the strongholds now matter as cover for strikes, and the gates for the saplings.

---

## 2026-11-25 — A main menu, and pixel-art health bars

**Goal:** open the game on a proper main menu instead of dropping straight into Room 404, and replace every health bar (the player's and the monsters', everywhere they appear) with new pixel-art bars.
**What happened:**
- **The main menu** (`ui/MainMenu`): the menu design (an HTML mock-up and its Java2D port): a dark hallway of lockers, the title, and on the right the door of Room 404 with a bloody "404" that drips once, three bands of NO ENTRY tape and a swinging sign. All drawn in code, no images. It's laid out on a 1280 × 720 stage and scaled to the window; in the game's 800 × 608 window the hallway carries on above and below, so there are no black bars, and the keys sit at the bottom of the window. `W` / `S` (or the arrows, or the mouse) choose, `Enter`, `Space` or `E` picks; the chosen line says what it does underneath.
- **What each line does** (`main/Main` now opens the window on the menu and swaps the menu and the game in it): *Start Game* makes a new `GamePanel`. *Continue* goes back to the game in progress, exactly as it was left; *Journal* opens that game's journal, and closing it comes back to the menu. Both are greyed out (and skipped by `W` / `S`) until a game has been started. *Options* swaps the list for two switches, *Quit* closes the game.
- **Esc in the game** opens the menu, unless it has something else to do (closing the journal, or stopping focus outside). The game loop keeps running but skips its updates while the menu is up (`GamePanel.paused`), and `resume()` lets go of every key, since presses and releases while the menu was showing never reached the game.
- **After the ending**, Enter now goes back to the menu (with nothing to continue) instead of starting again by itself.
- **Options** (`main/Settings`): *Turn timer* and *Flicker*. `BattleScreen` already had both as switches in the code (`TURN_TIMER`, `LIGHT_FLICKER`, "false turns it off"); they're now the settings the menu changes. Flicker also covers the hallways' lights (`Lighting`) and the menu's own.
- **The health bars** (`ui/HealthBar`, art in `src/main/resources/ui/`): 7 frames each, `hp_lead_1-7.png` for the player (a heart that drains) and `hp_enemy_1-7.png` for the monsters (a skull that fades). Frame 1 is full and frame 7 empty, each in between one segment less, so any health left shows at least one segment. `HealthBar` crops the frames to the bar (64 × 32 with space round it), scales them by whole numbers with no smoothing, and picks the frame. Used in all four places a bar was drawn: the bottom bar (the player's, ×2), the word battle and the typing race (both fighters, ×3, with the numbers beside them), and over the monsters outside (×1, under each word, see-through until you focus).
**Problems:**
- The menu's Java port compared the mouse's place in the window with the menu lines' boxes on the 1280 × 720 stage, so clicking only worked in a 1280 × 720 window. The mouse is now turned into a place on the stage first.
- The port also drew the vignette over the title and the menu, so once the tape's clip was fixed (see the next problem) the words nearly vanished into the dark. The HTML design draws the words above the vignette and the flicker below it; the Java now does the same.
- Each tape band set a clip to cut its text to the tape, then `setClip(null)`, which also threw away the clip Swing paints with. Putting the old clip back *before* undoing the rotation put it back rotated, which cut off the other two bands and the sign. The transform is undone first now.
- The typing race slides its bars with `shown += (hp - shown) * 0.15`, which never quite gets there: 80 HP stayed at 80.000002, which rounds up to 81 and could show one segment too many. The bar snaps to the real value once it's within 0.05, and the numbers show the real health, not the sliding one.
- The word battle's pale "damage trail" behind its bar had nothing to draw on in the new art, so `Fighter.trail` went. The bar still slides down a segment at a time, and the player's low-health warning became a red glow behind the heart bar that beats with the screen's pulse.
**Decisions:**
- **Continue resumes the game in this session; it isn't a save.** "Return to your last floor" across restarts needs a save file (which floor, health, supplies, keys, clues, the monsters beaten). The checkpoint `Player.respawn` uses already holds most of it, so that's the place to start.
- **Journal from the menu opens the real journal in the game** rather than a copy drawn on the menu, so *read again* still works.
- **Options are the two switches that already existed**, rather than new settings with nothing behind them.
- **The menu stays at the game's size** (800 × 608). Making the window 1280 × 720 for the menu and changing it for the game would jump, and the game's screen is built on its tile grid.
**Checks:** all 11 checks pass (542 tests), unchanged. The menu, the bars at full and low health, and the fight outside were drawn to pictures and looked at. The whole menu loop was also played with simulated key presses in a real window: Start Game, the opening scene, Esc, Continue (and walking still works), Journal, Esc back to the menu.
**Open issues:** a save file, so *Continue* survives closing the game. The menu's fonts are Java's built-in Serif and Monospaced; the HTML design used Permanent Marker and Special Elite, which could be bundled in `src/main/resources` and loaded with `Font.createFont`.

---

## 2026-11-25 — The code, simplified to beginner Java

**Goal:** rewrite the whole codebase so it only uses what an introductory Java course teaches, without changing how the game plays.
**What happened:** 516 places were changed, across 39 files of the game and all 15 checks and debug tools. A small script that ignores comments and strings counted them before and after: none are left.
- **No threads.** The game loop was a `Thread` that ran `update()` 60 times a second while Swing drew the screen on its own thread, which is why there were `volatile` fields, `synchronized` methods, a `ConcurrentLinkedQueue` for typed letters and `SwingUtilities.invokeLater`. The loop is now a `javax.swing.Timer` that calls `GamePanel.actionPerformed` every 16 ms, on Swing's thread, so none of that is needed. The battles already worked this way. Pausing for the main menu is now just stopping the timer. The word list that loaded on a background thread is now read at startup (it takes about 0.15 s).
- **No lambdas or method references.** The dialogue's "then do this" callbacks became fields in `Director` (`battleAfter`, `noticeAfter`, `journalAfter`, `endingAfter`), done in `Director.dialogueOver` when the box closes. Battles start with `GamePanel.startBattle` and end by calling `GamePanel.battleOver`; the checks look at `GamePanel.battleMonster` instead of swapping in their own `battleStarter`. The word battle's animation script was a queue of lambdas; each step is now a name (`"impact"`, `"enemyAttacks"`, `"won"`...) handled by `BattlePanel.doStep`, with the values it needs kept in fields. Keys use one named `KeyAction` class. The menu calls `Main.startGame()` and the others directly. Panels implement `ActionListener`, `KeyListener` and the mouse listeners themselves instead of anonymous adapter classes. The maps' floor patterns (`Lab::underlay`...) became classes implementing `MapBuilder.Underlay` (`new Lab()`).
- **No records, `switch` arrows, `?:`, streams or `Optional`.** The 12 records became plain classes with the same getter names, so nothing that used them changed. Every `?:` became `if`/`else`, every `case X ->` a classic `case X:` or an `if` chain. Streams, `removeIf`, `getOrDefault`, `computeIfAbsent`, iterators and `ArrayDeque` became loops and `ArrayList`s. `HealthBar`'s enum with fields became two objects, `HealthBar.PLAYER` and `HealthBar.MONSTER`. The hallway flicker's bit-mixing hash (`>>>`, `^=`) became a `Random` with a 1 in 300 chance of a burst each frame, the same average as before. `++x` and `--x` hidden inside conditions, and chained assignments like `a = b = 0`, were split into plain statements.
**Problems:**
- In `ViewCheck`, a `for` loop that moved two variables at once (`up = up.right, down = ...`) became a `while` loop with the step at the end, but the loop body has a `continue`, which would have skipped the step and looped forever. The step is now done before that `continue` too.
- The typing race's damage maths was first rewritten in a different order. With decimals, a different order can round differently, so it was put back in the original order.
**Decisions:**
- **Kept:** interfaces (`Backdrop`, `Underlay`, the listeners), plain enums, `List.of`/`Map.of` for fixed lists, try-with-resources for reading files, and the nested classes inside `BattleScreen`. These are part of normal beginner Java, or would need a much bigger reshuffle to remove.
- **Same behaviour, checked three ways:** all 11 checks pass (542 tests, as before), and every check's printed output was compared line by line with the old code's: the only differences are the tree-hit damage, which is random in both, and one check's renamed text. Screens were drawn to pictures (the school view is pixel-identical), a full attack was played through the word battle's new step script, and the whole loop was played in a real window with simulated keys: the menu, walking, Esc and Continue, the journal, a battle opening and closing, and the ending going back to the menu.
- **Timing:** a Swing timer ticks every 16 ms, so the game now runs at about 62 frames a second instead of exactly 60 (about 3% faster); the battles were already 62.
**Open issues:** the step-by-step guides show code as it was when each was written; the snippets that used records, method references and `switch` arrows were updated, but older ones may still show earlier versions of the code.

---

## Open issues and ideas

- ~~Redraw `down_idle`'s head to match the walk frames~~ (done: the new sprites, 2026-11-12; Down walks `walk1 → idle → walk2 → idle` again).
- ~~Make the west stairs work~~ (done: Floor 3, 2026-11-01). ~~Floor 2~~ (done: the labs, 2026-11-01). ~~Floor 1~~ (done: the ground floor, 2026-11-02). ~~The quadrangle~~ (done: 2026-11-03). Next: the last-stand gameplay.
- Hazard tiles (toxic puddles, slime) that drain health, the first real use of the HP bar.
- A way to open the sealed lab doors (the Security Keycard could open them too?).
- ~~Old unused images in `res/tiles/` (outside `Classroom2` and `Stairs`) can be deleted.~~ (done: the cleanup, 2026-11-03)
- Design the real bottom UI (art, layout, what it shows). `BottomUI` is a placeholder, apart from the health bar (done: the pixel-art bars, 2026-11-25).
- ~~Nothing changes `health` yet~~ (done: battles, 2026-11-15).
- Most of the project, including the 2026-11-03 cleanup, isn't committed to git yet; commit it so the history is safe.
