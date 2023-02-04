package dax.path;

import java.util.HashMap;


public class NodeInfo {

    // ok just an x,y,z hashmap to Details... but it only used for traverse
    private static HashMap<Integer,
                      HashMap<Integer,
                        HashMap<Integer, Details>>> mapping = new HashMap<>();

    public static Details get(DaxTile tile) {
        var xMap = mapping;

        var yMap = xMap.get(tile.getX());
        if (yMap == null) {
            return null;
        }

        var zMap = yMap.get(tile.getY());
        if (zMap == null) {
            return null;
        }

        Details details = zMap.get(tile.getZ());
        if (details == null) {
            return null;
        }

        return details;
    }

    public static Details create(DaxTile tile) {
        // maps x,y,z from mapping
        var xMap = mapping;
        var yMap = xMap.computeIfAbsent(tile.getX(), k -> new HashMap<>());
        var zMap = yMap.computeIfAbsent(tile.getY(), k -> new HashMap<>());
        return zMap.computeIfAbsent(tile.getZ(), k -> new Details());
    }


    public static void clearMemory() {
        mapping.clear();
        mapping = new HashMap<>();
    }

    public static class Details {
        public boolean traversed;
    }

}
