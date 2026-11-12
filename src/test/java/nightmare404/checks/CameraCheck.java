package nightmare404.checks;

import nightmare404.entity.Player;
import nightmare404.main.GamePanel;
import nightmare404.world.Room;

// Walks Floor 4's hallway from the east end to the west end and checks the camera follows smoothly:
// no jumps when the player crosses from one hallway screen to the next.
public class CameraCheck {
    static GamePanel gp; static Player p; static int fails = 0;

    static void check(String what, boolean ok) { System.out.println((ok ? "  ok   " : "  FAIL ") + what); if (!ok) fails++; }
    // Where a hallway screen starts along the whole floor, in pixels: the widths of all the screens to its left
    // (the screens aren't all the same width, so positions can't just be "screen number x 800")
    static int offset(Room r) { int px = 0; for (Room n = r.left; n != null; n = n.left) px += n.map.widthPx(); return px; }

    public static void main(String[] a) {
        gp = new GamePanel(); p = gp.flor;
        check("in Room 404 the camera doesn't scroll (camera = " + gp.cameraX() + ", " + gp.cameraY() + ")", gp.cameraX() == 0 && gp.cameraY() == 0);

        gp.currentRoom = gp.world.floor4.tail; p.x = 30; p.y = 9 * 32; p.update();   // East Stairs, left side
        int prevWorld = offset(gp.currentRoom) + p.x, prevCamWorld = offset(gp.currentRoom) + gp.cameraX();
        int maxWorldJump = 0, maxCamJump = 0;
        String rooms = gp.currentRoom.name;
        gp.leftPressed = true;
        for (int t = 0; t < 2000 && !(gp.currentRoom == gp.world.floor4.head && p.x <= 3 * 32 + 1); t++) {
            Room before = gp.currentRoom; p.update();
            if (gp.currentRoom != before) rooms += " -> " + gp.currentRoom.name;
            int world = offset(gp.currentRoom) + p.x, camWorld = offset(gp.currentRoom) + gp.cameraX();
            maxWorldJump = Math.max(maxWorldJump, Math.abs(world - prevWorld));
            maxCamJump = Math.max(maxCamJump, Math.abs(camWorld - prevCamWorld));
            prevWorld = world; prevCamWorld = camWorld;
        }
        gp.leftPressed = false;

        System.out.println("Walked: " + rooms);
        check("walked the whole hallway to the West Stairs", gp.currentRoom == gp.world.floor4.head);
        check("the player never jumps: biggest step " + maxWorldJump + " px (speed " + p.speed + ")", maxWorldJump <= p.speed);
        check("the camera never jumps, even between screens: biggest step " + maxCamJump + " px", maxCamJump <= p.speed);
        check("at the west end the camera doesn't show past the building (camera = " + gp.cameraX() + ")", gp.cameraX() >= 0);
        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }
}
