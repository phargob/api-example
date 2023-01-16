package botforlife;

import lombok.extern.slf4j.Slf4j;

import rsb_api.ScriptRunner;

import net.runelite.api.NPC;

import net.runelite.rsb.script.ScriptManifest;

import rsb_api.globval.VarpIndices;
import rsb_api.globval.GlobalWidgetInfo;
import rsb_api.globval.VarpValues;

import rsb_api.globval.enums.InterfaceTab;

import rsb_api.methods.NPCs;

import rsb_api.wrappers.RSNPC;
import rsb_api.wrappers.RSGroundItem;
import rsb_api.wrappers.RSItem;
import rsb_api.wrappers.RSPlayer;

import java.util.ArrayList;

@ScriptManifest(authors = { "phargob" }, name = "MonsterLite kill")


@Slf4j
public class MonsterLite extends ScriptRunner {
    private final boolean FIND_NEAREST_MONSTER = true;

    // use this to return a monster
    private RSNPC getMonster() {

        //return findOne("Chicken");
        return findOne("Cow", "Cow calf");
    }

    private enum State {
        attackAMonster, fighting, done;
    }

    // script state
    private State currentState;

    // stats
    private long startTime;
    private long lastReportTime;
    private long lastKillTime;
    private int totalKills;


    private RSNPC findOne(String... monsters) {
        // XXX needs to ignore unreachables
        int lower = 2;
        int upper = 10;

        // ughh really!
        NPC[] npcs = ctx.npcs.getNPCs();

        ArrayList<RSNPC> candidates = new ArrayList<RSNPC>();

        RSNPC nearestMonster = null;
        double nearestDistance = -1;
        for (NPC npc : npcs) {
            RSNPC rsnpc = new RSNPC(ctx, npc);

            boolean isOurMonster = false;
            for (String name : monsters) {
                if (npc.getName().equals(name)) {
                    isOurMonster = true;
                    break;
                } else {
                    //log.info("What? " + npc.getName());
                }
            }

            if (!isOurMonster) {
                continue;
            }

            int distance = ctx.calc.distanceTo(rsnpc);
            if (distance >= lower && distance <= upper) {
                candidates.add(rsnpc);
            }

            if (nearestMonster == null) {
                nearestMonster = rsnpc;
                nearestDistance = distance;
            } else {
                if (distance < nearestDistance) {
                    nearestMonster = rsnpc;
                    nearestDistance = distance;
                }
            }
        }

        if (FIND_NEAREST_MONSTER)  {
            return nearestMonster;
        }

        if (candidates.size() > 0) {
            return candidates.get(random(0, candidates.size()));
        } else {
            return ctx.npcs.getNearest(monsters);
        }
    }

    private boolean checkCanAttack(RSNPC monster) {
        if (monster.isInCombat() || monster.isBeingAttacked()) {
            return false;
        }

        // other forms of interating...
        return (monster.getInteracting() == null &&
                !monster.isInteractingWithLocalPlayer());
    }

    private int doAttackAMonster(RSPlayer myself) {
        log.info("doAttackAMonster at " + myself.getLocation());

        // just checking we are def idle
        int count = 0;
        for (int i=0; i<5; i++) {
            if (myself.isIdle()) {
                count++;
            }

            sleep(random(50, 100));

            if (myself.isInCombat()) {
                log.info("Player is in combat - probably some misclicking");
                this.currentState = State.fighting;
                return random(2000, 3000);
            }
        }


        if (count != 5) {
            log.info("Not idle - but not in combat??? !!");
            return random(2000, 3000);
        }

        RSNPC monster = getMonster();

        if (monster == null) {
            // Find one
            log.info("No monster available - Dropping back to start");
            return random(20000, 30000);
        }

        if (checkCanAttack(monster)) {

            if (ctx.combat.isAlive(monster)) {
                log.info("monster is alive!");
            }

            log.info(String.format("Attacking monster: ", monster.getName()));

            if (!monster.isOnScreen()) {
                log.info("Rotated camera so I can see " + monster);
                ctx.camera.turnTo(monster);
                sleep(random(250, 750));
            }

            // We passed our checks, let's attack a monster now
            if (monster.doAction("attack", monster.getName())) {
                sleep(random(1500, 3000));
                for (int i=0; i<3; i++) {

                    if (monster.isInCombat() ||
                        monster.isInteractingWithLocalPlayer() ||
                        monster.isBeingAttacked()) {
                        log.info("Successfully attacked monster");
                        this.currentState = State.fighting;
                        return random(500, 2500);
                    }

                    if (!monster.doAction("attack", monster.getName())) {
                        log.info("Failed to attack - starting over");
                        return random(5000, 10000);
                    }

                    sleep(random(500, 3000));
                }

            } else {
                log.info("Failed to attack - back to start");
                return random(5000, 10000);
            }
        }

        log.info("Intermittent fail find monster to attack - try again");
        return random(5000, 10000);
   }

    private int doFighting(RSPlayer myself) {
        log.info("Player is *fighting* at " + myself.getLocation());
        if (myself.isInCombat()) {
            return random(2500, 5000);
        }

        this.totalKills++;
        this.lastKillTime = System.currentTimeMillis();

        if (this.lastKillTime - this.lastReportTime > 10 * 60 * 1000) {
            long elapsed = this.lastKillTime - this.startTime;
            log.info("Been running " + elapsed);
            log.info("Total kills since start " + this.totalKills);
            this.lastReportTime = this.lastKillTime;
        }

        this.currentState = State.attackAMonster;
        return random(1500, 3000);
    }

    @Override
    public int loop() {
        try {
            RSPlayer myself = getMyPlayer();
            if (getMyPlayer() == null)  {
                log.info("Player is null :(");
                return 100;
            }

            // safeties
            long now = System.currentTimeMillis();
            long elapsedSinceStart = now - this.startTime;
            long elapsedSinceKill = now - this.lastKillTime;
            long MIN = 1000 * 60;

            // ten minutes
            if (elapsedSinceKill > 10 * MIN) {
                log.warn("Stopping script since elapsedSinceKill");
                this.stopScript(true);
            }

            // 4 hours
            if (elapsedSinceStart > MIN * 240) {
                log.warn("Stopping script since elapsedSinceStart");
                this.stopScript(true);
            }

            // a state machine
            switch (this.currentState) {
            case attackAMonster:
                return this.doAttackAMonster(myself);
            case fighting:
                return this.doFighting(myself);
            default:
                // State.done
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return random(5000, 10000);
    }

    @Override
    public boolean onStart() {
        log.info("Player at onStart()");

        // set the start time
        this.startTime = System.currentTimeMillis();
        this.totalKills = 0;

        // gets the ball rolling
        log.info("Setting lastKillTime");
        this.lastReportTime = this.lastKillTime = this.startTime;

        this.currentState = State.attackAMonster;

        return true;
    }

}
