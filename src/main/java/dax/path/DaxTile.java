package dax.path;

import dax.Ctx;

import dax.path.CollisionFlags;

import rsb.wrappers.WalkerTile;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;


public class DaxTile {

    private int x, y, z, collisionData;

    private DaxTile(int x, int y, int z, int collisionData) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.collisionData = collisionData;
    }

    public boolean blockedNorth() {
        return CollisionFlags.blockedNorth(this.collisionData);
    }

    public boolean blockedEast() {
        return CollisionFlags.blockedEast(this.collisionData);
    }

    public boolean blockedSouth() {
        return CollisionFlags.blockedSouth(this.collisionData);
    }
    public boolean blockedWest() {
        return CollisionFlags.blockedWest(this.collisionData);
    }

    public boolean isWalkable() {
        return CollisionFlags.isWalkable(this.collisionData);
    }

    public boolean isInitialized(){
        return (!(blockedNorth() &&
                  blockedEast() &&
                  blockedSouth() &&
                  blockedWest() &&
                  !isWalkable()) ||
                CollisionFlags.check(collisionData, CollisionFlags.INITIALIZED));
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Collection<DaxTile> getNeighbors() {
        Collection<DaxTile> neighbors = new HashSet<>();
        boolean nNeighbor = false, eNeighbor = false, sNeighbor = false, wNeighbor = false;
        DaxTile n = get(getX(), getY() + 1, getZ());
        if (!blockedNorth()) {
            if (n != null && n.isWalkable()){
                neighbors.add(n);
                nNeighbor = true;
            }
        }
        DaxTile e = get(getX() + 1, getY(), getZ());
        if (!blockedEast()) {
            if (e != null && e.isWalkable()){
                neighbors.add(e);
                eNeighbor = true;
            }
        }
        DaxTile s = get(getX(), getY() - 1, getZ());
        if (!blockedSouth()) {
            if (s != null && s.isWalkable()){
                neighbors.add(s);
                sNeighbor = true;
            }
        }

        DaxTile w = get(getX() - 1, getY(), getZ());
        if (!blockedWest()) {
            if (w != null && w.isWalkable()){
                neighbors.add(w);
                wNeighbor = true;
            }
        }

        if (nNeighbor && eNeighbor){
            if (!n.blockedEast() && !e.blockedNorth()) {
                DaxTile ne = get(getX() + 1, getY() + 1, getZ());
                if (ne != null && ne.isWalkable()) {
                    neighbors.add(ne);
                }
            }
        }
        if (sNeighbor && eNeighbor){
            if (!s.blockedEast() && !e.blockedNorth()) {
                DaxTile se = get(getX() + 1, getY() - 1, getZ());
                if (se != null && se.isWalkable()) {
                    neighbors.add(se);
                }
            }
        }
        if (sNeighbor && wNeighbor){
            if (!s.blockedWest() && !w.blockedSouth()) {
                DaxTile sw = get(getX() - 1, getY() - 1, getZ());
                if (sw != null && sw.isWalkable()) {
                    neighbors.add(sw);
                }
            }
        }
        if (nNeighbor && wNeighbor){
            if (!n.blockedWest() && !w.blockedNorth()) {
                DaxTile nw = get(getX() - 1, getY() + 1, getZ());
                if (nw != null && nw.isWalkable()) {
                    neighbors.add(nw);
                }
            }
        }
        return neighbors;
    }

    public int distance(DaxTile node) {
        return (int)((Math.sqrt(
                Math.pow((node.getX() - getX()), 2) + (Math.pow((node.getY() - getY()), 2)))) * 10);
    }

    public WalkerTile toWalkerTile() {
        return Ctx.ctx.tiles.createWalkerTile(getX(), getY(), getZ());
    }

    @Override
    public int hashCode() {
        long bits = 7L;
        bits = 31L * bits + Double.doubleToLongBits(getX());
        bits = 31L * bits + Double.doubleToLongBits(getY());
        bits = 31L * bits + Double.doubleToLongBits(getZ());
        return (int) (bits ^ (bits >> 32));
    }

    @Override
    // note collision data is ignored
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof DaxTile)) {
            return false;
        }

        DaxTile other = (DaxTile) obj;
        return getX() == other.getX() && getY() == other.getY() && getZ() == other.getZ();
    }

    @Override
    public String toString() {
        return "(" + getX() + "," + getY() + "," + getZ() + ")";
    }


    private static HashMap<Integer, HashMap<Integer, HashMap<Integer, DaxTile>>> xMap = new HashMap<>();

    public static DaxTile get(int x, int y, int z) {
        var yMap = xMap.get(x);
        if (yMap == null) {
            return null;
        }

        var zMap = yMap.get(y);
        if (zMap == null) {
            return null;
        }

        return zMap.get(z);
    }

    private static DaxTile create(int x, int y, int z, int collision){
        DaxTile tile = new DaxTile(x, y, z, collision);
        if (!tile.isInitialized()) {
            return null;
        }

        // add to map
        var yMap = xMap.computeIfAbsent(tile.getX(), k -> new HashMap<>());
        var zMap = yMap.computeIfAbsent(tile.getY(), k -> new HashMap<>());
        zMap.put(tile.getZ(), tile);

        return tile;
    }

    private static void clearMemory() {
        xMap = new HashMap<>();
    }

    /* grabs current collision data from client, and populates mapping */
    public static void generateRealTimeCollision() {
        clearMemory();

        int plane = Ctx.getMyLocation().getPlane();
        int[][] collisionData = Ctx.ctx.walking.getCollisionData();

        if (collisionData == null) {
            return;
        }

        for (int i = 0; i < collisionData.length; i++) {
            for (int j = 0; j < collisionData[i].length; j++) {
                // convert scene data to world location
                WalkerTile localTile = new WalkerTile(Ctx.ctx, i, j, plane, WalkerTile.TYPES.SCENE);
                WalkerTile worldTile = localTile.toWorldTile();

                DaxTile.create(worldTile.getX(), worldTile.getY(), worldTile.getPlane(), collisionData[i][j]);
            }
        }
    }


}
