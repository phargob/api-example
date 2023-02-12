package rsb;

import net.runelite.rsb.script.Script;

import rsb.methods.MethodContext;

import rsb.wrappers.RSPlayer;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public abstract class ScriptRunner extends Script {

    protected MethodContext ctx;

    // fun and games with class loaders...
    private static boolean cacheLoaded = false;

    ///////////////////////////////////////////////////////////////////////////////
    // implement these on your script:

    public boolean onStart() {
        return true;
    }

    public abstract int loop();

    public void onFinish() {
    }

    ///////////////////////////////////////////////////////////////////////////////
    // public API for your script:

    /**
     * Stops the current script; player can be logged out before
     * the script is stopped.
     */

    protected void stopScript(boolean logout) {
        if (logout && ctx != null) {
            if (ctx.game.isLoggedIn()) {
                ctx.game.logout();
            }
        }

        this.running = false;
        this.paused = false;
        log.info("Script stopping from within Script...");
    }

    protected int random(int minValue, int maxValue) {
        return ctx.random(minValue, maxValue);
    }

    protected RSPlayer getMyPlayer() {
        return ctx.players.getMyPlayer();
    }

    protected void sleep(int msecs) {
        sleep(msecs, false);
    }

    ///////////////////////////////////////////////////////////////////////////////
    // internal:

    private void sleep(int msecs, boolean earlyBreakAllowed) {
        try {
            while (running && msecs > 0) {
                // if script is stopped, then we can break early
                if (earlyBreakAllowed) {
                    if (!running) {
                        break;
                    }
                }

                Thread.sleep(25);
                msecs -= Math.min(25, msecs);
            }

        } catch (InterruptedException e) {
            ;
        } catch (ThreadDeath td) {
            log.error("ThreadDeath in Script.sleep()");
        }
    }

    public final void onInit() {
        log.info("Creating context");

        this.ctx = new MethodContext(bot.getProxy(), bot.getInputManager());

        if (!ScriptRunner.cacheLoaded) {
            ScriptRunner.cacheLoaded = true;
            ScriptHelper.checkForCacheAndLoad();
        }
    }

    public final void doRun() {
        boolean start = false;
        try {
            start = onStart();

        } catch (RuntimeException ex) {
            log.error("RuntimeException in Script.onStart() ", ex);
            ex.printStackTrace();

        } catch (Throwable ex) {
            log.warn("Uncaught exception from Script.onStart(): ", ex);
        }

        if (!start) {
            log.error("Failed Script.onStart().");

            ctx.mouse.moveOffScreen();
            cleanup();
            return;
        }

        running = true;
        paused = false;
        log.info("Script now running.");
        while (running) {
            if (!paused) {
                int timeOut = -1;
                try {
                    timeOut = loop();

                } catch (RuntimeException e) {
                    log.error("RuntimeException in Script.loop() ", e);
                    e.printStackTrace();

                } catch (Throwable ex) {
                    log.warn("Uncaught exception from Script.loop(): ", ex);
                }

                if (timeOut == -1) {
                    break;
                }

                this.sleep(timeOut, true);

            } else {
                this.sleep(250, true);
            }
        }

        try {
            onFinish();

        } catch (RuntimeException ex) {
            log.error("RuntimeException in Script.onFinish() ", ex);
            ex.printStackTrace();
        } catch (Throwable ex) {
            log.warn("Uncaught exception from Script.onFinish(): ", ex);
        }

        running = false;
        log.info("Script stopped.");

        ctx.mouse.moveOffScreen();
        this.cleanup();
    }
}
