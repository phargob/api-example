package dax;


// XXX temp
import dax.api_lib.models.*;
import dax.api_lib.WebWalkerServerApi;

import dax.engine.WalkerEngine;

import rsb.wrappers.common.Positionable;
import rsb.wrappers.WalkerTile;
import rsb.methods.MethodContext;

import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DaxWalker {

    public void setCredentials() {
        var provider = new DaxCredentialsProvider() {
                @Override
                public DaxCredentials getDaxCredentials() {
                    if (Ctx.ctx.random(0, 10) > 5) {
                        return new DaxCredentials("sub_DPjXXzL5DeSiPf", "PUBLIC-KEY");
                        //return new DaxCredentials("sub_DPjcfqN4YkIxm8", "PUBLIC-KEY");
                    } else {
                        return new DaxCredentials("sub_DPjXXzL5DeSiPf", "PUBLIC-KEY");
                    }

                }
            };


        WebWalkerServerApi.getInstance().setDaxCredentialsProvider(provider);
        log.info("DaxWalker - setCredentials");
    }

    public static void setCtx(MethodContext ctx) {
        Ctx.init(ctx);
    }

    public static WalkerEngine walkToEngine(MethodContext ctx, Positionable destination,
                                            boolean accurateDestination) {
        Ctx.init(ctx);

        var walker = new DaxWalker();
        walker.setCredentials();

        WalkerTile start = Ctx.getMyLocation();
        var w = WebWalkerServerApi.getInstance();

        PathResult res = w.getPath(Point3D.fromPositionable(start),
                                   Point3D.fromPositionable(destination),
                                   PlayerDetails.generate());

        var path = res.toWalkerTilePath();
        return new WalkerEngine(path, accurateDestination);
    }

    public static boolean walkTo(MethodContext ctx, Positionable destination,
                                 boolean accurateDestination) {


        Ctx.init(ctx);
        WalkerTile start = Ctx.getMyLocation();
        if (start.equals(destination)) {
            return true;
        }

        var engine = walkToEngine(ctx, destination, accurateDestination);
        return engine.walkPath();
    }

}
