# The Ceiling Blob: the first monster Flor meets, dropping on her from the ceiling outside Room 404
# (see FirstBlob.java). An inky, glossy blob with three pale eyes, dripping. Same 46 x 36 canvas as the
# Crawler's sprite, so it's drawn the same size:
#   ceiling_blob.png          standing: a round dome of goo
#   ceiling_blob_attack.png   attacking: stretched up tall, mouth wide open
#   ceiling_blob_hurt.png     hurt: squashed flat, eyes screwed shut, splattering
# Usage: python tools/art/make_ceiling_blob.py   (from the project folder)
from PIL import Image, ImageDraw

BATTLE = 'src/main/resources/battle/'
W, H = 46, 36

OUTLINE = (12, 8, 16, 255)
SHADES = [(34, 16, 44, 255), (52, 26, 66, 255), (76, 40, 96, 255), (108, 64, 136, 255)]
SHINE = (196, 168, 226, 255)
EYE, PUPIL = (240, 236, 220, 255), (24, 10, 24, 255)
MOUTH, TOOTH = (20, 6, 14, 255), (232, 222, 200, 255)
DRIP = (52, 26, 66, 255)


def blob(cx, cy, rx, ry, bottom):
    """A blob: an ellipse, flattened where it sits, shaded from the top left, outlined"""
    def inside(x, y):
        return ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1 and y <= bottom
    im = Image.new('RGBA', (W, H))
    p = im.load()
    for y in range(H):
        for x in range(W):
            if not inside(x, y):
                continue
            light = -0.6 * (x - cx) / rx - 0.8 * (y - cy) / ry
            p[x, y] = SHADES[0 if light < -0.5 else 1 if light < 0 else 2 if light < 0.45 else 3]
    for y in range(H):
        for x in range(W):
            if inside(x, y) and any(not inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                p[x, y] = OUTLINE
    return im


def shine(d, x, y):
    d.line([(x, y), (x + 3, y - 2)], fill=SHINE)
    d.point((x + 5, y - 3), fill=SHINE)


def drips(d, points):
    """Goo hanging off its sides: a thin strand ending in a round drop"""
    for x, y, length in points:
        d.line([(x, y), (x, y + length - 2)], fill=OUTLINE)
        d.ellipse([x - 1, y + length - 2, x + 1, y + length], fill=DRIP, outline=OUTLINE)


def eye(d, x, y, size):
    d.ellipse([x, y, x + size, y + size], fill=EYE, outline=OUTLINE)
    d.point((x + size // 2, y + size // 2), fill=PUPIL)
    if size >= 5:
        d.rectangle([x + size // 2 - 1, y + size // 2 - 1, x + size // 2, y + size // 2], fill=PUPIL)


def closed_eye(d, x, y, size):
    d.line([(x, y + size // 2), (x + size, y + size // 2)], fill=EYE)
    d.line([(x, y + size // 2 - 1), (x + size // 2, y + size // 2)], fill=EYE)


# Standing
idle = blob(23, 24, 19, 12, 33)
d = ImageDraw.Draw(idle)
shine(d, 12, 18)
eye(d, 18, 18, 6)
eye(d, 27, 20, 4)
eye(d, 13, 23, 3)
d.line([(19, 28), (27, 28)], fill=MOUTH)
drips(d, [(6, 26, 6), (40, 25, 7)])
idle.save(BATTLE + 'ceiling_blob.png')

# Attacking: stretched up, mouth wide
attack = blob(23, 20, 15, 15, 34)
d = ImageDraw.Draw(attack)
shine(d, 14, 11)
eye(d, 17, 9, 6)
eye(d, 26, 11, 4)
eye(d, 13, 15, 3)
d.ellipse([16, 19, 30, 30], fill=MOUTH, outline=OUTLINE)
for tx in range(18, 29, 3):
    d.polygon([(tx, 20), (tx + 2, 20), (tx + 1, 23)], fill=TOOTH)
    d.polygon([(tx, 29), (tx + 2, 29), (tx + 1, 26)], fill=TOOTH)
drips(d, [(9, 24, 6), (37, 22, 7)])
attack.save(BATTLE + 'ceiling_blob_attack.png')

# Hurt: squashed flat, splattering, paler
hurt = blob(23, 28, 21, 8, 34)
flash = Image.new('L', hurt.size, 0)                         # Paler, but not its outline
src, fp = hurt.load(), flash.load()
for y in range(H):
    for x in range(W):
        if src[x, y][3] and src[x, y] != OUTLINE:
            fp[x, y] = 60
hurt.paste(Image.new('RGBA', hurt.size, (255, 255, 255, 255)), (0, 0), flash)
d = ImageDraw.Draw(hurt)
closed_eye(d, 17, 24, 5)
closed_eye(d, 26, 25, 4)
d.ellipse([20, 29, 25, 32], fill=MOUTH)
for x, y in ((3, 18), (8, 12), (40, 14), (35, 9), (23, 10)):    # Splashes flying off
    d.ellipse([x, y, x + 2, y + 2], fill=SHADES[2], outline=OUTLINE)
hurt.save(BATTLE + 'ceiling_blob_hurt.png')
print('Saved ceiling_blob.png, ceiling_blob_attack.png and ceiling_blob_hurt.png to', BATTLE)
