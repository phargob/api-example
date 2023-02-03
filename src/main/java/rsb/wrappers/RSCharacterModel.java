package rsb.wrappers;

import net.runelite.api.Actor;
import net.runelite.api.Model;
import rsb.methods.MethodContext;

/**
 * @author GigiaJ
 */
public class RSCharacterModel extends RSModel {

    private final Actor c;

    RSCharacterModel(MethodContext ctx, Model model, Actor c) {
        super(ctx, model);
        this.c = c;
    }

    @Override
    protected int getLocalX() {
        return c.getLocalLocation().getX();
    }

    @Override
    protected int getLocalY() {
        return c.getLocalLocation().getY();
    }

    @Override
    public int getOrientation() {
        return c.getOrientation();
    }
}
