package nightmare404.map;

import static nightmare404.tile.TileMap.*;
import static nightmare404.tile.TileSet.*;

import nightmare404.tile.MapBuilder;
import nightmare404.tile.TileMap;
import nightmare404.tile.TileSet;

// The hallway on each floor: an open corridor running along the bottom of the rooms
// (the classrooms on Floor 4, the library on Floor 3, the labs on Floor 2).
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

    // The hallway screen in front of one classroom (Floor 4).
    // Its doors copy the classroom's bottom wall: same columns, broken in the same places.
    public static TileMap outside(TileSet tiles, int ts, TileMap classroom, int[] windowCols, int[] curtainCols) {
        MapBuilder b = corridorScreen(false);
        for (int col : PILLAR_COLS) {
            b.pillar(col, CAP_ROW, BASE_ROW);
        }
        copyDoors(b, tiles, classroom);
        windows(b, windowCols, curtainCols, false);
        return b.build(tiles, ts);
    }

    // The hallway screen in front of a one-screen room with a name plaque (the labs and the offices):
    // pillars between rooms, the room's doors, two windows, and the plaque in the middle of the wall.
    // ground: true on the ground floor, where the corridor looks out on grass instead of the floor below.
    private static MapBuilder namedRoomFront(TileSet tiles, TileMap room, int sign, boolean ground) {
        MapBuilder b = corridorScreen(ground);
        for (int col : PILLAR_COLS) {
            b.pillar(col, CAP_ROW, BASE_ROW);
        }
        copyDoors(b, tiles, room);
        windows(b, new int[] {8, 15}, new int[] {}, ground);
        for (int i = 0; i < SIGN_WIDTH; i++) {
            b.set(LAYER_OBJECTS, 11 + i, CAP_ROW + 1, sign + i);   // Plaque above the middle of the wall
        }
        return b;
    }

    // The hallway screen in front of one office (Floor 1, the ground floor)
    public static TileMap officeFront(TileSet tiles, int ts, TileMap office, Office.Part part) {
        MapBuilder b = namedRoomFront(tiles, office, Office.sign(part), true);
        switch (part) {
            case FACULTY -> b.set(LAYER_OBJECTS, 10, 9, BLOOD_SPLAT);
            case PRINCIPAL -> b.set(LAYER_OBJECTS, 17, WINDOW_ROW, FIRE_ALARM);
            case RECORDS -> {
                b.set(LAYER_OBJECTS, 5, WINDOW_ROW, SCRATCHES);       // Claw marks over the broken door
                b.set(LAYER_OBJECTS, 14, 10, CRACK);
            }
        }
        return b.build(tiles, ts);
    }

    // The hallway screen in front of one lab (Floor 2): like a classroom's, plus a name plaque
    // in the middle of the wall and some signs of what happened.
    public static TileMap labFront(TileSet tiles, int ts, TileMap lab, Lab.Part part) {
        MapBuilder b = namedRoomFront(tiles, lab, Lab.sign(part), false);
        switch (part) {
            case CHEMISTRY -> {
                b.set(LAYER_OBJECTS, 18, WINDOW_ROW, HAZARD_DECAL);  // Warning sign over the sealed door
                b.set(LAYER_OBJECTS, 10, 9, TOXIC_PUDDLE);
            }
            case BIOLOGY -> {
                b.set(LAYER_OBJECTS, 17, WINDOW_ROW, HANDPRINT);
                b.set(LAYER_OBJECTS, 13, 10, FOOTPRINTS);
            }
            case CONTAINMENT -> {
                b.set(LAYER_OBJECTS, 5, WINDOW_ROW, HAZARD_DECAL);   // Warning sign over the sealed door
                // The trail from the smashed pod carries on out of the door and along the corridor,
                // towards the collapsed east stairs
                int[][] trail = {{18, 7}, {19, 8}, {20, 8}, {21, 9}, {22, 9}, {23, 10}, {24, 10}};
                for (int i = 0; i < trail.length; i++) {
                    b.set(LAYER_OBJECTS, trail[i][0], trail[i][1], i % 2 == 0 ? FOOTPRINTS : SLIME);
                }
            }
        }
        return b.build(tiles, ts);
    }

    // Copies a room's doors (from its bottom wall) onto this hallway's wall, in the same columns.
    // A working door also needs walkable floor under it, because the wall base is solid.
    private static void copyDoors(MapBuilder b, TileSet tiles, TileMap room) {
        for (int col : DOOR_COLS) {
            for (int half = 0; half < 2; half++) {
                int id = room.tileAt(LAYER_OBJECTS, col + half, Classroom.DOOR_ROW);
                if (id == EMPTY) continue;
                b.set(LAYER_OBJECTS, col + half, BASE_ROW, id);
                if (!tiles.isSolid(id)) {
                    b.set(LAYER_FLOOR, col + half, BASE_ROW, FLOOR);
                }
            }
        }
    }

    // The hallway screen in front of one third of the library (Floor 3).
    // The library is one long room, so there are pillars only at its two ends,
    // and the only way in is the big double door on the middle screen.
    public static TileMap libraryFront(TileSet tiles, int ts, Library.Part part) {
        MapBuilder b = corridorScreen(false);
        switch (part) {
            case WEST -> {
                b.pillar(2, CAP_ROW, BASE_ROW);                        // The library's west end
                windows(b, new int[] {6, 10, 14, 18}, new int[] {}, false);
                b.set(LAYER_OBJECTS, 3, CAP_ROW + 1, COBWEB_LEFT);
                b.set(LAYER_OBJECTS, 8, CAP_ROW + 1, SCRATCHES);        // Claw marks on the wall...
                b.set(LAYER_OBJECTS, 15, 9, BLOOD_SPLAT);               // ...and blood on the floor
            }
            case ENTRANCE -> {
                b.bigDoor(10, WINDOW_ROW, FLOOR);                       // The only way into the library
                b.set(LAYER_OBJECTS, 11, CAP_ROW + 1, LIBRARY_PLAQUE_L);
                b.set(LAYER_OBJECTS, 12, CAP_ROW + 1, LIBRARY_PLAQUE_R);
                windows(b, new int[] {5, 19}, new int[] {}, false);
                b.set(LAYER_OBJECTS, 15, WINDOW_ROW, HANDPRINT);        // A bloody handprint beside the door
                b.set(LAYER_OBJECTS, 7, 10, CRACK);
            }
            case EAST -> {
                b.pillar(22, CAP_ROW, BASE_ROW);                       // The library's east end
                windows(b, new int[] {6, 10, 14, 18}, new int[] {}, false);
                b.set(LAYER_OBJECTS, 21, CAP_ROW + 1, COBWEB_RIGHT);
                b.set(LAYER_OBJECTS, 9, 8, BLOOD_DRIPS);
            }
        }
        return b.build(tiles, ts);
    }

    // Everything every hallway screen has: the front wall, the corridor, the low wall, and what's
    // seen beyond it: the floor below (with its own classroom pillars and doors) on the upper floors,
    // or grass on the ground floor. Each kind of hallway screen then adds its own doors, windows
    // and pillars on top.
    private static MapBuilder corridorScreen(boolean ground) {
        MapBuilder b = new MapBuilder();
        b.fill(LAYER_FLOOR, 0, 0, 24, 17, VOID);

        wallFace(b, CAP_ROW, 0, 24);
        corridor(b, 0, 24);
        railing(b, 0, 24);
        if (ground) {
            outsideGrass(b, 0, 24);
            return b;
        }
        floorBelow(b, 0, 24);

        for (int col : PILLAR_COLS) {
            b.pillar(col, BELOW_CAP_ROW, BELOW_BASE_ROW);
        }
        // The floor below has its own (unreachable) doors
        for (int col : DOOR_COLS) {
            b.set(LAYER_OBJECTS, col, BELOW_BASE_ROW, DOOR_LEFT);
            b.set(LAYER_OBJECTS, col + 1, BELOW_BASE_ROW, DOOR_RIGHT);
        }
        return b;
    }

    // Ground floor: grass beyond the corridor's low wall, darker just below it where its shadow falls.
    // The low wall stays, so the player can't step out onto the grass from the corridor.
    static void outsideGrass(MapBuilder b, int col0, int col1) {
        for (int col = col0; col <= col1; col++) {
            b.set(LAYER_FLOOR, col, BELOW_CAP_ROW, GRASS_EDGE);
            for (int row = BELOW_CAP_ROW + 1; row <= BELOW_FLOOR_ROW; row++) {
                b.set(LAYER_FLOOR, col, row, Ground.grass(col, row));
            }
        }
    }

    // Windows on the front wall, and (except on the ground floor) the same windows on the floor below
    private static void windows(MapBuilder b, int[] windowCols, int[] curtainCols, boolean ground) {
        for (int col : windowCols) {
            b.set(LAYER_OBJECTS, col, WINDOW_ROW, WINDOW);
            if (!ground) b.set(LAYER_OBJECTS, col, BELOW_CAP_ROW + 2, WINDOW);
        }
        for (int col : curtainCols) {
            b.set(LAYER_OBJECTS, col, WINDOW_ROW, WINDOW_CURTAIN);
            if (!ground) b.set(LAYER_OBJECTS, col, BELOW_CAP_ROW + 2, WINDOW_CURTAIN);
        }
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

    // The floor below, seen over the railing, darkened. Rows 13-17 here line up with rows 3-7 of the
    // screen directly underneath (its wall, doors and the edge of its corridor), so they're a VIEW
    // of those rows: World.showFloorsBelow copies the real tiles in once every floor exists.
    // Until then (or if there's nothing below yet) a plain wall and corridor stand in.
    static void floorBelow(MapBuilder b, int col0, int col1) {
        wallFace(b, BELOW_CAP_ROW, col0, col1);
        b.fill(LAYER_FLOOR, col0, BELOW_FLOOR_ROW, col1, BELOW_FLOOR_ROW, FLOOR);
        b.shade(col0, BELOW_CAP_ROW, col1, BELOW_FLOOR_ROW);
        b.view(col0, BELOW_CAP_ROW, col1, BELOW_FLOOR_ROW, CAP_ROW);
    }
}
