package nightmare404.checks;

import nightmare404.entity.Player;
import nightmare404.main.GamePanel;
import nightmare404.tile.MapBuilder;
import nightmare404.tile.TileMap;
import nightmare404.tile.TileSet;
import nightmare404.world.Floor;
import nightmare404.world.Room;

// Simulated play-through of the ground floor: stairs, offices, grass, both exits, and a test that the
// open exit is ready for the quadrangle (by attaching a stand-in outdoor screen, only inside this test)
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

    static void doorTest(Floor f, String hallway, String room, int doorCol, boolean works) {
        go(f, hallway, doorCol, 8); walk("U", 120);
        if (works) {
            check(hallway + ": door at cols " + doorCol + "-" + (doorCol + 1) + " -> " + room, in(room) && row() == 15);
            walk("D", 120);
            check("  and back out below the same door", in(hallway) && row() == 7 && col() >= doorCol && col() <= doorCol + 1);
        } else {
            check(hallway + ": broken door at cols " + doorCol + "-" + (doorCol + 1) + " is blocked", in(hallway) && row() == 7);
            go(f, room, doorCol, 14); walk("D", 120);
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
        go(f2, "West Stairs", 9, 14); walk("U", 200);
        check("Floor 2 left flight -> Floor 1, at the foot of its up flight", gp.currentRoom == f1.stairs && p.x == 400 && p.y == 448);
        walk("U", 200);
        check("Floor 1 right flight -> back up to Floor 2", gp.currentRoom == f2.stairs);
        go(f1, "West Stairs", 9, 14); walk("U", 200);
        check("Floor 1 left flight (no basement): blocked by rubble", gp.currentRoom == f1.stairs && row() >= 14);

        System.out.println("3. Along the ground floor hallway");
        go(f1, "West Stairs", 20, 9);
        for (String next : new String[] {"Hallway (Faculty Room)", "Hallway (Principal's Office)", "Hallway (Records Room)", "East Stairs (Exit)"}) {
            walk("R", 600); check("-> " + next, in(next));
        }
        go(f1, "Hallway (Principal's Office)", 12, 10); walk("D", 120);
        check("the low wall keeps you off the grass", in("Hallway (Principal's Office)") && row() == 11);

        System.out.println("4. Office doors");
        doorTest(f1, "Hallway (Faculty Room)", "Faculty Room", 5, true);
        doorTest(f1, "Hallway (Faculty Room)", "Faculty Room", 18, false);
        doorTest(f1, "Hallway (Principal's Office)", "Principal's Office", 5, true);
        doorTest(f1, "Hallway (Principal's Office)", "Principal's Office", 18, true);
        doorTest(f1, "Hallway (Records Room)", "Records Room", 5, false);
        doorTest(f1, "Hallway (Records Room)", "Records Room", 18, true);
        go(f1, "Principal's Office", 10, 12); walk("U", 200);
        check("Principal's Office: through the gap in the counter", in("Principal's Office") && row() <= 9);
        go(f1, "Principal's Office", 8, 12); walk("U", 200);
        check("Principal's Office: the counter blocks elsewhere", in("Principal's Office") && row() == 11);

        System.out.println("5. The exits");
        go(f1, "West Stairs", 5, 14); walk("D", 200);
        check("west exit is boarded up: can't get through", in("West Stairs") && row() == 15);
        check("  and it isn't a door", f1.find("West Stairs").map.nearestDoor(0) == null);
        Room east = f1.find("East Stairs (Exit)");
        check("east exit is a door (ready to link)", east.map.nearestDoor(0) != null);
        check("  it's the world's groundExit, linked to the Quadrangle", gp.world.groundExit == east && east.door == gp.world.quadrangle);
        go(f1, "East Stairs (Exit)", 18, 14); walk("D", 200);
        check("walking into the east exit now takes you outside, to the Quadrangle", gp.currentRoom == gp.world.quadrangle);

        System.out.println("6. Ready for the quadrangle: attach a stand-in outdoor screen, as the real one will be");
        MapBuilder b = new MapBuilder();
        b.fill(TileMap.LAYER_FLOOR, 0, 0, 24, 17, TileSet.GRASS_1);
        b.set(TileMap.LAYER_OBJECTS, 18, 0, TileSet.EXIT_L).set(TileMap.LAYER_OBJECTS, 19, 0, TileSet.EXIT_R);
        Room outside = f1.addRoomBehind("Stand-in Quadrangle", b.build(gp.tiles, 32), gp.world.groundExit);
        go(f1, "East Stairs (Exit)", 18, 14); walk("D", 200);
        check("walking out of the east exit -> the outside screen", gp.currentRoom == outside);
        check("  arrived just below its doorway", row() == 1 && col() >= 18 && col() <= 19);
        walk("U", 200);
        check("walking back in -> East Stairs, just above the exit", in("East Stairs (Exit)") && row() == 15 && col() >= 18 && col() <= 19);

        System.out.println("7. Linked list integrity (Floor 1)");
        int count = 0; Room prev = null; boolean back = true;
        for (Room r = f1.head; r != null; r = r.right) { back &= r.left == prev; prev = r; count++; }
        check("hallway: 5 screens, every left link points back, tail is last", back && count == 5 && f1.size == 5 && f1.tail == prev);

        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");

        System.exit(fails == 0 ? 0 : 1);
    }
}
