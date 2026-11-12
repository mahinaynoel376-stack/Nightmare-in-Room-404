# Draws the stair art in the classroom tiles' palette:
#   - the staircase: one 224 x 256 picture (7 x 8 tiles), cut into staircase_0.png ... staircase_55.png
#     in reading order (TileSet.STAIRCASE_FIRST + row * 7 + col)
#   - stair_step_broken.png and stair_hole.png: see-through overlays (cracks, a caved-in hole) that go
#     on the OBJECTS layer, on top of any staircase tile
#   - rubble.png, and the three old tiles (stair_landing, stair_step, stair_rail), unchanged
# Usage: python make_stair_tiles.py <classroom tiles dir> <out dir> <preview.png>
import sys, random
from PIL import Image, ImageDraw
src, out, preview = sys.argv[1:4]
rnd = random.Random(404)
T = (0, 0, 0, 0)
# Palette sampled from the classroom tiles
FLOOR = {(56,59,72):(88,92,108), (38,40,51):(64,68,82), (50,53,66):(76,80,96)}   # floor -> lighter landing
NOSE, TREAD, TREAD2, SHADOW, RISER = (104,109,128), (70,74,90), (62,66,81), (38,40,51), (23,25,34)
VOID, VOID2 = (8,8,12), (16,17,23)
STONE = [(187,187,187), (151,151,151), (119,119,119), (83,83,83), (51,51,51)]
WOOD = [(150,108,66), (125,87,50), (94,65,38)]
OUTLINE = (34,34,34)

def new(): return Image.new('RGBA', (32, 32), T)

# ---------------------------------------------------------------------------------------------
# The old single tiles (no longer placed by the game, kept so this script still reproduces them)
# ---------------------------------------------------------------------------------------------

def landing():
    floor = Image.open(src + '/01_floor.png').convert('RGBA')
    im = new(); p = im.load(); f = floor.load()
    for y in range(32):
        for x in range(32):
            c = f[x, y][:3]
            p[x, y] = FLOOR.get(c, (min(c[0]+30,255), min(c[1]+30,255), min(c[2]+34,255))) + (255,)
    return im

def step():
    im = new(); p = im.load()
    for y in range(32):
        k = y % 8
        col = NOSE if k == 0 else TREAD if k < 4 else TREAD2 if k < 5 else SHADOW if k < 7 else RISER
        for x in range(32):
            p[x, y] = col + (255,)
    for _ in range(14):                       # Worn speckles on the treads
        x, y = rnd.randrange(32), rnd.randrange(32)
        if 1 <= y % 8 <= 4: p[x, y] = (TREAD2 if rnd.random() < .5 else (80,84,100)) + (255,)
    return im

def rail():
    strip = Image.open(src + '/07_wall_v_left_1.png').convert('RGBA').crop((0, 0, 17, 32))
    im = new(); im.paste(strip, (8, 0), strip)   # Centre the stone strip in the tile
    return im

def rubble():
    im = new(); d = ImageDraw.Draw(im)
    d.ellipse([3, 22, 29, 31], fill=(18, 20, 28, 170))                 # Shadow under the pile
    chunks = [(6,15,15,25,1), (14,10,25,22,2), (19,18,28,27,3), (4,21,11,28,2), (12,20,19,28,1), (22,12,27,17,1)]
    for (x0, y0, x1, y1, s) in chunks:
        d.polygon([(x0+1,y0), (x1,y0+1), (x1,y1-1), (x1-2,y1), (x0,y1), (x0,y0+2)], fill=STONE[s] + (255,), outline=OUTLINE + (255,))
        d.line([x0+2, y0+1, x1-1, y0+1], fill=STONE[0] + (255,))         # Lit top edge
    d.line([8, 12, 17, 9], fill=WOOD[0] + (255,), width=2)              # Splintered plank
    d.line([8, 13, 17, 10], fill=WOOD[2] + (255,))
    for (x, y) in [(2,26), (29,24), (27,29), (5,30), (24,9)]:
        d.point((x, y), fill=STONE[2] + (255,))                          # Dust
    return im

# ---------------------------------------------------------------------------------------------
# Collapse overlays: see-through, so they sit on top of whichever staircase tile is under them
# ---------------------------------------------------------------------------------------------
orng = random.Random(4041)

def crack(p, x, y, n, col=RISER):
    for _ in range(n):
        if 0 <= x < 32 and 0 <= y < 32: p[x, y] = col + (255,)
        x += orng.choice((-1, 0, 1)); y += 1

def broken_step():
    # Cracks across the step and a chunk broken clean out of its right side: you can't step here
    im = new(); p = im.load()
    crack(p, 8, 0, 22); crack(p, 15, 4, 14); crack(p, 4, 14, 12)
    for y in range(32):
        edge = 17 + int(5 * abs(((y * 7) % 11) - 5) / 5) + (y // 12)
        for x in range(edge, 32):
            p[x, y] = (VOID if x > edge + 1 else SHADOW if x == edge + 1 else RISER) + (255,)
    for _ in range(8):                        # Loose bits in the gap
        x, y = orng.randrange(edge + 3, 31), orng.randrange(2, 30)
        p[x, y] = STONE[3] + (255,)
    for _ in range(10):                       # Grit on what's left of the step
        x, y = orng.randrange(0, 15), orng.randrange(0, 32)
        p[x, y] = (40, 42, 52, 255)
    return im

def hole():
    # The step has caved in: a black drop with a ragged edge (the step still shows above it)
    im = new(); p = im.load()
    for x in range(32):
        top = 3 + (x * 5 % 7) // 2 + (2 if 10 < x < 16 else 0)
        for y in range(top, 32):
            p[x, y] = (VOID2 if (x * 3 + y * 5) % 13 == 0 else VOID) + (255,)
        p[x, top] = RISER + (255,)
        if top > 0: p[x, top - 1] = (30, 32, 42, 255)           # Broken lip
    d = ImageDraw.Draw(im)
    for (x, y, w, h, c) in [(5,20,5,3,STONE[3]), (17,24,6,4,STONE[4]), (24,15,3,3,STONE[3]), (11,27,3,2,WOOD[2])]:
        d.rectangle([x, y, x + w - 1, y + h - 1], fill=c + (255,))
        d.line([x, y, x + w - 1, y], fill=STONE[2] + (255,))
    return im

# ---------------------------------------------------------------------------------------------
# The staircase: 7 x 8 tiles, a box standing in the stairwell, seen from above and slightly in
# front (like the rest of the game)
#
#   col 0       col 1-2              col 3      col 4-5              col 6
#   ------------------------ row 0: the back balustrade, closing the box ------------------------
#   balustrade  DOWN flight (a pit)  divider    UP flight (rising)   balustrade
#   rows 1-2: the landings (the pit's floor deep down / a raised landing), rows 3-7: the flights
#   ------------------- a stone threshold along the bottom (you can walk over it) -------------------
#
# Light comes from the top left. Depth comes from: the pit's inner walls widening and everything
# in it darkening as it goes down (its far wall carrying on below floor level), the up flight's
# risers facing you and its steps getting lighter as they climb, cast shadows, and the
# balustrades' front faces. The back balustrade runs left-right, so its railing shows its posts
# from the side, like a real handrail.
# ---------------------------------------------------------------------------------------------
W, H = 224, 256
OY = 32                                                    # The flights and landings start one row down
srng = random.Random(4040)
COLS, ROWS = W // 32, H // 32

def staircase():
    floor = Image.open(src + '/01_floor.png').convert('RGBA').load()
    wall = Image.open(src + '/02_wall.png').convert('RGBA').load()
    base = Image.open(src + '/03_wall_base.png').convert('RGBA').load()
    im = Image.new('RGBA', (W, H), (0, 0, 0, 255)); p = im.load()

    def scale(c, f): return tuple(max(0, min(255, int(round(v * f)))) for v in c[:3])
    def mix(a, b, t): return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))
    def put(x, y, c):
        if 0 <= x < W and 0 <= y < H: p[x, y] = tuple(c[:3]) + (255,)
    def get(x, y): return p[x, y][:3]
    def fl(x, y, f=1.0): return scale(floor[x % 32, y % 32], f)

    # Everything starts as the stairwell floor (only the edges of the outer columns stay floor)
    for y in range(H):
        for x in range(W):
            put(x, y, fl(x, y))

    # ---- UP flight (x 128-191): a raised landing, then 10 steps whose risers face you ----
    UX0, UX1 = 128, 191
    TREAD_UP, RISER_UP, NOSE_UP = (80, 84, 101), (37, 39, 51), (130, 135, 154)
    for y in range(OY, OY + 53):
        for x in range(UX0, UX1 + 1): put(x, y, fl(x, y, 1.5))
    for x in range(UX0, UX1 + 1):
        put(x, OY + 53, NOSE_UP)
        for y in range(OY + 54, OY + 64): put(x, y, scale(RISER_UP, 1.25 - (y - OY - 54) * 0.03))
    for i in range(10):                                   # i = 0: the top (highest) step
        y0 = OY + 64 + 16 * i
        f = 1.3 - 0.05 * i                                 # Higher steps catch more light
        for x in range(UX0, UX1 + 1):
            put(x, y0, scale(RISER_UP, 0.75))              # Shadow in the corner under the riser above
            for y in range(y0 + 1, y0 + 9): put(x, y, scale(TREAD_UP, f))
            put(x, y0 + 9, scale(NOSE_UP, f))              # The step's lit front edge
            for y in range(y0 + 10, y0 + 16): put(x, y, scale(RISER_UP, f * (1.05 - (y - y0 - 10) * 0.04)))
        for _ in range(10):                                # Worn speckles on the tread
            x, y = srng.randrange(UX0, UX1 + 1), srng.randrange(y0 + 2, y0 + 8)
            put(x, y, scale(TREAD_UP, f * 0.88))
    for y in range(OY, H):                                 # The divider's shadow falls across the left edge
        for k in range(7):
            put(UX0 + k, y, scale(get(UX0 + k, y), 0.55 + 0.065 * k))
    for y in range(OY, OY + 10):                           # ...and the back balustrade's across the landing
        for x in range(UX0, UX1 + 1):
            put(x, y, scale(get(x, y), 0.55 + 0.045 * (y - OY)))

    # ---- DOWN flight (x 32-95): a pit going down into the dark ----
    DX0, DX1 = 32, 95
    TREAD_DN, NOSE_DN = (72, 76, 93), (112, 117, 136)
    WALL_SHADE, WALL_LIT = (30, 32, 42), (60, 63, 78)      # The pit's inner walls: facing right / facing left
    def depth(y):                                          # 0 at floor level ... 1 at the bottom of the pit
        y -= OY
        return (223 - y) / 159 * 0.75 if y >= 64 else 0.75 + (63 - y) / 63 * 0.25
    # The far end, deep down: the pit's far wall carries on below floor level, then the lower landing
    for y in range(OY, OY + 64):
        for x in range(DX0, DX1 + 1):
            if y < OY + 18:   c = scale(wall[x % 32, (y - OY + 14) % 32], 0.9)
            elif y < OY + 24: c = scale(base[x % 32, (y - OY + 8) % 32], 0.9)
            else:             c = fl(x, y, 0.85)
            put(x, y, c)
    # 10 steps, each one lower than the one in front of it
    for j in range(10):                                    # j = 0: the step nearest you (at floor level)
        y0 = OY + 208 - 16 * j
        for x in range(DX0, DX1 + 1):
            put(x, y0, NOSE_DN)                            # Lit edge where it drops to the next step
            for y in range(y0 + 1, y0 + 12): put(x, y, TREAD_DN)
            for y in range(y0 + 12, y0 + 16): put(x, y, mix(TREAD_DN, (20, 21, 28), (y - y0 - 11) / 4))
        for _ in range(8):
            x, y = srng.randrange(DX0, DX1 + 1), srng.randrange(y0 + 2, y0 + 11)
            put(x, y, scale(TREAD_DN, 0.88))
    # Inner walls, widening as the pit gets deeper (a little perspective)
    for y in range(OY, H):
        w = int(round(depth(y) * 12))
        for k in range(w):
            put(DX0 + k, y, scale(WALL_SHADE, 1.0 - 0.15 * (k / max(w, 1))))
            put(DX1 - k, y, scale(WALL_LIT, 0.8 + 0.2 * (k / max(w, 1))))
        if w > 0:
            put(DX0 + w, y, scale(get(DX0 + w, y), 0.6))   # Where the wall meets the steps
            put(DX1 - w, y, scale(get(DX1 - w, y), 0.75))
            if (y - OY) % 16 == 0 and y >= OY + 64:        # Course lines along the walls, one per step
                for k in range(w):
                    put(DX0 + k, y, scale(get(DX0 + k, y), 0.75)); put(DX1 - k, y, scale(get(DX1 - k, y), 0.85))
    # Darkness: the deeper, the darker, almost black at the bottom
    for y in range(OY, H):
        f = 1.0 - 0.82 * depth(y) ** 1.2
        for x in range(DX0, DX1 + 1):
            put(x, y, scale(get(x, y), f))
    for y in range(OY, H):                                 # The rim of the floor along the pit's edges
        put(DX0, y, (20, 21, 28)); put(DX1, y, scale((70, 74, 90), 1.0 - 0.6 * depth(y)))

    # ---- Balustrades: a stone wall with an iron handrail and posts on top ----
    RAIL, RAIL_HI, POST, POST_HI, IRON_DARK = (44, 45, 54), (104, 106, 122), (36, 37, 45), (128, 130, 146), (16, 16, 21)
    TOP = 10                                               # Top edge of the back balustrade's stone
    FRONT = H - 22                                         # Where the side walls' tops end and their front faces start

    def stone_top(x, y, x0, x1, lift):
        c = STONE[3]
        if x == x0 + 1: c = STONE[1]
        elif x == x0 + 2: c = STONE[2]
        elif x == x1 - 1: c = STONE[4]
        elif (y + x * 3) % 37 == 0 or ((x - x0) == 6 and y % 11 < 4): c = STONE[4]   # Wear and seams
        return scale(c, 1.0 + lift)

    def post(xc, py, r, h):                                # An iron post on a side wall: a little box
        for y in range(py, py + h):
            for x in range(xc - r, xc + r + 1): put(x, y, POST)
            put(xc - r - 1, y, IRON_DARK); put(xc + r + 1, y, IRON_DARK)
        for x in range(xc - r, xc + r + 1): put(x, py, POST_HI)
        for x in range(xc - r - 1, xc + r + 2): put(x, py - 1, IRON_DARK); put(x, py + h, IRON_DARK)
        for y in range(py + h + 1, py + h + 4):
            for x in range(xc - r + 1, xc + r + 3): put(x, y, scale(get(x, y), 0.65))   # Shadow

    # The back balustrade (row 0), running left-right across the whole box: its top, then its front
    # face over the landings, and a railing standing on it whose posts you see from the side
    BX0, BX1 = 8, 215
    for y in range(TOP, 18):
        for x in range(BX0, BX1 + 1):
            c = STONE[3]
            if y == TOP: c = OUTLINE
            elif y == TOP + 1: c = STONE[1]
            elif y == TOP + 2: c = STONE[2]
            elif (x * 5 + y) % 41 == 0: c = STONE[4]
            put(x, y, c)
    for y in range(18, OY):
        t = (y - 18) / (OY - 18)
        for x in range(BX0, BX1 + 1):
            put(x, y, mix(STONE[2], STONE[4], t) if (x % 32) != 31 else STONE[4])
    for x in range(BX0, BX1 + 1):
        put(x, 18, STONE[0]); put(x, OY - 1, (20, 21, 28))
    for y in range(TOP, OY):
        put(BX0, y, OUTLINE); put(BX1, y, OUTLINE)
    for x in range(BX0 + 4, BX1 - 3):                      # The handrail along the top, and its shadow
        put(x, 1, IRON_DARK); put(x, 2, RAIL_HI); put(x, 3, RAIL); put(x, 4, IRON_DARK)
        put(x + 2, 13, scale(get(x + 2, 13), 0.7))
    for bx in range(BX0 + 8, BX1 - 4, 8):                  # Balusters: one every 8 px, standing on the wall
        thick = bx % 32 == 16
        for y in range(5, TOP):
            put(bx, y, POST_HI if not thick else POST)
            put(bx + 1, y, POST)
            if thick:
                put(bx - 1, y, POST_HI); put(bx + 2, y, IRON_DARK)
        put(bx + 2, TOP + 1, scale(get(bx + 2, TOP + 1), 0.7)); put(bx + 3, TOP + 2, scale(get(bx + 3, TOP + 2), 0.7))

    def balustrade(x0, x1, rises):
        xc = (x0 + x1) // 2
        for y in range(TOP + 1, FRONT):                    # The top of the wall, joining the back one
            lift = 0.0
            if rises:                                      # Rising beside the up flight: lighter further up
                lift = 0.22 if y < OY else (1.0 - (y - OY) / (FRONT - OY)) * 0.22
            for x in range(x0, x1 + 1):
                put(x, y, stone_top(x, y, x0, x1, lift))
            put(x0, y, OUTLINE); put(x1, y, OUTLINE)
            if (y - OY) % 32 == 31 and y > OY:             # Joints between the blocks
                for x in range(x0 + 1, x1): put(x, y, scale(STONE[4], 1.0 + lift))
        for y in range(FRONT, H):                          # The wall's front face, facing you
            t = (y - FRONT) / (H - FRONT)
            for x in range(x0, x1 + 1):
                c = mix(STONE[2], STONE[4], t)
                if x == x0 + 1: c = mix(STONE[1], STONE[3], t)
                put(x, y, c)
            put(x0, y, OUTLINE); put(x1, y, OUTLINE)
        for x in range(x0, x1 + 1):
            put(x, FRONT, STONE[0]); put(x, H - 1, (20, 21, 28))
        # The handrail's shadow on the wall top, then the rail itself
        for y in range(TOP + 4, FRONT - 6):
            for x in (xc + 2, xc + 3): put(x, y + 2, scale(get(x, y + 2), 0.6))
            put(xc - 1, y, RAIL_HI); put(xc, y, RAIL); put(xc + 1, y, IRON_DARK)
        # Posts: a thin baluster at every step, a thicker one at every block
        for py in range(OY + 6, FRONT - 20, 16):
            big = (py - OY - 6) % 32 == 0
            post(xc, py, 3 if big else 2, 5 if big else 4)
        # A thick newel post at the bottom end...
        ny = FRONT - 16
        for y in range(ny, ny + 14):
            for x in range(xc - 5, xc + 6):
                put(x, y, POST if x > xc - 4 else POST_HI)
            put(xc - 6, y, IRON_DARK); put(xc + 6, y, IRON_DARK)
        for x in range(xc - 6, xc + 7):
            put(x, ny - 1, IRON_DARK); put(x, ny + 14, IRON_DARK)
        for x in range(xc - 4, xc + 5): put(x, ny, POST_HI); put(x, ny + 1, POST_HI)
        for y in range(ny + 15, ny + 18):
            for x in range(xc - 4, xc + 8): put(x, y, scale(get(x, y), 0.7))     # Newel shadow on the wall top
        # ...and a tall corner post where it meets the back balustrade, holding both handrails
        for y in range(0, TOP + 8):
            for x in range(xc - 4, xc + 5):
                put(x, y, POST if x > xc - 3 else POST_HI)
            put(xc - 5, y, IRON_DARK); put(xc + 5, y, IRON_DARK)
        for x in range(xc - 5, xc + 6):
            put(x, 0, IRON_DARK); put(x, TOP + 8, IRON_DARK)
        for x in range(xc - 3, xc + 4): put(x, 1, POST_HI)
        for y in range(TOP + 9, TOP + 12):
            for x in range(xc - 3, xc + 7): put(x, y, scale(get(x, y), 0.7))

    # Floor-side shadows next to the outer walls (light from the left: shadow on their right side)
    for y in range(H):
        for k in range(4): put(W - 8 + k, y, scale(get(W - 8 + k, y), 0.6 + 0.1 * k))
    balustrade(8, 31, False)        # Left: along the pit, at floor level
    balustrade(98, 125, True)       # Middle: between the pit and the up flight
    balustrade(192, 215, True)      # Right: along the up flight, rising with it
    for y in range(OY, H):                                 # Dark gaps either side of the divider
        put(96, y, scale(get(96, y), 0.5)); put(97, y, (20, 21, 28))
        put(126, y, (20, 21, 28)); put(127, y, scale(get(127, y), 0.5))
    # A stone threshold along the bottom of both flights, closing the box (you walk over it)
    for x0, x1 in ((32, 95), (128, 191)):
        for x in range(x0, x1 + 1):
            put(x, H - 6, STONE[0]); put(x, H - 5, STONE[1])
            for y in range(H - 4, H - 1): put(x, y, STONE[3] if (x + y) % 23 else STONE[4])
            put(x, H - 1, (20, 21, 28))
    for y in range(H):
        for x in range(0, 8): put(x, y, fl(x, y, 0.92 if x > 4 else 1.0))   # Floor left of the left wall
    return im

def cut(im, cols, rows):
    return [im.crop((c * 32, r * 32, c * 32 + 32, r * 32 + 32)) for r in range(rows) for c in range(cols)]

# The old single tiles first, in the same order as before, so their random speckles don't change
tiles = {'stair_landing': landing(), 'stair_step': step(), 'stair_rail': rail(),
         'stair_step_broken': broken_step(), 'stair_hole': hole(), 'rubble': rubble()}
for name, im in tiles.items():
    im.save(f'{out}/{name}.png')
big_stairs = staircase()
for i, t in enumerate(cut(big_stairs, COLS, ROWS)):
    t.save(f'{out}/staircase_{i}.png')

# Preview: the staircase at 3x, then the single tiles at 3x
pw = max(W * 3, len(tiles) * 100)
sheet = Image.new('RGBA', (pw, H * 3 + 110), (255, 0, 255, 255))
sheet.paste(big_stairs.resize((W * 3, H * 3), Image.NEAREST), (0, 0))
for i, im in enumerate(tiles.values()):
    tile = im.resize((96, 96), Image.NEAREST); sheet.paste(tile, (i * 100 + 2, H * 3 + 8), tile)
sheet.save(preview)
print('ok', list(tiles), '+', COLS * ROWS, 'staircase tiles')
