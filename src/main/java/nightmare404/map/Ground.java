package nightmare404.map;

import static nightmare404.tile.TileMap.*;
import static nightmare404.tile.TileSet.*;

import java.awt.Rectangle;

import nightmare404.tile.MapBuilder;

// The ground floor's outdoors. On the upper floors, looking over the hallway's low wall shows the
// floor below; on the ground floor there's nothing below, so it shows grass instead. Each stairwell
// has a side exit in the middle of its bottom wall, boarded up, between two pillars that hold up the
// floors above (the way out is the main entrance, see Hallway.mainEntrance).
public class Ground {
    // Grass, mixing its two tiles in a scattered pattern so it doesn't look like a grid
    static int grass(int col, int row) {
        if ((col * 5 + row * 3) % 4 == 0) return GRASS_2;
        return GRASS_1;
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

    // A boarded-up exit in a bottom wall, centred on column `middle`: a 3-tile doorway (solid) with a
    // support pillar (2 tiles tall) on each side, and a stone path just outside. It's 3 tiles wide so
    // it can sit exactly in the middle of a stairwell, which is an odd number of tiles wide.
    static void boardedExit(MapBuilder b, int middle, int row) {
        int[] boards = {EXIT_BLOCKED_L, EXIT_BLOCKED_M, EXIT_BLOCKED_R};
        for (int i = 0; i < boards.length; i++) {
            int col = middle - 1 + i;
            b.set(LAYER_WALLS_H, col, row, EMPTY);                  // Open up the wall here
            b.set(LAYER_OBJECTS, col, row, boards[i]);
            b.set(LAYER_FLOOR, col, row + 1, G_FLOOR_1);             // The path outside
        }
        for (int pillarCol : new int[] {middle - 2, middle + 2}) {
            b.set(LAYER_OBJECTS, pillarCol, row, PILLAR_TOP);
            b.set(LAYER_OBJECTS, pillarCol, row + 1, PILLAR_BOTTOM);
        }
    }
}
