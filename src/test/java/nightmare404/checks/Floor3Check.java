package nightmare404.checks;

import nightmare404.entity.Player;
import nightmare404.main.GamePanel;
import nightmare404.world.Floor;
import nightmare404.world.Room;

// Simulated play-through of the stairs, Floor 3 and the library
public class Floor3Check {
    static GamePanel gp; static Player p; static int fails = 0;

    static void check(String what, boolean ok) { System.out.println((ok ? "  ok   " : "  FAIL ") + what + "   [" + at() + "]"); if (!ok) fails++; }
    static String at() { return "Floor " + gp.currentRoom.floor.number + ", " + gp.currentRoom.name + ", col " + (p.solidArea.x + 8) / 32 + " row " + (p.solidArea.y + 8) / 32 + ", x=" + p.x + " cam=" + gp.cameraX(); }
    static int row() { return (p.solidArea.y + 8) / 32; }
    // Holds keys for up to n ticks; stops early when the room changes
    static void walk(String keys, int n) {
        Room start = gp.currentRoom;
        gp.upPressed = keys.contains("U"); gp.downPressed = keys.contains("D"); gp.leftPressed = keys.contains("L"); gp.rightPressed = keys.contains("R");
        for (int i = 0; i < n && gp.currentRoom == start; i++) p.update();
        gp.upPressed = gp.downPressed = gp.leftPressed = gp.rightPressed = false;
    }
    static void go(Floor f, String room, int col, int row) { gp.currentRoom = f.find(room); p.x = col * 32; p.y = row * 32; p.update(); }
    static boolean in(Floor f, String room) { return gp.currentRoom == f.find(room); }
    static boolean atFoot(java.awt.Point foot) { return p.x == foot.x && p.y == foot.y; }

    public static void main(String[] a) {
        gp = new GamePanel(); p = gp.flor;
        Floor f4 = gp.world.floor4, f3 = gp.world.floor3;

        System.out.println("1. Floors are linked");
        check("floor4.below == floor3 and floor3.above == floor4", f4.below == f3 && f3.above == f4);
        check("Floor 4 has nothing above", f4.above == null);

        System.out.println("2. Down the west stairs (left flight) from Floor 4");
        go(f4, "West Stairs", 5, 14); walk("U", 200);
        check("arrived on Floor 3's West Stairs (Floor 3's way up)", in(f3, "West Stairs") && gp.currentRoom == f3.upStairs());
        check("standing at the foot of Floor 3's up (right) flight", atFoot(gp.currentRoom.map.stairs.upFoot));

        System.out.println("3. Back up (right flight) from Floor 3");
        walk("U", 200);
        check("arrived on Floor 4's West Stairs", in(f4, "West Stairs"));
        check("standing at the foot of Floor 4's down (left) flight", atFoot(gp.currentRoom.map.stairs.downFoot));

        System.out.println("4. Flights that don't work are blocked; the way down alternates ends");
        go(f4, "West Stairs", 8, 14); walk("U", 200);
        check("Floor 4 right flight (up, no Floor 5): blocked by rubble, still on Floor 4", in(f4, "West Stairs") && row() >= 14);
        go(f3, "West Stairs", 5, 14); walk("U", 200);
        check("Floor 3 west left flight has collapsed: no way down at this end", in(f3, "West Stairs") && row() >= 14);
        go(f3, "East Stairs", 10, 14); walk("U", 200);
        check("Floor 3 east right flight has collapsed too: the way up is the west end", in(f3, "East Stairs") && row() >= 14);
        go(f3, "East Stairs", 7, 14); walk("U", 200);
        check("Floor 3 east left flight leads down to Floor 2's East Stairs", in(gp.world.floor2, "East Stairs"));

        System.out.println("5. Floor 3 hallway to the library door");
        go(f3, "West Stairs", 15, 9); walk("R", 400);
        check("stairs -> Hallway (Library West)", gp.currentRoom.name.equals("Hallway (Library West)"));
        go(f3, "Hallway (Library West)", 12, 8); walk("U", 120);
        check("no door on the west part of the library front: blocked", gp.currentRoom.name.equals("Hallway (Library West)") && row() == 7);
        go(f3, "Hallway (Library West)", 14, 9); walk("R", 400);
        check("-> Hallway (Library Entrance)", gp.currentRoom.name.equals("Hallway (Library Entrance)"));
        go(f3, "Hallway (Library East)", 12, 8); walk("U", 120);
        check("no door on the east part either: blocked", gp.currentRoom.name.equals("Hallway (Library East)") && row() == 7);
        go(f3, "Hallway (Library Entrance)", 7, 8); walk("U", 120);
        check("the big double door leads into Library (Entrance)", gp.currentRoom.name.equals("Library (Entrance)"));
        check("  arrived just inside, above the door (row 14)", row() == 14 && p.solidArea.x >= 10 * 32 && p.solidArea.x + 16 <= 14 * 32);
        check("  camera doesn't scroll in the library", gp.cameraX() == 0);

        System.out.println("6. Inside the library: screen to screen");
        go(f3, "Library (Entrance)", 2, 9); walk("L", 300);
        check("walking off the left edge -> Library (West)", gp.currentRoom.name.equals("Library (West)"));
        check("  whole player on screen, camera still 0", p.x >= 0 && p.x <= 768 && gp.cameraX() == 0);
        walk("R", 300);
        check("walking back right -> Library (Entrance)", gp.currentRoom.name.equals("Library (Entrance)") && p.x >= 0);
        go(f3, "Library (Entrance)", 22, 7); walk("R", 300);
        check("off the right edge -> Library (East)", gp.currentRoom.name.equals("Library (East)") && gp.cameraX() == 0);
        go(f3, "Library (East)", 18, 11); walk("R", 300);
        check("the library's east wall stops you", gp.currentRoom.name.equals("Library (East)") && p.solidArea.x + 16 <= 704);
        go(f3, "Library (West)", 6, 6); walk("L", 300);
        check("the library's west wall stops you", gp.currentRoom.name.equals("Library (West)") && p.solidArea.x >= 96);
        go(f3, "Library (West)", 6, 14); walk("D", 120);
        check("no way out through the west screen's bottom wall", gp.currentRoom.name.equals("Library (West)") && row() == 15);
        go(f3, "Library (Entrance)", 11, 13); walk("D", 200);
        check("out through the big door -> Hallway (Library Entrance)", gp.currentRoom.name.equals("Hallway (Library Entrance)") && row() == 7);
        check("  camera follows again in the hallway (not fixed)", !gp.currentRoom.fixedScreen);

        System.out.println("7. Linked list integrity (Floor 3)");
        int count = 0; Room prev = null;
        for (Room r = f3.head; r != null; r = r.right) { if (r.left != prev) { check(r.name + ": left link points back", false); } prev = r; count++; }
        check("hallway: 5 screens, every left link points back, tail is last", count == 5 && f3.size == 5 && f3.tail == prev);
        Room libEntrance = f3.find("Library (Entrance)"), libWest = f3.find("Library (West)"), libEast = f3.find("Library (East)");
        check("library row: West <-> Entrance <-> East, both directions",
                libWest.right == libEntrance && libEntrance.left == libWest && libEntrance.right == libEast && libEast.left == libEntrance
                && libWest.left == null && libEast.right == null);
        check("only the entrance has a door, and it links both ways",
                libEntrance.door == f3.find("Hallway (Library Entrance)") && libEntrance.door.door == libEntrance
                && libWest.door == null && libEast.door == null);
        check("all three library screens are fixed screens", libWest.fixedScreen && libEntrance.fixedScreen && libEast.fixedScreen);

        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");

        System.exit(fails == 0 ? 0 : 1);
    }
}
