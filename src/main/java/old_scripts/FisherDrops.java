package old_scripts;

import lombok.extern.slf4j.Slf4j;

import rsb.ScriptRunner;
import net.runelite.rsb.script.ScriptManifest;

import rsb.wrappers.*;
import rsb.globval.enums.InterfaceTab;


@ScriptManifest(authors = { "phargob" }, name = "FisherDrops")

@Slf4j
public class FisherDrops extends ScriptRunner {
    // XXX to long distance, needs DAX
    // class BarbarianVillageConfig {

    //     public RSTile fishLocation0 = new RSTile(3103, 3434);
    //     public RSTile fishLocation1 = new RSTile(3104, 3429);
    // }

    //String fishAction = "Lure";
    //String[] SOME_FISH = {"Raw trout", "Raw salmon"};

    ///////////////////////////////////////////////////////////////////////////////
    // draynor

    // private RSTile fishLocation0 = new RSTile(3088, 3237);
    // private RSTile fishLocation1 = new RSTile(3089, 3225);

    // private String fishAction = "Bait";
    // private String[] SOME_FISH = {"Raw sardine", "Raw herring"};

    ///////////////////////////////////////////////////////////////////////////////
    // al kharid

    // private RSTile fishLocation0 = new RSTile(3272, 3150);
    // private RSTile fishLocation1 = new RSTile(3273, 3144);

    // private String fishAction = "Small Net";
    // private String[] SOME_FISH = {"Raw shrimps", "Raw anchovies"};

    ///////////////////////////////////////////////////////////////////////////////
    // lumbridge

    private RSTile fishLocation0 = new RSTile(3242, 3242, 0);
    private RSTile fishLocation1 = new RSTile(3241, 3249, 0);

    private String fishAction = "Bait";
    private String[] SOME_FISH = {"Raw pike"};


    ///////////////////////////////////////////////////////////////////////////////
    // current:

    // private RSTile fishLocation0 = new RSTile(3242, 3242);
    // private RSTile fishLocation1 = new RSTile(3241, 3249);

    // String fishAction = "Lure";
    // String[] SOME_FISH = {"Raw trout", "Raw salmon"};


    ///////////////////////////////////////////////////////////////////////////////

    private enum State {
        start, findFishingSpot0, findFishingSpot1, fishFishAllDay, dropAll, done;
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
            final int distance = ctx.calc.distanceTo(tile);
            if (distance < dist) {
                dist = distance;
                closest = tile;
            }
        }
        return closest;
    }

    private void antiBan(int c) {
        int random = random(1, c);
        switch (random) {
        case 1:
            if (random(1, 10) == 5) {
                if (ctx.game.getCurrentTab() != InterfaceTab.SKILLS) {
                    ctx.game.openTab(InterfaceTab.SKILLS);
                }
            }

        case 2:
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
                ctx.mouse.moveOffScreen();
            }
        }
    }

    private int doStart(RSPlayer myself) {
        l("Player at *start* : " + myself.getLocation());

        // assume fishing if not idle
        if (!myself.isIdle()) {
            this.currentState = State.fishFishAllDay;
            ctx.mouse.moveOffScreen();
            return 100;
        }

        if (ctx.inventory.isFull()) {
            l("no space left!");
            this.currentState = State.dropAll;
            return 100;
        }

        // XXX calc from ?
        int distance = ctx.calc.distanceTo(fishLocation0);
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
        ctx.camera.turnTo(spot);
        sleep(random(1000, 2000));

        l("Attempting to click spot");
        final int maxTries = 3;
        boolean success = false;
        for (int jj=0; jj<maxTries; jj++) {

            if (spot.getLocation().doAction(fishAction, "Fishing Spot")) {
                success = true;
                sleep(random(250, 500));
                break;
            }

            sleep(random(500, 1000));
        }

        if (success) {
            l("Booya! Now fishing");
            this.currentState = State.fishFishAllDay;
            ctx.mouse.moveOffScreen();

            // will check we are actually fishing in State.fishFishAllDay
            return random(1000, 2000);

        } else {
            l("Failed to start fishing");

            l("Pathing to fish spot");
            RSPath path = ctx.walking.getPath(spot.getLocation());

            if (path.traverse()) {
                l("Path success");
            }

            ctx.camera.turnTo(spot);
            return random(2000, 5000);
        }
    }

    private int doFishFishAllDay(RSPlayer myself) {
        l("Player at *doFishFishAllDay* : " + myself.getLocation());

        int cur = this.inventoryCount();
        if (cur > this.lastFishInvCount) {
            this.totalCaught += cur - this.lastFishInvCount;
            this.lastFishInvCount = cur;
            this.lastCaughtTime = System.currentTimeMillis();
        }

        if (ctx.inventory.isFull()) {
            l("no space left!");
            this.currentState = State.dropAll;
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

        if (count == 10) {
            l("we arnt fishing anymore!");
            this.currentState = State.findFishingSpot1;
            return 100;
        }

        antiBan(5);
        return random(5000, 30000);
    }

    private int doDropAll(RSPlayer myself) {
        l("Player at *doDropAll* left : : " + this.inventoryCount());
        if (this.inventoryCount() == 0) {
            this.currentState = State.findFishingSpot0;
            return random(500, 1500);
        }

        int[] itemIds = ctx.inventory.getItemIDs(SOME_FISH);

        // do 10 so we don't spin forever
        for (int count=0; count<10; count++) {

            if (this.inventoryCount() == 0) {
                break;
            }

            RSItem[] allInvItems = ctx.inventory.getItems();
            boolean leftToRight = true; //random(0, 100) > 50;
            for (int j = 0; j < 28; j++) {
                int c = leftToRight ? j % 4 : j / 7;
                int r = leftToRight ? j / 4 : j % 7;

                RSItem curItem = allInvItems[c + r * 4];
                l(String.format("HERE %d %d %d %s", c, r, c + r * 4, curItem));
                if (curItem == null) {
                    continue;
                }

                // i hate java
                boolean isInItems = false;
                for (int x : itemIds) {
                    l(String.format("HERE %d %d", curItem.getID(), x));
                    if (x == curItem.getID()) {
                        isInItems = true;
                        break;
                    }
                }

                if (!isInItems) {
                    continue;
                }

                final int maxTries = 3;
                for (int jj=0; jj<maxTries; jj++) {
                    if (curItem.doAction("Drop")) {
                        break;
                    }

                    sleep(random(300, 500));
                }
            }
        }

        return random(200, 400);
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
            if (elapsedSinceCaught > 20 * MIN) {
                log.warn("Stopping script since elapsedSinceCaught");
                this.stopScript(true);
            }

            // ten minutes
            if (elapsedSinceStart > 3 * HOUR) {
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
            case dropAll:
                return this.doDropAll(myself);
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
