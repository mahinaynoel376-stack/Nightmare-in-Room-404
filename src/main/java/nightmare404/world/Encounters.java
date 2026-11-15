package nightmare404.world;

import nightmare404.battle.BattleScreen.EnemyDef;
import nightmare404.battle.FourthFloorEnemies;
import nightmare404.battle.LaboratoryEnemies;
import nightmare404.battle.LibraryEnemies;
import nightmare404.battle.LobbyEnemies;
import nightmare404.entity.Loot;
import nightmare404.entity.Monster;

// Where the monsters are. Every room has 2 (a few have 3), the starting room (Room 404) has none,
// and each floor has a BOSS. Beaten, a boss drops the key to its floor's way out (locked till then)
// and some supplies for the battles ahead:
//
//   Floor 4  BOSS the Brute, standing in the middle of the West Stairs down: no getting past it.
//            Drops the janitor's West Stairwell Key.
//   Floor 3  BOSS the Whisperer, in Library (East). Drops the East Stairwell Key, hidden in a book.
//   Floor 2  BOSS Test Subject X, in the Containment Lab. Drops a Security Keycard (the lab's
//            lockdown has shut the west stairwell).
//   Floor 1  BOSS the Principal, behind his counter. Drops the Master Key to the front doors.
//
//   Floor 4  Room 402: Desk Mimic, Chalk Wraith      Room 403: 2 Crawlers, Hall Monitor
//            (Room 404, where you start, is safe)
//   Floor 3  Library (Entrance): Librarian, Undead Scholar    Library (West): 2 Bookworms, Paper Golem
//            BOSS the Whisperer, in Library (East)            (the way down: the East Stairs)
//   Floor 2  Chemistry Lab: Flask Slime, Lab Assistant         Biology Lab: Specimen 7, Anatomy Model
//            BOSS Test Subject X, in the Containment Lab      (the way down: the West Stairs)
//   Floor 1  Faculty Room: Substitute, Leftovers               Records Room: 2 Shadow Stalkers, The Mascot
//            BOSS the Principal, in his office                (the way out: the main entrance)
//
// Each monster patrols a straight run along an aisle or a row of desks, given as {col, row} tiles:
// where it starts, then where it turns round. They're all at least 4 tiles from the doors, so walking
// into a room is never an instant fight (the library screens are walked into from their sides,
// so there it's 4 tiles from those edges). (Run EncounterCheck after changing anything here.)
public class Encounters {
    private final int ts;
    private final World world;

    private Encounters(World world, int ts) {
        this.world = world;
        this.ts = ts;
    }

    static void populate(World world, int ts) {
        Encounters e = new Encounters(world, ts);

        // ---------- Floor 4: the classrooms ----------
        Floor f4 = world.floor4;
        e.patrol(f4, "Room 402", FourthFloorEnemies.DESK_MIMIC, 4, 9, 19, 9);        // The middle aisle
        e.patrol(f4, "Room 402", FourthFloorEnemies.CHALK_WRAITH, 20, 6, 4, 6);      // Along the chalkboard
        e.patrol(f4, "Room 403", FourthFloorEnemies.CRAWLER, 7, 10, 21, 10);         // The middle aisle
        e.patrol(f4, "Room 403", FourthFloorEnemies.CRAWLER, 20, 6, 4, 6);           // Along the front
        e.patrol(f4, "Room 403", FourthFloorEnemies.HALL_MONITOR, 10, 6, 10, 13);    // Down the gap between the desks
        e.boss(f4, "West Stairs", FourthFloorEnemies.BRUTE, 5, 11, 16,               // On the stairs down, filling them
                new Loot("West Stairwell Key", "The Brute crumbles. In the rubble: the janitor's key, tagged 4F WEST STAIRS.",
                        1, 0, 1, 0),
                "The way down is chained shut. The janitor's key must be on this floor.");

        // ---------- Floor 3: the library ----------
        Floor f3 = world.floor3;
        e.patrol(f3, "Library (Entrance)", LibraryEnemies.LIBRARIAN, 13, 7, 19, 7);   // Past the shelves
        e.patrol(f3, "Library (Entrance)", LibraryEnemies.UNDEAD, 4, 6, 9, 6);        // Between the reading tables
        e.patrol(f3, "Library (West)", LibraryEnemies.BOOKWORM, 5, 10, 17, 10);       // Along the stacks
        e.patrol(f3, "Library (West)", LibraryEnemies.BOOKWORM, 16, 14, 4, 14);
        e.patrol(f3, "Library (West)", LibraryEnemies.PAPERGOLEM, 4, 6, 20, 6);
        e.boss(f3, "Library (East)", LibraryEnemies.WHISPERER, 19, 11, 0,             // The far end of the library
                new Loot("East Stairwell Key", "The whispering stops. A hollowed-out book falls open: the East Stairwell Key.",
                        1, 1, 0, 1),
                "The east stairwell is locked. Something in the library is keeping the key.");

        // ---------- Floor 2: the labs ----------
        Floor f2 = world.floor2;
        e.patrol(f2, "Chemistry Lab", LaboratoryEnemies.FLASKSLIME, 4, 10, 21, 10);    // Between the benches
        e.patrol(f2, "Chemistry Lab", LaboratoryEnemies.LABASSISTANT, 19, 6, 5, 6);    // Along the back wall
        e.patrol(f2, "Biology Lab", LaboratoryEnemies.SPECIMEN7, 4, 11, 21, 11);
        e.patrol(f2, "Biology Lab", LaboratoryEnemies.ANATOMYMODEL, 21, 8, 4, 8);
        e.boss(f2, "Containment Lab", LaboratoryEnemies.TESTSUBJECTX, 6, 9, 0,         // Among the broken pods
                new Loot("Security Keycard", "Test Subject X dissolves. Left in the slime: a keycard. LOCKDOWN OVERRIDE.",
                        2, 0, 1, 1),
                "LOCKDOWN. The west stairwell won't open without a security keycard.");

        // ---------- Floor 1: the offices ----------
        Floor f1 = world.floor1;
        e.patrol(f1, "Faculty Room", LobbyEnemies.SUBSTITUTE, 4, 7, 21, 7);           // In front of the board
        e.patrol(f1, "Faculty Room", LobbyEnemies.LEFTOVERS, 21, 10, 4, 10);           // Between the tables
        e.patrol(f1, "Records Room", LobbyEnemies.SHADOWSTALKER, 9, 6, 9, 12);         // Down the aisles
        e.patrol(f1, "Records Room", LobbyEnemies.SHADOWSTALKER, 16, 10, 16, 6);       //   between the shelves
        e.patrol(f1, "Records Room", LobbyEnemies.MASCOT, 4, 12, 13, 12);
        e.boss(f1, "Principal's Office", LobbyEnemies.PRINCIPAL, 10, 7, 0,             // Behind his counter
                new Loot("Master Key", "The Principal slumps over his desk. On his key ring: the school's Master Key.",
                        2, 1, 1, 1),
                "The front doors are locked. Only the Principal has the key.");
    }

    // A monster walking back and forth between (col0, row0), where it starts, and (col1, row1)
    private void patrol(Floor floor, String room, EnemyDef enemy, int col0, int row0, int col1, int row1) {
        add(new Monster(enemy, find(floor, room), false, ts, new int[] {col0, row0}, new int[] {col1, row1}));
    }

    // The floor's boss, standing guard at (col, row), shifted shiftX pixels to the right (16 = between
    // two tiles). Beaten, it drops loot, whose key opens the floor's way out; lockedNote is what the
    // player reads at the locked way out without it.
    private void boss(Floor floor, String room, EnemyDef enemy, int col, int row, int shiftX, Loot loot, String lockedNote) {
        Monster m = new Monster(enemy, find(floor, room), true, ts, new int[] {col, row}).nudge(shiftX);
        m.loot = loot;
        floor.boss = m;
        floor.exitKey = loot.key();
        floor.lockedNote = lockedNote;
        add(m);
    }

    private void add(Monster m) {
        m.room.monsters.add(m);
        world.monsters.add(m);
    }

    private static Room find(Floor floor, String name) {
        Room room = floor.find(name);
        if (room == null) throw new IllegalStateException("Encounters: no room called " + name + " on Floor " + floor.number);
        return room;
    }
}
