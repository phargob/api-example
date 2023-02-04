package dax.api_lib.models;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dax.Ctx;
import net.runelite.api.coords.WorldPoint;
import rsb.wrappers.common.Positionable;
import rsb.wrappers.WalkerTile;

public class Point3D {


    private int x, y, z;

    public Point3D(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
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

    public JsonElement toJson() {
        return new Gson().toJsonTree(this);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    public Positionable toPositionable() {
        return new Positionable() {
            public WalkerTile getAnimablePosition() {
                return Ctx.ctx.tiles.createWalkerTile(x, y, z);
            }

            public boolean adjustCameraTo() {
                return false;
            }

            @Override
            public WalkerTile getLocation() {
                return Ctx.ctx.tiles.createWalkerTile(new WorldPoint(x, y, z));
            }

            @Override
            public boolean turnTo() {
                return false;
            }
        };
    }

    public static Point3D fromPositionable(Positionable positionable) {
        WalkerTile WalkerTile = positionable.getLocation();
        return new Point3D(WalkerTile.getX(), WalkerTile.getY(), WalkerTile.getPlane());
    }

}
