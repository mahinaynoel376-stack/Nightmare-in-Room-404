package nightmare404.checks;

import java.awt.Point;

import nightmare404.entity.Player;
import nightmare404.main.GamePanel;
import nightmare404.map.Hallway;
import nightmare404.map.Quadrangle;
import nightmare404.tile.TileMap;
import nightmare404.tile.TileSet;
import nightmare404.world.Room;

// Simulated play in the quadrangle: getting in and out, the centred camera, and each stronghold's defences
public class QuadCheck {
    static GamePanel gp; static Player p; static int fails = 0;

    static void check(String what, boolean ok) { System.out.println((ok ? "  ok   " : "  FAIL ") + what + "   [" + at() + "]"); if (!ok) fails++; }
    static String at() { return gp.currentRoom.name + ", col " + col() + " row " + row() + ", cam " + gp.cameraX() + "," + gp.cameraY(); }
    static int row() { return (p.solidArea.y + 8) / 32; }
    static int col() { return (p.solidArea.x + 8) / 32; }
    static void walk(String keys, int n) {
        Room start = gp.currentRoom;
        gp.upPressed = keys.contains("U"); gp.downPressed = keys.contains("D"); gp.leftPressed = keys.contains("L"); gp.rightPressed = keys.contains("R");
        for (int i = 0; i < n && gp.currentRoom == start; i++) p.update();
        gp.upPressed = gp.downPressed = gp.leftPressed = gp.rightPressed = false;
    }
    static void quad(int c, int r) { gp.currentRoom = gp.world.quadrangle; p.x = c * 32; p.y = r * 32; p.update(); }
    static boolean inQuad() { return gp.currentRoom == gp.world.quadrangle; }

    public static void main(String[] a) {
        gp = new GamePanel(); p = gp.flor;
        Room quad = gp.world.quadrangle, exit = gp.world.groundExit;
        TileMap m = quad.map;

        System.out.println("1. The quadrangle and its links");
        check("50 x 36 tiles (2 x 2 rooms' worth)", m.cols == 50 && m.rows == 36 && m.widthPx() == 1600 && m.heightPx() == 1152);
        check("linked both ways with the main entrance, in the middle of the ground floor",
                exit.doorLeadsTo(exit.map.doorsInBottomWall().get(0)) == quad && quad.door == exit);
        check("outdoors (the screen says Outside)", quad.outdoors);

        System.out.println("2. Getting in and out");
        gp.currentRoom = exit; p.x = (Hallway.ENTRANCE_COL + 1) * 32; p.y = 10 * 32; p.update(); walk("D", 200);
        check("walking out of the main entrance -> the quadrangle", inQuad());
        check("  arrived just below its doorway, in the middle of the building's face (cols 23-26, row 4)",
                row() == 4 && col() >= 23 && col() <= 26);
        walk("U", 200);
        check("walking back into the doorway -> the middle hallway, just above the entrance", gp.currentRoom == exit && row() == 11);
        for (int col : new int[] {5, 43}) {
            quad(col, 6); walk("U", 200);
            check("the side doorway at cols " + col + "-" + (col + 1) + " is boarded up: no way in", inQuad() && row() == 4);
            check("  and it isn't a door (the main entrance is the only one)", m.doorCount() == 1);
        }

        System.out.println("3. The camera follows the player");
        quad(25, 18);
        int viewH = gp.screenHeight - gp.tileSize;
        int screenX = p.x + 16 - gp.cameraX(), screenY = p.y + 16 - gp.cameraY();
        check("in the middle of the map the player is in the middle of the view", screenX == 400 && screenY == viewH / 2);
        quad(1, 5);
        check("top-left corner: the camera stops at the map's edges (0, 0)", gp.cameraX() == 0 && gp.cameraY() == 0);
        quad(47, 33);
        check("bottom-right corner: it stops there too (800, 576)", gp.cameraX() == 1600 - 800 && gp.cameraY() == 1152 - viewH);
        quad(25, 18); int cx = gp.cameraX(), cy = gp.cameraY(); walk("D", 10);
        check("walking moves the camera with the player", gp.cameraY() > cy && gp.cameraX() == cx);

        System.out.println("4. Stronghold 1: the wrecked bus (roof at cols 8-11, rows 27-28; ladder at col 10, row 29; its front at col 12)");
        quad(10, 31); walk("U", 200);
        check("up the ladder onto the roof", inQuad() && row() <= 28 && col() == 10);
        walk("R", 300);
        check("along the roof to the front", row() <= 28 && col() == 11);
        walk("R", 100);
        check("  can't step off the front (onto the bus's lower front)", col() == 11);
        quad(11, 28); walk("D", 100);
        check("  can't step off the side (it's the bus's side)", row() == 28);
        quad(11, 27); walk("U", 100);
        check("  can't step off the far side", row() == 27);
        quad(11, 31); walk("U", 200);
        check("from below (not at the ladder): blocked by the bus's side", row() == 30);
        quad(11, 24); walk("D", 200);
        check("from above: blocked by the roof's far edge", row() == 26);
        quad(5, 28); walk("R", 200);
        check("from behind: blocked by the roof's back edge", col() == 7);
        quad(17, 28); walk("L", 200);
        check("from in front: blocked by the bus's front", col() == 13);
        quad(6, 29); walk("R", 200);
        check("onto the ladder from the side: blocked (only from below)", col() == 7);

        System.out.println("5. Stronghold 2: the book barricade (front row 10, gaps at cols 27 and 31)");
        quad(29, 8); walk("D", 100);
        check("the front row stops you", row() == 9);
        quad(27, 9); walk("D", 100);
        check("through the gap at col 27", row() >= 11);
        quad(31, 9); walk("D", 100);
        check("through the gap at col 31", row() >= 11);
        quad(29, 12); walk("U", 100);
        check("from the plaza side the front row stops you too", row() == 11);
        quad(29, 6); walk("D", 30);
        check("its back is open towards the building", row() >= 7);

        System.out.println("6. Stronghold 3: the cage (court cols 32-45, rows 22-32; gate at cols 33-34)");
        quad(40, 19); walk("D", 200);
        check("the fence stops you from outside", row() == 20);
        quad(40, 26); walk("U", 200);
        check("...and from inside", row() == 22);
        quad(33, 24); walk("U", 200);
        check("the gate (cols 33-34) is the way in and out", row() <= 20);
        TileSet ts = gp.tiles;
        check("the fence blocks walking but not shots", ts.isSolid(TileSet.FENCE_H) && ts.isShootThrough(TileSet.FENCE_H)
                && ts.isShootThrough(TileSet.FENCE_V) && !ts.isShootThrough(TileSet.TREE_FIRST + 2) && !ts.isShootThrough(TileSet.BUS_FIRST + 11)
                && ts.isShootThrough(TileSet.BUSH_FIRST) && !ts.isShootThrough(TileSet.ROCKS_FIRST));

        System.out.println("7. The perimeter");
        quad(5, 20); walk("L", 300);
        check("west fence", col() == 1);
        quad(40, 13); walk("R", 300);
        check("east fence", col() == 48);
        quad(10, 32); walk("D", 300);
        check("south fence (behind the trees)", row() <= 34);
        for (Point g : Quadrangle.SPAWN_GATES) {
            int id = m.tileAt(TileMap.LAYER_OBJECTS, g.x, g.y);
            check("spawn gate at (" + g.x + "," + g.y + ") is a locked gate in the fence", (id == TileSet.GATE_H || id == TileSet.GATE_V) && m.isSolid(g.x, g.y));
        }

        System.out.println("8. Stronghold data");
        check("3 strongholds", Quadrangle.STRONGHOLDS.size() == 3);
        for (Quadrangle.KeyLocation k : Quadrangle.STRONGHOLDS) {
            int open = 0, total = 0;
            for (int r = k.area().y; r < k.area().y + k.area().height; r++)
                for (int c = k.area().x; c < k.area().x + k.area().width; c++) { total++; if (!m.isSolid(c, r)) open++; }
            check(k.name() + ": " + open + " of " + total + " tiles can be stood on", open >= total * 3 / 4);
        }
        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }
}
