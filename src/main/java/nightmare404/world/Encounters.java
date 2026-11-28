package nightmare404.world;

import nightmare404.battle.BattleScreen.EnemyDef;
import nightmare404.battle.FourthFloorEnemies;
import nightmare404.battle.LaboratoryEnemies;
import nightmare404.battle.LibraryEnemies;
import nightmare404.battle.LobbyEnemies;
import nightmare404.battle.QuadrangleEnemies;
import nightmare404.entity.Loot;
import nightmare404.entity.SleepingBoss;
import nightmare404.entity.FirstBlob;
import nightmare404.entity.Haze;
import nightmare404.entity.Whisperer;
import nightmare404.entity.Monster;
import nightmare404.entity.SubjectZero;
import nightmare404.entity.TreeMonster;
import nightmare404.map.Quadrangle;

// Where the monsters are. Every room has 2 (a few have 3), the starting room (Room 404) has none,
// and each floor has a BOSS. Beaten, a boss drops the key to its floor's way out (locked till then)
// and some supplies for the battles ahead:
//
//   Floor 4  BOSS the Brute, standing in the middle of the West Stairs down: no getting past it.
//            Asleep as a boulder until Kai reads it the janitor's pages; beaten, it clears the way down.
//   Floor 3  BOSS the Whisperer, in Library (East): intangible until Kai brings her the three checkout
//            cards; beaten, she drops them with every name filled in, and the haze over the east stairs clears.
//   Floor 2  BOSS Test Subject X, in the Containment Lab. Drops a Security Keycard (the lab's
//            lockdown has shut the west stairwell).
//   Floor 1  BOSS the Principal, behind his counter, in an office that stays locked until Kai has found
//            the floor's three books. Drops the Master Key to the front doors.
//
// Most hallway screens have ONE monster too, walking up and down the corridor. Not the one in front
// of Room 404 (where you start), the library's entrance or the main entrance, nor the stairwells:
//   Floor 4  a Crawler (Room 402), the Hall Monitor (Room 403)
//   Floor 3  an Undead (Library West), a Bookworm (Library East)
//   Floor 2  a Flask Slime (Chemistry Lab), Specimen 7 (Biology Lab)
//   Floor 1  a Shadow Stalker (Faculty Room), the Substitute (Records Room)
// They patrol rows 9-10, the middle of the corridor, which the clutter leaves clear (see Hallway.scatter).
// There's room to slip past along the wall, but not without them noticing.
//
// Outside, every tree in the quadrangle (six of them) is a Hollow Oak, asleep until the player comes
// out. Subject Zero, the FINAL BOSS, sleeps by the south gate until they're all down. They're all
// fought out on the map, not in a battle screen (see TreeMonster, SubjectZero and battle.LastStand).
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
    private static final String CONTAINMENT_KEYCARD = "Containment Keycard";
    private static final String APPOINTMENT = "An appointment with the Principal";

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
        // The first fight: a blob that drops on Kai from the ceiling the first time he steps out of
        // Room 404 (the Director starts it; see FirstBlob). It's not one of the room's monsters: it never
        // walks a round, and once beaten it stays beaten, even if the player gives up later on this floor.
        world.firstFight = new FirstBlob(FourthFloorEnemies.CEILING_BLOB, e.find(f4, "Hallway (Room 404)"), ts);
        e.patrol(f4, "Room 402", FourthFloorEnemies.DESK_MIMIC, 4, 9, 19, 9);        // The middle aisle
        e.patrol(f4, "Room 402", FourthFloorEnemies.CHALK_WRAITH, 20, 6, 4, 6);      // Along the chalkboard
        e.patrol(f4, "Room 403", FourthFloorEnemies.CRAWLER, 7, 10, 21, 10);         // The middle aisle
        e.patrol(f4, "Room 403", FourthFloorEnemies.CRAWLER, 20, 6, 4, 6);           // Along the front
        e.patrol(f4, "Room 403", FourthFloorEnemies.HALL_MONITOR, 10, 6, 10, 13);    // Down the gap between the desks
        e.hallway(f4, "Hallway (Room 402)", FourthFloorEnemies.CRAWLER, 4, 10, 12, 10);
        e.hallway(f4, "Hallway (Room 403)", FourthFloorEnemies.HALL_MONITOR, 10, 9, 4, 9);
        // On the stairs down, filling them: asleep as a boulder until Kai reads it the janitor's logbook.
        // It drops nothing: beating it clears the way down.
        e.bossInTheWay(f4, new SleepingBoss(FourthFloorEnemies.BRUTE, find(f4, "West Stairs"), ts, 5, 11, "_boulder", true), 16, null,
                "The boulder blocks the way down.");

        // ---------- Floor 3: the library ----------
        Floor f3 = world.floor3;
        e.patrol(f3, "Library (Entrance)", LibraryEnemies.LIBRARIAN, 13, 7, 19, 7);   // Past the shelves
        e.patrol(f3, "Library (Entrance)", LibraryEnemies.UNDEAD, 4, 6, 9, 6);        // Between the reading tables
        e.patrol(f3, "Library (West)", LibraryEnemies.BOOKWORM, 5, 10, 17, 10);       // Along the stacks
        e.patrol(f3, "Library (West)", LibraryEnemies.BOOKWORM, 16, 14, 4, 14);
        e.patrol(f3, "Library (West)", LibraryEnemies.PAPERGOLEM, 4, 6, 20, 6);
        e.hallway(f3, "Hallway (Library West)", LibraryEnemies.UNDEAD, 5, 9, 12, 9);
        e.hallway(f3, "Hallway (Library East)", LibraryEnemies.BOOKWORM, 12, 10, 4, 10);
        // At the far end of the library, still reading out the names, too faint to touch until Kai brings
        // her the three checkout cards (see Whisperer). Beaten, she drops the cards with every name filled
        // in, and the haze over the east stairs (the way down) clears.
        e.bossInTheWay(f3, new Whisperer(LibraryEnemies.WHISPERER, find(f3, "Library (East)"), ts, 19, 11), 0,
                new Loot("The three checkout cards", "The whispering stops for good. Three checkout cards drift down "
                        + "where she floated, and every blank is filled in: every name, written out in full.", 1, 1, 0, 1,
                        Loot.Look.CARDS),
                "The haze won't let you through.");
        Room eastStairs = find(f3, "East Stairs");
        eastStairs.haze = new Haze(eastStairs);

        // ---------- Floor 2: the labs ----------
        Floor f2 = world.floor2;
        e.patrol(f2, "Chemistry Lab", LaboratoryEnemies.FLASKSLIME, 4, 10, 21, 10);    // Between the benches
        // The Lab Assistant carries the keycard to the Containment Lab, which is locked
        e.patrol(f2, "Chemistry Lab", LaboratoryEnemies.LABASSISTANT, 19, 6, 5, 6).loot =   // Along the back wall
                new Loot(CONTAINMENT_KEYCARD, "The Lab Assistant slumps over a bench. Clipped to its coat: a keycard "
                        + "marked CONTAINMENT.", 0, 0, 0, 0);
        Room containment = find(f2, "Containment Lab");
        containment.lockedWith = CONTAINMENT_KEYCARD;
        containment.lockedNote = "The Containment Lab is locked. LAB STAFF ONLY.";
        e.patrol(f2, "Biology Lab", LaboratoryEnemies.SPECIMEN7, 4, 11, 21, 11);
        e.patrol(f2, "Biology Lab", LaboratoryEnemies.ANATOMYMODEL, 21, 8, 4, 8);
        e.hallway(f2, "Hallway (Chemistry Lab)", LaboratoryEnemies.FLASKSLIME, 4, 10, 12, 10);
        e.hallway(f2, "Hallway (Biology Lab)", LaboratoryEnemies.SPECIMEN7, 10, 9, 4, 9);
        // Still sealed in the last whole pod, in the middle of the lab (see SleepingBoss). Optional: the
        // stairs down are open anyway. Beaten, it leaves one of its tentacles, which calms its brother,
        // Subject Zero, in the quadrangle (see SubjectZero.CALMING_ITEM).
        e.optionalBoss(f2, new SleepingBoss(LaboratoryEnemies.TESTSUBJECTX, containment, ts, 12, 10, "_pod", false), 16,
                new Loot(SubjectZero.CALMING_ITEM, "Test Subject X dissolves. Left in the slime: one of its tentacles, "
                        + "still twitching, with a tag tied round it: SUBJECT X. SIBLING: SUBJECT ZERO.", 2, 0, 1, 1,
                        Loot.Look.TENTACLE));

        // ---------- Floor 1: the offices ----------
        Floor f1 = world.floor1;
        e.patrol(f1, "Faculty Room", LobbyEnemies.SUBSTITUTE, 4, 7, 21, 7);           // In front of the board
        e.patrol(f1, "Faculty Room", LobbyEnemies.LEFTOVERS, 21, 10, 4, 10);           // Between the tables
        e.patrol(f1, "Records Room", LobbyEnemies.SHADOWSTALKER, 9, 6, 9, 12);         // Down the aisles
        e.patrol(f1, "Records Room", LobbyEnemies.SHADOWSTALKER, 16, 10, 16, 6);       //   between the shelves
        e.patrol(f1, "Records Room", LobbyEnemies.MASCOT, 4, 12, 13, 12);
        e.hallway(f1, "Hallway (Faculty Room)", LobbyEnemies.SHADOWSTALKER, 12, 10, 4, 10);
        e.hallway(f1, "Hallway (Records Room)", LobbyEnemies.SUBSTITUTE, 6, 9, 12, 9);
        // He doesn't see students who haven't done their homework: his office is locked until Kai has
        // found the floor's three books and shown he knows what the Principal did (see Director.doorLocked)
        Room office = find(f1, "Principal's Office");
        office.lockedWith = APPOINTMENT;
        office.opensWhenQuestDone = true;
        e.boss(f1, "Principal's Office", LobbyEnemies.PRINCIPAL, 10, 7, 0,             // Behind his counter
                new Loot("Master Key", "The Principal slumps over his desk. On his key ring: the school's Master Key.",
                        2, 1, 1, 1),
                "The front doors are locked. Only the Principal has the key.");

        // ---------- Outside: the quadrangle's trees ----------
        for (java.awt.Point tree : Quadrangle.trees()) {
            e.add(new TreeMonster(QuadrangleEnemies.HOLLOW_OAK, world.quadrangle, ts, tree.x, tree.y));
        }
        // At the end of the slime trail from the labs, in front of the south gate
        world.finalBoss = new SubjectZero(QuadrangleEnemies.SUBJECT_ZERO, world.quadrangle, ts,
                Quadrangle.ZERO_COL, Quadrangle.ZERO_ROW);
        e.add(world.finalBoss);
    }

    // A monster walking back and forth between (col0, row0), where it starts, and (col1, row1)
    private Monster patrol(Floor floor, String room, EnemyDef enemy, int col0, int row0, int col1, int row1) {
        Monster m = new Monster(enemy, find(floor, room), false, ts, new int[] {col0, row0}, new int[] {col1, row1});
        add(m);
        return m;
    }

    // A monster patrolling a hallway screen. It's the same monster as in the rooms, but the sign
    // over its battle says where the fight is: "4F HALLWAY".
    private void hallway(Floor floor, String screen, EnemyDef enemy, int col0, int row0, int col1, int row1) {
        EnemyDef here = new EnemyDef(enemy.name(), enemy.sprite(), enemy.maxHp(), floor.number + "F HALLWAY",
                enemy.notes(), enemy.attacks(), enemy.backdrop());
        patrol(floor, screen, here, col0, row0, col1, row1);
    }

    // The floor's boss, standing guard at (col, row), shifted shiftX pixels to the right (16 = between
    // two tiles). Beaten, it drops loot, whose key opens the floor's way out; lockedNote is what the
    // player reads at the locked way out without it.
    private void boss(Floor floor, String room, EnemyDef enemy, int col, int row, int shiftX, Loot loot, String lockedNote) {
        boss(floor, new Monster(enemy, find(floor, room), true, ts, new int[] {col, row}), shiftX, loot, lockedNote);
    }

    // A boss with no key to drop: the floor's way down opens as soon as it's beaten (the Brute, which
    // fills the stairs itself, and the Whisperer, whose haze covers them). loot may be null: nothing dropped.
    private void bossInTheWay(Floor floor, Monster m, int shiftX, Loot loot, String lockedNote) {
        m.nudge(shiftX);
        m.loot = loot;
        floor.boss = m;
        floor.openWhenBossBeaten = true;
        floor.lockedNote = lockedNote;
        add(m);
    }

    // A boss the player doesn't have to fight: it keeps no way shut (Test Subject X, whose tentacle only
    // makes the last fight easier)
    private void optionalBoss(Floor floor, Monster m, int shiftX, Loot loot) {
        m.nudge(shiftX);
        m.loot = loot;
        floor.boss = m;
        add(m);
    }

    // The same, for a boss that's its own kind of monster
    private void boss(Floor floor, Monster m, int shiftX, Loot loot, String lockedNote) {
        m.nudge(shiftX);
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
