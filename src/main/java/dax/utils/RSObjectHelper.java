package dax.utils;

import dax.Ctx;
import net.runelite.cache.definitions.ObjectDefinition;
import rsb.wrappers.RSObject;

import java.util.Arrays;
import java.util.List;


public class RSObjectHelper {

    public static List<String> getActionsList(RSObject object){
        return Arrays.asList(getActions(object));
    }

    private static String[] getActions(RSObject object){
        String[] emptyActions = new String[0];
        ObjectDefinition definition = object.getDef();
        if (definition == null){
            return emptyActions;
        }
        String[] actions = definition.getActions();
        for (int i = 0; i < actions.length; i++) {
            if (actions[i] == null) {
                actions[i] = "";
            }
        }
        return actions != null ? actions : emptyActions;
    }

    public static String getName(RSObject object){
        ObjectDefinition definition = object.getDef();
        if (definition == null){
            return "null";
        }
        String name = definition.getName();
        return name != null ? name : "null";
    }

}
