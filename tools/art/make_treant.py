# The Hollow Oak: the quadrangle's tree (the 2 x 2 tree tiles in tiles/props), come alive.
# Glowing eyes and a jagged mouth on its trunk, two branch arms, and roots for legs.
# Three poses, like every battle sprite: treant.png (standing), treant_attack.png (arms up, mouth
# wide) and treant_hurt.png (eyes screwed shut, leaves shaken loose).
# The tree sits in the same place in every pose, 18 px above the bottom: the roots fill the rest,
# so in the game it can rise out of the ground on them (see TreeMonster).
# Usage: python tools/art/make_treant.py   (from the project folder)
from PIL import Image, ImageDraw

PROPS = 'src/main/resources/tiles/props/'
OUT = 'src/main/resources/battle/'
W, H = 80, 88
OX, OY = 8, 6                      # Where the 64 x 64 tree goes on the 80 x 88 canvas

OUTLINE = (0, 0, 0, 255)
BARK_DARK = (60, 14, 0, 255)
BARK = (128, 69, 13, 255)
BARK_LIGHT = (185, 105, 44, 255)
LEAF = (0, 143, 68, 255)
LEAF_LIGHT = (110, 192, 68, 255)
EYE_GLOW = (255, 70, 40, 255)
EYE_HOT = (255, 214, 120, 255)
HALO = (255, 60, 40, 110)
TOOTH = (232, 222, 190, 255)

tree = Image.new('RGBA', (64, 64))
for i in range(4):
    tree.paste(Image.open(f'{PROPS}tree_{i}.png').convert('RGBA'), ((i % 2) * 32, (i // 2) * 32))


def limb(d, points, width):
    """A root or a branch: black outline, dark bark, then a light streak along it."""
    d.line(points, fill=OUTLINE, width=width + 2, joint='curve')
    d.line(points, fill=BARK, width=width, joint='curve')
    d.line(points, fill=BARK_LIGHT, width=max(1, width - 2), joint='curve')


def claw(d, x, y, spread, up):
    """Three twig fingers at the end of an arm, spreading out and up (up=-1) or down (up=1)."""
    for dx in (-spread, 0, spread):
        d.line([(x, y), (x + dx, y + 5 * up)], fill=OUTLINE, width=3)
        d.line([(x, y), (x + dx, y + 5 * up)], fill=BARK_DARK, width=1)


def leaves(d, x, y):
    d.rectangle([x, y, x + 2, y + 1], fill=LEAF)
    d.point((x + 1, y), fill=LEAF_LIGHT)


def sprite(pose):
    im = Image.new('RGBA', (W, H))
    d = ImageDraw.Draw(im)
    base = OY + 61                                        # The bottom of the trunk
    # Roots for legs, splaying out from under the trunk to the ground
    for start, bend, end in (((OX + 26, base), (OX + 14, base + 8), (OX + 4, H - 2)),
                             ((OX + 29, base), (OX + 24, base + 10), (OX + 20, H - 1)),
                             ((OX + 35, base), (OX + 40, base + 10), (OX + 44, H - 1)),
                             ((OX + 38, base), (OX + 50, base + 8), (OX + 60, H - 2))):
        limb(d, [start, bend, end], 4)
    # Arms: branches from the sides of the trunk
    if pose == 'attack':
        arms = [((OX + 25, OY + 50), (OX + 8, OY + 40), (OX - 4, OY + 18)),
                ((OX + 39, OY + 50), (OX + 56, OY + 40), (OX + 68, OY + 18))]
    elif pose == 'hurt':
        arms = [((OX + 25, OY + 50), (OX + 10, OY + 56), (OX + 0, OY + 66)),
                ((OX + 39, OY + 50), (OX + 54, OY + 56), (OX + 64, OY + 66))]
    else:
        arms = [((OX + 25, OY + 50), (OX + 10, OY + 50), (OX + 0, OY + 38)),
                ((OX + 39, OY + 50), (OX + 54, OY + 50), (OX + 64, OY + 38))]
    for a in arms:
        limb(d, list(a), 3)
        hand = a[2]
        claw(d, hand[0], hand[1], 3, 1 if pose == 'hurt' else -1)
        leaves(d, (a[1][0] + a[2][0]) // 2, (a[1][1] + a[2][1]) // 2 - 2)
    im.alpha_composite(tree, (OX, OY))
    if pose == 'hurt':
        # Flinching: the whole tree a little paler, and leaves shaken loose
        mask = im.split()[3].point(lambda a: 60 if a > 0 else 0)
        im.paste(Image.new('RGBA', im.size, (255, 255, 255, 255)), (0, 0), mask)
    d = ImageDraw.Draw(im)
    if pose == 'hurt':
        for x, y in ((6, 10), (70, 14), (12, 30), (66, 34), (40, 2), (22, 4), (58, 4)):
            leaves(d, x, y)

    # The face, on the trunk (x 25-38 of the tree)
    ex1, ex2, ey = OX + 27, OX + 34, OY + 46
    if pose == 'hurt':                                   # Eyes screwed shut: > <
        for ex, s in ((ex1, 1), (ex2, -1)):
            d.line([(ex, ey - 1), (ex + 1 + s, ey), (ex, ey + 1)], fill=OUTLINE, width=1)
            d.line([(ex + 2, ey - 1), (ex + 1 - s, ey), (ex + 2, ey + 1)], fill=OUTLINE, width=1)
        d.ellipse([OX + 30, OY + 52, OX + 33, OY + 56], fill=OUTLINE)        # A small "o"
    else:
        hot = pose == 'attack'
        for ex in (ex1, ex2):
            d.rectangle([ex - 1, ey - 2, ex + 3, ey + 2], fill=HALO)        # The glow around it
            d.rectangle([ex, ey - 1, ex + 2, ey + 1], fill=OUTLINE)         # The socket
            d.rectangle([ex, ey, ex + 2, ey], fill=EYE_HOT if hot else EYE_GLOW)
            d.point((ex + 1, ey), fill=EYE_HOT)
        # Angry brows, slanting down to the middle
        d.line([(ex1 - 1, ey - 4), (ex1 + 3, ey - 2)], fill=BARK_DARK, width=1)
        d.line([(ex2 - 1, ey - 2), (ex2 + 3, ey - 4)], fill=BARK_DARK, width=1)
        # A jagged mouth (wider when it attacks), with splintered teeth
        top, bottom = OY + 52, OY + (59 if hot else 56)
        d.rectangle([OX + 27, top, OX + 37, bottom], fill=OUTLINE)
        for tx in range(OX + 27, OX + 37, 3):
            d.polygon([(tx, top), (tx + 2, top), (tx + 1, top + 2)], fill=TOOTH)
            d.polygon([(tx + 1, bottom), (tx + 3, bottom), (tx + 2, bottom - 2)], fill=TOOTH)

    return im


for pose, name in (('idle', 'treant'), ('attack', 'treant_attack'), ('hurt', 'treant_hurt')):
    sprite(pose).save(f'{OUT}{name}.png')
print('Saved treant.png, treant_attack.png and treant_hurt.png to', OUT)
