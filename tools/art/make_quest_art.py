# Makes Subject Zero, the quadrangle's final boss: the subject that broke out of pod 2 in the labs.
# It's built from Test Subject X's sprites (it's from the same experiment), turned ashen, with roots,
# a crown of dead leaves and more eyes:
#   battle/subject_zero.png, _attack.png, _hurt.png   awake (drawn in the world, standing, striking, hit)
#   battle/subject_zero_sleeping.png                  asleep by the south gate: a heap of dead leaves
#                                                     and roots that's easy to walk past (64 x 34)
# Usage: python make_quest_art.py <src/main/resources> <preview.png>
import random
import sys

from PIL import Image, ImageDraw

res, preview = sys.argv[1:3]
OUTLINE = (0, 0, 0, 255)
BARK, BARK_LIGHT = (92, 52, 20, 255), (138, 82, 34, 255)
LEAF = [(96, 86, 66, 255), (62, 56, 46, 255), (132, 118, 86, 255)]


def load(path):
    return Image.open(f'{res}/{path}').convert('RGBA')


def recolour(im, table):
    out = im.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            p = px[x, y]
            if p[3] and p[:3] in table:
                px[x, y] = table[p[:3]] + (p[3],)
    return out


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


ASHEN = {
    (233, 208, 178): (168, 158, 172), (233, 208, 179): (168, 158, 172), (232, 208, 178): (168, 158, 172),
    (155, 138, 112): (110, 100, 122), (93, 61, 54): (70, 52, 40), (151, 115, 93): (112, 86, 58),
    (67, 28, 29): (36, 8, 22), (217, 134, 150): (178, 52, 84), (225, 157, 156): (196, 84, 104),
    (182, 93, 89): (140, 40, 70), (181, 85, 86): (140, 40, 70), (255, 246, 241): (230, 230, 206),
}


def subject_zero(sprite, seed):
    body = recolour(load(f'battle/{sprite}.png'), ASHEN)
    im = Image.new('RGBA', (96, 84))
    roots = Image.new('RGBA', im.size)   # Roots spreading over the ground under it
    d = ImageDraw.Draw(roots)
    for pts in ([(48, 70), (30, 76), (10, 80), (2, 78)], [(48, 70), (40, 80), (28, 83)],
                [(48, 70), (66, 76), (86, 80), (94, 77)], [(50, 70), (58, 81), (70, 83)]):
        d.line(pts, fill=BARK, width=3)
        d.line(pts, fill=BARK_LIGHT, width=1)
    im.alpha_composite(outline(roots))
    ox, oy = 12, 14
    im.alpha_composite(body, (ox, oy))
    # Where its flesh is, to put the crown and the extra eyes on it
    skin = [(x, y) for y in range(body.height) for x in range(body.width)
            if body.getpixel((x, y))[:3] == (168, 158, 172) and body.getpixel((x, y))[3]]
    top = min(y for _, y in skin)
    xs = [x for x, y in skin if y <= top + 3]
    left, right = min(xs), max(xs)
    crown = Image.new('RGBA', im.size)   # A crown of dead leaves and twigs growing out of its head
    d = ImageDraw.Draw(crown)
    rng = random.Random(seed)
    for _ in range(40):
        cx = ox + rng.randint(left - 4, right + 4)
        cy = oy + top - rng.randint(1, 12)
        d.ellipse((cx - 2, cy - 2, cx + 2, cy + 1), fill=rng.choice(LEAF))
    for tx in (left - 2, (left + right) // 2, right + 2):
        d.line((ox + tx, oy + top, ox + tx + rng.randint(-4, 4), oy + top - 13), fill=BARK, width=2)
    im.alpha_composite(outline(crown))
    d = ImageDraw.Draw(im)               # Four more eyes, yellow, along the top of its body
    for i, ex in enumerate((left + 2, left + (right - left) // 3, left + 2 * (right - left) // 3, right - 3)):
        ey = oy + top + 3 + (i % 2) * 2
        d.rectangle((ox + ex - 1, ey - 1, ox + ex + 2, ey + 2), fill=OUTLINE)
        d.rectangle((ox + ex, ey, ox + ex + 1, ey + 1), fill=(250, 214, 80, 255))
    return im


subject_zero('LabExperiment', 3).save(f'{res}/battle/subject_zero.png')
subject_zero('LabExperiment_attack', 3).save(f'{res}/battle/subject_zero_attack.png')
subject_zero('LabExperiment_hurt', 3).save(f'{res}/battle/subject_zero_hurt.png')

# Asleep: a low heap of dead leaves with a couple of roots and one shut eye, drawn 1:1 in the world
sleep = Image.new('RGBA', (64, 34))
rng = random.Random(11)
d = ImageDraw.Draw(sleep)
for pts in ([(14, 29), (6, 31), (1, 30)], [(50, 29), (58, 32), (63, 30)], [(30, 30), (26, 33)]):
    d.line(pts, fill=BARK, width=2)
heap = Image.new('RGBA', sleep.size)
hp = heap.load()
for y in range(34):
    for x in range(64):
        dx, dy = (x - 32) / 27.0, (y - 30) / 17.0
        if dy <= 0 and dx * dx + dy * dy <= 1 and y < 31:
            n = rng.random()
            hp[x, y] = LEAF[0] if n < 0.5 else LEAF[1] if n < 0.8 else LEAF[2]
sleep.alpha_composite(heap)
d = ImageDraw.Draw(sleep)
for _ in range(14):   # Clumps of leaves, so it reads as a heap and not a blob
    cx, cy = rng.randint(10, 54), rng.randint(17, 28)
    d.ellipse((cx - 3, cy - 2, cx + 3, cy + 1), fill=rng.choice(LEAF))
d.arc((34, 19, 42, 24), 0, 180, fill=(26, 18, 14, 255), width=1)   # The shut eye
d.point((37, 25), fill=(26, 18, 14, 255))
sleep = outline(sleep)
sleep.save(f'{res}/battle/subject_zero_sleeping.png')

# Preview, 4x, on night grass
parts = [load('battle/subject_zero.png'), load('battle/subject_zero_attack.png'),
         load('battle/subject_zero_hurt.png'), sleep]
S = 4
sheet = Image.new('RGBA', (sum(p.width * S + 12 for p in parts), max(p.height for p in parts) * S), (38, 56, 40, 255))
x = 0
for p in parts:
    sheet.alpha_composite(p.resize((p.width * S, p.height * S), Image.NEAREST), (x, sheet.height - p.height * S))
    x += p.width * S + 12
sheet.save(preview)
