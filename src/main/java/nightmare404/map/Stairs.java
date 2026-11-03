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

// The stairwells at the two ends of each floor's hallway.
// Each screen: the hallway comes in from one side (with its railing and the floor below),
// then opens into a walled stairwell (rows 7-15) with a staircase in the middle:
//
//   rows 7-8    the landing (5 tiles wide)
//   rows 9-13   two flights of steps, 2 tiles wide each, with a rail between them
//
// West stairs: working. The LEFT flight goes down a floor, the RIGHT flight goes up (see Staircase).
// East stairs: collapsed and unusable.
public class Stairs {
    private static final int BOTTOM_WALL_ROW = 16;
    private static final int LANDING_TOP = 7, LANDING_BOTTOM = 8;
    private static final int FLIGHT_TOP = 9, FLIGHT_BOTTOM = 13;
    private static final int WEST_LEFT = 9;    // Leftmost column of the west staircase
    private static final int EAST_LEFT = 11;   // ...and of the east one

    // Left end of the hallway. The hallway comes in from the right; the outer wall is column 2.
    // leadsDown / leadsUp: whether there's a floor below / above to go to. A flight that leads
    // nowhere yet is blocked with rubble, so the player can't climb it.
    // wallSign (optional): tile IDs of a sign to hang on the back wall above the stairs, left to right.
    // The "..." means you can pass any number of them, or none (Floors 3 and 4 have no sign).
    public static TileMap west(TileSet tiles, int ts, boolean leadsDown, boolean leadsUp, int... wallSign) {
        return westBuilder(ts, leadsDown, leadsUp, false, wallSign).build(tiles, ts);
    }

    // The ground floor's west stairs: they only go up, the hallway's open side looks out on grass,
    // and there's an exit in the stairwell's bottom wall (columns 5-6)
    public static TileMap westGround(TileSet tiles, int ts, Ground.Exit exit) {
        MapBuilder b = westBuilder(ts, false, true, true);
        Ground.grassWhereVoid(b, CAP_ROW + 1, new Rectangle(WEST_LEFT + 2, FLIGHT_TOP, 1, FLIGHT_BOTTOM - FLIGHT_TOP + 1));
        Ground.exit(b, 5, BOTTOM_WALL_ROW, exit);
        return b.build(tiles, ts);
    }

    private static MapBuilder westBuilder(int ts, boolean leadsDown, boolean leadsUp, boolean ground, int... wallSign) {
        MapBuilder b = new MapBuilder();
        b.fill(LAYER_FLOOR, 0, 0, 24, 17, VOID);

        // Hallway part (columns 22-24) and the stairwell share the back wall and the corridor floor
        wallFace(b, CAP_ROW, 3, 24);
        corridor(b, 3, 24);
        railing(b, 22, 24);
        if (!ground) floorBelow(b, 22, 24);   // On the ground floor there's grass there instead
        b.pillar(21, CAP_ROW, BASE_ROW);                       // Where the hallway meets the stairwell
        b.wallVLeft(LAYER_WALLS_V, 21, RAIL_ROW, BOTTOM_WALL_ROW); // Stairwell's right wall, below the corridor

        stairwellWalls(b, 2, 21, true);
        b.set(LAYER_OBJECTS, 6, WINDOW_ROW, WINDOW);
        b.set(LAYER_OBJECTS, 16, WINDOW_ROW, WINDOW_CURTAIN);

        int left = WEST_LEFT;
        staircase(b, left);

        // Where the stairs take you: the half of the landing above each flight
        Rectangle downZone = new Rectangle(left * ts, LANDING_TOP * ts, 2 * ts, 2 * ts);
        Rectangle upZone = new Rectangle((left + 3) * ts, LANDING_TOP * ts, 2 * ts, 2 * ts);
        b.staircase(new Staircase(downZone, upZone, foot(left, ts), foot(left + 3, ts)));

        if (!leadsDown) blockFlight(b, left);
        if (!leadsUp) blockFlight(b, left + 3);
        for (int i = 0; i < wallSign.length; i++) {
            b.set(LAYER_OBJECTS, left + 1 + i, CAP_ROW + 1, wallSign[i]);   // Centred above the staircase
        }
        return b;
    }

    // Where the player stands (x, y in pixels) at the bottom of the flight starting at column col:
    // centred on its 2 columns, on the floor row just below the last step
    private static Point foot(int col, int ts) {
        return new Point((col + 1) * ts - ts / 2, (FLIGHT_BOTTOM + 1) * ts);
    }

    // Rubble across the bottom step: the only way onto a flight, so the flight can't be used
    private static void blockFlight(MapBuilder b, int col) {
        b.set(LAYER_OBJECTS, col, FLIGHT_BOTTOM, RUBBLE);
        b.set(LAYER_OBJECTS, col + 1, FLIGHT_BOTTOM, RUBBLE);
    }

    // Right end of the hallway, next to Room 404. The hallway comes in from the left; the outer wall is column 22.
    // The staircase has collapsed: holes, broken steps and rubble make it impossible to use.
    public static TileMap eastDestroyed(TileSet tiles, int ts) {
        return eastBuilder(false).build(tiles, ts);
    }

    // The ground floor's east stairs: the bottom of the collapse, grass on the hallway's open side,
    // and an exit in the stairwell's bottom wall (columns 18-19). A trail of footprints and slime
    // leads from the rubble to that exit: whatever escaped upstairs came down here and went outside.
    public static TileMap eastGround(TileSet tiles, int ts, Ground.Exit exit) {
        MapBuilder b = eastBuilder(true);
        Ground.grassWhereVoid(b, CAP_ROW + 1, new Rectangle(EAST_LEFT + 2, FLIGHT_TOP, 1, FLIGHT_BOTTOM - FLIGHT_TOP + 1));
        Ground.exit(b, 18, BOTTOM_WALL_ROW, exit);
        int[][] trail = {{12, 14}, {14, 14}, {16, 15}, {18, 15}};
        for (int i = 0; i < trail.length; i++) {
            b.set(LAYER_OBJECTS, trail[i][0], trail[i][1], i % 2 == 0 ? FOOTPRINTS : SLIME);
        }
        return b.build(tiles, ts);
    }

    private static MapBuilder eastBuilder(boolean ground) {
        MapBuilder b = new MapBuilder();
        b.fill(LAYER_FLOOR, 0, 0, 24, 17, VOID);

        wallFace(b, CAP_ROW, 0, 21);
        corridor(b, 0, 21);
        railing(b, 0, 2);
        if (!ground) floorBelow(b, 0, 2);
        b.pillar(3, CAP_ROW, BASE_ROW);
        b.wallVRight(LAYER_WALLS_V, 3, RAIL_ROW, BOTTOM_WALL_ROW); // Stairwell's left wall, below the corridor

        stairwellWalls(b, 3, 22, false);
        b.set(LAYER_OBJECTS, 8, WINDOW_ROW, WINDOW_CURTAIN);
        b.set(LAYER_OBJECTS, 18, WINDOW_ROW, WINDOW);

        int left = EAST_LEFT;
        staircase(b, left);

        // Each flight has caved in across its whole width, so there's no way down
        b.set(LAYER_FLOOR, left, 10, STAIR_HOLE).set(LAYER_FLOOR, left + 1, 10, STAIR_HOLE);
        b.set(LAYER_FLOOR, left, 11, STAIR_HOLE).set(LAYER_FLOOR, left + 1, 11, STAIR_STEP_BROKEN);
        b.set(LAYER_FLOOR, left + 3, 11, STAIR_HOLE).set(LAYER_FLOOR, left + 4, 11, STAIR_HOLE);
        b.set(LAYER_FLOOR, left + 3, 12, STAIR_STEP_BROKEN).set(LAYER_FLOOR, left + 4, 12, STAIR_HOLE);
        b.set(LAYER_FLOOR, left + 3, 9, STAIR_STEP_BROKEN);
        b.set(LAYER_FLOOR, left, 13, STAIR_STEP_BROKEN);

        // Rubble on the landing, the steps and the stairwell floor (rubble is solid)
        int[][] rubble = {{12, 7}, {14, 8}, {11, 9}, {15, 13}, {9, 10}, {17, 12}, {16, 8}, {8, 14}, {13, 15}};
        for (int[] cell : rubble) {
            b.set(LAYER_OBJECTS, cell[0], cell[1], RUBBLE);
        }
        return b;
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
            b.set(LAYER_WALLS_H, leftWall, CAP_ROW, WALL_CORNER_LEFT);
            b.wallVRight(LAYER_WALLS_V, leftWall, CAP_ROW + 1, BOTTOM_WALL_ROW);
        } else {
            b.set(LAYER_WALLS_H, rightWall, CAP_ROW, WALL_CORNER_RIGHT);
            b.wallVLeft(LAYER_WALLS_V, rightWall, CAP_ROW + 1, BOTTOM_WALL_ROW);
        }
    }

    // The staircase: a 5-wide landing on top, then two flights of steps with a rail between them.
    // left = the staircase's leftmost column
    private static void staircase(MapBuilder b, int left) {
        b.fill(LAYER_FLOOR, left, LANDING_TOP, left + 4, LANDING_BOTTOM, STAIR_LANDING);
        b.fill(LAYER_FLOOR, left, FLIGHT_TOP, left + 1, FLIGHT_BOTTOM, STAIR_STEP);
        b.fill(LAYER_FLOOR, left + 3, FLIGHT_TOP, left + 4, FLIGHT_BOTTOM, STAIR_STEP);
        // The gap between the flights: a drop (void) with a stone rail along it
        b.fill(LAYER_FLOOR, left + 2, FLIGHT_TOP, left + 2, FLIGHT_BOTTOM, VOID);
        b.fill(LAYER_OBJECTS, left + 2, FLIGHT_TOP, left + 2, FLIGHT_BOTTOM, STAIR_RAIL);

        // The stairs can only be entered from one end: the bottom of the flights.
        // The landing's top is the back wall and the rail blocks the middle, so invisible walls
        // along the outer left and right sides close off everything else.
        b.edgeWall(left, LANDING_TOP, FLIGHT_BOTTOM);
        b.edgeWall(left + 5, LANDING_TOP, FLIGHT_BOTTOM);
    }
}
