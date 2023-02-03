package basicloopbot;

import dax_api.api_lib.DaxWalker;
import dax_api.api_lib.models.DaxCredentials;
import dax_api.api_lib.models.DaxCredentialsProvider;

import net.runelite.rsb.methods.NPCs;
import net.runelite.rsb.methods.Skills;
import net.runelite.rsb.methods.Methods;

import net.runelite.rsb.script.Script;
import net.runelite.rsb.script.ScriptManifest;
import net.runelite.rsb.wrappers.RSNPC;
import net.runelite.rsb.wrappers.RSPath;
import net.runelite.rsb.wrappers.subwrap.WalkerTile;
import net.runelite.rsb.wrappers.RSPlayer;

import lombok.extern.slf4j.Slf4j;

@ScriptManifest(authors = { "phargob" }, name = "Walk walk walk", version = 0.1,
				description = "<html><head>"
				+ "</head><body>"
				+ "<center>Walk here and there</center>"
				+ "</body></html>")

@Slf4j
public class Walker extends Script {
    private final WalkerTile chickenCoop = new WalkerTile(3233, 3295, 0);
    private final WalkerTile draynor = new WalkerTile(3092, 3246, 0);
    private final WalkerTile draynorManor = new WalkerTile(3109, 3349, 0);
    private final WalkerTile barbarianVillage = new WalkerTile(3082, 3424, 0);
    private final WalkerTile restlessGhostFatherUkney = new WalkerTile(3147, 3170, 0);
    private final WalkerTile restlessGhost = new WalkerTile(3245, 3192, 0);
    private final WalkerTile wizardTower = new WalkerTile(3113, 3167, 0);
    private final WalkerTile portSarimDeposit = new WalkerTile(3043, 3236, 0);
    private final WalkerTile varrockTownCentre = new WalkerTile(3211, 3419, 0);
    private final WalkerTile varrockWest = new WalkerTile(3182, 3429, 0);
	private final WalkerTile varrockEastMine = new WalkerTile(3286, 3371, 0);
    private final WalkerTile lumbridgeNorth = new WalkerTile(3188, 3282, 0);

    private final WalkerTile grandExchange = new WalkerTile(3177, 3488, 0);
    private final WalkerTile alKharidBank = new WalkerTile(3278, 3162, 0);
    private final WalkerTile alKharidMine = new WalkerTile(3302, 3284, 0);

    private final WalkerTile faladorNorth = new WalkerTile(2965, 3390, 0);
    private final WalkerTile faladorBank1 = new WalkerTile(3012, 3357, 0);
    private final WalkerTile faladorSouth = new WalkerTile(3007, 3322, 0);

    private final WalkerTile airAlter = new WalkerTile(2989, 3294, 0);
    private final WalkerTile mindAlter = new WalkerTile(2978, 3506, 0);

    private final WalkerTile edgeville = new WalkerTile(3094, 3501, 0);

    private final WalkerTile thurgoHut = new WalkerTile(3001, 3160, 0);

    private final WalkerTile rimmington = new WalkerTile(2934, 3221, 0);
    private final WalkerTile iceMountain = new WalkerTile(3033, 3471, 0);

    private final WalkerTile letsGo = lumbridgeNorth;

    final ScriptManifest properties = getClass().getAnnotation(
            ScriptManifest.class);

    @Override
    public boolean onStart() {
        // Pass DaxWalker credentials
        DaxWalker.setCredentials(new DaxCredentialsProvider() {
            @Override
            public DaxCredentials getDaxCredentials() {
                return new DaxCredentials("sub_DPjXXzL5DeSiPf", "PUBLIC-KEY");
            }
        });

        return true;
    }

    @Override
    public int loop() {
        if (!game.isLoggedIn()) {
            return 2000;
        }

        log.warn("Here " + letsGo);
        DaxWalker.walkTo(letsGo);
        this.stopScript(false);

        return 42;
    }

}
