package nightmare404.battle;

import java.util.List;

import nightmare404.battle.BattleScreen.Attack;
import nightmare404.battle.BattleScreen.EnemyDef;

// The monsters outside, in the quadrangle: its trees (see nightmare404.entity.TreeMonster), Subject
// Zero and its saplings. They're fought out on the map (see LastStand), not in a battle screen: an
// attack's damage is what the player takes when the monster grabs them ("scratch" and "heal" aren't
// used there), and maxHp is how much typing it takes to bring it down.
public class QuadrangleEnemies {
    static final Backdrop QUADRANGLE = new QuadrangleBackdrop();

    public static final EnemyDef HOLLOW_OAK = new EnemyDef(
            "Hollow Oak",
            "treant",
            60,
            "QUADRANGLE",
            "The groundskeeper planted a tree for every student who never went home. Every spring there are more of them.\n"
                    + "- Janitor's logbook, last entry",
            List.of(
                    new Attack("Root Snare",   9, 0, 0, 5, "Roots burst out of the ground and drag at your ankles."),
                    new Attack("Branch Lash", 12, 0, 0, 4, "A heavy branch whips across the grass."),
                    new Attack("Leaf Storm",   6, 0, 0, 3, "A gust of rotten leaves, sharp as paper."),
                    new Attack("Drink Deep",   0, 0, 8, 1, "It sinks its roots and drinks. Its bark knits back together.")),
            QUADRANGLE);

    // The final boss, asleep by the south gate until every tree is down (see nightmare404.entity.SubjectZero).
    // Its sprites are made by tools/art/make_quest_art.py from Test Subject X's.
    public static final EnemyDef SUBJECT_ZERO = new EnemyDef(
            "Subject Zero",
            "subject_zero",
            240,
            "QUADRANGLE",
            "Subject Zero broke the glass of pod 2 on day 40. It was the first of them. It has been forgotten the longest.\n"
                    + "- Lab report, Project REMEMBER",
            List.of(
                    new Attack("Crushing Grip", 20, 0, 0, 3, "It closes every hand it has around you."),
                    new Attack("Many Mouths",   15, 0, 0, 2, "Every mouth bites at once.")),
            QUADRANGLE);

    // What Subject Zero calls up through the gates once it's hurt: a small, quick Hollow Oak
    public static final EnemyDef SAPLING = new EnemyDef(
            "Sapling",
            "treant",
            1,
            "QUADRANGLE",
            "",
            List.of(new Attack("Switch", 6, 0, 0, 1, "A whippy little branch, right across the shins.")),
            QUADRANGLE);
}
