package nightmare404.battle;

import java.util.List;

import nightmare404.battle.BattleScreen.Attack;
import nightmare404.battle.BattleScreen.EnemyDef;

// The monsters of Floor 1, the offices (Lobby in the Battle_Screens folder).
// Where each one stands in the game is set in nightmare404.world.Encounters.
public class LobbyEnemies {
    static final Backdrop STAFF_ROOM = new StaffRoomBackdrop();
    static final Backdrop STORAGE_ROOM = new StorageRoomBackdrop();
    static final Backdrop PRINCIPALS_OFFICE = new PrincipalsOfficeBackdrop();

    // ---- the Faculty Room (was StaffRoom) ----

    public static final EnemyDef LEFTOVERS= new EnemyDef(
            "Leftovers",
            "leftovers",
            90,                                   
            "Faculty Room",
            "The note on the lid says DON'T EAT. Nobody ever asked whether it would eat you.\n"
                    + "- Janitor's logbook, last entry",
            List.of(
                    new Attack("Mold Bite",  13, 0,  0, 6, "Sharp, fuzzy, and very old."),
                    new Attack("Spore Puff",  6, 2,  0, 3, "A cloud of spores settles on your letters."),
                    new Attack("Reheat",      0, 0, 10, 2, "Sizzles back together.")),
            STAFF_ROOM);
    public static final EnemyDef SUBSTITUTE = new EnemyDef(
            "Substitute",
            "substitute",
            95,                                   
            "Faculty Room",
            "She's covering for your teacher today. Your teacher has been gone for thirty years.\n"
                    + "- Janitor's logbook, last entry",
           List.of(
                    new Attack("Roll Call", 14, 0, 0, 6, "She calls your name. It hurts."),
                    new Attack("Pop Quiz",   5, 3, 0, 4, "Surprise! Some letters are marked wrong.")),
            STAFF_ROOM);

    // ---- the Records Room (was StockRoom) ----

    public static final EnemyDef SHADOWSTALKER = new EnemyDef(
            "Shadow Stalker",
            "shadow",
            95,                                   
            "Records Room",
            "Students who stayed late in Room 404 were never marked absent. They were never marked anything at all.\n"
                    + "- Janitor's logbook, last entry",
            List.of(
                    
                    new Attack("Shadow Swipe",  10,    0,     0,   7,      "Slashes you with its sharp shadow cloak."),
                    new Attack("Shadow Strike",  8,    0,     0,   8,      "Launches a powerful strike.")),
            STORAGE_ROOM);
    public static final EnemyDef MASCOT = new EnemyDef(
            "The Mascot", 
            "mascot", 
            90, 
            "Records Room",
            "Nobody has worn the costume since the homecoming game. Nobody has taken it off either.\n"
            + "- Janitor's logbook, last entry", 
            
            List.of(
                    new Attack("Big Hug",     11, 0,  0, 6, "A crushing, fuzzy embrace."),
                    new Attack("Team Spirit",  0, 0, 10, 2, "Does a little dance and feels better.")),
            STORAGE_ROOM);

    // ---- the boss, in the Principal's Office (was Principals office) ----

    public static final EnemyDef PRINCIPAL = new EnemyDef(
            "Principal",
            "principal",
            200,                                   
            "Principals Office",
            "Your permanent record is on his desk. He has been waiting a very long time to discuss it.\n"
                    + "- Janitor's logbook, last entry",
            List.of(
                    
                    new Attack("Paddle",       15, 0,  0, 7, "An old-fashioned punishment."),
                    new Attack("Detention",     8, 4,  0, 3, "Your letters are kept after class."),
                    new Attack("Expel",        20, 0,  0, 1, "Get. Out."),
                    new Attack("Coffee Break",  0, 0, 15, 1, "He sips his coffee and composes himself.")),
            PRINCIPALS_OFFICE);
}
