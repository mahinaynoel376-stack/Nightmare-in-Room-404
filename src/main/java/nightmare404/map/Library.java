package nightmare404.map;

import static nightmare404.tile.TileMap.*;
import static nightmare404.tile.TileSet.*;

import java.util.Map;

import nightmare404.tile.MapBuilder;
import nightmare404.tile.TileMap;
import nightmare404.tile.TileSet;

// The library on Floor 3: one big room, three screens wide. The screens are joined (walk off the
// side of one into the next), but the camera doesn't scroll: each screen is shown on its own,
// like a classroom. The only way in or out is the big double door on the middle screen.
//
// Each screen is written as a CHARACTER MAP: 18 strings of 25 characters, one character per tile.
// The legend below (and the LEGEND map) says which tile each character means; MapBuilder.paint reads it.
// What's underneath each character (brick wall or wooden floor) depends only on its row,
// and the stone wall strips around the room are added by code, like the classrooms have.
public class Library {
    public enum Part { WEST, ENTRANCE, EAST }

    private static final int TOP_WALL_ROW = 2;
    private static final int BOTTOM_WALL_ROW = 16;
    private static final int DOOR_COL = 10, DOOR_ROW = 15;   // The big door (4 x 2) in the bottom wall

    // LEGEND
    //   ' ' void (outside the room)    '.' wooden floor    'w' brick wall    '_' wall base
    //   'B' bookshelf   'W' window   'L' wall lamp   'X' locked bloody door
    //   'S' 'H' 'h'  blood writing: together they spell "SHHHHH"
    //   'q' 'Q'  QUIET sign (left and right half)
    //   '<' '>'  cobweb (top-left / top-right corner)   'm' claw marks   '!' bloody handprint
    //   'c' chair   't' table   'C' computer desk   'k' counter   'f' front desk   'K' card catalog
    //   'x' boxes   'n' broken bench   's' pile of books   'e' glowing eyes (watching from the dark)
    //   on the floor, walkable:  'p' papers   'o' open book   'b' scattered books
    //                            '*' blood   '%' blood drips   '~' crack

    // West: the stacks. Rows of bookshelves with narrow aisles between them.
    private static final String[] WEST = {
        "                         ",
        "                         ",
        "                         ",
        "   <wBBBwLwBBBwLwBBBwLwBB",
        "   wwBBBwWwBBBwWwBBBwWwBB",
        "   ______________________",
        "   ......................",
        "   .BBBBBB..BBBBBB..BBB..",
        "   .......s......o.......",
        "   .BBBBBB..BBBBBB..BBB..",
        "   e.......*.............",
        "   .BBBBBB..BBBBBB..BBB..",
        "   ....b.......~.........",
        "   .BBBBBB..BBnBBB..BBB..",
        "   .........p....s.......",
        "   ...%..................",
        "   ......................",
        "                         ",
    };

    // Middle: the entrance. Reading tables on the left, the librarian's desk on the right,
    // and the QUIET sign above a message written in blood.
    private static final String[] ENTRANCE = {
        "                         ",
        "                         ",
        "                         ",
        "BBBwLwBBBwwqQwwwBBBwLwBBB",
        "BBBwWwBBBwwSHhwwBBBwWwBBB",
        "_______________X_________",
        ".........................",
        "......o..................",
        "..ctc..ctc.....*......s..",
        ".........................",
        ".........................",
        "..ctc..ctc.......c..KK...",
        "...............Ckfk......",
        "...b...............p...xx",
        "......%..................",
        ".........................",
        ".........................",
        "                         ",
    };

    // East: the archive. Card catalogues, storage boxes, a reading corner, and something in the corner.
    private static final String[] EAST = {
        "                         ",
        "                         ",
        "                         ",
        "wwBBBwLwBBBwwmwBBBwLw>   ",
        "wwBBBwWwBBBw!wwBBBwWww   ",
        "______________________   ",
        "......................   ",
        ".KK..xx...............   ",
        "........ctc....BBBBB..   ",
        ".n....................   ",
        "........ctc....BBBBB..   ",
        "...p..................   ",
        ".....................e   ",
        "..%.....s......BBBBB..   ",
        "...........*..........   ",
        "......b...............   ",
        "......................   ",
        "                         ",
    };

    public static TileMap build(TileSet tiles, int ts, Part part) {
        String[] map = switch (part) {
            case WEST -> WEST;
            case ENTRANCE -> ENTRANCE;
            case EAST -> EAST;
        };
        MapBuilder b = new MapBuilder();
        b.fill(LAYER_FLOOR, 0, 0, 24, 17, VOID);
        b.paint(map, LEGEND, Library::underlay);
        frame(b, part);
        if (part == Part.ENTRANCE) {
            b.bigDoor(DOOR_COL, DOOR_ROW, LIB_FLOOR_1);
        }
        return b.build(tiles, ts);
    }

    // What's under every character: brick wall on rows 3-4, the wall base on row 5, wooden floor below.
    // The floor mixes its two tiles in a scattered pattern, so it doesn't look like a grid.
    private static int underlay(int col, int row) {
        if (row <= 4) return LIB_WALL;
        if (row == 5) return LIB_WALL_BASE;
        return (col * 7 + row * 3) % 5 == 0 ? LIB_FLOOR_2 : LIB_FLOOR_1;
    }

    // The legend: which tile goes on top of each character (EMPTY = nothing on top).
    // Map.entry(key, value) pairs a character with a tile ID.
    private static final Map<Character, Integer> LEGEND = Map.ofEntries(
            Map.entry('.', EMPTY), Map.entry('w', EMPTY), Map.entry('_', EMPTY),
            Map.entry('B', BOOKSHELF), Map.entry('W', LIB_WINDOW), Map.entry('L', WALL_LAMP),
            Map.entry('X', LIB_DOOR_BLOODY),
            Map.entry('S', BLOOD_WRITING_1), Map.entry('H', BLOOD_WRITING_2), Map.entry('h', BLOOD_WRITING_3),
            Map.entry('q', QUIET_SIGN_L), Map.entry('Q', QUIET_SIGN_R),
            Map.entry('<', COBWEB_LEFT), Map.entry('>', COBWEB_RIGHT), Map.entry('m', SCRATCHES), Map.entry('!', HANDPRINT),
            Map.entry('c', LIB_CHAIR), Map.entry('t', LIB_TABLE), Map.entry('C', COMPUTER_DESK),
            Map.entry('k', COUNTER), Map.entry('f', FRONT_DESK), Map.entry('K', CARD_CATALOG),
            Map.entry('x', LIB_BOXES), Map.entry('n', BROKEN_BENCH), Map.entry('s', BOOK_STACK), Map.entry('e', GLOWING_EYES),
            Map.entry('p', PAPERS), Map.entry('o', OPEN_BOOK), Map.entry('b', SCATTERED_BOOKS),
            Map.entry('*', BLOOD_SPLAT), Map.entry('%', BLOOD_DRIPS), Map.entry('~', CRACK));

    // The stone frame every room in the building has: a top wall, a bottom wall, and side walls
    // at the library's two ends. Between the three screens there's no wall: it's one room.
    private static void frame(MapBuilder b, Part part) {
        int first = part == Part.WEST ? 3 : 0;     // First and last columns inside the room
        int last = part == Part.EAST ? 21 : 24;
        b.wallH(TOP_WALL_ROW, first, last);
        b.wallH(BOTTOM_WALL_ROW, first, last);
        if (part == Part.WEST) {
            b.set(LAYER_WALLS_H, 2, TOP_WALL_ROW, WALL_CORNER_LEFT);
            b.set(LAYER_WALLS_H, 2, BOTTOM_WALL_ROW, WALL_CORNER_LEFT);
            b.wallVRight(LAYER_WALLS_V, 2, TOP_WALL_ROW + 1, BOTTOM_WALL_ROW);
        }
        if (part == Part.EAST) {
            b.set(LAYER_WALLS_H, 22, TOP_WALL_ROW, WALL_CORNER_RIGHT);
            b.set(LAYER_WALLS_H, 22, BOTTOM_WALL_ROW, WALL_CORNER_RIGHT);
            b.wallVLeft(LAYER_WALLS_V, 22, TOP_WALL_ROW + 1, BOTTOM_WALL_ROW);
        }
    }
}
