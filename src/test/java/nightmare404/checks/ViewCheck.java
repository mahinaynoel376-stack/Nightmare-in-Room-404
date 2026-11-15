package nightmare404.checks;

import java.awt.Rectangle;

import nightmare404.main.GamePanel;
import nightmare404.tile.TileMap;
import nightmare404.world.Floor;
import nightmare404.world.Room;

// Checks the view over every railing shows exactly the floor below, tile for tile, in every layer
// (on the ground floor: the quadrangle outside, lined up with the main entrance), and that views
// are only scenery (solid, no doors)
public class ViewCheck {
    public static void main(String[] a) {
        GamePanel gp = new GamePanel(); gp.peaceful();
        int fails = 0, screens = 0, cells = 0;
        // Ground floor: where its columns land in the quadrangle (the main entrance meets its doorway)
        int floorCol = 0, entranceCol = 0;
        for (Room r = gp.world.floor1.head; r != null; r = r.right) {
            if (r == gp.world.groundExit) entranceCol = floorCol + nightmare404.map.Hallway.ENTRANCE_COL;
            floorCol += r.map.cols;
        }
        int shift = nightmare404.map.Quadrangle.DOOR_COL - entranceCol;
        TileMap quad = gp.world.quadrangle.map;
        int screenStart = 0;
        for (Floor floor = gp.world.top; floor != null; floor = floor.below) {
            Room up = floor.head, down = floor.below == null ? null : floor.below.head;
            for (; up != null; up = up.right, down = down == null ? null : down.right) {
                TileMap map = up.map;
                int start = screenStart;
                screenStart = up.right == null ? 0 : screenStart + map.cols;
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
                            if (down != null) {
                                for (int layer = 0; layer < TileMap.LAYERS; layer++)
                                    if (map.tileAt(layer, col, row) != down.map.tileAt(layer, col, src)) bad++;
                            } else {   // The ground floor: the quadrangle, where it reaches
                                int qc = start + col + shift;
                                if (qc >= 0 && qc < quad.cols)
                                    for (int layer = 0; layer < TileMap.LAYERS; layer++)
                                        if (map.tileAt(layer, col, row) != quad.tileAt(layer, qc, src)) bad++;
                            }
                            if (!map.isSolid(col, row)) bad++;
                        }
                    }
                }
                boolean doorsOk = true;
                for (int x = 0; x < map.widthPx(); x += 16) {
                    Rectangle d = map.nearestDoor(x);
                    if (d != null && d.y >= 13 * 32) doorsOk = false;
                }
                boolean ok = bad == 0 && doorsOk;
                String shows = down != null ? "Floor " + floor.below.number + " " + down.name : "the Quadrangle";
                System.out.println((ok ? "  ok   " : "  FAIL ") + "Floor " + floor.number + " " + up.name + " shows "
                        + shows + (bad > 0 ? " (" + bad + " mismatches)" : "") + (doorsOk ? "" : " (door in view!)"));
                if (!ok) fails++;
            }
        }
        System.out.println(screens + " screens with a view, " + cells + " cells checked");
        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }
}
