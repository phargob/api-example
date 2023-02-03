package rsb;

import net.runelite.rsb.script.Script;
import dax.DaxWalker;

import rsb.RemotePy;
import rsb.methods.MethodContext;

import rsb.wrappers.RSPlayer;
import rsb.wrappers.common.CacheProvider;

import org.python.util.PythonInterpreter;
import org.python.core.PyObject;
import org.python.core.PyInteger;

import java.util.Properties;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public abstract class ScriptRunnerPy extends Script {
    protected PythonInterpreter interpreter;

    private MethodContext ctx;

    // fun and games...
    private static boolean cacheLoaded = false;

    // implement these:

    public void onInit() {
        log.info("Creating java context");

        this.ctx = new MethodContext(bot.getProxy(), bot.getInputManager());

        if (!ScriptRunnerPy.cacheLoaded) {
            ScriptRunnerPy.cacheLoaded = true;
            ScriptHelper.checkForCacheAndLoad();
        }

        String[] argv = {"botlite!"};

        Properties p = new Properties();
        p.setProperty("python.sys.path", "xyz");
        PythonInterpreter.initialize(System.getProperties(), p, argv);
        this.interpreter = PythonInterpreter.threadLocalStateInterpreter(null);
    }

    // so python can access them
    public boolean getRunning() {
        return running;
    }

    public boolean getPaused() {
        return paused;
    }

    public void stopFromWithin() {
        this.running = false;
        this.paused = false;
        log.info("Script stopping from within Script...");
    }

    public final void doRun() {
        log.info("In doRun()");
        try {

            interpreter.set("log", this.log);
            interpreter.set("java_ctx", this.ctx);
            interpreter.set("java_parent", this);

            Class daxClz = Class.forName("dax.DaxWalker");
            interpreter.set("DaxWalker", daxClz);

            Class daxClzEngineState = Class.forName("dax.engine.State");
            interpreter.set("Dax_WalkerEngineState", daxClzEngineState);

            Class reachableClz = Class.forName("dax.path.Reachable");
            interpreter.set("Reachable", reachableClz);

            Class rstileClz = Class.forName("rsb.wrappers.RSTile");
            interpreter.set("RSTile", rstileClz);

            Class rsNPCClz = Class.forName("rsb.wrappers.RSNPC");
            interpreter.set("RSNPC", rsNPCClz);

            Class rsWidgetClz = Class.forName("rsb.wrappers.RSWidget");
            interpreter.set("RSWidget", rsWidgetClz);

            Class rsItemClz = Class.forName("rsb.wrappers.RSItem");
            interpreter.set("RSItem", rsItemClz);

            log.info("execfile...");
            interpreter.execfile("/home/rxe/working/playing/lib/bootstrap.py");

            //Class pythonClz = Class.forName("org.python.util.PythonInterpreter");
            //interpreter.set("py_ClzLoader", pythonClz.getClassLoader());

            running = true;
            paused = false;
            onStart();

        } catch (java.lang.Throwable ex) {
            ex.printStackTrace();
            interpreter.close();
            interpreter = null;
        }

        running = false;
        log.info("Script stopped.");

        ctx.mouse.moveOffScreen();
        this.cleanup();
    }

    protected abstract void onStart();
}
