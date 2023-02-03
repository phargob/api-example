package basicloopbot;

import net.runelite.rsb.methods.NPCs;
import net.runelite.rsb.methods.Skills;
import net.runelite.rsb.methods.Methods;

import net.runelite.rsb.script.Script;
import net.runelite.rsb.script.ScriptManifest;
import net.runelite.rsb.wrappers.RSItem;
import net.runelite.rsb.wrappers.RSPath;
import net.runelite.rsb.wrappers.RSTile;
import net.runelite.rsb.wrappers.RSPlayer;
import net.runelite.rsb.wrappers.RSObject;
import net.runelite.rsb.wrappers.RSWidget;
import net.runelite.rsb.wrappers.subwrap.WalkerTile;
import net.runelite.rsb.internal.globval.enums.InterfaceTab;


import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.event.*;

@ScriptManifest(authors = { "phargob" }, name = "miner cambozaal",
				version = 0.2, description = "miner cambozaal")

@Slf4j
public class MineCambozaal extends Script {

    private int leastDistance = 6;
    private String mineThis = "Barronite";

	///////////////////////////////////////////////////////////////////////////////
    // cambozaal
    final private RSTile mineTile = new RSTile(2939, 5809, 0);
    final private RSTile anvilTile = new RSTile(2960, 5800, 0);

	String getOreType() {
		if (mineThis.equals("Clay") || mineThis.equals("Coal")) {
			return mineThis;
		}

		return mineThis + " ore";
	}

	int clayIds[] = { 11362, 11363 };
	int tinIds[] = { 11360, 11361 };
	int copperIds[] = { 11161, 10943 };
	int ironIds[] = { 11364, 11365 };
	int silverIds[] = { 11368, 11369 };
	int goldIds[] = { 11370, 11371 };
	int coalIds[] = { 11366, 11367 };

	int barroniteIds[] = { 41547, 41548 };

	int[] getObjectIds() {

		if (mineThis.equals("Clay")) {
			return clayIds;
		}

		if (mineThis.equals("Tin")) {
			return tinIds;
		}

		if (mineThis.equals("Copper")) {
			return copperIds;
		}

		if (mineThis.equals("Iron")) {
			return ironIds;
		}

		if (mineThis.equals("Silver")) {
			return silverIds;
		}

		if (mineThis.equals("Gold")) {
			return goldIds;
		}

		if (mineThis.equals("Coal")) {
			return coalIds;
		}

		if (mineThis.equals("Barronite")) {
			return barroniteIds;
		}

		return null;
	}

    private int inventoryCount() {
        return ctx.inventory.getCount(getOreType());
    }

	///////////////////////////////////////////////////////////////////////////////

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
            if (random(1, 20) < 15) {
                if (!lastWasMoveOnScreen) {
                    ctx.mouse.moveOffScreen();
                    lastWasMoveOnScreen = true;
                }
            }
        }

    }

	///////////////////////////////////////////////////////////////////////////////

	private boolean checkObject(RSObject rso) {
        if (rso.getType() != RSObject.Type.WALL) {
            return false;
        }

		boolean found = false;
        for (int findme: getObjectIds()) {
            if (rso.getID() == findme) {
                found = true;
            }
        }
		if (!found) {
			return false;
		}

        RSPlayer myself = getMyPlayer();
        double distTmp = ctx.calc.distanceBetween(myself.getLocation(), rso.getLocation());
        if (distTmp > leastDistance) {
            return false;
        }

		log.info(String.format("rso : %f %s %d", distTmp, rso.getName(), rso.getID()));
        return true;
    }

    private boolean doMine() {
        RSPlayer myself = getMyPlayer();
        int distance = calc.distanceTo(mineTile);
        log.info(String.format("Player at *doMine* (distance = %d): %s", distance, myself.getLocation()));

        // dump local objects
		boolean mined = false;
		double closestDist = -1;
		RSObject choice = null;
        for (RSObject rso: ctx.objects.getAll()) {
            if (rso == null) {
                continue;
            }

            if (!checkObject(rso)) {
                continue;
            }

			double dist = ctx.calc.distanceBetween(myself.getLocation(), rso.getLocation());
			if (closestDist < 0 || dist < closestDist) {
				closestDist = dist;
				choice = rso;
			}
		}

		if (choice == null) {
			return false;
		}

		log.info(String.format("choice : %f %d on-screen:%s",
							   closestDist, choice.getID(), choice.isOnScreen()));

		if (!choice.isOnScreen()) {
			camera.turnTo(choice.getLocation());
			sleep(random(800, 1600));
			if (!choice.isOnScreen()) {
				return false;
			}
		}

		if (!choice.doAction("Mine")) {
			// that faild let's traverse there so maybe work next time
			log.info("failed doAction mine...");

            RSPath path = ctx.walking.getPath(choice.getLocation());

            if (path.traverse()) {
                log.info("Move there");
            }

            camera.turnTo(choice.getLocation());
            sleep(random(1000, 2000));
			return false;
		}

		// XXX hack since we cant detect moving properly
		sleep(random((int) closestDist * 300, (int) closestDist * 750));
		//sleep(random(200, 500));

		return true;
	}

    @Override
    public int loop() {
        try {
            RSPlayer myself = getMyPlayer();

            if (ctx.inventory.isFull()) {
                this.stopScript(false);
				return 42;
            }

			// just checking we are def idle
			int idleCount = 0;
            for (int i=0; i<10; i++) {
                if (myself.isIdle() && !myself.isLocalPlayerMoving()) {
					idleCount++;
                }

                sleep(random(100, 200));
            }

            if (idleCount == 10) {
                // just make sure we've been idle for a bit
                sleep(random(250, 500));

                if (doMine()) {
					log.info("state == now mining");
					antiBan(20);
					return random(2500, 10000);

                } else {
                    // depends what it is
                    return random(2000, 3000);
                }

            } else {
				log.info("state == mining");
				antiBan(20);
				return random(2500, 10000);
			}


        } catch (NullPointerException e) {
            e.printStackTrace();
        }

		return random(5000, 10000);
    }

    @Override
    public boolean onStart() {
        return true;
    }
}
