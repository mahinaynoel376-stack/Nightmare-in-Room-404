package tile;

import static tile.TileMap.*;
import static tile.TileSet.*;

// The Floor 4 hallway: an open corridor running along the bottom of the classrooms.
// Every hallway screen (and the hallway part of the stairs screens) uses the same rows:
//
//   rows 0-2    void (above the building)
//   row  3      top edge of the classrooms' front wall
//   rows 4-5    the front wall (windows on row 5)
//   row  6      bottom of the wall, with the classroom doors
//   rows 7-11   the corridor the player walks along
//   row  12     the railing at the edge of the corridor (solid)
//   rows 13-17  the floor below, seen over the railing (darkened, can't be reached)
public class Hallway {
    static final int CAP_ROW = 3;
    static final int WINDOW_ROW = 5;
    static final int BASE_ROW = 6;
    static final int CORRIDOR_TOP = 7;
    static final int CORRIDOR_BOTTOM = 11;
    static final int RAIL_ROW = 12;
    static final int BELOW_CAP_ROW = 13;
    static final int BELOW_BASE_ROW = 16;
    static final int BELOW_FLOOR_ROW = 17;

    // Door positions shared by every classroom: the left column of each 2-tile door
    static final int[] DOOR_COLS = {5, 18};
    // The classrooms' side walls are in these columns, so the pillars between rooms go here too
    static final int[] PILLAR_COLS = {2, 22};

    // The hallway screen in front of one classroom.
    // Its doors copy the classroom's bottom wall: same columns, broken in the same places.
    public static TileMap outside(TileSet tiles, int ts, int[][] classroomObjects, int[] windowCols, int[] curtainCols) {
        MapBuilder b = new MapBuilder();
        b.fill(LAYER_FLOOR, 0, 0, 24, 17, VOID);

        wallFace(b, CAP_ROW, 0, 24);
        corridor(b, 0, 24);
        railing(b, 0, 24);
        floorBelow(b, 0, 24);

        for (int col : PILLAR_COLS) {
            b.pillar(col, CAP_ROW, BASE_ROW);
            b.pillar(col, BELOW_CAP_ROW, BELOW_BASE_ROW);
        }

        for (int col : DOOR_COLS) {
            for (int half = 0; half < 2; half++) {
                int id = classroomObjects[Classroom.DOOR_ROW][col + half];
                b.set(LAYER_OBJECTS, col + half, BASE_ROW, id);
                if (id == DOOR_LEFT || id == DOOR_RIGHT) {
                    b.set(LAYER_FLOOR, col + half, BASE_ROW, FLOOR); // The wall base is solid; a working door isn't
                }
            }
            // The floor below has its own (unreachable) doors in the same places
            b.set(LAYER_OBJECTS, col, BELOW_BASE_ROW, DOOR_LEFT);
            b.set(LAYER_OBJECTS, col + 1, BELOW_BASE_ROW, DOOR_RIGHT);
        }

        for (int col : windowCols) {
            b.set(LAYER_OBJECTS, col, WINDOW_ROW, WINDOW);
            b.set(LAYER_OBJECTS, col, BELOW_CAP_ROW + 2, WINDOW);
        }
        for (int col : curtainCols) {
            b.set(LAYER_OBJECTS, col, WINDOW_ROW, WINDOW_CURTAIN);
            b.set(LAYER_OBJECTS, col, BELOW_CAP_ROW + 2, WINDOW_CURTAIN);
        }
        return b.build(tiles, ts);
    }

    // A wall seen from the front: a top edge strip, two rows of wall and a base row
    static void wallFace(MapBuilder b, int capRow, int col0, int col1) {
        b.fill(LAYER_FLOOR, col0, capRow, col1, capRow, VOID);
        b.wallH(capRow, col0, col1);
        b.fill(LAYER_FLOOR, col0, capRow + 1, col1, capRow + 2, WALL);
        b.fill(LAYER_FLOOR, col0, capRow + 3, col1, capRow + 3, WALL_BASE);
    }

    // The walkable corridor floor
    static void corridor(MapBuilder b, int col0, int col1) {
        b.fill(LAYER_FLOOR, col0, CORRIDOR_TOP, col1, CORRIDOR_BOTTOM, FLOOR);
    }

    // The low wall along the open side of the corridor
    static void railing(MapBuilder b, int col0, int col1) {
        b.fill(LAYER_FLOOR, col0, RAIL_ROW, col1, RAIL_ROW, FLOOR);
        b.wallH(RAIL_ROW, col0, col1);
    }

    // The floor below, seen over the railing: the same wall and corridor, darkened
    static void floorBelow(MapBuilder b, int col0, int col1) {
        wallFace(b, BELOW_CAP_ROW, col0, col1);
        b.fill(LAYER_FLOOR, col0, BELOW_FLOOR_ROW, col1, BELOW_FLOOR_ROW, FLOOR);
        b.shade(col0, BELOW_CAP_ROW, col1, BELOW_FLOOR_ROW);
    }
}
