package rsb_api.wrappers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.TileItem;
import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.definitions.ObjectDefinition;
import rsb_api.methods.MethodContext;
import rsb_api.wrappers.common.CacheProvider;
import rsb_api.wrappers.common.Clickable07;

import java.util.Arrays;

/**
 * Represents an item (with an id and stack size). May or may not
 * wrap a component.
 */
@Slf4j
public class RSItem implements Clickable07, CacheProvider<ItemDefinition>  {

    private final int id;
    private final int stackSize;
    private final ItemDefinition def;
    private RSWidget component;
    private RSWidgetItem item;

    // XXX actually not used
    private MethodContext ctx;
    public RSItem(final MethodContext ctx, final RSWidgetItem item) {
        this.ctx = ctx;
        this.id = item.getItemId();
        this.stackSize = item.getStackSize();
        this.item = item;
        this.def = this.getDefinition(id);
    }

    public RSItem(final MethodContext ctx, final TileItem item) {
        this.ctx = ctx;
        this.id = item.getId();
        this.stackSize = item.getQuantity();
        this.def = this.getDefinition(id);
        // This is only used for ground objects and thus does not need component declared
    }

    public RSItem(final MethodContext ctx, final RSWidget item) {
        this.ctx = ctx;
        this.id = item.getItemId();
        this.stackSize = item.getStackSize();
        this.component = item;
        this.def = this.getDefinition(id);
    }

    private ItemDefinition getDefinition(int id) {
        if (id == -1) {
            return null;
        }

        ItemDefinition def = (ItemDefinition) createDefinition(id);
        if (def.notedTemplate != -1 && def.notedID != -1) {
            // assume def is a noted version, we need to link it
            ItemDefinition template = (ItemDefinition) createDefinition(def.notedTemplate);
            ItemDefinition unNoted = (ItemDefinition) createDefinition(def.notedID);
            if (template != null && unNoted != null) {
                def.linkNote(template, unNoted);
            }
        }

        return def;
    }

    /**
     * Gets this item's definition if available.
     *
     * @return The RSItemDef; or <code>null</code> if unavailable.
     */
    public ItemDefinition getDefinition() {
        return def;
    }

    /**
     * Gets this item's id.
     *
     * @return The id.
     */
    public int getID() {
        return id;
    }

    /**
     * Gets this item's stack size.
     *
     * @return The stack size.
     */
    public int getStackSize() {
        return stackSize;
    }

    /**
     * Returns whether or not this item has an available definition.
     *
     * @return <code>true</code> if an item definition is available;
     *         otherwise <code>false</code>.
     */
    public boolean hasDefinition() {
        return getDefinition() != null;
    }

    /**
     * Gets the item wrapped by this RSItem
     *
     * @return The wrapped item or <code>null</code>
     */
    public RSWidgetItem getItem() {
        return item;
    }

    /**
     * Gets the component wrapped by this RSItem.
     *
     * @return The wrapped component or <code>null</code>.
     */
    public RSWidget getComponent() {
        return component;
    }

    /**
     * Checks whether or not a valid component is being wrapped.
     *
     * @return <code>true</code> if there is a visible wrapped component.
     */
    public boolean isComponentValid() {
        return component != null && component.isVisible();
    }

    /**
     * Checks whether a valid item is being wrapped.
     *
     * @return <code>true</code> if there is a visible wrapped item
     */
    public boolean isItemValid() {
        return item.isValid();
    }

    /**
     * Gets the name of this item using the wrapped component's name
     * if available, otherwise the definition if available.
     *
     * @return The item's name or <code>null</code> if not found.
     */
    public String getName() {
        if (component != null && component.getName() != null) {
            return component.getName();
        } else {
            ItemDefinition definition = getDefinition();
            if (definition != null) {
                return definition.getName().replaceAll("<.*?>", "");
            }
        }
        return null;
    }

    /**
     * Provides a list of inventory actions for the given item.
     * @return The list of inventory actions for the item
     */
    public String[] getInventoryActions() {
        if (id < 0 && stackSize < 0) {
            return null;
        }
        ItemDefinition definition = getDefinition();
        if (definition != null) {
            return getDefinition().getInterfaceOptions();
        }
        return null;
    }

    /**
     * Provides a list of ground actions for the given item.
     * @return The list of ground actions for the item
     */
    public String[] getGroundActions() {
        if (id < 0 && stackSize < 0) {
            return new String[]{""};
        }
        ItemDefinition definition = getDefinition();
        if (definition != null) {
            return getDefinition().getOptions();
        }
        return null;
    }

    /**
     * Performs the given action on the component wrapped by
     * this RSItem if possible.
     *
     * @param action The action to perform.
     * @return <code>true</code> if the component was clicked
     *         successfully; otherwise <code>false</code>.
     */
    public boolean doAction(final String action) {
        return doAction(action, null);
    }

    /**
     * Performs the given action on the component wrapped by
     * this RSItem if possible.
     *
     * @param action The action to perform.
     * @param option The option of the action to perform.
     * @return <code>true</code> if the component was clicked
     *         successfully; otherwise <code>false</code>.
     */
    public boolean doAction(final String action, final String option) {
        return (component != null) ? component.doAction(action, option) : item.doAction(action, option);
    }

    /**
     * Clicks the component wrapped by this RSItem if possible.
     *
     * @param left <code>true</code> if the component should be
     *             left-click; <code>false</code> if it should be right-clicked.
     * @return <code>true</code> if the component was clicked
     *         successfully; otherwise <code>false</code>.
     */
    public boolean doClick(boolean left) {
        return (component != null) ? component.doClick(left) : item.doClick(left);
    }

    public boolean doClick() {
        return (component != null) ? component.doClick(true) : item.doClick(true);
    }

    public boolean doHover() {
        return (component != null) && component.doHover();
    }

    public boolean isClickable() {
        return component.isValid() && component.isVisible() && component.isSelfVisible();
    }
}