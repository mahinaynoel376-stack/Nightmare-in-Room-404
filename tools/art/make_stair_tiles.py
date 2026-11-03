# Draws the 6 new stair tiles (32x32) in the classroom tiles' palette.
# Usage: python make_stair_tiles.py <classroom tiles dir> <out dir> <preview.png>
import sys, random
from PIL import Image, ImageDraw
src, out, preview = sys.argv[1:4]
rnd = random.Random(404)
T = (0, 0, 0, 0)
# Palette sampled from the classroom tiles
FLOOR = {(56,59,72):(88,92,108), (38,40,51):(64,68,82), (50,53,66):(76,80,96)}   # floor -> lighter landing
NOSE, TREAD, TREAD2, SHADOW, RISER = (104,109,128), (70,74,90), (62,66,81), (38,40,51), (23,25,34)
VOID, VOID2 = (8,8,12), (16,17,23)
STONE = [(187,187,187), (151,151,151), (119,119,119), (83,83,83), (51,51,51)]
WOOD = [(150,108,66), (125,87,50), (94,65,38)]
OUTLINE = (34,34,34)

def new(): return Image.new('RGBA', (32, 32), T)

def landing():
    floor = Image.open(src + '/01_floor.png').convert('RGBA')
    im = new(); p = im.load(); f = floor.load()
    for y in range(32):
        for x in range(32):
            c = f[x, y][:3]
            p[x, y] = FLOOR.get(c, (min(c[0]+30,255), min(c[1]+30,255), min(c[2]+34,255))) + (255,)
    return im

def step():
    im = new(); p = im.load()
    for y in range(32):
        k = y % 8
        col = NOSE if k == 0 else TREAD if k < 4 else TREAD2 if k < 5 else SHADOW if k < 7 else RISER
        for x in range(32):
            p[x, y] = col + (255,)
    for _ in range(14):                       # Worn speckles on the treads
        x, y = rnd.randrange(32), rnd.randrange(32)
        if 1 <= y % 8 <= 4: p[x, y] = (TREAD2 if rnd.random() < .5 else (80,84,100)) + (255,)
    return im

def rail():
    strip = Image.open(src + '/07_wall_v_left_1.png').convert('RGBA').crop((0, 0, 17, 32))
    im = new(); im.paste(strip, (8, 0), strip)   # Centre the stone strip in the tile
    return im

def crack(p, x, y, n):
    for _ in range(n):
        if 0 <= x < 32 and 0 <= y < 32: p[x, y] = RISER + (255,)
        x += rnd.choice((-1, 0, 1)); y += 1

def broken_step():
    im = step(); p = im.load()
    crack(p, 9, 0, 20); crack(p, 22, 6, 18)
    # Missing jagged chunk on the right side
    for y in range(32):
        edge = 22 + int(4 * abs(((y * 7) % 11) - 5) / 5) + (y // 10)
        for x in range(edge, 32):
            p[x, y] = (VOID if x > edge else SHADOW) + (255,)
    for _ in range(6):                        # Loose bits in the gap
        x, y = rnd.randrange(25, 31), rnd.randrange(2, 30)
        p[x, y] = STONE[3] + (255,)
    return im

def hole():
    im = new(); p = im.load()
    for y in range(32):
        for x in range(32):
            p[x, y] = (VOID2 if (x * 3 + y * 5) % 13 == 0 else VOID) + (255,)
    # Ragged remains of a step along the top edge
    for x in range(32):
        depth = 3 + (x * 5 % 7) // 2 + (2 if 10 < x < 16 else 0)
        for y in range(depth):
            p[x, y] = (NOSE if y == 0 else TREAD if y < depth - 1 else RISER) + (255,)
    # Debris lying at the bottom of the hole
    d = ImageDraw.Draw(im)
    for (x, y, w, h, c) in [(5,20,5,3,STONE[3]), (17,24,6,4,STONE[4]), (24,15,3,3,STONE[3]), (11,27,3,2,WOOD[2])]:
        d.rectangle([x, y, x + w - 1, y + h - 1], fill=c + (255,))
        d.line([x, y, x + w - 1, y], fill=STONE[2] + (255,))
    return im

def rubble():
    im = new(); d = ImageDraw.Draw(im)
    d.ellipse([3, 22, 29, 31], fill=(18, 20, 28, 170))                 # Shadow under the pile
    chunks = [(6,15,15,25,1), (14,10,25,22,2), (19,18,28,27,3), (4,21,11,28,2), (12,20,19,28,1), (22,12,27,17,1)]
    for (x0, y0, x1, y1, s) in chunks:
        d.polygon([(x0+1,y0), (x1,y0+1), (x1,y1-1), (x1-2,y1), (x0,y1), (x0,y0+2)], fill=STONE[s] + (255,), outline=OUTLINE + (255,))
        d.line([x0+2, y0+1, x1-1, y0+1], fill=STONE[0] + (255,))         # Lit top edge
    d.line([8, 12, 17, 9], fill=WOOD[0] + (255,), width=2)              # Splintered plank
    d.line([8, 13, 17, 10], fill=WOOD[2] + (255,))
    for (x, y) in [(2,26), (29,24), (27,29), (5,30), (24,9)]:
        d.point((x, y), fill=STONE[2] + (255,))                          # Dust
    return im

tiles = {'stair_landing': landing(), 'stair_step': step(), 'stair_rail': rail(),
         'stair_step_broken': broken_step(), 'stair_hole': hole(), 'rubble': rubble()}
for name, im in tiles.items():
    im.save(f'{out}/{name}.png')
# Preview sheet, 6x zoom
sheet = Image.new('RGBA', (len(tiles) * 200, 200), (255, 0, 255, 255))
for i, im in enumerate(tiles.values()):
    big = im.resize((192, 192), Image.NEAREST); sheet.paste(big, (i * 200 + 4, 4), big)
sheet.save(preview)
print('ok', list(tiles))
