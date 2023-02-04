package old_scripts;

import lombok.extern.slf4j.Slf4j;

import rsb.ScriptRunner;
import net.runelite.rsb.script.ScriptManifest;

import rsb.wrappers.*;
import rsb.globval.enums.InterfaceTab;

import dax.DaxWalker;

@ScriptManifest(authors = { "phargob" }, name = "smelt object")

@Slf4j
public class SmeltObject extends ScriptRunner {

    ///////////////////////////////////////////////////////////////////////////////
    // al kharid

    private RSTile bankTile = new RSTile(3270, 3167, 0);
    private RSTile furnaceTile = new RSTile(3274, 3186, 0);

    private final int FURNACE_KHARID_OBJECT_ID = 24009;

    //String bar = "Gold bar";
    //String obj = "Gold ring";
    String bar = "Silver bar";
    String obj = "Unstrung symbol";

    private enum State {
        start, walkToBank, depositWithdrawal, walkToFurnace, smeltAll, done;
    }

    private State currentState;

    private int countBars() {
        return ctx.inventory.getCount(bar);
    }

    private int countObjs() {
        return ctx.inventory.getCount(obj);
    }

    boolean lastWasMoveOffScreen = false;
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

    private int doStart(RSPlayer myself) {
        log.info("Player at *start* : " + myself.getLocation());

        if (this.countBars() > 0) {
            this.currentState = State.walkToFurnace;
            return 100;
        }

        this.currentState = State.walkToBank;
        return 100;
    }

    private int doWalkTo(RSPlayer myself, RSTile tile, State nextState) {
        int distance = ctx.calc.distanceTo(tile);
        log.info(String.format("Player at *doWalkTo* (distance = %d): %s", distance, myself.getLocation()));

        if (distance > 100) {
            log.info(String.format("Player not near (distance: %d) area - PLEASE WALK THERE", distance));
            this.stopScript(false);
            return 42;

        } else if (distance > 5) {
            DaxWalker.walkTo(ctx, ctx.tiles.createWalkerTile(tile), false);
        }

        if (distance < 4) {
            this.currentState = nextState;
            return 200;
        }

        if (distance >= 4) {
            log.info("Pathing to mineTile");
            RSPath path = ctx.walking.getPath(tile);

            if (path.traverse()) {
                log.info("Path success");
            }

            while (true) {
                sleep(200);
                if (myself.isIdle()) {
                    break;
                }
            }

            ctx.camera.turnTo(tile);
            sleep(random(800, 1600));
        }

        if (distance < 4) {
            this.currentState = nextState;
            return random(800, 1600);
        }

        log.warn("Failed to reach tile in doWalkTo");
        return random(1000, 2500);
    }

    private int doSmeltAll(RSPlayer myself) {
        int distance = ctx.calc.distanceTo(furnaceTile);
        log.info(String.format("Player at *doSmeltAll* (distance = %d): %s", distance, myself.getLocation()));

        if (this.countBars() == 0) {
            this.currentState = State.walkToBank;
            return 100;
        }

        if (!myself.isIdle()) {
            log.info("are we still smelting?!");
            antiBan(20);
            return random(3000, 30000);
        }

        RSObject stove = ctx.objects.getNearest(FURNACE_KHARID_OBJECT_ID);
        if (stove == null) {
            log.warn(":( stove == null ");
            this.stopScript(false);
            return 42;
        }

        final int maxTries = 3;
        boolean success = false;
        for (int jj=0; jj<maxTries; jj++) {

            log.info("Attempting to doAction()");
            if (stove.doAction("Smelt")) {
                success = true;
                break;
            }

            sleep(random(500, 1000));
        }

        // try again?
        if (!success) {
            this.currentState = State.walkToFurnace;
            return random(5000, 30000);
        }

        // will bring up a dialogue
        // XXX issue with 1 not working
        sleep(random(2000, 3000));
        for (int i=0; i<5; i++) {
            ctx.keyboard.sendKey(' ');
            sleep(random(800, 1500));

            if (!myself.isIdle()) {
                ctx.mouse.moveOffScreen();
                lastWasMoveOffScreen = true;
                return random(3000, 10000);
            }
        }

        this.currentState = State.walkToFurnace;
        return random(5000, 30000);
    }

    private int doDepositWithdrawal(RSPlayer myself) {
        log.info("Player at *doBankAllFish* : " + myself.getLocation());

        if (ctx.bank.isCollectionOpen()) {
            log.warn("oops isCollectionOpen(), closing by moving away");
            // hack XXX
            ctx.walking.walkTileMM(bankTile, 1, 1);
            sleep(random(2000, 3000));
            this.currentState = State.walkToBank;
            return 100;
        }

        int failCount = 0;
        if (!ctx.bank.isOpen()) {
            if (ctx.bank.open()) {
                sleep(random(500, 750));
            }
        }

        while (!ctx.bank.isOpen()) {
            sleep(50);
            failCount++;
            if (failCount > 30) {
                // try again
                this.currentState = State.walkToBank;
                return random(1000, 2000);
            }
        }

        int objCount = this.countObjs();
        if (objCount > 0) {
            RSItem objItem = ctx.inventory.getItem(obj);
            if (objItem == null) {
                log.error("Could not find obj: " + obj);
                this.stopScript(true);
                return -1;
            }

            ctx.bank.deposit(objItem.getID(), 0);
            sleep(random(1000, 2500));

            int afterCount = ctx.inventory.getCount(true, objItem.getID());
            log.info(String.format("before (%d) and after (%d) counts for %s", objCount, afterCount, obj));
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
            this.stopScript(false);
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
            this.currentState = State.walkToFurnace;
        }

        ctx.bank.close();
        return random(1000, 2000);
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
            case walkToBank:
                return this.doWalkTo(myself, bankTile, State.depositWithdrawal);
            case depositWithdrawal:
                return this.doDepositWithdrawal(myself);
            case walkToFurnace:
                return this.doWalkTo(myself, furnaceTile, State.smeltAll);
            case smeltAll:
                return this.doSmeltAll(myself);

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
