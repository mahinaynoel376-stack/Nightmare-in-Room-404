# Test Subject X still sealed in its pod in the Containment Lab, and two frames of it bursting out
# (see SleepingBoss.java). Same 71 x 60 canvas as its own sprite (battle/LabExperiment.png), so the pod
# and the creature are drawn the same size in the game:
#   LabExperiment_pod.png          the pod: green liquid, the creature curled up small inside, bubbles
#   LabExperiment_pod_cracked.png  bursting, part 1: it presses against the glass, mouth open; cracks
#   LabExperiment_pod_split.png    bursting, part 2: the glass shatters and it comes out, full size
# The animation ends on LabExperiment.png itself: the creature as it always looks.
# Colours from the lab's own pod tile (tiles/lab/containment_pod.png).
# Usage: python tools/art/make_specimen_pod.py   (from the project folder)
from PIL import Image, ImageDraw

BATTLE = 'src/main/resources/battle/'
W, H = 71, 60

METAL_DARK, METAL, METAL_LIGHT = (18, 40, 38, 255), (40, 78, 74, 255), (70, 118, 110, 255)
LIQUID = (58, 190, 148, 150)
GLASS_EDGE = (34, 120, 96, 220)
SHINE = (170, 246, 214, 170)
BUBBLE = (205, 255, 236, 220)
CRACK = (232, 255, 246, 255)
SHARD, SHARD_EDGE = (150, 236, 204, 200), (40, 120, 96, 230)

GX0, GX1, GY0, GY1 = 18, 52, 9, 49      # The glass


def creature(name, scale, tint):
    """The creature's own sprite, scaled, and (inside the liquid) tinted green."""
    im = Image.open(BATTLE + name + '.png').convert('RGBA')
    im = im.resize((max(1, int(im.width * scale)), max(1, int(im.height * scale))), Image.NEAREST)
    if tint:
        p = im.load()
        for y in range(im.height):
            for x in range(im.width):
                r, g, b, a = p[x, y]
                if a:
                    p[x, y] = (int(r * 0.45), int(g * 0.75 + 40), int(b * 0.6 + 30), int(a * 0.8))
    return im


def frame(d, with_glass):
    """The pod's cap and base, and its glass if it's still whole."""
    d.rectangle([14, 2, 56, 8], fill=METAL_DARK)              # Cap
    d.rectangle([15, 3, 55, 7], fill=METAL)
    d.line([(16, 4), (54, 4)], fill=METAL_LIGHT)
    d.rectangle([12, 50, 58, 59], fill=METAL_DARK)            # Base
    d.rectangle([13, 51, 57, 58], fill=METAL)
    d.line([(14, 52), (56, 52)], fill=METAL_LIGHT)
    for x in (20, 30, 40, 50):                                # Rivets
        d.point((x, 55), fill=METAL_LIGHT)
    if with_glass:
        d.rectangle([GX0 - 1, GY0, GX1 + 1, GY1], outline=GLASS_EDGE)


def pod(inside, cracked):
    im = Image.new('RGBA', (W, H))
    liquid = Image.new('RGBA', (W, H))
    ImageDraw.Draw(liquid).rectangle([GX0, GY0, GX1, GY1], fill=LIQUID)
    im.alpha_composite(liquid)
    im.alpha_composite(inside, ((W - inside.width) // 2, GY0 + (GY1 - GY0 - inside.height) // 2 + 2))
    d = ImageDraw.Draw(im)
    d.line([(22, GY0 + 3), (22, GY1 - 4)], fill=SHINE, width=2)     # The shine on the glass
    for x, y in ((28, 14), (44, 20), (33, 40), (47, 33), (25, 30)):  # Bubbles
        d.ellipse([x, y, x + 2, y + 2], outline=BUBBLE)
    frame(d, True)
    if cracked:
        for line in ([(35, 20), (30, 14), (27, 9)], [(35, 20), (42, 15), (46, 11)], [(35, 20), (33, 29), (36, 36)],
                     [(35, 20), (44, 25), (51, 27)], [(35, 20), (25, 24), (19, 23)]):
            d.line(line, fill=CRACK, width=1)
        for x in (24, 31, 45):                                # Liquid leaking out at the bottom
            d.line([(x, GY1 + 1), (x, GY1 + 3)], fill=LIQUID[:3] + (255,))
    return im


def shattered():
    im = Image.new('RGBA', (W, H))
    d = ImageDraw.Draw(im)
    frame(d, False)
    for tri in ([(18, 9), (24, 9), (18, 20)], [(52, 9), (46, 9), (52, 17)],      # Jagged glass left in the frame
                [(18, 49), (18, 38), (25, 49)], [(52, 49), (52, 40), (45, 49)]):
        d.polygon(tri, fill=SHARD, outline=SHARD_EDGE)
    im.alpha_composite(creature('LabExperiment', 1.0, False), (0, 1))            # Out, full size
    d = ImageDraw.Draw(im)
    for tri in ([(4, 14), (9, 12), (7, 18)], [(64, 10), (69, 14), (63, 16)],     # Shards flying
                [(9, 36), (14, 34), (12, 40)], [(60, 32), (66, 35), (61, 38)]):
        d.polygon(tri, fill=SHARD, outline=SHARD_EDGE)
    d.ellipse([6, 55, 64, 59], fill=LIQUID)                                       # A puddle round the base
    return im


pod(creature('LabExperiment', 0.62, True), False).save(BATTLE + 'LabExperiment_pod.png')
pod(creature('LabExperiment_attack', 0.72, True), True).save(BATTLE + 'LabExperiment_pod_cracked.png')
shattered().save(BATTLE + 'LabExperiment_pod_split.png')
print('Saved LabExperiment_pod.png, LabExperiment_pod_cracked.png and LabExperiment_pod_split.png to', BATTLE)
