package nightmare404.debug;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

import nightmare404.main.GamePanel;
import nightmare404.tile.TileSet;
import nightmare404.world.World;

// Debug tool: measures start-up time and how long one frame takes to draw (not a pass/fail check).
// Run: in Eclipse, Run As > Java Application. Or, after run-checks.bat has compiled everything:
//   java -Djava.awt.headless=true -cp build/test/classes nightmare404.debug.PerfDebug
public class PerfDebug { public static void main(String[] a) throws Exception {
    long t0 = System.nanoTime(); TileSet ts = new TileSet(); long t1 = System.nanoTime(); new World(ts, 32); long t2 = System.nanoTime();
    System.out.printf("Startup: load+mirror tiles %.1f ms, build the whole world %.2f ms%n", (t1-t0)/1e6, (t2-t1)/1e6);
    Map<Integer,Integer> types = new TreeMap<>();
    for (int i = 0; i < TileSet.COUNT; i++) types.merge(ts.image(i).getType(), 1, Integer::sum);
    System.out.println("Tile image types (5=3BYTE_BGR, 6=4BYTE_ABGR, 13=BYTE_INDEXED, 2=INT_ARGB): " + types);

    GamePanel gp = new GamePanel(); gp.setSize(800, 608);
    BufferedImage frame = new BufferedImage(800, 608, BufferedImage.TYPE_INT_RGB);
    String[] where = {"Room 404 (no scrolling)", "Hallway mid-scroll (2 maps drawn)"};
    for (int s = 0; s < 2; s++) {
        if (s == 1) { gp.currentRoom = gp.world.floor4.find("Hallway (Room 403)"); gp.flor.x = 700; gp.flor.y = 9*32; }
        for (int i = 0; i < 300; i++) { Graphics2D g = frame.createGraphics(); gp.paint(g); g.dispose(); }   // warm up
        int n = 1000; long st = System.nanoTime();
        for (int i = 0; i < n; i++) { Graphics2D g = frame.createGraphics(); gp.paint(g); g.dispose(); }
        System.out.printf("%s: %.2f ms per frame (budget at 60 FPS: 16.7 ms)%n", where[s], (System.nanoTime()-st)/1e6/n);
    }
} }
