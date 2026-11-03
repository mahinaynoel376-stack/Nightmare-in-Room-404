package nightmare404.checks;

import nightmare404.main.GamePanel;
import nightmare404.map.Library;
import nightmare404.world.Floor;

// Linking a room by a door to a map that has no door must be refused when the game starts,
// instead of crashing later when the player walks through it.
public class DoorSafetyCheck {
    static int fails = 0;

    static void check(String what, boolean ok) { System.out.println((ok ? "  ok   " : "  FAIL ") + what); if (!ok) fails++; }

    public static void main(String[] a) {
        GamePanel gp = new GamePanel();
        Floor f2 = gp.world.floor2;
        boolean refused = false;
        try {   // The library's west screen has no door, so linking it by a door must be refused
            f2.addRoomBehind("Doorless room", Library.build(gp.tiles, 32, Library.Part.WEST), f2.find("Hallway (Biology Lab)"));
        } catch (IllegalStateException e) {
            refused = true;
            System.out.println("  refused with: " + e.getMessage());
        }
        check("a door link to a map without a door is refused at start-up", refused);
        check("the existing link is untouched (Biology Lab)", f2.find("Hallway (Biology Lab)").door.name.equals("Biology Lab"));
        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }
}
