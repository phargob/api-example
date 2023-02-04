package dax.path;

import dax.utils.WaitFor;
import rsb.wrappers.WalkerTile;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BFS {

    public static DaxTile bfsClosestToPath(List<WalkerTile> path, DaxTile start) {
        return bfsClosestToPath(path, start, -1);
    }

    public static DaxTile bfsClosestToPath(List<WalkerTile> path, DaxTile start, int limit) {
        if (path == null || start == null){
            return null;
        }

        if (path.contains(start.toWalkerTile())) {
            return start;
        }

        NodeInfo.clearMemory();

        int iteration = 0;
        Queue<DaxTile> queue = new LinkedList<>();
        queue.add(start);
        NodeInfo.create(queue.peek()).traversed = true;

        while (!queue.isEmpty()){
            if (iteration != -1 && iteration++ == limit){
                break;
            }
            DaxTile current = queue.remove();
            for (DaxTile neighbor : current.getNeighbors()){
                NodeInfo.Details nodeInfo = NodeInfo.create(neighbor);
                if (nodeInfo.traversed){
                    continue;
                }
                nodeInfo.traversed = true;
                if (path.contains(neighbor.toWalkerTile())){
                    return neighbor;
                }
                queue.add(neighbor);
            }
        }
        return null;
    }

    public static boolean isReachable(DaxTile start, DaxTile end, int limit) {
        if (start == null || end == null) {
            return false;
        }

        if (start.equals(end)) {
            return true;
        }

        NodeInfo.clearMemory();

        int iteration = 0;
        Queue<DaxTile> queue = new LinkedList<>();
        queue.add(start);
        NodeInfo.create(queue.peek()).traversed = true;

        while (!queue.isEmpty()){
            if (iteration != -1 && iteration++ == limit){
                return false;
            }
            DaxTile current = queue.remove();
            for (DaxTile neighbor : current.getNeighbors()){
                NodeInfo.Details nodeInfo = NodeInfo.create(neighbor);
                if (nodeInfo.traversed){
                    continue;
                }

                nodeInfo.traversed = true;

                if (neighbor.equals(end)){
                    return true;
                }


                queue.add(neighbor);
            }
        }
        return false;
    }

    public static boolean isReachable(DaxTile start, DaxTile end){
        return isReachable(start, end, -1);
    }


    public static int OFFSET_SEARCH = 12;
    public static DaxTile getRandomTileNearby(DaxTile start) {
        int limit = WaitFor.random(1, OFFSET_SEARCH);
        return getRandomTileNearby(start, limit);
    }

    public static DaxTile getRandomTileNearby(DaxTile start, int limit) {
        NodeInfo.clearMemory();

        int currentLimit = 0;
        Queue<DaxTile> queue = new LinkedList<>();
        queue.add(start);

        NodeInfo.create(queue.peek()).traversed = true;

        while (!queue.isEmpty()){
            DaxTile current = queue.remove();

            if (++currentLimit > limit) {
                return current;
            }

            if (start.distance(current) > limit * 10) {
                return current;
            }

            for (DaxTile neighbor : current.getNeighbors()) {
                NodeInfo.Details nodeInfo = NodeInfo.create(neighbor);
                if (nodeInfo.traversed){
                    continue;
                }

                nodeInfo.traversed = true;

                queue.add(neighbor);
            }
        }

        log.info(":(");
        return null;
    }


}
