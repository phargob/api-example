package old_scripts;

import lombok.extern.slf4j.Slf4j;

import rsb.ScriptRunner;
import net.runelite.rsb.script.ScriptManifest;

import rsb.wrappers.*;
import rsb.globval.enums.InterfaceTab;

@ScriptManifest(authors = { "phargob" }, name = "Craft rings")

@Slf4j
public class CraftRings extends ScriptRunner {

    ///////////////////////////////////////////////////////////////////////////////
    // edgeville

    private RSTile bankTile = new RSTile(3096, 3494, 0);
    private RSTile furnaceTile = new RSTile(3109, 3499, 0);
    private final int FURNACE_EDGEVILLE_OBJECT_ID = 16469;

    String bar = "Gold bar";
    String ring = "Gold ring";

    private enum State {
        start, bank, smelt, done;
    }

    private State currentState;


    // ###############################################################################

    private boolean lastWasMoveOffScreen = false;
    private void antiBan(int c) {
        int random = random(1, c);
        switch (random) {
        case 1:
            lastWasMoveOffScreen = false;
            if (random(1, 10) == 5) {
                if (ctx.game.getCurrentTab() != InterfaceTab.SKILLS) {
                    ctx.game.openTab(InterfaceTab.SKILLS);
                }
            }

        case 2:
            lastWasMoveOffScreen = false;
            if (random(1, 20) == 10) {
                int angle = ctx.camera.getAngle() + random(-90, 90);
                if (angle < 0) {
                    angle = 0;
                }

                if (angle > 359) {
                    angle = 0;
                }
                ctx.camera.setAngle(angle);
            }
        default:
            if (random(1, 20) < 10) {
                if (!lastWasMoveOffScreen) {
                    ctx.mouse.moveOffScreen();
                    lastWasMoveOffScreen = true;
                }
            }
        }

    }

    private int countBars() {
        return ctx.inventory.getCount(bar);
    }

    private int countRings() {
        return ctx.inventory.getCount(ring);
    }

    private int doStart(RSPlayer myself) {
        log.info(String.format("Player at *start* bars: %d, rings: %d", this.countBars(), this.countRings()));
        if (this.countBars() > 0) {
            this.currentState = State.smelt;
            return 100;
        }

        this.currentState = State.bank;
        return 100;
    }

    private int doBank(RSPlayer myself) {
        int distance = ctx.calc.distanceTo(bankTile);
        log.info(String.format("Player at *doBank* (distance = %d): %s", distance, myself.getLocation()));

        if (!ctx.calc.tileOnScreen(bankTile)) {
            log.info("Distance : " + myself.getLocation());
            ctx.camera.turnTo(bankTile);
            ctx.walking.walkTileOnScreen(bankTile);
            return random(1000, 1500);
        }

        if (!ctx.bank.isOpen()) {
            if (ctx.bank.open()) {
                sleep(random(500, 750));
            }

            int failCount = 0;
            while (!ctx.bank.isOpen()) {
                sleep(50);
                failCount++;
                if (failCount > 30) {
                    return random(1000, 2000);
                }
            }
        }

        int ringCount = this.countRings();
        if (ringCount > 0) {
            RSItem ringItem = ctx.inventory.getItem(ring);
            if (ringItem == null) {
                log.error("Could not find ringItem: " + ring);
                this.stopScript(true);
                return -1;
            }

            ctx.bank.deposit(ringItem.getID(), 0);
            sleep(random(1000, 2500));

            int afterCount = ctx.inventory.getCount(true, ringItem.getID());
            log.info(String.format("before (%d) and after (%d) counts for %s", ringCount, afterCount, ring));
            if (afterCount != 0) {
                return random(1000, 2000);
            }
        }

        boolean success = false;
        int barId = ctx.bank.getItemID(bar);

        int leftCount = ctx.bank.getCount(barId);
        log.info(String.format("Our bank has %s left %d", bar, leftCount));
        if (leftCount < 28) {
            log.warn("Not enough left to continue");
            this.stopScript(true);
        }

        for (int i=0; i<3; i++) {
            ctx.bank.withdraw(barId, 0);
            sleep(random(500, 750));
            if (this.countBars() > 20) {
                success = true;
                break;
            }
        }

        log.info(String.format("After banking our inventory %d", this.countBars()));
        ctx.bank.close();

        if (success) {
            this.currentState = State.smelt;
        }

        ctx.bank.close();
        return random(1000, 2000);
    }


    private int doSmelt(RSPlayer myself) {
        int distance = ctx.calc.distanceTo(furnaceTile);
        log.info(String.format("Player at *doSmelt* (distance = %d): %s", distance, myself.getLocation()));

        if (!ctx.calc.tileOnScreen(furnaceTile)) {
            log.info("Distance : " + myself.getLocation());
            ctx.camera.turnTo(furnaceTile);
            ctx.walking.walkTileOnScreen(furnaceTile);
            return random(1000, 1500);
        }

        if (this.countBars() == 0) {
            this.currentState = State.bank;
            return 100;
        }

        for (int ii=0; ii<20; ii++) {
            // sometimes doesnt trigger
            if (!myself.isIdle()) {
                log.info("are we still smelting?!");
                antiBan(20);
                return random(3000, 10000);
            }

            sleep(50);
        }

        RSObject furnace = ctx.objects.getNearest(FURNACE_EDGEVILLE_OBJECT_ID);

        if (furnace == null) {
            log.warn(":( furnace == null ");
            this.stopScript(false);
            return -1;
        }

        final int maxTries = 3;
        boolean success = false;
        for (int jj=0; jj<maxTries; jj++) {

            log.info("Attempting to doAction()");
            if (furnace.doAction("Smelt")) {
                success = true;
                break;
            }

            sleep(random(500, 1000));
        }

        // try again?
        if (!success) {
            log.warn("Waiting try again... ");
            return random(5000, 30000);
        }

        // will bring up a special widget ...
        // XXX issue with 1 not working
        while (!myself.isIdle()) {
            log.info("Waiting for idle before space... ");
            sleep(random(1000, 2000));
        }

        for (int i=0; i<5; i++) {
            ctx.keyboard.sendKey(' ');
            sleep(random(800, 1500));

            if (!myself.isIdle()) {
                ctx.mouse.moveOffScreen();
                lastWasMoveOffScreen = true;
                break;
            }
        }
        return random(3000, 10000);
    }

    @Override
    public int loop() {
        try {
            RSPlayer myself = getMyPlayer();
            if (getMyPlayer() == null)  {
                log.warn("Player is null :(");
                return 100;
            }

            // a state machine
            switch (this.currentState) {
            case start:
                return this.doStart(myself);
            case bank:
                return this.doBank(myself);
            case smelt:
                return this.doSmelt(myself);

            default:
                // State.done
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return random(30000, 60000);
    }

    @Override
    public boolean onStart() {
        this.currentState = State.start;
        return true;
    }

}
