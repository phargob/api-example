package rsb.methods;

import lombok.extern.slf4j.Slf4j;

import net.runelite.rsb.util.Timer;
import rsb.wrappers.RSCharacter;
import rsb.wrappers.RSObject;
import rsb.wrappers.RSTile;
import rsb.wrappers.RSWidget;

import java.awt.event.KeyEvent;

/**
 * Camera related operations.
 */
@Slf4j
public class Camera {

    private MethodContext ctx;
    private ROTATION_METHOD rotationMethod;

    Camera(MethodContext ctx) {
        this.ctx = ctx;
        rotationMethod = ROTATION_METHOD.DEFAULT;
    }

    /**
     * Turns to a RSCharacter (RSNPC or RSPlayer).
     *
     * @param c The RSCharacter to turn to.
     */
    public void turnTo(final RSCharacter c) {
        int angle = getCharacterAngle(c);
        setAngle(angle);
    }

    /**
     * Turns to within a few degrees of an RSCharacter (RSNPC or RSPlayer).
     *
     * @param c   The RSCharacter to turn to.
     * @param dev The maximum difference in the angle.
     */
    public void turnTo(final RSCharacter c, final int dev) {
        int angle = getCharacterAngle(c);
        angle = ctx.random(angle - dev, angle + dev + 1);
        setAngle(angle);
    }

    /**
     * Turns to an RSObject.
     *
     * @param o The RSObject to turn to.
     */
    public void turnTo(final RSObject o) {
        int angle = getObjectAngle(o);
        setAngle(angle);
    }


    /**
     * Turns to within a few degrees of an RSObject.
     *
     * @param o   The RSObject to turn to.
     * @param dev The maximum difference in the turn angle.
     */
    public void turnTo(final RSObject o, final int dev) {
        int angle = getObjectAngle(o);
        angle = ctx.random(angle - dev, angle + dev + 1);
        setAngle(angle);
    }

    /**
     * Turns to a specific RSTile.
     *
     * @param tile Tile to turn to.
     */
    public void turnTo(final RSTile tile) {
        int angle = getTileAngle(tile);
        setAngle(angle);
    }

    /**
     * Turns within a few degrees to a specific RSTile.
     *
     * @param tile Tile to turn to.
     * @param dev  Maximum deviation from the angle to the tile.
     */
    public void turnTo(final RSTile tile, final int dev) {
        int angle = getTileAngle(tile);
        angle = ctx.random(angle - dev, angle + dev + 1);
        setAngle(angle);
    }

    /**
     * Sets the altitude to max or minimum.
     *
     * @param up True to go up. False to go down.
     * @return <code>true</code> if the altitude was changed.
     */
    public boolean setPitch(boolean up) {
        if (up) {
            return setPitch(100);
        } else {
            return setPitch(0);
        }
    }

    public void setRotationMethod(ROTATION_METHOD rotationMethod) {
        this.rotationMethod = rotationMethod;
    }

    /**
     * Set the camera to a certain percentage of the maximum pitch. Don't rely
     * on the return value too much - it should return whether the camera was
     * successfully set, but it isn't very accurate near the very extremes of
     * the height.
     *
     * This also depends on the maximum camera angle in a region, as it changes
     * depending on situation and surroundings. So in some areas, 68% might be
     * the maximum altitude. This method will do the best it can to switch the
     * camera altitude to what you want, but if it hits the maximum or stops
     * moving for any reason, it will return.
     *
     * Mess around a little to find the altitude percentage you like. In later
     * versions, there will be easier-to-work-with methods regarding altitude.
     *
     * @param percent The percentage of the maximum pitch to set the camera to.
     * @return true if the camera was successfully moved; otherwise false.
     */
    public boolean setPitch(int percent) {
        int curAlt = getPitch();
        int lastAlt = 0;
        if (curAlt == percent) {
            return true;
        } else if (curAlt < percent) {
            ctx.keyboard.pressKey((char) KeyEvent.VK_UP);
            long start = System.currentTimeMillis();
            while (curAlt < percent && System.currentTimeMillis() - start < ctx.random(50, 100)) {
                if (lastAlt != curAlt) {
                    start = System.currentTimeMillis();
                }
                lastAlt = curAlt;
                ctx.sleepRandom(20, 30);
                curAlt = getPitch();
            }
            ctx.keyboard.releaseKey((char) KeyEvent.VK_UP);
            return true;
        } else {
            ctx.keyboard.pressKey((char) KeyEvent.VK_DOWN);
            long start = System.currentTimeMillis();
            while (curAlt > percent && System.currentTimeMillis() - start < ctx.random(50, 100)) {
                if (lastAlt != curAlt) {
                    start = System.currentTimeMillis();
                }
                lastAlt = curAlt;
                ctx.sleepRandom(20, 30);
                curAlt = getPitch();
            }
            ctx.keyboard.releaseKey((char) KeyEvent.VK_DOWN);
            return true;
        }
    }

    /**
     * Moves the camera in a random direction for a given time.
     *
     * @param timeOut The maximum time in milliseconds to move the camera for.
     */
    public void moveRandomly(int timeOut) {
        Timer timeToHold = new Timer(timeOut);
        int lowestCamAltPossible = ctx.random(75, 100);
        int vertical = ctx.random(0, 20) < 15 ? KeyEvent.VK_UP : KeyEvent.VK_DOWN;
        int horizontal = ctx.random(0, 20) < 5 ? KeyEvent.VK_LEFT : KeyEvent.VK_RIGHT;
        if (ctx.random(0, 10) < 8) {
            ctx.keyboard.pressKey((char) vertical);
        }
        if (ctx.random(0, 10) < 8) {
            ctx.keyboard.pressKey((char) horizontal);
        }
        while (timeToHold.isRunning() &&
               ctx.proxy.getCameraZ() >= lowestCamAltPossible) {
            ctx.sleep(10);
        }
        ctx.keyboard.releaseKey((char) vertical);
        ctx.keyboard.releaseKey((char) horizontal);
    }

    /**
     * Rotates the camera to a specific angle in the closest direction.
     *
     * @param degrees The angle to rotate to.
     */
    public void setAngle(int degrees) {
        final long start = System.currentTimeMillis();

        if (getAngleTo(degrees) > 5) {
            ctx.keyboard.pressKey((char) KeyEvent.VK_LEFT);

            while (getAngleTo(degrees) > 5) {
                ctx.sleepRandom(25, 100);

                if (System.currentTimeMillis() - start > 2000) {
                    log.warn("exiting setAngle() early");
                    break;
                }
            }

            ctx.keyboard.releaseKey((char) KeyEvent.VK_LEFT);

        } else if (getAngleTo(degrees) < -5) {
            ctx.keyboard.pressKey((char) KeyEvent.VK_RIGHT);

            while (getAngleTo(degrees) < -5) {
                ctx.sleepRandom(25, 100);

                if (System.currentTimeMillis() - start > 2000) {
                    log.warn("exiting setAngle() early");
                    break;
                }
            }

            ctx.keyboard.releaseKey((char) KeyEvent.VK_RIGHT);
        }
    }

    /**
     * Rotates the camera to the specified cardinal direction.
     *
     * @param direction The char direction to turn the map. char options are w,s,e,n
     *                  and defaults to north if character is unrecognized.
     */
    public void setCompass(char direction) {
        switch (direction) {
            case 'n':
                setAngle(359);
                break;
            case 'w':
                setAngle(89);
                break;
            case 's':
                setAngle(179);
                break;
            case 'e':
                setAngle(269);
                break;
            default:
                setAngle(359);
                break;
        }
    }

    /**
     * Uses the compass component to set the camera to face north.
     */
    public void setNorth() {
        new RSWidget(ctx, ctx.gui.getCompass()).doClick();
    }

    /**
     * Returns the camera angle at which the camera would be facing a certain
     * character.
     *
     * @param n the RSCharacter
     * @return The angle
     */
    public int getCharacterAngle(RSCharacter n) {
        return getTileAngle(n.getLocation());
    }

    /**
     * Returns the camera angle at which the camera would be facing a certain
     * object.
     *
     * @param o The RSObject
     * @return The angle
     */
    public int getObjectAngle(RSObject o) {
        return getTileAngle(o.getLocation());
    }

    /**
     * Returns the camera angle at which the camera would be facing a certain
     * tile.
     *
     * @param t The target tile
     * @return The angle in degrees
     */
    public int getTileAngle(RSTile t) {
        int a = (ctx.calc.angleToTile(t) - 90) % 360;
        return a < 0 ? a + 360 : a;
    }

    /**
     * Returns the angle between the current camera angle and the given angle in
     * degrees.
     *
     * @param degrees The target angle.
     * @return The angle between the who angles in degrees.
     */
    public int getAngleTo(int degrees) {
        int ca = getAngle();
        if (ca < degrees) {
            ca += 360;
        }
        int da = ca - degrees;
        if (da > 180) {
            da -= 360;
        }
        return da;
    }

    /**
     * Returns the current compass orientation in degrees, with North at 0,
     * increasing counter-clockwise to 360.
     *
     * @return The current camera angle in degrees.
     */
    public int getAngle() {
        // the client uses fixed point radians 0 - 2^14
        // degrees = yaw * 360 / 2^14 = yaw / 45.5111...
        // This leaves it on a scale of 45 versus a scale of 360 so we multiply it by 8 to fix that.
        return (int) Math.abs(ctx.proxy.getCameraYaw() / 45.51 * 8);
    }

    /**
     * Returns the current percentage of the maximum pitch of the camera in an
     * open area.
     *
     * @return The current camera altitude percentage.
     */
    public int getPitch() {
        return (int) ((ctx.proxy.getCameraPitch() - 1024) / 20.48);
    }

    /**
     * Returns the current x position of the camera.
     *
     * @return The x position.
     */
    public int getX() {
        return ctx.proxy.getCameraX();
    }

    /**
     * Returns the current y position of the camera.
     *
     * @return The y position.
     */
    public int getY() {
        return ctx.proxy.getCameraY();
    }

    /**
     * Returns the current z position of the camera.
     *
     * @return The z position.
     */
    public int getZ() {
        return ctx.proxy.getCameraZ();
    }

    public enum ROTATION_METHOD {
        DEFAULT, ONLY_KEYS, ONLY_MOUSE;
    }

}
