package rsb_api.wrappers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.cache.definitions.ObjectDefinition;
import net.runelite.client.callback.ClientThread;
import rsb_api.methods.MethodContext;
import rsb_api.wrappers.common.CacheProvider;
import rsb_api.wrappers.common.Clickable07;
import rsb_api.wrappers.common.Positionable;
import rsb_api.wrappers.subwrap.WalkerTile;

/**
 * A wrapper for a tile object which interprets the underlying tile objects type and furthermore
 * acts as a factory for the RSModel of the RSObject (refer to getModel for better explanation)
 *
 * RSObject can represent any {@link Type types} game object
 */
@Slf4j
public class RSObject implements Clickable07, Positionable, CacheProvider<ObjectDefinition> {

    private final TileObject obj;
    private final Type type;
    private final int plane;
    private final ObjectDefinition def;
    private final int id;
    private final MethodContext ctx;

    /**
     * Creates a new RSObject with the following parameters:
     * @param ctx   The context in which the object exists (the singleton RuneLite)
     * @param obj   The TileObject which this RSObject is associated with
     * @param type  The type of game object corresponding to the enumerated {@link Type types}
     * @param plane The plane that this object exists on
     */
    public RSObject(final MethodContext ctx,
                    final TileObject obj, final Type type,
                    final int plane) {
        this.ctx = ctx;
        this.obj = obj;
        this.type = type;
        this.plane = plane;

        if (obj != null) {
            int objId = obj.getId();
            var composition = ctx.proxy.getObjectDefinition(objId);
            if (composition != null && composition.getImpostorIds() != null) {
                var imposter = composition.getImpostor();
                if (imposter != null) {
                    var imposterId = imposter.getId();
                    if (imposterId != -1 && imposterId != objId) {
                        // log.info("imposter {} -> {}", objId, imposterId);
                        objId = imposterId;
                    }
                }
            }


            this.id = objId;
            this.def = (ObjectDefinition) createDefinition(objId);

        } else {
            this.id = -1;
            this.def = null;
        }
    }

    /**
     * Gets the RSTile on which this object is centered. An RSObject may cover
     * multiple tiles, in which case this will return the floored central tile.
     *
     * @return The central RSTile.
     * @see #getArea()
     */
    public WalkerTile getLocation() {
        return ctx.tiles.createWalkerTile(obj.getWorldLocation().getX(),
                                          obj.getWorldLocation().getY(),
                                          obj.getWorldLocation().getPlane());
    }

    /**
     * Gets the area of tiles covered by this object.
     *
     * @return The RSArea containing all the tiles on which this object can be
     *         found.
     */
    public RSArea getArea() {
        if (obj instanceof GameObject) {
            Point sceneMin = ((GameObject) obj).getSceneMinLocation();
            Point sceneMax = ((GameObject) obj).getSceneMaxLocation();
            WorldPoint worldMin = WorldPoint.fromScene(ctx.proxy,
                                                       sceneMin.getX(),
                                                       sceneMin.getY(),
                                                       ctx.proxy.getPlane());
            WorldPoint worldMax = WorldPoint.fromScene(ctx.proxy,
                                                       sceneMax.getX(),
                                                       sceneMax.getY(),
                                                       ctx.proxy.getPlane());

            return new RSArea(new RSTile(worldMin), new RSTile(worldMax), plane);
        }

        RSTile loc = getLocation();
        return new RSArea(loc, loc, plane);
    }

    /**
     * Gets the object definition of this object.
     *
     * @return The RSObjectDef if available, otherwise <code>null</code>.
     */
    public ObjectDefinition getDef() {
        if (obj != null) {
            return def;
        }
        return null;
    }

    /**
     * Gets the ID of this object.
     *
     * @return The ID.
     */
    public int getID() {
        return id;
    }

    /**
     * Returns the name of the object.
     *
     * @return The object name if the definition is available; otherwise "".
     */
    public String getName() {
        ObjectDefinition objectDef = getDef();
        return objectDef != null ? objectDef.getName() : "";
    }

    /**
     * Gets the Model of this object.
     * Checks what kind of object it is and returns the model of the object based on that
     *
     * @return The RSModel, or <code>null</code> if unavailable.
     */
    public RSModel getModel() {
        try {
            Model model;
            if (obj instanceof WallObject) {
                model = toModel(((WallObject) obj).getRenderable1());
                if (model != null && model.getVerticesX() != null)
                    if (((WallObject) obj).getRenderable2() != null)
                        return new RSWallObjectModel(ctx, model, toModel(((WallObject) obj).getRenderable2()), obj);
                    else {
                        return new RSWallObjectModel(ctx, model, obj);
                    }
                return new RSWallObjectModel(ctx, null, obj);
            } else if (obj instanceof GroundObject) {
                model = toModel(((GroundObject) obj).getRenderable());
                if (model != null && model.getVerticesX() != null)
                    return new RSGroundObjectModel(ctx, model, new RSTile(obj.getWorldLocation()).getTile(ctx));
            } else if (obj instanceof DecorativeObject) {
                model = toModel(((DecorativeObject) obj).getRenderable());
                if (model != null && model.getVerticesX() != null)
                    return new RSGroundObjectModel(ctx, model, new RSTile(obj.getWorldLocation()).getTile(ctx));
            } else if (obj instanceof ItemLayer) {
                return null;
            } else if (obj instanceof GameObject) {
                model = toModel(((GameObject) obj).getRenderable());
                if (model != null && model.getVerticesX() != null)
                    return new RSObjectModel(ctx, model, (GameObject) obj);
            }
        } catch (AbstractMethodError e) {
            log.debug("Error", e);
        }
        return null;
    }

    private Model toModel(Renderable r) {
        if (r instanceof Model) {
            return (Model) r;
        } else if (r != null) {
            return r.getModel();
        } else {
            return null;
        }
    }

    /**
     * Determines whether this object is on the game screen.
     *
     * @return <code>true</code> if the object is on screen else <code>false</code>
     */
    public boolean isOnScreen() {
        RSModel model = getModel();
        if (model == null) {
            return ctx.calc.tileOnScreen(getLocation());
        } else {
            return ctx.calc.pointOnScreen(model.getPoint());
        }
    }

    /**
     * Returns this object's type.
     *
     * @return The type of the object.
     */
    public Type getType() {
        return type;
    }

    /**
     * Performs the specified action on this object.
     *
     * @param action the menu item to search and click
     * @return       <code>true</code> if clicked, <code>false</code> if object does not contain the
     *               desired action
     */
    public boolean doAction(final String action) {
        return doAction(action, null);
    }

    /**
     * Performs the specified action on this object.
     *
     * @param action the action of the menu item to search and click
     * @param option the option of the menu item to search and click
     * @return       <code>true</code> if clicked, <code>false</code> if object does not contain the
     *               desired action
     */
    public boolean doAction(final String action, final String option) {
        RSModel model = this.getModel();
        if (model != null) {
            return model.doAction(action, option);
        }

        // XXX ugh dont silently revert to this - was reason model getting was broken for ever

        return ctx.tiles.doAction(getLocation(), action, option);
    }

    /**
     * Left-clicks this object.
     *
     * @return <code>true</code> if clicked otherwise <code>false</code>
     */
    public boolean doClick() {
        return doClick(true);
    }

    /**
     * Clicks this object.
     *
     * @param leftClick <code>true</code> to left-click; <code>false</code> to right-click.
     * @return <code>true</code> if clicked otherwise <code>false</code>
     */
    public boolean doClick(boolean leftClick) {
        RSModel model = this.getModel();
        if (model != null) {
            return model.doClick(leftClick);
        } else {
            Point p = ctx.calc.tileToScreen(getLocation());
            if (ctx.calc.pointOnScreen(p)) {
                ctx.mouse.move(p);
                if (ctx.calc.pointOnScreen(p)) {
                    ctx.mouse.click(leftClick);
                    return true;
                } else {
                    p = ctx.calc.tileToScreen(getLocation());
                    if (ctx.calc.pointOnScreen(p)) {
                        ctx.mouse.move(p);
                        ctx.mouse.click(leftClick);
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Moves the mouse over this object.
     * @return true if the object was hovered over (or attempted to) otherwise false
     */
    public boolean doHover() {
        RSModel model = getModel();
        if (model != null) {
            model.hover();
            return true;
        } else {
            Point p = ctx.calc.tileToScreen(getLocation());
            if (ctx.calc.pointOnScreen(p)) {
                ctx.mouse.move(p);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof RSObject) && ((RSObject) o).obj == obj;
    }

    @Override
    public int hashCode() {
        if (obj != null) {
            return obj.hashCode();
        }
        return 0;
    }

    /**
     * Turns the camera towards the RSObject.
     * @return <code>true</code> If RSObject is on screen after attempted to move camera angle.
     */
    public boolean turnTo() {
        ctx.camera.turnTo(this);
        for (int i=0; i<10; i++) {
            ctx.sleep(25);
            return isOnScreen();
        }

        return false;
    }

    /**
     * Checks if the RSObject is clickable (interactive)
     * @return  <code>true</code> if the object is capable of being interacted with otherwise <code>false</code>
     */
    public boolean isClickable() {
        if (obj == null) {
            return false;
        }
        RSModel model = getModel();
        if (model == null) {
            return false;
        }
        return true;
        // XXX ???
        //return model.getModel().isClickable();
    }

    /**
     * Gets the TileObject associated with this RSObject
     * @return the TileObject else null
     */
    public TileObject getObj() {
        return obj;
    }

    /**
     * The type of game object
     * Game, Decorative, Ground, or Wall
     */
    public enum Type {
        GAME(1), DECORATIVE(2), GROUND(4), WALL(8);

        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getBitValue() {
            return value;
        }

        public static Type getType(int value) {
            for (Type type : values()) {
                if (type.getBitValue() == value) {
                    return type;
                }
            }
            return null;
        }

    }
}
