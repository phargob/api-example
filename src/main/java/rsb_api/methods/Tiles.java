package rsb_api.methods;

import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;

import rsb_api.wrappers.RSTile;
import rsb_api.wrappers.subwrap.WalkerTile;

/**
 * Tile related operations.
 */
public class Tiles {

    private MethodContext ctx;
    Tiles(final MethodContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Clicks a tile if it is on screen. It will left-click if the action is
     * available as the default option, otherwise it will right-click and check
     * for the action in the context methods.menu.
     *
     * @param tile   The RSTile that you want to click.
     * @param action Action command to use click
     * @return <code>true</code> if the tile was clicked; otherwise
     *         <code>false</code>.
     */
    public boolean doAction(final RSTile tile, final String action) {
        return doAction(tile, action, null);
    }

    /**
     * Clicks a tile if it is on screen. It will left-click if the action is
     * available as the default menu action, otherwise it will right-click and check
     * for the action in the context methods.menu.
     *
     * @param tile   The RSTile that you want to click.
     * @param action Action of the menu entry to click
     * @param option Option of the menu entry to click
     * @return <code>true</code> if the tile was clicked; otherwise
     *         <code>false</code>.
     */
    public boolean doAction(final RSTile tile, final String action, final String option) {
        // XXX Height to click the <code>RSTile</code> at. Use 1 for tables, 0 by default.
        final int height = 0;

        try {
            double lower = 0.1;
            double upper = 0.9;
            for (int i = 0; i < 8; i++) {
                Point location = ctx.calc.tileToScreen(tile,
                                                       ctx.randomLinear(lower, upper),
                                                       ctx.randomLinear(lower, upper),
                                                       height);
                if (location.getX() == -1 || location.getY() == -1) {
                    return false;
                }

                ctx.mouse.move(location.getX(), location.getY());

                if (ctx.menu.getIndex(action, option) != -1) {
                    return ctx.menu.doAction(action, option);
                }

                for (int j=0; j<3; j++) {
                    var next = ctx.calc.tileToScreen(tile,
                                                     ctx.randomLinear(lower, upper),
                                                     ctx.randomLinear(lower, upper),
                                                     height);

                    ctx.mouse.move(next.getX(), next.getY());

                    if (ctx.menu.getIndex(action, option) != -1) {
                        return ctx.menu.doAction(action, option);
                    }
                }

                ctx.sleepRandom(100, 250);
                lower = Math.min(0.5, lower + 0.1);
                upper = Math.max(0.5, upper - 0.1);
            }

            return false;

        } catch (Exception e) {
            //log.warn("Shit" + e);
            return false;
        }
    }

    /**
     * Returns the RSTile under the mouse.
     *
     * @return The <code>RSTile</code> under the mouse, or null if the mouse is
     *         not over the viewport.
     */
    public RSTile getTileUnderMouse() {
        Point p = ctx.mouse.getLocation();
        if (!ctx.calc.pointOnScreen(p)) {
            return null;
        }
        RSTile close = null;
        for (int x = 0; x < 104; x++) {
            for (int y = 0; y < 104; y++) {
                RSTile t = new RSTile(x + ctx.proxy.getBaseX(), y
                        + ctx.proxy.getBaseY(), ctx.proxy.getPlane());
                Point s = ctx.calc.tileToScreen(t);
                if (s.getX() != -1 && s.getY() != -1) {
                    if (close == null) {
                        close = t;
                    }
                    if (ctx.calc.tileToScreen(close).distanceTo(p) > ctx.calc
                            .tileToScreen(t).distanceTo(p)) {
                        close = t;
                    }
                }
            }
        }
        return close;
    }

    /**
     * Gets the tile under a point.
     *
     * @param point a point (X, Y)
     * @return RSTile at the point's location
     */
    public RSTile getTileUnderPoint(final Point point) {
        if (!ctx.calc.pointOnScreen(point)) {
            return null;
        }
        RSTile close = null;
        for (int x = 0; x < 104; x++) {
            for (int y = 0; y < 104; y++) {
                RSTile tile = new RSTile(x + ctx.proxy.getBaseX(), y
                        + ctx.proxy.getBaseY(), ctx.proxy.getPlane());
                Point pointOfTile = ctx.calc.tileToScreen(tile);
                if (pointOfTile.getX() != -1 && pointOfTile.getY() != -1) {
                    if (close == null) {
                        close = tile;
                    }
                    if (ctx.calc.tileToScreen(close).distanceTo(point) > ctx.calc
                            .tileToScreen(tile).distanceTo(point)) {
                        close = tile;
                    }
                }
            }
        }
        return close;
    }

    public WalkerTile createWalkerTile(RSTile tile) {
        var x = tile.getWorldLocation().getX();
        var y = tile.getWorldLocation().getY();
        var plane = tile.getWorldLocation().getPlane();

        return new WalkerTile(ctx, x, y, plane, WalkerTile.TYPES.WORLD);
    }

    public WalkerTile createWalkerTile(WorldPoint point) {
        var x = point.getX();
        var y = point.getY();
        var plane = point.getPlane();

        return new WalkerTile(ctx, x, y, plane, WalkerTile.TYPES.WORLD);
    }

    public WalkerTile createWalkerTile(int x, int y, int plane) {
        return new WalkerTile(ctx, x, y, plane, WalkerTile.TYPES.WORLD);
    }

}
