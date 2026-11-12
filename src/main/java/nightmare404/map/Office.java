package nightmare404.map;

import static nightmare404.tile.TileMap.*;
import static nightmare404.tile.TileSet.*;

import java.util.Map;

import nightmare404.tile.TileMap;
import nightmare404.tile.TileSet;

// The three rooms on the ground floor (Floor 1): the school's offices. Each is a single screen in
// the classroom shape (see SingleRoom), drawn from a character map in the ground floor's stone palette.
// Besides the ground sheet's own furniture they use the building's standard chairs and teacher's desk,
// which have see-through backgrounds and so fit on any floor.
public class Office {
    public enum Part { FACULTY, PRINCIPAL, RECORDS }

    // LEGEND
    //   ' ' void   '.' stone floor   'w' wall   '_' wall base
    //   doors (bottom wall):  'D' 'd' door (left, right half)   'Z' 'z' broken door
    //   on the wall:  'W' window  'L' lamp  'X' locked bloody door  'A' fire alarm bell
    //                 '<' '>' cobwebs  '!' handprint  'm' claw marks
    //   furniture:    'K' cabinet  'k' reception counter (repeats sideways)  'b' bench  't' table  'x' boxes
    //                 'c' chair (facing right)  'C' chair (facing left)  'n' broken chair
    //                 'T' 'Y' desk (top, bottom)  'H' 'J' desk chair (top, bottom)   'e' glowing eyes
    //   on the floor, walkable:  '*' blood  '~' crack

    // Faculty Room: the teachers' desks pushed together in threes, cupboards along the wall
    private static final String[] FACULTY = {
        "                         ",
        "                         ",
        "                         ",
        "   <wKKwLwwKKwwLwwKKww   ",
        "   wwKKwWwwKKwAWwwKKw!   ",
        "   ___________________   ",
        "   ...................   ",
        "   ...................   ",
        "   ..ctC...ctC...ctC..   ",
        "   ..ctC...ctC...ctC..   ",
        "   ......*............   ",
        "   ..ctC...ntC...ct...   ",
        "   ..ctC...ctC...ctC..   ",
        "   ...................   ",
        "   x...........~.....x   ",
        "   ...................   ",
        "   ..Dd...........Zz..   ",
        "                         ",
    };

    // Principal's Office: a waiting area by the doors, the reception counter across the room
    // (with a gap to walk through), and the principal's desk behind it. A locked door leads further in.
    private static final String[] PRINCIPAL = {
        "                         ",
        "                         ",
        "                         ",
        "   wwLwwKKwwwwwKKwwLww   ",
        "   wwWwwKKwwwwwKKwwWwA   ",
        "   ___________________   ",
        "   ...................   ",
        "   .K.........cTH.....   ",
        "   .K.........cYJ.....   ",
        "   ...................   ",
        "   kkkkkk...kkkkkkkkkk   ",
        "   ...................   ",
        "   ..bb....*......bb..   ",
        "   ...................   ",
        "   ..bb.....~.....bb..   ",
        "   ...................   ",
        "   ..Dd...........Dd..   ",
        "                         ",
    };

    // Records Room: rows of filing cabinets, boxes of files, and something in the corner
    private static final String[] RECORDS = {
        "                         ",
        "                         ",
        "                         ",
        "   wwKKKwLwKKKwLwKKKw>   ",
        "   wwKKKwWwKKKwWwKKKmw   ",
        "   ___________________   ",
        "   ...................   ",
        "   .KKKKK..KKKKK..KKK.   ",
        "   ........x..........   ",
        "   .KKKKK..KKKKK..KKK.   ",
        "   ...*..............e   ",
        "   .KKKKK..KKxKK..KKK.   ",
        "   ........~..........   ",
        "   ..t.......xx.......   ",
        "   ...................   ",
        "   ...................   ",
        "   ..Zz...........Dd..   ",
        "                         ",
    };

    private static final Map<Character, Integer> LEGEND = Map.ofEntries(
            Map.entry('.', EMPTY), Map.entry('w', EMPTY), Map.entry('_', EMPTY),
            Map.entry('D', DOOR_LEFT), Map.entry('d', DOOR_RIGHT),
            Map.entry('Z', DOOR_BROKEN_LEFT), Map.entry('z', DOOR_BROKEN_RIGHT),
            Map.entry('W', WINDOW), Map.entry('L', G_LAMP), Map.entry('X', G_DOOR_BLOODY), Map.entry('A', FIRE_ALARM),
            Map.entry('<', COBWEB_LEFT), Map.entry('>', COBWEB_RIGHT), Map.entry('!', HANDPRINT), Map.entry('m', SCRATCHES),
            Map.entry('K', CABINET), Map.entry('k', COUNTER_DESK), Map.entry('b', BENCH),
            Map.entry('t', G_TABLE), Map.entry('x', G_BOXES),
            Map.entry('c', CHAIR), Map.entry('C', CHAIR_L), Map.entry('n', CHAIR_BROKEN_1),
            Map.entry('T', TEACHER_DESK_TOP), Map.entry('Y', TEACHER_DESK_BOTTOM),
            Map.entry('H', TEACHER_CHAIR_TOP), Map.entry('J', TEACHER_CHAIR_BOTTOM),
            Map.entry('e', GLOWING_EYES), Map.entry('*', BLOOD_SPLAT), Map.entry('~', CRACK));

    public static TileMap build(TileSet tiles, int ts, Part part) {
        String[] map = switch (part) {
            case FACULTY -> FACULTY;
            case PRINCIPAL -> PRINCIPAL;
            case RECORDS -> RECORDS;
        };
        return SingleRoom.build(tiles, ts, map, LEGEND, Office::underlay);
    }

    // The name plaque for the hallway wall in front of each room (3 tiles: this ID and the next two)
    public static int sign(Part part) {
        return switch (part) {
            case FACULTY -> SIGN_FACULTY;
            case PRINCIPAL -> SIGN_PRINCIPAL;
            case RECORDS -> SIGN_RECORDS;
        };
    }

    // Brick wall on rows 3-4, the wall base on row 5, stone floor below (two tiles mixed)
    private static int underlay(int col, int row) {
        if (row <= 4) return G_WALL;
        if (row == 5) return G_WALL_BASE;
        return (col * 7 + row * 3) % 5 == 0 ? G_FLOOR_2 : G_FLOOR_1;
    }
}
