package dax.utils;

import dax.Ctx;

import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.cache.definitions.ObjectDefinition;

import dax.path.DaxTile;

import dax.utils.StdRandom;

import rsb.wrappers.RSModel;
import rsb.wrappers.RSTile;
import rsb.wrappers.RSObject;

import rsb.wrappers.common.Clickable;
import rsb.wrappers.common.Positionable;
import rsb.wrappers.WalkerTile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class does NOT examine objects.
 * <p>
 * clickAction should never include the target entity's name. Just the action.
 */
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AccurateMouse {

    public static void click(int button) {
        click(Ctx.ctx.mouse.getLocation(), button);
    }

    public static void click(int x, int y) {
        click(x, y, 1);
    }

    public static void click(Point point) {
        click(point, 1);
    }

    public static void click(Point point, int button) {
        click(point.getX(), point.getY(), button);
    }

    public static void click(int x, int y, int button) {
        Ctx.ctx.mouse.click(x, y, button==1);
    }

    public static boolean click(Clickable clickable, String... clickActions) {
        return action(clickable, false, clickActions);
    }

    public static boolean hover(Clickable clickable, String... clickActions) {
        return action(clickable, true, clickActions);
    }

    public static boolean clickMinimap(Positionable tile) {
        if (tile == null) {
            log.info("clickMinimap 1, tile is null :(");
            return false;
        }

        // still walking and reached destination
        RSTile currentDestination = Ctx.ctx.walking.getDestination();
        if (currentDestination != null && Ctx.ctx.calc.distanceBetween(currentDestination,
                                                                       tile.getLocation()) < 1.5) {
            return true;
        }

        boolean alreadyClicked = false;
        // attempt to click on canvas percentClickage of the time
        // XXX percentClickage = 101 --> turned off
        // XXX do this differently
        // final int percentClickage = 101;
        // boolean alreadyClicked = false;
        // if (StdRandom.uniform(0, 100) > percentClickage &&
        //     Ctx.ctx.calc.tileOnScreen(tile.getLocation())) {
        //     double x = StdRandom.uniform(2, 8) / 10.0;
        //     double y = StdRandom.uniform(2, 8) / 10.0;
        //     Point point = Ctx.ctx.calc.tileToScreen(tile.getLocation(), x, y, 0);
        //     if (point != null) {
        //         Ctx.ctx.mouse.move(point.getX(), point.getY());
        //         if (Ctx.ctx.menu.doAction("Walk here")) {
        //             alreadyClicked = true;
        //         }

        //      Ctx.ctx.sleep(StdRandom.uniform(500, 1000));
        //     }
        // }

        if (!alreadyClicked) {
            Point point = null;
            for (int ii=0; ii<5; ii++) {
                point = Ctx.ctx.calc.tileToMinimap(tile.getLocation());
                if (point != null) {
                    break;
                }

                Ctx.ctx.sleep(StdRandom.uniform(20, 30));
            }

            if (point == null) {
                return false;
            }

            // this is the click
            AccurateMouse.click(point);
        }

        RSTile newDestination = WaitFor.getValue(250, () -> {
                RSTile dest = Ctx.ctx.walking.getDestination();
                return dest == null || dest.equals(currentDestination) ? null : dest;
            });

        if (newDestination == null) {
            var l = Ctx.getMyLocation();
            return Ctx.ctx.calc.distanceBetween(l, tile.getLocation()) < 1.5;
        }

        if (Ctx.ctx.calc.distanceBetween(newDestination,
                                         tile.getLocation()) < 1.5) {
            return true;
        }

        return false;
    }

    public static void hoverMinimap(DaxTile node) {
        if (node == null) {
            return;
        }

        WalkerTile t = node.toWalkerTile();
        Ctx.ctx.mouse.move(Ctx.ctx.calc.tileToMinimap(t));
    }

    public static boolean action(Clickable clickable, boolean hover, String... clickActions) {
        if (clickable == null) {
            return false;
        }
        String name = null;
        RSModel model = null;
        if (clickable instanceof RSObject) {
            RSObject rsObject = ((RSObject) clickable);
            ObjectDefinition rsObjectDefinition = rsObject.getDef();
            name = rsObjectDefinition != null ? rsObjectDefinition.getName() : null;
            model = rsObject.getModel();
        }
        return action(model, clickable, name, hover, clickActions);
    }

    /**
     * @param model        model of {@code clickable}
     * @param clickable    target entity
     * @param clickActions actions to click or hover. Do not include {@code targetName}
     * @param targetName   name of the {@code clickable} entity
     * @param hover        True to hover the OPTION, not the entity model. It will right click {@code clickable} and hover over option {@code clickAction}
     * @return whether action was successful.
     */
    private static boolean action(RSModel model,
                                  Clickable clickable,
                                  String targetName,
                                  boolean hover, String... clickActions) {
        for (int i = 0; i < 5; i++) {
            if (attemptAction(model, clickable, targetName, hover, clickActions)) {
                return true;
            }

            Ctx.ctx.sleep(StdRandom.uniform(150, 300));
        }

        return false;
    }


    // public static boolean walkScreenTile(WalkerTile destination) {
    //     if (!destination.isOnScreen() || !destination.isClickable()) {
    //         return false;
    //     }

    //     for (int i = 0; i < StdRandom.uniform(3, 5); i++) {
    //         Point point = getWalkingPoint(destination);
    //         if (point == null) {
    //             continue;
    //         }

    //      if (WaitFor.condition(100, () -> {
    //                  String uptext = Ctx.ctx.menu.getHoverText();
    //                  return uptext != null && uptext.startsWith("Walk here") ? WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE;
    //              }) != WaitFor.Return.SUCCESS) {
    //          return false;
    //      }

    //      click(1);

    //      WalkerTile clicked = new WalkerTile(Objects.requireNonNull(WaitFor.getValue(900, Ctx.ctx.walking::getDestination)));
    //      return clicked.equals(destination) || Ctx.ctx.players.getMyPlayer().getPosition().equals(destination);
    //  }

    //  return false;
    // }

    // public static boolean hoverScreenTileWalkHere(WalkerTile destination) {
    //     for (int i = 0; i < Ctx.ctx.random(4, 6); i++) {
    //         Point point = getWalkingPoint(destination);
    //         if (point == null) {
    //             continue;
    //         }
    //         Ctx.ctx.mouse.move(point);
    //         Ctx.ctx.sleep(StdRandom.uniform(20, 30));
    //         return isHoveringScreenTileWalkHere(destination);
    //     }
    //     ;
    //     return false;
    // }

    // public static boolean isHoveringScreenTileWalkHere(WalkerTile destination) {
    //     return isWalkingPoint(new java.awt.Point(Ctx.ctx.mouse.getLocation().getX(),
    //                                           Ctx.ctx.mouse.getLocation().getY())
    //                        , destination);
    // }

    /**
     * Clicks or hovers desired action of entity.
     *
     * @param model        target entity model
     * @param clickable    target entity
     * @param clickActions actions
     * @param targetName   name of target
     * @param hover        hover option or not
     * @return result of action
     */
    private static boolean attemptAction(RSModel model,
                                         Clickable clickable,
                                         String targetName,
                                         boolean hover,
                                         String... clickActions) {

        // XXX log
        System.out.println((hover ? "Hovering over" : "Clicking on") + " " + targetName + " with [" + Arrays.stream(clickActions).reduce("", String::concat) + "]");

        if (model == null) {
            return false;
        }

        if (!hover) {
            return model.doAction(clickActions[0]);
        } else {
            model.doHover();
            return true;
        }
    }
}
