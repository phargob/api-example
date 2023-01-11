package rsb_api.wrappers.subwrap;

import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import rsb_api.methods.MethodContext;

import rsb_api.wrappers.RSTile;
import rsb_api.wrappers.common.Clickable07;
import rsb_api.wrappers.common.Positionable;

public class WalkerTile extends RSTile implements Clickable07, Positionable {


    public enum TYPES {
        ANIMABLE, LOCAL, WORLD, SCENE;
    }

    private TYPES type;

    private MethodContext ctx;

    public WalkerTile(WalkerTile tile) {
        // XXX why not use the tile.plane?
        super(tile.getX(), tile.getY(), tile.ctx.proxy.getPlane());
        this.ctx = tile.ctx;
        type = tile.type;
    };

    public WalkerTile(MethodContext ctx, int x, int y, int plane, TYPES type) {
        super(x, y, plane);
        this.ctx = ctx;
        this.type = type;
    }

    public boolean isClickable() {
        return ctx.calc.tileOnScreen(this.toWorldTile());
    }

    @Override
    public boolean doAction(String action) {
        return ctx.tiles.doAction(this.toWorldTile(), action);
    }

    @Override
    public boolean doAction(String action, String option) {
        return ctx.tiles.doAction(this.toWorldTile(), action, option);
    }

    @Override
    public boolean doClick() {
        return ctx.tiles.doAction(this.toWorldTile(), "Walk here");
    }

    @Override
    public boolean doClick(boolean leftClick) {
        return ctx.tiles.doAction(this.toWorldTile(), "Walk here");
    }

    @Override
    public boolean doHover() {
        Point p = ctx.calc.tileToScreen(this.toWorldTile());
        if (isClickable()) {
            ctx.mouse.move(p);
            return true;
        }
        return false;
    }

    @Override
    public boolean turnTo() {
        if (isClickable()) {
            ctx.camera.turnTo(this.toWorldTile());
            return true;
        }
        return false;
    }

    public boolean isOnScreen() {
        return ctx.calc.tileOnScreen(this.toWorldTile());
    }

    public WalkerTile toWorldTile() {
        WalkerTile walkerTile = new WalkerTile(this);

        if (walkerTile.type == TYPES.LOCAL) {
            WorldPoint point = WorldPoint.fromLocal(ctx.proxy, new LocalPoint(x, y));
            walkerTile.x = point.getX();
            walkerTile.y = point.getY();
            walkerTile.plane = ctx.proxy.getPlane();
        }

        if (walkerTile.type == TYPES.SCENE) {
            walkerTile.x = ctx.proxy.getBaseX() + x;
            walkerTile.y = ctx.proxy.getBaseY() + y;
            //WorldPoint.fromScene(ctx.client, x, y, plane);
        }

        walkerTile.type = TYPES.WORLD;
        return walkerTile;
    }

    public WalkerTile toLocalTile() {
        WalkerTile walkerTile = new WalkerTile(this);
        if (walkerTile.type == TYPES.WORLD) {
            int baseX = ctx.proxy.getBaseX();
            int baseY = ctx.proxy.getBaseY();
            LocalPoint point = LocalPoint.fromScene(x - baseX, y - baseY);
            walkerTile.x = point.getX();
            walkerTile.y = point.getY();

        } else if (walkerTile.type == TYPES.SCENE) {
            LocalPoint point = LocalPoint.fromScene(x, y);
            walkerTile.x = point.getX();
            walkerTile.y = point.getY();
        }

        walkerTile.type = TYPES.LOCAL;
        return walkerTile;
    }

    public WalkerTile toSceneTile() {
        // XXX does this lose the plane?
        WalkerTile walkerTile = new WalkerTile(this);
        if (walkerTile.type != TYPES.SCENE) {
            if (walkerTile.type == TYPES.WORLD) {
                walkerTile = walkerTile.toLocalTile();
            }
            walkerTile.x = walkerTile.x >>> Perspective.LOCAL_COORD_BITS;
            walkerTile.y = walkerTile.y >>> Perspective.LOCAL_COORD_BITS;
            walkerTile.type = TYPES.SCENE;
        }
        return walkerTile;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getPlane() {
        return plane;
    }

    public TYPES getType() {
        return type;
    }

    public int distanceTo(Positionable positionable) {
        return (int) ctx.calc.distanceBetween(this.toWorldTile(), positionable.getLocation());
    }

    public double distanceToDouble(Positionable positionable) {
        return ctx.calc.distanceBetween(this.toWorldTile(), positionable.getLocation());
    }

    @Override
    public WalkerTile getLocation() {
        return this;
    }

    public WalkerTile translate(int x, int y) {
        // ??? where is this used, and shouldnt it return a copy.  why bother returning if doing
        // local translation...
        this.x += x;
        this.y += y;
        return this;
    }
}
