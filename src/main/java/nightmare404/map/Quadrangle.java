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

// The quadrangle: the school's courtyard, reached through the open exit on the ground floor.
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
// on it). The bus, the court's fence and the doorway are placed in code, because they're more than
// one tile or need invisible walls.
public class Quadrangle {
    public static final int WIDTH = 50, HEIGHT = 36;

    private static final int DOOR_COL = 36, DOOR_ROW = 3;      // The doorway from the East Stairs
    private static final int BUS_COL = 8, BUS_ROW = 27;         // Top-left of the bus
    private static final int CAGE_LEFT = 31, CAGE_TOP = 21, CAGE_RIGHT = 46, CAGE_BOTTOM = 33;
    private static final int CAGE_GATE_COL = 33;                // The 2-tile gap in the court's fence

    // A place worth knowing about: its name, the tiles it covers, and why it helps
    public record KeyLocation(String name, Rectangle area, String whyItHelps) {}

    public static final List<KeyLocation> STRONGHOLDS = List.of(
            new KeyLocation("The Wrecked Bus", new Rectangle(BUS_COL, BUS_ROW, BUS_WIDTH, 2),
                    "High ground. The roof is walled in on three sides; the only way up is the ladder at the "
                    + "back, so attackers have to climb it one at a time while you watch the whole car park."),
            new KeyLocation("The Book Barricade", new Rectangle(26, 8, 7, 2),
                    "A wall of library books and broken desks facing the south gate and the plaza. "
                    + "Two one-tile gaps are the only ways through its front, and its back is the building."),
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
        "____________________________________,,____________",
        ".,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,.",
        "....................................,,............",
        "....................................,,............",
        "....................................,,............",
        "....................................,,............",
        "....................................,,............",
        "....................................,,,,,,,,,,,,,.",
        "....................................,,,,,,,,,,,,,.",
        "....................................,,............",
        "...................,,,,,,,,,,,,,,,,,,,............",
        "...................,,,,,,,,,,,,,,,,,,,............",
        "...................,,,,,,,,,,,,...................",
        "...................,,,,,,,,,,,,...................",
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
        ".......L...........L...........L............L.....",
        "....W.....W.....W.....W.....W.............W...W...",
        ".............X....................................",
        "|................................................|",
        "|................................................|",
        "|..T..T..T..T..T..T..T...............f.........T.|",
        "|..................................Q..Q....T.....|",
        "|........................K.c.....k.q..q........T.|",
        "|.T......................D...*.b.K..f............|",
        "|........................Kk.KDk.Kk...............g",
        "|.................................Q..............g",
        "|.T...............Q...............q..f...........|",
        "|.................q..............................|",
        "|................T............f...s............T.|",
        "|.T.................B.~......B..........T........|",
        "|.......................A..f.....................|",
        "g.......................a......................T.|",
        "g.....~..........................................|",
        "|.............*.....B....f...B..T..............T.|",
        "|................T...............................|",
        "|.T...............Q..............................|",
        "|.................q.....s.....Q..................|",
        "|.............................q..................|",
        "|...........................................~..T.|",
        "|....................Q...f.......................|",
        "|...x............xx..q...........................|",
        "|.T.....................................*........|",
        "|.......................f......................T.|",
        "|................................................|",
        "|.T.............~................................|",
        "|...../../*./../../......f.......................|",
        "|...t./../../../../t...........................T.|",
        "|.T.../../../../../..............................|",
        "|...T...T...T...T...T...s...T.....T...T...T..T...|",
        "|-----------------------GG-----------------------|",
    };

    // LEGEND for THINGS
    //   on the building:  'W' window  'L' lamp  'X' locked bloody door
    //   cover:            'T' tree  'x' crates  't' picnic table  'B' park bench
    //   the barricade:    'K' 'k' piles of books  'D' broken desk  'c' fallen chair  'b' scattered books
    //   the fence:        '|' fence (side)  '-' fence (bottom)  'g' locked gate (side)  'G' locked gate (bottom)
    //   tall things:      'A' 'a' flagpole (top, bottom)  'Q' 'q' lamppost (top, bottom)
    //   on the ground:    '/' parking line  'f' footprints  's' slime  '*' blood  '~' crack
    private static final Map<Character, Integer> LEGEND = Map.ofEntries(
            Map.entry('.', EMPTY),
            Map.entry('W', Q_WINDOW), Map.entry('L', Q_LAMP), Map.entry('X', Q_DOOR_BLOODY),
            Map.entry('T', TREE), Map.entry('x', Q_BOXES), Map.entry('t', Q_TABLE), Map.entry('B', PARK_BENCH),
            Map.entry('K', BOOK_BARRICADE_1), Map.entry('k', BOOK_BARRICADE_2), Map.entry('D', DESK_BROKEN_1),
            Map.entry('c', CHAIR_FALLEN), Map.entry('b', SCATTERED_BOOKS),
            Map.entry('|', FENCE_V), Map.entry('-', FENCE_H), Map.entry('g', GATE_V), Map.entry('G', GATE_H),
            Map.entry('A', FLAGPOLE_TOP), Map.entry('a', FLAGPOLE_BOTTOM),
            Map.entry('Q', LAMPPOST_TOP), Map.entry('q', LAMPPOST_BOTTOM),
            Map.entry('/', PAINT_LINE), Map.entry('f', FOOTPRINTS), Map.entry('s', SLIME),
            Map.entry('*', BLOOD_SPLAT), Map.entry('~', CRACK));

    public static TileMap build(TileSet tiles, int ts) {
        MapBuilder b = new MapBuilder(WIDTH, HEIGHT);
        b.paint(THINGS, LEGEND, Quadrangle::ground);
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

    // The way back into the building: the same doorway as the exit on the other side, between two
    // pillars. It's a door (EXIT_L / EXIT_R), so going through it follows the door link back.
    private static void doorway(MapBuilder b) {
        b.set(LAYER_OBJECTS, DOOR_COL, DOOR_ROW, EXIT_L).set(LAYER_OBJECTS, DOOR_COL + 1, DOOR_ROW, EXIT_R);
        for (int col : new int[] {DOOR_COL - 1, DOOR_COL + 2}) {
            b.set(LAYER_OBJECTS, col, DOOR_ROW, PILLAR_TOP);
            b.set(LAYER_OBJECTS, col, DOOR_ROW + 1, PILLAR_BOTTOM);
        }
    }

    // Stronghold 1: the wrecked school bus, 7 x 3 tiles with its top-left at (col, row).
    // Its roof (rows 0-1) can be walked on, but invisible walls run along its left, right and far
    // sides; the side of the bus (row 2) is solid except the ladder at the back. So the ONLY way
    // onto the roof is up the ladder from below it: a single choke point.
    private static void bus(MapBuilder b, int col, int row) {
        for (int r = 0; r < BUS_HEIGHT; r++) {
            for (int c = 0; c < BUS_WIDTH; c++) {
                b.set(LAYER_OBJECTS, col + c, row + r, BUS_FIRST + r * BUS_WIDTH + c);
            }
        }
        b.edgeWall(col, row, row + 2);                  // Back of the roof, and the ladder's outer side
        b.edgeWall(col + BUS_WIDTH, row, row + 1);      // Front of the roof
        b.edgeWallH(row, col, col + BUS_WIDTH - 1);     // Far side of the roof
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
