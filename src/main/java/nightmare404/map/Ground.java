package nightmare404.map;

import static nightmare404.tile.TileMap.*;
import static nightmare404.tile.TileSet.*;

import java.awt.Rectangle;

import nightmare404.tile.MapBuilder;

// The ground floor's outdoors. On the upper floors, looking over the hallway's low wall shows the
// floor below; on the ground floor there's nothing below, so it shows grass instead. The building
// can be left through an exit at each end, each one flanked by two pillars that hold up the floors above.
public class Ground {
    // An exit is either a way out, or the same doorway boarded up
    public enum Exit { OPEN, BLOCKED }

    // Grass, mixing its two tiles in a scattered pattern so it doesn't look like a grid
    static int grass(int col, int row) {
        return (col * 5 + row * 3) % 4 == 0 ? GRASS_2 : GRASS_1;
    }

    // Grass in every cell from fromRow down that is still void (outside the building).
    // keep: a rectangle of tiles (in tile units) that must stay void, like the drop between the
    // stair flights. Rows above fromRow are left alone: above the back wall is never shown.
    static void grassWhereVoid(MapBuilder b, int fromRow, Rectangle keep) {
        for (int row = fromRow; row < b.rows(); row++) {
            for (int col = 0; col < b.cols(); col++) {
                if (b.get(LAYER_FLOOR, col, row) == VOID && !keep.contains(col, row)) {
                    b.set(LAYER_FLOOR, col, row, grass(col, row));
                }
            }
        }
    }

    // An exit in a bottom wall: a 2-tile doorway with its left half at (col, row), a support pillar
    // (2 tiles tall) on each side, and a stone path just outside.
    // An OPEN exit is a door like any other (TileSet.doorSize knows EXIT_L), so whatever is outside
    // can be linked to this room the usual way. A BLOCKED one is boarded up and solid.
    static void exit(MapBuilder b, int col, int row, Exit kind) {
        int first = kind == Exit.OPEN ? EXIT_L : EXIT_BLOCKED_L;
        for (int half = 0; half < 2; half++) {
            b.set(LAYER_WALLS_H, col + half, row, EMPTY);           // Open up the wall here
            b.set(LAYER_OBJECTS, col + half, row, first + half);
            b.set(LAYER_FLOOR, col + half, row + 1, G_FLOOR_1);      // The path outside
        }
        for (int pillarCol : new int[] {col - 1, col + 2}) {
            b.set(LAYER_OBJECTS, pillarCol, row, PILLAR_TOP);
            b.set(LAYER_OBJECTS, pillarCol, row + 1, PILLAR_BOTTOM);
        }
    }
}
