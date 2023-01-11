package rsb_api.methods;

import net.runelite.rsb.internal.input.InputManager;

/**
 * Keyboard related operations.
 */
public class Keyboard {

    private MethodContext ctx;
    private InputManager inputManager;
    Keyboard(MethodContext ctx, InputManager im) {
        this.ctx = ctx;
        this.inputManager = im;
    }

    /**
     * Presses and releases a given key.
     *
     * @param c The character to press.
     */
    public void sendKey(final char c) {
        inputManager.sendKey(c);
    }

    /**
     * Types a given string.
     *
     * @param text       The text to press/send.
     * @param pressEnter <code>true</code> to press enter after pressing the text.
     */
    public void sendText(final String text, final boolean pressEnter) {
        inputManager.sendKeys(text, pressEnter);
    }

    /**
     * Types a given string instantly.
     *
     * @param text       The text to press/send.
     * @param pressEnter <code>true</code> to press enter after pressing the text.
     */
    public void sendTextInstant(final String text, final boolean pressEnter) {
        inputManager.sendKeysInstant(text, pressEnter);
    }

    /**
     * Presses and holds a given key.
     *
     * @param c The character to press.
     * @see #releaseKey(char)
     */
    public void pressKey(final char c) {
        inputManager.pressKey(c);
    }

    /**
     * Releases a given held key.
     *
     * @param c The character to release.
     * @see #pressKey(char)
     */
    public void releaseKey(final char c) {
        inputManager.releaseKey(c);
    }
}
