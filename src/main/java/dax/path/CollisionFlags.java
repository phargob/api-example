package dax.path;

public class CollisionFlags {
    public static final int OPEN        = 0x0;
    public static final int OCCUPIED    = 0x100;
    public static final int SOLID       = 0x20000;
    public static final int BLOCKED     = 0x200000;
    public static final int CLOSED      = 0xFFFFFF;
    public static final int INITIALIZED = 0x1000000;

    public static final int NORTHWEST = 0x1;
    public static final int NORTH  = 0x2; //fences etc
    public static final int NORTHEAST = 0x4;
    public static final int EAST   = 0x8;
    public static final int SOUTHEAST = 0x10;
    public static final int SOUTH  = 0x20;
    public static final int SOUTHWEST = 0x40;
    public static final int WEST   = 0x80;

    // blocked line of sight
    public static final int BLOCKED_LOS_NORTH = 0x400;
    public static final int BLOCKED_LOS_EAST = 0x1000;
    public static final int BLOCKED_LOS_SOUTH = 0x4000;
    public static final int BLOCKED_LOS_WEST = 0x10000;


    public static boolean check(int flag, int checkFlag) {
        return (flag & checkFlag) == checkFlag;
    }

    public static boolean blockedNorth(int collisionData) {
        return check(collisionData, NORTH) || check(collisionData, BLOCKED_LOS_NORTH);
    }

    public static boolean blockedEast(int collisionData) {
        return check(collisionData, EAST) || check(collisionData, BLOCKED_LOS_EAST);
    }

    public static boolean blockedSouth(int collisionData) {
        return check(collisionData, SOUTH) || check(collisionData, BLOCKED_LOS_SOUTH);
    }

    public static boolean blockedWest(int collisionData) {
        return check(collisionData, WEST) || check(collisionData, BLOCKED_LOS_WEST);
    }

    public static boolean isWalkable(int collisionData) {
        return !(check(collisionData, OCCUPIED) || check(collisionData, SOLID) ||
                 check(collisionData, BLOCKED) || check(collisionData, CLOSED));
    }

    public static boolean isInitialized(int collisionData){
        return check(collisionData, INITIALIZED);
    }

}
