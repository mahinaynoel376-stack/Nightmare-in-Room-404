package nightmare404.checks;

import nightmare404.entity.Player;
import nightmare404.main.GamePanel;
import nightmare404.map.Hallway;
import nightmare404.world.Floor;
import nightmare404.world.Room;

// Simulated play-through of Floor 2: the stairs from Floor 3, the hallway, and every lab door
public class Floor2Check {
    static GamePanel gp; static Player p; static int fails = 0;

    static void check(String what, boolean ok) { System.out.println((ok ? "  ok   " : "  FAIL ") + what + "   [" + at() + "]"); if (!ok) fails++; }
    static String at() { return "Floor " + gp.currentRoom.floor.number + ", " + gp.currentRoom.name + ", col " + (p.solidArea.x + 8) / 32 + " row " + row(); }
    static int row() { return (p.solidArea.y + 8) / 32; }
    static int col() { return (p.solidArea.x + 8) / 32; }
    static void walk(String keys, int n) {
        Room start = gp.currentRoom;
        gp.upPressed = keys.contains("U"); gp.downPressed = keys.contains("D"); gp.leftPressed = keys.contains("L"); gp.rightPressed = keys.contains("R");
        for (int i = 0; i < n && gp.currentRoom == start; i++) p.update();
        gp.upPressed = gp.downPressed = gp.leftPressed = gp.rightPressed = false;
    }
    static void go(Floor f, String room, int col, int row) { gp.currentRoom = f.find(room); p.x = col * 32; p.y = row * 32; p.update(); }
    static boolean in(String room) { return gp.currentRoom.name.equals(room); }
    static boolean atFoot(java.awt.Point foot) { return p.x == foot.x && p.y == foot.y; }

    // Walks up into hallway door number d (0 = left, 1 = right). The lab's door number d is in a
    // different column (the hallway screen is narrower), but it's the one you should arrive at.
    static void doorTest(Floor f, String hallway, String lab, int d, boolean shouldWork) {
        int hallCol = Hallway.HALL_DOOR_COLS[d], roomCol = Hallway.ROOM_DOOR_COLS[d];
        go(f, hallway, hallCol, 8); walk("U", 120);
        if (shouldWork) {
            check(hallway + ": door at cols " + hallCol + "-" + (hallCol + 1) + " -> " + lab, in(lab));
            check("  arrived just above the same door inside (row 15)", row() == 15 && col() >= roomCol && col() <= roomCol + 1);
            walk("D", 120);
            check("  and back out to the hallway below it (row 7)", in(hallway) && row() == 7 && col() >= hallCol && col() <= hallCol + 1);
            go(f, lab, roomCol, 14); walk("D", 120);
            check("  " + lab + ": walking down into that door leads out too", in(hallway));
        } else {
            check(hallway + ": sealed door at cols " + hallCol + "-" + (hallCol + 1) + " doesn't open", in(hallway) && row() == 7);
            go(f, lab, roomCol, 14); walk("D", 120);
            check("  " + lab + ": blocked from inside too", in(lab) && row() == 15);
        }
    }

    public static void main(String[] a) {
        gp = new GamePanel(); p = gp.flor;
        Floor f4 = gp.world.floor4, f3 = gp.world.floor3, f2 = gp.world.floor2;

        System.out.println("1. Floors are linked: 4 <-> 3 <-> 2");
        check("floor3.below == floor2 and floor2.above == floor3", f3.below == f2 && f2.above == f3);
        check("Floor 2 now has Floor 1 below it", f2.below == gp.world.floor1);
        int n = 0; for (Floor f = gp.world.top; f != null; f = f.below) n++;
        check("walking the floor list from the top visits 4 floors", n == 4);

        System.out.println("2. Stairs: Floor 3 <-> 2 at the east end, Floor 2 <-> 1 at the west end");
        go(f3, "East Stairs", 7, 14); walk("U", 200);
        check("Floor 3 east left flight -> Floor 2's East Stairs, at the foot of its up flight",
                gp.currentRoom == f2.find("East Stairs") && atFoot(gp.currentRoom.map.stairs.upFoot));
        walk("U", 200);
        check("Floor 2 east right flight -> Floor 3's East Stairs, at the foot of its down flight",
                gp.currentRoom == f3.find("East Stairs") && atFoot(gp.currentRoom.map.stairs.downFoot));
        go(f2, "East Stairs", 7, 14); walk("U", 200);
        check("Floor 2 east left flight has collapsed: no way down at this end", gp.currentRoom == f2.find("East Stairs") && row() >= 14);
        go(f2, "West Stairs", 8, 14); walk("U", 200);
        check("Floor 2 west right flight has collapsed: no way up at this end", gp.currentRoom == f2.find("West Stairs") && row() >= 14);
        go(f2, "West Stairs", 5, 14); walk("U", 200);
        check("Floor 2 west left flight leads down to Floor 1", gp.currentRoom.floor.number == 1);
        go(f4, "West Stairs", 5, 14); walk("U", 200); go(f3, "East Stairs", 7, 14); walk("U", 200);
        check("all the way down: Floor 4 (west) -> 3 (east) -> 2", gp.currentRoom.floor == f2);

        System.out.println("3. Along the Floor 2 hallway");
        go(f2, "West Stairs", 15, 9);
        String[] order = {"Hallway (Chemistry Lab)", "Hallway (Biology Lab)", "Hallway (Containment Lab)", "East Stairs"};
        for (String next : order) { walk("R", 600); check("-> " + next, in(next)); }
        walk("R", 600);
        check("the building's east wall stops you", in("East Stairs"));

        System.out.println("4. Lab doors (working doors open both ways, sealed ones block both ways)");
        doorTest(f2, "Hallway (Chemistry Lab)", "Chemistry Lab", 0, true);
        doorTest(f2, "Hallway (Chemistry Lab)", "Chemistry Lab", 1, false);
        doorTest(f2, "Hallway (Biology Lab)", "Biology Lab", 0, true);
        doorTest(f2, "Hallway (Biology Lab)", "Biology Lab", 1, true);
        doorTest(f2, "Hallway (Containment Lab)", "Containment Lab", 0, false);
        doorTest(f2, "Hallway (Containment Lab)", "Containment Lab", 1, true);

        System.out.println("5. Inside the labs");
        go(f2, "Chemistry Lab", 12, 10);
        check("labs are single screens: no left/right links, camera fixed", gp.currentRoom.left == null && gp.currentRoom.right == null && gp.cameraX() == 0);
        go(f2, "Chemistry Lab", 12, 10); walk("U", 300);
        check("Chemistry: benches block the way up the middle", in("Chemistry Lab") && row() >= 9);
        go(f2, "Containment Lab", 13, 6); walk("D", 60);
        check("Containment: pods are solid", in("Containment Lab") && row() == 6);
        go(f2, "Containment Lab", 11, 9); walk("L", 30);
        check("Containment: the slime/glass trail can be walked over", in("Containment Lab") && col() < 11);

        System.out.println("6. Linked list integrity (Floor 2)");
        int count = 0; Room prev = null; boolean back = true;
        for (Room r = f2.head; r != null; r = r.right) { back &= r.left == prev; prev = r; count++; }
        check("hallway: 5 screens, every left link points back, tail is last", back && count == 5 && f2.size == 5 && f2.tail == prev);
        boolean doors = true;
        for (Room r = f2.head; r != null; r = r.right) if (r.door != null) doors &= r.door.door == r && r.door.floor == f2;
        check("every lab's door links both ways, and the labs belong to Floor 2", doors);

        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");

        System.exit(fails == 0 ? 0 : 1);
    }
}
