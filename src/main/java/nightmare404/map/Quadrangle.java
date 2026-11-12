package nightmare404.map;

import static nightmare404.tile.TileMap.*;
import static nightmare404.tile.TileSet.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

import nightmare404.tile.MapBuilder;
import nightmare404.tile.TileMap;
import nightmare404.tile.TileSet;

// The quadrangle: the school's courtyard, reached through the main entrance in the middle of the ground floor.
// It's one big map, 50 x 36 tiles (the area of 2 x 2 rooms), and the camera follows the player around it.
//
// It's built for a LAST STAND: attackers break in through three locked gates in the perimeter fence,
// and three STRONGHOLDS give the player better odds:
//   1. The Wrecked Bus      - high ground. Its roof is reached only by the ladder at the back.
//   2. The Book Barricade   - a wall of library books and desks facing the south gate, with two narrow gaps.
//   3. The Cage             - the fenced basketball court: one way in, and the fence stops attackers
//                             but not bullets (see TileSet.isShootThrough).
// None of the fighting exists yet; STRONGHOLDS and SPAWN_GATES are here for the gameplay to use.
//
// Written as TWO character maps of the same size: GROUND (what's underfoot) and THINGS (what stands
// on it). The bus, the court's fence and the doorways are placed in code, because they're more than
// one tile or need invisible walls.
public class Quadrangle {
    public static final int WIDTH = 50, HEIGHT = 36;

    private static final int DOOR_COL = 23, DOOR_ROW = 3;      // The main entrance (4 wide), in the middle
    private static final int[] SIDE_EXIT_COLS = {5, 43};        // The side exits at the two ends, boarded up
    private static final int BUS_COL = 8, BUS_ROW = 27;         // Top-left of the bus
    private static final int CAGE_LEFT = 31, CAGE_TOP = 21, CAGE_RIGHT = 46, CAGE_BOTTOM = 33;
    private static final int CAGE_GATE_COL = 33;                // The 2-tile gap in the court's fence

    // A place worth knowing about: its name, the tiles it covers, and why it helps
    public record KeyLocation(String name, Rectangle area, String whyItHelps) {}

    public static final List<KeyLocation> STRONGHOLDS = List.of(
            new KeyLocation("The Wrecked Bus", new Rectangle(BUS_COL, BUS_ROW, BUS_ROOF_WIDTH, 2),
                    "High ground. The roof is walled in on three sides; the only way up is the ladder at the "
                    + "back, so attackers have to climb it one at a time while you watch the whole car park."),
            new KeyLocation("The Book Barricade", new Rectangle(26, 8, 7, 2),
                    "A wall of library books and broken desks facing the south gate and the plaza, right in "
                    + "front of the main entrance. Two one-tile gaps are the only ways through its front, and its "
                    + "back is the building."),
            new KeyLocation("The Cage", new Rectangle(CAGE_LEFT + 1, CAGE_TOP + 1,
                    CAGE_RIGHT - CAGE_LEFT - 1, CAGE_BOTTOM - CAGE_TOP - 1),
                    "The fenced basketball court. There's one gate in; the fence stops attackers "
                    + "but not your shots, so you can hit them while they walk round to the gate."));

    // The locked gates in the perimeter fence (top tile of each 2-tile gate): where attackers break in
    public static final List<Point> SPAWN_GATES = List.of(
            new Point(0, 17), new Point(WIDTH - 1, 10), new Point(24, HEIGHT - 1));

    // What's underfoot: '.' grass  ',' paving  ':' asphalt  'w' wall  '_' wall base  ' ' void
    private static final String[] GROUND = {
        "                                                  ",
        "wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww",
        "wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww",
        "_____,,________________,,,,________________,,_____",
        ".,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,.",
        ".....,,................,,,,................,,.....",
        ".....,,................,,,,................,,.....",
        ".....,,................,,,,................,,.....",
        ".....,,................,,,,................,,.....",
        ".....,,................,,,,................,,.....",
        ".....,,................,,,,.........,,,,,,,,,,,,,.",
        ".....,,................,,,,.........,,,,,,,,,,,,,.",
        ".....,,................,,,,.........,,............",
        ".....,,............,,,,,,,,,,,,,,,,,,,............",
        ".....,,............,,,,,,,,,,,,,,,,,,,............",
        ".....,,............,,,,,,,,,,,,...................",
        ".....,,............,,,,,,,,,,,,...................",
        ".,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,...................",
        ".,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,...................",
        "...................,,,,,,,,,,,,...................",
        "...................,,,,,,,,,,,,...................",
        "...................,,,,,,,,,,,,...................",
        "........................,,......::::::::::::::....",
        "........................,,......::::::::::::::....",
        "...::::::::::::::::::...,,......::::::::::::::....",
        "...::::::::::::::::::...,,......::::::::::::::....",
        "...::::::::::::::::::...,,......::::::::::::::....",
        "...::::::::::::::::::...,,......::::::::::::::....",
        "...::::::::::::::::::...,,......::::::::::::::....",
        "...::::::::::::::::::...,,......::::::::::::::....",
        "...::::::::::::::::::...,,......::::::::::::::....",
        "...::::::::::::::::::...,,......::::::::::::::....",
        "...::::::::::::::::::...,,......::::::::::::::....",
        "...::::::::::::::::::...,,........................",
        "........................,,........................",
        "..................................................",
    };

    // What stands on it (see LEGEND); '.' means nothing
    private static final String[] THINGS = {
        "..................................................",
        ".......L............L........L............L.......",
        "..W.......W.....W................W.....W.......W..",
        "..................................................",
        "|................................................|",
        "|.h+...h+............h+....h+.T+.........h+..h+..|",
        "|........T+......T+...........++.....T+.....f....|",
        "|...Q..Q.++......++..................++...Q..Q...|",
        "|...q..q.................K.c.....k........q..q...|",
        "|........................D...*.b.K.........f.....|",
        "|........................Kk.KDk.Kk...............g",
        "|.T+..............................Q.....f........g",
        "|.++..............Q...............q..f...........|",
        "|...........o+....q..............................|",
        "|.............................f...s..............|",
        "|..................nB.~......Bn.........T+....T+.|",
        "|..................+....A..f..+.........++....++.|",
        "g.......................a.........o+.............|",
        "g.....~.....................................o+...|",
        "|.............*.....B....f...B.....n.............|",
        "|..................................+.............|",
        "|T+.....o+........Q..............................|",
        "|++...............q.....s.....Q..................|",
        "|....................h+...h+..q................T+|",
        "|...........................................~..++|",
        "|....................Q...f.......................|",
        "|...x............xx..q......T+...................|",
        "|T+...................n.....++..........*........|",
        "|++...................+.f........................|",
        "|................................................|",
        "|...............~................................|",
        "|...../../*./../../......f.......................|",
        "|...t./../../../../t.T+.....T+...................|",
        "|...../../../../../..++.....++...................|",
        "|.......................s........................|",
        "|-----------------------GG-----------------------|",
    };

    // LEGEND for THINGS
    //   on the building:  'W' window  'L' lamp  'X' locked bloody door
    //   cover:            'x' crates  't' picnic table  'B' park bench
    //   bigger things:    'T' tree (2 x 2)  'h' bush (2 x 1)  'o' rocks (2 x 1)  'n' trash can (1 x 2):
    //                     the letter is their top-left tile, '+' marks the rest (see PROPS)
    //   the barricade:    'K' 'k' piles of books  'D' broken desk  'c' fallen chair  'b' scattered books
    //   the fence:        '|' fence (side)  '-' fence (bottom)  'g' locked gate (side)  'G' locked gate (bottom)
    //   tall things:      'A' 'a' flagpole (top, bottom)  'Q' 'q' lamppost (top, bottom)
    //   on the ground:    '/' parking line  'f' footprints  's' slime  '*' blood  '~' crack
    private static final Map<Character, Integer> LEGEND = Map.ofEntries(
            Map.entry('.', EMPTY),
            Map.entry('W', WINDOW), Map.entry('L', Q_LAMP), Map.entry('X', Q_DOOR_BLOODY),
            Map.entry('x', Q_BOXES), Map.entry('t', Q_TABLE), Map.entry('B', PARK_BENCH),
            Map.entry('T', EMPTY), Map.entry('h', EMPTY), Map.entry('o', EMPTY), Map.entry('n', EMPTY), Map.entry('+', EMPTY),
            Map.entry('K', BOOK_BARRICADE_1), Map.entry('k', BOOK_BARRICADE_2), Map.entry('D', DESK_BROKEN_1),
            Map.entry('c', CHAIR_FALLEN), Map.entry('b', SCATTERED_BOOKS),
            Map.entry('|', FENCE_V), Map.entry('-', FENCE_H), Map.entry('g', GATE_V), Map.entry('G', GATE_H),
            Map.entry('A', FLAGPOLE_TOP), Map.entry('a', FLAGPOLE_BOTTOM),
            Map.entry('Q', LAMPPOST_TOP), Map.entry('q', LAMPPOST_BOTTOM),
            Map.entry('/', PAINT_LINE), Map.entry('f', FOOTPRINTS), Map.entry('s', SLIME),
            Map.entry('*', BLOOD_SPLAT), Map.entry('~', CRACK));

    // Things bigger than one tile. In THINGS each one is written as its top-left letter, with '+' on
    // the rest of the cells it covers; props() then fills in its tiles, in reading order from first.
    private record Prop(int first, int width, int height) {}
    private static final Map<Character, Prop> PROPS = Map.of(
            'T', new Prop(TREE_FIRST, 2, 2),
            'h', new Prop(BUSH_FIRST, 2, 1),
            'o', new Prop(ROCKS_FIRST, 2, 1),
            'n', new Prop(TRASH_CAN_FIRST, 1, 2));

    public static TileMap build(TileSet tiles, int ts) {
        MapBuilder b = new MapBuilder(WIDTH, HEIGHT);
        b.paint(THINGS, LEGEND, Quadrangle::ground);
        props(b);
        b.wallH(0, 0, WIDTH - 1);              // Stone edge along the top of the building's face
        doorway(b);
        bus(b, BUS_COL, BUS_ROW);
        cage(b);
        return b.build(tiles, ts);
    }

    // What's under each tile comes from the GROUND map
    private static int ground(int col, int row) {
        char ch = GROUND[row].charAt(col);
        return switch (ch) {
            case '.' -> Ground.grass(col, row);
            case ',' -> PAVING;
            case ':' -> (col * 7 + row * 3) % 5 == 0 ? Q_ASPHALT_2 : Q_ASPHALT_1;
            case 'w' -> Q_WALL;
            case '_' -> Q_WALL_BASE;
            case ' ' -> VOID;
            default -> throw new IllegalArgumentException(
                    "Unknown ground character '" + ch + "' at column " + col + ", row " + row);
        };
    }

    // The way back into the building: the main entrance, the same 4-wide doorway as on the other side,
    // between two pillars. It's a door (TileSet.doorSize knows it), so going through it follows the
    // door link back. The side exits at the two ends of the building are boarded up, like they are inside.
    private static void doorway(MapBuilder b) {
        for (int i = 0; i < MAIN_ENTRANCE_WIDTH; i++) {
            b.set(LAYER_OBJECTS, DOOR_COL + i, DOOR_ROW, MAIN_ENTRANCE_FIRST + i);
        }
        pillars(b, DOOR_COL - 1, DOOR_COL + MAIN_ENTRANCE_WIDTH);
        for (int col : SIDE_EXIT_COLS) {
            b.set(LAYER_OBJECTS, col, DOOR_ROW, EXIT_BLOCKED_L).set(LAYER_OBJECTS, col + 1, DOOR_ROW, EXIT_BLOCKED_R);
            pillars(b, col - 1, col + 2);
        }
    }

    // A support pillar (2 tiles tall) in each of these columns, standing on the building's face
    private static void pillars(MapBuilder b, int... cols) {
        for (int col : cols) {
            b.set(LAYER_OBJECTS, col, DOOR_ROW, PILLAR_TOP);
            b.set(LAYER_OBJECTS, col, DOOR_ROW + 1, PILLAR_BOTTOM);
        }
    }

    // Fills in the bigger things (see PROPS) wherever THINGS has their letter
    private static void props(MapBuilder b) {
        for (int row = 0; row < HEIGHT; row++) {
            for (int col = 0; col < WIDTH; col++) {
                Prop prop = PROPS.get(THINGS[row].charAt(col));
                if (prop == null) continue;
                for (int r = 0; r < prop.height(); r++) {
                    for (int c = 0; c < prop.width(); c++) {
                        b.set(LAYER_OBJECTS, col + c, row + r, prop.first() + r * prop.width() + c);
                    }
                }
            }
        }
    }

    // Stronghold 1: the wrecked school bus, 5 x 3 tiles with its top-left at (col, row).
    // Its roof (rows 0-1 of its first 4 columns) can be walked on. Its front (column 4) is lower and
    // solid, and so is its side (row 2), except the ladder in column 2. Invisible walls run along the
    // back and the far side of the roof. So the ONLY way onto the roof is up the ladder: a single
    // choke point.
    private static void bus(MapBuilder b, int col, int row) {
        for (int r = 0; r < BUS_HEIGHT; r++) {
            for (int c = 0; c < BUS_WIDTH; c++) {
                b.set(LAYER_OBJECTS, col + c, row + r, BUS_FIRST + r * BUS_WIDTH + c);
            }
        }
        b.edgeWall(col, row, row + 1);                          // Back of the roof
        b.edgeWallH(row, col, col + BUS_ROOF_WIDTH - 1);        // Far side of the roof
    }

    // Stronghold 3: the fenced basketball court. A fence all the way round with one 2-tile gap,
    // and a hoop at each end.
    private static void cage(MapBuilder b) {
        for (int col = CAGE_LEFT; col <= CAGE_RIGHT; col++) {
            b.set(LAYER_OBJECTS, col, CAGE_TOP, FENCE_H);
            b.set(LAYER_OBJECTS, col, CAGE_BOTTOM, FENCE_H);
        }
        for (int row = CAGE_TOP; row <= CAGE_BOTTOM; row++) {
            b.set(LAYER_OBJECTS, CAGE_LEFT, row, FENCE_V);
            b.set(LAYER_OBJECTS, CAGE_RIGHT, row, FENCE_V);
        }
        b.set(LAYER_OBJECTS, CAGE_GATE_COL, CAGE_TOP, EMPTY).set(LAYER_OBJECTS, CAGE_GATE_COL + 1, CAGE_TOP, EMPTY);
        int middle = (CAGE_LEFT + CAGE_RIGHT) / 2;
        b.set(LAYER_OBJECTS, middle, CAGE_TOP + 1, HOOP_N);
        b.set(LAYER_OBJECTS, middle, CAGE_BOTTOM - 1, HOOP_S);
    }
}
