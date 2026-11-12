package nightmare404.checks;

import nightmare404.entity.Player;
import nightmare404.main.GamePanel;
import nightmare404.map.Hallway;
import nightmare404.world.Floor;
import nightmare404.world.Room;

// Simulated play-through of the ground floor: stairs, offices, grass, the boarded-up side exits and
// the main entrance out to the quadrangle
public class Floor1Check {
    static GamePanel gp; static Player p; static int fails = 0;

    static void check(String what, boolean ok) { System.out.println((ok ? "  ok   " : "  FAIL ") + what + "   [" + at() + "]"); if (!ok) fails++; }
    static String at() { return "Floor " + gp.currentRoom.floor.number + ", " + gp.currentRoom.name + ", col " + col() + " row " + row(); }
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

    // Walks up into hallway door number d (0 = left, 1 = right), which leads to the office's door number d
    static void doorTest(Floor f, String hallway, String room, int d, boolean works) {
        int hallCol = Hallway.HALL_DOOR_COLS[d], roomCol = Hallway.ROOM_DOOR_COLS[d];
        go(f, hallway, hallCol, 8); walk("U", 120);
        if (works) {
            check(hallway + ": door at cols " + hallCol + "-" + (hallCol + 1) + " -> " + room + ", above the same door",
                    in(room) && row() == 15 && col() >= roomCol && col() <= roomCol + 1);
            walk("D", 120);
            check("  and back out below the same door", in(hallway) && row() == 7 && col() >= hallCol && col() <= hallCol + 1);
        } else {
            check(hallway + ": broken door at cols " + hallCol + "-" + (hallCol + 1) + " is blocked", in(hallway) && row() == 7);
            go(f, room, roomCol, 14); walk("D", 120);
            check("  blocked from inside too", in(room) && row() == 15);
        }
    }

    public static void main(String[] a) {
        gp = new GamePanel(); p = gp.flor;
        Floor f2 = gp.world.floor2, f1 = gp.world.floor1;

        System.out.println("1. Floors: 4 <-> 3 <-> 2 <-> 1");
        check("floor2.below == floor1, floor1.above == floor2, nothing below Floor 1", f2.below == f1 && f1.above == f2 && f1.below == null);
        int n = 0; for (Floor f = gp.world.top; f != null; f = f.below) n++;
        check("walking the floor list from the top visits 4 floors", n == 4);

        System.out.println("2. Stairs");
        go(f2, "West Stairs", 5, 14); walk("U", 200);
        check("Floor 2 west left flight -> Floor 1's West Stairs, at the foot of its up flight",
                gp.currentRoom == f1.find("West Stairs") && atFoot(gp.currentRoom.map.stairs.upFoot));
        walk("U", 200);
        check("Floor 1 right flight -> back up to Floor 2's West Stairs", gp.currentRoom == f2.find("West Stairs"));
        go(f1, "West Stairs", 5, 14); walk("U", 200);
        check("Floor 1 left flight (no basement): blocked by rubble", in("West Stairs") && row() >= 14);
        go(f1, "East Stairs", 10, 14); walk("U", 200);
        check("Floor 1 east right flight has collapsed: blocked", in("East Stairs") && row() >= 14);

        System.out.println("3. Along the ground floor hallway");
        go(f1, "West Stairs", 15, 9);
        for (String next : new String[] {"Hallway (Faculty Room)", "Hallway (Principal's Office)", "Hallway (Records Room)", "East Stairs"}) {
            walk("R", 600); check("-> " + next, in(next));
        }
        go(f1, "Hallway (Principal's Office)", 12, 10); walk("D", 120);
        check("the low wall keeps you off the grass", in("Hallway (Principal's Office)") && row() == 11);

        System.out.println("4. Office doors");
        doorTest(f1, "Hallway (Faculty Room)", "Faculty Room", 0, true);
        doorTest(f1, "Hallway (Faculty Room)", "Faculty Room", 1, false);
        doorTest(f1, "Hallway (Principal's Office)", "Principal's Office", 0, true);
        doorTest(f1, "Hallway (Principal's Office)", "Principal's Office", 1, true);
        doorTest(f1, "Hallway (Records Room)", "Records Room", 0, false);
        doorTest(f1, "Hallway (Records Room)", "Records Room", 1, true);
        go(f1, "Principal's Office", 10, 12); walk("U", 200);
        check("Principal's Office: through the gap in the counter", in("Principal's Office") && row() <= 9);
        go(f1, "Principal's Office", 8, 12); walk("U", 200);
        check("Principal's Office: the counter blocks elsewhere", in("Principal's Office") && row() == 11);

        System.out.println("5. The way out: both side exits are boarded up, the main entrance is in the middle");
        go(f1, "West Stairs", 3, 14); walk("D", 200);
        check("west exit is boarded up: can't get through", in("West Stairs") && row() == 15);
        check("  and it isn't a door", f1.find("West Stairs").map.doorCount() == 0);
        go(f1, "East Stairs", 12, 14); walk("D", 200);
        check("east exit is boarded up too", in("East Stairs") && row() == 15);
        check("  and it isn't a door", f1.find("East Stairs").map.doorCount() == 0);
        Room middle = f1.find("Hallway (Principal's Office)"), quad = gp.world.quadrangle;
        int entrance = middle.map.doorsInBottomWall().get(0);
        check("the main entrance is in the middle hallway screen (the world's groundExit)", gp.world.groundExit == middle);
        check("  4 tiles wide: twice a side exit", middle.map.door(entrance).width == 4 * 32);
        check("  it leads to the Quadrangle, and the Quadrangle's doorway leads back",
                middle.doorLeadsTo(entrance) == quad && quad.doorLeadsTo(0) == middle && quad.door == middle);
        check("  the screen's other doors still lead to the Principal's Office", middle.door == f1.find("Principal's Office")
                && middle.doorLeadsTo(middle.map.doorsInTopWall().get(0)) == middle.door);
        go(f1, "Hallway (Principal's Office)", Hallway.ENTRANCE_COL + 1, 10); walk("D", 200);
        check("walking into the main entrance takes you outside, to the Quadrangle", gp.currentRoom == quad);
        walk("U", 200);
        check("  and walking back in -> the middle hallway, just above the entrance", in("Hallway (Principal's Office)")
                && row() == 11 && col() >= Hallway.ENTRANCE_COL && col() <= Hallway.ENTRANCE_COL + 3);

        System.out.println("6. Linked list integrity (Floor 1)");
        int count = 0; Room prev = null; boolean back = true;
        for (Room r = f1.head; r != null; r = r.right) { back &= r.left == prev; prev = r; count++; }
        check("hallway: 5 screens, every left link points back, tail is last", back && count == 5 && f1.size == 5 && f1.tail == prev);

        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");

        System.exit(fails == 0 ? 0 : 1);
    }
}
