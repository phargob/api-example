package rsb_api.wrappers;

import net.runelite.api.GameObject;
import net.runelite.api.Model;
import rsb_api.methods.MethodContext;

class RSObjectModel extends RSModel {

    private final GameObject object;

    RSObjectModel(MethodContext ctx, Model model, GameObject object) {
        super(ctx, model);
        this.object = object;
    }

    @Override
    protected int getLocalX() {
        return object.getX();
    }

    @Override
    protected int getLocalY() {
        return object.getY();
    }

    @Override
    public int getOrientation() {
        return object.getOrientation();
    }
}
