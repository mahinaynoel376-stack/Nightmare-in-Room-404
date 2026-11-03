# Art scripts

These Python scripts made most of the game's tile images. They **cut** tiles out of the tile sheets you drew, **recolour** some of them, and **draw** new ones in code: the stairs, the big library door, the lab doors, the room plaques, the school bus and the fences.

They're kept so the art can be remade, tweaked, or used to learn from. **The game never runs them.** It only loads the finished PNGs in `src/main/resources/tiles/`.

## What you need

- Python 3
- Pillow, the image library: `pip install pillow`

## The scripts

Run every command from the **project folder** (`GameProd`). Each script **overwrites** the tiles in its output folder and saves a zoomed-in preview picture, which isn't part of the game.

| Script | Makes | Reads |
|---|---|---|
| `make_stair_tiles.py` | the 6 stair tiles | the classroom tiles (for the palette) |
| `make_library_tiles.py` | the 43 library tiles | the library sheet and the classroom wall |
| `make_lab_tiles.py` | the 51 lab tiles | the lab sheet and the library sheet |
| `make_ground_tiles.py` | the 31 ground-floor tiles | the ground sheet |
| `make_quad_tiles.py` | the 46 quadrangle tiles **and** the ground floor's 3 grass tiles | the quad sheet |
| `make_quad_maps.py` | the quadrangle's layout (not images) | nothing |

```bat
python tools/art/make_stair_tiles.py   src/main/resources/tiles/classroom src/main/resources/tiles/stairs stairs_preview.png
python tools/art/make_library_tiles.py docs/images/floor3-library/library_sheet.png src/main/resources/tiles/classroom/02_wall.png src/main/resources/tiles/library library_preview.png
python tools/art/make_lab_tiles.py     docs/images/floor2-lab/lab_sheet.png docs/images/floor3-library/library_sheet.png src/main/resources/tiles/lab lab_preview.png
python tools/art/make_ground_tiles.py  docs/images/floor1-ground/ground_sheet.png src/main/resources/tiles/ground ground_preview.png
python tools/art/make_quad_tiles.py    docs/images/quadrangle/quad_sheet.png src/main/resources/tiles/quad src/main/resources/tiles/ground quad_preview.png
```

**The order matters:** run `make_ground_tiles.py` **before** `make_quad_tiles.py`. The quad script replaces the ground floor's grass with the quad sheet's grass, so the building and the quadrangle share the same ground. Running the ground script afterwards would bring the old grass back.

The scripts use fixed random seeds, so they give the same pixels every time. On 2026-11-03, running all five in this order reproduced all 177 tiles in the game exactly.

## The quadrangle's layout

`make_quad_maps.py` lays out the 50 × 36 quadrangle as two character maps, `GROUND` and `THINGS`, and prints them as Java:

```bat
python tools/art/make_quad_maps.py > quad_maps.txt
```

To change the layout:
1. Edit the script.
2. Run it.
3. Paste the two printed arrays over the ones in `src/main/java/nightmare404/map/Quadrangle.java`.
4. Run `run-checks.bat`: `QuadCheck` checks the strongholds still work.

The arrays in `Quadrangle.java` currently match the script's output exactly.

## The classroom tiles

The 36 classroom tiles (`tiles/classroom`) and the player sprites were made by hand, so no script makes them.
