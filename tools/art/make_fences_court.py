# The quadrangle's fences, its main (south) gate, and the basketball court, all drawn in code.
# Every fence tile has a see-through background, so the grass, paving or asphalt under it shows.
#
#   fence_h.png            chain-link fence running across the screen: posts, top and bottom rails, mesh
#   fence_v.png            the same fence running up and down the screen. Seen from above and in front,
#                          it's a thin upright line of rail and posts (not the front of the fence lying flat)
#   fence_corner_tl/tr/bl/br.png   corner posts where the two meet (tl = top-left corner of a fenced area)
#   gate_v.png, gate_v_2.png       a padlocked side gate, 2 tiles tall (top, bottom)
#   gate_pillar.png        a brick pillar either side of the main gate
#   gate_l.png, gate_r.png         the main gate: two iron leaves, chained and padlocked in the middle
#   gate_open_l.png, gate_open_r.png   the same, open (the leaves swung out, the chain on the ground)
#   court_0..153.png       the basketball court's surface, 14 x 11 tiles, cut in reading order
#   hoop_w_pole/ring/base.png, hoop_e_ring/pole/base.png
#                          a hoop at each end of the court, 2 x 2 tiles: the pole's base (solid), the pole's
#                          top with the backboard, and the ring with its net (both up in the air)
# All in src/main/resources/tiles/quad. Run it after make_quad_tiles.py (which no longer writes the
# fence or hoop tiles).
# Usage: python tools/art/make_fences_court.py [preview.png]   (from the project folder)
import math
import random
import sys

from PIL import Image, ImageDraw

OUT = 'src/main/resources/tiles/quad/'
T = (0, 0, 0, 0)
OUTLINE = (24, 26, 32, 255)
POST = (122, 126, 136, 255)
POST_LIGHT = (176, 180, 190, 255)
POST_DARK = (70, 72, 82, 255)
RAIL = (146, 150, 160, 255)
MESH = (150, 156, 166, 150)            # The chain-link, half see-through
SHADOW = (0, 0, 0, 70)
IRON = (58, 66, 66, 255)
IRON_LIGHT = (132, 144, 142, 255)
RUST = (120, 66, 40, 255)
BRICK = (128, 58, 46, 255)
BRICK_DARK = (90, 38, 32, 255)
MORTAR = (150, 132, 116, 255)
STONE = (168, 164, 154, 255)
STONE_DARK = (120, 116, 108, 255)
CHAIN = (150, 150, 140, 255)
LOCK = (196, 160, 60, 255)
LOCK_DARK = (120, 92, 30, 255)

BASE = 29                               # The line a fence stands on, in a 32 x 32 tile
TOP = 7                                 # The height of its top rail


def tile():
    return Image.new('RGBA', (32, 32), T)


# ---------------------------------------------------------------- chain-link fence, across

# The fence across the screen, between x0 and x1 (a post at x0 if post_at_start)
def fence_across(d, x0, x1, post_at_start=True):
    d.rectangle((x0, BASE, x1, BASE + 2), fill=SHADOW)                     # Its shadow on the ground
    for y in range(TOP + 2, BASE - 1):                                     # The mesh: little diamonds
        for x in range(x0, x1 + 1):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                d.point((x, y), fill=MESH)
    d.line((x0, TOP, x1, TOP), fill=RAIL)                                  # Top rail
    d.line((x0, TOP + 1, x1, TOP + 1), fill=POST_DARK)
    d.line((x0, BASE - 2, x1, BASE - 2), fill=POST_DARK)                   # Bottom rail
    if post_at_start:
        post(d, x0 + 1)


# A post standing on the base line, centred on x
def post(d, x):
    d.rectangle((x - 1, TOP - 2, x + 1, BASE), fill=POST, outline=OUTLINE)
    d.line((x - 1, TOP - 1, x - 1, BASE - 1), fill=POST_LIGHT)
    d.rectangle((x - 2, TOP - 3, x + 2, TOP - 2), fill=POST_LIGHT, outline=OUTLINE)   # Its cap


# ---------------------------------------------------------------- chain-link fence, up and down

COL = 15                                # The fence's line, up the middle of the tile


# The fence up and down the screen, from y0 to y1: seen from above, an upright rail with a little
# mesh, and a shadow falling to the right
def fence_up(d, y0, y1):
    d.rectangle((COL + 3, y0, COL + 5, y1), fill=SHADOW)
    d.rectangle((COL - 1, y0, COL + 2, y1), fill=MESH)
    d.line((COL - 1, y0, COL - 1, y1), fill=OUTLINE)
    d.line((COL, y0, COL, y1), fill=RAIL)
    d.line((COL + 1, y0, COL + 1, y1), fill=POST_DARK)
    d.line((COL + 2, y0, COL + 2, y1), fill=OUTLINE)


# A post seen from above, at y (its cap): a little square, lit from the top left
def post_up(d, y):
    d.rectangle((COL - 2, y, COL + 3, y + 5), fill=POST, outline=OUTLINE)
    d.line((COL - 1, y + 1, COL + 2, y + 1), fill=POST_LIGHT)
    d.line((COL - 1, y + 1, COL - 1, y + 4), fill=POST_LIGHT)


made = {}

im = tile()
fence_across(ImageDraw.Draw(im), 0, 31)
made['fence_h'] = im

im = tile()
d = ImageDraw.Draw(im)
fence_up(d, 0, 31)
post_up(d, 13)
made['fence_v'] = im

# Corners: a bigger post where the fence turns, with each half going its way
for name, across, up in (('fence_corner_tl', (COL, 31), (BASE - 2, 31)), ('fence_corner_tr', (0, COL), (BASE - 2, 31)),
                         ('fence_corner_bl', (COL, 31), (0, BASE)), ('fence_corner_br', (0, COL), (0, BASE))):
    im = tile()
    d = ImageDraw.Draw(im)
    fence_up(d, up[0], up[1])
    fence_across(d, across[0], across[1], post_at_start=False)
    d.rectangle((COL - 2, TOP - 3, COL + 3, BASE), fill=POST, outline=OUTLINE)   # The corner post, thicker
    d.line((COL - 1, TOP - 2, COL - 1, BASE - 1), fill=POST_LIGHT)
    d.rectangle((COL - 3, TOP - 5, COL + 4, TOP - 2), fill=POST_LIGHT, outline=OUTLINE)
    made[name] = im


# ---------------------------------------------------------------- the side gates (2 tiles tall)

def gate_up(d, y0, y1):
    d.rectangle((COL + 4, y0, COL + 7, y1), fill=SHADOW)
    d.rectangle((COL - 2, y0, COL + 3, y1), fill=IRON, outline=OUTLINE)    # Heavier than the fence
    d.line((COL - 1, y0, COL - 1, y1), fill=IRON_LIGHT)
    for y in range(y0 + 3, y1, 6):                                         # Its cross bars, end on
        d.line((COL - 1, y, COL + 2, y), fill=IRON_LIGHT)
    for y in range(y0 + 2, y1, 9):                                         # A little rust
        d.point((COL + 1, y), fill=RUST)


im = tile()                                         # Top: the hinge post, and the padlock at the bottom
d = ImageDraw.Draw(im)
gate_up(d, 0, 31)
post_up(d, 1)
d.line((COL - 4, 24, COL + 5, 24), fill=CHAIN)      # The chain round the gate, and the padlock on it
d.line((COL - 4, 26, COL + 5, 26), fill=CHAIN)
d.rectangle((COL + 3, 25, COL + 8, 30), fill=LOCK, outline=OUTLINE)
d.arc((COL + 3, 21, COL + 8, 27), 180, 360, fill=OUTLINE)
d.point((COL + 5, 27), fill=LOCK_DARK)
made['gate_v'] = im

im = tile()                                         # Bottom: the gate, and its post at the far end
d = ImageDraw.Draw(im)
gate_up(d, 0, 31)
post_up(d, 24)
made['gate_v_2'] = im


# ---------------------------------------------------------------- the main gate (the south gate)

# A brick pillar either side of the gate, taller than the fence, with a stone cap
im = tile()
d = ImageDraw.Draw(im)
fence_across(d, 0, 31, post_at_start=False)         # (The fence runs into it from both sides)
d.rectangle((6, 2, 26, 31), fill=SHADOW)
d.rectangle((5, 6, 25, 30), fill=BRICK, outline=OUTLINE)
for y in range(9, 30, 4):                           # Mortar lines, and the bricks staggered
    d.line((6, y, 24, y), fill=MORTAR)
    for x in range(7 if (y // 4) % 2 else 11, 25, 8):
        d.line((x, y - 3, x, y - 1), fill=BRICK_DARK)
d.rectangle((4, 1, 26, 6), fill=STONE, outline=OUTLINE)    # The cap
d.line((5, 2, 25, 2), fill=(206, 202, 192, 255))
d.line((5, 5, 25, 5), fill=STONE_DARK)
made['gate_pillar'] = im


# One iron leaf of the gate: bars with spear tips, rails, a heavy frame. The two leaves arch up to
# meet in the middle. hinge_left: the hinge is on the left (the left leaf)
def gate_leaf(hinge_left):
    im = tile()
    d = ImageDraw.Draw(im)
    d.rectangle((0, BASE, 31, BASE + 2), fill=SHADOW)

    def top(x):                                     # The arch: low at the hinge, high in the middle
        return 8 - round(6 * math.sin(math.pi / 2 * x / 31))
    for x in range(3, 31, 4):                       # The bars, with spear tips
        d.line((x - 1, top(x), x - 1, BASE - 1), fill=OUTLINE)
        d.line((x, top(x), x, BASE - 1), fill=IRON)
        d.line((x + 1, top(x) + 2, x + 1, BASE - 2), fill=IRON_LIGHT)
        d.polygon([(x - 1, top(x)), (x, top(x) - 3), (x + 1, top(x))], fill=IRON_LIGHT, outline=OUTLINE)
    for x in range(0, 32):                          # The arched top rail
        d.line((x, top(x) + 2, x, top(x) + 3), fill=IRON)
        d.point((x, top(x) + 2), fill=IRON_LIGHT)
    for y in (17, BASE - 1):                        # The rails
        d.rectangle((0, y, 31, y + 1), fill=IRON)
        d.line((0, y, 31, y), fill=IRON_LIGHT)
    d.rectangle((0, top(0), 1, BASE), fill=IRON, outline=OUTLINE)       # The frame
    d.rectangle((30, top(31), 31, BASE), fill=IRON, outline=OUTLINE)
    for y in range(10, 16, 2):                      # A curl of ironwork between the rails
        d.point((15 + (y - 10) // 2, y), fill=IRON_LIGHT)
    if not hinge_left:
        im = im.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    return im


left, right = gate_leaf(True), gate_leaf(False)
d = ImageDraw.Draw(left)                            # The chain across the middle, and the padlock on it
for x in range(20, 32, 3):
    d.ellipse((x, 15, x + 3, 18), outline=CHAIN)
d.rectangle((24, 19, 31, 26), fill=LOCK, outline=OUTLINE)       # A big padlock
d.arc((25, 14, 30, 22), 180, 360, fill=OUTLINE, width=2)
d.line((25, 20, 30, 20), fill=(240, 210, 110, 255))
d.point((27, 23), fill=LOCK_DARK); d.point((27, 24), fill=LOCK_DARK)
d = ImageDraw.Draw(right)
for x in range(0, 12, 3):
    d.ellipse((x, 15, x + 3, 18), outline=CHAIN)
made['gate_l'], made['gate_r'] = left, right


# Open: the leaves swung outwards, so all that shows is each one's edge by its hinge, and the
# chain and padlock lie on the ground
def gate_open(hinge_left):
    im = tile()
    d = ImageDraw.Draw(im)
    d.rectangle((1, 5, 4, 31), fill=SHADOW)
    d.rectangle((0, 5, 2, 31), fill=IRON, outline=OUTLINE)      # The leaf, end on, swung out towards you
    d.line((1, 6, 1, 30), fill=IRON_LIGHT)
    if hinge_left:
        for x in range(10, 26, 3):                              # The chain on the ground, and the lock, open
            d.ellipse((x, 26 + (x // 3) % 2, x + 3, 29 + (x // 3) % 2), outline=CHAIN)
        d.rectangle((24, 24, 29, 29), fill=LOCK, outline=OUTLINE)
        d.arc((22, 20, 27, 26), 180, 330, fill=OUTLINE)
    else:
        im = im.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    return im


made['gate_open_l'], made['gate_open_r'] = gate_open(True), gate_open(False)


# ---------------------------------------------------------------- the basketball court

CW, CH = 14 * 32, 11 * 32               # The court's tiles: 14 x 11
SURFACE = (54, 74, 66)                  # Old green paint, faded
KEY = (98, 44, 44)                      # The keys, a faded dark red
LINE = (200, 196, 180)                  # Worn white lines
rng = random.Random(404)
asphalt = Image.open(OUT + 'q_asphalt_1.png').convert('RGBA')
court = Image.new('RGBA', (CW, CH))
for x in range(0, CW, 32):              # The asphalt it's painted on (the same as round it)
    for y in range(0, CH, 32):
        court.alpha_composite(asphalt, (x, y))
d = ImageDraw.Draw(court)
L, R, U, D = 10, CW - 11, 10, CH - 11   # The court's edges (left, right, up, down)
MID_Y = CH // 2
BASKET = 37                             # How far in from each end line the basket is
d.rectangle((L, U, R, D), fill=SURFACE)
for side in (1, -1):                    # Each end: the key, the free-throw circle, the three-point line
    end = L if side == 1 else R
    key_x = end + side * 84
    d.rectangle((min(end, key_x), MID_Y - 38, max(end, key_x), MID_Y + 38), fill=KEY)
    bx = end + side * (BASKET - 10)
    d.arc((bx - 120, MID_Y - 120, bx + 120, MID_Y + 120), -90 if side == 1 else 90, 90 if side == 1 else 270, fill=LINE, width=2)
    d.line((end, MID_Y - 120, bx, MID_Y - 120), fill=LINE, width=2)
    d.line((end, MID_Y + 120, bx, MID_Y + 120), fill=LINE, width=2)
    d.rectangle((min(end, key_x), MID_Y - 38, max(end, key_x), MID_Y + 38), outline=LINE, width=2)
    d.arc((key_x - 30, MID_Y - 30, key_x + 30, MID_Y + 30), -90 if side == 1 else 90, 90 if side == 1 else 270, fill=LINE, width=2)
    d.ellipse((bx - 9, MID_Y - 4, bx + 9, MID_Y + 4), fill=(30, 40, 36))   # The ring's shadow on the ground
d.rectangle((L, U, R, D), outline=LINE, width=2)                           # Outside lines
d.line((CW // 2, U, CW // 2, D), fill=LINE, width=2)                       # Halfway line, centre circle
d.ellipse((CW // 2 - 40, MID_Y - 40, CW // 2 + 40, MID_Y + 40), outline=LINE, width=2)
d.ellipse((CW // 2 - 14, MID_Y - 14, CW // 2 + 14, MID_Y + 14), fill=KEY, outline=LINE, width=2)

px = court.load()                       # Worn: the paint flaked off here and there, and grain all over
for y in range(CH):
    for x in range(CW):
        r, g, b, a = px[x, y]
        n = rng.random()
        if n < 0.10 and (r, g, b) != asphalt.getpixel((x % 32, y % 32))[:3]:
            px[x, y] = asphalt.getpixel((x % 32, y % 32))                  # Paint worn away
        elif n < 0.35:
            k = rng.choice((-8, -5, 5))
            px[x, y] = (max(0, r + k), max(0, g + k), max(0, b + k), a)
for _ in range(5):                      # Cracks across it
    x, y = rng.randint(30, CW - 30), rng.randint(30, CH - 30)
    for _ in range(rng.randint(10, 22)):
        nx, ny = x + rng.randint(-3, 3), y + rng.randint(1, 3)
        d.line((x, y, nx, ny), fill=(22, 24, 24))
        x, y = nx, ny
for _ in range(14):                     # Weeds coming up at the edges
    x = rng.choice((rng.randint(2, 8), rng.randint(CW - 9, CW - 3), rng.randint(4, CW - 4)))
    y = rng.randint(2, CH - 3) if x < 10 or x > CW - 11 else rng.choice((rng.randint(1, 7), rng.randint(CH - 8, CH - 2)))
    d.line((x, y, x - 1, y - 3), fill=(70, 110, 50)); d.line((x, y, x + 2, y - 2), fill=(86, 130, 60))
bx, by = 70, MID_Y + 70                 # A flat old basketball by the west key
d.ellipse((bx - 7, by - 5, bx + 7, by + 5), fill=(190, 96, 40), outline=(40, 20, 10))
d.arc((bx - 7, by - 5, bx + 7, by + 5), 200, 340, fill=(40, 20, 10))
d.line((bx, by - 5, bx, by + 5), fill=(40, 20, 10))
d.line((bx - 4, by - 2, bx - 2, by - 3), fill=(230, 150, 90))

for i in range(14 * 11):
    c, r = i % 14, i // 14
    made[f'court_{i}'] = court.crop((c * 32, r * 32, c * 32 + 32, r * 32 + 32))


# ---------------------------------------------------------------- the hoops

# The west hoop, 2 x 2 tiles: the pole stands behind the end line, the backboard faces the court
# (seen almost end on from here), and the ring sticks out over the court, up in the air
hoop = Image.new('RGBA', (64, 64), T)
d = ImageDraw.Draw(hoop)
d.ellipse((1, 57, 17, 63), fill=SHADOW)                                     # The pole's shadow
d.rectangle((2, 48, 14, 61), fill=(52, 60, 96), outline=OUTLINE)            # Its padded base (school blue)
d.line((3, 49, 3, 60), fill=(90, 100, 150))
d.line((3, 49, 13, 49), fill=(90, 100, 150))
d.rectangle((6, 8, 10, 50), fill=POST, outline=OUTLINE)                     # The pole
d.line((7, 9, 7, 49), fill=POST_LIGHT)
d.rectangle((10, 10, 19, 14), fill=POST, outline=OUTLINE)                   # Its arm out to the board
d.line((11, 11, 18, 11), fill=POST_LIGHT)
d.polygon([(18, 0), (27, 3), (27, 31), (18, 28)], fill=(228, 228, 220), outline=OUTLINE)   # Backboard
d.line((19, 2, 19, 27), fill=(186, 186, 178))
d.polygon([(21, 12), (26, 14), (26, 24), (21, 22)], outline=(200, 40, 40))  # The square on it
d.ellipse((25, 15, 50, 25), outline=OUTLINE, width=1)                       # The ring, from just above
d.ellipse((26, 16, 49, 24), outline=(230, 110, 36), width=2)
d.arc((26, 16, 49, 24), 200, 340, fill=(255, 160, 80))
for i, x in enumerate(range(29, 48, 3)):                                    # What's left of the net
    sway = 1 if i % 2 else -1
    d.line((x, 24, x + (37 - x) // 3 + sway, 31), fill=(214, 214, 204))
d.line((31, 28, 44, 28), fill=(194, 194, 184))
made['hoop_w_pole'] = hoop.crop((0, 0, 32, 32))
made['hoop_w_ring'] = hoop.crop((32, 0, 64, 32))
made['hoop_w_base'] = hoop.crop((0, 32, 32, 64))
east = hoop.transpose(Image.Transpose.FLIP_LEFT_RIGHT)                     # The east hoop faces the other way
made['hoop_e_ring'] = east.crop((0, 0, 32, 32))
made['hoop_e_pole'] = east.crop((32, 0, 64, 32))
made['hoop_e_base'] = east.crop((32, 32, 64, 64))
# (The ring's piece of the west hoop reaches back into the pole's tile, so put it there too)
pole = made['hoop_w_pole']
pole.alpha_composite(hoop.crop((0, 0, 32, 32)))

for name, im in made.items():
    im.save(OUT + name + '.png')
print('Saved', len(made), 'tiles to', OUT)


# ---------------------------------------------------------------- preview

if len(sys.argv) > 1:
    grass = Image.open('src/main/resources/tiles/ground/grass_1.png').convert('RGBA')
    paving = Image.open(OUT + 'paving.png').convert('RGBA')
    # The court (16 x 13 with its fence), then the bottom fence with the gate, on grass
    W, H = 18, 17
    prev = Image.new('RGBA', (W * 32, H * 32))
    for x in range(W):
        for y in range(H):
            prev.alpha_composite(grass, (x * 32, y * 32))
    prev.alpha_composite(court, (2 * 32, 1 * 32))

    def put(name, c, r):
        prev.alpha_composite(made[name], (c * 32, r * 32))
    for c in range(1, 17):
        if c not in (8, 9):
            put('fence_h', c, 0)
        put('fence_h', c, 12)
    for r in range(0, 13):
        put('fence_v', 1, r); put('fence_v', 16, r)
    put('fence_corner_tl', 1, 0); put('fence_corner_tr', 16, 0)
    put('fence_corner_bl', 1, 12); put('fence_corner_br', 16, 12)
    put('hoop_w_pole', 2, 5); put('hoop_w_ring', 3, 5); put('hoop_w_base', 2, 6)
    put('hoop_e_ring', 14, 5); put('hoop_e_pole', 15, 5); put('hoop_e_base', 15, 6)
    for c in range(0, 18):
        prev.alpha_composite(paving, (c * 32, 15 * 32)) if c in (7, 8, 12, 13) else None
    for c in range(0, 18):
        name = {6: 'gate_pillar', 7: 'gate_l', 8: 'gate_r', 9: 'gate_pillar',
                11: 'gate_pillar', 12: 'gate_open_l', 13: 'gate_open_r', 14: 'gate_pillar'}.get(c, 'fence_h')
        put(name, c, 16)
    put('fence_corner_bl', 0, 16); put('fence_v', 0, 15); put('gate_v', 17, 13); put('gate_v_2', 17, 14)
    put('fence_v', 17, 15); put('fence_corner_br', 17, 16)
    prev.resize((prev.width * 2, prev.height * 2), Image.NEAREST).save(sys.argv[1])
    print('Preview:', sys.argv[1])
