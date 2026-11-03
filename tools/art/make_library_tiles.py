# Cuts the library tiles out of the sheet and draws the new ones (big door, QUIET sign, LIBRARY plaque).
# Usage: python make_library_tiles.py <sheet.png> <02_wall.png> <out dir> <preview.png>
import sys
from PIL import Image

sheet_path, cool_wall_path, out, preview = sys.argv[1:5]
sheet = Image.open(sheet_path).convert('RGBA')
T = (0, 0, 0, 0)

# ---- 1. Tiles cut straight from the sheet: name -> (row, col) ----
CUTS = [
    ('lib_floor_1', 0, 0), ('lib_floor_2', 0, 1), ('lib_wall', 0, 2), ('lib_wall_base', 0, 3),
    ('lib_window', 0, 4), ('lib_door_bloody', 0, 5), ('lib_table', 0, 6), ('lib_boxes', 0, 7),
    ('blood_splat', 1, 0), ('crack', 1, 1), ('cobweb_left', 1, 2), ('cobweb_right', 1, 3),
    ('handprint', 1, 4), ('glowing_eyes', 1, 5), ('wall_lamp', 1, 6), ('scratches', 1, 7),
    ('bookshelf', 2, 0), ('computer_desk', 2, 1), ('papers', 2, 2), ('front_desk', 2, 3),
    ('lib_chair', 2, 4), ('broken_bench', 2, 5), ('scattered_books', 2, 6), ('open_book', 2, 7),
    ('book_stack', 3, 0), ('blood_writing_1', 3, 7), ('blood_writing_2', 4, 0), ('blood_writing_3', 4, 1),
    ('counter', 4, 2), ('card_catalog', 4, 3), ('blood_drips', 4, 4),
]
made = {}
for name, r, c in CUTS:
    made[name] = sheet.crop((c * 32, r * 32, c * 32 + 32, r * 32 + 32))

# ---- Palette (sampled from the sheet's door, and the building's stone wall strips) ----
OUTLINE, FRAME = (23, 17, 12), (28, 20, 14)
WOOD, WOOD_DARK, WOOD_LIGHT, PANEL = (61, 40, 22), (47, 30, 17), (84, 57, 31), (40, 30, 22)
GOLD, GOLD_DARK = (201, 162, 60), (140, 105, 38)
BLOOD, BLOOD_DARK = (122, 16, 32), (88, 10, 22)
STONE_LIGHT, STONE, STONE_DARK = (151, 151, 151), (119, 119, 119), (83, 83, 83)
BOARD, BEIGE, CREAM = (38, 31, 28), (196, 184, 150), (232, 222, 188)

FONT = {  # 5 x 7 pixel letters
    'Q': [".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#"],
    'U': ["#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    'I': [".###.", "..#..", "..#..", "..#..", "..#..", "..#..", ".###."],
    'E': ["#####", "#....", "#....", "####.", "#....", "#....", "#####"],
    'T': ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."],
    'L': ["#....", "#....", "#....", "#....", "#....", "#....", "#####"],
    'B': ["####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."],
    'R': ["####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"],
    'A': [".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    'Y': ["#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#.."],
}


def rect(p, x0, y0, x1, y1, col):
    """Filled rectangle, both ends included."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            p[x, y] = col + (255,)


def text(p, word, x, y, col):
    for i, ch in enumerate(word):
        for dy, line in enumerate(FONT[ch]):
            for dx, bit in enumerate(line):
                if bit == '#':
                    p[x + i * 6 + dx, y + dy] = col + (255,)


# ---- 2. Big double door, 128 x 64 (4 x 2 tiles) ----
door = Image.new('RGBA', (128, 64), FRAME + (255,))
p = door.load()
rect(p, 0, 0, 127, 3, STONE); rect(p, 0, 0, 3, 63, STONE); rect(p, 124, 0, 127, 63, STONE)   # stone surround
rect(p, 0, 0, 127, 0, STONE_LIGHT); rect(p, 0, 0, 0, 63, STONE_LIGHT)
rect(p, 3, 3, 124, 3, STONE_DARK); rect(p, 3, 3, 3, 63, STONE_DARK); rect(p, 124, 3, 124, 63, STONE_DARK)
for x0, x1 in ((5, 62), (65, 122)):                         # the two leaves
    rect(p, x0, 5, x1, 62, WOOD)
    rect(p, x0, 5, x1, 5, WOOD_LIGHT); rect(p, x0, 5, x0, 62, WOOD_LIGHT)
    rect(p, x0, 62, x1, 62, WOOD_DARK); rect(p, x1, 5, x1, 62, WOOD_DARK)
    for y0, y1 in ((10, 27), (33, 57)):                     # two recessed panels per leaf
        rect(p, x0 + 6, y0, x1 - 6, y1, PANEL)
        rect(p, x0 + 6, y0, x1 - 6, y0, WOOD_DARK); rect(p, x0 + 6, y0, x0 + 6, y1, WOOD_DARK)
        rect(p, x0 + 6, y1, x1 - 6, y1, WOOD_LIGHT); rect(p, x1 - 6, y0, x1 - 6, y1, WOOD_LIGHT)
rect(p, 63, 4, 64, 63, OUTLINE)                             # seam between the leaves
rect(p, 57, 29, 58, 38, GOLD); rect(p, 57, 38, 58, 38, GOLD_DARK)   # handles
rect(p, 69, 29, 70, 38, GOLD); rect(p, 69, 38, 70, 38, GOLD_DARK)
for i, x in enumerate((86, 91, 96)):                        # claw marks in blood on the right leaf
    for k in range(17):
        xx, yy = x + k // 5, 36 + k + (i % 2)
        p[xx, yy] = BLOOD + (255,)
        p[xx + 1, yy] = BLOOD_DARK + (255,)
for k in range(4):                                          # a drip under the marks
    p[97, 53 + k] = BLOOD + (255,)
rect(p, 4, 63, 123, 63, OUTLINE)                            # threshold shadow
for r in range(2):
    for c in range(4):
        made[f'big_door_{r * 4 + c}'] = door.crop((c * 32, r * 32, c * 32 + 32, r * 32 + 32))

# ---- 3. QUIET sign (bloody), 64 x 32, hanging on chains ----
sign = Image.new('RGBA', (64, 32), T)
p = sign.load()
for cx in (13, 50):                                         # chains
    for y in range(0, 11):
        if y % 3 == 2:
            continue
        for x in (cx - 1, cx, cx + 1):
            p[x, y] = ((105, 105, 105) if (y % 3 == 1 and x == cx) else (160, 160, 160)) + (255,)
rect(p, 3, 11, 60, 28, OUTLINE); rect(p, 4, 12, 59, 27, BEIGE); rect(p, 5, 13, 58, 26, BOARD)
text(p, "QUIET", 17, 16, CREAM)
for x in range(4, 61):
    p[x, 29] = (0, 0, 0, 90)                                # soft shadow under the board
rect(p, 5, 13, 9, 14, BLOOD); rect(p, 6, 15, 7, 17, BLOOD)  # smear on the corner
for x, n in ((18, 3), (34, 2), (47, 2)):                    # drips off the bottom edge
    for k in range(n):
        p[x, 27 + k] = BLOOD + (255,)
    p[x, 27 + n] = BLOOD_DARK + (255,)
    p[x + 1, 27 + n] = BLOOD + (255,)
made['quiet_sign_left'] = sign.crop((0, 0, 32, 32))
made['quiet_sign_right'] = sign.crop((32, 0, 64, 32))

# ---- 4. LIBRARY plaque, 64 x 32, mounted on the hallway wall ----
plaque = Image.new('RGBA', (64, 32), T)
p = plaque.load()
rect(p, 1, 9, 62, 25, OUTLINE); rect(p, 2, 10, 61, 24, GOLD_DARK); rect(p, 3, 11, 60, 23, WOOD_DARK)
text(p, "LIBRARY", 11, 14, (215, 180, 85))
for x, y in ((4, 12), (59, 12), (4, 22), (59, 22)):
    p[x, y] = GOLD + (255,)
made['library_plaque_left'] = plaque.crop((0, 0, 32, 32))
made['library_plaque_right'] = plaque.crop((32, 0, 64, 32))

for name, im in made.items():
    im.save(f'{out}/{name}.png')

# Preview: the new pieces on the building's (cool) wall, 4x zoom
wall = Image.open(cool_wall_path).convert('RGBA')
prev = Image.new('RGBA', (320, 64), (20, 20, 24, 255))
for x in range(0, prev.width, 32):
    for y in (0, 32):
        prev.alpha_composite(wall, (x, y))
prev.alpha_composite(door, (0, 0))
prev.alpha_composite(sign, (144, 0))
prev.alpha_composite(plaque, (224, 16))
prev.resize((prev.width * 4, prev.height * 4), Image.NEAREST).save(preview)
print(len(made), 'tiles written')
