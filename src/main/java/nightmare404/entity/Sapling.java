package nightmare404.entity;

import nightmare404.battle.BattleScreen.EnemyDef;
import nightmare404.world.Room;

// A sapling: a small, quick Hollow Oak that Subject Zero calls up through the quadrangle's gates
// once it's been hurt (see battle.LastStand). One short word fells it. It isn't in the room's list of
// monsters: it only exists during the fight with Subject Zero, and going down clears them all away.
public class Sapling extends Hunter {
    public static final int HUNT_SPEED = 3;          // As fast as the trees
    private static final int SIZE = 44;              // Half a tree

    // Coming in at (col, row) on the room's map (just inside a gate)
    public Sapling(EnemyDef enemy, Room room, int ts, int col, int row) {
        super(enemy, room, false, ts, SIZE, new int[] {col, row});
        chasing = true;
    }

    @Override
    public boolean hunting() {
        return !defeated;
    }

    @Override
    public void update(Player player) {
        if (!hunting() || player == null) return;
        hunt(player, HUNT_SPEED);
    }

    @Override
    public boolean touches(Player player) {
        return hunting() && super.touches(player);
    }
}
