package nightmare404.checks;

import java.awt.Rectangle;

import nightmare404.main.GamePanel;
import nightmare404.tile.TileMap;
import nightmare404.world.Floor;
import nightmare404.world.Room;

// Checks the view over every railing shows exactly the floor below, tile for tile, in every layer,
// and that views are only scenery (solid, no doors)
public class ViewCheck {
    public static void main(String[] a) {
        GamePanel gp = new GamePanel();
        int fails = 0, screens = 0, cells = 0;
        for (Floor floor = gp.world.top; floor != null; floor = floor.below) {
            Room up = floor.head, down = floor.below == null ? null : floor.below.head;
            for (; up != null; up = up.right, down = down == null ? null : down.right) {
                TileMap map = up.map;
                if (map.views().isEmpty()) {
                    if (floor.below != null) { System.out.println("  FAIL " + floor.number + " " + up.name + ": no view"); fails++; }
                    continue;
                }
                screens++;
                int bad = 0;
                for (TileMap.View v : map.views()) {
                    for (int row = v.row0(); row <= v.row1(); row++) {
                        for (int col = v.col0(); col <= v.col1(); col++) {
                            cells++;
                            int src = v.sourceRow0() + row - v.row0();
                            for (int layer = 0; layer < TileMap.LAYERS; layer++)
                                if (map.tileAt(layer, col, row) != down.map.tileAt(layer, col, src)) bad++;
                            if (!map.isSolid(col, row)) bad++;
                        }
                    }
                }
                boolean doorsOk = true;
                for (int x = 0; x < 800; x += 16) {
                    Rectangle d = map.nearestDoor(x);
                    if (d != null && d.y >= 13 * 32) doorsOk = false;
                }
                boolean ok = bad == 0 && doorsOk;
                System.out.println((ok ? "  ok   " : "  FAIL ") + "Floor " + floor.number + " " + up.name + " shows Floor "
                        + floor.below.number + " " + down.name + (bad > 0 ? " (" + bad + " mismatches)" : "") + (doorsOk ? "" : " (door in view!)"));
                if (!ok) fails++;
            }
        }
        System.out.println(screens + " screens with a view, " + cells + " cells checked");
        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }
}
