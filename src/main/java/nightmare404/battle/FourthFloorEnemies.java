package nightmare404.battle;

import java.util.List;

import nightmare404.battle.BattleScreen.Attack;
import nightmare404.battle.BattleScreen.EnemyDef;

// The monsters of Floor 4, the classrooms (Fourth_Floor in the Battle_Screens folder).
// Where each one stands in the game is set in nightmare404.world.Encounters.
public class FourthFloorEnemies {
    static final Backdrop CLASSROOM = new ClassroomBackdrop();

    // ---- The first fight: it drops on Kai the first time he steps out of Room 404 (see FirstBlob) ----
    // A weak blob, so the first battle (with the controls explained) is a gentle one. Its pictures are
    // made by tools/art/make_ceiling_blob.py.

    public static final EnemyDef CEILING_BLOB = new EnemyDef(
            "Ceiling Blob",
            "ceiling_blob",
            40,
            "4F HALLWAY",
            "It clung to the ceiling outside Room 404 and waited for someone to come out. It waited a long time.\n"
                    + "- Janitor's logbook, last entry",
            List.of(new Attack("Goo Splat", 5, 0, 0, 1, "A cold glob of goo smacks into you.")),
            CLASSROOM);

    // ---- Room 402 (was 1stRoom) ----

    public static final EnemyDef DESK_MIMIC = new EnemyDef(
            "Desk Mimic", 
            "desk_mimic",
            80,                                   
            "ROOM 402",
            "Whoever sat in the back row left their homework inside. They never came back for it.",
            List.of(
                    new Attack("Pencil Bite",   9, 0, 0, 6, "Sharpened pencils for teeth."),
                    new Attack("Homework Spit",  8, 2, 0, 4, "Soggy homework sticks to your letters.")),
            CLASSROOM);
    public static final EnemyDef CHALK_WRAITH = new EnemyDef(
        "Chalk Wraith", 
        "chalk_wraith",
            85,                                   
            "ROOM 402",
            "Every morning the board is wiped clean. Every night, someone writes on it again.",
            List.of(
                    new Attack("Screech",    8, 0,  0, 5, "Like nails down a chalkboard."),
                    new Attack("Erase",       4, 2,  0, 3, "Wipes some of your letters away."),
                    new Attack("Dust Cloud",  0, 0, 6, 2, "Gathers loose chalk dust to mend itself.")),
            CLASSROOM);

    // ---- Room 403 (was 2ndRoom) ----

    public static final EnemyDef CRAWLER = new EnemyDef(
            "Crawler",
            "hallwaycrawler",
            75,                                   
            "ROOM 403",
            "The school closed at midnight. So why were there still footsteps echoing through the fourth floor?\n"
                    + "- Janitor's logbook, last entry",
            List.of(
                    
                    new Attack("Slime Splat",  7,    0,     0,   7,      "The crawler launches a glob of sticky goo."),
                    new Attack("Healing Ooze",  0,    0,     8,   7,      "The crawler heals itself.")),
            CLASSROOM);
    public static final EnemyDef HALL_MONITOR = new EnemyDef(
            "Hall Monitor",
            "hall_monitor",
            80,                                   
            "ROOM 403",
            "The school closed at midnight. So why were there still footsteps echoing through the fourth floor?\n"
                    + "- Janitor's logbook, last entry",
            
            List.of(
                    new Attack("Long Reach",  9,    0,     0,   6,     "Grasping fingers from too far away."),
                    new Attack("Write-Up",     6,    3,     0,   4,     "Scribbles over your letters.")),
            CLASSROOM);

    // ---- the boss, at the West Stairs (was Brute) ----

    public static final EnemyDef BRUTE = new EnemyDef(
            "Brute",
            "brute3",
            140,                                   
            "WEST STAIRS",
            "Students who stayed late in Room 404 were never marked absent. They were never marked anything at all.\n"
                    + "- Janitor's logbook, last entry",
            List.of(
                    
                    new Attack("Rock Throw",  13,    0,     0,   7,      "A heavy, crushing blow."),
                    new Attack("Rocky Regeneration ",  0,    0,     10,   7,      "The Brute absorbs nearby stones to repair its body."),
                    new Attack("Earthquake Stomp",  8,    3,     0,   7,      "The Brute stomps on the ground. breaking tiles.")),
            CLASSROOM);
}
