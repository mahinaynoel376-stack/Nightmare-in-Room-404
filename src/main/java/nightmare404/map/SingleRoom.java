package nightmare404.map;

import static nightmare404.tile.TileMap.*;
import static nightmare404.tile.TileSet.*;

import java.util.Map;

import nightmare404.tile.MapBuilder;
import nightmare404.tile.TileMap;
import nightmare404.tile.TileSet;

// A room that fills one screen in the classroom shape: side walls at columns 2 and 22, a top wall
// on row 2, and a bottom wall on row 16 with its doors at columns 5-6 and 18-19 (so it lines up
// with the hallway). Drawn from a character map. The labs (Floor 2) and the offices (Floor 1)
// are built this way: only their legend and their wall/floor tiles differ.
public class SingleRoom {
    private static final int TOP_WALL_ROW = 2;
    private static final int BOTTOM_WALL_ROW = 16;

    public static TileMap build(TileSet tiles, int ts, String[] map,
                                Map<Character, Integer> legend, MapBuilder.Underlay underlay) {
        MapBuilder b = new MapBuilder();
        b.fill(LAYER_FLOOR, 0, 0, 24, 17, VOID);
        b.paint(map, legend, underlay);
        frame(b, map);
        return b.build(tiles, ts);
    }

    // The same stone frame as a classroom: walls all round, with the bottom wall left open
    // wherever the map has a door
    private static void frame(MapBuilder b, String[] map) {
        b.wallH(TOP_WALL_ROW, 3, 21);
        b.wallH(BOTTOM_WALL_ROW, 3, 21);
        for (int col = 3; col <= 21; col++) {
            if (map[BOTTOM_WALL_ROW].charAt(col) != '.') {
                b.set(LAYER_WALLS_H, col, BOTTOM_WALL_ROW, EMPTY);   // A door goes here instead
            }
        }
        b.set(LAYER_WALLS_H, 2, TOP_WALL_ROW, WALL_CORNER_LEFT);
        b.set(LAYER_WALLS_H, 2, BOTTOM_WALL_ROW, WALL_CORNER_LEFT);
        b.set(LAYER_WALLS_H, 22, TOP_WALL_ROW, WALL_CORNER_RIGHT);
        b.set(LAYER_WALLS_H, 22, BOTTOM_WALL_ROW, WALL_CORNER_RIGHT);
        b.wallVRight(LAYER_WALLS_V, 2, TOP_WALL_ROW + 1, BOTTOM_WALL_ROW);
        b.wallVLeft(LAYER_WALLS_V, 22, TOP_WALL_ROW + 1, BOTTOM_WALL_ROW);
    }
}
