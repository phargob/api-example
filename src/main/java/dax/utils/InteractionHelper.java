package dax.utils;

import dax.Ctx;

import dax.utils.StdRandom;

import net.runelite.rsb.utils.Filter;
import net.runelite.rsb.util.Timer;

import rsb.wrappers.RSObject;
import rsb.wrappers.common.Clickable07;
import rsb.wrappers.common.Positionable;
import rsb.wrappers.WalkerTile;


public class InteractionHelper {

    public static boolean click(Clickable07 clickable, String... actions) {
        return click(clickable, actions, null);
    }

    public static boolean click(Clickable07 clickable, String action, WaitFor.Condition condition) {
        return click(clickable, new String[]{action}, condition);
    }

    /**
     * Interacts with nearby object and waits for {@code condition}.
     *
     * @param clickable clickable entity
     * @param actions actions to click
     * @param condition condition to wait for after the click action
     * @return if {@code condition} is null, then return the outcome of condition.
     *          Otherwise, return the result of the click action.
     */
    public static boolean click(Clickable07 clickable, String[] actions, WaitFor.Condition condition){
        if (clickable == null){
            return false;
        }

        WalkerTile position = ((Positionable) clickable).getLocation();

        if (!isOnScreenAndClickable(clickable)){
            Ctx.ctx.walking.walkTo(Ctx.ctx.walking.randomizeTile(position, 10, 10));
        }

        WaitFor.Return result = WaitFor.condition(WaitFor.getMovementRandomSleep(position), new WaitFor.Condition() {
            final long startTime = System.currentTimeMillis();
            @Override
            public WaitFor.Return active() {
                if (isOnScreenAndClickable(clickable)){
                    return WaitFor.Return.SUCCESS;
                }
                if (Timer.timeFromMark(startTime) > 2000 && !Ctx.ctx.players.getMyPlayer().isLocalPlayerMoving()){
                    return WaitFor.Return.FAIL;
                }
                return WaitFor.Return.IGNORE;
            }
        });

        if (result != WaitFor.Return.SUCCESS){
            return false;
        }

        if (!AccurateMouse.click(clickable, actions)){
            if (Ctx.ctx.camera.getAngle() < 90){
                Ctx.ctx.camera.setPitch(StdRandom.uniform(90, 100));
            }
            return false;
        }

        return condition == null || WaitFor.condition(StdRandom.uniform(7000, 8500), condition) == WaitFor.Return.SUCCESS;
    }

    private static boolean isOnScreenAndClickable(Clickable07 clickable){
        if (clickable instanceof RSObject && !((RSObject) clickable).isOnScreen()){
            return false;
        }
        return clickable.isClickable();
    }

}
