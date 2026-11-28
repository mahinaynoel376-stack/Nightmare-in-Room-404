# Kai waking up at his desk in Room 404, for the opening scene (see ui/WakeUp.java). Every frame is
# made from his own walking sprites (characters/player/*.png): his head, his jacket and his trousers,
# moved, leaned and turned, so he looks the same as when he gets up and walks.
#
# Each frame is 64 x 64: the left half covers his chair's tile, the right half his desk's tile (the
# chair faces right, at the desk), and the top half is the row above them. The game draws the frame
# over the chair and desk tiles, so they aren't in it.
#   desk_sleep_1.png   slumped over the desk, face down on his arms
#   desk_sleep_2.png   the same, breathing in (head and back a pixel higher)
#   desk_stir.png      lifting his head, still half on the desk
#   desk_drowsy.png    sitting up, eyes shut
#   desk_awake.png     sitting up, eyes open
#   desk_look_back.png sitting up, looking back over his shoulder at the empty room
#
# Usage: python tools/art/make_desk_wakeup.py [preview.png]   (from the project folder)
import sys
from PIL import Image, ImageDraw

PLAYER = 'src/main/resources/characters/player/'
CLASSROOM = 'src/main/resources/tiles/classroom/'

right = Image.open(PLAYER + 'right_idle.png').convert('RGBA')
left = Image.open(PLAYER + 'left_idle.png').convert('RGBA')
back = Image.open(PLAYER + 'up_idle.png').convert('RGBA')

# His colours (taken from the sprites)
SLEEVE = (122, 126, 136, 255)
SLEEVE_DARK = (88, 92, 104, 255)
OUTLINE = (28, 29, 37, 255)
SKIN = (246, 210, 192, 255)
SKIN_DARK = (224, 168, 152, 255)
LID = (30, 28, 36, 255)
TROUSER_LIGHT = (60, 60, 74, 255)
TROUSER = (34, 34, 44, 255)
TROUSER_EDGE = (19, 19, 26, 255)
TROUSER_DARK = (7, 7, 10, 255)
SHOE = (30, 30, 38, 255)
SHOE_EDGE = (9, 9, 13, 255)
SOLE = (110, 112, 124, 255)

# The parts of a 32 x 64 walking sprite
def head(sprite):          # Hair and face (rows 0-23)
    return sprite.crop((0, 0, 32, 24))

def body(sprite):          # Jacket, arm and hand (rows 24-45)
    return sprite.crop((0, 24, 32, 46))

# His eyes shut: the open eye on the side-facing sprite (rows 17-19, columns 20-23) becomes skin with
# a dark line for the closed lid
def eyes_shut(h):
    h = h.copy()
    px = h.load()
    for x in range(20, 24):
        for y in (17, 18):
            px[x, y] = SKIN
        px[x, 19] = LID
    px[20, 19] = SKIN
    return h

# His legs, sitting: thigh along the seat, shin down in front of it, shoe on the floor.
# (hipX, hipY) is where the thigh starts.
def seated_legs(frame, hip_x, hip_y):
    d = ImageDraw.Draw(frame)
    knee = hip_x + 13
    d.rectangle((hip_x, hip_y, knee, hip_y + 5), fill=TROUSER)            # Thigh
    d.line((hip_x, hip_y, knee, hip_y), fill=TROUSER_DARK)
    d.line((hip_x, hip_y + 1, knee - 1, hip_y + 1), fill=TROUSER_LIGHT)
    d.line((hip_x, hip_y + 5, knee - 4, hip_y + 5), fill=TROUSER_DARK)
    d.rectangle((knee - 5, hip_y + 2, knee, 59), fill=TROUSER)            # Shin
    d.line((knee - 5, hip_y + 5, knee - 5, 59), fill=TROUSER_EDGE)
    d.line((knee - 4, hip_y + 2, knee - 4, 59), fill=TROUSER_LIGHT)
    d.line((knee, hip_y, knee, 59), fill=TROUSER_DARK)
    d.rectangle((knee - 5, 59, knee + 3, 61), fill=SHOE)                  # Shoe
    d.line((knee - 5, 59, knee - 5, 61), fill=SHOE_EDGE)
    d.line((knee + 3, 59, knee + 3, 61), fill=SHOE_EDGE)
    d.line((knee - 5, 62, knee + 3, 62), fill=SOLE)

# Sitting up straight: the top of the walking sprite lowered onto the seat, legs bent.
# h = the head to use (eyes open, shut, or looking back)
def sitting(h):
    frame = Image.new('RGBA', (64, 64))
    frame.alpha_composite(body(right), (7, 29))
    seated_legs(frame, 19, 46)              # His near leg is in front of his jacket
    frame.alpha_composite(h, (7, 5))
    return frame

# His arms folded on the desk top (the desk's top face is x 38-58, y 39-51 in the frame)
def folded_arms(frame, x, y):
    d = ImageDraw.Draw(frame)
    d.rounded_rectangle((x, y, x + 20, y + 8), radius=3, fill=SLEEVE, outline=OUTLINE)
    d.line((x + 2, y + 7, x + 18, y + 7), fill=SLEEVE_DARK)               # Shadow under the sleeves
    d.line((x + 9, y + 2, x + 9, y + 6), fill=SLEEVE_DARK)                # Where the arms cross
    d.rectangle((x + 19, y + 2, x + 22, y + 5), fill=SKIN, outline=OUTLINE)   # A hand peeking out
    d.line((x + 20, y + 5, x + 21, y + 5), fill=SKIN_DARK)

# His back leaning forward: the jacket squashed and slanted, so the shoulders move `lean` pixels
# forward from the hips (at the frame's (hipX, 50)), and the top of the back is at `top`
def leaning_body(lean, top):
    src = body(right)                       # 32 x 22
    rows = 50 - top
    out = Image.new('RGBA', (64, 64))
    for oy in range(rows):
        sy = int((oy + 0.5) * src.height / rows)
        shift = 7 + round(lean * (1 - oy / rows))        # More forward the higher up
        for sx in range(32):
            p = src.getpixel((sx, sy))
            if p[3] and 0 <= sx + shift < 64:
                out.putpixel((sx + shift, top + oy), p)
    return out

# His back hunched over the desk: the jacket from his hips up to his shoulders, curving forward.
# lift = 1 raises it a pixel (breathing in).
def hunched_back(frame, lift):
    d = ImageDraw.Draw(frame)
    up = -lift
    back_line = [(15, 51), (15, 45 + up), (18, 40 + up), (23, 36 + up), (30, 34 + up), (35, 37 + up),
                 (33, 43), (26, 48), (22, 52)]
    d.polygon(back_line, fill=SLEEVE, outline=OUTLINE)
    d.line([(16, 46 + up), (19, 41 + up), (24, 37 + up)], fill=(205, 210, 216, 255))   # Light on his back
    d.line([(20, 51), (26, 47), (32, 43)], fill=SLEEVE_DARK)                            # His side, in shadow
    d.line([(29, 35 + up), (33, 37 + up)], fill=(52, 68, 112, 255))                     # His collar

# Asleep: face down on his folded arms, so all that shows of his head is the back of it, his hair.
# breath = 1 lifts his head and back a pixel.
def asleep(breath):
    frame = Image.new('RGBA', (64, 64))
    hunched_back(frame, breath)
    seated_legs(frame, 17, 46)
    hair = back.crop((4, 0, 31, 22))                    # The back of his head (all hair)
    frame.alpha_composite(hair, (27, 19 - breath))
    folded_arms(frame, 33, 39)                          # His face is buried in his arms
    return frame

# Stirring: his back half up, his head lifted off his arms, drooping forward, eyes still shut
def stirring():
    frame = Image.new('RGBA', (64, 64))
    frame.alpha_composite(leaning_body(6, 32))
    seated_legs(frame, 18, 46)
    folded_arms(frame, 33, 39)
    frame.alpha_composite(eyes_shut(head(right)), (12, 11))
    return frame

frames = {
    'desk_sleep_1': asleep(0),
    'desk_sleep_2': asleep(1),
    'desk_stir': stirring(),
    'desk_drowsy': sitting(eyes_shut(head(right))),
    'desk_awake': sitting(head(right)),
    'desk_look_back': sitting(head(left)),
}
for name, frame in frames.items():
    frame.save(PLAYER + name + '.png')
print('Saved', len(frames), 'frames to', PLAYER)

# A zoomed-in preview: each frame over the chair and desk tiles, on the classroom floor
if len(sys.argv) > 1:
    floor = Image.open(CLASSROOM + '01_floor.png').convert('RGBA')
    chair = Image.open(CLASSROOM + '15_chair.png').convert('RGBA')
    desk = Image.open(CLASSROOM + '16_desk.png').convert('RGBA')
    sheet = Image.new('RGBA', (68 * len(frames), 64), (20, 20, 24, 255))
    for i, frame in enumerate(frames.values()):
        x = 68 * i
        for tx in (0, 32):
            for ty in (0, 32):
                sheet.alpha_composite(floor, (x + tx, ty))
        sheet.alpha_composite(chair, (x, 32))
        sheet.alpha_composite(desk, (x + 32, 32))
        sheet.alpha_composite(frame, (x, 0))
    sheet.resize((sheet.width * 5, sheet.height * 5), Image.NEAREST).save(sys.argv[1])
    print('Preview:', sys.argv[1])
