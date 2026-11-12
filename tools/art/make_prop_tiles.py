# Cuts the hand-drawn props in docs/images/props into game tiles (32 x 32) in tiles/props:
#   window.png          64 x 32  -> window                            (1 tile: the whole window, see below)
#   window_curtain.png  64 x 32  -> window_curtain
#   tree.png            64 x 64  -> tree_0 ... tree_3                 (2 x 2, reading order)
#   bush.png            64 x 32  -> bush_0, bush_1                    (2 x 1)
#   rocks.png           64 x 32  -> rocks_0, rocks_1                  (2 x 1)
#   trash_can.png       32 x 64  -> trash_can_0 (lid), trash_can_1    (1 x 2)
#   bus.png             64 x 32  -> bus_0 ... bus_14                  (5 x 3, see below)
# The bus is drawn small (the size of a bush), so it's scaled up 3 times, pixel for pixel, to be
# bus-sized next to the player, and placed so its ladder fills exactly one tile column (column 2):
# that column is the only way onto the roof. Column 4 is the lower front of the bus (the hood).
# The windows are drawn a little wider than a tile (38 and 42 px), so to keep each one whole in a
# single tile, a few columns are taken out where nothing changes: from the plain window, 6 columns
# that are exactly like the one beside them (3 in each pane); from the curtained one, 10 columns
# of plain curtain (5 in each curtain), leaving its valance, frame, glass and vines as drawn.
# Usage: python make_prop_tiles.py <docs/images/props> <out dir (tiles/props)> <preview.png>
import sys
from PIL import Image

src, out, preview = sys.argv[1:4]


def cut(im, name, cols, rows):
    tiles = []
    for r in range(rows):
        for c in range(cols):
            tiles.append(im.crop((c * 32, r * 32, c * 32 + 32, r * 32 + 32)))
    for i, t in enumerate(tiles):
        t.save(f'{out}/{name}_{i}.png')
    return tiles


def load(name):
    return Image.open(f'{src}/{name}.png').convert('RGBA')


def drop_columns(im, cols):
    keep = [x for x in range(im.width) if x not in cols]
    narrow = Image.new('RGBA', (len(keep), im.height), (0, 0, 0, 0))
    for i, x in enumerate(keep):
        narrow.paste(im.crop((x, 0, x + 1, im.height)), (i, 0))
    return narrow


def one_tile(im):
    # Crop to the drawing and centre it in a 32 x 32 tile
    xs = [x for x in range(im.width) if any(im.getpixel((x, y))[3] for y in range(im.height))]
    im = im.crop((xs[0], 0, xs[-1] + 1, im.height))
    assert im.width <= 32 and im.height <= 32, im.size
    tile = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    tile.alpha_composite(im, ((32 - im.width) // 2, 0))
    return tile


made = {}
WINDOW_DROP = {19, 28, 30, 36, 38, 42}                       # Each exactly like its left neighbour
CURTAIN_DROP = {15, 16, 17, 19, 20, 45, 48, 49, 50, 51}      # Plain curtain, inside the valance ends
for name, drop in (('window', WINDOW_DROP), ('window_curtain', CURTAIN_DROP)):
    tile = one_tile(drop_columns(load(name), drop))
    tile.save(f'{out}/{name}.png')
    made[name] = tile
for name, cols, rows in (('tree', 2, 2), ('bush', 2, 1), ('rocks', 2, 1), ('trash_can', 1, 2)):
    im = load(name)
    cut(im, name, cols, rows)
    made[name] = im

# The bus: crop to the drawing (x 4-56, y 2-28), scale x3 (159 x 81), stand it on the bottom of a
# 5 x 3 tile area. The ladder is drawn at x 26-33, so after scaling it's at x 66-89; 1 px from the
# left puts it at x 67-90, inside column 2 (64-95).
BUS_SCALE, BUS_LEFT = 3, 1
small = load('bus').crop((4, 2, 57, 29))
big = small.resize((small.width * BUS_SCALE, small.height * BUS_SCALE), Image.NEAREST)
bus = Image.new('RGBA', (5 * 32, 3 * 32), (0, 0, 0, 0))
bus.alpha_composite(big, (BUS_LEFT, 3 * 32 - big.height))
cut(bus, 'bus', 5, 3)
made['bus'] = bus

# Preview at 3x on grass-green, each prop with its tile grid
sheet_w = sum(im.width * 3 + 12 for im in made.values())
sheet = Image.new('RGBA', (sheet_w, 96 * 3), (70, 100, 60, 255))
x = 0
for im in made.values():
    big_im = im.resize((im.width * 3, im.height * 3), Image.NEAREST)
    sheet.alpha_composite(big_im, (x, 0))
    for gx in range(0, big_im.width + 1, 96):
        for y in range(big_im.height):
            sheet.putpixel((min(x + gx, sheet_w - 1), y), (255, 0, 255, 255))
    for gy in range(0, big_im.height + 1, 96):
        for xx in range(big_im.width):
            sheet.putpixel((x + xx, min(gy, sheet.height - 1)), (255, 0, 255, 255))
    x += big_im.width + 12
sheet.save(preview)
print('props cut:', ', '.join(made))
