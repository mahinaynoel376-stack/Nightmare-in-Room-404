package nightmare404.map;

import static nightmare404.map.Hallway.*;
import static nightmare404.tile.TileMap.*;
import static nightmare404.tile.TileSet.*;

import java.awt.Point;
import java.awt.Rectangle;

import nightmare404.tile.MapBuilder;
import nightmare404.tile.Staircase;
import nightmare404.tile.TileMap;
import nightmare404.tile.TileSet;

// The stairwells at the two ends of each floor's hallway. Each one is a single screen, as wide as
// the hallway screens in front of the rooms (Hallway.WIDTH = 17 columns). The hallway comes in from
// one side (with its railing and the floor below), then opens into a walled stairwell. The stairwell
// rises higher than the hallway (its back wall starts at row 1, not 3), which leaves floor behind
// the staircase, so the staircase stands in the room as a box:
//
//   rows 1-4    the stairwell's back wall
//   rows 5-15   its floor (the corridor runs through rows 7-11)
//   row  6      the staircase's back balustrade
//   rows 7-8    the landings
//   rows 9-13   two flights of steps, 2 tiles wide each, with a divider between them
//
// West stairs, columns:  0 void | 1 outer wall | 2-12 stairwell (staircase 4-10) | 13 pillar | 14-16 hallway
// East stairs: the mirror image (hallway 0-2, pillar 3, stairwell 4-14 with the staircase at 6-12, outer wall 15).
// The staircase is 7 columns: a balustrade, the down flight (2), a divider, the up flight (2), a balustrade.
//
// The LEFT flight goes down a floor and the RIGHT flight goes up (see Staircase), at both ends.
// Each flight is one of:
//   OPEN       it works
//   BLOCKED    there's no floor that way (nothing above the top floor, no basement): rubble on its bottom step
//   COLLAPSED  it has caved in: holes and broken steps, so it can't be climbed
// One flight joins two floors, so a flight that has collapsed is collapsed from both ends.
// World decides which flights work (the way down alternates between the two ends of the building).
public class Stairs {
    public enum Side { WEST, EAST }
    public enum Flight { OPEN, BLOCKED, COLLAPSED }

    private static final int LAST = Hallway.WIDTH - 1;   // The last column (16)
    private static final int BOTTOM_WALL_ROW = 16;
    private static final int LANDING_TOP = 7;
    private static final int TOWER_CAP = 1;              // Top edge of the stairwell's back wall (the hallway's is row 3)
    private static final int FLIGHT_TOP = 9, FLIGHT_BOTTOM = 13;

    // A stairwell on an upper floor (the floor below is seen over the hallway's railing).
    // wallSign (optional): tile IDs of a sign to hang on the back wall above the stairs, left to right.
    // The "..." means you can pass any number of them, or none.
    public static TileMap build(TileSet tiles, int ts, Side side, Flight down, Flight up, int... wallSign) {
        return builder(ts, side, down, up, false, wallSign).build(tiles, ts);
    }

    // A ground-floor stairwell: no basement (the down flight is BLOCKED), the hallway's open side
    // looks out on grass, and there's an exit in the stairwell's bottom wall.
    // On the east side, a trail of footprints and slime leads from the stairs to the exit: whatever
    // escaped upstairs came down here and went outside.
    public static TileMap ground(TileSet tiles, int ts, Side side, Flight up, Ground.Exit exit) {
        MapBuilder b = builder(ts, side, Flight.BLOCKED, up, true);
        Ground.grassWhereVoid(b, CAP_ROW + 1, new Rectangle());   // Nothing inside the building is void
        int exitCol = side == Side.WEST ? 3 : LAST - 4;      // Columns 3-4 (west) or 12-13 (east)
        Ground.exit(b, exitCol, BOTTOM_WALL_ROW, exit);
        if (side == Side.EAST) {
            int[][] trail = {{8, 14}, {10, 14}, {11, 15}, {12, 15}};
            for (int i = 0; i < trail.length; i++) {
                b.set(LAYER_OBJECTS, trail[i][0], trail[i][1], i % 2 == 0 ? FOOTPRINTS : SLIME);
            }
        }
        return b.build(tiles, ts);
    }

    // The column a west-side column ends up in: the same on the west, mirrored on the east
    private static int mirror(Side side, int westCol) {
        return side == Side.WEST ? westCol : LAST - westCol;
    }

    // The down flight's left column: 5 on the west (staircase 4-10), 7 on the east (staircase 6-12)
    private static int staircaseLeft(Side side) {
        return side == Side.WEST ? 5 : LAST - 9;
    }

    private static MapBuilder builder(int ts, Side side, Flight down, Flight up, boolean ground, int... wallSign) {
        boolean west = side == Side.WEST;
        int outerWall = mirror(side, 1);     // The end of the building
        int innerWall = mirror(side, 13);    // Where the hallway meets the stairwell
        int leftWall = Math.min(outerWall, innerWall), rightWall = Math.max(outerWall, innerWall);
        int hall0 = west ? innerWall + 1 : 0, hall1 = west ? LAST : innerWall - 1;   // The hallway part

        MapBuilder b = new MapBuilder(Hallway.WIDTH, ROWS);
        b.fill(LAYER_FLOOR, 0, 0, LAST, ROWS - 1, VOID);

        // The hallway part keeps the hallway's wall (rows 3-6); the stairwell's back wall is higher
        // (rows 1-4), with floor in front of it (rows 5-6). The corridor runs through both.
        int inside0 = west ? outerWall + 1 : 0, inside1 = west ? LAST : outerWall - 1;
        int well0 = leftWall + 1, well1 = rightWall - 1;     // The stairwell's own columns
        wallFace(b, CAP_ROW, west ? innerWall : 0, west ? LAST : innerWall);
        wallFace(b, TOWER_CAP, well0, well1);
        b.fill(LAYER_FLOOR, well0, TOWER_CAP + 4, well1, CORRIDOR_TOP - 1, FLOOR);
        corridor(b, inside0, inside1);
        railing(b, hall0, hall1);
        if (!ground) floorBelow(b, hall0, hall1);   // On the ground floor there's grass there instead
        // Where the hallway meets the stairwell: a pillar, carried up to the stairwell's higher wall
        b.pillar(innerWall, CAP_ROW, BASE_ROW);
        if (west) {
            b.wallVLeft(LAYER_OBJECTS, innerWall, TOWER_CAP + 1, CAP_ROW - 1);
            b.set(LAYER_WALLS_H, innerWall, TOWER_CAP, WALL_CORNER_RIGHT);
        } else {
            b.wallVRight(LAYER_WALLS_V, innerWall, TOWER_CAP + 1, CAP_ROW - 1);
            b.set(LAYER_WALLS_H, innerWall, TOWER_CAP, WALL_CORNER_LEFT);
        }
        // The stairwell's inner wall, below the corridor
        if (west) {
            b.wallVLeft(LAYER_WALLS_V, innerWall, RAIL_ROW, BOTTOM_WALL_ROW);
        } else {
            b.wallVRight(LAYER_WALLS_V, innerWall, RAIL_ROW, BOTTOM_WALL_ROW);
        }

        stairwellWalls(b, leftWall, rightWall, west);
        b.set(LAYER_OBJECTS, mirror(side, 3), TOWER_CAP + 2, WINDOW);
        b.set(LAYER_OBJECTS, mirror(side, 11), TOWER_CAP + 2, WINDOW_CURTAIN);

        int left = staircaseLeft(side);
        staircase(b, left);

        // Where the stairs take you: the half of the landing above each flight. A flight that
        // doesn't work has no zone (null), so it can never take you anywhere.
        Rectangle downZone = down == Flight.OPEN ? new Rectangle(left * ts, LANDING_TOP * ts, 2 * ts, 2 * ts) : null;
        Rectangle upZone = up == Flight.OPEN ? new Rectangle((left + 3) * ts, LANDING_TOP * ts, 2 * ts, 2 * ts) : null;
        b.staircase(new Staircase(downZone, upZone, foot(left, ts), foot(left + 3, ts)));

        flight(b, left, down);
        flight(b, left + 3, up);
        if (down != Flight.OPEN && up != Flight.OPEN) {
            debris(b, left);   // Neither flight works: the whole stairwell is a ruin
        }
        for (int i = 0; i < wallSign.length; i++) {
            b.set(LAYER_OBJECTS, left + 1 + i, TOWER_CAP + 1, wallSign[i]);   // Centred above the staircase
        }
        return b;
    }

    // Where the player stands (x, y in pixels) at the bottom of the flight starting at column col:
    // centred on its 2 columns, on the floor row just below the last step
    private static Point foot(int col, int ts) {
        return new Point((col + 1) * ts - ts / 2, (FLIGHT_BOTTOM + 1) * ts);
    }

    // Makes the flight starting at column col match its state
    private static void flight(MapBuilder b, int col, Flight state) {
        switch (state) {
            case OPEN -> { }
            case BLOCKED -> {
                // Rubble across the bottom step: the only way onto a flight, so it can't be used
                b.set(LAYER_OBJECTS, col, FLIGHT_BOTTOM, RUBBLE);
                b.set(LAYER_OBJECTS, col + 1, FLIGHT_BOTTOM, RUBBLE);
            }
            case COLLAPSED -> {
                // Caved in: holes right across it, broken steps, and rubble on the bottom step.
                // The holes and cracks are see-through overlays, drawn over the staircase's own steps.
                b.set(LAYER_OBJECTS, col + 1, FLIGHT_TOP, STAIR_STEP_BROKEN);
                b.set(LAYER_OBJECTS, col, 10, STAIR_HOLE).set(LAYER_OBJECTS, col + 1, 10, STAIR_STEP_BROKEN);
                b.set(LAYER_OBJECTS, col, 11, STAIR_HOLE).set(LAYER_OBJECTS, col + 1, 11, STAIR_HOLE);
                b.set(LAYER_OBJECTS, col + 1, 12, STAIR_HOLE);
                b.set(LAYER_OBJECTS, col, FLIGHT_BOTTOM, STAIR_STEP_BROKEN);
                b.set(LAYER_OBJECTS, col + 1, FLIGHT_BOTTOM, RUBBLE);
            }
        }
    }

    // Rubble scattered over the landing, the steps and the stairwell floor (rubble is solid).
    // Positions are relative to the staircase, and keep a way clear around it (to the ground floor's exit).
    private static void debris(MapBuilder b, int left) {
        int[][] rubble = {{1, 7}, {3, 8}, {0, 9}, {4, 13}, {-2, 10}, {6, 12}, {6, 9}, {-3, 15}, {2, 15}};
        for (int[] cell : rubble) {
            b.set(LAYER_OBJECTS, left + cell[0], cell[1], RUBBLE);
        }
    }

    // Floor below the corridor, the bottom wall and the outer side wall of a stairwell
    // that spans columns leftWall to rightWall (both are wall columns).
    // outerWallOnLeft: true if the end of the building is on the left (west stairs), false if on the right
    private static void stairwellWalls(MapBuilder b, int leftWall, int rightWall, boolean outerWallOnLeft) {
        b.fill(LAYER_FLOOR, leftWall + 1, RAIL_ROW, rightWall - 1, BOTTOM_WALL_ROW, FLOOR);
        b.wallH(BOTTOM_WALL_ROW, leftWall + 1, rightWall - 1);
        b.set(LAYER_WALLS_H, leftWall, BOTTOM_WALL_ROW, WALL_CORNER_LEFT);
        b.set(LAYER_WALLS_H, rightWall, BOTTOM_WALL_ROW, WALL_CORNER_RIGHT);

        // The outer wall at the end of the building, from the top edge down to the bottom wall
        if (outerWallOnLeft) {
            b.set(LAYER_WALLS_H, leftWall, TOWER_CAP, WALL_CORNER_LEFT);
            b.wallVRight(LAYER_WALLS_V, leftWall, TOWER_CAP + 1, BOTTOM_WALL_ROW);
        } else {
            b.set(LAYER_WALLS_H, rightWall, TOWER_CAP, WALL_CORNER_RIGHT);
            b.wallVLeft(LAYER_WALLS_V, rightWall, TOWER_CAP + 1, BOTTOM_WALL_ROW);
        }
    }

    // The staircase: one 7 x 8 picture (TileSet.STAIRCASE_FIRST), a box from the column left of the
    // down flight to the column right of the up flight: its back balustrade (row 6), the landings
    // (rows 7-8) and the flights (rows 9-13):
    //
    //   [               back balustrade                                 ]
    //   [balustrade][down flight: a pit][divider][up flight: rising][balustrade]
    //
    // The balustrades and the divider are solid, so the stairs can only be entered from one end:
    // the bottom of the flights. left = the down flight's left column
    private static void staircase(MapBuilder b, int left) {
        for (int row = 0; row < STAIRCASE_HEIGHT; row++) {
            for (int col = 0; col < STAIRCASE_WIDTH; col++) {
                b.set(LAYER_FLOOR, left - 1 + col, LANDING_TOP - 1 + row, STAIRCASE_FIRST + row * STAIRCASE_WIDTH + col);
            }
        }
    }
}
