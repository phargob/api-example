package rsb_api.wrappers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.cache.definitions.NpcDefinition;
import rsb_api.methods.MethodContext;
import rsb_api.wrappers.common.Clickable07;
import rsb_api.wrappers.common.Positionable;
import rsb_api.wrappers.subwrap.WalkerTile;

@Slf4j
public abstract class RSCharacter implements Clickable07, Positionable {
    protected MethodContext ctx;
    public RSCharacter(MethodContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Retrieves a reference to the client accessor. For internal use. The
     * reference should be stored in a SoftReference by subclasses to allow for
     * garbage collection when appropriate.
     *
     * @return The client accessor.
     */

    protected abstract Actor getAccessor();
    protected abstract Actor getInteracting();

    /**
     * Performs an action on a humanoid character (tall and skinny).
     *
     * @param action The action of the menu entry to be clicked (if available).
     * @return <code>true</code> if the option was found; otherwise <code>false</code>.
     */
    public boolean doAction(final String action) {
        return doAction(action, null);
    }

    /**
     * Performs an action on a humanoid character (tall and skinny).
     *
     * @param action The action of the menu entry to be clicked (if available).
     * @param option The option of the menu entry to be clicked (if available).
     * @return <code>true</code> if the option was found; otherwise <code>false</code>.
     */
    public boolean doAction(final String action, final String option) {
        RSModel model = this.getModel();
        return model != null && this.isValid() && model.doAction(action, option);
    }

    public RSModel getModel() {
        Actor actor = getAccessor();
        if (actor != null) {
            Model model = actor.getModel();
            if (model != null) {
                return new RSCharacterModel(ctx, model, actor);
            }
        }

        return null;
    }

    public int getAnimation() {
        return getAccessor().getAnimation();
    }

    public int getPoseAnimation() {
        // trying to keep same semantics as getAnimation()
        int idleAnimation = getAccessor().getIdlePoseAnimation();
        int poseAnimation = getAccessor().getPoseAnimation();
        if (idleAnimation == poseAnimation) {
            return -1;
        }

        return poseAnimation;
    }

    public int getGraphic() {
        return getAccessor().getGraphic();
    }

    public int getHeight() {
        return getAccessor().getLogicalHeight();
    }

    /**
     * @return The % of HP
     */
    public int getHPPercent() {
        int healthRatio = getAccessor().getHealthRatio();
        if (healthRatio == -1) return -1;
        return isInCombat() ? healthRatio : 100;
    }

    public WalkerTile getLocation() {
        Actor actor = getAccessor();
        if (actor == null) {
            return null;
        }

        return ctx.tiles.createWalkerTile(actor.getWorldLocation());
    }

    public String getMessage() {
        return getAccessor().getOverheadText();
    }

    /**
     * Gets the minimap location, of the character. Note: This does work when
     * it's walking!
     *
     * @return The location of the character on the minimap.
     */
    public Point getMinimapLocation() {
        Actor actor = getAccessor();
        int actorX = ctx.proxy.getBaseX() + (actor.getLocalLocation().getX() / 32 - 2) / 4;
        int actorY = ctx.proxy.getBaseY() + (actor.getLocalLocation().getY() / 32 - 2) / 4;
        return ctx.calc.worldToMinimap(actorX, actorY);
    }

    public String getName() {
        return null; // should be overridden, obviously
    }

    public int getLevel() {
        return -1; // should be overridden too
    }

    public int getOrientation() {
        return (int) (270 - (getAccessor().getOrientation() & 0x3fff) / 45.51) % 360;
    }

    public Point getScreenLocation() {
        Actor actor = getAccessor();
        RSModel model = getModel();
        if (model == null) {
            return ctx.calc.groundToScreen(actor.getLocalLocation().getX(),
                                           actor.getLocalLocation().getY(),
                                           actor.getLogicalHeight() / 2);
        } else {
            return model.getPoint();
        }
    }

    public boolean isBeingAttacked() {
        if (ctx.game.isLoggedIn()) {
            if (getAccessor().getInteracting() != null) {
                return getAccessor().getHealthRatio() > 0;
            }
        }
        return false;
    }

    public boolean isAttacking() {
        if (ctx.game.isLoggedIn()) {
            if (getAccessor().getInteracting() != null) {
                return getAccessor().getInteracting().getHealthRatio() > 0;
            }
        }
        return false;
    }

    // TODO: this is far from enough to decide
    public boolean isInCombat() {
        return isAttacking() || isBeingAttacked();
    }

    public boolean isInteractingWithLocalPlayer() {
        Player localPlayer = ctx.proxy.getLocalPlayer();
        if (localPlayer == null) return false;
        return getAccessor() == localPlayer.getInteracting();
    }

    /**
    * Checks to determine whether the character is in the idle animation or not
    *
    * @return true if the character is in the idle animation otherwise false
    */
    public boolean isIdle() {
        return getAnimation() == -1 && getPoseAnimation() == -1;
    }

     // TODO: public boolean isMoving()

    public boolean isOnScreen() {
        RSModel model = getModel();
        if (model == null) {
            return ctx.calc.tileOnScreen(getLocation());
        } else {
            return ctx.calc.pointOnScreen(model.getPoint());
        }
    }

    public boolean isValid() {
        return getAccessor() != null;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(getAccessor());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RSCharacter character) {
            return character.getAccessor() == getAccessor();
        }
        return false;
    }

    /*
    @Override
    public String toString() {
        final RSCharacter inter = getInteracting();
        return "[anim="
                + getAnimation()
                + ",msg="
                + getMessage()
                + ",interact="
                + (inter == null ? "null" : inter.isValid() ? inter
                .getMessage() : "Invalid") + "]";
    }*/

    /**
     * Turns towards the RSCharacter.
     * @return <code>true</code> - If RSCharacter is on screen after attempting to move camera angle.
     */
    public boolean turnTo() {
        if (!this.isOnScreen()) {
            ctx.camera.turnTo(this);
            return this.isOnScreen();
        }
        return false;
    }

    /**
     * Hovers this Player/NPC
     */
    public boolean doHover() {
        RSModel model = getModel();
        if (model == null) {
            return false;
        }
        this.getModel().hover();
        return true;
    }

    public boolean doClick() {
        RSModel model = getModel();
        if (model == null) {
            return false;
        }
        this.getModel().doClick(true);
        return true;
    }

    public boolean doClick(boolean leftClick) {
        RSModel model = getModel();
        if (model == null) {
            return false;
        }
        this.getModel().doClick(leftClick);
        return true;
    }

    public boolean isClickable() {
        RSModel model = getModel();
        if (model == null) {
            return false;
        }
        return model.getModel().isClickable();
    }

    public DIRECTION getDirectionFacing() {
        int angle = this.getAccessor().getOrientation();

        int round = angle >>> 9;
        int up = angle & 128;
        if (up != 0) {
            // round up
            ++round;
        }
        return switch (round & 7) {
        case 0 -> DIRECTION.S;
        case 1 -> DIRECTION.SW;
        case 2 -> DIRECTION.W;
        case 3 -> DIRECTION.NW;
        case 4 -> DIRECTION.N;
        case 5 -> DIRECTION.NE;
        case 6 -> DIRECTION.E;
        case 7 -> DIRECTION.SE;
        default -> throw new IllegalStateException();
        };
    }

    public enum DIRECTION {
        N, S, E, W, NE, NW, SE, SW;
    }
}
