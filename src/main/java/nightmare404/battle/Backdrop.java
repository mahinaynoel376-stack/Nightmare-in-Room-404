package nightmare404.battle;

import java.awt.Graphics2D;

// The room behind a fight (the top part of the battle screen). Each monster says which one it's
// fought in (EnemyDef.backdrop). draw() is called every frame, clipped to the stage, so it can animate
// using battle.ticks (frames since the battle began) and battle.light (the flickering ceiling light, 0-1).
public interface Backdrop {
    void draw(Graphics2D g, BattleScreen.BattlePanel battle);
}
