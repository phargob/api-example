package dax.path;

import dax.Ctx;

import rsb.wrappers.WalkerTile;

import java.util.List;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PathAnalyzer {

    public static DaxTile closestToPlayer = null, furthestReachable = null;

    public static DaxTile closestTileInPathToPlayer(List<WalkerTile> path) {
        DaxTile.generateRealTimeCollision();

        final WalkerTile playerPosition = Ctx.getMyLocation();
        DaxTile t = DaxTile.get(playerPosition.getX(),
                                playerPosition.getY(),
                                playerPosition.getPlane());
        return (DaxTile) BFS.bfsClosestToPath(path, t);
    }


    public static DestinationDetails furthestReachableTile(List<WalkerTile> path) {
        return furthestReachableTile(path, closestTileInPathToPlayer(path));
    }


    public static DestinationDetails furthestReachableTile(List<WalkerTile> path,
                                                           DaxTile currentPosition) {

        if (path == null || currentPosition == null) {
            System.out.println("PathAnalyzer attempt to find closest tile in path: " + currentPosition + " " + path);
            return null;
        }

        final int start = path.indexOf(currentPosition.toWalkerTile());
        if (start < 0) {
            return null;
        }

        int i = start;

        while (i < path.size()) {

            WalkerTile currentNode = path.get(i);
            DaxTile current = DaxTile.get(currentNode.getX(), currentNode.getY(), currentNode.getPlane());
            //log.info("current:" + current);

            if (current == null) {
                return null;
            }

            // end of the road
            if (i + 1 >= path.size()) {
                return new DestinationDetails(PathState.END_OF_PATH, current);
            }

            // not in this chunk/region/ whatever? XXX
            WalkerTile nextNode = path.get(i + 1);
            if (!isLoaded(nextNode) && Ctx.ctx.calc.tileOnMap(nextNode.toWorldTile())) {
                return new DestinationDetails(PathState.FURTHEST_CLICKABLE_TILE, current);
            }

            DaxTile next = DaxTile.get(nextNode.getX(), nextNode.getY(), nextNode.getPlane());

            Direction direction = directionTo(current.toWalkerTile(), nextNode);

            if (direction == Direction.UNKNOWN) {
                furthestReachable = current;
                return new DestinationDetails(PathState.DISCONNECTED_PATH,
                                              current,
                                              nextNode.getX(), nextNode.getY(), nextNode.getPlane());
            }

            // something is blocking
            if (!direction.confirmTileMovable(DaxTile.get(current.getX(), current.getY(), current.getZ()))){

                // we can walk around it, so do that
                boolean walkedAround = false;
                for (int j = 2; j < 6 && j + i < path.size(); j++) {
                    WalkerTile nextInPath = path.get(i + j);
                    DaxTile nextInPathCollision = DaxTile.get(nextInPath.getX(), nextInPath.getY(), nextInPath.getPlane());
                    if (nextInPathCollision != null && nextInPathCollision.isWalkable()) {
                        if (BFS.isReachable(current, nextInPathCollision, 360)) {
                            i += j-1;
                            walkedAround = true;
                            break;
                        }
                    }
                }

                if (walkedAround) {
                    continue;
                }

                furthestReachable = current;
                if (next != null) {
                    return new DestinationDetails(PathState.OBJECT_BLOCKING, current, next);
                }

                return new DestinationDetails(PathState.OBJECT_BLOCKING, current, nextNode.getX(), nextNode.getY(), nextNode.getPlane());
            }

            if (!Ctx.ctx.calc.tileOnMap(Ctx.ctx.tiles.createWalkerTile(nextNode.getX(), nextNode.getY(), nextNode.getPlane()))) {
                furthestReachable = current;
                if (next != null) {
                    return new DestinationDetails(PathState.FURTHEST_CLICKABLE_TILE, current, next);
                }

                return new DestinationDetails(PathState.FURTHEST_CLICKABLE_TILE,
                                              current,
                                              nextNode.getX(),
                                              nextNode.getY(),
                                              nextNode.getPlane());
            }

            i++;
        }

        return null;
    }

    public static Direction directionTo(WalkerTile fromNode, WalkerTile toNode) {
        if (fromNode.getPlane() != toNode.getPlane()){
            return Direction.UNKNOWN;
        }

        for (Direction direction : Direction.values()) {
            if (fromNode.getX() + direction.x == toNode.getX() &&
                fromNode.getY() + direction.y == toNode.getY()) {
                return direction;
            }
        }

        return Direction.UNKNOWN;
    }

    public enum PathState {
        FURTHEST_CLICKABLE_TILE,
        DISCONNECTED_PATH,
        OBJECT_BLOCKING,
        END_OF_PATH
    }

    private enum Direction {
        NORTH (0, 1),
        EAST (1, 0),
        SOUTH (0, -1),
        WEST (-1, 0),
        NORTH_EAST (1, 1),
        SOUTH_EAST (1, -1),
        NORTH_WEST (-1, 1),
        SOUTH_WEST (-1, -1),
        SAME_TILE (0, 0),
        UNKNOWN (104, 104);

        int x, y;

        Direction(int x, int y){
            this.x = x;
            this.y = y;
        }

        boolean confirmTileMovable(DaxTile tile) {
            if (this == SAME_TILE) {
                return true;
            }

            DaxTile destination = DaxTile.get(tile.getX() + this.x,
                                              tile.getY() + this.y,
                                              tile.getZ());
            if (destination == null) {
                return false;
            }

            if (tile.getNeighbors().contains(destination)) {
                return true;
            }

            return BFS.isReachable(tile, destination, 225);
        }
    }


    public static class DestinationDetails {
        private PathState state;
        private DaxTile destination, nextTile;
        private int assumedX, assumedY, assumedZ;

        private DestinationDetails(PathState state, DaxTile destination){
            this.state = state;
            this.destination = destination;
            this.assumedX = -1;
            this.assumedY = -1;
            this.assumedZ = -1;
        }
        private DestinationDetails(PathState state, DaxTile destination, DaxTile nextTile){
            this.state = state;
            this.destination = destination;
            this.nextTile = nextTile;
            this.assumedX = nextTile.getX();
            this.assumedY = nextTile.getY();
            this.assumedZ = nextTile.getZ();

        }
        private DestinationDetails(PathState state, DaxTile destination, int x, int y, int z){
            this.state = state;
            this.destination = destination;
            this.assumedX = x;
            this.assumedY = y;
            this.assumedZ = z;
        }

        public PathState getState() {
            return state;
        }

        public WalkerTile getAssumed(){
            return Ctx.ctx.tiles.createWalkerTile(assumedX, assumedY, assumedZ);
        }


        public DaxTile getDestination() {
            return destination;
        }

        public DaxTile getNextTile() {
            return nextTile;
        }

        public int getAssumedX() {
            return assumedX;
        }

        public int getAssumedY() {
            return assumedY;
        }

        public int getAssumedZ() {
            return assumedZ;
        }

        @Override
        public String toString(){
            String debug = "PATH_DEBUG[ ";
            if (state == PathState.END_OF_PATH){
                debug += state;
            } else {
                if (destination != null){
                    debug += (destination.getX() + ", " + destination.getY() + ", " + destination.getZ());
                } else {
                    debug += null;
                }
                debug += ") -> " + state + " -> (";
                if (nextTile != null){
                    debug += (nextTile.getX() + ", " + nextTile.getY() + ", " + nextTile.getZ());
                } else {
                    debug += null + " [" + assumedX + ", " + assumedY + ", " + assumedZ + "] ";
                }
                debug += ")";
            }
            debug += " ]";
            return debug;
        }
    }

    private static boolean isLoaded(WalkerTile tile){
        final WalkerTile local = tile.toSceneTile();
        return local.getX() >= 0 && local.getX() < 104 && local.getY() >= 0 && local.getY() < 104;
    }

}
