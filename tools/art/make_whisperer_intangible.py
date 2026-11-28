# The Whisperer while she's intangible (see Whisperer.java): her own sprite (battle/whisperer.png),
# see-through and pale blue, with every other row fainter so she seems to shimmer.
#   whisperer_intangible.png   the same 48 x 36 canvas as whisperer.png
# Usage: python tools/art/make_whisperer_intangible.py   (from the project folder)
from PIL import Image

BATTLE = 'src/main/resources/battle/'
TINT = (186, 214, 255)            # Pale blue

ghost = Image.open(BATTLE + 'whisperer.png').convert('RGBA')
out = Image.new('RGBA', ghost.size)
src, dst = ghost.load(), out.load()
for y in range(ghost.height):
    for x in range(ghost.width):
        r, g, b, a = src[x, y]
        if a == 0:
            continue
        light = (r + g + b) / (3 * 255)                     # Keep the shading, in the tint's colour
        colour = tuple(int(TINT[i] * (0.45 + 0.55 * light)) for i in range(3))
        alpha = int(a * (0.68 if y % 2 == 0 else 0.45))     # See-through, fainter every other row
        dst[x, y] = colour + (alpha,)

out.save(BATTLE + 'whisperer_intangible.png')
print('Saved whisperer_intangible.png to', BATTLE)
