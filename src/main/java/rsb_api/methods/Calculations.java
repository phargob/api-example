package rsb_api.methods;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.Point;
import rsb_api.wrappers.*;
import rsb_api.wrappers.common.Positionable;
import rsb_api.wrappers.subwrap.WalkerTile;

import java.awt.*;

/**
 * Game world and projection calculations.
 */
@Slf4j
public class Calculations {

    private MethodContext ctx;

    /**
     * Creates the singleton for calculations
     * @param ctx   The bot context to associate this calculations object with
     */
    Calculations(final MethodContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Checks whether a given tile is on the minimap.
     *
     * @param t The Tile to check.
     * @return <code>true</code> if the RSTile is on the minimap; otherwise
     *         <code>false</code>.
     * @see #tileToMinimap(RSTile)
     */
    public boolean tileOnMap(RSTile t) {
        return tileToMinimap(t) != null;
    }

    /**
     * Checks whether the centroid of a given tile is on the screen.
     *
     * @param t The RSTile to check.
     * @return <code>true</code> if the RSTile is on the screen; otherwise
     *         <code>false</code>.
     */
    public boolean tileOnScreen(RSTile t) {
        Point point = tileToScreen(t, 0.5, 0.5, 0);
        return (point != null) && pointOnScreen(point);
    }

    /**
     * Returns the Point on screen where a given tile is shown on the minimap.
     *
     * @param t The RSTile to check.
     * @return <code>Point</code> within minimap; otherwise
     *         <code>new Point(-1, -1)</code>.
     */

    public Point tileToMinimap(RSTile t) {
        return worldToMinimap(t.getWorldLocation().getX(), t.getWorldLocation().getY());
    }

    /**
     * Checks whether a point is within the rectangle that determines the bounds
     * of game screen. This will work fine when in fixed mode. In resizable mode
     * it will exclude any points that are less than 253 pixels from the right
     * of the screen or less than 169 pixels from the bottom of the screen,
     * giving a rough area.
     *
     * @param check The point to check.
     * @return <code>true</code> if the point is within the rectangle; otherwise
     *         <code>false</code>.
     */
    public boolean pointOnScreen(Point check) {
        int x = check.getX(), y = check.getY();
        return x > ctx.proxy.getViewportXOffset() && x < ctx.proxy.getViewportWidth()
            && y > ctx.proxy.getViewportYOffset() && y < ctx.proxy.getViewportHeight();
    }

    /**
     * Calculates the distance between two points.
     *
     * @param curr The first point.
     * @param dest The second point.
     * @return The distance between the two points, using the distance formula.
     * @see #distanceBetween(RSTile, RSTile)
     */
    public double distanceBetween(Point curr, Point dest) {
        return Math.sqrt(((curr.getX() - dest.getX()) * (curr.getX() - dest.getX())) + ((curr.getY() - dest.getY()) * (curr.getY() - dest.getY())));
    }

    /**
     * Will return the closest tile that is on screen to the given tile.
     *
     * @param tile Tile you want to get to.
     * @return <code>RSTile</code> that is onScreen.
     */
    public RSTile getTileOnScreen(RSTile tile) {
        try {
            if (tileOnScreen(tile)) {
                return tile;
            } else {
                RSTile loc = new RSTile(ctx.proxy.getLocalPlayer().getWorldLocation().getX(),
                                        ctx.proxy.getLocalPlayer().getWorldLocation().getY(), ctx.proxy.getPlane());
                RSTile halfWayTile = new RSTile((tile.getWorldLocation().getX() +
                                                 loc.getWorldLocation().getX()) / 2, (tile.getWorldLocation().getY() +
                                                                                      loc.getWorldLocation().getY()) / 2, ctx.proxy.getPlane());

                if (tileOnScreen(halfWayTile)) {
                    return halfWayTile;
                } else {
                    return getTileOnScreen(halfWayTile);
                }
            }
        } catch (StackOverflowError soe) {
            return null;
        }
    }
    /**
     * Returns the angle to a given tile in degrees anti-clockwise from the
     * positive x axis (where the x-axis is from west to east).
     *
     * @param t The target tile
     * @return The angle in degrees
     */
    public int angleToTile(RSTile t) {
        RSTile me = new RSTile(ctx.proxy.getLocalPlayer().getWorldLocation().getX(),
                               ctx.proxy.getLocalPlayer().getWorldLocation().getY(), ctx.proxy.getPlane());
        int angle = (int) Math.toDegrees(Math.atan2(t.getWorldLocation().getY() - me.getWorldLocation().getY(),
                                                    t.getWorldLocation().getX() - me.getWorldLocation().getX()));
        return angle >= 0 ? angle : 360 + angle;
    }

    /**
     * Returns the screen location of a Tile with given 3D x, y and height
     * offset values.
     *
     * @param tile   RSTile for which the screen location should be calculated.
     * @param dX     Distance from bottom left of the tile to bottom right. Ranges
     *               from 0-1;
     * @param dY     Distance from bottom left of the tile to top left. Ranges from
     *               0-1;
     * @param height Height offset (normal to the ground) to return the
     *               <code>Point</code> at.
     * @return <code>Point</code> based on position on the game plane; otherwise
     *         <code>new Point(-1, -1)</code>.
     */
    public Point tileToScreen(final RSTile tile, final double dX, final double dY, final int height) {
        WalkerTile walkerTile = ctx.tiles.createWalkerTile(tile).toLocalTile();
        return Perspective.localToCanvas(ctx.proxy,
                                         new LocalPoint(walkerTile.getX(), walkerTile.getY()),
                                         ctx.proxy.getPlane(),
                                         height);
    }

    /**
     * Returns the screen location of a Tile with a given 3D height offset.
     *
     * @param tile   RSTile for which the screen location should be calculated.
     * @param height Height offset (normal to the ground) to return the
     *               <code>Point</code> at.
     * @return <code>Point</code> based on position on the game plane; if null
     *         <code>new Point(-1, -1)</code>.
     * @see #tileToScreen(RSTile, double, double, int)
     */
    public Point tileToScreen(final RSTile tile, final int height) {
        return tileToScreen(tile, 0.5, 0.5, height);
    }

    /**
     * Returns the screen location of the south-west corner of the given tile.
     *
     * @param tile RSTile for which the screen location should be calculated.
     * @return Center <code>Point</code> of the RSTile at a height of 0; if null
     *         <code>new Point(-1, -1)</code>.
     * @see #tileToScreen(RSTile, int)
     */
    public Point tileToScreen(final RSTile tile) {
        return tileToScreen(tile, 0);
    }

    /**
     * Returns the diagonal distance to a given RSCharacter.
     *
     * @param c The destination character.
     * @return Distance to <code>RSCharacter</code>.
     * @see #distanceTo(RSTile)
     */
    public int distanceTo(RSCharacter c) {
        return c == null ? -1 : distanceTo(c.getLocation());
    }

    /**
     * Returns the diagonal distance to a given RSObject.
     *
     * @param o The destination object.
     * @return Distance to <code>RSObject</code>.
     * @see #distanceTo(RSTile)
     */

    public int distanceTo(RSObject o) {
        return o == null ? -1 : distanceTo(o.getLocation());
    }

    /**
     * Returns the diagonal distance to a given RSTile.
     *
     * @param t The destination tile.
     * @return Distance to <code>RSTile</code>.
     */
    public int distanceTo(RSTile t) {
        return t == null ? -1 : (int) distanceBetween(ctx.players.getMyPlayer().getLocation(), t);
    }

    /**
     * Returns the diagonal distance (hypot) between two RSTiles.
     *
     * @param curr The starting tile.
     * @param dest The destination tile.
     * @return The diagonal distance between the two <code>RSTile</code>s.
     * @see #distanceBetween(Point, Point)
     */
    public double distanceBetween(RSTile curr, RSTile dest) {
        return Math.sqrt((curr.getWorldLocation().getX() - dest.getWorldLocation().getX()) *
                (curr.getWorldLocation().getX() - dest.getWorldLocation().getX()) +
                (curr.getWorldLocation().getY() - dest.getWorldLocation().getY()) *
                (curr.getWorldLocation().getY() - dest.getWorldLocation().getY()));
    }

    /**
     * Returns the screen Point of given absolute x and y values in the game's
     * 3D plane.
     *
     * @param x x value based on the game plane.
     * @param y y value based on the game plane.
     * @return <code>Point</code> within minimap; otherwise
     *         <code>new Point(-1, -1)</code>.
     */
    public Point worldToMinimap(double x, double y) {
        LocalPoint test = LocalPoint.fromWorld(ctx.proxy, (int) x, (int) y);
        if (test != null) {
            return Perspective.localToMinimap(ctx.proxy, test, 2150);
        }

        return null;
    }

    /**
     * Returns the screen location of a given point on the ground. This accounts
     * for the height of the ground at the given location.
     *
     * @param x      x value based on the game plane.
     * @param y      y value based on the game plane.
     * @param height height offset (normal to the ground).
     * @return <code>Point</code> based on screen; otherwise
     *         <code>new Point(-1, -1)</code>.
     */
    public Point groundToScreen(final int x, final int y, final int height) {
        return Perspective.localToCanvas(ctx.proxy, x, y, height);
    }

    /**
     * Returns the height of the ground at the given location in the game world.
     *
     * @param x x value based on the game plane.
     * @param y y value based on the game plane.
     * @return The ground height at the given location; otherwise <code>0</code>
     *         .
     */
    public int tileHeight(final int x, final int y) {
        return Perspective.getTileHeight(ctx.proxy, new LocalPoint(x, y), ctx.proxy.getPlane());

    }

    /**
     * Returns the screen location of a given 3D point in the game world.
     *
     * @param x x value on the game plane.
     * @param y y value on the game plane.
     * @param z z value on the game plane.
     * @return <code>Point</code> based on screen; otherwise
     *         <code>new Point(-1, -1)</code>.
     */
    public Point worldToScreen(int x, int y, int z) {
        LocalPoint local = LocalPoint.fromWorld(ctx.proxy, x, y);
        if (local == null) {
            local = new LocalPoint(x, y);
        }

        return Perspective.localToCanvas(ctx.proxy, local, z);
    }

    /**
     * Returns the screen location of a given 3D point in the game world.
     *
     * @param x         x value on the game plane.
     * @param y         y value on the game plane.
     * @param plane     the game level (plane) value.
     * @param z         z value on the game plane.
     * @return <code>Point</code> based on screen; otherwise
     *         <code>new Point(-1, -1)</code>.
     */
    public Point worldToScreen(int x, int y, int plane, int z) {
        LocalPoint local = LocalPoint.fromWorld(ctx.proxy, x, y);
        if (local == null) {
            local = new LocalPoint(x, y);
        }
        return Perspective.localToCanvas(ctx.proxy, local, plane, z);
    }

    public static java.awt.Point convertRLPointToAWTPoint(Point point) {
        return new java.awt.Point(point.getX(), point.getY());
    }


    public Polygon getTileBoundsPoly(Positionable positionable, int additionalHeight) {
        return Perspective.getCanvasTilePoly(ctx.proxy, positionable.getLocation().getLocalLocation(ctx));
    }

    public Point getRandomPolyPoint(Polygon polygon) {
        return new Point(polygon.xpoints[ctx.random(0, polygon.npoints)],
                         polygon.ypoints[ctx.random(0, polygon.npoints)]);
    }

}
