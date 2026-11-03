# Quadrangle tiles: cut from the quad sheet, plus fences, the wrecked school bus, book barricades,
# basketball hoops, a flagpole, lampposts and parking lines. Also remakes the ground floor's grass from
# the sheet's grass, so the building and the quadrangle share the same ground.
# Usage: python make_quad_tiles.py <quad_sheet.png> <quad out dir> <ground out dir> <preview.png>
import random
import sys
from PIL import Image

sheet_path, out, ground_out, preview = sys.argv[1:5]
sheet = Image.open(sheet_path).convert('RGBA')
T = (0, 0, 0, 0)


def tile(r, c):
    return sheet.crop((c * 32, r * 32, c * 32 + 32, r * 32 + 32))


def rect(p, x0, y0, x1, y1, col):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            p[x, y] = col if len(col) == 4 else col + (255,)


made = {}
# ---- 1. Cut from the sheet ----
for name, r, c in [('q_asphalt_1', 0, 0), ('q_asphalt_2', 0, 1), ('q_wall', 0, 2), ('q_wall_base', 0, 3),
                   ('q_window', 0, 4), ('q_door_bloody', 0, 5), ('q_table', 0, 6), ('q_boxes', 0, 7),
                   ('q_lamp', 1, 6), ('paving', 2, 1), ('tree', 2, 2), ('gate_h', 2, 3), ('park_bench', 2, 4)]:
    made[name] = tile(r, c)

# ---- 2. Fences: remove the padlock (the bars repeat every 8 px, so copy from 8 px to the left) ----
fence = tile(2, 3)
p = fence.load()
for y in range(13, 20):
    for x in range(13, 21):
        p[x, y] = p[x - 8, y]
made['fence_h'] = fence
made['fence_v'] = fence.rotate(90)                 # the same fence turned for the side edges
made['gate_v'] = tile(2, 3).rotate(90)

# ---- 3. Grass for everywhere outdoors, made from the sheet's grass ----
grass = tile(2, 0)
ground = {'grass_1': grass,
          'grass_2': grass.transpose(Image.Transpose.ROTATE_180)}   # same grass, turned: breaks up repeats
edge = grass.copy()
p = edge.load()
for y in range(6):                                  # the shadow of the low wall above
    for x in range(32):
        r, g, b, a = p[x, y]
        k = 0.55 if y < 4 else 0.75
        p[x, y] = (int(r * k), int(g * k), int(b * k), a)
ground['grass_edge'] = edge

# ---- 4. The wrecked school bus: 7 x 3 tiles (224 x 96). Rows 0-1: the roof (you can stand on it);
#         row 2: its side, with the ladder at the back (left) ----
YEL, YEL_L, YEL_D = (190, 146, 42), (218, 172, 62), (142, 104, 28)
BLK, GLASS, GLASS_D, GLASS_L = (24, 24, 26), (40, 52, 64), (22, 28, 36), (110, 130, 150)
RUST, RUST_D, TIRE, HUB, STEEL, STEEL_D = (120, 70, 40), (82, 48, 28), (18, 18, 20), (96, 96, 102), (150, 150, 158), (90, 90, 98)
BLOOD = (122, 16, 32)
rnd = random.Random(7)
bus = Image.new('RGBA', (224, 96), T)
p = bus.load()
rect(p, 0, 0, 223, 63, YEL)                          # roof
rect(p, 0, 0, 223, 0, BLK); rect(p, 0, 0, 0, 63, BLK); rect(p, 223, 0, 223, 63, BLK)
rect(p, 1, 1, 222, 2, YEL_L)                         # light catching the top edge
for x in range(32, 192, 32):                         # roof panel seams
    rect(p, x, 3, x, 62, YEL_D)
for hx in (52, 132):                                 # two roof hatches
    rect(p, hx, 18, hx + 22, 40, BLK); rect(p, hx + 1, 19, hx + 21, 39, STEEL_D); rect(p, hx + 3, 21, hx + 19, 37, STEEL)
rect(p, 188, 3, 195, 62, GLASS_D); rect(p, 189, 4, 194, 61, GLASS)   # windscreen at the front
rect(p, 196, 3, 222, 62, YEL_D)                       # bonnet
for i in range(70):                                   # rust and grime
    x, y = rnd.randrange(4, 186), rnd.randrange(4, 60)
    p[x, y] = (RUST if rnd.random() < 0.6 else RUST_D) + (255,)
for y in range(26, 44):                               # a hole punched through the roof
    w = 6 - abs(y - 35) // 2
    for x in range(98 - w, 98 + w + 1):
        p[x, y] = BLK + (255,)
rect(p, 0, 63, 223, 63, BLK)                          # roof edge
# The side (y 64-95): body, window band, black stripes, wheels; open ground under the body
rect(p, 0, 64, 223, 88, YEL)
rect(p, 0, 64, 223, 65, BLK); rect(p, 0, 79, 223, 80, BLK); rect(p, 0, 88, 223, 88, YEL_D)
for wx in range(24, 186, 22):                         # passenger windows, some smashed
    rect(p, wx, 67, wx + 16, 77, GLASS_D)
    if (wx // 22) % 3 == 1:
        for k in range(6):
            p[wx + 3 + k, 68 + k] = GLASS_L + (255,); p[wx + 12 - k, 70 + k // 2] = GLASS_L + (255,)
    else:
        rect(p, wx + 1, 68, wx + 15, 76, GLASS); p[wx + 3, 69] = GLASS_L + (255,)
rect(p, 196, 67, 214, 77, GLASS)                      # driver's window
rect(p, 217, 70, 221, 74, (230, 220, 160))            # a headlight still on
for cx in (52, 168):                                  # wheels
    for y in range(84, 96):
        for x in range(cx - 10, cx + 11):
            if (x - cx) ** 2 / 110 + (y - 90) ** 2 / 40 <= 1:
                p[x, y] = TIRE + (255,)
    rect(p, cx - 3, 88, cx + 3, 92, HUB)
for x in range(100, 124):                             # blood smeared down the side
    if x % 5 != 0:
        p[x, 81 + (x % 3)] = BLOOD + (255,)
for x in (4, 19):                                     # the ladder up to the roof, at the back
    rect(p, x, 50, x + 2, 95, STEEL_D); rect(p, x, 50, x, 95, STEEL)
for y in range(54, 96, 6):
    rect(p, 6, y, 18, y + 1, STEEL)
for r in range(3):
    for c in range(7):
        made[f'bus_{r * 7 + c}'] = bus.crop((c * 32, r * 32, c * 32 + 32, r * 32 + 32))

# ---- 5. Book barricades: piles of library books (see-through background) ----
BOOKS = [(150, 50, 50), (60, 110, 60), (60, 80, 140), (180, 120, 50), (110, 70, 120), (200, 190, 150)]
EDGE, PAGES, SHADOW = (30, 24, 20), (220, 210, 185), (10, 12, 10, 150)


def book_pile(seed):
    rnd = random.Random(seed)
    im = Image.new('RGBA', (32, 32), T)
    p = im.load()
    for x in range(2, 30):                            # shadow on the ground
        for y in range(28, 32):
            if (x - 16) ** 2 / 196 + (y - 30) ** 2 / 4 <= 1:
                p[x, y] = SHADOW
    for x0, x1 in ((1, 15), (16, 30)):                # two tall piles side by side, almost tile-high
        y = 30
        top = rnd.randint(2, 6)
        while y - 3 >= top:
            h = rnd.randint(3, 4)
            dx = rnd.randint(-1, 1)
            col = rnd.choice(BOOKS)
            rect(p, x0 + dx, y - h, x1 + dx, y - 1, col)
            rect(p, x0 + dx, y - h, x1 + dx, y - h, EDGE)
            rect(p, x1 + dx - 1, y - h + 1, x1 + dx - 1, y - 1, PAGES)
            y -= h
    return im


made['book_barricade_1'] = book_pile(3)
made['book_barricade_2'] = book_pile(8)

# ---- 6. Basketball hoops (backboard and ring, seen from above), one for each end of the court ----
hoop = Image.new('RGBA', (32, 32), T)
p = hoop.load()
rect(p, 15, 0, 16, 4, (60, 60, 66))                   # the post behind the board
rect(p, 5, 4, 26, 10, (40, 40, 44)); rect(p, 6, 5, 25, 9, (205, 205, 200))   # backboard
rect(p, 12, 6, 19, 8, (170, 40, 40)); rect(p, 13, 7, 18, 7, (205, 205, 200))
for y in range(11, 21):                               # the ring
    for x in range(9, 23):
        d = (x - 15.5) ** 2 / 42 + (y - 15.5) ** 2 / 25
        if 0.65 <= d <= 1:
            p[x, y] = (210, 100, 40, 255)
for y in range(13, 19):                               # what's left of the net
    if y % 2:
        p[12 + (y % 4), y] = (190, 190, 185, 255); p[19 - (y % 4), y] = (190, 190, 185, 255)
made['hoop_n'] = hoop
made['hoop_s'] = hoop.transpose(Image.Transpose.FLIP_TOP_BOTTOM)

# ---- 7. Flagpole and lamppost, 2 tiles tall each (top + bottom), see-through background ----
flag = Image.new('RGBA', (32, 64), T)
p = flag.load()
rect(p, 15, 6, 16, 58, (150, 150, 160)); rect(p, 15, 6, 15, 58, (190, 190, 200))
rect(p, 14, 3, 17, 5, (201, 162, 60))
for y in range(7, 20):                                # a torn, dark red flag
    for x in range(17, 30 - (y % 4) * 2 + (2 if y < 12 else 0)):
        p[x, y] = ((120, 30, 36) if (x + y) % 7 else (90, 20, 26)) + (255,)
rect(p, 10, 57, 21, 63, (74, 78, 88)); rect(p, 10, 57, 21, 57, (123, 127, 137))
made['flagpole_top'] = flag.crop((0, 0, 32, 32)); made['flagpole_bottom'] = flag.crop((0, 32, 32, 64))

lamp = Image.new('RGBA', (32, 64), T)
p = lamp.load()
for y in range(0, 22):                                # faint glow round the lamp
    for x in range(4, 28):
        d = (x - 15.5) ** 2 + (y - 8) ** 2
        if d < 110:
            p[x, y] = (240, 220, 150, 45 if d > 50 else 70)
rect(p, 15, 10, 16, 60, (40, 42, 48)); rect(p, 16, 10, 16, 60, (70, 72, 80))
rect(p, 10, 4, 21, 10, (40, 42, 48)); rect(p, 12, 6, 19, 9, (240, 220, 150))
rect(p, 12, 58, 19, 63, (40, 42, 48))
made['lamppost_top'] = lamp.crop((0, 0, 32, 32)); made['lamppost_bottom'] = lamp.crop((0, 32, 32, 64))

# ---- 8. A painted parking line (worn), see-through ----
line = Image.new('RGBA', (32, 32), T)
p = line.load()
rnd = random.Random(5)
for y in range(32):
    for x in (15, 16):
        if rnd.random() > 0.12:
            p[x, y] = (190, 190, 178, 255)
made['paint_line'] = line

for name, im in made.items():
    im.save(f'{out}/{name}.png')
for name, im in ground.items():
    im.save(f'{ground_out}/{name}.png')

# ---- Preview: bus on asphalt, fences, barricades, hoops, flagpole and lamppost on grass ----
asphalt = tile(0, 0)
prev = Image.new('RGBA', (32 * 18, 96), (20, 20, 24, 255))
for x in range(0, prev.width, 32):
    for y in (0, 32, 64):
        prev.alpha_composite(asphalt if x < 256 else ground['grass_1'], (x, y))
prev.alpha_composite(bus, (16, 0))
items = [('fence_h', 0), ('gate_h', 0), ('fence_v', 1), ('gate_v', 1), ('book_barricade_1', 2), ('book_barricade_2', 2),
         ('hoop_n', 0), ('hoop_s', 1)]
for i, (name, row) in enumerate(items):
    prev.alpha_composite(made[name], (256 + i * 32, row * 32 if row < 2 else 64))
prev.alpha_composite(flag, (512, 0)); prev.alpha_composite(lamp, (544, 0))
prev.resize((prev.width * 2, prev.height * 2), Image.NEAREST).save(preview)
print(len(made), 'quad tiles and', len(ground), 'grass tiles written')
