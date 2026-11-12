# Lays out the quadrangle (50 x 36) as two character maps (ground and things) and prints them as Java.
# Usage: python make_quad_maps.py > quad_maps.txt
W, H = 50, 36
ground = [['.'] * W for _ in range(H)]     # '.' grass  ',' paving  ':' asphalt  'w' wall  '_' wall base  ' ' void
things = [['.'] * W for _ in range(H)]


def g(c0, r0, c1, r1, ch):
    for r in range(r0, r1 + 1):
        for c in range(c0, c1 + 1):
            ground[r][c] = ch


def t(c, r, ch):
    assert things[r][c] == '.', f'({c},{r}) already has {things[r][c]!r}'
    things[r][c] = ch


def prop(c, r, w, h, ch, on='.'):
    # A thing w x h tiles big: its letter on its top-left tile, '+' on the rest (filled in by the
    # game, see Quadrangle.PROPS). Every tile must be free, and the ground under it one of `on`.
    for rr in range(r, r + h):
        for cc in range(c, c + w):
            assert things[rr][cc] == '.', f'({cc},{rr}) already has {things[rr][cc]!r}'
            assert ground[rr][cc] in on, f'({cc},{rr}) is {ground[rr][cc]!r}, not one of {on!r}'
            things[rr][cc] = '+'
    things[r][c] = ch


# ---- The building's south face along the top ----
g(0, 0, W - 1, 0, ' ')                 # top edge (a stone strip is added in code)
g(0, 1, W - 1, 2, 'w')
g(0, 3, W - 1, 3, '_')
g(23, 3, 26, 3, ',')                   # the main entrance, in the middle (placed in code)
for c in (5, 43):
    g(c, 3, c + 1, 3, ',')             # the side exits at the two ends, boarded up (placed in code)
for c in (2, 10, 16, 33, 39, 47):
    t(c, 2, 'W')                       # windows
for c in (7, 20, 29, 42):
    t(c, 1, 'L')                       # lamps

# ---- Paths ----
g(1, 4, W - 2, 4, ',')                 # walkway along the building
g(23, 5, 26, 12, ',')                  # from the main entrance, south to the plaza
g(5, 5, 6, 16, ',')                    # from the west side exit, south to the west gate path
g(43, 5, 44, 9, ',')                   # from the east side exit, south to the east gate path
g(36, 10, 37, 13, ',')                 # east gate path -> the plaza
g(30, 13, 37, 14, ',')                 # ...west to the plaza
g(19, 13, 30, 21, ',')                 # the central plaza
g(1, 17, 18, 18, ',')                  # plaza -> west gate
g(38, 10, W - 2, 11, ',')              # main path -> east gate
g(24, 22, 25, 34, ',')                 # plaza -> south gate

# ---- Car park (stronghold 1: the bus is placed in code) ----
g(3, 24, 20, 33, ':')
for c in (6, 9, 12, 15, 18):
    for r in (31, 32, 33):
        t(c, r, '/')                   # parking bays
for c, r in ((17, 26), (18, 26), (4, 26)):
    t(c, r, 'x')                       # crates: cover around the bus
t(4, 32, 't'); t(19, 32, 't')          # picnic tables

# ---- Basketball court (stronghold 3: its fence and hoops are placed in code) ----
g(32, 22, 45, 32, ':')

# ---- Book barricade (stronghold 2), facing south, open towards the building ----
for c, r, ch in ((25, 8, 'K'), (33, 8, 'k'), (25, 9, 'D'), (33, 9, 'K'),
                 (25, 10, 'K'), (26, 10, 'k'), (28, 10, 'K'), (29, 10, 'D'), (30, 10, 'k'), (32, 10, 'K'), (33, 10, 'k')):
    t(c, r, ch)                        # gaps at columns 27 and 31: the only ways through the front
t(27, 8, 'c'); t(31, 9, 'b')           # a fallen chair, scattered books

# ---- Perimeter fence, with three locked gates (where attackers will break in) ----
for r in range(4, H):
    t(0, r, '|'); t(W - 1, r, '|')
for c in range(1, W - 1):
    t(c, H - 1, '-')
for r in (17, 18):
    things[r][0] = 'g'                 # west gate
for r in (10, 11):
    things[r][W - 1] = 'g'             # east gate
for c in (24, 25):
    things[H - 1][c] = 'G'             # south gate

# ---- Trees (2 x 2): cover, spread round the edges and the lawns ----
for c, r in ((9, 6), (17, 6), (2, 11), (30, 5), (37, 6), (40, 15), (46, 15),
             (1, 21), (1, 27), (28, 26), (47, 23), (21, 32), (28, 32)):
    prop(c, r, 2, 2, 'T')

# ---- Bushes (2 x 1): either side of each doorway, and of the path south ----
for c, r in ((2, 5), (7, 5), (21, 5), (27, 5), (41, 5), (45, 5), (21, 23), (26, 23)):
    prop(c, r, 2, 1, 'h')

# ---- Rocks (2 x 1): low cover out on the lawns ----
for c, r in ((12, 13), (34, 17), (8, 21), (44, 18)):
    prop(c, r, 2, 1, 'o')

# ---- Trash cans (1 x 2): by the plaza's benches, the car park and the court ----
for c, r in ((19, 15), (30, 15), (22, 27), (35, 19)):
    prop(c, r, 1, 2, 'n', on='.,')

# ---- The plaza: flagpole in the middle, benches, lampposts ----
t(24, 16, 'A'); t(24, 17, 'a')
for c, r in ((20, 15), (29, 15), (20, 19), (29, 19)):
    t(c, r, 'B')
for c, r in ((18, 12), (34, 11), (18, 21), (30, 22), (42, 7), (45, 7), (4, 7), (7, 7), (21, 25)):
    t(c, r, 'Q'); t(c, r + 1, 'q')     # lamppost: top, then bottom

# ---- The creature's trail: from the east side exit to the south gate ----
for c, r, ch in ((44, 6, 'f'), (43, 9, 'f'), (40, 11, 'f'), (37, 12, 'f'), (34, 14, 's'), (30, 14, 'f'), (27, 16, 'f'),
                 (25, 19, 'f'), (24, 22, 's'), (25, 25, 'f'), (24, 28, 'f'), (25, 31, 'f'), (24, 34, 's')):
    t(c, r, ch)

# ---- Blood and cracks ----
for c, r in ((10, 31), (40, 27), (29, 9), (14, 19)):
    t(c, r, '*')
for c, r in ((22, 15), (44, 24), (6, 18), (16, 30)):
    t(c, r, '~')


def emit(name, grid, comment):
    print(f'    // {comment}')
    print(f'    private static final String[] {name} = {{')
    for row in grid:
        print(f'        "{"".join(row)}",')
    print('    };')
    print()


emit('GROUND', ground, "What's underfoot: '.' grass  ',' paving  ':' asphalt  'w' wall  '_' wall base  ' ' void")
emit('THINGS', things, "What stands on it (see LEGEND); '.' means nothing")
