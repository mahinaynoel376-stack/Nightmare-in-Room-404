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

---

## Open issues and ideas

- Redraw `down_idle`'s head to match the walk frames (then Down can go back to `walk1 → idle → walk2 → idle`).
- Make the west stairs work: build Floor 3, link `floor4.below` and `floor3.above`, and add a stairs trigger.
- Old unused images in `res/tiles/` (outside `Classroom2` and `Stairs`) can be deleted.
- Design the real bottom UI (art, layout, what it shows). `BottomUI` is a placeholder.
- Nothing changes `health` yet: it's always 100 until there are hazards or enemies.
- `src/` and `res/` aren't committed to git yet; commit them so the history is safe.
