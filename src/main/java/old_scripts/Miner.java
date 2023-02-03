package basicloopbot;

import dax_api.api_lib.DaxWalker;
import dax_api.api_lib.models.DaxCredentials;
import dax_api.api_lib.models.DaxCredentialsProvider;

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

@ScriptManifest(authors = { "phargob" }, name = "miner +1",
				version = 0.2, description = "miner +1")

@Slf4j
public class Miner extends Script {

    private int sleepMult = 1;
    private int leastDistance = 6;

	// XXX can't do both... should probably be enum
    private boolean bankOre = true;
    private boolean dropOre = false;

    private String mineThis = "Tin";


	///////////////////////////////////////////////////////////////////////////////
    // al kharid
    //final private RSTile bankTile = new RSTile(3270, 3166, 0);

	// iron
    //final private RSTile mineTile = new RSTile(3302, 3284, 0);

	// silver
	//final private RSTile mineTile = new RSTile(3294, 3300, 0);

	// gold
	//final private RSTile mineTile = new RSTile(3295, 3288, 0);

	///////////////////////////////////////////////////////////////////////////////
    // al kharid - near Giants Plateau
	// coal:
    //final private RSTile mineTile = new RSTile(3401, 3170, 0);
    //final private RSTile bankTile = new RSTile(3270, 3166, 0);

	///////////////////////////////////////////////////////////////////////////////
    // varrock west
	// clay:
    //final private RSTile mineTile = new RSTile(3180, 3371, 0);

	// tin:
    //final private RSTile mineTile = new RSTile(3182, 3375, 0);
	//final private RSTile bankTile = new RSTile(3184, 3438, 0);

	///////////////////////////////////////////////////////////////////////////////
    // varrock east:
    final private RSTile bankTile = new RSTile(3253, 3424, 0);

	// copper:
    //final private RSTile mineTile = new RSTile(3286, 3361, 0);

	// tin:
    final private RSTile mineTile = new RSTile(3282, 3363, 0);

	// iron:
    //final private RSTile mineTile = new RSTile(3286, 3368, 0);

	///////////////////////////////////////////////////////////////////////////////
    // close to guild
    //final private RSTile bankTile = new RSTile(0, 0, 0);

	// iron:
    //final private RSTile mineTile = new RSTile(3033, 9826, 0);

	// coal:
    //final private RSTile mineTile = new RSTile(3051, 9777, 0);


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

		return null;
	}

    private int inventoryCount() {
        return ctx.inventory.getCount(getOreType());
    }

    private int doDropAll(RSPlayer myself) {
        log.info("Player at *doDropAll* left : : " + this.inventoryCount());
        if (this.inventoryCount() == 0) {
            return random(500, 1500);
        }

        int itemId = ctx.inventory.getItemID(getOreType());

        // do 10 so we don't spin forever

		ctx.keyboard.pressKey((char) (KeyEvent.VK_SHIFT));
		ctx.mouse.pushDefaultMoveAfter(0);

		sleep(random(100, 200));
		try {
			for (int count=0; count<3; count++) {

				if (this.inventoryCount() == 0) {
					break;
				}

				RSItem[] allInvItems = ctx.inventory.getItems();
				boolean leftToRight = random(0, 100) > 50;
				for (int j = 0; j < 28; j++) {
					int c = leftToRight ? j % 4 : j / 7;
					int r = leftToRight ? j / 4 : j % 7;

					RSItem curItem = allInvItems[c + r * 4];
					if (curItem == null || curItem.getID() != itemId) {
						continue;
					}

					curItem.doClick(true);
					sleep(random(150, 300));
				}
			}
		} finally {
			sleep(random(100, 200));
			ctx.keyboard.releaseKey((char) (KeyEvent.VK_SHIFT));
			ctx.mouse.popDefaultMoveAfter();
		}

		return random(200, 400);
    }

	private int doBank(RSPlayer myself) {
		int distance = calc.distanceTo(bankTile);
        log.info(String.format("Player at *doBank* (distance = %d): %s", distance, myself.getLocation()));

		if (distance > 10) {
			DaxWalker.walkTo(new WalkerTile(bankTile));
		}

        if (ctx.bank.isCollectionOpen()) {
			ctx.bank.closeCollection();
            return random(500, 1000);
        }

        if (!bank.isOpen()) {
            if (bank.open()) {
                sleep(random(500, 750));
            }

            int failCount = 0;
            while (!bank.isOpen()) {
                sleep(50);
                failCount++;
                if (failCount > 20) {
                    log.warn("Bank open failure...");
                    ctx.walking.walkTileMM(bankTile, 1, 1);
                    return random(2000, 3000);
                }
            }
        }

        int count = this.inventoryCount();
        if (count > 0) {
            RSItem item = inventory.getItem(getOreType());
            if (item == null) {
                log.error("Could not find : " + getOreType());
                bank.close();
                this.stopScript(true);
                return -1;
            }

            //if (random(0, 100) > 50 && doDepositAll) {
            // XXX here
            ctx.bank.depositAll();
            //} else {
            //    bank.deposit(item.getID(), 0);
            //}

            sleep(random(1000, 2500));

            int afterCount = this.inventoryCount();
            log.info(String.format("before (%d) and after (%d) counts for %s", count, afterCount, getOreType()));
            if (afterCount != 0) {
                bank.close();
                return random(1000, 2000);
            }
        }

        bank.close();
        return random(1000, 2000);
    }

    private boolean checkObject(RSObject rso) {
        if (rso.getType() != RSObject.Type.GAME) {
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
		log.info(String.format("rso : %f %s %d", distTmp, rso.getName(), rso.getID()));

        if (distTmp > leastDistance) {
            return false;
        }

        return true;
    }

    private boolean doMine() {
        RSPlayer myself = getMyPlayer();
        int distance = calc.distanceTo(mineTile);
        log.info(String.format("Player at *doMine* (distance = %d): %s", distance, myself.getLocation()));

        if (distance > leastDistance * 1.5) {
            DaxWalker.walkTo(new WalkerTile(mineTile));
        }

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
			log.info("faild doAction mine...");

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
                if (bankOre) {
                    this.doBank(myself);

                } else if (dropOre) {
                    doDropAll(myself);

                } else {
					this.stopScript(false);
				}

				return random(1000, 2000);
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
					if (random(1, 15) == 5) {
						log.info("Having a break");
						ctx.mouse.moveOffScreen();

						return random(sleepMult * 5000, sleepMult * 10000);
					}

					return random(sleepMult * 500, sleepMult * 1500);

                } else {
                    // depends what it is
                    return random(3000, 5000);
                }

            } else {
				log.info("state == mining");
				return random(sleepMult * 50, sleepMult * 250);
			}


        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return random(250, 3000);
    }


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
}
