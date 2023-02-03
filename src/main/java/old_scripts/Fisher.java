package basicloopbot;

import lombok.extern.slf4j.Slf4j;

import dax_api.api_lib.DaxWalker;

import net.runelite.rsb.methods.Methods;
import net.runelite.rsb.internal.globval.enums.InterfaceTab;

import net.runelite.rsb.script.Script;
import net.runelite.rsb.script.ScriptManifest;

import net.runelite.rsb.wrappers.RSNPC;
import net.runelite.rsb.wrappers.RSPath;
import net.runelite.rsb.wrappers.RSPlayer;
import net.runelite.rsb.wrappers.RSItem;
import net.runelite.rsb.wrappers.RSTile;
import net.runelite.rsb.wrappers.subwrap.WalkerTile;


@ScriptManifest(
        authors = { "phargob" },
        name = "Fisher",
        version = 0.1,
        description = "Fisher Fisher")


@Slf4j
public class Fisher extends Script {
    // XXX to long distance, needs DAX
    // class BarbarianVillageConfig {
    //     public RSTile bankTile = new RSTile(3093, 3491, 0);

    //     public RSTile fishLocation0 = new RSTile(3103, 3434, 0);
    //     public RSTile fishLocation1 = new RSTile(3104, 3429, 0);
    // }

    //String fishAction = "Lure";
    //String[] SOME_FISH = {"Raw trout", "Raw salmon"};

    ///////////////////////////////////////////////////////////////////////////////
    // draynor

    // public RSTile bankTile = new RSTile(3093, 3491, 0);

    // private RSTile fishLocation0 = new RSTile(3088, 3237, 0);
    // private RSTile fishLocation1 = new RSTile(3089, 3225, 0);

    // private String fishAction = "Bait";
    // private String[] SOME_FISH = {"Raw sardine", "Raw herring"};

    ///////////////////////////////////////////////////////////////////////////////
    // al kharid

    // private RSTile bankTile = new RSTile(3270, 3166, 0);

    // private RSTile fishLocation0 = new RSTile(3272, 3150, 0);
    // private RSTile fishLocation1 = new RSTile(3273, 3144, 0);

    // private String fishAction = "Small Net";
    // private  String[] SOME_FISH = {"Raw shrimps", "Raw anchovies"};

    ///////////////////////////////////////////////////////////////////////////////
    // current:

    private RSTile bankTile = new RSTile(3270, 3166, 0);

    private RSTile fishLocation0 = new RSTile(3272, 3150, 0);
    private RSTile fishLocation1 = new RSTile(3273, 3144, 0);

    private String fishAction = "Small Net";
	private  String[] SOME_FISH = {"Raw shrimps", "Raw anchovies"};

    ///////////////////////////////////////////////////////////////////////////////

    private enum State {
        start, findFishingSpot0, findFishingSpot1, fishFishAllDay, walkToBank, bankAllFish, done;
    }

    private State currentState;
    private int lastFishInvCount;
    private int totalCaught;

    private long startTime;
    private long lastReportTime;
    private long lastCaughtTime;

    private void l(String toLog) {
        log.info(toLog);
    }

    private int inventoryCount() {
        return ctx.inventory.getCount(SOME_FISH);
    }

    private RSTile closestTile(final RSTile tiles[]) {
        int dist = 999;
        RSTile closest = null;
        for (final RSTile tile : tiles) {
            final int distance = calc.distanceTo(tile);
            if (distance < dist) {
                dist = distance;
                closest = tile;
            }
        }
        return closest;
    }

    boolean lastWasMoveOnScreen = false;
    private void antiBan(int c) {
        int random = random(1, c);
        switch (random) {
        case 1:
            lastWasMoveOnScreen = false;
            if (random(1, 10) == 5) {
                if (ctx.game.getCurrentTab() != InterfaceTab.SKILLS) {
                    game.openTab(InterfaceTab.SKILLS);
                }
            }

        case 2:
            lastWasMoveOnScreen = false;
            if (random(1, 20) == 10) {
                int angle = camera.getAngle() + random(-90, 90);
                if (angle < 0) {
                    angle = 0;
                }

                if (angle > 359) {
                    angle = 0;
                }
                camera.setAngle(angle);
            }
        default:
            if (random(1, 20) < 10) {
                if (!lastWasMoveOnScreen) {
                    ctx.mouse.moveOffScreen();
                    lastWasMoveOnScreen = true;
                }
            }
        }

    }

    private int doStart(RSPlayer myself) {
        l("Player at *start* : " + myself.getLocation());

        // XXX calc from ?
        int distance = calc.distanceTo(fishLocation0);
        if (distance > 50) {
            l(String.format("Player not near (distance: %d) fishing area - PLEASE WALK THERE", distance));
            this.stopScript(false);

        } else if (distance > 10) {
            ctx.walking.walkTileMM(fishLocation0, 1, 1);
            return random(1000, 2500);
        }

        this.currentState = State.findFishingSpot0;
        return random(1000, 2500);
    }

    private RSNPC closestFishingSpot(RSPlayer myself) {
        int closest = 10;
        RSNPC spot = null;
        for (RSNPC n : ctx.npcs.getAll()) {
            if (n == null) {
                continue;
            }

            if (n.getName().contains("Fishing spot")) {
                l("npc!" + n.getName());

                int distance = myself.getLocation().distanceTo(n.getLocation());
                if (distance < closest) {
                    spot = n;
                    closest = distance;
                }
            }
        }

        return spot;
    }

    private int doFindFishingSpot(RSPlayer myself, RSTile walkTile) {
        l("Player at *findFishingSpot* : " + myself.getLocation());

        RSNPC spot = this.closestFishingSpot(myself);
        if (spot == null) {
            ctx.walking.walkTileMM(fishLocation1, 1, 1);
            return random(1000, 2500);
        }

        l("Turning to spot");
        camera.turnTo(spot.getLocation());
        sleep(random(1000, 2000));

        final int maxTries = 3;
        boolean success = false;
        for (int jj=0; jj<maxTries; jj++) {

            l("Attempting to doAction()");
            if (spot.getLocation().doAction(fishAction, "Fishing Spot")) {
                success = true;
                sleep(random(250, 500));
                break;
            }

            sleep(random(500, 1000));
        }

        if (success) {
            l("Now fishing!");
            this.currentState = State.fishFishAllDay;
            lastWasMoveOnScreen = true;
            ctx.mouse.moveOffScreen();

            // will check we are actually fishing in State.fishFishAllDay
            return random(5000, 10000);

        } else {
            l("Failed to start fishing");

            l("Pathing to fish spot");
            RSPath path = ctx.walking.getPath(spot.getLocation());

            if (path.traverse()) {
                l("Path success");
            }

            camera.turnTo(spot);
            return random(2000, 5000);
        }
    }

    private int doFishFishAllDay(RSPlayer myself) {
        int cur = this.inventoryCount();
        int assumedCaught = 0;
        if (cur > this.lastFishInvCount) {
            assumedCaught = cur - this.lastFishInvCount;
            this.totalCaught += assumedCaught;
            this.lastFishInvCount = cur;
            this.lastCaughtTime = System.currentTimeMillis();
        }

        l(String.format("*doFishFishAllDay* (loc: %s, caught: %d)",
                        myself.getLocation(), assumedCaught));

        if (ctx.inventory.isFull()) {
            l("no space left");
            this.currentState = State.walkToBank;
            return 100;
        }

		// just checking we are def idle
		int count = 0;
		for (int i=0; i<5; i++) {
			if (myself.isIdle()) {
				count++;
			}

			sleep(random(50, 100));
		}

        if (count == 5) {
            l("not fishing anymore");
            this.currentState = State.findFishingSpot1;
            return 100;
        }

        antiBan(20);
        return random(5000, 30000);
    }

    private int doWalkToBank(RSPlayer myself) {
        int distance = calc.distanceTo(bankTile);
        l(String.format("Player at *doWalkToBank* (distance = %d): %s", distance, myself.getLocation()));

        if (distance > 50) {
            l(String.format("Player not near (distance: %d) fishing area - PLEASE WALK THERE", distance));
            return random(30000, 60000);

        } else if (distance > 10) {
            DaxWalker.walkTo(new WalkerTile(bankTile));

        } else if (distance >= 4) {
            // this can get stuck
            for (int i=0; i<3; i++) {
                int before = calc.distanceTo(bankTile);
                ctx.walking.walkTileOnScreen(bankTile);
                sleep(random(2000, 3000));
                int after = calc.distanceTo(bankTile);
                if (before == after) {
                    ctx.walking.walkTileMM(bankTile, 1, 1);
                    break;
                }

                if (after < 4) {
                    break;
                }
            }
        }

        if (distance < 4) {
            this.currentState = State.bankAllFish;
        }

        return random(1000, 2500);
    }

    private int doBankAllFish(RSPlayer myself) {
        l("Player at *doBankAllFish* : " + myself.getLocation());

        if (ctx.bank.isCollectionOpen()) {
			ctx.bank.closeCollection();
			sleep(random(500, 750));
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
                return 1000;
            }
        }

        // get all items, choose one and then deposit all

        // get one fish type - so we can deposit them all
        sleep(random(500, 750));

        for (String name : SOME_FISH) {
            if (!ctx.bank.isOpen()) {
                log.warn("Bank not open");
                return random(1000, 2000);
            }

            RSItem fish = ctx.inventory.getItem(name);
            if (fish == null) {
                log.error("Could not find fish id: " + name);
                sleep(random(250, 500));
                continue;
            }

            int itemCount = ctx.inventory.getCount(true, fish.getID());
            if (itemCount > 0) {
                bank.deposit(fish.getID(), 0);
            }

            sleep(random(1000, 2500));

            int afterCount = ctx.inventory.getCount(true, fish.getID());

            l(String.format("before (%d) and after (%d) counts for %s", itemCount, afterCount, name));

            if (afterCount != 0) {
                return random(1000, 2000);
            }
        }

        ctx.bank.close();

        this.lastFishInvCount = this.inventoryCount();
        l(String.format("After banking our inventory %d", this.lastFishInvCount));

        this.lastReportTime = System.currentTimeMillis();
        long elapsed = this.lastReportTime - this.startTime;
        l("Been running " + elapsed);
        l("Total caught since start " + this.totalCaught);

        this.currentState = State.findFishingSpot0;
        return random(1000, 2000);
    }

    @Override
    public int loop() {
        try {
            RSPlayer myself = getMyPlayer();
            if (getMyPlayer() == null)  {
                l("Player is null :(");
                return 100;
            }

            // safeties
            long now = System.currentTimeMillis();
            long elapsedSinceStart = now - this.startTime;
            long elapsedSinceCaught = now - this.lastCaughtTime;
            long MIN = 1000 * 60;
            long HOUR = 60 * MIN;

            // ten minutes
            if (elapsedSinceCaught > 5 * MIN) {
                log.warn("Stopping script since elapsedSinceCaught");
                this.stopScript(true);
            }

            // ten minutes
            if (elapsedSinceStart > 4 * HOUR) {
                log.warn("Stopping script since elapsedSinceStart");
                this.stopScript(true);
            }

            // a state machine
            switch (this.currentState) {
            case start:
                return this.doStart(myself);
            case findFishingSpot0:
                return this.doFindFishingSpot(myself, fishLocation0);
            case findFishingSpot1:
                return this.doFindFishingSpot(myself, fishLocation1);
            case fishFishAllDay:
                return this.doFishFishAllDay(myself);
            case walkToBank:
                return this.doWalkToBank(myself);
            case bankAllFish:
                return this.doBankAllFish(myself);
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

        // set the start time
        this.startTime = System.currentTimeMillis();
        this.totalCaught = 0;
        this.lastReportTime = this.lastCaughtTime = this.startTime;
        this.lastFishInvCount = this.inventoryCount();

        return true;
    }

}
