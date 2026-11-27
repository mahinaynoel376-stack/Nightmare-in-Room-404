# The Brute asleep as a boulder on the West Stairs, and two frames of it waking up (see Golem.java).
# Same 52 x 45 canvas and palette as the Brute's own sprite (battle/brute3.png), so the boulder and the
# golem are drawn the same size in the game:
#   brute3_boulder.png         the boulder: a round rock with cracks, the janitor's key ring half buried in it
#   brute3_boulder_cracked.png waking, part 1: the cracks glow orange and two eyes open in the rock
#   brute3_boulder_split.png   waking, part 2: the boulder splits in two and the golem rises out of it
# The animation ends on brute3.png itself: the golem as it always looks.
# Usage: python tools/art/make_golem_boulder.py   (from the project folder)
from PIL import Image, ImageDraw

BATTLE = 'src/main/resources/battle/'
W, H = 52, 45

OUTLINE = (8, 9, 9, 255)
CRACK = (13, 13, 12, 255)
SHADES = [(60, 56, 55, 255), (69, 67, 66, 255), (115, 103, 97, 255), (188, 179, 158, 255), (195, 186, 166, 255)]
SPECK_DARK, SPECK_LIGHT = (98, 84, 74, 255), (210, 202, 182, 255)
GLOW, GLOW_HOT = (240, 120, 40, 255), (255, 214, 120, 255)
GOLD, GOLD_DARK = (232, 184, 70, 255), (140, 100, 30, 255)

CX, CY, RX, RY = 26, 29, 22, 15        # The boulder: an ellipse, flattened where it sits on the step
BOTTOM = 43


def inside(x, y):
    return ((x - CX) / RX) ** 2 + ((y - CY) / RY) ** 2 <= 1 and y <= BOTTOM


def boulder():
    im = Image.new('RGBA', (W, H))
    p = im.load()
    for y in range(H):
        for x in range(W):
            if not inside(x, y):
                continue
            nx, ny = (x - CX) / RX, (y - CY) / RY
            light = -0.55 * nx - 0.85 * ny           # Lit from the top left
            shade = 0 if light < -0.55 else 1 if light < -0.1 else 2 if light < 0.35 else 3 if light < 0.7 else 4
            p[x, y] = SHADES[shade]
    for y in range(H):                              # A dark outline all round
        for x in range(W):
            if inside(x, y) and any(not inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                p[x, y] = OUTLINE
    # Pits and specks in the stone, always in the same places
    for x, y in ((14, 22), (31, 19), (38, 30), (20, 36), (28, 33), (11, 31), (35, 38)):
        p[x, y] = SPECK_DARK
        p[x + 1, y] = SPECK_DARK
    for x, y in ((18, 18), (25, 17), (13, 26), (22, 24)):
        p[x, y] = SPECK_LIGHT
    return im


# The cracks, as lines from point to point (drawn dark while it sleeps, glowing as it wakes)
CRACKS = [[(17, 16), (20, 21), (18, 26), (22, 31)],
          [(33, 15), (31, 21), (35, 26)],
          [(26, 30), (27, 36), (25, 42)],
          [(40, 24), (44, 29)],
          [(9, 30), (13, 34)]]


def cracks(im, colour, core=None):
    d = ImageDraw.Draw(im)
    for line in CRACKS:
        d.line(line, fill=colour, width=1)
        if core:
            for a, b in zip(line, line[1:]):
                d.point(((a[0] + b[0]) // 2, (a[1] + b[1]) // 2), fill=core)


def key_ring(im):
    # The janitor's key ring, half buried at the bottom right
    d = ImageDraw.Draw(im)
    d.ellipse([33, 35, 39, 41], outline=GOLD_DARK)
    d.ellipse([34, 36, 38, 40], outline=GOLD)
    d.line([(38, 38), (43, 36)], fill=GOLD, width=1)     # Two keys sticking out
    d.line([(37, 40), (41, 42)], fill=GOLD, width=1)
    d.point((43, 35), fill=GOLD)
    d.point((41, 43), fill=GOLD)


def eyes(im):
    d = ImageDraw.Draw(im)
    for ex in (19, 30):
        d.rectangle([ex - 1, 24, ex + 3, 26], fill=OUTLINE)    # A slit in the rock...
        d.rectangle([ex, 25, ex + 2, 25], fill=GLOW)           # ...and an eye in it
        d.point((ex + 1, 25), fill=GLOW_HOT)


sleeping = boulder()
cracks(sleeping, CRACK)
key_ring(sleeping)
sleeping.save(BATTLE + 'brute3_boulder.png')

cracked = boulder()
cracks(cracked, GLOW, GLOW_HOT)
key_ring(cracked)
eyes(cracked)
cracked.save(BATTLE + 'brute3_boulder_cracked.png')

# Split: the boulder breaks along a jagged line down the middle, its top breaks off, and the golem rises
# out of the broken shell: its legs still inside, its body above. The shell's halves lean outwards.
SPLIT_TOP = 24                                              # The shell is what's left below this row


def split_col(y):
    return CX + (1, -1, 0, 2, 1, -1, 0)[y % 7]              # A jagged line down the middle


def shell_half(shell, left):
    half = Image.new('RGBA', (W, H))
    src, dst = shell.load(), half.load()
    for y in range(SPLIT_TOP, H):
        for x in range(W):
            if src[x, y][3] and ((x < split_col(y)) == left):
                dst[x, y] = src[x, y]
    for y in range(SPLIT_TOP, H):                           # Outline the broken edges
        for x in range(W):
            if dst[x, y][3] and (y == SPLIT_TOP or x == split_col(y) - 1 or x == split_col(y)
                                 or not dst[x, y - 1][3]):
                dst[x, y] = OUTLINE
    return half


golem = Image.open(BATTLE + 'brute3.png').convert('RGBA')
shell = boulder()
cracks(shell, GLOW, GLOW_HOT)
split = Image.new('RGBA', (W, H))
split.alpha_composite(golem, (0, 3))                        # Still coming up: a little lower than it stands
split.alpha_composite(shell_half(shell, True).crop((5, 0, W, H)), (0, 0))   # Left half, pushed left
split.alpha_composite(shell_half(shell, False), (5, 0))                     # Right half, pushed right
split.save(BATTLE + 'brute3_boulder_split.png')
print('Saved brute3_boulder.png, brute3_boulder_cracked.png and brute3_boulder_split.png to', BATTLE)
