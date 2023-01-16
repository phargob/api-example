package rsb_api.methods;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ObjectID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.cache.definitions.ObjectDefinition;

import net.runelite.rsb.utils.Filter;
import rsb_api.globval.*;
import rsb_api.wrappers.*;

import java.lang.Integer;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;

@Slf4j
/**
 * Bank related operations.
 */
public class Bank {
    private int[] BANKERS;
    private int[] BANK_DEPOSIT_BOX;
    private int[] BANK_CHESTS;
    private int[] BANK_BOOTHS;
    private Point[] UNREACHABLE_BANKERS = {
        new Point(3191, 3445),
        new Point(3180, 3433) // VARROCK EAST
    };

    private MethodContext ctx;
    Bank(final MethodContext ctx) {
        this.ctx = ctx;
        this.assignConstants();
    }

    /**
     * Assigns the ID constants for all the banking objects in RuneScape
     */
    public void assignConstants() {
        ArrayList<Integer> bankers = new ArrayList<>();
        ArrayList<Integer> bankBooths = new ArrayList<>();
        ArrayList<Integer> bankChests = new ArrayList<>();
        ArrayList<Integer> bankDepositBox = new ArrayList<>();
        ObjectID ObjectIDs = new ObjectID();
        try {
            for (Field i : ObjectIDs.getClass().getDeclaredFields()) {
                i.setAccessible(true);
                if (i.getName().contains("BANK_BOOTH")) {
                    bankBooths.add(Integer.valueOf(i.get(ObjectIDs).toString()));
                } else if (i.getName().contains("BANK_DEPOSIT_BOX")) {
                    bankDepositBox.add(Integer.valueOf(i.get(ObjectIDs).toString()));
                } else if (i.getName().contains("BANK_CHEST")) {
                    i.get(ObjectIDs);
                    bankChests.add(Integer.valueOf(i.get(ObjectIDs).toString()));
                }
            }
            for (Field i : net.runelite.api.NpcID.class.getDeclaredFields()) {
                if (i.getName().contains("BANKER")) {
                    bankers.add(Integer.valueOf(i.get(ObjectIDs).toString()));
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        BANKERS = toIntArray(bankers);
        BANK_BOOTHS = toIntArray(bankBooths);
        BANK_CHESTS = toIntArray(bankChests);
        BANK_DEPOSIT_BOX = toIntArray(bankDepositBox);
    }

    private int[] toIntArray(ArrayList<Integer> list) {
        int[] ret = new int[list.size()];
        for(int i = 0;i < ret.length;i++)
            ret[i] = list.get(i);
        return ret;
    }

    /**
     * Checks whether or not the bank is open.
     *
     * @return <code>true</code> if the bank interface is open; otherwise <code>false</code>.
     */
    public boolean isOpen() {
        RSWidget bankInterface = getInterface();
        return bankInterface.isValid() && bankInterface.isVisible();
    }

    /**
     * Checks whether or not the deposit box is open.
     *
     * @return <code>true</code> if the deposit box interface is open; otherwise <code>false</code>.
     */
    public boolean isDepositOpen() {
        return ctx.interfaces.get(WidgetIndices.DepositBox.GROUP_INDEX).isValid();
    }

    public boolean isCollectionOpen() {
        RSWidget widget = ctx.interfaces.get(WidgetIndices.CollectionBox.GROUP_INDEX);
        return widget.isValid() && widget.isVisible();
    }

    /**
     * Closes the bank interface. Supports deposit boxes.
     *
     * @return <code>true</code> if the bank interface is no longer open.
     */
    public boolean close() {
        if (isOpen()) {
            ctx.interfaces.getComponent(GlobalWidgetInfo.BANK_DYNAMIC_CONTAINER)
                .getDynamicComponent(WidgetIndices.DynamicComponents.Global.DYNAMIC_CLOSE_BUTTON).doClick();
            ctx.sleepRandom(500, 1000);
            return !isOpen();
        }
        if (isDepositOpen()) {
            ctx.interfaces.getComponent(GlobalWidgetInfo.DEPOSIT_DYNAMIC_COMPONENTS)
                .getDynamicComponent(WidgetIndices.DynamicComponents.Global.DYNAMIC_CLOSE_BUTTON).doClick();
            ctx.sleepRandom(500, 1000);
            return !isDepositOpen();
        }
        return false;
    }

    public boolean closeCollection() {
        if (isCollectionOpen()) {
            RSWidget w = ctx.interfaces.getComponent(WidgetIndices.CollectionBox.GROUP_INDEX,
                                                     WidgetIndices.CollectionBox.DYNAMIC_CONTAINER);

            w.getDynamicComponent(WidgetIndices.DynamicComponents.Global.DYNAMIC_CLOSE_BUTTON).doClick();
            ctx.sleepRandom(500, 1000);
            return !isOpen();
        }

        return false;
    }

    /**
     * Gets the bank interface.
     *
     * @return The bank <code>RSWidget</code>.
     */
    public RSWidget getInterface() {
        return ctx.interfaces.get(WidgetIndices.Bank.GROUP_INDEX);
    }

    /**
     * Gets the deposit box interface.
     *
     * @return The deposit box <code>RSWidget</code>.
     */
    public RSWidget getBoxInterface() {
        return ctx.interfaces.get(WidgetIndices.DepositBox.GROUP_INDEX);
    }

    public int getAvailableBankSpace() {
        if (isOpen()) {
            return Integer.parseInt(ctx.interfaces.getComponent(GlobalWidgetInfo.BANK_ITEM_MAX).getText())
                    - Integer.parseInt(ctx.interfaces.getComponent(GlobalWidgetInfo.BANK_ITEM_COUNT).getText());
        }
        return -1;
    }

    private class ReachableBankerFilter implements Filter<RSNPC> {
        @Override
        public boolean test(RSNPC npc) {
            final int id = npc.getID();
            final RSTile location = npc.getLocation();
            for (int banker : BANKERS) {
                if (banker == id) {
                    for (Point unreachableBanker : UNREACHABLE_BANKERS) {
                        if (unreachableBanker.equals(location)) {
                            return false;
                        }
                    }

                    for (String s: npc.getActions()) {
                        if (s != null && s.equals("Bank")) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    private class ValidBankFilter implements Filter<RSObject> {
        private int[] validIds;

        public ValidBankFilter(int[] validIds) {
            this.validIds = validIds;
        }

        private boolean checkId(int rsoId) {
            for (int id : validIds) {
                if (id == rsoId) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean test(RSObject rso) {
            final int id = rso.getID();
            if (!this.checkId(id)) {
                return false;
            }

            ObjectDefinition def = rso.getDef();
            if (def != null) {
                for (String s: def.getActions()) {
                    if (s != null && s.equals("Bank")) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public Object getNearest() {
        RSObject bankBooth = ctx.objects.getNearest(new ValidBankFilter(BANK_BOOTHS));
        RSNPC banker = ctx.npcs.getNearest(new ReachableBankerFilter());
        RSObject bankChest = ctx.objects.getNearest(new ValidBankFilter(BANK_CHESTS));

        /* Find the closest one, others are set to null. Remember distance and tile. */
        int lowestDist = Integer.MAX_VALUE;
        RSTile tile;
        if (bankBooth != null) {
            tile = bankBooth.getLocation();
            lowestDist = ctx.calc.distanceTo(tile);
        }
        if (banker != null && ctx.calc.distanceTo(banker) < lowestDist) {
            tile = banker.getLocation();
            lowestDist = ctx.calc.distanceTo(tile);
            bankBooth = null;
        }
        if (bankChest != null && ctx.calc.distanceTo(bankChest) < lowestDist) {
            bankBooth = null;
            banker = null;
        }
        return (bankChest == null) ? (banker == null) ? bankBooth : banker : bankChest;
    }

    /**
     * Opens one of the supported banker NPCs, booths, or chests nearby. If they
     * are not nearby, and they are not null, it will automatically walk to the
     * closest one.
     *
     * @return <code>true</code> if the bank was opened; otherwise <code>false</code>.
     */
    private boolean openHelper(boolean doBankers) {
        if (isOpen()) {
            return true;
        }

        try {
            // XXX i don't know if this works.
            // Does move slightly move enough?
            // if (ctx.menu.isOpen()) {
            //  ctx.mouse.moveSlightly();
            //  ctx.sleepRandom(100, 150);
            // }

            RSObject bankBooth = ctx.objects.getNearest(new ValidBankFilter(BANK_BOOTHS));
            RSNPC banker = ctx.npcs.getNearest(new ReachableBankerFilter());
            RSObject bankChest = ctx.objects.getNearest(new ValidBankFilter(BANK_CHESTS));

            /* Find the closest one, others are set to null. Remember distance and tile. */
            int lowestDist = Integer.MAX_VALUE;
            RSTile tile = null;
            if (bankBooth != null) {
                tile = bankBooth.getLocation();
                lowestDist = ctx.calc.distanceTo(tile);
            }

            if (doBankers &&
                banker != null &&
                ctx.calc.distanceTo(banker) < lowestDist) {
                tile = banker.getLocation();
                lowestDist = ctx.calc.distanceTo(tile);
                bankBooth = null;
            }

            if (bankChest != null &&
                ctx.calc.distanceTo(bankChest) < lowestDist) {
                tile = bankChest.getLocation();
                lowestDist = ctx.calc.distanceTo(tile);
                bankBooth = null;
                banker = null;
            }

            /* Open closest one, if any found */
            if (ctx.calc.tileOnMap(tile) &&
                ctx.fixme.canReach(tile, true)) {

                boolean didAction = false;
                if (bankBooth != null) {
                    didAction = bankBooth.doAction("Bank") || bankBooth.doAction("Use-Quickly");
                } else if (banker != null) {
                    didAction = banker.doAction("Bank", "Banker");
                } else if (bankChest != null) {
                    didAction = bankChest.doAction("Bank") || ctx.menu.doAction("Use");
                }

                if (didAction) {
                    int count = 0;
                    while (!isOpen() && ++count < 20) {
                        ctx.sleepRandom(25, 50);
                        if (ctx.walking.getDestination() != null) {
                            count = 0;
                        }
                    }
                }

            } else if (tile != null) {
                ctx.camera.turnTo(tile);
                ctx.sleepRandom(2000, 3000);
            }

            ctx.sleepRandom(750, 3000);
            return isOpen();

        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean open() {
        return this.openHelper(true);
    }

    public boolean openNoBanker() {
        return this.openHelper(false);
    }

    /**
     * Opens one of the supported deposit boxes nearby. If they are not nearby, and they are not null,
     * it will automatically walk to the closest one.
     *
     * @return <code>true</code> if the deposit box was opened; otherwise
     *         <code>false</code>.
     */
    public boolean openDepositBox() {
        try {
            if (!isDepositOpen()) {
                if (ctx.menu.isOpen()) {
                    ctx.mouse.moveSlightly();
                    ctx.sleepRandom(20, 30);
                }
                RSObject depositBox = ctx.objects.getNearest(BANK_DEPOSIT_BOX);
                if (depositBox != null && ctx.calc.tileOnMap(depositBox.getLocation())) {
                    if (depositBox.doAction("Deposit")) {
                        int count = 0;
                        while (!isDepositOpen() && ++count < 10) {
                            ctx.sleepRandom(200, 400);
                            if (ctx.walking.getDestination() != null) {
                                count = 0;
                            }
                        }
                    } else {
                        ctx.camera.turnTo(depositBox, 20);
                    }
                } else {
                    if (depositBox != null) {
                        ctx.walking.walkTo(depositBox.getLocation());
                    }
                }
            }
            return isDepositOpen();
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deposits all items in inventory, if bank is open. Supports deposit boxes.
     *
     * @return <code>true</code> on success.
     */
    public boolean depositAll() {
        if (isOpen()) {
            return ctx.interfaces.getComponent(GlobalWidgetInfo.BANK_BUTTON_DEPOSIT_CARRIED_ITEMS).doClick();

        } else if (isDepositOpen()) {
            return ctx.interfaces.getComponent(GlobalWidgetInfo.DEPOSIT_BUTTON_DEPOSIT_INVENTORY_ITEMS).doClick();
        }
        return false;
    }

    /**
     * Deposit everything your player has equipped. Supports deposit boxes.
     *
     * @return <code>true</code> on success.
     * @since 6 March 2009.
     */
    public boolean depositAllEquipped() {
        if (isOpen()) {
            return ctx.interfaces.getComponent(GlobalWidgetInfo.BANK_BUTTON_DEPOSIT_WORN_ITEMS).doClick();
        }
        return isDepositOpen() && ctx.interfaces.getComponent(GlobalWidgetInfo.DEPOSIT_BUTTON_DEPOSIT_WORN_ITEMS).doClick();
    }

    /**
     * If bank is open, deposits specified amount of an item into the bank.
     * Supports deposit boxes.
     *
     * @param itemID The ID of the item.
     * @param number The amount to deposit. 0 deposits All. 1,5,10 deposit
     *               corresponding amount while other numbers deposit X.
     * @return <code>true</code> if successful; otherwise <code>false</code>.
     */
    public boolean deposit(int itemID, int number) {
        if (isOpen() || isDepositOpen()) {
            if (number < 0) {
                throw new IllegalArgumentException("number < 0 (" + number + ")");
            }
            RSWidget item = null;
            int itemCount = 0;
            int invCount = isOpen() ? ctx.inventory.getCount(true) : getBoxCount();
            if (!isOpen()) {
                boolean match = false;
                for (int i = 0; i < 28; i++) {
                    RSWidget comp = ctx.interfaces.getComponent(GlobalWidgetInfo.DEPOSIT_ITEMS_CONTAINER).getDynamicComponent(i);
                    if (comp.getId() == itemID) {
                        itemCount += comp.getStackSize();
                        if (!match) {
                            item = comp;
                            match = true;
                        }
                    }
                    if (itemCount > 1) {
                        break;
                    }
                }
            } else {
                RSItem rsItem = ctx.inventory.getItem(itemID);
                if (rsItem != null) {
                    item = rsItem.getComponent();
                }
                itemCount = ctx.inventory.getCount(true, itemID);
            }
            if (item == null) {
                return true;
            }
            switch (number) {
                case 0:
                    item.doAction(itemCount > 1 ? "Deposit-All" : "Deposit-1");
                    break;
                case 1:
                    item.doClick();
                    break;
                case 5:
                    item.doAction("Deposit-" + number);
                    break;
                default:
                    if (!item.doAction("Deposit-" + number)) {
                        if (item.doAction("Deposit-X")) {
                            ctx.sleepRandom(1000, 1300);
                            ctx.keyboard.sendText(String.valueOf(number), true);
                        }
                    }
                    break;
            }
            ctx.sleepRandom(250, 750);
            int cInvCount = isOpen() ? ctx.inventory.getCount(true) : getBoxCount();
            return cInvCount < invCount || cInvCount == 0;
        }
        return false;
    }

    /**
     * Deposits all items in inventory except for the given IDs. Supports
     * deposit boxes.
     *
     * @param items The items not to deposit.
     * @return true on success.
     */
    public boolean depositAllExcept(int... items) {
        if (isOpen() || isDepositOpen()) {
            boolean deposit = true;
            int invCount = isOpen() ? ctx.inventory.getCount(true) : getBoxCount();
            outer:
            for (int i = 0; i < 28; i++) {
                RSWidget item = isOpen() ? ctx.inventory.getItemAt(i).getComponent() :
                        ctx.interfaces.getComponent(GlobalWidgetInfo.DEPOSIT_ITEMS_CONTAINER).getDynamicComponent(i);
                if (item != null && item.getId() != -1) {
                    for (int id : items) {
                        if (item.getId() == id) {
                            continue outer;
                        }
                    }
                    for (int tries = 0; tries < 5; tries++) {
                        deposit(item.getId(), 0);
                        ctx.sleepRandom(600, 900);
                        int cInvCount = isOpen() ? ctx.inventory.getCount(true) : getBoxCount();
                        if (cInvCount < invCount) {
                            invCount = cInvCount;
                            continue outer;
                        }
                    }
                    deposit = false;
                }
            }
            return deposit;
        }
        return false;
    }

    /**
     * Returns the sum of the count of the given items in the bank.
     *
     * @param items The array of items.
     * @return The sum of the stacks of the items.
     */
    public int getCount(final int... items) {
        int itemCount = 0;
        final RSItem[] inventoryArray = getItems();
        for (RSItem item : inventoryArray) {
            for (final int id : items) {
                if (item.getID() == id) {
                    itemCount += item.getStackSize();
                }
            }
        }
        return itemCount;
    }

    /**
     * Get current tab open in the bank.
     *
     * @return int of tab (0-8), or -1 if none are selected (bank is not open).
     */
    public int getCurrentTab() {
        if (isOpen()) {
            return ctx.proxy.getVarbitValue(VarbitIndices.BANK_ACTIVE_TAB);
        }

        return -1;
    }

    /**
     * Gets a tab
     * @param index     Gets the bank tab at the specified index
     * @return          The bank tab
     */
    public RSWidget getTab(int index) {
        return ctx.interfaces.getComponent(GlobalWidgetInfo.BANK_TAB).getComponent(index);
    }

    /**
     * Gets the <code>RSWidget</code> of the given item at the specified index.
     *
     * @param index The index of the item.
     * @return <code>RSWidget</code> if item is found at index; otherwise null.
     */
    public RSItem getItemAt(final int index) {
        final RSItem[] items = getItems();
        if (items != null) {
            for (final RSItem item : items) {
                if (GlobalWidgetInfo.TO_CHILD(item.getComponent().getId()) == index) {
                    return item;
                }
            }
        }
        return null;
    }

    /**
     * Gets the first item with the provided ID in the bank.
     *
     * @param id ID of the item to get.
     * @return The component of the item; otherwise null.
     */
    public RSItem getItem(final int id) {
        final RSItem[] items = getItems();
        if (items != null) {
            for (final RSItem item : items) {
                if (item.getID() == id) {
                    return item;
                }
            }
        }
        return null;
    }

    /**
     * Gets the point on the screen for a given item. Numbered left to right then top to bottom.
     *
     * @param slot The index of the item.
     * @return The point of the item or new Point(-1, -1) if null.
     */
    public Point getItemPoint(final int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("slot < 0 " + slot);
        }
        final RSItem item = getItemAt(slot);
        if (item != null) {
            return item.getComponent().getLocation();
        }
        return new Point(-1, -1);
    }

    /**
     * Gets all the items in the bank's inventory.
     *
     * @return an <code>RSItem</code> array of the bank's inventory interface.
     */
    public RSItem[] getItems() {
        RSWidget bankInterface = getInterface();
        if ((bankInterface == null) || (ctx.interfaces.getComponent(WidgetInfo.BANK_ITEM_CONTAINER) == null)) {
            return new RSItem[0];
        }
        RSWidget[] components = ctx.interfaces.getComponent(WidgetInfo.BANK_ITEM_CONTAINER).getComponents();
        RSItem[] items = new RSItem[components.length];
        for (int i = 0; i < items.length; ++i) {
            items[i] = new RSItem(ctx, components[i]);
        }
        return items;
    }


    /**
     * Opens the bank tab.
     *
     * @param tabNumber The tab number - e.g. view all is 1.
     * @return <code>true</code> on success.
     */
    public boolean openTab(final int tabNumber) {
        return isOpen() && getTab(tabNumber).doClick();
    }

    /**
     * @return <code>true</code> if currently searching the bank.
     */
    public boolean isSearchOpen() {
        return ctx.proxy.getVarcIntValue(VarcIntIndices.CHATBOX_INPUT_TYPE)
                == VarcIntValues.SEARCH.getValue();
    }

    /**
     * Searches for an item in the bank. Returns true if succeeded (does not
     * necessarily mean it was found).
     *
     * @param itemName The item name to find.
     * @return <code>true</code> on success.
     */
    public boolean searchItem(final String itemName) {
        if (!isOpen()) { return false; }
        ctx.interfaces.getComponent(GlobalWidgetInfo.BANK_BUTTON_SEARCH).doAction("Search");
        ctx.sleepRandom(1000, 1500);
        if (!isSearchOpen()) {
            ctx.sleepRandom(250, 750);
        }
        if (isOpen() && isSearchOpen()) {
            ctx.keyboard.sendText(itemName, false);
            ctx.sleepRandom(300, 700);
            return true;
        }
        return false;
    }

    /**
     * Sets the bank rearrange mode to insert.
     *
     * @return <code>true</code> on success.
     */
    public boolean setRearrangeModeToInsert() {
        if (!isOpen()) { return false; }
        if (ctx.clientLocalStorage.getVarpValueAt(VarpIndices.TOGGLE_BANK_REARRANGE_MODE)
                == VarpValues.BANK_REARRANGE_MODE_SWAP.getValue()) {
            ctx.interfaces.getComponent(GlobalWidgetInfo.BANK_BUTTON_INSERT).doClick();
            ctx.sleepRandom(500, 700);
        }
        return ctx.clientLocalStorage.getVarpValueAt(VarpIndices.TOGGLE_BANK_REARRANGE_MODE)
                == VarpValues.BANK_REARRANGE_MODE_INSERT.getValue();
    }

    /**
     * Sets the bank rearrange mode to swap.
     *
     * @return <code>true</code> on success.
     */
    public boolean setRearrangeModeToSwap() {
        if (!isOpen()) { return false; }
        if (ctx.clientLocalStorage.getVarpValueAt(VarpIndices.TOGGLE_BANK_REARRANGE_MODE)
                == VarpValues.BANK_REARRANGE_MODE_INSERT.getValue()) {
            ctx.interfaces.getComponent(GlobalWidgetInfo.BANK_BUTTON_SWAP).doClick();
            ctx.sleepRandom(500, 700);
        }
        return ctx.clientLocalStorage.getVarpValueAt(VarpIndices.TOGGLE_BANK_REARRANGE_MODE)
                == VarpValues.BANK_REARRANGE_MODE_SWAP.getValue();
    }

    /**
     * Sets the bank withdraw mode to item.
     *
     * @return <code>true</code> on success.
     */
    public boolean setWithdrawModeToItem() {
        if (!isOpen()) { return false; }
        if (ctx.clientLocalStorage.getVarpValueAt(VarpIndices.TOGGLE_BANK_WITHDRAW_MODE)
                == VarpValues.BANK_WITHDRAW_MODE_NOTE.getValue()) {
            ctx.interfaces.getComponent(GlobalWidgetInfo.BANK_BUTTON_ITEM).doClick();
            ctx.sleepRandom(500, 700);
        }
        return ctx.clientLocalStorage.getVarpValueAt(VarpIndices.TOGGLE_BANK_WITHDRAW_MODE)
                == VarpValues.BANK_WITHDRAW_MODE_ITEM.getValue();
    }

    /**
     * Sets the bank withdraw mode to note.
     *
     * @return <code>true</code> on success.
     */
    public boolean setWithdrawModeToNote() {
        if (!isOpen()) { return false; }
        if (ctx.clientLocalStorage.getVarpValueAt(VarpIndices.TOGGLE_BANK_WITHDRAW_MODE)
                == VarpValues.BANK_WITHDRAW_MODE_ITEM.getValue()) {
            ctx.interfaces.getComponent(GlobalWidgetInfo.BANK_BUTTON_NOTE).doClick();
            ctx.sleepRandom(500, 700);
        }
        return ctx.clientLocalStorage.getVarpValueAt(VarpIndices.TOGGLE_BANK_WITHDRAW_MODE)
                == VarpValues.BANK_WITHDRAW_MODE_NOTE.getValue();
    }

    /**
     * Tries to withdraw an item.
     * 0 is All. 1,5,10 use Withdraw 1,5,10 while other numbers Withdraw X.
     *
     * @param itemID The ID of the item.
     * @param count  The number to withdraw.
     * @return <code>true</code> on success.
     */
    public boolean withdraw(final int itemID, final int count) {
        if (!isOpen()) { return false; }
        if (count < 0) {
            throw new IllegalArgumentException("count (" + count + ") < 0");
        }
        RSItem rsi = getItem(itemID);
        if (rsi == null) { return false; }
        RSWidget item = rsi.getComponent();
        if (item == null) { return false; }
        while (item.getRelativeX() == 0 && ctx.bank.getCurrentTab() != 0) {
            if (getTab(0).doClick()) {
                ctx.sleepRandom(800, 1300);
            }
        }
        if (!ctx.interfaces.scrollTo(item, ctx.interfaces.getComponent(GlobalWidgetInfo.BANK_SCROLLBAR))) {
            return false;
        }
        int invCount = ctx.inventory.getCount(true);
        String defaultAction = "Withdraw-" + count;
        String action = null;
        switch (count) {
            case 0:
                action = "Withdraw-All";
                break;
            case 1:
                action = defaultAction;
                break;
            case 5:
                action = defaultAction;
                break;
            case 10:
                action = defaultAction;
                break;
            default:
                int i = -1;
                try {
                    i = Integer.parseInt(item.getActions()[4].toLowerCase().trim().replaceAll("\\D", ""));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (i == count) {
                    action = defaultAction;
                }
                else if (item.doAction("Withdraw-X")) {
                    ctx.sleepRandom(1000, 1300);
                    ctx.keyboard.sendText(String.valueOf(count), true);
                }
        }
        if (action != null && item.doAction(action)) {
            ctx.sleepRandom(1000, 1300);
        }
        int newInvCount = ctx.inventory.getCount(true);
        return newInvCount > invCount || newInvCount == 28;
    }

    /**
     * Gets the count of all the items in the inventory with the any of the
     * specified IDs while deposit box is open.
     *
     * @param ids the item IDs to include
     * @return The count.
     */
    public int getBoxCount(int... ids) {
        if (!isDepositOpen()) {
            return -1;
        }
        int count = 0;
        for (int i = 0; i < 28; ++i) {
            for (int id : ids) {
                if (ctx.interfaces.getComponent(GlobalWidgetInfo.DEPOSIT_ITEMS_CONTAINER).isValid()
                        && ctx.interfaces.getComponent(GlobalWidgetInfo.DEPOSIT_ITEMS_CONTAINER).getComponent(i).getId() == id) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Gets the count of all items in your inventory ignoring stack sizes while
     * deposit box is open.
     *
     * @return The count.
     */
    public int getBoxCount() {
        if (!isDepositOpen()) {
            return -1;
        }
        int count = 0;
        for (int i = 0; i < 28; i++) {
            if (ctx.interfaces.getComponent(GlobalWidgetInfo.DEPOSIT_ITEMS_CONTAINER).isValid()
                    && ctx.interfaces.getComponent(GlobalWidgetInfo.DEPOSIT_ITEMS_CONTAINER).getComponent(i).getId() != -1) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the equipment items from the bank interface.
     *
     * @return All equipment items that are being worn.
     */
    public RSItem[] getEquipmentItems() {
        if (ctx.interfaces.getComponent(GlobalWidgetInfo.EQUIPMENT_ITEMS_CONTAINER).isValid()) {
            return new RSItem[0];
        }
        RSWidget[] components = ctx.interfaces.getComponent(GlobalWidgetInfo.EQUIPMENT_ITEMS_CONTAINER).getComponents();
        RSItem[] items = new RSItem[components.length];
        for (int i = 0; i < items.length; i++) {
            items[i] = new RSItem(ctx, components[i]);
        }
        return items;
    }

    /**
     * Gets a equipment item from the bank interface.
     *
     * @param id ID of the item.
     * @return RSItem
     */
    public RSItem getEquipmentItem(final int id) {
        RSItem[] items = getEquipmentItems();
        if (items != null) {
            for (final RSItem item : items) {
                if (item.getID() == id) {
                    return item;
                }
            }
        }
        return null;
    }

    /**
     * Gets the ID of a equipment item based on name.
     *
     * @param name Name of the item.
     * @return -1 if item is not found.
     */
    public int getEquipmentItemID(final String name) {
        RSItem[] items = getEquipmentItems();
        if (items != null) {
            for (final RSItem item : items) {
                if (item.getName().contains(name)) {
                    return item.getID();
                }
            }
        }
        return -1;
    }

    /**
     * Gets the item ID of a item side the bank.
     *
     * @param name Name of the item.
     * @return -1 if item is not found.
     */
    public int getItemID(final String name) {
        RSItem[] items = getItems();
        if (items != null) {
            for (final RSItem item : items) {
                if (item.getName().equalsIgnoreCase(name) ||
                item.getName().toLowerCase().contains(name.toLowerCase())) {
                    return item.getID();
                }
            }
        }
        return -1;
    }
}
