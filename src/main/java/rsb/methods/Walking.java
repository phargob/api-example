package rsb.methods;

import net.runelite.api.CollisionData;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import rsb.globval.WidgetIndices;
import rsb.globval.GlobalWidgetInfo;
import rsb.globval.VarpIndices;
import rsb.globval.VarpValues;
import rsb.wrappers.RSLocalPath;
import rsb.wrappers.RSPath;
import rsb.wrappers.RSTile;
import rsb.wrappers.RSTilePath;


/**
 * Walking related operations.
 */
public class Walking {
    private MethodContext ctx;

    Walking(final MethodContext ctx) {
        this.ctx = ctx;
    }

    private RSPath lastPath;
    private RSTile lastDestination;
    private RSTile lastStep;

    /**
     * Creates a new path based on a provided array of tile waypoints.
     *
     * @param tiles The waypoint tiles.
     * @return An RSTilePath.
     */
    public RSTilePath newTilePath(final RSTile[] tiles) {
        if (tiles == null) {
            throw new IllegalArgumentException("null waypoint list");
        }
        return new RSTilePath(ctx, tiles);
    }

    /**
     * Generates a path from the player's current location to a destination
     * tile.
     *
     * @param destination The destination tile.
     * @return The path as an RSPath.
     */
    public RSPath getPath(final RSTile destination) {
        return new RSLocalPath(ctx, destination);
    }

    /**
     * Determines whether or not a given tile is in the loaded map area.
     *
     * @param tile The tile to check.
     * @return <code>true</code> if local; otherwise <code>false</code>.
     */
    public boolean isLocal(final RSTile tile) {
        int[][] flags = getCollisionData();
        int x = tile.getWorldLocation().getX() - ctx.game.getBaseX();
        int y = tile.getWorldLocation().getY() - ctx.game.getBaseY();
        return (flags != null && x >= 0 && y >= 0 && x < flags.length && y < flags.length);
    }

    /**
     * Walks one tile towards the given destination using a generated path.
     *
     * @param destination The destination tile.
     * @return <code>true</code> if the next tile was walked to; otherwise
     *         <code>false</code>.
     */
    public boolean walkTo(final RSTile destination) {
        if (destination.equals(lastDestination)
                && ctx.calc.distanceTo(lastStep) < 10) {
            return lastPath.traverse();
        }
        lastDestination = destination;
        lastPath = getPath(destination);
        if (!lastPath.isValid()) {
            return false;
        }
        lastStep = lastPath.getNext();
        return lastPath.traverse();
    }

    /**
     * Walks to the given tile using the minimap with 1 tile randomness.
     *
     * @param t The tile to walk to.
     * @return <code>true</code> if the tile was clicked; otherwise <code>false</code>.
     * @see #walkTileMM(RSTile, int, int)
     */
    public boolean walkTileMM(final RSTile t) {
        return walkTileMM(t, 0, 0);
    }

    private int addRandom(int r) {
        if (r < 1) {
            return 0;
        }

        if (ctx.random(0, 2) == 1) {
            return ctx.random(0, r + 1);

        } else {
            return ctx.random(1, r + 1) * -1;
        }
    }

    /**
     * Walks to the given tile using the minimap with given randomness.
     *
     * @param t The tile to walk to.
     * @param x The x randomness (between 0 and x-1).
     * @param y The y randomness (between 0 and y-1).
     * @return <code>true</code> if the tile was clicked; otherwise <code>false</code>.
     */
    public boolean walkTileMM(final RSTile t, final int x, final int y) {
        int xx = t.getWorldLocation().getX() + addRandom(x);
        int yy = t.getWorldLocation().getY() + addRandom(y);

        RSTile dest = new RSTile(xx, yy, t.getWorldLocation().getPlane());
        if (!ctx.calc.tileOnMap(dest)) {
            dest = getClosestTileOnMap(dest);
        }

        Point p = ctx.calc.tileToMinimap(dest);
        if (p.getX() != -1 && p.getY() != -1) {
            ctx.mouse.move(p);

            Point p2 = ctx.calc.tileToMinimap(dest);
            // ctx.mouse takes time, if character got far enough (i.e. died) p2 will be null
            if (p2 == null) {
                return false;
            }

            if (p2.getX() != -1 && p2.getY() != -1) {
                //Get exact since we're moving... should be removed?
                if (!ctx.mouse.getLocation().equals(p2)) {
                    ctx.mouse.hop(p2);
                }

                ctx.sleepRandom(50, 100);
                ctx.mouse.click(true);
                return true;
            }
        }

        return false;
    }

    /**
     * Walks to the given tile using the minimap with given randomness.
     *
     * @param t The tile to walk to.
     * @param r The maximum deviation from the tile to allow.
     * @return <code>true</code> if the tile was clicked; otherwise <code>false</code>.
     */
    public boolean walkTileMM(final RSTile t, final int r) {
        int x = t.getWorldLocation().getX() + addRandom(r);
        int y = t.getWorldLocation().getY() + addRandom(r);

        RSTile dest = new RSTile(x, y, t.getWorldLocation().getPlane());
        return !ctx.players.getMyPlayer().getLocation().equals(dest) && walkTileMM(dest, 0, 0);
    }

    /**
     * Walks to a tile using onScreen clicks and not the MiniMap. If the tile is
     * not on the screen, it will find the closest tile that is on screen and it
     * will walk there instead.
     *
     * @param tileToWalk Tile to walk.
     * @return True if successful.
     */
    public boolean walkTileOnScreen(final RSTile tileToWalk) {
        return ctx.tiles.doAction(ctx.calc.getTileOnScreen(tileToWalk), "Walk here");
    }

    /**
     * Turns run on or off using the game GUI controls.
     *
     * @param enable <code>true</code> to enable run, <code>false</code> to disable it.
     * @return  if run was attempted to be enabled <code>true</code>; otherwise <code>false</code>
     */
    public boolean setRun(final boolean enable) {
        if (isRunEnabled() != enable) {
            // hack...
            return ctx.interfaces.getComponent(WidgetIndices.Minimap.GROUP_INDEX, 27).doClick();
            //return ctx.interfaces.getComponent(GlobalWidgetInfo.MINIMAP_RUN_ORB).doClick();
        }
        return false;
    }

    /**
     * Generates a path from the player's current location to a destination
     * tile.
     *
     * @param destination The destination tile.
     * @return The path as an RSTile array.
     */
    @Deprecated
    public RSTile[] findPath(RSTile destination) {
        RSLocalPath path = new RSLocalPath(ctx, destination);
        if (path.isValid()) {
            RSTilePath tp = path.getCurrentTilePath();
            if (tp != null) {
                return tp.toArray();
            }
        }
        return new RSTile[0];
    }

    /**
     * Randomizes a single tile.
     *
     * @param tile          The RSTile to randomize.
     * @param maxXDeviation Max X distance from tile.getX().
     * @param maxYDeviation Max Y distance from tile.getY().
     * @return The randomized tile.
     */
    @Deprecated
    public RSTile randomize(RSTile tile, int maxXDeviation, int maxYDeviation) {
        return tile.randomize(maxXDeviation, maxYDeviation);
    }

    /**
     * Returns the closest tile on the minimap to a given tile.
     *
     * @param tile The destination tile.
     * @return Returns the closest tile to the destination on the minimap.
     */
    public RSTile getClosestTileOnMap(final RSTile tile) {
        if (!ctx.calc.tileOnMap(tile) && ctx.game.isLoggedIn()) {
            RSTile loc = ctx.players.getMyPlayer().getLocation();

            int xx = (loc.getWorldLocation().getX() + tile.getWorldLocation().getX()) / 2;
            int yy = (loc.getWorldLocation().getY() + tile.getWorldLocation().getY()) / 2;
            xx += addRandom(1);
            yy += addRandom(1);

            RSTile walk = new RSTile(xx, yy, tile.getWorldLocation().getPlane());
            return ctx.calc.tileOnMap(walk) ? walk : getClosestTileOnMap(walk);
        }

        return tile;
    }

    /**
     * Returns whether or not run is enabled.
     *
     * @return <code>true</code> if run mode is enabled; otherwise <code>false</code>.
     */
    public boolean isRunEnabled() {
        return ctx.clientLocalStorage.getVarpValueAt(VarpIndices.TOGGLE_RUN)
                == VarpValues.RUN_ENABLED.getValue();
    }

    /**
     * Returns the player's current run energy.
     *
     * @return The player's current run energy.
     */
    public int getEnergy() {
        return ctx.proxy.getEnergy() / 100;
    }

    /**
     * Gets the destination tile (where the flag is on the minimap). If there is
     * no destination currently, null will be returned.
     *
     * @return The current destination tile, or null.
     */
    public RSTile getDestination() {
        var local = ctx.proxy.getLocalDestinationLocation();
        if (local == null) {
            return null;
        }

        WorldPoint wp = WorldPoint.fromLocal(ctx.proxy,
                                             local.getX(), local.getY(), ctx.game.getPlane());
        return new RSTile(wp.getX(), wp.getY(), wp.getPlane());
    }

    /**
     * Randomizes a single tile.
     *
     * @param tile          The RSTile to randomize.
     * @param maxXDeviation Max X distance from tile.getX().
     * @param maxYDeviation Max Y distance from tile.getY().
     * @return The randomized tile.
     *             .
     */
    @Deprecated
    public RSTile randomizeTile(RSTile tile,
                                int maxXDeviation,
                                int maxYDeviation) {
        return randomize(tile, maxXDeviation, maxYDeviation);
    }

    /**
     * Walks towards the end of a path. This method should be looped.
     *
     * @param path The path to walk along.
     * @return <code>true</code> if the next tile was reached; otherwise
     *         <code>false</code>.
     * @see #walkPathMM(RSTile[], int)
     */
    @Deprecated
    public boolean walkPathMM(RSTile[] path) {
        return walkPathMM(path, 16);
    }

    /**
     * Walks towards the end of a path. This method should be looped.
     *
     * @param path    The path to walk along.
     * @param maxDist See {@link #nextTile(RSTile[], int)}.
     * @return <code>true</code> if the next tile was reached; otherwise
     *         <code>false</code>.
     * @see #walkPathMM(RSTile[], int, int)
     */
    @Deprecated
    public boolean walkPathMM(RSTile[] path, int maxDist) {
        return walkPathMM(path, maxDist, 1, 1);
    }

    /**
     * Walks towards the end of a path. This method should be looped.
     *
     * @param path  The path to walk along.
     * @param randX The X value to randomize each tile in the path by.
     * @param randY The Y value to randomize each tile in the path by.
     * @return <code>true</code> if the next tile was reached; otherwise
     *         <code>false</code>.
     * @see #walkPathMM(RSTile[], int, int, int)
     */
    @Deprecated
    public boolean walkPathMM(RSTile[] path, int randX, int randY) {
        return walkPathMM(path, 16, randX, randY);
    }

    /**
     * Walks towards the end of a path. This method should be looped.
     *
     * @param path    The path to walk along.
     * @param maxDist See {@link #nextTile(RSTile[], int)}.
     * @param randX   The X value to randomize each tile in the path by.
     * @param randY   The Y value to randomize each tile in the path by.
     * @return <code>true</code> if the next tile was reached; otherwise
     *         <code>false</code>.
     */
    @Deprecated
    public boolean walkPathMM(RSTile[] path, int maxDist, int randX, int randY) {
        try {
            RSTile next = nextTile(path, maxDist);
            return next != null && walkTileMM(next, randX, randY);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Walks to the end of a path via the screen. This method should be looped.
     *
     * @param path The path to walk along.
     * @return <code>true</code> if the next tile was reached; otherwise
     *         <code>false</code>.
     * @see #walkPathOnScreen(RSTile[], int)
     */
    @Deprecated
    public boolean walkPathOnScreen(RSTile[] path) {
        return walkPathOnScreen(path, 16);
    }

    /**
     * Walks a path using onScreen clicks and not the MiniMap. If the next tile
     * is not on the screen, it will find the closest tile that is on screen and
     * it will walk there instead.
     *
     * @param path    Path to walk.
     * @param maxDist Max distance between tiles in the path.
     * @return True if successful.
     */
    @Deprecated
    public boolean walkPathOnScreen(RSTile[] path, int maxDist) {
        RSTile next = nextTile(path, maxDist);
        if (next != null) {
            RSTile os = ctx.calc.getTileOnScreen(next);
            return os != null && ctx.tiles.doAction(os, "Walk");
        }
        return false;
    }

    /**
     * Reverses an array of tiles.
     *
     * @param other The <code>RSTile</code> path array to reverse.
     * @return The reverse <code>RSTile</code> path for the given <code>RSTile</code>
     *         path.
     */
    @Deprecated
    public RSTile[] reversePath(RSTile[] other) {
        RSTile[] t = new RSTile[other.length];
        for (int i = 0; i < t.length; i++) {
            t[i] = other[other.length - i - 1];
        }
        return t;
    }

    /**
     * Returns the next tile to walk to on a path.
     *
     * @param path The path.
     * @return The next <code>RSTile</code> to walk to on the provided path; or
     *         <code>null</code> if far from path or at destination.
     * @see #nextTile(RSTile[], int)
     */
    @Deprecated
    public RSTile nextTile(RSTile path[]) {
        return nextTile(path, 17);
    }

    /**
     * Returns the next tile to walk to in a path.
     *
     * @param path     The path.
     * @param skipDist If the distance to the tile after the next in the path is less
     *                 than or equal to this distance, the tile after next will be
     *                 returned rather than the next tile, skipping one. This
     *                 interlacing aids continuous walking.
     * @return The next <code>RSTile</code> to walk to on the provided path; or
     *         <code>null</code> if far from path or at destination.
     */
    @Deprecated
    public RSTile nextTile(RSTile path[], int skipDist) {
        int dist = 99;
        int closest = -1;
        for (int i = path.length - 1; i >= 0; i--) {
            RSTile tile = path[i];
            int d = ctx.calc.distanceTo(tile);
            if (d < dist) {
                dist = d;
                closest = i;
            }
        }

        int feasibleTileIndex = -1;

        for (int i = closest; i < path.length; i++) {

            if (ctx.calc.distanceTo(path[i]) <= skipDist) {
                feasibleTileIndex = i;
            } else {
                break;
            }
        }

        if (feasibleTileIndex == -1) {
            return null;
        } else {
            return path[feasibleTileIndex];
        }
    }

    /**
     * Randomizes a path of tiles.
     *
     * @param path          The RSTiles to randomize.
     * @param maxXDeviation Max X distance from tile.getX().
     * @param maxYDeviation Max Y distance from tile.getY().
     * @return The new, randomized path.
     */
    @Deprecated
    public RSTile[] randomizePath(RSTile[] path, int maxXDeviation,
                                  int maxYDeviation) {
        RSTile[] rez = new RSTile[path.length];
        for (int i = 0; i < path.length; i++) {
            rez[i] = randomize(path[i], maxXDeviation, maxYDeviation);
        }
        return rez;
    }


    public int[][] getCollisionData() {
        return ctx.proxy.getCollisionMaps()[ctx.game.getPlane()].getFlags();
    }
}
