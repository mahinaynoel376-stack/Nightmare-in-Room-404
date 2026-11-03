package nightmare404.checks;

import nightmare404.entity.Player;
import nightmare404.main.GamePanel;
import nightmare404.world.Room;

// Walks Floor 4's hallway from the east end to the west end and checks the camera follows smoothly:
// no jumps when the player crosses from one hallway screen to the next.
public class CameraCheck {
    static GamePanel gp; static Player p; static int fails = 0;

    static void check(String what, boolean ok) { System.out.println((ok ? "  ok   " : "  FAIL ") + what); if (!ok) fails++; }
    // Which screen of the hallway a room is (0 = the leftmost), so positions can be measured along the whole floor
    static int index(Room r) { int i = 0; for (Room n = gp.world.floor4.head; n != null; n = n.right, i++) if (n == r) return i; return -1; }

    public static void main(String[] a) {
        gp = new GamePanel(); p = gp.flor;
        check("in Room 404 the camera doesn't scroll (camera = " + gp.cameraX() + ", " + gp.cameraY() + ")", gp.cameraX() == 0 && gp.cameraY() == 0);

        gp.currentRoom = gp.world.floor4.tail; p.x = 30; p.y = 9 * 32; p.update();   // East Stairs, left side
        int prevWorld = index(gp.currentRoom) * 800 + p.x, prevCamWorld = index(gp.currentRoom) * 800 + gp.cameraX();
        int maxWorldJump = 0, maxCamJump = 0;
        String rooms = gp.currentRoom.name;
        gp.leftPressed = true;
        for (int t = 0; t < 2000 && !(gp.currentRoom == gp.world.floor4.head && p.x <= 3 * 32 + 1); t++) {
            Room before = gp.currentRoom; p.update();
            if (gp.currentRoom != before) rooms += " -> " + gp.currentRoom.name;
            int world = index(gp.currentRoom) * 800 + p.x, camWorld = index(gp.currentRoom) * 800 + gp.cameraX();
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
