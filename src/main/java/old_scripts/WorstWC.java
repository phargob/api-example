package basicloopbot;

// booo :(
import net.runelite.api.NPC;

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
import net.runelite.rsb.wrappers.RSPlayer;
import net.runelite.rsb.wrappers.RSObject;
import net.runelite.rsb.wrappers.RSWidget;
import net.runelite.rsb.wrappers.RSModel;
import net.runelite.rsb.wrappers.RSTile;
import net.runelite.rsb.wrappers.RSNPC;
import net.runelite.rsb.wrappers.subwrap.WalkerTile;
import net.runelite.rsb.internal.globval.enums.InterfaceTab;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.util.Set;
import java.util.ArrayList;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.NpcID;

@ScriptManifest(
        authors = { "phargob" }, name = "worst WC", version = 0.1,
        description = "worst WC")

@Slf4j
public class WorstWC extends Script {


	private static final Set<Integer> EVENT_NPCS = ImmutableSet.of(
		NpcID.BEE_KEEPER_6747,
		NpcID.CAPT_ARNAV,
		NpcID.DR_JEKYLL, NpcID.DR_JEKYLL_314,
		NpcID.DRUNKEN_DWARF,
		NpcID.DUNCE_6749,
		NpcID.EVIL_BOB, NpcID.EVIL_BOB_6754,
		NpcID.FLIPPA_6744,
		NpcID.FREAKY_FORESTER_6748,
		NpcID.FROG_5429,
		NpcID.GENIE, NpcID.GENIE_327,
		NpcID.GILES, NpcID.GILES_5441,
		NpcID.LEO_6746,
		NpcID.MILES, NpcID.MILES_5440,
		NpcID.MYSTERIOUS_OLD_MAN_6750, NpcID.MYSTERIOUS_OLD_MAN_6751,
		NpcID.MYSTERIOUS_OLD_MAN_6752, NpcID.MYSTERIOUS_OLD_MAN_6753,
		NpcID.NILES, NpcID.NILES_5439,
		NpcID.PILLORY_GUARD,
		NpcID.POSTIE_PETE_6738,
		NpcID.QUIZ_MASTER_6755,
		NpcID.RICK_TURPENTINE, NpcID.RICK_TURPENTINE_376,
		NpcID.SANDWICH_LADY,
		NpcID.SERGEANT_DAMIEN_6743
	);


    ///////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////

	// only useful if chopping "Tree"
    boolean doAFK = true;

	// will hit the deposit all button when banking
    boolean doDepositAll = true;

	// for testing, does not bank
	boolean stopIfFull = false;

	// only set if you want locally chop trees only.  wont walk to tryTiles
	boolean dontWalk = false;

	int MIN_DISTANCE = 16;
    int LAZYNESS = 1;
	int MAX_LOGS_STOP = 1000;

    ///////////////////////////////////////////////////////////////////////////////
	// grand exchange
	// note to self: use vertical camera view

    // final String treeType = "Tree";
    // final String logType = "Log";
	// final private RSTile tryTiles[] = {
	// 	new RSTile(3167, 3490, 0)
    // };

    // final private RSTile bankTile = new RSTile(3167, 3490, 0);

    ///////////////////////////////////////////////////////////////////////////////
    //draynor

    // final String treeType = "Oak";
    // final String logType = "Oak logs";
    // final private RSTile tryTiles[] = {
    //     new RSTile(3085, 3294, 0),
    //     new RSTile(3097, 3290, 0),
    //     new RSTile(3106, 3281, 0),
    //     new RSTile(3071, 3252, 0)
    // };

    // final private RSTile bankTile = new RSTile(3093, 3243, 0);

    ///////////////////////////////////////////////////////////////////////////////
    // draynor willows
    // final String treeType = "Willow";
    // final String logType = "Willow logs";
    // final private RSTile tryTiles[] = {
    //     new RSTile(3083, 3239, 0),
    //     new RSTile(3087, 3236, 0),
    //     new RSTile(3087, 3236, 0),
    //     new RSTile(3087, 3236, 0),
    //     new RSTile(3089, 3229, 0)
    // };
    // final private RSTile bankTile = new RSTile(3092, 3246, 0);

    ///////////////////////////////////////////////////////////////////////////////

	// varrock east bank
    final String treeType = "Oak";
    final String logType = "Oak logs";

    final private RSTile tryTiles[] = {
        new RSTile(3279, 3437, 0),
        new RSTile(3281, 3428, 0),
        new RSTile(3281, 3415, 0),
        new RSTile(3281, 3415, 0)
    };

    final private RSTile bankTile = new RSTile(3253, 3421, 0);

    ///////////////////////////////////////////////////////////////////////////////
    // edgville willow
    // final String treeType = "Willow";
    // final String logType = "Willow logs";
    // final private RSTile tryTiles[] = {
    //     new RSTile(3118, 3498, 0)
    // };

    // final private RSTile bankTile = new RSTile(3096, 3494, 0);

    ///////////////////////////////////////////////////////////////////////////////
    // grand exchange yew
    // final String treeType = "Yew";
    // final String logType = "Yew logs";
    // final private RSTile tryTiles[] = {
    //     new RSTile(3209, 3503, 0),
    //     new RSTile(3220, 3503, 0)
    // };

    // final private RSTile bankTile = new RSTile(3167, 3490, 0);

    ///////////////////////////////////////////////////////////////////////////////
    // Draynor yew
    // final String treeType = "Yew";
    // final String logType = "Yew logs";
    // final private RSTile tryTiles[] = {
    //     new RSTile(3165, 3222, 0),
    //     new RSTile(3150, 3230, 0)
    // };

    // final private RSTile bankTile = new RSTile(3093, 3243, 0);


    ///////////////////////////////////////////////////////////////////////////////

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

    boolean lastWasMoveOffScreen = false;
    private void antiBan() {
        int random = random(1, 30);
        switch (random) {
        case 1:
            if (ctx.game.getCurrentTab() != InterfaceTab.SKILLS) {
                game.openTab(InterfaceTab.SKILLS);

            } else if (ctx.game.getCurrentTab() != InterfaceTab.INVENTORY) {
                game.openTab(InterfaceTab.INVENTORY);
            }

			log.info("Switch tab");
			lastWasMoveOffScreen = false;
			break;
        case 2:
            log.info("Move camera");
			int angle = camera.getAngle() + random(-90, 90);
			if (angle < 0) {
				angle = 0;
			}

			if (angle > 359) {
				angle = 0;
			}

			camera.setAngle(angle);
			break;

		case 3:
			log.info("Move randomly");
			RSPlayer myself = getMyPlayer();
			myself.doHover();

			int x = random(4, 12);
			for (int i = 0; i<x; i++) {
				if (random(1, 100) > 50) {
					myself.doHover();
				} else {
					ctx.mouse.moveRandomly(random(50, 400));
				}

				sleep(50, 200);
			}

			lastWasMoveOffScreen = false;
			break;

		default:
            if (!lastWasMoveOffScreen && random(1, 20) < 10) {
				log.info("Move offscreen");
                ctx.mouse.moveOffScreen();
                lastWasMoveOffScreen = true;
            }
        }

    }

    private int doBank(RSPlayer myself) {
		if (bankTile == null) {
			log.warn("bank is null :(");
			this.stopScript();
			return -1;
		}

		int distance = calc.distanceTo(bankTile);
		if (distance > 10) {
			checkRun();

			if (!DaxWalker.walkTo(new WalkerTile(bankTile))) {
				log.info("DAX.walkTo failed");
				return random(2000, 5000);
			}

			distance = calc.distanceTo(bankTile);
			sleep(random(400, 800));

			if (distance > 10) {
				ctx.walking.walkTileMM(bankTile, 2, 2);
				while (true) {
					sleep(random(400, 800));
					if (!myself.isLocalPlayerMoving()) {
						break;
					}
				}
			}

			camera.turnTo(bankTile);
			sleep(random(800, 1600));

			// update distance
			distance = calc.distanceTo(bankTile);
		}

        log.info(String.format("Player at *doBank* (distance = %d): %s", distance, myself.getLocation()));

        if (ctx.bank.isCollectionOpen()) {
            log.warn("oops isCollectionOpen(), closing by moving away");
            ctx.walking.walkTileMM(bankTile, 1, 1);
            return random(2000, 3000);
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

        int count = ctx.inventory.getCount(logType);
        if (count > 0) {
            RSItem item = inventory.getItem(logType);
            if (item == null) {
                log.error("Could not find : " + logType);
                bank.close();
                this.stopScript(true);
                return -1;
            }

            if (random(0, 100) > 50 && doDepositAll) {
                ctx.bank.depositAll();
            } else {
                bank.deposit(item.getID(), 0);
            }

            sleep(random(1000, 2500));

            int afterCount = ctx.inventory.getCount(logType);
			int inBankCount = ctx.bank.getCount(item.getID());
            log.info(String.format("before (%d) and after (%d) counts for %s, in bank: %d",
								   count, afterCount, logType, inBankCount));


			if (inBankCount > MAX_LOGS_STOP) {
                this.stopScript(true);
				return -1;
			}
        }

        bank.close();
        return random(1000, 15000);
    }

    private RSTile randomTile() {
        int dist = 999;
        RSTile result = tryTiles[0];
        for (RSTile tile : tryTiles) {
            int distance = calc.distanceTo(tile);
            if (random(1, 100) < 25) {
                return tile;
            }

            if (distance < dist) {
                dist = distance;
                result = tile;
            }
        }

        return result;
    }

    private int shortestDistance() {
        int dist = 999;
        for (RSTile tile : tryTiles) {
            int distance = calc.distanceTo(tile);
            if (distance < dist) {
                dist = distance;
            }
        }

        return dist;
    }

    private int findTreeAndCut() {
        RSPlayer myself = getMyPlayer();

        RSObject nearestRSO = null;
        double shortestDistance = 10000;
        boolean dump = false;
        for (RSObject rso: ctx.objects.getAll()) {
            if (rso == null) {
                continue;
            }

            if (rso.getType() == RSObject.Type.GAME) {
                double distTmp = ctx.calc.distanceBetween(myself.getLocation(), rso.getLocation());
                if (distTmp > 25) {
                    continue;
                }
                if (dump) {
                    log.info(String.format("rso %s %d %f", rso.getName(), rso.getID(), distTmp));
                }
                if (rso.getName().equals(treeType)) {
                    if (distTmp < shortestDistance) {
                        shortestDistance = distTmp;
                        nearestRSO = rso;
                    }
                }
            }
        }

        //log.info(String.format("nearest tree %s %d %f", nearestRSO, nearestRSO.getID(), shortestDistance));

        if (nearestRSO == null || shortestDistance > MIN_DISTANCE) {
            return -1;
        }

        if (shortestDistance > 2) {
            log.info("Pathing to tree");
            RSPath path = ctx.walking.getPath(nearestRSO.getLocation());

            if (path.traverse()) {
                log.info("Path success");
            }

            while (true) {
                sleep(200);
                if (myself.isIdle()) {
                    break;
                }
            }

            camera.turnTo(nearestRSO);
            sleep(random(800, 1600));
        }

        RSModel model = nearestRSO.getModel();
        if (model == null) {
            log.info(String.format("----> model is null"));
            if (nearestRSO.getLocation().doAction("Chop down", treeType)) {
                sleep(200);
                return (int) (shortestDistance + 1);
            }
        } else {
            if (nearestRSO.getLocation().doAction("Chop down", treeType)) {
                sleep(200);
                return (int) (shortestDistance + 1);
            }
        }

        return -1;
    }

    void checkRun() {
        log.info(String.format("checkRun: %d", ctx.walking.getEnergy()));

        if (!ctx.walking.isRunEnabled() && ctx.walking.getEnergy() > 60) {
            log.info("checkRun: setRun");
            ctx.walking.setRun(true);
            sleep(random(300, 750));
            log.info("after setRun() -> {}", ctx.walking.isRunEnabled());
        }
    }

	long lastTimeInteractedAction = -1;
	private boolean areWeInteracting(RSPlayer myself) {
		// ughh really!
		NPC[] npcs = ctx.npcs.getNPCs();

		ArrayList<RSNPC> candidates = new ArrayList<RSNPC>();
		for (NPC npc : npcs) {
			RSNPC rsnpc = new RSNPC(ctx, npc);

			int distance = ctx.calc.distanceTo(rsnpc);
			if (distance > 5) {
				continue;
			}

			if (rsnpc.isInteractingWithLocalPlayer()) {
				log.warn("We are interacting with: {} / {}", rsnpc.getName(), rsnpc.getID());

				boolean isDog = false;
				boolean isRandom = false;
				for (String s: rsnpc.getActions()) {
					if (s != null) {
						log.info("Actions are: {}", s);
						if (s.equals("Dismiss")) {
							isRandom = true;
						}
						if (s.equals("Shoo-away")) {
							isDog = true;
						}
					}
				}

				long t = System.currentTimeMillis();
				if (t - lastTimeInteractedAction > 15000) {

					if (isRandom) {
						log.info("Stupid random");
						if (rsnpc.doAction("Dismiss", rsnpc.getName())) {
							lastTimeInteractedAction = t;
							return true;
						}
					}

					if (isDog) {
						log.info("Stupid dog");
						if (random(1, 6) == 5) {
							if (rsnpc.doAction("Shoo-away", rsnpc.getName())) {
								lastTimeInteractedAction = t;
								return true;
							}
						}
					}
				}
			}

		}

		return false;
	}

    boolean cutting = false;
	int afkCount = 0;
    @Override
    public int loop() {
        RSPlayer myself = getMyPlayer();

		// checking for randoms part 1
		if (this.areWeInteracting(myself)) {
			return random(1000, 2000);
		}

		try {
            if (!cutting) {
				if (myself.isIdle()) {
					if (ctx.inventory.isFull()) {
						if (this.stopIfFull) {
							log.warn("Stopping script since stopIfFull");
							this.stopScript(false);
						} else {
							return this.doBank(myself);
						}
					}

                    lastWasMoveOffScreen = false;
                    int distance = this.findTreeAndCut();
                    if (distance != -1) {
                        log.info("now cutting a tree");
                        cutting = true;
						afkCount = 0;
                        return random(distance * 1000, distance * 1500);

                    } else {
                        log.info("no tree found");

                        if (shortestDistance() < MIN_DISTANCE) {
                            ctx.mouse.moveOffScreen();
                            return random(2000, 5000);
                        }

						if (dontWalk) {
							return random(2500, 5000);
						}

                        RSTile startTile = randomTile();
                        checkRun();
						if (DaxWalker.walkTo(new WalkerTile(startTile))) {
							return random(500, 1000);
						} else {
							log.info("DAX.walkTo failed");
							return random(2000, 5000);
						}
                    }

                } else {
                    log.info("WTF not idle");
                    return random(5000, 10000);
                }
            } else {
                for (int i=0; i<25; i++) {
                    if (!myself.isIdle()) {

                        // need more afk option here
                        if (doAFK) {
							afkCount++;
                            log.info(String.format("still cutting a tree %d", afkCount));

							// forces it to reclick tree
							// XXX change this to since last clicked
							if (afkCount > 50) {
								log.info("reseting cutting");
								ctx.walking.walkTileMM(tryTiles[0], 1, 1);
								cutting = false;
							} else {
								antiBan();
							}

                            return random(5000, 10000);
                        } else {
                            return random(500, 2000);
                        }
                    }

                    sleep(100);
                }

                log.info("finished cutting a tree");
                cutting = false;
				if (doAFK) {
					return random(1000 * LAZYNESS, 2500 * LAZYNESS);
				} else {
					return random(500, 1000);
				}
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return random(500, 1000);
    }
}
