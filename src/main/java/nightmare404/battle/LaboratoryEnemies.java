package nightmare404.battle;

import java.util.List;

import nightmare404.battle.BattleScreen.Attack;
import nightmare404.battle.BattleScreen.EnemyDef;

// The monsters of Floor 2, the labs (Laboratory in the Battle_Screens folder).
// Where each one stands in the game is set in nightmare404.world.Encounters.
public class LaboratoryEnemies {
    static final Backdrop LAB = new LabBackdrop();

    // ---- from Lab2: Anatomy Model (in the Biology Lab), Flask Slime (in the Chemistry Lab) ----

    public static final EnemyDef ANATOMYMODEL = new EnemyDef(
            "Anatomy Model",
            "anatomy_model",
            85,                                   
            "Biology Lab",
            "The laboratory was supposed to be abandoned after the accident. So why does someone keep turning on the lights?\n"
                    + "- Janitor's logbook, last entry",
            
                    List.of(
                    new Attack("Pointer Jab", 11, 0, 0, 6, "A sharp jab with the teacher's pointer."),
                    new Attack("Bone Rattle",  6, 2, 0, 4, "The rattling scatters your letters.")),
            LAB);
    public static final EnemyDef FLASKSLIME= new EnemyDef(
            "Flask Slime",
            "flask_slime",
            80,                                   
            "Chemistry Lab",
            "The laboratory was supposed to be abandoned after the accident. So why does someone keep turning on the lights?\n"
                    + "- Janitor's logbook, last entry",
            List.of(
                    new Attack("Acid Spit", 10, 0, 0, 6, "A glob of burning slime."),
                    new Attack("Ooze Over",  3, 3, 0, 4, "Slime oozes over your letters.")),
            LAB);

    // ---- from Lab3: Specimen 7 (in the Biology Lab), Lab Assistant (in the Chemistry Lab) ----

    public static final EnemyDef SPECIMEN7 = new EnemyDef(
            "Specimen 7",
            "specimen_7",
            90,                                   
            "Biology Lab",
            "Six frogs were dissected that day. The toe tag says there were seven.\n"
                    + "- Janitor's logbook, last entry",
            
                    List.of(
                    new Attack("Tongue Snap", 9, 0, 0, 6, "A sticky tongue lashes out."),
                    new Attack("Body Slam",   4, 2, 0, 3, "It leaps on you with its full weight.")),
            LAB);
    public static final EnemyDef LABASSISTANT= new EnemyDef(
            "Lab Assistant",
            "lab_assistant",
            85,                                   
            "Chemistry Lab",
            "It is still running the last experiment. You are the last experiment.\n"
                    + "- Janitor's logbook, last entry",
            List.of(
                    new Attack("Acid Spit", 9, 0, 0, 6, "A glob of burning slime."),
                    new Attack("Ooze Over",  3, 2, 0, 4, "Slime oozes over your letters.")),
            LAB);

    // ---- the boss, in the Containment Lab (was TestSubjectX) ----

    public static final EnemyDef TESTSUBJECTX = new EnemyDef(
            "Test Subject X",
            "LabExperiment",
            170,                                   
            "Containment Lab",
            "Students who entered the laboratory after dark were never recorded as missing. Their names simply disappeared from the attendance sheet.\n"
                    + "- Janitor's logbook, last entry",
            List.of(
                    
                    new Attack("Mutant Claw",  15,    0,     0,   7,      "A painful slash with its mutated claws."),
                    new Attack("Bio-Reconstruction",  0,    0,     9,   7,      "Rebuilds its damaged body using unstable cells."),
                    new Attack("Tentacle Whip",  8,    2,     0,   9,      "Swings its tentacles at the player with great force.")),
            LAB);
}
