# Cuts the lab tiles out of the lab sheet, recolours the few that still have the library's warm
# background, and draws the new pieces (lab doors, broken pod, name plaques, LABORATORY sign, hazard decal).
# Usage: python make_lab_tiles.py <lab_sheet.png> <library_sheet.png> <out dir> <preview.png>
import sys
from collections import deque
from PIL import Image

lab_path, lib_path, out, preview = sys.argv[1:5]
lab = Image.open(lab_path).convert('RGBA')
lib = Image.open(lib_path).convert('RGBA')
T = (0, 0, 0, 0)


def tile(sheet, r, c):
    return sheet.crop((c * 32, r * 32, c * 32 + 32, r * 32 + 32))


# ---- 1. Colour map library -> lab, from tiles that are the same drawing in both sheets ----
colour_map = {}
for r, c in [(0, 0), (0, 1), (0, 2), (0, 3)]:          # floor x2, brick wall, wall base
    a, b = tile(lib, r, c).load(), tile(lab, r, c).load()
    for y in range(32):
        for x in range(32):
            colour_map[a[x, y][:3]] = b[x, y][:3]


def recolour_background(im):
    """Flood fill from the tile's edges through library floor/wall colours, swapping each for the
    lab colour. Pixels of the object itself are never reached, so they keep their colours."""
    im = im.copy()
    p = im.load()
    seen = set()
    queue = deque((x, y) for x in range(32) for y in (0, 31)) + deque((x, y) for y in range(32) for x in (0, 31))
    while queue:
        x, y = queue.popleft()
        if (x, y) in seen or not (0 <= x < 32 and 0 <= y < 32):
            continue
        seen.add((x, y))
        col = p[x, y]
        if col[3] == 0 or col[:3] not in colour_map:
            continue
        p[x, y] = colour_map[col[:3]] + (col[3],)
        queue.extend([(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)])
    return im


def cut_background_out(im, background):
    """Like recolour_background, but makes the background transparent instead (for decals)."""
    im = im.copy()
    p = im.load()
    seen = set()
    queue = deque((x, y) for x in range(32) for y in (0, 31)) + deque((x, y) for y in range(32) for x in (0, 31))
    while queue:
        x, y = queue.popleft()
        if (x, y) in seen or not (0 <= x < 32 and 0 <= y < 32):
            continue
        seen.add((x, y))
        if p[x, y][:3] not in background:
            continue
        p[x, y] = T
        queue.extend([(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)])
    return im


# ---- 2. Tiles cut from the lab sheet: name -> (row, col) ----
CUTS = [
    ('lab_floor_1', 0, 0), ('lab_floor_2', 0, 1), ('lab_wall', 0, 2), ('lab_wall_base', 0, 3), ('lab_window', 0, 4),
    ('reagent_shelf', 2, 0), ('containment_pod', 2, 1), ('slime', 2, 2), ('broken_glass', 2, 3), ('hazard_triangle', 2, 4),
    ('specimen_shelf_1', 2, 5), ('specimen_shelf_2', 2, 6), ('specimen_shelf_3', 2, 7), ('specimen_shelf_broken', 3, 0),
    ('bench_microscope', 3, 1), ('bench_burner', 3, 2), ('bench_sink', 3, 3), ('skeleton', 3, 4),
    ('stool', 3, 5), ('stool_fallen', 3, 6), ('periodic_table', 3, 7),
    ('hazard_sign', 4, 0), ('vent_eyes', 4, 1), ('broken_light', 4, 2), ('toxic_puddle', 4, 3),
    ('footprints', 4, 4), ('dangling_wires', 4, 5), ('cracked_monitor', 4, 6), ('cracked_wall', 4, 7),
]
made = {}
for name, r, c in CUTS:
    made[name] = tile(lab, r, c)

# The 4 objects that still have the library's warm floor/brick drawn in: recolour their background
for name, r, c in [('lab_door_bloody', 0, 5), ('lab_table', 0, 6), ('lab_boxes', 0, 7), ('lab_lamp', 1, 6)]:
    made[name] = recolour_background(tile(lab, r, c))

# The hazard sign without its brick background, so it can go on the hallway's (blue-grey) walls
brick_colours = {px[:3] for px in tile(lab, 0, 2).get_flattened_data()} | {px[:3] for px in tile(lab, 0, 3).get_flattened_data()}
made['hazard_decal'] = cut_background_out(tile(lab, 4, 0), brick_colours)

# ---- Palette ----
OUTLINE, STEEL_DARK, STEEL, STEEL_LIGHT, STEEL_SHADOW = (18, 26, 28), (44, 54, 56), (108, 120, 122), (150, 162, 164), (78, 88, 90)
GLASS, GLASS_FRAME, LIQUID, LIQUID_LIGHT, GLINT = (26, 79, 70), (60, 70, 72), (63, 209, 160), (143, 255, 208), (208, 255, 240)
EMPTY_TANK = (10, 26, 24)
HAZARD_YELLOW, HAZARD_BLACK = (214, 170, 40), (24, 24, 24)
PLAQUE_FILL, MINT, CREAM = (34, 46, 48), (150, 225, 195), (225, 232, 220)
BLOOD, BLOOD_DARK = (122, 16, 32), (88, 10, 22)

FONT = {  # 5 x 7 pixel letters
    'A': [".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    'B': ["####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."],
    'C': [".###.", "#...#", "#....", "#....", "#....", "#...#", ".###."],
    'E': ["#####", "#....", "#....", "####.", "#....", "#....", "#####"],
    'G': [".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".###."],
    'H': ["#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    'I': [".###.", "..#..", "..#..", "..#..", "..#..", "..#..", ".###."],
    'L': ["#....", "#....", "#....", "#....", "#....", "#....", "#####"],
    'M': ["#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#"],
    'N': ["#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#"],
    'O': [".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    'R': ["####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"],
    'S': [".####", "#....", "#....", ".###.", "....#", "....#", "####."],
    'T': ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."],
    'Y': ["#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#.."],
}


def rect(p, x0, y0, x1, y1, col):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            p[x, y] = col + (255,)


def text(p, word, x, y, col):
    for i, ch in enumerate(word):
        for dy, line in enumerate(FONT[ch]):
            for dx, bit in enumerate(line):
                if bit == '#':
                    p[x + i * 6 + dx, y + dy] = col + (255,)


def hazard_stripes(p, x0, y0, x1, y1):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            p[x, y] = (HAZARD_YELLOW if ((x + y) // 3) % 2 == 0 else HAZARD_BLACK) + (255,)


def split(im, prefix):
    for i in range(im.width // 32):
        made[f'{prefix}_{i + 1}'] = im.crop((i * 32, 0, i * 32 + 32, 32))


# ---- 3. Steel lab door (2 tiles), and the same door sealed with caution tape ----
door = Image.new('RGBA', (64, 32), STEEL_DARK + (255,))
p = door.load()
for x0, x1 in ((2, 30), (33, 61)):
    rect(p, x0, 2, x1, 31, STEEL)
    rect(p, x0, 2, x1, 2, STEEL_LIGHT); rect(p, x0, 2, x0, 31, STEEL_LIGHT)
    rect(p, x1, 2, x1, 31, STEEL_SHADOW)
    rect(p, x0 + 6, 5, x1 - 6, 13, GLASS_FRAME); rect(p, x0 + 7, 6, x1 - 7, 12, GLASS)     # small window
    p[x0 + 8, 7] = LIQUID + (255,); p[x0 + 9, 8] = LIQUID + (255,)                         # glint
    rect(p, x0 + 4, 17, x1 - 4, 18, STEEL_LIGHT); rect(p, x0 + 4, 19, x1 - 4, 19, STEEL_SHADOW)   # push bar
    hazard_stripes(p, x0 + 1, 25, x1 - 1, 29)
rect(p, 31, 1, 32, 31, OUTLINE)
rect(p, 0, 0, 63, 0, OUTLINE)
made['lab_door_l'] = door.crop((0, 0, 32, 32)); made['lab_door_r'] = door.crop((32, 0, 64, 32))

sealed = door.copy()
p = sealed.load()
for x0, x1 in ((2, 30), (33, 61)):                       # the windows are dark: nothing behind them
    rect(p, x0 + 7, 6, x1 - 7, 12, EMPTY_TANK)
for k in range(64):                                     # caution tape in an X across both leaves
    for (ya, yb) in ((4, 26), (26, 4)):
        y = ya + (yb - ya) * k // 63
        for t in range(-1, 2):
            p[k, y + t] = (HAZARD_BLACK if (k // 4) % 3 == 0 and t == 0 else HAZARD_YELLOW) + (255,)
made['lab_door_sealed_l'] = sealed.crop((0, 0, 32, 32)); made['lab_door_sealed_r'] = sealed.crop((32, 0, 64, 32))

# ---- 4. Broken containment pod: the same tank, smashed and empty ----
pod = tile(lab, 2, 1)
broken = pod.copy()
p = broken.load()
liquid_px = [(x, y) for y in range(32) for x in range(32) if pod.getpixel((x, y))[:3] in (LIQUID, LIQUID_LIGHT, GLINT)]
gx0, gx1 = min(x for x, _ in liquid_px), max(x for x, _ in liquid_px)
gy0, gy1 = min(y for _, y in liquid_px), max(y for _, y in liquid_px)
for y in range(gy0, gy1 + 1):
    for x in range(gx0, gx1 + 1):
        top_shard = 1 + (x * 7) % 5                       # jagged glass left along the edges
        side_shard = 1 + (y * 3) % 3
        if y - gy0 < top_shard or x - gx0 < side_shard or gx1 - x < side_shard:
            p[x, y] = LIQUID + (255,)
        elif gy1 - y < 3:
            p[x, y] = (LIQUID_LIGHT if gy1 - y == 2 else LIQUID) + (255,)   # what's left of the liquid
        else:
            p[x, y] = EMPTY_TANK + (255,)
for x, y in ((gx0 + 2, gy0 + 3), (gx0 + 6, gy0 + 2), (gx1 - 3, gy0 + 4), (gx0 + 1, gy0 + 9), (gx1 - 1, gy0 + 12)):
    p[x, y] = GLINT + (255,)
for x in (gx0 + 5, gx0 + 6, gx1 - 4):                     # liquid leaking out through the frame
    for y in range(gy1 + 1, 32):
        p[x, y] = LIQUID + (255,)
made['pod_broken'] = broken

# ---- 5. Name plaques for the hallway, 3 tiles (96 x 32) each ----
for word, hazard in (('CHEMISTRY', False), ('BIOLOGY', False), ('CONTAINMENT', True)):
    plaque = Image.new('RGBA', (96, 32), T)
    p = plaque.load()
    rect(p, 1, 9, 94, 25, OUTLINE); rect(p, 2, 10, 93, 24, STEEL); rect(p, 3, 11, 92, 23, PLAQUE_FILL)
    if hazard:
        hazard_stripes(p, 4, 12, 9, 22); hazard_stripes(p, 86, 12, 91, 22)
    width = len(word) * 6 - 1
    text(p, word, (96 - width) // 2, 14, MINT)
    for x, y in ((4, 11), (91, 11), (4, 23), (91, 23)):
        p[x, y] = STEEL_LIGHT + (255,)
    split(plaque, 'sign_' + word.lower())

# ---- 6. LABORATORY sign, 3 tiles: hanging crooked from one chain (the other has snapped) ----
flat = Image.new('RGBA', (96, 32), T)
p = flat.load()
rect(p, 4, 8, 91, 24, OUTLINE); rect(p, 5, 9, 90, 23, STEEL_LIGHT); rect(p, 6, 10, 89, 22, (30, 40, 42))
text(p, "LABORATORY", (96 - 59) // 2, 13, CREAM)
for x, n in ((22, 3), (51, 2), (70, 4)):
    for k in range(n):
        p[x, 23 + k] = BLOOD + (255,)
    p[x, 23 + n] = BLOOD_DARK + (255,)
sign = Image.new('RGBA', (96, 32), T)
for x in range(96):                                     # shear: each column a little lower than the last
    drop = x // 24
    for y in range(32 - drop):
        sign.putpixel((x, y + drop), flat.getpixel((x, y)))
p = sign.load()
for y in range(0, 8):                                   # the chain that held
    if y % 3 != 2:
        for x in (13, 14, 15):
            p[x, y] = ((105, 105, 105) if (y % 3 == 1 and x == 14) else (160, 160, 160)) + (255,)
for y in range(0, 4):                                   # the snapped one: a short stub
    if y % 3 != 2:
        for x in (80, 81, 82):
            p[x, y] = (160, 160, 160, 255)
split(sign, 'laboratory_sign')

for name, im in made.items():
    im.save(f'{out}/{name}.png')

# ---- Preview: the new and recoloured pieces, 4x ----
labwall, labfloor = tile(lab, 0, 2), tile(lab, 0, 0)
pieces = [('lab_door_l', 'lab_door_r'), ('lab_door_sealed_l', 'lab_door_sealed_r'), ('pod_broken',), ('containment_pod',),
          ('lab_lamp',), ('lab_table',), ('lab_door_bloody',), ('hazard_decal',),
          ('sign_containment_1', 'sign_containment_2', 'sign_containment_3'),
          ('laboratory_sign_1', 'laboratory_sign_2', 'laboratory_sign_3')]
width = sum(len(g) * 32 + 8 for g in pieces)
prev = Image.new('RGBA', (width, 32), (20, 20, 24, 255))
x = 0
for g in pieces:
    for i, name in enumerate(g):
        prev.alpha_composite(labfloor if name == 'lab_table' else labwall, (x + i * 32, 0))
        prev.alpha_composite(made[name], (x + i * 32, 0))
    x += len(g) * 32 + 8
prev.resize((prev.width * 3, prev.height * 3), Image.NEAREST).save(preview)
pink = sum(1 for px in made['specimen_shelf_broken'].get_flattened_data() if px[:3] == (255, 0, 255))
print(len(made), 'tiles written; pure-magenta pixels in the broken shelf:', pink)
