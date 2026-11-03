# Ground floor tiles: cut from the ground sheet, plus a 2-tile support pillar, grass, the two kinds of
# exit (open / boarded up) and the room plaques.
# Usage: python make_ground_tiles.py <ground_sheet.png> <out dir> <preview.png>
import random
import sys
from PIL import Image

sheet_path, out, preview = sys.argv[1:4]
sheet = Image.open(sheet_path).convert('RGBA')
T = (0, 0, 0, 0)


def tile(r, c):
    return sheet.crop((c * 32, r * 32, c * 32 + 32, r * 32 + 32))


made = {}
# ---- 1. Cut from the sheet (the decals in row 1 are the same as the library's, so they're reused) ----
for name, r, c in [('g_floor_1', 0, 0), ('g_floor_2', 0, 1), ('g_wall', 0, 2), ('g_wall_base', 0, 3),
                   ('g_window', 0, 4), ('g_door_bloody', 0, 5), ('g_table', 0, 6), ('g_boxes', 0, 7),
                   ('g_lamp', 1, 6), ('counter_desk', 2, 1), ('cabinet', 2, 2), ('bench', 2, 3),
                   ('fire_alarm', 2, 4)]:
    made[name] = tile(r, c)

# ---- 2. Support pillar, 2 tiles tall, background removed so it can stand on grass ----
# The sheet's pillar is 20 px wide (x 6-25): a cap (rows 0-5), a cracked shaft (rows 6-25), a base (rows 26-31).
src = tile(2, 0)
tall = Image.new('RGBA', (32, 64), T)
for y in range(64):
    if y < 6:
        sy = y                              # cap
    elif y >= 58:
        sy = y - 32                         # base (rows 26-31)
    else:
        sy = 6 + (y - 6) % 20               # shaft, repeated to fill the extra height
    for x in range(6, 26):
        tall.putpixel((x, y), src.getpixel((x, sy)))
made['pillar_top'] = tall.crop((0, 0, 32, 32))
made['pillar_bottom'] = tall.crop((0, 32, 32, 64))

# ---- 3. Grass at night: dark and muted, to sit with the building's palette ----
GRASS, GRASS_DARK, BLADE, BLADE_LIGHT, SHADOW = (34, 52, 38), (26, 40, 30), (50, 76, 52), (66, 96, 64), (20, 30, 23)


def grass(seed, shadow_rows=0):
    rnd = random.Random(seed)
    im = Image.new('RGBA', (32, 32), GRASS + (255,))
    p = im.load()
    for _ in range(40):                                 # darker clumps
        p[rnd.randrange(32), rnd.randrange(32)] = GRASS_DARK + (255,)
    for _ in range(26):                                 # blades: short vertical strokes
        x, y = rnd.randrange(32), rnd.randrange(2, 32)
        col = BLADE_LIGHT if rnd.random() < 0.3 else BLADE
        for k in range(rnd.randint(2, 3)):
            if y - k >= 0:
                p[x, y - k] = col + (255,)
    for y in range(shadow_rows):                        # shadow cast by the ledge above
        for x in range(32):
            if y < shadow_rows - 1 or (x + y) % 2 == 0:
                p[x, y] = SHADOW + (255,)
    return im


made['grass_1'] = grass(11)
made['grass_2'] = grass(29)
made['grass_edge'] = grass(47, shadow_rows=5)

# ---- 4. Exits, 2 tiles (64 x 32): open stone steps, and the same doorway boarded up ----
JAMB, JAMB_LIGHT = (74, 78, 88), (123, 127, 137)
STEPS = [(146, 141, 124), (128, 123, 107), (110, 106, 92)]
STEP_EDGE, STEP_SHADOW = (166, 161, 144), (72, 68, 58)
WOOD, WOOD_LIGHT, WOOD_DARK, NAIL, DARK = (125, 87, 50), (150, 108, 66), (94, 65, 38), (40, 40, 44), (16, 16, 20)


def rect(p, x0, y0, x1, y1, col):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            p[x, y] = col + (255,)


exit_open = Image.new('RGBA', (64, 32), T)
p = exit_open.load()
for i, (y0, y1) in enumerate(((0, 10), (11, 21), (22, 31))):        # three steps down to the grass
    rect(p, 3, y0, 60, y1, STEPS[i])
    rect(p, 3, y0, 60, y0, STEP_EDGE)
    rect(p, 3, y1, 60, y1, STEP_SHADOW)
rect(p, 0, 0, 2, 31, JAMB); rect(p, 61, 0, 63, 31, JAMB)            # stone jambs
rect(p, 2, 0, 2, 31, JAMB_LIGHT); rect(p, 61, 0, 61, 31, JAMB_LIGHT)
made['exit_l'] = exit_open.crop((0, 0, 32, 32)); made['exit_r'] = exit_open.crop((32, 0, 64, 32))

blocked = Image.new('RGBA', (64, 32), DARK + (255,))
p = blocked.load()
rect(p, 0, 0, 2, 31, JAMB); rect(p, 61, 0, 63, 31, JAMB)
rect(p, 2, 0, 2, 31, JAMB_LIGHT); rect(p, 61, 0, 61, 31, JAMB_LIGHT)
for y0 in (3, 12, 21):                                              # planks nailed across
    rect(p, 1, y0, 62, y0 + 6, WOOD)
    rect(p, 1, y0, 62, y0, WOOD_LIGHT)
    rect(p, 1, y0 + 6, 62, y0 + 6, WOOD_DARK)
    for x in (4, 59):
        p[x, y0 + 3] = NAIL + (255,)
for k in range(56):                                                 # one plank nailed diagonally on top
    x, y = 4 + k, 27 - k * 22 // 55
    for t in range(-2, 3):
        if 0 <= y + t < 32:
            p[x, y + t] = (WOOD_LIGHT if t == -2 else WOOD_DARK if t == 2 else WOOD) + (255,)
made['exit_blocked_l'] = blocked.crop((0, 0, 32, 32)); made['exit_blocked_r'] = blocked.crop((32, 0, 64, 32))

# ---- 5. Room plaques, 3 tiles (96 x 32): wood and brass, like the LIBRARY plaque ----
FONT = {
    'A': [".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    'C': [".###.", "#...#", "#....", "#....", "#....", "#...#", ".###."],
    'D': ["####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####."],
    'E': ["#####", "#....", "#....", "####.", "#....", "#....", "#####"],
    'F': ["#####", "#....", "#....", "####.", "#....", "#....", "#...."],
    'I': [".###.", "..#..", "..#..", "..#..", "..#..", "..#..", ".###."],
    'L': ["#....", "#....", "#....", "#....", "#....", "#....", "#####"],
    'N': ["#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#"],
    'O': [".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    'P': ["####.", "#...#", "#...#", "####.", "#....", "#....", "#...."],
    'R': ["####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"],
    'S': [".####", "#....", "#....", ".###.", "....#", "....#", "####."],
    'T': ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."],
    'U': ["#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    'Y': ["#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#.."],
}
OUTLINE, GOLD, GOLD_DARK, PLAQUE_WOOD, TEXT = (23, 17, 12), (201, 162, 60), (140, 105, 38), (47, 30, 17), (215, 180, 85)
for word in ('FACULTY', 'PRINCIPAL', 'RECORDS'):
    plaque = Image.new('RGBA', (96, 32), T)
    p = plaque.load()
    rect(p, 1, 9, 94, 25, OUTLINE); rect(p, 2, 10, 93, 24, GOLD_DARK); rect(p, 3, 11, 92, 23, PLAQUE_WOOD)
    x0 = (96 - (len(word) * 6 - 1)) // 2
    for i, ch in enumerate(word):
        for dy, line in enumerate(FONT[ch]):
            for dx, bit in enumerate(line):
                if bit == '#':
                    p[x0 + i * 6 + dx, 14 + dy] = TEXT + (255,)
    for x, y in ((4, 12), (91, 12), (4, 22), (91, 22)):
        p[x, y] = GOLD + (255,)
    for i in range(3):
        made[f'sign_{word.lower()}_{i + 1}'] = plaque.crop((i * 32, 0, i * 32 + 32, 32))

for name, im in made.items():
    im.save(f'{out}/{name}.png')

# ---- Preview: pillar | open exit | blocked exit | pillar, on a stairwell-floor / grass background ----
prev = Image.new('RGBA', (32 * 12, 64), (20, 20, 24, 255))
for x in range(0, prev.width, 32):
    prev.alpha_composite(made['grass_edge' if x < 0 else 'grass_1'], (x, 32))
    prev.alpha_composite(tile(0, 0), (x, 0))
prev.alpha_composite(tall, (0, 0)); prev.alpha_composite(exit_open, (32, 0)); prev.alpha_composite(tall, (96, 0))
prev.alpha_composite(tall, (128, 0)); prev.alpha_composite(blocked, (160, 0)); prev.alpha_composite(tall, (224, 0))
for i in range(3):
    prev.alpha_composite(made[f'sign_principal_{i + 1}'], (256 + i * 32, 0))
prev.alpha_composite(made['grass_edge'], (256, 32)); prev.alpha_composite(made['grass_2'], (288, 32))
prev.resize((prev.width * 3, prev.height * 3), Image.NEAREST).save(preview)
print(len(made), 'tiles written')
