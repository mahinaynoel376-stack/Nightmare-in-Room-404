package Nightmareon404;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import tile.TileSet;
import world.Floor;
import world.Room;
import world.World;

// Debug tool: walks the linked list, prints it, and saves a picture of every room
// (hallway screens and the classrooms behind them) so you can check a map without playing.
// Run: java -Djava.awt.headless=true -cp "bin;res" Nightmareon404.MapPreviewDebug [output folder]
public class MapPreviewDebug {
    public static void main(String[] args) throws IOException {
        File outDir = new File(args.length > 0 ? args[0] : "map_previews");
        outDir.mkdirs();

        int ts = 32;
        World world = new World(new TileSet(), ts);
        world.print(world.start);

        Floor floor = world.floor4;
        int n = 1;
        for (Room r = floor.head; r != null; r = r.right) {
            save(r, ts, new File(outDir, String.format("%02d_%s.png", n++, fileName(r.name))));
            if (r.door != null) {
                save(r.door, ts, new File(outDir, String.format("%02d_%s.png", n++, fileName(r.door.name))));
            }
        }
        System.out.println("Saved " + (n - 1) + " pictures to " + outDir.getAbsolutePath());
    }

    private static void save(Room room, int ts, File file) throws IOException {
        BufferedImage img = new BufferedImage(25 * ts, 19 * ts, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        room.map.draw(g2d);
        g2d.dispose();
        ImageIO.write(img, "png", file);
    }

    private static String fileName(String roomName) {
        return roomName.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("_$", "");
    }
}
