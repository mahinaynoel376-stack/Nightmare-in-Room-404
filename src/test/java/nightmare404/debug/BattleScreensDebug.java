package nightmare404.debug;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

import nightmare404.battle.BattleScreen;
import nightmare404.battle.BattleScreen.EnemyDef;
import nightmare404.battle.FourthFloorEnemies;
import nightmare404.battle.LaboratoryEnemies;
import nightmare404.battle.LibraryEnemies;
import nightmare404.battle.LobbyEnemies;
import nightmare404.battle.QuadrangleEnemies;
import nightmare404.battle.TypingBattle;

// Debug tool: draws the battle screen of every monster into a PNG (a few seconds into the fight,
// after the fighters have walked in), so you can check the backgrounds and sprites without playing.
// Run: in Eclipse, Run As > Java Application. Or, after run-checks.bat has compiled everything:
//   java -Djava.awt.headless=true -cp build/test/classes nightmare404.debug.BattleScreensDebug [output folder]
public class BattleScreensDebug {
    public static final List<EnemyDef> ALL = List.of(
            FourthFloorEnemies.DESK_MIMIC, FourthFloorEnemies.CHALK_WRAITH, FourthFloorEnemies.CRAWLER,
            FourthFloorEnemies.HALL_MONITOR, FourthFloorEnemies.BRUTE,
            LibraryEnemies.UNDEAD, LibraryEnemies.LIBRARIAN, LibraryEnemies.BOOKWORM, LibraryEnemies.PAPERGOLEM,
            LibraryEnemies.WHISPERER,
            LaboratoryEnemies.ANATOMYMODEL, LaboratoryEnemies.FLASKSLIME, LaboratoryEnemies.SPECIMEN7,
            LaboratoryEnemies.LABASSISTANT, LaboratoryEnemies.TESTSUBJECTX,
            LobbyEnemies.SUBSTITUTE, LobbyEnemies.LEFTOVERS, LobbyEnemies.SHADOWSTALKER, LobbyEnemies.MASCOT,
            LobbyEnemies.PRINCIPAL);

    public static void main(String[] args) throws IOException {
        File outDir = new File(args.length > 0 ? args[0] : "battle_screens");
        outDir.mkdirs();
        int n = 1;
        for (EnemyDef enemy : ALL) {
            BufferedImage image = BattleScreen.preview(enemy, 150);
            File file = new File(outDir, String.format("%02d_%s.png", n++, enemy.sprite()));
            ImageIO.write(image, "png", file);
        }
        // The quadrangle's trees are fought with the typing race instead (a word up, timer running)
        BufferedImage typing = TypingBattle.preview(QuadrangleEnemies.HOLLOW_OAK, 200);
        ImageIO.write(typing, "png", new File(outDir, String.format("%02d_%s_typing.png", n, QuadrangleEnemies.HOLLOW_OAK.sprite())));
        System.out.println("Saved " + (ALL.size() + 1) + " battle screens to " + outDir.getAbsolutePath());
    }
}
