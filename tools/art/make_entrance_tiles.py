# The school's main entrance: a doorway 4 tiles wide (twice a side exit), in the same style as the
# ground floor's exits (see make_ground_tiles.py): stone steps between stone jambs, but wider, with
# heavier jambs and an iron handrail down the middle. It's cut into main_entrance_0.png ... _3.png,
# left to right (TileSet.MAIN_ENTRANCE_FIRST + i). The game puts a support pillar on each side.
# It writes only these four tiles, so it can run after the other scripts without undoing anything.
# Usage: python make_entrance_tiles.py <out dir (tiles/ground)> <preview.png>
import random
import sys
from PIL import Image

out, preview = sys.argv[1:3]
T = (0, 0, 0, 0)
# The exits' palette (from make_ground_tiles.py)
STEPS = [(146, 141, 124), (128, 123, 107), (110, 106, 92)]
STEP_EDGE, STEP_SHADOW = (166, 161, 144), (72, 68, 58)
JAMB, JAMB_LIGHT, JAMB_DARK = (74, 78, 88), (123, 127, 137), (44, 46, 54)
IRON, IRON_HI, IRON_DARK = (44, 45, 54), (104, 106, 122), (16, 16, 21)
WEAR = (156, 151, 134)

W = 128
im = Image.new('RGBA', (W, 32), T)
p = im.load()


def rect(x0, y0, x1, y1, col):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            p[x, y] = col + (255,)


# Three wide steps down to the ground outside, lighter where feet have worn them
rng = random.Random(777)
for i, (y0, y1) in enumerate(((0, 10), (11, 21), (22, 31))):
    rect(5, y0, W - 6, y1, STEPS[i])
    for _ in range(40):                                 # Wear, mostly towards the middle of each side
        x = int(rng.triangular(8, W - 9, rng.choice((W // 4, 3 * W // 4))))
        y = rng.randrange(y0 + 2, y1 - 1)
        p[x, y] = (WEAR if rng.random() < 0.7 else STEPS[min(i + 1, 2)]) + (255,)
    rect(5, y0, W - 6, y0, STEP_EDGE)
    rect(5, y1, W - 6, y1, STEP_SHADOW)
# Heavy stone jambs, lit on the inside edge
rect(0, 0, 4, 31, JAMB); rect(W - 5, 0, W - 1, 31, JAMB)
rect(4, 0, 4, 31, JAMB_LIGHT); rect(W - 5, 0, W - 5, 31, JAMB_LIGHT)
rect(0, 0, 0, 31, JAMB_DARK); rect(W - 1, 0, W - 1, 31, JAMB_DARK)
for y in (7, 18, 29):                                   # Joints in the jamb stones
    rect(1, y, 3, y, JAMB_DARK); rect(W - 4, y, W - 2, y, JAMB_DARK)
# An iron handrail down the middle of the steps, with a post at each step
cx = W // 2
for y in range(0, 32):
    p[cx - 1, y] = IRON_HI + (255,); p[cx, y] = IRON + (255,); p[cx + 1, y] = IRON_DARK + (255,)
    for x in (cx + 2, cx + 3):
        r, g, b, a = p[x, y]
        p[x, y] = (r * 2 // 3, g * 2 // 3, b * 2 // 3, a)    # Its shadow on the steps
for y0 in (1, 12, 23):
    rect(cx - 2, y0, cx + 2, y0 + 3, IRON)
    rect(cx - 2, y0, cx + 2, y0, IRON_HI)
    rect(cx - 2, y0 + 4, cx + 2, y0 + 4, IRON_DARK)

for i in range(4):
    im.crop((i * 32, 0, i * 32 + 32, 32)).save(f'{out}/main_entrance_{i}.png')

# Preview at 4x, on dark ground, between two plain pillars' worth of space
prev = Image.new('RGBA', (W + 64, 32), (30, 32, 40, 255))
prev.alpha_composite(im, (32, 0))
prev.resize((prev.width * 4, prev.height * 4), Image.NEAREST).save(preview)
print('4 tiles written')
