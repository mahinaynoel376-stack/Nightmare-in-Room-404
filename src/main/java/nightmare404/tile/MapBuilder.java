package nightmare404.tile;

import static nightmare404.tile.TileMap.*;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// Helper for building a TileMap step by step: start empty, place tiles, then call build().
// All positions are in tiles, and ranges include both ends. A map is one screen (25 x 18 tiles)
// unless you ask for another size, like the quadrangle (50 x 36: four screens' worth).
public class MapBuilder {
    private final int cols, rows;
    private final int[][][] layers;
    private final List<Rectangle> shadeTiles = new ArrayList<>();
    private final List<int[]> edgeWalls = new ArrayList<>();    // {col, row0, row1}: along a column's left edge
    private final List<int[]> edgeWallsH = new ArrayList<>();   // {row, col0, col1}: along a row's top edge
    private Staircase staircase;                                // null = no working stairs on this map
    private final List<TileMap.View> views = new ArrayList<>();

    // One screen: 25 x 18 tiles
    public MapBuilder() {
        this(COLS, ROWS);
    }

    public MapBuilder(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        layers = new int[LAYERS][rows][cols];
        for (int[][] layer : layers) {
            for (int[] row : layer) {
                Arrays.fill(row, EMPTY);
            }
        }
    }

    public int cols() {
        return cols;
    }

    public int rows() {
        return rows;
    }

    // Copies a whole hand-written grid (the same size as the map) into a layer (-1 cells are left empty)
    public MapBuilder copy(int layer, int[][] grid) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                layers[layer][row][col] = grid[row][col];
            }
        }
        return this;
    }

    public MapBuilder set(int layer, int col, int row, int id) {
        layers[layer][row][col] = id;
        return this;
    }

    // The tile currently at a cell (EMPTY if nothing has been placed there)
    public int get(int layer, int col, int row) {
        return layers[layer][row][col];
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

    // The same, lying flat: an invisible wall along the TOP edge of row `row`, from col0 to col1
    // (e.g. along the far side of the bus roof, so it can't be climbed onto from there)
    public MapBuilder edgeWallH(int row, int col0, int col1) {
        edgeWallsH.add(new int[] {row, col0, col1});
        return this;
    }

    // A big double door, 4 tiles wide and 2 tall, with its top-left corner at (col, row).
    // The cells under it get floorUnder (a walkable tile) and any wall strip there is removed,
    // so the player can walk into the door, which is how you go through it.
    public MapBuilder bigDoor(int col, int row, int floorUnder) {
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 4; c++) {
                layers[LAYER_FLOOR][row + r][col + c] = floorUnder;
                layers[LAYER_WALLS_H][row + r][col + c] = EMPTY;
                layers[LAYER_OBJECTS][row + r][col + c] = TileSet.BIG_DOOR_TL + r * 4 + c;
            }
        }
        return this;
    }

    // Decides which tile goes UNDER each character of a character map (e.g. wall or floor, by row)
    public interface Underlay {
        int tileAt(int col, int row);
    }

    // Paints a CHARACTER MAP: one string per row, one character per tile (18 strings of 25 for a screen).
    // For every character except ' ' (void, left as it is):
    //   1. the floor layer gets underlay.tileAt(col, row)
    //   2. the objects layer gets legend.get(character), unless the legend says EMPTY
    // A wrong row length or a character that isn't in the legend stops the game with a clear message.
    public MapBuilder paint(String[] map, Map<Character, Integer> legend, Underlay underlay) {
        if (map.length != rows) {
            throw new IllegalArgumentException("A character map needs " + rows + " rows, not " + map.length);
        }
        for (int row = 0; row < rows; row++) {
            if (map[row].length() != cols) {
                throw new IllegalArgumentException("Character map row " + row + " has "
                        + map[row].length() + " characters, not " + cols + ": \"" + map[row] + "\"");
            }
            for (int col = 0; col < cols; col++) {
                char ch = map[row].charAt(col);
                if (ch == ' ') continue;
                Integer object = legend.get(ch);
                if (object == null) {
                    throw new IllegalArgumentException("Unknown map character '" + ch + "' at column "
                            + col + ", row " + row);
                }
                layers[LAYER_FLOOR][row][col] = underlay.tileAt(col, row);
                if (object != EMPTY) {
                    layers[LAYER_OBJECTS][row][col] = object;
                }
            }
        }
        return this;
    }

    // Marks a rectangle as a VIEW of another map (see TileMap.View): scenery that will show that
    // map's rows from sourceRow0 down. Whatever is placed here now is only a stand-in until then.
    public MapBuilder view(int col0, int row0, int col1, int row1, int sourceRow0) {
        views.add(new TileMap.View(col0, row0, col1, row1, sourceRow0));
        return this;
    }

    // Marks this map as having working stairs (see Staircase)
    public MapBuilder staircase(Staircase stairs) {
        this.staircase = stairs;
        return this;
    }

    public TileMap build(TileSet tiles, int ts) {
        List<Rectangle> shade = new ArrayList<>();
        for (Rectangle r : shadeTiles) {
            shade.add(new Rectangle(r.x * ts, r.y * ts, r.width * ts, r.height * ts));
        }
        // Each edge wall is 4 px thick, centred on the line between the two columns (or rows)
        List<Rectangle> walls = new ArrayList<>();
        for (int[] w : edgeWalls) {
            walls.add(new Rectangle(w[0] * ts - 2, w[1] * ts, 4, (w[2] - w[1] + 1) * ts));
        }
        for (int[] w : edgeWallsH) {
            walls.add(new Rectangle(w[1] * ts, w[0] * ts - 2, (w[2] - w[1] + 1) * ts, 4));
        }
        return new TileMap(tiles, ts, layers, shade, walls, staircase, views);
    }
}
