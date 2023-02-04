package dax.utils;

import net.runelite.rsb.utils.Filter;

import rsb.wrappers.RSArea;
import rsb.wrappers.RSObject;

import rsb.wrappers.common.Positionable;
import rsb.wrappers.WalkerTile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

public interface Filters {

    /**
     * A class used to compare an object against an array with a passable comparing operation
     * @param <T> The type of the objects being compared
     */
    class Comparator<T> {
        /**
         * Iterates through the array and uses the function to perform the passed comparing operation on the passed
         * <var>obj</var> amd each entry in the array to find a result that returns a true statement from the
         * comparing function.
         * @param obj The object derived from a instance of an in-game entity
         * @param array The array of options to compare against
         * @param operation The comparing operation being passed to perform on each obj and option in the array
         * @return <tt>True</tt> if any option in the array fulfills the comparing operation; otherwise <tt>false</tt>
         */
        boolean iterateAndCompare(T obj, T[] array, Function<ArrayList<T>, Boolean> operation) {
            for (T t : array) {
                ArrayList<T> opArray = new ArrayList<>(Arrays.asList(t, obj));
                if (operation.apply(opArray)) {
                    return true;
                }
            }
            return false;
        }
    }

    class Objects implements Filters {

        public static Filter<RSObject> actionsContains(String... actions) {
            return (RSObject object) -> {
                String[] objectActions = object.getDef().getActions();
                Function<ArrayList<String>, Boolean> operation = e -> e.get(0).contains(e.get(1));
                boolean actionCheck = false;
                for (int i = 0; i<objectActions.length; i++)
                    actionCheck = actionCheck  || (new Comparator<String>()).iterateAndCompare(objectActions[i], Arrays.stream(actions).toArray(String[]::new), operation);
                return actionCheck;
            };
        }


        public static Filter<RSObject> actionsEquals(String... actions) {
            return (RSObject object) -> {
                String[] objectActions = object.getDef().getActions();
                Function<ArrayList<String>, Boolean> operation = e -> e.get(0).equals(e.get(1));
                boolean actionCheck = false;
                for (int i = 0; i<objectActions.length; i++)
                    actionCheck = actionCheck  || (new Comparator<String>()).iterateAndCompare(objectActions[i], Arrays.stream(actions).toArray(String[]::new), operation);
                return actionCheck;
            };
        }


        public static Filter<RSObject> actionsNotContains(String... actions) {
            return (RSObject object) -> {
                String[] objectActions = object.getDef().getActions();
                Function<ArrayList<String>, Boolean> operation = e -> !e.get(0).contains(e.get(1));
                boolean actionCheck = false;
                for (int i = 0; i<objectActions.length; i++)
                    actionCheck = actionCheck  || (new Comparator<String>()).iterateAndCompare(objectActions[i], Arrays.stream(actions).toArray(String[]::new), operation);
                return actionCheck;
            };
        }


        public static Filter<RSObject> actionsNotEquals(String... actions) {
            return (RSObject object) -> {
                String[] objectActions = object.getDef().getActions();
                Function<ArrayList<String>, Boolean> operation = e -> !e.get(0).equals(e.get(1));
                boolean actionCheck = false;
                for (int i = 0; i<objectActions.length; i++)
                    actionCheck = actionCheck  || (new Comparator<String>()).iterateAndCompare(objectActions[i], Arrays.stream(actions).toArray(String[]::new), operation);
                return actionCheck;
            };
        }


        public static Filter<RSObject> idEquals(int... ids) {
            return (RSObject object) -> {
                int oid = object.getID();
                Function<ArrayList<Integer>, Boolean> operation = e -> e.get(0).equals(e.get(1));
                return (new Comparator<Integer>()).iterateAndCompare(oid, Arrays.stream(ids).boxed().toArray(Integer[]::new), operation);
            };
        }


        public static Filter<RSObject> idNotEquals(int... ids) {
            return (RSObject object) -> {
                int oid = object.getID();
                Function<ArrayList<Integer>, Boolean> operation = e -> !e.get(0).equals(e.get(1));
                return (new Comparator<Integer>()).iterateAndCompare(oid, Arrays.stream(ids).boxed().toArray(Integer[]::new), operation);
            };
        }


        public static Filter<RSObject> nameContains(String... names) {
            return (RSObject object) -> {
                String oName = object.getName();
                Function<ArrayList<String>, Boolean> operation = e -> e.get(0).contains(e.get(1));
                return (new Comparator<String>()).iterateAndCompare(oName, Arrays.stream(names).toArray(String[]::new), operation);
            };
        }


        public static Filter<RSObject> nameEquals(String... names) {
            return (RSObject object) -> {
                String oName = object.getName();
                Function<ArrayList<String>, Boolean> operation = e -> e.get(0).equals(e.get(1));
                return (new Comparator<String>()).iterateAndCompare(oName, Arrays.stream(names).toArray(String[]::new), operation);
            };
        }


        public static Filter<RSObject> nameNotContains(String... names) {
            return (RSObject object) -> {
                String oName = object.getName();
                Function<ArrayList<String>, Boolean> operation = e -> !e.get(0).contains(e.get(1));
                return (new Comparator<String>()).iterateAndCompare(oName, Arrays.stream(names).toArray(String[]::new), operation);
            };
        }


        public static Filter<RSObject> nameNotEquals(String... names) {
            return (RSObject object) -> {
                String oName = object.getName();
                Function<ArrayList<String>, Boolean> operation = e -> !e.get(0).equals(e.get(1));
                return (new Comparator<String>()).iterateAndCompare(oName, Arrays.stream(names).toArray(String[]::new), operation);
            };
        }

        public static Filter<RSObject> inArea(RSArea area) {
            return (RSObject object) -> {
                RSArea oArea = object.getArea();
                Function<ArrayList<RSArea>, Boolean> operation = e -> e.get(0).contains(e.get(1).getTileArray());
                return (new Comparator<RSArea>()).iterateAndCompare(oArea, new RSArea[]{area}, operation);
            };
        }

        public static Filter<RSObject> notInArea(RSArea area) {
            return (RSObject object) -> {
                RSArea oArea = object.getArea();
                Function<ArrayList<RSArea>, Boolean> operation = e -> !e.get(0).contains(e.get(1).getTileArray());
                return (new Comparator<RSArea>()).iterateAndCompare(oArea, new RSArea[]{area}, operation);
            };
        }

        public static Filter<RSObject> tileEquals(Positionable pos) {
            return (RSObject object) -> {
                WalkerTile oTile = new WalkerTile(object.getLocation());
                WalkerTile pTile = pos.getLocation();
                Function<ArrayList<WalkerTile>, Boolean> operation = e -> e.get(0).equals(e.get(1));
                return (new Comparator<WalkerTile>()).iterateAndCompare(oTile, new WalkerTile[]{pTile}, operation);
            };
        }

        public static Filter<RSObject> tileNotEquals(Positionable pos) {
            return (RSObject object) -> {
                WalkerTile oTile = new WalkerTile(object.getLocation());
                WalkerTile pTile = pos.getLocation();
                Function<ArrayList<WalkerTile>, Boolean> operation = e -> !e.get(0).equals(e.get(1));
                return (new Comparator<WalkerTile>()).iterateAndCompare(oTile, new WalkerTile[]{pTile}, operation);
            };
        }
    }
}
