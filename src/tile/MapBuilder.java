package tile;

import static tile.TileMap.*;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Helper for building a TileMap step by step: start empty, place tiles, then call build().
// All positions are in tiles (col 0-24, row 0-17), and ranges include both ends.
public class MapBuilder {
    private final int[][][] layers = new int[LAYERS][ROWS][COLS];
    private final List<Rectangle> shadeTiles = new ArrayList<>();
    private final List<int[]> edgeWalls = new ArrayList<>();   // {col, row0, row1}

    public MapBuilder() {
        for (int[][] layer : layers) {
            for (int[] row : layer) {
                Arrays.fill(row, EMPTY);
            }
        }
    }

    // Copies a whole hand-written 25 x 18 grid into a layer (-1 cells are left empty)
    public MapBuilder copy(int layer, int[][] grid) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                layers[layer][row][col] = grid[row][col];
            }
        }
        return this;
    }

    public MapBuilder set(int layer, int col, int row, int id) {
        layers[layer][row][col] = id;
        return this;
    }

    // Fills the rectangle from (col0, row0) to (col1, row1) with one tile
    public MapBuilder fill(int layer, int col0, int row0, int col1, int row1, int id) {
        for (int row = row0; row <= row1; row++) {
            for (int col = col0; col <= col1; col++) {
                layers[layer][row][col] = id;
            }
        }
        return this;
    }

    // A horizontal wall strip along one row. The 3 wall_h tiles repeat by column (col % 3), like the
    // classroom arrays, so strips line up across neighbouring screens
    public MapBuilder wallH(int row, int col0, int col1) {
        int[] cycle = {TileSet.WALL_H_1, TileSet.WALL_H_2, TileSet.WALL_H_3};
        for (int col = col0; col <= col1; col++) {
            layers[LAYER_WALLS_H][row][col] = cycle[col % 3];
        }
        return this;
    }

    // A vertical wall strip on the LEFT half of the tiles: the right-hand wall of a room
    public MapBuilder wallVLeft(int layer, int col, int row0, int row1) {
        int[] cycle = {TileSet.WALL_V_LEFT_1, TileSet.WALL_V_LEFT_2, TileSet.WALL_V_LEFT_3};
        for (int row = row0; row <= row1; row++) {
            layers[layer][row][col] = cycle[row % 3];
        }
        return this;
    }

    // A vertical wall strip on the RIGHT half of the tiles: the left-hand wall of a room
    public MapBuilder wallVRight(int layer, int col, int row0, int row1) {
        int[] cycle = {TileSet.WALL_V_RIGHT_1, TileSet.WALL_V_RIGHT_2, TileSet.WALL_V_RIGHT_3};
        for (int row = row0; row <= row1; row++) {
            layers[layer][row][col] = cycle[row % 3];
        }
        return this;
    }

    // Two wall strips back to back in one column: where two rooms' walls meet
    public MapBuilder pillar(int col, int row0, int row1) {
        wallVRight(LAYER_WALLS_V, col, row0, row1);
        return wallVLeft(LAYER_OBJECTS, col, row0, row1);
    }

    // Darkens a rectangle of tiles (used for the floor below, seen over the railing)
    public MapBuilder shade(int col0, int row0, int col1, int row1) {
        shadeTiles.add(new Rectangle(col0, row0, col1 - col0 + 1, row1 - row0 + 1));
        return this;
    }

    // An invisible wall along the LEFT edge of column col, from row0 to row1.
    // It stops the player crossing between two cells without making either cell solid,
    // e.g. so the stairs can be walked on but not stepped onto from the side.
    public MapBuilder edgeWall(int col, int row0, int row1) {
        edgeWalls.add(new int[] {col, row0, row1});
        return this;
    }

    public TileMap build(TileSet tiles, int ts) {
        List<Rectangle> shade = new ArrayList<>();
        for (Rectangle r : shadeTiles) {
            shade.add(new Rectangle(r.x * ts, r.y * ts, r.width * ts, r.height * ts));
        }
        // Each edge wall is 4 px thick, centred on the line between the two columns
        List<Rectangle> walls = new ArrayList<>();
        for (int[] w : edgeWalls) {
            walls.add(new Rectangle(w[0] * ts - 2, w[1] * ts, 4, (w[2] - w[1] + 1) * ts));
        }
        return new TileMap(tiles, ts, layers, shade, walls);
    }
}
