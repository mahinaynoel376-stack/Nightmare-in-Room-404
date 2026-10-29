package tile;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

// One screen of tiles: 25 x 18 cells, drawn in 4 layers.
// Collision and doors are worked out from the tiles themselves:
//   - a cell is solid if any tile on it is solid (see TileSet.isSolid)
//   - every DOOR_LEFT tile followed by a DOOR_RIGHT tile is a 2-tile-wide door
// Build one with MapBuilder.
public class TileMap {
    public static final int COLS = 25, ROWS = 18;
    public static final int EMPTY = -1;

    // Layers, in the order they're drawn (later layers are drawn on top)
    public static final int LAYER_FLOOR = 0;    // Floor, walls, void
    public static final int LAYER_WALLS_V = 1;  // Vertical wall strips
    public static final int LAYER_WALLS_H = 2;  // Horizontal wall strips and corners
    public static final int LAYER_OBJECTS = 3;  // Furniture, doors, windows, rubble
    public static final int LAYERS = 4;

    private static final Color SHADE = new Color(0, 0, 0, 150);

    private final TileSet tiles;
    private final int ts;                // Tile size in pixels
    private final int[][][] layers;      // [layer][row][col] = tile ID, or EMPTY
    private final boolean[][] solid;     // [row][col], true = the player can't walk here
    private final List<Rectangle> doors = new ArrayList<>();   // In pixels
    private final List<Rectangle> shade;                       // Darkened areas, in pixels
    private final List<Rectangle> edgeWalls;                   // Thin invisible walls between cells, in pixels
    private BufferedImage cache;         // The whole map drawn once; null until it's first needed

    TileMap(TileSet tiles, int ts, int[][][] layers, List<Rectangle> shade, List<Rectangle> edgeWalls) {
        this.tiles = tiles;
        this.ts = ts;
        this.layers = layers;
        this.shade = shade;
        this.edgeWalls = edgeWalls;
        this.solid = new boolean[ROWS][COLS];

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                for (int[][] layer : layers) {
                    int id = layer[row][col];
                    if (id != EMPTY && tiles.isSolid(id)) {
                        solid[row][col] = true;
                    }
                }
            }
        }

        // A door only counts if the player can step onto it. Door pictures drawn on a solid wall
        // (like the floor below, seen over the hallway railing) are just decoration.
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col + 1 < COLS; col++) {
                if (layers[LAYER_OBJECTS][row][col] == TileSet.DOOR_LEFT
                        && layers[LAYER_OBJECTS][row][col + 1] == TileSet.DOOR_RIGHT
                        && !solid[row][col] && !solid[row][col + 1]) {
                    doors.add(new Rectangle(col * ts, row * ts, 2 * ts, ts));
                }
            }
        }
    }

    public boolean isSolid(int col, int row) {
        return solid[row][col];
    }

    // True if the hitbox overlaps a solid cell or an edge wall.
    // Above and below the map counts as solid. Left and right of the map is open:
    // walking off the side of the screen is how the player moves to the next room.
    public boolean collides(Rectangle box) {
        for (Rectangle wall : edgeWalls) {
            if (box.intersects(wall)) return true;
        }

        int leftCol = Math.floorDiv(box.x, ts);
        int rightCol = Math.floorDiv(box.x + box.width - 1, ts);
        int topRow = Math.floorDiv(box.y, ts);
        int bottomRow = Math.floorDiv(box.y + box.height - 1, ts);

        for (int row = topRow; row <= bottomRow; row++) {
            for (int col = leftCol; col <= rightCol; col++) {
                if (row < 0 || row >= ROWS) return true;
                if (col < 0 || col >= COLS) continue;
                if (solid[row][col]) return true;
            }
        }
        return false;
    }

    // The door the hitbox is touching, or null
    public Rectangle doorTouching(Rectangle box) {
        for (Rectangle door : doors) {
            if (box.intersects(door)) return door;
        }
        return null;
    }

    // The door whose centre is closest to x, or null if this map has no doors
    public Rectangle nearestDoor(int x) {
        Rectangle best = null;
        for (Rectangle door : doors) {
            if (best == null || Math.abs(door.getCenterX() - x) < Math.abs(best.getCenterX() - x)) {
                best = door;
            }
        }
        return best;
    }

    // True if the walkable side of this door is above it (a classroom's bottom wall),
    // false if it's below it (a hallway's wall)
    public boolean isFloorAbove(Rectangle door) {
        int col = door.x / ts;
        int row = door.y / ts;
        return row > 0 && !solid[row - 1][col];
    }

    // Draws the map. The tiles never change, so instead of drawing ~600 tiles every frame,
    // the whole map is drawn ONCE into an image (the cache), and every frame just copies that image.
    public void draw(Graphics2D g2d) {
        if (cache == null) {
            cache = render();           // First time this map is shown: build the picture
        }
        g2d.drawImage(cache, 0, 0, null);
    }

    // Throws the cached picture away, so the next draw() rebuilds it.
    // Call this if the map's tiles are ever changed during the game (e.g. a door gets smashed).
    public void clearCache() {
        cache = null;
    }

    // Draws every layer and the shading into one new image, tile by tile (the old per-frame work)
    private BufferedImage render() {
        BufferedImage image = newScreenImage(COLS * ts, ROWS * ts);
        Graphics2D g = image.createGraphics();
        for (int[][] layer : layers) {
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    int id = layer[row][col];
                    if (id == EMPTY) continue;
                    g.drawImage(tiles.image(id), col * ts, row * ts, null);
                }
            }
        }
        g.setColor(SHADE);
        for (Rectangle area : shade) {
            g.fill(area);
        }
        g.dispose();                    // Free the drawing tool we made for this image
        return image;
    }

    // Makes an empty image in the screen's own pixel format ("compatible" image).
    // Java can then copy it to the screen without converting every pixel, and can keep it
    // on the graphics card. Every map cell is filled (void is a black tile), so the image
    // doesn't need transparency: OPAQUE is the fastest kind.
    // Without a screen (the headless debug tools) there's no screen format, so use a plain image.
    private static BufferedImage newScreenImage(int width, int height) {
        if (GraphicsEnvironment.isHeadless()) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }
        GraphicsConfiguration screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        return screen.createCompatibleImage(width, height, Transparency.OPAQUE);
    }
}
