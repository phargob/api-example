package old_scripts;

import lombok.extern.slf4j.Slf4j;

import rsb.ScriptRunner;
import net.runelite.rsb.script.ScriptManifest;

import rsb.wrappers.*;
import rsb.globval.enums.InterfaceTab;

import dax.DaxWalker;

@ScriptManifest(authors = { "phargob" }, name = "smelting 101")

@Slf4j
public class Smelting extends ScriptRunner {

    ///////////////////////////////////////////////////////////////////////////////
    // al kharid

    private RSTile bankTile = new RSTile(3270, 3165, 0);
    private RSTile furnaceTile = new RSTile(3281, 3185, 0);
	boolean logoutWhenDone = false;

    private final int FURNACE_KHARID_OBJECT_ID = 24009;

    // String withdrawNames[] = {"Iron ore"};
    // int withdrawQuants[] = {28};
	// char SMELT_KEY = '2';

    String withdrawNames[] = {"Copper ore", "Tin ore"};
    int withdrawQuants[] = {14, 14};
	char SMELT_KEY = ' ';

    // String withdrawNames[] = {"Silver ore"};
    // int withdrawQuants[] = {28};
	// char SMELT_KEY = '3';

    private enum State {
        start, walkToBank, depositWithdrawal, walkToFurnace, smeltAll, done;
    }

    private State currentState;

    private int inventoryCount() {
        return ctx.inventory.getCount(withdrawNames[0]);
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

        if (this.inventoryCount() > 0) {
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

        } else if (distance > 12) {
            DaxWalker.walkTo(ctx, ctx.tiles.createWalkerTile(tile), false);
        }

        distance = ctx.calc.distanceTo(tile);
        if (distance <= 12) {
            ctx.camera.turnTo(tile);
            sleep(random(800, 1600));

            this.currentState = nextState;
            return random(500, 1500);
        }

        log.warn("Failed to reach tile in doWalkTo");
        return random(1000, 2500);
    }

    private int doSmeltAll(RSPlayer myself) {
        int distance = ctx.calc.distanceTo(furnaceTile);
        log.info(String.format("Player at *doSmeltAll* (distance = %d): %s", distance, myself.getLocation()));

        if (this.inventoryCount() == 0) {
            this.currentState = State.walkToBank;
            return 100;
        }

		for (int i=0; i<10; i++) {
			if (!myself.isIdle()) {
				log.info("are we still smelting?!");
				antiBan(20);
				return random(10000, 30000);
			}

			sleep(random(50, 250));
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
            ctx.keyboard.sendKey(SMELT_KEY);
            sleep(random(800, 1500));

            if (!myself.isIdle()) {
                ctx.mouse.moveOffScreen();
                lastWasMoveOffScreen = true;
                break;
            }
        }

        return random(3000, 10000);
    }

    private int doDepositWithdrawal(RSPlayer myself) {
        log.info("Player at *doBankAllFish* : " + myself.getLocation());

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

        if (ctx.inventory.getCount() > 0) {
            ctx.bank.depositAll();
            sleep(random(500, 750));
        }

        // get one fish type - so we can deposit them all
        sleep(random(500, 750));

        for (int ii=0; ii<withdrawQuants.length; ii++) {
            boolean success = false;
            String name = withdrawNames[ii];
            int quant = withdrawQuants[ii];

			// do get all... XXX make this random, should be at higher level
			if (quant == 28) {
				quant = 0;
			}

            int itemId = ctx.bank.getItemID(withdrawNames[ii]);

            int leftCount = ctx.bank.getCount(itemId);
            log.info(String.format("Our bank has %s left %d", name, leftCount));
            if (leftCount < quant) {
                log.warn("Not enough left to continue");
                this.stopScript(logoutWhenDone);
            }

            for (int i=0; i<3; i++) {
                if (ctx.bank.withdraw(itemId, quant)) {
                    success = true;
                    break;
                }
                sleep(random(500, 750));
            }

            if (!success) {
                ctx.bank.close();
                return random(1000, 2000);
            }
        }

        ctx.bank.close();

        log.info(String.format("After banking our inventory %d", this.inventoryCount()));
        this.currentState = State.walkToFurnace;
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
