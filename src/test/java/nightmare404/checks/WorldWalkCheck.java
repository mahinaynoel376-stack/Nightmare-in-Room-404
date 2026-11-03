package nightmare404.checks;

import nightmare404.entity.Player;
import nightmare404.main.GamePanel;
import nightmare404.world.Room;

// Simulated play-through of Floor 4: out of Room 404, along the hallway to both stairwells, every door,
// and the linked list itself (every left/right/door link points back the right way)
public class WorldWalkCheck {
    static GamePanel gp; static Player p; static int fails = 0;
    static void check(String what, boolean ok) { System.out.println((ok ? "  ok   " : "  FAIL ") + what); if (!ok) fails++; }
    static String at() { return gp.currentRoom.name + " col " + (p.solidArea.x + 8) / 32 + " row " + (p.solidArea.y + 8) / 32; }
    // Holds keys for up to n ticks; stops early when the room changes
    static void walk(String keys, int n) {
        var start = gp.currentRoom;
        gp.upPressed = keys.contains("U"); gp.downPressed = keys.contains("D"); gp.leftPressed = keys.contains("L"); gp.rightPressed = keys.contains("R");
        for (int i = 0; i < n && gp.currentRoom == start; i++) p.update();
        gp.upPressed = gp.downPressed = gp.leftPressed = gp.rightPressed = false;
    }
    static void put(int col, int row) { p.x = col * 32; p.y = row * 32; p.update(); }
    static void go(String room) { gp.currentRoom = gp.world.floor4.find(room); }
    public static void main(String[] a) {
        gp = new GamePanel(); p = gp.flor;

        System.out.println("1. Out of Room 404 through its working door");
        check("starts in Room 404: " + at(), gp.currentRoom.name.equals("Room 404"));
        put(18, 14); walk("D", 120);
        check("door leads to the hallway: " + at(), gp.currentRoom.name.equals("Hallway (Room 404)"));
        check("arrives in the corridor below the door (row 7)", (p.solidArea.y) / 32 == 7 && p.solidArea.x / 32 >= 18 && p.solidArea.x / 32 <= 19);
        walk("U", 60);
        check("walking back up re-enters Room 404: " + at(), gp.currentRoom.name.equals("Room 404"));
        go("Hallway (Room 404)"); put(5, 8); walk("U", 120);
        check("404's broken door can't be entered from the hallway: " + at(), gp.currentRoom.name.equals("Hallway (Room 404)"));

        System.out.println("2. Hallway 404 -> East Stairs (right) and back");
        put(20, 9); walk("R", 400);
        check("right edge leads to: " + at(), gp.currentRoom.name.equals("East Stairs (destroyed)"));
        walk("R", 600);
        check("the building's end wall stops you: " + at(), gp.currentRoom.name.startsWith("East") && p.x < 22 * 32);
        put(12, 14); walk("U", 200);
        check("can't climb the collapsed stairs from below (stopped at row " + p.solidArea.y / 32 + ")", p.solidArea.y / 32 >= 12);
        put(2, 9); walk("L", 200);
        check("left edge leads back to: " + at(), gp.currentRoom.name.equals("Hallway (Room 404)"));

        System.out.println("3. Along the hallway to the left");
        put(3, 9); walk("L", 200);
        check("Hallway 404 -> left: " + at(), gp.currentRoom.name.equals("Hallway (Room 403)"));
        put(5, 8); walk("U", 120);
        check("403's left door works: " + at(), gp.currentRoom.name.equals("Room 403"));
        check("  arrived above the left door (col 5-6, row 15)", p.solidArea.x / 32 >= 5 && p.solidArea.x / 32 <= 6 && p.solidArea.y / 32 == 15);
        put(18, 14); walk("D", 120);
        check("403's right door leads out too: " + at(), gp.currentRoom.name.equals("Hallway (Room 403)") && p.solidArea.x / 32 >= 18);
        put(3, 9); walk("L", 200);
        check("Hallway 403 -> left: " + at(), gp.currentRoom.name.equals("Hallway (Room 402)"));
        put(18, 8); walk("U", 120);
        check("402's right door is broken: " + at(), gp.currentRoom.name.equals("Hallway (Room 402)"));
        put(5, 8); walk("U", 120);
        check("402's left door works: " + at(), gp.currentRoom.name.equals("Room 402"));
        walk("D", 120);
        check("and leads back out: " + at(), gp.currentRoom.name.equals("Hallway (Room 402)"));
        put(3, 9); walk("L", 200);
        check("Hallway 402 -> left: " + at(), gp.currentRoom.name.equals("West Stairs"));
        put(9, 14); walk("U", 200);
        check("the working (left) flight takes you down a floor: " + at(), gp.currentRoom.floor.number == 3);
        gp.currentRoom = gp.world.floor4.head;
        put(5, 9); walk("L", 400);
        check("the building's end wall stops you: " + at(), gp.currentRoom.name.equals("West Stairs") && p.solidArea.x >= 3 * 32);
        put(20, 9); walk("R", 300);
        check("West Stairs -> right: " + at(), gp.currentRoom.name.equals("Hallway (Room 402)"));

        System.out.println("4. Linked list integrity");
        int count = 0; var prev = (Room) null;
        for (var r = gp.world.floor4.head; r != null; r = r.right) { check(r.name + ": left link points back", r.left == prev); prev = r; count++; }
        check("tail is the last node, size " + gp.world.floor4.size + " == " + count, gp.world.floor4.tail == prev && count == gp.world.floor4.size);
        for (var r = gp.world.floor4.head; r != null; r = r.right) if (r.door != null) check(r.door.name + " <-> " + r.name + " door links both ways", r.door.door == r);
        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }
}
