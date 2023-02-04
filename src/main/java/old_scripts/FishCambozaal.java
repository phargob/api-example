package old_scripts;

import lombok.extern.slf4j.Slf4j;

import rsb.ScriptRunner;
import net.runelite.rsb.script.ScriptManifest;

import rsb.wrappers.RSObject;
import rsb.wrappers.RSNPC;
import rsb.wrappers.RSPath;
import rsb.wrappers.RSPlayer;
import rsb.wrappers.RSItem;
import rsb.wrappers.RSTile;
import rsb.globval.enums.InterfaceTab;

import java.awt.event.*;

@ScriptManifest(
        authors = { "phargob" },
        name = "Fish at Cambozaal")


@Slf4j
public class FishCambozaal extends ScriptRunner {
    private String fishAction = "Small Net";
    private RSTile fishLocation = new RSTile(2930, 5776, 0);
    private RSTile otherLocation = new RSTile(2935, 5772, 0);

	private String[] RAW_FISH = {"Raw guppy", "Raw cavefish", "Raw tetra"};
	private String[] PREPARED_FISH = {"Guppy", "Cavefish", "Tetra"};
	private String[] BAD_FISH = {"Ruined guppy", "Ruined cavefish", "Ruined tetra"};
	private int CAMBOZAAL_PREPARATION_TABLE_ID = 41545;
	private int CAMBOZAAL_ALTER_ID = 41546;
	// XZXX Offer-fis

    ///////////////////////////////////////////////////////////////////////////////

    private enum State {
        start, findFishingSpot, fishAllDay, prepare, offer, dropBad;
    }

    private State currentState;
    private int lastFishInvCount;
    private int totalCaught;

    private long startTime;
    private long lastReportTime;
    private long lastCaughtTime;


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

    private int doStart(RSPlayer myself) {
        log.info("Player at *start* : " + myself.getLocation());

        // set the start time
        this.startTime = System.currentTimeMillis();
        this.totalCaught = 0;
        this.lastReportTime = this.lastCaughtTime = this.startTime;
        this.lastFishInvCount = ctx.inventory.getCount(RAW_FISH);

		this.currentState = State.findFishingSpot;
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
                log.info("npc!" + n.getName());

                int distance = myself.getLocation().distanceTo(n.getLocation());
                if (distance < closest) {
                    spot = n;
                    closest = distance;
                }
            }
        }

        return spot;
    }

    private int doFindFishingSpot(RSPlayer myself) {
        log.info("Player at *findFishingSpot* : " + myself.getLocation());

		// XXX calc from ?
        int distance = ctx.calc.distanceTo(fishLocation);
        if (distance > 50) {
            log.info(String.format("Player not near (distance: %d) fishing area - PLEASE WALK THERE", distance));
            this.stopScript(false);

        } else if (distance > 10) {
            ctx.walking.walkTileMM(fishLocation, 2, 2);
            return random(1000, 2500);
        }

        RSNPC spot = this.closestFishingSpot(myself);
        if (spot == null) {
            ctx.walking.walkTileMM(fishLocation, 2, 2);
            return random(1000, 2500);
        }

        log.info("Turning to spot");
        ctx.camera.turnTo(spot.getLocation());
        sleep(random(1000, 2000));

        final int maxTries = 3;
        boolean success = false;
        for (int jj=0; jj<maxTries; jj++) {

            log.info("Attempting to doAction()");
            if (spot.getLocation().doAction(fishAction, "Fishing Spot")) {
                success = true;
                sleep(random(250, 500));
                break;
            }

            sleep(random(500, 1000));
        }

        if (success) {
            log.info("Now fishing!");
			this.lastFishInvCount = ctx.inventory.getCount(RAW_FISH);
            this.currentState = State.fishAllDay;
            lastWasMoveOffScreen = true;
            ctx.mouse.moveOffScreen();

            // will check we are actually fishing in State.fishFishAllDay
            return random(5000, 10000);

        } else {
            log.info("Failed to start fishing");

            log.info("Pathing to fish spot");
            RSPath path = ctx.walking.getPath(spot.getLocation());

            if (path.traverse()) {
                log.info("Path success");
            }

            ctx.camera.turnTo(spot);
            return random(2000, 5000);
        }
    }

    private int doFishAllDay(RSPlayer myself) {
        int cur = ctx.inventory.getCount(RAW_FISH);
        int assumedCaught = 0;
        if (cur > this.lastFishInvCount) {
            assumedCaught = cur - this.lastFishInvCount;
            this.totalCaught += assumedCaught;
            this.lastFishInvCount = cur;
            this.lastCaughtTime = System.currentTimeMillis();
        }

        log.info(String.format("*doFishFishAllDay* (loc: %s, caught: %d)",
                        myself.getLocation(), assumedCaught));

        if (ctx.inventory.isFull()) {
            log.info("no space left");
            this.currentState = State.prepare;
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
            log.info("not fishing anymore");
            this.currentState = State.findFishingSpot;
            return 100;
        }

        antiBan(20);
        return random(5000, 30000);
    }

	private int doPrepare(RSPlayer myself) {
		for (int i=0; i<10; i++) {
			if (!myself.isIdle()) {
				log.info("are we still preparing?!");
				antiBan(20);
				return random(2000, 10000);
			}

			sleep(random(50, 250));
		}

		RSObject table = ctx.objects.getNearest(CAMBOZAAL_PREPARATION_TABLE_ID);
        if (table == null) {
            log.warn(":( table == null ");
            this.stopScript(false);
            return 42;
        }

		if (ctx.calc.distanceBetween(myself.getLocation(), table.getLocation()) > 3) {
            RSPath path = ctx.walking.getPath(table.getLocation());

            if (path.traverse()) {
                log.info("Move there");
            }

            ctx.camera.turnTo(table.getLocation());
            sleep(random(1000, 2000));
		}

		if (ctx.inventory.getCount(RAW_FISH) == 0) {
            this.currentState = State.offer;
			return random(500, 2500);
		}

        final int maxTries = 3;
        boolean success = false;
        for (int jj=0; jj<maxTries; jj++) {

            log.info("Attempting to doAction()");
            if (table.doAction("Prepare-fish")) {
                success = true;
                break;
            }

            sleep(random(500, 1000));
        }

		for (int i=0; i<25; i++) {
			if (myself.isIdle() && !myself.isLocalPlayerMoving()) {
				break;
			}

			sleep(random(500, 1000));
		}

        // will bring up a dialogue
        sleep(random(2000, 3000));

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

	private int doOffer(RSPlayer myself) {
		for (int i=0; i<10; i++) {
			if (!myself.isIdle()) {
				log.info("are we still preparing?!");
				antiBan(20);
				return random(2000, 10000);
			}

			sleep(random(50, 250));
		}


		if (ctx.inventory.getCount(PREPARED_FISH) == 0) {
            this.currentState = State.dropBad;
			return random(500, 2500);
		}

		RSObject table = ctx.objects.getNearest(CAMBOZAAL_ALTER_ID);
        if (table == null) {
            log.warn(":( table == null ");
            this.stopScript(false);
            return 42;
        }

        final int maxTries = 3;
        boolean success = false;
        for (int jj=0; jj<maxTries; jj++) {

            log.info("Attempting to doAction()");
            if (table.doAction("Offer-fish")) {
                success = true;
                break;
            }

            sleep(random(500, 1000));
        }

		for (int i=0; i<25; i++) {
			if (myself.isIdle() && !myself.isLocalPlayerMoving()) {
				break;
			}

			sleep(random(500, 1000));
		}

        // will bring up a dialogue
        sleep(random(1000, 3000));

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

    private int doDropBad(RSPlayer myself) {
        // do 10 so we don't spin forever

        int badCount = ctx.inventory.getCount(BAD_FISH);
		if (badCount == 0) {
			this.currentState = State.findFishingSpot;
			return random(1000, 2000);
		}

		ctx.keyboard.pressKey((char) (KeyEvent.VK_SHIFT));
		ctx.mouse.pushDefaultMoveAfter(0);

		sleep(random(100, 200));
		try {
			for (int count=0; count<3; count++) {
				int itemIds[] = ctx.inventory.getItemIDs(BAD_FISH);

				if (itemIds.length == 0) {
					break;
				}

				RSItem[] allInvItems = ctx.inventory.getItems();
				boolean leftToRight = random(0, 100) > 50;
				for (int j = 0; j < 28; j++) {
					int c = leftToRight ? j % 4 : j / 7;
					int r = leftToRight ? j / 4 : j % 7;

					RSItem curItem = allInvItems[c + r * 4];
					if (curItem == null) {
						continue;
					}

					int itemId = curItem.getID();
					for (int i : itemIds) {
						if (i == itemId) {
							curItem.doClick(true);
							sleep(random(150, 300));
							break;
						}
					}
				}
			}
		} finally {
			sleep(random(100, 200));
			ctx.keyboard.releaseKey((char) (KeyEvent.VK_SHIFT));
			ctx.mouse.popDefaultMoveAfter();
		}

        badCount = ctx.inventory.getCount(BAD_FISH);
		if (badCount == 0) {
			this.currentState = State.findFishingSpot;
			return random(1000, 2000);
		}

		log.warn("doDropBad() failed");
		return random(250, 500);
	}

    @Override
    public int loop() {
        try {
            RSPlayer myself = getMyPlayer();
            if (getMyPlayer() == null)  {
                log.info("Player is null :(");
                return 100;
            }

			if (this.currentState != State.start) {
				// safeties
				long now = System.currentTimeMillis();
				long elapsedSinceStart = now - this.startTime;
				long elapsedSinceCaught = now - this.lastCaughtTime;
				long MIN = 1000 * 60;
				long HOUR = 60 * MIN;

				// ten minutes
				if (elapsedSinceCaught > 5 * MIN) {
					log.warn("Stopping script since elapsedSinceCaught");
					this.stopScript(false);
					return 42;
				}

				// ten minutes
				if (elapsedSinceStart > 4 * HOUR) {
					log.warn("Stopping script since elapsedSinceStart");
					this.stopScript(false);
					return 42;
				}
			}

            // a state machine
            switch (this.currentState) {
            case start:
                return this.doStart(myself);
            case findFishingSpot:
                return this.doFindFishingSpot(myself);
            case fishAllDay:
                return this.doFishAllDay(myself);
            case prepare:
                return this.doPrepare(myself);
			case offer:
			    return this.doOffer(myself);
            case dropBad:
                return this.doDropBad(myself);
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
