package old_scripts;

import lombok.extern.slf4j.Slf4j;

import rsb.ScriptRunner;
import net.runelite.rsb.script.ScriptManifest;

import rsb.wrappers.*;
import rsb.globval.enums.InterfaceTab;

import dax.DaxWalker;

@ScriptManifest(authors = { "phargob" }, name = "Walk walk walk")

@Slf4j
public class Walker extends ScriptRunner {
    private final RSTile chickenCoop = new RSTile(3233, 3295, 0);
    private final RSTile draynor = new RSTile(3092, 3246, 0);
    private final RSTile draynorManor = new RSTile(3109, 3349, 0);
    private final RSTile barbarianVillage = new RSTile(3082, 3424, 0);
    private final RSTile restlessGhostFatherUkney = new RSTile(3147, 3170, 0);
    private final RSTile restlessGhost = new RSTile(3245, 3192, 0);
    private final RSTile wizardTower = new RSTile(3113, 3167, 0);
    private final RSTile portSarimDeposit = new RSTile(3043, 3236, 0);
    private final RSTile varrockTownCentre = new RSTile(3211, 3419, 0);
    private final RSTile varrockWest = new RSTile(3182, 3429, 0);
	private final RSTile varrockEastMine = new RSTile(3286, 3371, 0);
    private final RSTile lumbridgeNorth = new RSTile(3188, 3282, 0);

    private final RSTile grandExchange = new RSTile(3177, 3488, 0);
    private final RSTile alKharidBank = new RSTile(3278, 3162, 0);
    private final RSTile alKharidMine = new RSTile(3302, 3284, 0);

    private final RSTile faladorNorth = new RSTile(2965, 3390, 0);
    private final RSTile faladorBank1 = new RSTile(3012, 3357, 0);
    private final RSTile faladorSouth = new RSTile(3007, 3322, 0);

    private final RSTile airAlter = new RSTile(2989, 3294, 0);
    private final RSTile mindAlter = new RSTile(2978, 3506, 0);

    private final RSTile edgeville = new RSTile(3094, 3501, 0);

    private final RSTile thurgoHut = new RSTile(3001, 3160, 0);

    private final RSTile rimmington = new RSTile(2934, 3221, 0);
    private final RSTile iceMountain = new RSTile(3033, 3471, 0);

    private final RSTile letsGo = lumbridgeNorth;


    @Override
    public boolean onStart() {
        return true;
    }

    @Override
    public int loop() {
        if (!ctx.game.isLoggedIn()) {
            return 2000;
        }

        log.warn("Here " + letsGo);
        DaxWalker.walkTo(ctx, ctx.tiles.createWalkerTile(letsGo), false);
        this.stopScript(false);

        return 42;
    }

}
