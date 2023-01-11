package rsb_api.wrappers;

import net.runelite.api.Model;
import net.runelite.api.Tile;
import rsb_api.methods.MethodContext;

public class RSGroundItemModel extends RSModel {
    private final Tile tile;

    RSGroundItemModel(MethodContext ctx, Model model, Tile tile) {
        super(ctx, model);
        this.tile = tile;
    }

    @Override
    protected int getLocalX() {
        return tile.getLocalLocation().getX();
    }

    @Override
    protected int getLocalY() {
        return tile.getLocalLocation().getY();
    }
}
