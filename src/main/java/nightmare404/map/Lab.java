package nightmare404.map;

import static nightmare404.tile.TileMap.*;
import static nightmare404.tile.TileSet.*;

import java.util.Map;

import nightmare404.tile.TileMap;
import nightmare404.tile.TileSet;

// The three labs on Floor 2. Each one is a single screen with the same shape as a classroom
// (side walls at columns 2 and 22, doors in the bottom wall at columns 5-6 and 18-19), so the
// hallway lines up with them exactly like it does on Floor 4. Written as character maps, like the library.
//
// The rooms tell a story without any text: in the Containment Lab one pod has been smashed from
// the inside, and a trail of slime and footprints leads out of the door towards the east stairs.
public class Lab {
    public enum Part { CHEMISTRY, BIOLOGY, CONTAINMENT }


    // LEGEND
    //   ' ' void   '.' lab floor   'w' tiled wall   '_' wall base
    //   doors (bottom wall):  'D' 'd' steel lab door (left, right half)   'Z' 'z' sealed with caution tape
    //   on the wall:  'W' window  'L' lamp  'F' broken fluorescent light  'A' hazard triangle  'Y' hazard sign
    //                 'P' periodic table  'V' vent (something's eyes behind it)  'M' cracked monitor
    //                 '/' cracked wall  'X' locked bloody door  '!' handprint  'c' claw marks  '<' '>' cobwebs
    //   shelves:      '1' '2' '3' specimen shelves   '4' smashed specimen shelf   'R' reagent shelf
    //   furniture:    'm' 'u' 'n' lab bench (microscope, burner, sink - together one 3-tile bench)
    //                 'S' skeleton  'h' stool  'H' knocked-over stool  't' table  'x' boxes
    //                 'O' containment pod  'Q' smashed pod  'e' glowing eyes
    //   on the floor, walkable:  'g' slime  'G' toxic puddle  'f' bloody footprints  'i' broken glass
    //                            'r' dangling wires  '*' blood  '~' crack

    // Chemistry: three rows of lab benches with stools, reagent shelves, the periodic table.
    // Its right-hand door has been sealed.
    private static final String[] CHEMISTRY = {
        "                         ",
        "                         ",
        "                         ",
        "   wwLwwFwwwwwwwwLwww>   ",
        "   wwWwwwwwPwwAwwWwwYw   ",
        "   ___________________   ",
        "   ...................   ",
        "   .R.........r.....R.   ",
        "   ..mun..mun..mun....   ",
        "   ..h.h..H.h..h.h....   ",
        "   ......G.....~......   ",
        "   ..mun..mun..mun....   ",
        "   ..h.H..h.h..h.h....   ",
        "   ..i.........g...*..   ",
        "   R.................R   ",
        "   ...................   ",
        "   ..Dd...........Zz..   ",
        "                         ",
    };

    // Biology: specimen shelves along the wall and in the room, dissection tables, the skeleton.
    private static final String[] BIOLOGY = {
        "                         ",
        "                         ",
        "                         ",
        "   <w123wLw213wLw321ww   ",
        "   ww312wWw132wWw213!w   ",
        "   ___________________   ",
        "   ...................   ",
        "   ..S........231.312.   ",
        "   ......f............   ",
        "   ..t.t.t............   ",
        "   ..h.h.H....312.123.   ",
        "   .........g.........   ",
        "   ..t.t.t............   ",
        "   ..h.H.h....123.413.   ",
        "   ....*.....f.......i   ",
        "   ...................   ",
        "   ..Dd...........Dd..   ",
        "                         ",
    };

    // Containment: two rows of pods. One is smashed; follow the trail it left.
    private static final String[] CONTAINMENT = {
        "                         ",
        "                         ",
        "                         ",
        "   wFwwYwwFwwwAwwFw/w>   ",
        "   wMwwMww/wVwwMwwMw/w   ",
        "   ___________________   ",
        "   ....r.........r....   ",
        "   ..O.O.O.Q.O.O.O....   ",
        "   ........ig.........   ",
        "   .........gg........   ",
        "   ..O.O.O....g.O.O...   ",
        "   ............g......   ",
        "   .....x......fG.....   ",
        "   ..t.H.........f...e   ",
        "   ...............f...   ",
        "   ..*................   ",
        "   ..Zz...........Dd..   ",
        "                         ",
    };

    private static final Map<Character, Integer> LEGEND = Map.ofEntries(
            Map.entry('.', EMPTY), Map.entry('w', EMPTY), Map.entry('_', EMPTY),
            Map.entry('D', LAB_DOOR_L), Map.entry('d', LAB_DOOR_R),
            Map.entry('Z', LAB_DOOR_SEALED_L), Map.entry('z', LAB_DOOR_SEALED_R),
            Map.entry('W', WINDOW), Map.entry('L', LAB_LAMP), Map.entry('F', BROKEN_LIGHT),
            Map.entry('A', HAZARD_TRIANGLE), Map.entry('Y', HAZARD_SIGN), Map.entry('P', PERIODIC_TABLE),
            Map.entry('V', VENT_EYES), Map.entry('M', CRACKED_MONITOR), Map.entry('/', CRACKED_WALL),
            Map.entry('X', LAB_DOOR_BLOODY), Map.entry('!', HANDPRINT), Map.entry('c', SCRATCHES),
            Map.entry('<', COBWEB_LEFT), Map.entry('>', COBWEB_RIGHT),
            Map.entry('1', SPECIMEN_SHELF_1), Map.entry('2', SPECIMEN_SHELF_2), Map.entry('3', SPECIMEN_SHELF_3),
            Map.entry('4', SPECIMEN_SHELF_BROKEN), Map.entry('R', REAGENT_SHELF),
            Map.entry('m', BENCH_MICROSCOPE), Map.entry('u', BENCH_BURNER), Map.entry('n', BENCH_SINK),
            Map.entry('S', SKELETON), Map.entry('h', STOOL), Map.entry('H', STOOL_FALLEN),
            Map.entry('t', LAB_TABLE), Map.entry('x', LAB_BOXES),
            Map.entry('O', CONTAINMENT_POD), Map.entry('Q', POD_BROKEN), Map.entry('e', GLOWING_EYES),
            Map.entry('g', SLIME), Map.entry('G', TOXIC_PUDDLE), Map.entry('f', FOOTPRINTS),
            Map.entry('i', BROKEN_GLASS), Map.entry('r', DANGLING_WIRES),
            Map.entry('*', BLOOD_SPLAT), Map.entry('~', CRACK));

    public static TileMap build(TileSet tiles, int ts, Part part) {
        return SingleRoom.build(tiles, ts, mapFor(part), LEGEND, Lab::underlay);
    }

    // The name plaque for the hallway wall in front of each lab (3 tiles: this ID and the next two)
    public static int sign(Part part) {
        return switch (part) {
            case CHEMISTRY -> SIGN_CHEMISTRY;
            case BIOLOGY -> SIGN_BIOLOGY;
            case CONTAINMENT -> SIGN_CONTAINMENT;
        };
    }

    private static String[] mapFor(Part part) {
        return switch (part) {
            case CHEMISTRY -> CHEMISTRY;
            case BIOLOGY -> BIOLOGY;
            case CONTAINMENT -> CONTAINMENT;
        };
    }

    // Tiled wall on rows 3-4, the wall base on row 5, lab floor below (two tiles mixed, like the library)
    private static int underlay(int col, int row) {
        if (row <= 4) return LAB_WALL;
        if (row == 5) return LAB_WALL_BASE;
        return (col * 7 + row * 3) % 5 == 0 ? LAB_FLOOR_2 : LAB_FLOOR_1;
    }
}
