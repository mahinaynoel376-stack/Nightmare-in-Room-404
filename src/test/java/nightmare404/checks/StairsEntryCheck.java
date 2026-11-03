package nightmare404.checks;

import nightmare404.entity.Player;
import nightmare404.main.GamePanel;

// The stairs can only be entered from their open end (the foot of each flight), never through the sides
public class StairsEntryCheck {
    static GamePanel gp; static Player p; static int fails = 0;
    static void walk(String k, int n) {
        gp.upPressed = k.equals("U"); gp.downPressed = k.equals("D"); gp.leftPressed = k.equals("L"); gp.rightPressed = k.equals("R");
        for (int i = 0; i < n; i++) p.update();
        gp.upPressed = gp.downPressed = gp.leftPressed = gp.rightPressed = false;
    }
    static void put(int col, int row) { p.x = col * 32; p.y = row * 32; p.update(); }
    static int hl() { return p.solidArea.x; } static int hr() { return p.solidArea.x + p.solidArea.width; }
    static int row() { return (p.solidArea.y + 8) / 32; }
    static void check(String what, boolean ok, String got) { System.out.println((ok ? "  ok   " : "  FAIL ") + what + "  [" + got + "]"); if (!ok) fails++; }
    public static void main(String[] a) {
        gp = new GamePanel(); p = gp.flor;
        gp.currentRoom = gp.world.floor4.head;   // West Stairs: staircase cols 9-13 (x 288-448), rows 7-13
        System.out.println("West Stairs (working)");
        put(16, 8); walk("L", 200);  check("landing from the right side: blocked", hl() >= 448, "hitbox x " + hl() + "-" + hr());
        put(16, 11); walk("L", 200); check("flight from the right side: blocked", hl() >= 448, "hitbox x " + hl() + "-" + hr());
        put(5, 8); walk("R", 200);   check("landing from the left side: blocked", hr() <= 288, "hitbox x " + hl() + "-" + hr());
        put(5, 12); walk("R", 200);  check("flight from the left side: blocked", hr() <= 288, "hitbox x " + hl() + "-" + hr());
        put(9, 15); walk("U", 200);  check("bottom end of the left flight: climbing it changes floor", gp.currentRoom.floor.number == 3, "floor " + gp.currentRoom.floor.number);
        gp.currentRoom = gp.world.floor4.head;
        put(13, 15); walk("U", 200); check("right flight (no Floor 5): rubble stops you at the bottom", row() >= 14, "row " + row());
        gp.currentRoom = gp.world.floor4.tail;  // East Stairs: staircase cols 11-15 (x 352-512)
        System.out.println("East Stairs (destroyed)");
        put(18, 11); walk("L", 200); check("side of the ruined stairs: blocked", hl() >= 512, "hitbox x " + hl());
        put(8, 9); walk("R", 200);   check("other side: blocked", hr() <= 352, "hitbox x " + hr());
        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }
}
