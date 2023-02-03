package basicloopbot;
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
import net.runelite.rsb.internal.globval.enums.InterfaceTab;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;

@ScriptManifest(
        authors = { "phargob" }, name = "FireStarter", version = 0.1,
        description = "FireStarter")

@Slf4j
public class FireStarter extends Script {

    @Override
    public boolean onStart() {
        return true;
    }

    String[] LOGS = {"Logs", "Oak logs", "Willow logs", "Maple logs", "Yew logs"};
    private int doTinderBox(RSPlayer myself) {
        if (ctx.game.getCurrentTab() != InterfaceTab.INVENTORY) {
            ctx.game.openTab(InterfaceTab.INVENTORY);
            sleep(random(200, 1000));
        }

        final int maxTries = 3;
        boolean selected = false;
        for (int jj=0; jj<maxTries; jj++) {
            RSItem tinderbox = ctx.inventory.getItem("Tinderbox");
            if (tinderbox == null) {
                log.warn(String.format("Tinderbox is null"));
                continue;
            }

            if (!tinderbox.doAction("Use")) {
                log.warn("Tinderbox doAction(\"use\") failed");
                sleep(random(500, 1000));
                continue;
            }

            log.info(String.format("Tinderbox selected"));
            selected = true;
            sleep(random(300, 1200));
            break;
        }

        if (!selected) {
            log.info(String.format("No item selected"));
            return -1;
        }

        log.info(String.format("Selected tinderbox"));

        RSItem[] allInvItems = ctx.inventory.getItems();
        boolean leftToRight = random(1, 100) > 50 ;
        int[] itemIds = ctx.inventory.getItemIDs(LOGS);
        for (int j = 0; j < 28; j++) {
            int c = leftToRight ? j % 4 : j / 7;
            int r = leftToRight ? j / 4 : j % 7;

            RSItem curItem = allInvItems[c + r * 4];
            if (curItem == null) {
                continue;
            }

            // i hate java
            boolean isInItems = false;
            for (int x : itemIds) {
                if (x == curItem.getID()) {
                    isInItems = true;
                    break;
                }
            }

            if (!isInItems) {
                continue;
            }

			// do action
			if (!curItem.doClick(true)) {
                log.warn("Failed to fire start - stopping");
                return -1;
            }

            break;
        }

        return random(1000, 2000);
     }

    @Override
    public int loop() {
        try {
            if (ctx.inventory.getCount(LOGS) == 0) {
                this.stopScript(false);
                return 1000;
            }

            log.info(String.format("Left with %d", ctx.inventory.getCount(LOGS)));

            RSPlayer myself = getMyPlayer();

			// just checking we are def idle
			int count = 0;
			for (int i=0; i<5; i++) {
				if (myself.isIdle()) {
					count++;
				}

				sleep(random(50, 100));
			}

			if (count == 5) {
				sleep(random(100, 400));
                return doTinderBox(myself);
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return random(1000, 3000);
    }
}
