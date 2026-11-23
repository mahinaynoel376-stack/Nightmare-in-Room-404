package nightmare404.entity;

import nightmare404.battle.BattleScreen.EnemyDef;
import nightmare404.world.Room;

// A monster that HUNTS: it comes straight for the player wherever they are in its room, sliding along
// anything in its way and stepping round it if it's stuck. The quadrangle's monsters are hunters: the
// trees (TreeMonster), Subject Zero and its saplings. They're fought out on the map (see
// battle.LastStand), so a hunter can also be STUNNED for a moment: when it grabs the player it reels
// back, and when a strike hits it, it flinches.
public abstract class Hunter extends Monster {
    private static final int STUCK_LIMIT = 20;      // Ticks without moving before it steps round what's in the way
    private static final int SIDESTEP = 36;         // ...for this many ticks

    public int stunned;                             // Ticks left standing still (reeling or flinching)
    private int stuck, sidestep, sideX, sideY;

    protected Hunter(EnemyDef enemy, Room room, boolean boss, int ts, int height, int[]... routeTiles) {
        super(enemy, room, boss, ts, height, routeTiles);
    }

    // Forgets how it was getting round things, and stops reeling (when it's put back in its place)
    protected void resetHunt() {
        stuck = sidestep = 0;
        stunned = 0;
    }

    // True while it's after the player (awake, not beaten): it can be struck, and it can grab them
    public abstract boolean hunting();

    // One step towards the player, `speed` pixels: along whichever way is further first, then the other;
    // if both are blocked for a while, step sideways round the obstacle for a bit
    protected void hunt(Player player, int speed) {
        chasing = true;
        if (stunned > 0) {
            stunned--;
            return;
        }
        int oldX = x, oldY = y;
        for (int moves = 0; moves < speed; moves++) {
            if (sidestep > 0) {
                if (!tryMove(sideX, sideY)) { sideX = -sideX; sideY = -sideY; }   // Blocked that way too: turn
                continue;
            }
            int dx = Integer.signum(player.x - x), dy = Integer.signum(player.y - y);
            boolean acrossFirst = Math.abs(player.x - x) >= Math.abs(player.y - y);
            boolean moved = acrossFirst ? tryMove(dx, 0) || tryMove(0, dy) : tryMove(0, dy) || tryMove(dx, 0);
            if (!moved) break;
        }
        if (sidestep > 0) {
            sidestep--;
        } else if (x == oldX && y == oldY) {
            if (++stuck > STUCK_LIMIT) {        // Step round: at right angles to the way it's trying to go
                boolean acrossFirst = Math.abs(player.x - x) >= Math.abs(player.y - y);
                int way = (phase + stuck) % 2 == 0 ? 1 : -1;
                sideX = acrossFirst ? 0 : way;
                sideY = acrossFirst ? way : 0;
                sidestep = SIDESTEP;
                stuck = 0;
            }
        } else {
            stuck = 0;
        }
    }

    private boolean tryMove(int dx, int dy) {
        if (dx == 0 && dy == 0) return false;
        x += dx;
        y += dy;
        syncHitbox();
        if (blocked()) {
            x -= dx;
            y -= dy;
            syncHitbox();
            return false;
        }
        return true;
    }

    // Its middle, in pixels: where strikes fly to
    public int centreX() {
        return x + 16;
    }

    public int centreY() {
        return y + 32 - drawnHeight() / 2;
    }
}
