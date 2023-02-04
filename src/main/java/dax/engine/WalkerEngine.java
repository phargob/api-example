package dax.engine;

import dax.Ctx;

import dax.utils.WaitFor;
import dax.utils.AccurateMouse;

import dax.path.BFS;
import dax.path.DaxTile;
import dax.path.Reachable;
import dax.path.PathAnalyzer;
import dax.path.DaxPathFinder;

import dax.utils.StdRandom;

import rsb.wrappers.WalkerTile;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalkerEngine {

    // need to be accessible from python
    public final int failThreshold;

    public int attemptsForAction;
    public List<WalkerTile> path;
    public boolean accurateDestination;

    public WalkerEngine(List<WalkerTile> path, boolean accurateDestination) {
        this.attemptsForAction = 0;
        this.failThreshold = 3;
        this.path = path;
        this.accurateDestination = accurateDestination;
    }

    public boolean walkPath() {
        if (path.size() == 0) {
            log.warn("Path is empty");
            return false;
        }

        while (true) {
            switch (poll()) {
                case EXIT_OUT_WALKER_SUCCESS:
                    return true;
                case EXIT_OUT_WALKER_FAIL:
                    return false;
                case CONTINUE_WALKER:
                    ;
            }

            WaitFor.milliseconds(400, 800);
        }
    }

    public State poll() {
        if (!Ctx.ctx.game.isLoggedIn()) {
            return State.EXIT_OUT_WALKER_FAIL;
        }

        if (isFailedOverThreshhold()) {
            log.info("Too many failed attempts");
            return State.EXIT_OUT_WALKER_FAIL;
        }

        // if (ShipUtils.isOnShip()) {
        //     if (!ShipUtils.crossGangplank()) {
        //         log.info("Failed to exit ship via gangplank.");
        //         failedAttempt();
        //     }

        //     return State.CONTINUE_WALKER;
        // }

        var destinationDetails = PathAnalyzer.furthestReachableTile(path);

        // we dont do anything with furthestMinimap other than check it is not null
        WalkerTile furthestMinimap = DaxPathFinder.getFurthestReachableTileInMinimap(path);
        if (furthestMinimap == null || destinationDetails == null) {
            log.info("Could not grab destination details.");
            failedAttempt();
            return State.CONTINUE_WALKER;
        }

        log.info("destinationDetails: {}, furthestMinimap: {}",
                 destinationDetails, furthestMinimap);

        DaxTile currentNode = destinationDetails.getDestination();
        WalkerTile assumedNext = destinationDetails.getAssumed();

        log.info("currentNode: {}, assumedNext: {}", currentNode, assumedNext);

        final DaxTile destination = currentNode;
        if (!Ctx.ctx.calc.tileOnMap(Ctx.ctx.tiles.createWalkerTile(destination.getX(), destination.getY(), destination.getZ()))) {
            log.info("Closest tile in path is not in minimap: " + destination);
            failedAttempt();
            return State.CONTINUE_WALKER;
        }

        switch (destinationDetails.getState()) {

        case DISCONNECTED_PATH:
            // XXX why do this?  just let the object blocking code do it stuff?
            if (currentNode.toWalkerTile().distanceTo(Ctx.getMyLocation()) > 10) {
                clickMinimap(currentNode, true);
                WaitFor.milliseconds(1200, 3400);
            }

            // DO NOT BREAK OUT

        case OBJECT_BLOCKING:
            Reachable reachable = new Reachable();
            WalkerTile walkingTile = reachable.getBestWalkableTile(destination.toWalkerTile());
            if (isDestinationClose(destination) ||
                (walkingTile != null ? clickMinimap(walkingTile, true) : clickMinimap(destination, false))) {

                log.info("Handling Object...");
                if (PathObjectHandler.handle(destinationDetails, path)) {
                    successfulAttempt();

                } else {
                    failedAttempt();
                }
            }

            break;

        case FURTHEST_CLICKABLE_TILE:
            if (clickMinimap(currentNode, true)) {
                long offsetWalkingTimeout = System.currentTimeMillis() + StdRandom.uniform(5000, 10000);

                WaitFor.condition(20000, () -> {
                        PathAnalyzer.DestinationDetails furthestReachable = PathAnalyzer.furthestReachableTile(path);
                        if (furthestReachable == null) {
                            log.warn("Furthest reachable is null");
                            return WaitFor.Return.FAIL;
                        }

                        var collisionTile = DaxTile.get(destination.getX(),
                                                        destination.getY(),
                                                        destination.getZ());
                        DaxTile currentDestination = BFS.bfsClosestToPath(path, collisionTile);
                        if (currentDestination == null) {
                            log.info("Could not walk to closest tile in path.");
                            failedAttempt();
                            return WaitFor.Return.FAIL;
                        }

                        DaxTile closestToPlayer = PathAnalyzer.closestTileInPathToPlayer(path);
                        if (closestToPlayer == null) {
                            log.info("Could not detect closest tile to player in path.");
                            failedAttempt();
                            return WaitFor.Return.FAIL;
                        }

                        int indexCurrentDestination = path.indexOf(currentDestination.toWalkerTile());
                        int indexCurrentPosition = path.indexOf(closestToPlayer.toWalkerTile());
                        int indexNextDestination = path.indexOf(furthestReachable.getDestination().toWalkerTile());
                        if (indexNextDestination - indexCurrentDestination > 4 ||
                            indexCurrentDestination - indexCurrentPosition < 4) {
                            successfulAttempt();
                            return WaitFor.Return.SUCCESS;
                        }

                        if (System.currentTimeMillis() > offsetWalkingTimeout &&
                            !Ctx.ctx.players.getMyPlayer().isLocalPlayerMoving()) {
                            log.info("offsetWalkingTimeout");
                            return WaitFor.Return.FAIL;
                        }

                        return WaitFor.milliseconds(400, 800);
                    });

            }

            break;

        case END_OF_PATH:
            if (accurateDestination) {
                clickMinimap(destinationDetails.getDestination(), false);
            }

            log.info("Reached end of path");
            return State.EXIT_OUT_WALKER_SUCCESS;
        }

        return State.CONTINUE_WALKER;
    }

    private boolean isDestinationClose(DaxTile pathFindingNode) {
        final WalkerTile playerPosition = Ctx.getMyLocation();

        var tile = pathFindingNode.toWalkerTile();
        return (tile.isClickable() &&
                playerPosition.distanceTo(tile) <= 16 &&
                BFS.isReachable(DaxTile.get(playerPosition.getX(),
                                                          playerPosition.getY(),
                                                          playerPosition.getPlane()),
                                DaxTile.get(pathFindingNode.getX(),
                                                          pathFindingNode.getY(),
                                                          pathFindingNode.getZ()), 200));
    }


    public boolean clickMinimap(WalkerTile tile, boolean randomizePoint) {
        var dt = DaxTile.get(tile.getX(), tile.getY(), tile.getPlane());
        return clickMinimap(dt, randomizePoint);
    }

    public boolean clickMinimap(DaxTile pathFindingNode, boolean randomizePoint) {
        final WalkerTile playerPosition = Ctx.getMyLocation();

        if (playerPosition.distanceTo(pathFindingNode.toWalkerTile()) <= 1) {
            return true;
        }

        DaxTile randomNearby = null;
        if (randomizePoint) {
            randomNearby = BFS.getRandomTileNearby(pathFindingNode);
            if (randomNearby == null) {
                log.warn("Unable to generate randomization.");
            }
        }

        boolean result = false;
        if (randomNearby != null) {
            result = AccurateMouse.clickMinimap(randomNearby.toWalkerTile());
        }

        if (!result) {
            result = AccurateMouse.clickMinimap(pathFindingNode.toWalkerTile());
            if (!result) {
                log.info("Accurate click failed");
            }
        }

        return result;
    }

    private boolean successfulAttempt() {
        attemptsForAction = 0;
        return true;
    }

    private void failedAttempt() {
        log.info("Failed attempt on action.");
        attemptsForAction++;

        if (Ctx.ctx.camera.getPitch() < 90) {
            Ctx.ctx.camera.setPitch(StdRandom.uniform(90, 100));
        }

        if (attemptsForAction > 1) {
            Ctx.ctx.camera.setAngle(StdRandom.uniform(0, 360));
        }

        WaitFor.milliseconds(450 * (attemptsForAction + 1), 850 * (attemptsForAction + 1));

        DaxTile.generateRealTimeCollision();
    }

    private boolean isFailedOverThreshhold() {
        return attemptsForAction >= failThreshold;
    }
}
