package nightmare404.battle;

import java.util.List;

import nightmare404.battle.BattleScreen.Attack;
import nightmare404.battle.BattleScreen.EnemyDef;

// The monsters of Floor 3, the library (Library in the Battle_Screens folder).
// Where each one stands in the game is set in nightmare404.world.Encounters.
public class LibraryEnemies {
    static final Backdrop LIBRARY = new LibraryBackdrop();

    // ---- Library (Entrance) (was Lib 1) ----

    public static final EnemyDef UNDEAD = new EnemyDef(
            "Undead Scholar",
            "undead",
            85,                                   
            "Library",
            "The library was closed decades ago. Yet every morning, a book was found open on a different page.\n"
                    + "- Janitor's logbook, last entry",
            List.of(
                    
                    new Attack("Page Whack",  9,    0,     0,   7,      "Swings its massive book towards you."),
                    new Attack("Book Bash",  6,    2,     0,   4,      "Slams its heavy book disabling two tiles and dealing damage")),
            LIBRARY);
    public static final EnemyDef LIBRARIAN= new EnemyDef(
            "Librarian",
            "librarian",
            90,                                   
            "Library",
            "She has worked here longer than the building has stood. She does not like noise.\n"
                    + "- Janitor's logbook, last entry",
            List.of(
                    new Attack("SHHHH!",         10, 0,  0, 5, "A shriek that rattles your skull."),
                    new Attack("Overdue Notice",  6, 2,  0, 3, "Fines you a few of your letters.")),
            LIBRARY);

    // ---- Library (West) (was Lib 2) ----

    public static final EnemyDef BOOKWORM = new EnemyDef(
            "Bookworm",
            "bookworm",
            85,                                   
            "Library",
            "It has eaten every overdue book in the building. It is still hungry.\n"
                    + "- Janitor's logbook, last entry",
            List.of(
                    new Attack("Page Bite", 10, 0, 0, 6, "Covers snap shut like jaws."),
                    new Attack("Ink Smear",  5, 2, 0, 4, "Smudges ink across your letters.")),
            LIBRARY);
    public static final EnemyDef PAPERGOLEM= new EnemyDef(
            "Paper Golem",
            "paper_golem",
            90,                                   
            "Library",
            "Built from every page anyone ever tore out of a library book.\n"
                    + "- Janitor's logbook, last entry",
             List.of(
                    new Attack("Hardcover Punch", 11, 0, 0, 6, "A fist made of a very heavy book."),
                    new Attack("Paper Cuts",       8, 0, 0, 4, "A storm of sharp pages.")),
            LIBRARY);

    // ---- the boss, in Library (East) (was Whisperer) ----

    public static final EnemyDef WHISPERER = new EnemyDef(
            "Whisperer",
            "whisperer",
            100,                                   
            "Library",
            "The silence was comforting, until it started whispering back.\n"
                    + "- Janitor's logbook, last entry",
            List.of(
                    
                    new Attack("Echoing Scream",  15,    0,     0,   6,      "Releases a horrifying scream."),
                    new Attack("Soul Feast",  0,    0,     9,   4,      "Feeds on fear to recover HP."),
                    new Attack("Cursed Murmur",  5,    2,     0,   4,      "Places a curse on the player, and locks 2 tiles.")),
            LIBRARY);
}
