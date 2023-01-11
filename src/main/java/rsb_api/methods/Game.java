package rsb_api.methods;

import net.runelite.api.GameState;
import net.runelite.api.widgets.WidgetID;
import net.runelite.rsb.internal.globval.GlobalWidgetInfo;
import net.runelite.rsb.internal.globval.VarcIntIndices;
import net.runelite.rsb.internal.globval.VarcIntValues;
import net.runelite.rsb.internal.globval.WidgetIndices;
import net.runelite.rsb.internal.globval.enums.InterfaceTab;
import net.runelite.rsb.internal.globval.enums.ViewportLayout;
import rsb_api.wrappers.*;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Game state and GUI operations.
 */
public class Game {
    private MethodContext ctx;
    Game(final MethodContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Different Types of Chat Modes
     *
     */
    public enum ChatMode {
        VIEW, ON, FRIENDS, OFF, HIDE, AUTOCHAT, STANDARD, CLEAR, SWITCH
    }

    /**
     * Set the specified chat mode
     *
     * @param chatOption one of CHAT_OPTION_
     * @param mode       one of ChatMode
     * @return <code>true</code> if item was clicked correctly; otherwise
     *         <code>false</code>
     */
    public boolean setChatOption(int chatOption, ChatMode mode) {
        mouseChatButton(chatOption, false);
        return ctx.menu.doAction(mode.toString());
    }

    /**
     * Access the last message spoken by a player.
     *
     * @return The last message spoken by a player or "" if none
     */
    public String getLastMessage() {
        RSWidget messages = ctx.interfaces.getComponent(GlobalWidgetInfo.CHATBOX_MESSAGES);
        RSWidget first = messages.getDynamicComponent(WidgetIndices.ChatBox.FIRST_MESSAGE_LABEL);
        if (!first.getText().isEmpty()) {
            RSWidget last = messages.getDynamicComponent(WidgetIndices.ChatBox.LAST_MESSAGE_LABEL);
            if (last.isVisible() && !last.getText().isEmpty()) {
                return last.getText();
            }

            return first.getText();
        }

        return "";
    }

    /**
     * Opens the specified tab at the specified index.
     *
     * @param tab The tab to open.
     * @return <code>true</code> if tab successfully selected; otherwise
     *         <code>false</code>.
     * @see #openTab(InterfaceTab tab, boolean functionKey)
     */
    public boolean openTab(InterfaceTab tab) {
        return openTab(tab, false);
    }

    /**
     * Opens the specified tab at the specified index.
     *
     * @param interfaceTab The tab to open
     * @param useHotkey If wanting to use hotkeys to switch.
     * @return <code>true</code> if tab successfully selected; otherwise
     *         <code>false</code>.
     */
    public boolean openTab(InterfaceTab interfaceTab, boolean useHotkey) {
        if (interfaceTab == getCurrentTab()) {
            return true;
        }

        if (useHotkey) {
            if (interfaceTab.getHotkey() == 0) { return false; } // no hotkey for specified tab
            ctx.keyboard.pressKey((char) interfaceTab.getHotkey());
            ctx.sleepRandom(80, 200);
            ctx.keyboard.releaseKey((char) interfaceTab.getHotkey());

        } else {
            net.runelite.api.widgets.Widget tabWidget = ctx.gui.getTab(interfaceTab);
            if (tabWidget == null) { return false; }
            ctx.interfaces.getComponent(
                    GlobalWidgetInfo.TO_GROUP(tabWidget.getParent().getId()),
                    GlobalWidgetInfo.TO_CHILD(tabWidget.getId())).doClick();
        }

        ctx.sleepRandom(400, 600);
        return interfaceTab == getCurrentTab();
    }

    /**
     * Closes the currently open tab if in resizable mode.
     */
    public void closeTab() {
        InterfaceTab interfaceTab = getCurrentTab();
        if (ctx.gui.getViewportLayout() == ViewportLayout.FIXED_CLASSIC ||
            interfaceTab == InterfaceTab.LOGOUT) {
            return;
        }
        net.runelite.api.widgets.Widget tabWidget = ctx.gui.getTab(interfaceTab);
        if (tabWidget != null) {
            ctx.interfaces.getComponent(
                    GlobalWidgetInfo.TO_GROUP(tabWidget.getParent().getId()),
                    GlobalWidgetInfo.TO_CHILD(tabWidget.getId())).doClick();
        }
    }

    /**
     * Click chat button.
     *
     * @param button Which button? One of CHAT_OPTION
     * @param left   Left or right button? Left = true. Right = false.
     * @return <code>true</code> if it was clicked.
     */
    public boolean mouseChatButton(int button, boolean left) {
        RSWidget chatButton = ctx.interfaces.get(WidgetID.CHATBOX_GROUP_ID).getComponent(button);
        return chatButton.isValid() && chatButton.doClick(left);
    }

    /**
     * Gets the currently open tab.
     *
     * @return The currently open interfaceTab if tab recognized else null;
     */
    public InterfaceTab getCurrentTab() {
        int varcIntValue = ctx.proxy.getVarcIntValue(VarcIntIndices.CURRENT_INTERFACE_TAB);
        return switch (VarcIntValues.valueOf2(varcIntValue)) {
            case TAB_COMBAT_OPTIONS -> InterfaceTab.COMBAT;
            case TAB_SKILLS -> InterfaceTab.SKILLS;
            case TAB_QUEST_LIST -> InterfaceTab.QUESTS;
            case TAB_INVENTORY -> InterfaceTab.INVENTORY;
            case TAB_WORN_EQUIPMENT -> InterfaceTab.EQUIPMENT;
            case TAB_PRAYER -> InterfaceTab.PRAYER;
            case TAB_SPELLBOOK -> InterfaceTab.MAGIC;
            case TAB_FRIEND_LIST -> InterfaceTab.FRIENDS;
            case TAB_LOGOUT -> InterfaceTab.LOGOUT;
            case TAB_SETTINGS -> InterfaceTab.SETTINGS;
            case TAB_MUSIC -> InterfaceTab.MUSIC;
            case TAB_CHAT_CHANNEL -> InterfaceTab.CHAT;
            case TAB_ACC_MANAGEMENT -> InterfaceTab.ACC_MAN;
            case TAB_EMOTES -> InterfaceTab.EMOTES;
            default -> throw new IllegalStateException("Unexpected value: " + VarcIntValues.valueOf(varcIntValue));
        };
    }

    /**
     * Returns the valid chat component.
     *
     * @return <code>RSWidgetChild</code> of the current valid talk interface;
     *         otherwise null.
     */
    public RSWidget getTalkInterface() {
        for (RSWidget component : ctx.interfaces.getComponent(GlobalWidgetInfo.CHATBOX_FULL_INPUT).getComponents()) {
            if (component.isValid() && component.isVisible())
                return component;
        }
        return null;
    }

    /**
     * Closes the bank if it is open and logs out.
     *
     * @return <code>true</code> if the player was logged out.
     */
    public boolean logout() {
        if (ctx.bank.isOpen()) {
            ctx.bank.close();
            ctx.sleepRandom(200, 400);
        }

        if (ctx.bank.isOpen()) {
            return false;
        }

        if (ctx.inventory.isItemSelected()) {
            InterfaceTab currentTab = ctx.game.getCurrentTab();
            InterfaceTab randomTab = InterfaceTab.values()[ctx.random(1, 6)];

            while (randomTab == currentTab) {
                randomTab = InterfaceTab.values()[ctx.random(1, 6)];
            }

            ctx.game.openTab(randomTab);
            ctx.sleepRandom(400, 800);
        }

        if (ctx.inventory.isItemSelected()) {
            return false;
        }

        if (getCurrentTab() != InterfaceTab.LOGOUT) {
            openTab(InterfaceTab.LOGOUT);
            ctx.sleepRandom(300, 600);
        }

        ctx.interfaces.getComponent(GlobalWidgetInfo.LOGOUT_BUTTON).doClick();

        // Final logout button in the logout tab
        ctx.sleepRandom(1500, 2000);
        return !isLoggedIn();
    }

    /**
     * Determines whether or not the client is currently logged in to an
     * account.
     *
     * @return <code>true</code> if logged in; otherwise <code>false</code>.
     */
    public boolean isLoggedIn() {
        return ctx.proxy.getLocalPlayer() != null;
    }

    /**
     * Determines whether or not the client is showing the login screen.
     *
     * @return <code>true</code> if the client is showing the login screen;
     *         otherwise <code>false</code>.
     */
    public boolean isLoginScreen() {
        return ctx.proxy.getLocalPlayer() == null;
    }

    /**
     * Determines whether or not the welcome screen is open.
     *
     * @return <code>true</code> if the client is showing the welcome screen;
     *         otherwise <code>false</code>.
     */
    public boolean isWelcomeScreen() {
        var w = ctx.interfaces.getComponent(GlobalWidgetInfo.LOGIN_MOTW_TEXT);
        return w != null && w.isValid() && w.getAbsoluteY() > 2;
    }

    /**
     * Gets the game state.
     *
     * @return The game state.
     */
    public GameState getClientState() {
        return ctx.proxy.getGameState();
    }

    /**
     * Gets the plane we are currently on. Typically 0 (ground level), but will
     * increase when going up ladders. You cannot be on a negative plane. Most
     * dungeons/basements are on plane 0 elsewhere on the world map.
     *
     * @return The current plane.
     */
    public int getPlane() {
        return ctx.proxy.getPlane();
    }

    /**
     * Gets the x coordinate of the loaded map area (far west).
     *
     * @return The region base x.
     */
    public int getBaseX() {
        return ctx.proxy.getBaseX();
    }

    /**
     * Gets the y coordinate of the loaded map area (far south).
     *
     * @return The region base y.
     */
    public int getBaseY() {
        return ctx.proxy.getBaseY();
    }

    /**
     * Gets the (x, y) coordinate pair of the south-western tile at the base of
     * the loaded map area.
     *
     * @return The region base tile.
     */
    public RSTile getMapBase() {
        return new RSTile(ctx.proxy.getBaseX(), ctx.proxy.getBaseY(), ctx.proxy.getPlane());
    }

    /**
     * Gets the flags relating to the tiles in the scene
     *
     * @return the flags for all the tiles in the current scene
     */
    public byte[][][] getSceneFlags() {
        return ctx.proxy.getTileSettings();
    }

    /**
     * Gets the canvas height.
     *
     * @return The canvas' width.
     */
    public int getWidth() {
        return ctx.proxy.getCanvasWidth();
    }

    /**
     * Gets the canvas height.
     *
     * @return The canvas' height.
     */
    public int getHeight() {
        return ctx.proxy.getCanvasHeight();
    }

}
