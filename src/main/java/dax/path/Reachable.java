package dax.path;


import dax.Ctx;

import rsb.wrappers.common.Positionable;
import rsb.wrappers.WalkerTile;

import java.util.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Reachable {

    private WalkerTile[][] map;

    /**
     * Generates reachable map from player position
     */
    public Reachable() {
        this(null);
    }

    private Reachable(WalkerTile homeTile) {
        map = generateMap(homeTile != null ? homeTile : Ctx.getMyLocation());
    }

    public boolean canReach(WalkerTile position) {
        position = position.toWorldTile();
        WalkerTile playerPosition = Ctx.getMyLocation();
        if (playerPosition.getX() == position.getX() && playerPosition.getY() == position.getY()) {
            return true;
        }

        return getParent(position.toSceneTile()) != null;
    }

    public boolean canReach(int x, int y) {
        WalkerTile playerPosition = Ctx.getMyLocation();
        if (playerPosition.getX() == x && playerPosition.getY() == y) {
            return true;
        }
        WalkerTile position = convertToLocal(x, y);
        return getParent(position) != null;
    }

    public WalkerTile closestTile(Collection<WalkerTile> tiles) {
        WalkerTile closest = null;
        double closestDistance = Integer.MAX_VALUE;
        WalkerTile playerPosition = Ctx.getMyLocation();
        for (WalkerTile positionable : tiles) {
            double distance = playerPosition.distanceToDouble(positionable);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = positionable;
            }
        }
        return closest;
    }

    /**
     * @param x
     * @param y
     * @return parent tile of x and y through BFS.
     */
    public WalkerTile getParent(int x, int y) {
        WalkerTile position = convertToLocal(x, y);
        return getParent(position);
    }

    public WalkerTile getParent(Positionable positionable) {
        WalkerTile tile = positionable.getLocation();
        if (tile.getType() != WalkerTile.TYPES.SCENE) {
            tile = tile.toSceneTile();
        }

        int x = tile.getX(), y = tile.getY();
        if (x < 0 || y < 0) {
            return null;
        }

        if (x >= 104 || y >= 104 || x >= map.length || y >= map[x].length){
            return null;
        }

        return map[x][y];
    }

    /**
     * @param x
     * @param y
     * @return Distance to tile. Max integer value if unreachable. Does not account for positionable behind doors
     */
    public int getDistance(int x, int y) {
        WalkerTile position = convertToLocal(x, y);
        return getDistance(position);
    }

    /**
     * @param positionable
     * @return path to tile. Does not account for positionable behind doors
     */
    public ArrayList<WalkerTile> getPath(Positionable positionable) {
        WalkerTile position = convertToLocal(positionable.getLocation().getX(),
                                             positionable.getLocation().getY());
        int x = position.getX(), y = position.getY();
        return getPath(x, y);
    }

    /**
     * @param x
     * @param y
     * @return null if no path.
     */
    public ArrayList<WalkerTile> getPath(int x, int y) {
        ArrayList<WalkerTile> path = new ArrayList<>();
        WalkerTile playerPos = new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation()).toSceneTile();
        if (x == playerPos.getX() && y == playerPos.getY()) {
            return path;
        }
        if (x < 0 || y < 0) {
            return null;
        }
        if (x >= 104 || y >= 104) {
            return null;
        }
        if (map[x][y] == null) {
            return null;
        }
        WalkerTile tile = new WalkerTile(Ctx.ctx, x, y, new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation()).getWorldLocation().getPlane(), WalkerTile.TYPES.SCENE);
        while ((tile = map[tile.getX()][tile.getY()]) != null) {
            path.add(tile.toWorldTile());
        }
        Collections.reverse(path);
        return path;
    }

    public int getDistance(Positionable positionable) {
        WalkerTile position = convertToLocal(positionable.getLocation().getX(), positionable.getLocation().getY());
        int x = position.getX(), y = position.getY();
        WalkerTile playerPos = new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation()).toSceneTile();
        if (x == playerPos.getX() && y == playerPos.getY()) {
            return 0;
        }
        if (x < 0 || y < 0) {
            return Integer.MAX_VALUE;
        }
        if (x >= 104 || y >= 104) {
            return Integer.MAX_VALUE;
        }
        if (map[x][y] == null) {
            return Integer.MAX_VALUE;
        }
        int length = 0;
        WalkerTile tile = position;
        while ((tile = map[tile.getX()][tile.getY()]) != null) {
            length++;
        }
        return length;
    }

    // XXX worst named function ever...
    private static WalkerTile convertToLocal(int x, int y) {
        int plane = Ctx.getMyLocation().getPlane();
        WalkerTile position = new WalkerTile(Ctx.ctx, x, y, plane,
                                             (x >= 104 || y >= 104) ? WalkerTile.TYPES.WORLD : WalkerTile.TYPES.SCENE);

        if (position.getType() != WalkerTile.TYPES.SCENE) {
            position = position.toSceneTile();
        }

        return position;
    }

    public WalkerTile getBestWalkableTile(Positionable positionable) {
        boolean[][] traversed = new boolean[104][104];
        Queue<WalkerTile> queue = new LinkedList<>();

        WalkerTile startPosition = positionable.getLocation().toSceneTile();
        queue.add(startPosition);

        try {
            traversed[startPosition.getX()][startPosition.getY()] = true;

        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }

        int[][] collisionData = Ctx.ctx.walking.getCollisionData();

        if (collisionData == null) {
            //XXX warn?
            return null;
        }


        while (!queue.isEmpty()) {
            // pop front of queue
            WalkerTile currentLocal = queue.poll();
            int x = currentLocal.getX(), y = currentLocal.getY();

            int currentCollisionFlags = collisionData[x][y];

            // goal is to find first reachable / walkable tile that is reachable
            if (CollisionFlags.isWalkable(currentCollisionFlags) &&
                this.canReach(currentLocal)) {
                return currentLocal.toWorldTile();
            }

            if (CollisionFlags.isWalkable(currentCollisionFlags)) {
                if (!this.canReach(currentLocal.toWorldTile().getX(),
                                   currentLocal.toWorldTile().getY())) {
                    continue;
                }

                return currentLocal.toWorldTile();
            }


            // Go through all directions (bfs), traverse
            log.info("here {}", currentLocal.toWorldTile());
            for (Direction direction : Direction.values()) {
                // Cannot traverse to tile from current
                if (!direction.isValidDirection(x, y, collisionData)) {
                    continue;
                }

                log.info("go {}", direction);
                WalkerTile neighbor = direction.getPointingTile(currentLocal);
                int destinationX = neighbor.getX(), destinationY = neighbor.getY();

                // Traversed already
                if (traversed[destinationX][destinationY]) {
                    continue;
                }

                traversed[destinationX][destinationY] = true;
                queue.add(neighbor);
            }

        }
        return null;
    }

    /**
     * @return local reachable tiles - as represented by parentMap
     */
    private static WalkerTile[][] generateMap(WalkerTile start) {
        boolean[][] traversed = new boolean[104][104];
        WalkerTile[][] parentMap = new WalkerTile[104][104];
        Queue<WalkerTile> queue = new LinkedList<>();

        WalkerTile startPosition = start.toSceneTile();
        queue.add(startPosition);

        try {
            traversed[startPosition.getX()][startPosition.getY()] = true;
            parentMap[startPosition.getX()][startPosition.getY()] = null;

        } catch (ArrayIndexOutOfBoundsException e) {
            return parentMap;
        }

        int[][] collisionData = Ctx.ctx.walking.getCollisionData();
        if (collisionData == null) {
            // XXX warn?
            return new WalkerTile[][]{};
        }

        // build up reachable tiles
        while (!queue.isEmpty()) {
            // pop front of queue
            WalkerTile currentLocal = queue.poll();
            int x = currentLocal.getX(), y = currentLocal.getY();

            int currentCollisionFlags = collisionData[x][y];

            // cant walk here, ignore
            if (!CollisionFlags.isWalkable(currentCollisionFlags)) {
                continue;
            }

            // go through all directions (bfs), traverse building up shortest path via parent
            for (Direction direction : Direction.values()) {
                // Cannot traverse to tile from current
                if (!direction.isValidDirection(x, y, collisionData)) {
                    continue;
                }

                WalkerTile neighbor = direction.getPointingTile(currentLocal);
                int destinationX = neighbor.getX(), destinationY = neighbor.getY();

                // Traversed already
                if (traversed[destinationX][destinationY]) {
                    continue;
                }

                traversed[destinationX][destinationY] = true;
                parentMap[destinationX][destinationY] = currentLocal;

                queue.add(neighbor);
            }
        }

        return parentMap;
    }

    public enum Direction {
        EAST(1, 0),
        NORTH(0, 1),
        WEST(-1, 0),
        SOUTH(0, -1),
        NORTH_EAST(1, 1),
        NORTH_WEST(-1, 1),
        SOUTH_EAST(1, -1),
        SOUTH_WEST(-1, -1),
        ;

        int x, y;

        Direction(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public WalkerTile getPointingTile(WalkerTile tile) {
            return tile.translateTo(x, y);
        }

        public boolean isValidDirection(int x, int y, int[][] collisionData) {
            try {
                switch (this) {
                    case NORTH:
                        return !CollisionFlags.blockedNorth(collisionData[x][y]);
                    case EAST:
                        return !CollisionFlags.blockedEast(collisionData[x][y]);
                    case SOUTH:
                        return !CollisionFlags.blockedSouth(collisionData[x][y]);
                    case WEST:
                        return !CollisionFlags.blockedWest(collisionData[x][y]);
                    case NORTH_EAST:
                        if (CollisionFlags.blockedNorth(collisionData[x][y]) ||
                            CollisionFlags.blockedEast(collisionData[x][y])) {
                            return false;
                        }
                        if (!CollisionFlags.isWalkable(collisionData[x + 1][y])) {
                            return false;
                        }
                        if (!CollisionFlags.isWalkable(collisionData[x][y + 1])) {
                            return false;
                        }
                        if (CollisionFlags.blockedNorth(collisionData[x + 1][y])) {
                            return false;
                        }
                        if (CollisionFlags.blockedEast(collisionData[x][y + 1])) {
                            return false;
                        }
                        return true;
                    case NORTH_WEST:
                        if (CollisionFlags.blockedNorth(collisionData[x][y]) ||
                            CollisionFlags.blockedWest(collisionData[x][y])) {
                            return false;
                        }
                        if (!CollisionFlags.isWalkable(collisionData[x - 1][y])) {
                            return false;
                        }
                        if (!CollisionFlags.isWalkable(collisionData[x][y + 1])) {
                            return false;
                        }
                        if (CollisionFlags.blockedNorth(collisionData[x - 1][y])) {
                            return false;
                        }
                        if (CollisionFlags.blockedWest(collisionData[x][y + 1])) {
                            return false;
                        }
                        return true;
                    case SOUTH_EAST:
                        if (CollisionFlags.blockedSouth(collisionData[x][y]) ||
                            CollisionFlags.blockedEast(collisionData[x][y])) {
                            return false;
                        }
                        if (!CollisionFlags.isWalkable(collisionData[x + 1][y])) {
                            return false;
                        }
                        if (!CollisionFlags.isWalkable(collisionData[x][y - 1])) {
                            return false;
                        }
                        if (CollisionFlags.blockedSouth(collisionData[x + 1][y])) {
                            return false;
                        }
                        if (CollisionFlags.blockedEast(collisionData[x][y - 1])) {
                            return false;
                        }
                        return true;
                    case SOUTH_WEST:
                        if (CollisionFlags.blockedSouth(collisionData[x][y]) ||
                            CollisionFlags.blockedWest(collisionData[x][y])) {
                            return false;
                        }
                        if (!CollisionFlags.isWalkable(collisionData[x - 1][y])) {
                            return false;
                        }
                        if (!CollisionFlags.isWalkable(collisionData[x][y - 1])) {
                            return false;
                        }
                        if (CollisionFlags.blockedSouth(collisionData[x - 1][y])) {
                            return false;
                        }
                        if (CollisionFlags.blockedWest(collisionData[x][y - 1])) {
                            return false;
                        }
                        return true;
                    default:
                        return false;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return false;
            }
        }
    }

}
