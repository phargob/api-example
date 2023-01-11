package rsb_api.methods;

import net.runelite.api.MenuEntry;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.Text;

import net.runelite.api.Point;

import lombok.extern.slf4j.Slf4j;

import java.awt.FontMetrics;
import java.util.Arrays;
import java.util.Collections;


/**
 * Context menu related operations.
 * XXX Doesn't support stretched or anything like that?
 */

@Slf4j
public class Menu {
    private final int TOP_OF_MENU_BAR = 18;

    // XXX isnt the height not length?
    private final int MENU_ENTRY_LENGTH = 15;

    private final int MENU_SIDE_BORDER = 7;
    private final int MAX_DISPLAYABLE_ENTRIES = 32;

    private boolean LOG_MENU_DEBUG = false;

    private MethodContext ctx;

    Menu(final MethodContext ctx) {
        this.ctx = ctx;
    }

    private FontMetrics getFontMetrics() {
        return ctx.proxy.getCanvas().getGraphics().getFontMetrics(FontManager.getRunescapeBoldFont());
    }

    private boolean clickMain(final int i) {
        String[] entries = getEntriesString();
        String item = entries[i];

        int x = ctx.proxy.getMenuX();
        int y = ctx.proxy.getMenuY();

        FontMetrics fm = getFontMetrics();

        int mid = (fm.stringWidth(item) + MENU_SIDE_BORDER) / 2;
        int rwidth = Math.max(2, (int) (fm.stringWidth(item) * 0.25));
        int xOff = mid + ctx.random(-rwidth, rwidth);


        int yOff = TOP_OF_MENU_BAR + (((MENU_ENTRY_LENGTH * i) + ctx.random(2, MENU_ENTRY_LENGTH - 2)));

        if (LOG_MENU_DEBUG) {
            //log.info("x {}, y {} w {} h {}", calculateX(), calculateY(),
            //       calculateWidth(), calculateHeight());
            log.info("xx {}, yy {} ww {} hh {}",
                     ctx.proxy.getMenuX(),
                     ctx.proxy.getMenuY(),
                     ctx.proxy.getMenuWidth(),
                     ctx.proxy.getMenuHeight());

            log.info("mid {}, xOff {}", x + mid, x + xOff);
        }

        x += xOff;
        y += yOff;

        ctx.mouse.move(x, y);

        if (!this.isOpen()) {
            log.warn("NOT OPEN anymore in clickMain() :(");
            return false;
        }

        if (!ctx.mouse.isPresent()) {
            log.warn("Mouse moved offscreen");
            // XXX dump everything here
            return false;
        }

        ctx.sleepRandom(50, 100);
        ctx.mouse.click(true);
        if (LOG_MENU_DEBUG) {
            log.info("Click menu success");
        }

        return true;
    }

    /**
     * Returns the index in the menu for a given action. Starts at 0.
     *
     * @param action The action that you want the index of.
     * @return The index of the given target in the context menu; otherwise -1.
     */
    private int getIndex(String action) {
        // note this can return the first one, which might not be what you want (ie use target)
        action = action.toLowerCase();

        MenuEntry[] entries = getEntries();
        for (int i = 0; i < entries.length; i++) {
            if (entries[i] == null) {
                continue;
            }

            String menuAction = Text.standardize(entries[i].getOption());

            if (menuAction.contains(action)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Returns the index in the menu for a given action with a given target.
     * Starts at 0.
     *
     * @param action The action of the menu entry of which you want the index.
     * @param target The target of the menu entry of which you want the index.
     *               If target is null, operates like getIndex(String action).
     * @return The index of the given target in the context menu; otherwise -1.
     */
    public int getIndex(String action, String target) {
        if (target == null) {
            return getIndex(action);
        }

        action = action.toLowerCase();
        target = target.toLowerCase();

        MenuEntry[] entries = getEntries();
        for (int i = 0; i < entries.length; i++) {
            if (entries[i] == null) {
                continue;
            }

            String menuAction = Text.standardize(entries[i].getOption());
            String menuTarget = Text.standardize(entries[i].getTarget());

            if (menuAction.contains(action) && menuTarget.contains(target)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Clicks the menu target. Will left-click if the menu item is the first,
     * otherwise open menu and click the target.
     *
     * @param action The action (or action substring) to click.
     * @return <code>true</code> if the menu item was clicked; otherwise
     * <code>false</code>.
     */
    public boolean doAction(String action) {
        return doAction(action, null);
    }

    /**
     * Clicks the menu target. Will left-click if the menu item is the first,
     * otherwise open menu and click the target.
     *
     * @param action The action (or action substring) to click.
     * @param target The target (or target substring) of the action to click.
     * @return <code>true</code> if the menu item was clicked; otherwise
     * <code>false</code>.
     */
    public boolean doAction(String action, String target) {
        int idx = getIndex(action, target);

        if (idx == -1 || idx > MAX_DISPLAYABLE_ENTRIES) {
            log.info("failed Menu.doAction() with idx {}", idx);
            return false;
        }

        if (LOG_MENU_DEBUG) {
            log.info("action: {}, target: {}, indx: {}", action, target, idx);
        }

        if (!isOpen()) {
            if (idx == 0) {
                // XXX run on client thread?  often reports wrong
                if (getIndex(action, target) == 0) {
                    ctx.mouse.click(true);
                    if (LOG_MENU_DEBUG) {
                        log.info("Menu.doAction() - success left clicking action");
                    }
                    return true;

                } else {
                    return false;
                }
            }

            if (LOG_MENU_DEBUG) {
                log.info("right click - open menu");
            }

            // ensure we don't move after
            ctx.mouse.click(false, 0);
            for (int ii=0; ii<5; ii++) {
                ctx.sleepRandom(50, 100);
                if (isOpen()) {
                    if (LOG_MENU_DEBUG) {
                        log.info("menu is now open");
                    }

                    break;
                }
            }
        }

        if (!isOpen()) {
            log.warn("menu NOT open in doAction: {}", idx);
            return false;
        }

        // recalculate index, and then if not changed, click
        if (idx != getIndex(action, target)) {
            log.warn("menu changed underneath feet");
            return false;
        }

        return clickMain(idx);
    }

    public MenuEntry[] getEntries() {
        // gets from runelite
        MenuEntry[] entries = ctx.proxy.getMenuEntries();

        // want to reverse order so from top to bottom
        Collections.reverse(Arrays.asList(entries));
        return entries;
    }

    public String[] getEntriesString() {
        MenuEntry[] entries = getEntries();
        String[] entryStrings = new String[entries.length];

        for (int i = 0; i < entries.length; i++) {
            var e = entries[i];
            String s = Text.standardize(e.getOption());
            if (e.getTarget() != null) {
                s += " " + Text.standardize(e.getTarget());
            }

            entryStrings[i] = s;
        }

        return entryStrings;
    }

    public String getHoverText() {
        // used from DAX - untested
        var entries = getEntriesString();
        String item = entries[0];
        return (entries.length > 2) ? item + " / " + (entries.length - 1) + " more options" : item;
    }

    /**
     * Checks whether or not the menu is open.
     *
     * @return <code>true</code> if the menu is open; otherwise <code>false</code>.
     */
    public boolean isOpen() {
        return ctx.proxy.isMenuOpen();
    }

    public void enableDebug(boolean value) {
        LOG_MENU_DEBUG = value;
    }
}
