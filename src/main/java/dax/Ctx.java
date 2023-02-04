package dax;

import rsb.methods.MethodContext;
import rsb.wrappers.WalkerTile;

// XXX HACK TO KEEP WORKING
public class Ctx {
    public static MethodContext ctx;

    public static void init(MethodContext ctx) {
        Ctx.ctx = ctx;
    }

    public static WalkerTile getMyLocation() {
        return new WalkerTile(new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation()));
    }
}
