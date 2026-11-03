package nightmare404.debug;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import nightmare404.tile.TileSet;
import nightmare404.world.Floor;
import nightmare404.world.Room;
import nightmare404.world.World;

// Debug tool: walks the linked list, prints it, and saves a picture of every room
// (hallway screens and the classrooms behind them) so you can check a map without playing.
// Run: in Eclipse, Run As > Java Application. Or, after run-checks.bat has compiled everything:
//   java -Djava.awt.headless=true -cp build/test/classes nightmare404.debug.MapPreviewDebug [output folder]
public class MapPreviewDebug {
    public static void main(String[] args) throws IOException {
        File outDir = new File(args.length > 0 ? args[0] : "map_previews");
        outDir.mkdirs();

        int ts = 32;
        World world = new World(new TileSet(), ts);
        world.print(world.start);

        // Every floor (top to bottom), every hallway screen (left to right),
        // and the row of rooms behind each door (left to right)
        int n = 1;
        for (Floor floor = world.top; floor != null; floor = floor.below) {
            String prefix = "floor" + floor.number + "_";
            for (Room r = floor.head; r != null; r = r.right) {
                save(r, ts, new File(outDir, String.format("%02d_%s%s.png", n++, prefix, fileName(r.name))));
                if (r.door != null) {
                    for (Room side = Floor.leftEnd(r.door); side != null; side = side.right) {
                        save(side, ts, new File(outDir, String.format("%02d_%s%s.png", n++, prefix, fileName(side.name))));
                    }
                }
            }
        }
        System.out.println("Saved " + (n - 1) + " pictures to " + outDir.getAbsolutePath());
    }

    private static void save(Room room, int ts, File file) throws IOException {
        // The whole map (bigger than a screen for the quadrangle), at least one screen tall
        BufferedImage img = new BufferedImage(room.map.widthPx(), Math.max(room.map.heightPx(), 19 * ts),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        room.map.draw(g2d);
        g2d.dispose();
        ImageIO.write(img, "png", file);
    }

    private static String fileName(String roomName) {
        return roomName.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("_$", "");
    }
}
