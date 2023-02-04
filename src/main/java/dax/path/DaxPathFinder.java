package dax.path;

import dax.Ctx;

import rsb.methods.Calculations;
import rsb.wrappers.RSCharacter;
import rsb.wrappers.common.Positionable;
import rsb.wrappers.WalkerTile;

import java.util.List;
import java.util.Queue;
import java.util.*;

/**
 * For local pathing ONLY. Anything outside your region will return unexpected results.
 * Exactly same code as Reachable... just a little different
 */

public class DaxPathFinder {

    public static WalkerTile getFurthestReachableTileInMinimap(List<WalkerTile> path) {
        List<WalkerTile> reversed = new ArrayList<>(path);
        Collections.reverse(reversed);

        Destination[][] map = DaxPathFinder.getMap();
        for (WalkerTile tile : reversed) {
            // XXX this is currently redundant, as calc.tileOnMap() does this
            net.runelite.api.Point point = Ctx.ctx.calc.tileToMinimap(tile);
            if (point == null) {
                continue;
            }

            if (DaxPathFinder.canReach(map, tile) && Ctx.ctx.calc.tileOnMap(tile)) {
                return tile;
            }
        }

        return null;
    }

    public static WalkerTile getFurthestReachableTileOnScreen(List<WalkerTile> path) {
        List<WalkerTile> reversed = new ArrayList<>(path);
        Collections.reverse(reversed);

        Destination[][] map = DaxPathFinder.getMap();
        // XXX use calc.tileOnScreen() ?
        for (WalkerTile tile : reversed) {
            if (DaxPathFinder.canReach(map, tile) && tile.isOnScreen() && tile.isClickable()) {
                return tile;
            }
        }

        return null;
    }


    private static class Destination {
        private WalkerTile tile;
        private Destination parent;
        private int distance;

        public Destination(WalkerTile tile, Destination parent, int distance) {
            this.tile = tile;
            this.parent = parent;
            this.distance = distance;
        }

        public WalkerTile getLocalTile() {
            return tile;
        }

        public WalkerTile getWorldTile() {
            return tile.toWorldTile();
        }

        public Destination getParent() {
            return parent;
        }

        public int getDistance() {
            return distance;
        }
    }

    /**
     *
     * @param tile
     * @return Distance to a tile accounting for collision. Integer.MAX_VALUE if not reachable.
     */
    private static int distance(Positionable tile) {
        return distance(getMap(), tile.getLocation());
    }

    private static int distance(Destination[][] map, Positionable tile) {
        WalkerTile worldTile = tile.getLocation().toSceneTile();
        int x = worldTile.getX(), y = worldTile.getY();

        if (!validLocalBounds(tile)) {
            return Integer.MAX_VALUE;
        }

        Destination destination = map[x][y];
        return destination == null ? Integer.MAX_VALUE : destination.distance;
    }

    private static boolean canReach(WalkerTile tile) {
        return canReach(getMap(), tile);
    }

    public static boolean canReach(Destination[][] map, WalkerTile tile) {
        int myPlane = Ctx.getMyLocation().getPlane();
        if (tile.getPlane() != myPlane) {
            return false;
        }

        WalkerTile worldTile = tile.getType() != WalkerTile.TYPES.SCENE ? tile.toSceneTile() : tile;
        int x = worldTile.getX();
        int y = worldTile.getY();

        if (!validLocalBounds(tile) || x > map.length || y > map[x].length) {
            return false;
        }

        Destination destination = map[x][y];
        return destination != null;
    }

    private static List<WalkerTile> getPath(WalkerTile tile) {
        return getPath(getMap(), tile);
    }

    private static List<WalkerTile> getPath(Destination destination) {
        Stack<WalkerTile> WalkerTiles = new Stack<>();
        Destination parent = destination;
        while (parent != null) {
            WalkerTiles.add(parent.getWorldTile());
            parent = parent.parent;
        }
        return new ArrayList<>(WalkerTiles);
    }

    private static List<WalkerTile> getPath(Destination[][] map, WalkerTile tile) {
        WalkerTile worldTile = tile.getType() != WalkerTile.TYPES.SCENE ? tile.toSceneTile() : tile;
        int x = worldTile.getX(), y = worldTile.getY();

        Destination destination = map[x][y];

        if (destination == null) {
            return null;
        }

        return getPath(destination);
    }

    public static Destination[][] getMap() {

        final WalkerTile home = Ctx.getMyLocation().toSceneTile();

        Destination[][] map = new Destination[104][104];
        int[][] collisionData = Ctx.ctx.walking.getCollisionData();

        if (collisionData == null ||
            collisionData.length < home.getX() ||
            collisionData[home.getX()].length < home.getY()){
            return map;
        }

        Queue<Destination> queue = new LinkedList<>();
        queue.add(new Destination(home, null, 0));
        map[home.getX()][home.getY()] = queue.peek();

        while (!queue.isEmpty()) {
            Destination currentLocal = queue.poll();

            int x = currentLocal.getLocalTile().getX();
            int y = currentLocal.getLocalTile().getY();
            Destination destination = map[x][y];

            for (Reachable.Direction direction : Reachable.Direction.values()) {
                // Cannot traverse to tile from current.
                if (!direction.isValidDirection(x, y, collisionData)) {
                    continue;
                }

                WalkerTile neighbor = direction.getPointingTile(currentLocal.getLocalTile());
                int destinationX = neighbor.getX(), destinationY = neighbor.getY();

                if (!CollisionFlags.isWalkable(collisionData[destinationX][destinationY])) {
                    continue;
                }

                if (map[destinationX][destinationY] != null) {
                    continue; //Traversed already
                }

                map[destinationX][destinationY] = new Destination(neighbor, currentLocal, (destination != null) ? destination.getDistance() + 1 : 0);
                queue.add(map[destinationX][destinationY]);
            }
        }

        return map;
    }

    private static boolean validLocalBounds(Positionable positionable) {
        var l = positionable.getLocation();
        WalkerTile tile = l.getType() == WalkerTile.TYPES.SCENE ? l : l.toSceneTile();
        return tile.getX() >= 0 && tile.getX() < 104 && tile.getY() >= 0 && tile.getY() < 104;
    }

}
