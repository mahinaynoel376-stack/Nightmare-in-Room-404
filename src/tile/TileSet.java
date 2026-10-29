package tile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

// Every tile image in the game, loaded once and shared by all rooms.
// A tile is identified by its ID (an index into the images array):
//   0-35  the classroom tiles in res/tiles/Classroom2 (same numbers as their file names)
//   36-50 mirrored copies of the furniture, made in code, for classrooms that face left
//   51-56 the stair tiles in res/tiles/Stairs
public class TileSet {

    // --- Classroom2 tiles (0-35) ---
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

    public static final int COUNT = 57;

    private static final String CLASSROOM_DIR = "/tiles/Classroom2/";
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

    private static final String STAIRS_DIR = "/tiles/Stairs/";
    private static final String[] STAIR_FILES = {
        "stair_landing.png", "stair_step.png", "stair_rail.png",
        "stair_step_broken.png", "stair_hole.png", "rubble.png"
    };

    // Tiles the player can walk on. Everything else (walls, furniture, void, rubble...) blocks.
    private static final int[] WALKABLE = {
        FLOOR, DOOR_LEFT, DOOR_RIGHT, WINDOW, WINDOW_CURTAIN, STAIR_LANDING, STAIR_STEP
    };

    private final BufferedImage[] images = new BufferedImage[COUNT];
    private final boolean[] solid = new boolean[COUNT];

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

        java.util.Arrays.fill(solid, true);
        for (int id : WALKABLE) {
            solid[id] = false;
        }
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
