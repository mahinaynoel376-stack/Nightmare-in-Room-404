package nightmare404.tile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

// Every tile image in the game, loaded once and shared by all rooms.
// A tile is identified by its ID (an index into the images array). The images are in src/main/resources:
//   0-35  the classroom tiles in tiles/classroom (same numbers as their file names)
//   36-50 mirrored copies of the furniture, made in code, for classrooms that face left
//   51-56 the stair tiles in tiles/stairs
//   57-99 the library tiles and the big double door in tiles/library
//   100-150 the lab tiles, lab doors and signs in tiles/lab
//   151-181 the ground floor tiles, pillars, grass, exits and plaques in tiles/ground
//   182-227 the quadrangle tiles, fences, the bus, barricades, hoops, flagpole and lampposts in tiles/quad
public class TileSet {

    // --- Classroom tiles (0-35) ---
    public static final int VOID = 0;
    public static final int FLOOR = 1;
    public static final int WALL = 2;
    public static final int WALL_BASE = 3;
    public static final int WALL_H_1 = 4;
    public static final int WALL_H_2 = 5;
    public static final int WALL_H_3 = 6;
    public static final int WALL_V_LEFT_1 = 7;   // "LEFT" = the stone strip is on the tile's left half
    public static final int WALL_V_RIGHT_1 = 8;  // "RIGHT" = the stone strip is on the tile's right half
    public static final int WALL_V_LEFT_2 = 9;
    public static final int WALL_V_RIGHT_2 = 10;
    public static final int WALL_V_LEFT_3 = 11;
    public static final int WALL_V_RIGHT_3 = 12;
    public static final int WALL_CORNER_LEFT = 13;
    public static final int WALL_CORNER_RIGHT = 14;
    public static final int CHAIR = 15;
    public static final int DESK = 16;
    public static final int TEACHER_DESK_TOP = 17;
    public static final int TEACHER_DESK_BOTTOM = 18;
    public static final int TEACHER_CHAIR_TOP = 19;
    public static final int TEACHER_CHAIR_BOTTOM = 20;
    public static final int DOOR_LEFT = 21;
    public static final int DOOR_RIGHT = 22;
    public static final int SIDE_BOARD_1 = 23;
    public static final int SIDE_BOARD_2 = 24;
    public static final int SIDE_BOARD_3 = 25;
    public static final int SIDE_BOARD_4 = 26;
    public static final int WINDOW = 27;
    public static final int WINDOW_CURTAIN = 28;
    public static final int CHAIR_FALLEN = 29;
    public static final int CHAIR_BROKEN_1 = 30;
    public static final int CHAIR_BROKEN_2 = 31;
    public static final int DESK_BROKEN_1 = 32;
    public static final int DESK_BROKEN_2 = 33;
    public static final int DOOR_BROKEN_LEFT = 34;
    public static final int DOOR_BROKEN_RIGHT = 35;

    // --- Mirrored furniture (36-50): same pictures flipped left-right, so students can face left ---
    public static final int CHAIR_L = 36;
    public static final int DESK_L = 37;
    public static final int TEACHER_DESK_TOP_L = 38;
    public static final int TEACHER_DESK_BOTTOM_L = 39;
    public static final int TEACHER_CHAIR_TOP_L = 40;
    public static final int TEACHER_CHAIR_BOTTOM_L = 41;
    public static final int SIDE_BOARD_1_L = 42;
    public static final int SIDE_BOARD_2_L = 43;
    public static final int SIDE_BOARD_3_L = 44;
    public static final int SIDE_BOARD_4_L = 45;
    public static final int CHAIR_FALLEN_L = 46;
    public static final int CHAIR_BROKEN_1_L = 47;
    public static final int CHAIR_BROKEN_2_L = 48;
    public static final int DESK_BROKEN_1_L = 49;
    public static final int DESK_BROKEN_2_L = 50;

    // --- Stairs (51-56) ---
    public static final int STAIR_LANDING = 51;
    public static final int STAIR_STEP = 52;
    public static final int STAIR_RAIL = 53;
    public static final int STAIR_STEP_BROKEN = 54;
    public static final int STAIR_HOLE = 55;
    public static final int RUBBLE = 56;

    // --- Library (57-91): cut from the library sheet, plus the QUIET sign and LIBRARY plaque ---
    // Many of these have the library's wooden floor or brick wall drawn into them,
    // so they're meant to sit on LIB_FLOOR / LIB_WALL.
    public static final int LIB_FLOOR_1 = 57;
    public static final int LIB_FLOOR_2 = 58;
    public static final int LIB_WALL = 59;
    public static final int LIB_WALL_BASE = 60;
    public static final int LIB_WINDOW = 61;
    public static final int LIB_DOOR_BLOODY = 62;
    public static final int LIB_TABLE = 63;
    public static final int LIB_BOXES = 64;
    public static final int BLOOD_SPLAT = 65;
    public static final int CRACK = 66;
    public static final int COBWEB_LEFT = 67;
    public static final int COBWEB_RIGHT = 68;
    public static final int HANDPRINT = 69;
    public static final int GLOWING_EYES = 70;
    public static final int WALL_LAMP = 71;
    public static final int SCRATCHES = 72;
    public static final int BOOKSHELF = 73;
    public static final int COMPUTER_DESK = 74;
    public static final int PAPERS = 75;
    public static final int FRONT_DESK = 76;
    public static final int LIB_CHAIR = 77;
    public static final int BROKEN_BENCH = 78;
    public static final int SCATTERED_BOOKS = 79;
    public static final int OPEN_BOOK = 80;
    public static final int BOOK_STACK = 81;
    public static final int BLOOD_WRITING_1 = 82;   // "SH" - with the next two it spells "SHHHHH"
    public static final int BLOOD_WRITING_2 = 83;
    public static final int BLOOD_WRITING_3 = 84;
    public static final int COUNTER = 85;
    public static final int CARD_CATALOG = 86;
    public static final int BLOOD_DRIPS = 87;
    public static final int QUIET_SIGN_L = 88;
    public static final int QUIET_SIGN_R = 89;
    public static final int LIBRARY_PLAQUE_L = 90;
    public static final int LIBRARY_PLAQUE_R = 91;

    // --- Big double door (92-99): 4 tiles wide, 2 tall. Top row 92-95, bottom row 96-99 ---
    public static final int BIG_DOOR_TL = 92;   // Top-left piece; the other 7 follow in reading order
    public static final int BIG_DOOR_TILES = 8;

    // --- Labs (100-150): cut from the lab sheet, plus lab doors, a broken pod and signs ---
    // The lab sheet's horror decals (blood, cobwebs, eyes...) are identical to the library's,
    // so the labs reuse those IDs instead of loading copies.
    public static final int LAB_FLOOR_1 = 100;
    public static final int LAB_FLOOR_2 = 101;
    public static final int LAB_WALL = 102;
    public static final int LAB_WALL_BASE = 103;
    public static final int LAB_WINDOW = 104;
    public static final int LAB_DOOR_BLOODY = 105;
    public static final int LAB_TABLE = 106;
    public static final int LAB_BOXES = 107;
    public static final int LAB_LAMP = 108;
    public static final int REAGENT_SHELF = 109;
    public static final int CONTAINMENT_POD = 110;
    public static final int SLIME = 111;
    public static final int BROKEN_GLASS = 112;
    public static final int HAZARD_TRIANGLE = 113;
    public static final int SPECIMEN_SHELF_1 = 114;
    public static final int SPECIMEN_SHELF_2 = 115;
    public static final int SPECIMEN_SHELF_3 = 116;
    public static final int SPECIMEN_SHELF_BROKEN = 117;
    public static final int BENCH_MICROSCOPE = 118;   // The three bench pieces join into one 3-tile bench
    public static final int BENCH_BURNER = 119;
    public static final int BENCH_SINK = 120;
    public static final int SKELETON = 121;
    public static final int STOOL = 122;
    public static final int STOOL_FALLEN = 123;
    public static final int PERIODIC_TABLE = 124;
    public static final int HAZARD_SIGN = 125;
    public static final int VENT_EYES = 126;
    public static final int BROKEN_LIGHT = 127;
    public static final int TOXIC_PUDDLE = 128;
    public static final int FOOTPRINTS = 129;
    public static final int DANGLING_WIRES = 130;
    public static final int CRACKED_MONITOR = 131;
    public static final int CRACKED_WALL = 132;
    public static final int HAZARD_DECAL = 133;      // The hazard sign without its bricks, for the hallway
    public static final int POD_BROKEN = 134;
    public static final int LAB_DOOR_L = 135;
    public static final int LAB_DOOR_R = 136;
    public static final int LAB_DOOR_SEALED_L = 137;
    public static final int LAB_DOOR_SEALED_R = 138;
    // Signs 3 tiles wide: the first ID of each, the next two follow
    public static final int SIGN_CHEMISTRY = 139;
    public static final int SIGN_BIOLOGY = 142;
    public static final int SIGN_CONTAINMENT = 145;
    public static final int LABORATORY_SIGN = 148;
    public static final int SIGN_WIDTH = 3;

    // --- Ground floor (151-181): cut from the ground sheet, plus pillars, grass, exits and plaques ---
    public static final int G_FLOOR_1 = 151;
    public static final int G_FLOOR_2 = 152;
    public static final int G_WALL = 153;
    public static final int G_WALL_BASE = 154;
    public static final int G_WINDOW = 155;
    public static final int G_DOOR_BLOODY = 156;
    public static final int G_TABLE = 157;
    public static final int G_BOXES = 158;
    public static final int G_LAMP = 159;
    public static final int COUNTER_DESK = 160;    // Reception counter; repeats sideways into one long counter
    public static final int CABINET = 161;
    public static final int BENCH = 162;
    public static final int FIRE_ALARM = 163;
    public static final int PILLAR_TOP = 164;      // A support pillar is 2 tiles tall: top above bottom
    public static final int PILLAR_BOTTOM = 165;
    public static final int GRASS_1 = 166;
    public static final int GRASS_2 = 167;
    public static final int GRASS_EDGE = 168;      // Grass with the shadow of the ledge above it
    public static final int EXIT_L = 169;          // A way out of the building (2 tiles): a door like any other
    public static final int EXIT_R = 170;
    public static final int EXIT_BLOCKED_L = 171;  // The same doorway boarded up (solid)
    public static final int EXIT_BLOCKED_R = 172;
    public static final int SIGN_FACULTY = 173;
    public static final int SIGN_PRINCIPAL = 176;
    public static final int SIGN_RECORDS = 179;

    // --- Quadrangle (182-227): cut from the quad sheet, plus fences, the bus, barricades and more ---
    public static final int Q_ASPHALT_1 = 182;
    public static final int Q_ASPHALT_2 = 183;
    public static final int Q_WALL = 184;          // The building's outside wall
    public static final int Q_WALL_BASE = 185;
    public static final int Q_WINDOW = 186;
    public static final int Q_DOOR_BLOODY = 187;
    public static final int Q_TABLE = 188;         // Picnic table (on asphalt)
    public static final int Q_BOXES = 189;         // Crates (on asphalt)
    public static final int Q_LAMP = 190;          // Wall lamp
    public static final int PAVING = 191;
    public static final int TREE = 192;
    public static final int GATE_H = 193;          // Fence with a padlocked gate
    public static final int PARK_BENCH = 194;      // (on paving)
    public static final int FENCE_H = 195;
    public static final int FENCE_V = 196;         // The fence turned for the side edges
    public static final int GATE_V = 197;
    public static final int BUS_FIRST = 198;       // The wrecked bus: 7 x 3 tiles, IDs 198-218 in reading order.
    public static final int BUS_WIDTH = 7;         // Rows 0-1 are its roof (walkable), row 2 its side;
    public static final int BUS_HEIGHT = 3;        // the first tile of row 2 is the ladder (walkable)
    public static final int BOOK_BARRICADE_1 = 219;
    public static final int BOOK_BARRICADE_2 = 220;
    public static final int HOOP_N = 221;          // Basketball hoop at the north end of the court
    public static final int HOOP_S = 222;          // ...and the south end
    public static final int FLAGPOLE_TOP = 223;
    public static final int FLAGPOLE_BOTTOM = 224;
    public static final int LAMPPOST_TOP = 225;
    public static final int LAMPPOST_BOTTOM = 226;
    public static final int PAINT_LINE = 227;      // Worn parking line

    public static final int COUNT = 228;

    private static final String CLASSROOM_DIR = "/tiles/classroom/";
    private static final String[] CLASSROOM_FILES = {
        "00_void.png",
        "01_floor.png",
        "02_wall.png",
        "03_wall_base.png",
        "04_wall_h_1.png",
        "05_wall_h_2.png",
        "06_wall_h_3.png",
        "07_wall_v_left_1.png",
        "08_wall_v_right_1.png",
        "09_wall_v_left_2.png",
        "10_wall_v_right_2.png",
        "11_wall_v_left_3.png",
        "12_wall_v_right_3.png",
        "13_wall_corner_left.png",
        "14_wall_corner_right.png",
        "15_chair.png",
        "16_desk.png",
        "17_teacher_desk_top.png",
        "18_teacher_desk_bottom.png",
        "19_teacher_chair_top.png",
        "20_teacher_chair_bottom.png",
        "21_door_left.png",
        "22_door_right.png",
        "23_side_board_1.png",
        "24_side_board_2.png",
        "25_side_board_3.png",
        "26_side_board_4.png",
        "27_window.png",
        "28_window_curtain.png",
        "29_chair_fallen.png",
        "30_chair_broken_1.png",
        "31_chair_broken_2.png",
        "32_desk_broken_1.png",
        "33_desk_broken_2.png",
        "34_door_broken_left.png",
        "35_door_broken_right.png"
    };

    // Which tile each mirrored ID (36, 37, ...) is copied from, in order
    private static final int[] MIRROR_SOURCES = {
        CHAIR, DESK, TEACHER_DESK_TOP, TEACHER_DESK_BOTTOM, TEACHER_CHAIR_TOP, TEACHER_CHAIR_BOTTOM,
        SIDE_BOARD_1, SIDE_BOARD_2, SIDE_BOARD_3, SIDE_BOARD_4,
        CHAIR_FALLEN, CHAIR_BROKEN_1, CHAIR_BROKEN_2, DESK_BROKEN_1, DESK_BROKEN_2
    };

    private static final String STAIRS_DIR = "/tiles/stairs/";
    private static final String[] STAIR_FILES = {
        "stair_landing.png", "stair_step.png", "stair_rail.png",
        "stair_step_broken.png", "stair_hole.png", "rubble.png"
    };

    // In the same order as the IDs 57-99 above
    private static final String LIBRARY_DIR = "/tiles/library/";
    private static final String[] LIBRARY_FILES = {
        "lib_floor_1.png", "lib_floor_2.png", "lib_wall.png", "lib_wall_base.png",
        "lib_window.png", "lib_door_bloody.png", "lib_table.png", "lib_boxes.png",
        "blood_splat.png", "crack.png", "cobweb_left.png", "cobweb_right.png",
        "handprint.png", "glowing_eyes.png", "wall_lamp.png", "scratches.png",
        "bookshelf.png", "computer_desk.png", "papers.png", "front_desk.png",
        "lib_chair.png", "broken_bench.png", "scattered_books.png", "open_book.png",
        "book_stack.png", "blood_writing_1.png", "blood_writing_2.png", "blood_writing_3.png",
        "counter.png", "card_catalog.png", "blood_drips.png",
        "quiet_sign_left.png", "quiet_sign_right.png", "library_plaque_left.png", "library_plaque_right.png",
        "big_door_0.png", "big_door_1.png", "big_door_2.png", "big_door_3.png",
        "big_door_4.png", "big_door_5.png", "big_door_6.png", "big_door_7.png"
    };

    // In the same order as the IDs 100-150 above
    private static final String LAB_DIR = "/tiles/lab/";
    private static final String[] LAB_FILES = {
        "lab_floor_1.png", "lab_floor_2.png", "lab_wall.png", "lab_wall_base.png", "lab_window.png",
        "lab_door_bloody.png", "lab_table.png", "lab_boxes.png", "lab_lamp.png",
        "reagent_shelf.png", "containment_pod.png", "slime.png", "broken_glass.png", "hazard_triangle.png",
        "specimen_shelf_1.png", "specimen_shelf_2.png", "specimen_shelf_3.png", "specimen_shelf_broken.png",
        "bench_microscope.png", "bench_burner.png", "bench_sink.png", "skeleton.png",
        "stool.png", "stool_fallen.png", "periodic_table.png",
        "hazard_sign.png", "vent_eyes.png", "broken_light.png", "toxic_puddle.png",
        "footprints.png", "dangling_wires.png", "cracked_monitor.png", "cracked_wall.png",
        "hazard_decal.png", "pod_broken.png",
        "lab_door_l.png", "lab_door_r.png", "lab_door_sealed_l.png", "lab_door_sealed_r.png",
        "sign_chemistry_1.png", "sign_chemistry_2.png", "sign_chemistry_3.png",
        "sign_biology_1.png", "sign_biology_2.png", "sign_biology_3.png",
        "sign_containment_1.png", "sign_containment_2.png", "sign_containment_3.png",
        "laboratory_sign_1.png", "laboratory_sign_2.png", "laboratory_sign_3.png"
    };

    // In the same order as the IDs 151-181 above
    private static final String GROUND_DIR = "/tiles/ground/";
    private static final String[] GROUND_FILES = {
        "g_floor_1.png", "g_floor_2.png", "g_wall.png", "g_wall_base.png", "g_window.png",
        "g_door_bloody.png", "g_table.png", "g_boxes.png", "g_lamp.png",
        "counter_desk.png", "cabinet.png", "bench.png", "fire_alarm.png",
        "pillar_top.png", "pillar_bottom.png", "grass_1.png", "grass_2.png", "grass_edge.png",
        "exit_l.png", "exit_r.png", "exit_blocked_l.png", "exit_blocked_r.png",
        "sign_faculty_1.png", "sign_faculty_2.png", "sign_faculty_3.png",
        "sign_principal_1.png", "sign_principal_2.png", "sign_principal_3.png",
        "sign_records_1.png", "sign_records_2.png", "sign_records_3.png"
    };

    // In the same order as the IDs 182-227 above
    private static final String QUAD_DIR = "/tiles/quad/";
    private static final String[] QUAD_FILES = {
        "q_asphalt_1.png", "q_asphalt_2.png", "q_wall.png", "q_wall_base.png", "q_window.png",
        "q_door_bloody.png", "q_table.png", "q_boxes.png", "q_lamp.png",
        "paving.png", "tree.png", "gate_h.png", "park_bench.png", "fence_h.png", "fence_v.png", "gate_v.png",
        "bus_0.png", "bus_1.png", "bus_2.png", "bus_3.png", "bus_4.png", "bus_5.png", "bus_6.png",
        "bus_7.png", "bus_8.png", "bus_9.png", "bus_10.png", "bus_11.png", "bus_12.png", "bus_13.png",
        "bus_14.png", "bus_15.png", "bus_16.png", "bus_17.png", "bus_18.png", "bus_19.png", "bus_20.png",
        "book_barricade_1.png", "book_barricade_2.png", "hoop_n.png", "hoop_s.png",
        "flagpole_top.png", "flagpole_bottom.png", "lamppost_top.png", "lamppost_bottom.png", "paint_line.png"
    };

    // Solid tiles that a bullet could still pass through: you can't walk through a fence or over a
    // bench, but you can shoot through or over them. Nothing uses this yet; it's ready for the shooting.
    private static final int[] SHOOT_THROUGH = {
        FENCE_H, FENCE_V, GATE_H, GATE_V, PARK_BENCH, Q_TABLE, BENCH
    };

    // Tiles the player can walk on. Everything else (walls, furniture, void, rubble...) blocks.
    // Decorations painted on the floor (blood, papers, cracks...) are walkable; the same decorations
    // on a wall still block, because the wall tile underneath is solid.
    private static final int[] WALKABLE = {
        FLOOR, DOOR_LEFT, DOOR_RIGHT, WINDOW, WINDOW_CURTAIN, STAIR_LANDING, STAIR_STEP,
        LIB_FLOOR_1, LIB_FLOOR_2, BLOOD_SPLAT, CRACK, COBWEB_LEFT, COBWEB_RIGHT, HANDPRINT, SCRATCHES,
        PAPERS, SCATTERED_BOOKS, OPEN_BOOK, BLOOD_DRIPS,
        QUIET_SIGN_L, QUIET_SIGN_R, LIBRARY_PLAQUE_L, LIBRARY_PLAQUE_R,
        LAB_FLOOR_1, LAB_FLOOR_2, SLIME, BROKEN_GLASS, TOXIC_PUDDLE, FOOTPRINTS, DANGLING_WIRES, HAZARD_DECAL,
        LAB_DOOR_L, LAB_DOOR_R,
        G_FLOOR_1, G_FLOOR_2, FIRE_ALARM, GRASS_1, GRASS_2, GRASS_EDGE, EXIT_L, EXIT_R,
        Q_ASPHALT_1, Q_ASPHALT_2, PAVING, PAINT_LINE,
        FLAGPOLE_TOP, LAMPPOST_TOP     // The tops are up in the air: you walk past underneath them
    };

    private final BufferedImage[] images = new BufferedImage[COUNT];
    private final boolean[] solid = new boolean[COUNT];
    private final boolean[] shootThrough = new boolean[COUNT];

    public TileSet() {
        for (int i = 0; i < CLASSROOM_FILES.length; i++) {
            images[i] = load(CLASSROOM_DIR + CLASSROOM_FILES[i]);
        }
        for (int i = 0; i < MIRROR_SOURCES.length; i++) {
            images[CHAIR_L + i] = mirror(images[MIRROR_SOURCES[i]]);
        }
        for (int i = 0; i < STAIR_FILES.length; i++) {
            images[STAIR_LANDING + i] = load(STAIRS_DIR + STAIR_FILES[i]);
        }
        for (int i = 0; i < LIBRARY_FILES.length; i++) {
            images[LIB_FLOOR_1 + i] = load(LIBRARY_DIR + LIBRARY_FILES[i]);
        }
        for (int i = 0; i < LAB_FILES.length; i++) {
            images[LAB_FLOOR_1 + i] = load(LAB_DIR + LAB_FILES[i]);
        }
        for (int i = 0; i < GROUND_FILES.length; i++) {
            images[G_FLOOR_1 + i] = load(GROUND_DIR + GROUND_FILES[i]);
        }
        for (int i = 0; i < QUAD_FILES.length; i++) {
            images[Q_ASPHALT_1 + i] = load(QUAD_DIR + QUAD_FILES[i]);
        }

        java.util.Arrays.fill(solid, true);
        for (int id : WALKABLE) {
            solid[id] = false;
        }
        for (int i = 0; i < BIG_DOOR_TILES; i++) {
            solid[BIG_DOOR_TL + i] = false;   // Like the small doors: you walk into a door to go through it
        }
        for (int id = SIGN_CHEMISTRY; id < LABORATORY_SIGN + SIGN_WIDTH; id++) {
            solid[id] = false;                // Signs hang on walls (the wall underneath still blocks)
        }
        for (int id = SIGN_FACULTY; id < SIGN_RECORDS + SIGN_WIDTH; id++) {
            solid[id] = false;
        }
        for (int i = 0; i <= 2 * BUS_WIDTH; i++) {
            solid[BUS_FIRST + i] = false;     // The bus roof (2 rows) and the ladder just below it
        }
        for (int id : SHOOT_THROUGH) {
            shootThrough[id] = true;
        }
    }

    // True if a bullet can pass this tile: anything you can walk on, plus fences, benches and the like
    public boolean isShootThrough(int id) {
        return !solid[id] || shootThrough[id];
    }

    // Doors are recognised by their top-left piece. Returns {width, height} in tiles if this tile
    // starts a door, or null if it doesn't. To add a new kind of door, add one line here.
    public static int[] doorSize(int id) {
        if (id == DOOR_LEFT || id == LAB_DOOR_L || id == EXIT_L) return new int[] {2, 1};
        if (id == BIG_DOOR_TL) return new int[] {4, 2};
        return null;
    }

    public BufferedImage image(int id) {
        return images[id];
    }

    public boolean isSolid(int id) {
        return solid[id];
    }

    private BufferedImage load(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Missing tile: " + path);
            }
            return ImageIO.read(is);
        } catch (IOException e) {
            throw new IllegalStateException("Can't read tile: " + path, e);
        }
    }

    // Returns a left-right flipped copy: pixel x is copied to (width - 1 - x)
    private static BufferedImage mirror(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out.setRGB(w - 1 - x, y, src.getRGB(x, y));
            }
        }
        return out;
    }
}
