# Subject Zero rising out of its heap of leaves when the quadrangle's last tree falls (see
# entity/SubjectZero.java). Made from its own sprites: the heap (subject_zero_sleeping.png) and the
# awake Subject Zero (subject_zero.png), so the frames lead from one to the other.
#   battle/subject_zero_rise_1.png   the heap swells up, an eye opens in it, roots crawl out
#   battle/subject_zero_rise_2.png   its crown and eyes push up out of the leaves
#   battle/subject_zero_rise_3.png   nearly up, the heap falling away, leaves flying
# Each is the awake sprite's 96 x 84 canvas, standing on the same line, so the game draws them the
# same way and size as the awake one.
# Usage: python tools/art/make_zero_rising.py [preview.png]   (from the project folder)
import random
import sys

from PIL import Image, ImageDraw

BATTLE = 'src/main/resources/battle/'
OUTLINE = (0, 0, 0, 255)
BARK, BARK_LIGHT = (92, 52, 20, 255), (138, 82, 34, 255)
LEAF = [(96, 86, 66, 255), (62, 56, 46, 255), (132, 118, 86, 255)]
EYE = (250, 214, 80, 255)

awake = Image.open(BATTLE + 'subject_zero.png').convert('RGBA')      # 96 x 84
heap = Image.open(BATTLE + 'subject_zero_sleeping.png').convert('RGBA')   # 64 x 34
W, H = awake.size
GROUND = 78                     # The line it stands on (the middle of its roots)


def outline(im):
    # A 1-pixel black line round everything drawn (the art's style), on the transparent side
    out = im.copy()
    src, px = im.load(), out.load()
    for y in range(im.height):
        for x in range(im.width):
            if src[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < im.width and 0 <= ny < im.height and src[nx, ny][3] and src[nx, ny][:3] != (0, 0, 0):
                    px[x, y] = OUTLINE
                    break
    return out


# The heap, stretched to w x h, standing on the ground line in the middle of the canvas
def heap_at(frame, w, h):
    frame.alpha_composite(heap.resize((w, h), Image.NEAREST), ((W - w) // 2, GROUND + 4 - h))


# Roots crawling out over the ground, `reach` pixels long on each side
def roots(frame, reach):
    layer = Image.new('RGBA', frame.size)
    d = ImageDraw.Draw(layer)
    for side in (-1, 1):
        pts = [(48, GROUND), (48 + side * reach // 2, GROUND + 3), (48 + side * reach, GROUND + 1)]
        d.line(pts, fill=BARK, width=3)
        d.line(pts, fill=BARK_LIGHT, width=1)
    frame.alpha_composite(outline(layer))


# The awake Subject Zero coming up out of the ground: `sunk` pixels of it are still under the ground line
def emerging(frame, sunk):
    body = Image.new('RGBA', frame.size)
    body.alpha_composite(awake, (0, sunk))
    cut = body.load()
    for y in range(GROUND - 2, H):              # Nothing below the ground line shows
        for x in range(W):
            cut[x, y] = (0, 0, 0, 0)
    frame.alpha_composite(body)


# Dead leaves flying up and off it
def flying_leaves(frame, count, seed, top, bottom):
    d = ImageDraw.Draw(frame)
    rng = random.Random(seed)
    for _ in range(count):
        cx, cy = rng.randint(8, W - 8), rng.randint(top, bottom)
        d.ellipse((cx - 2, cy - 1, cx + 2, cy + 1), fill=rng.choice(LEAF), outline=OUTLINE)


# 1: the heap swells, and one yellow eye opens in it
rise1 = Image.new('RGBA', (W, H))
roots(rise1, 30)
heap_at(rise1, 58, 34)
d = ImageDraw.Draw(rise1)
d.rectangle((50, 64, 54, 67), fill=OUTLINE)
d.rectangle((51, 65, 53, 66), fill=EYE)

# 2: its crown and eyes push up out of the leaves
rise2 = Image.new('RGBA', (W, H))
roots(rise2, 40)
emerging(rise2, 27)
heap_at(rise2, 70, 26)
flying_leaves(rise2, 6, 5, 26, 50)

# 3: nearly up, the heap falling away around its roots, leaves everywhere
rise3 = Image.new('RGBA', (W, H))
emerging(rise3, 10)
heap_at(rise3, 84, 14)
flying_leaves(rise3, 14, 9, 4, 60)

for name, im in (('rise_1', rise1), ('rise_2', rise2), ('rise_3', rise3)):
    im.save(BATTLE + 'subject_zero_' + name + '.png')
print('Saved subject_zero_rise_1..3.png to', BATTLE)

if len(sys.argv) > 1:          # Preview: asleep, the three frames, awake, on night grass, 4x
    sheet = Image.new('RGBA', (W * 5 + 40, H), (36, 58, 34, 255))
    sheet.alpha_composite(heap, ((W - heap.width) // 2, GROUND + 4 - heap.height))
    for i, im in enumerate((rise1, rise2, rise3, awake)):
        sheet.alpha_composite(im, ((i + 1) * (W + 10), 0))
    sheet.resize((sheet.width * 4, sheet.height * 4), Image.NEAREST).save(sys.argv[1])
    print('Preview:', sys.argv[1])
