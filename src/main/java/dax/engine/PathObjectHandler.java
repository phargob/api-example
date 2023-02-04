package dax.engine;

import dax.Ctx;

import dax.utils.Filters;
import dax.utils.WaitFor;
import dax.utils.RSObjectHelper;
import dax.utils.AccurateMouse;
import dax.utils.InteractionHelper;
import dax.utils.StdRandom;

import dax.path.BFS;
import dax.path.DaxTile;
import dax.path.PathAnalyzer;

import net.runelite.cache.definitions.ObjectDefinition;

import net.runelite.rsb.utils.Filter;

import rsb.wrappers.RSArea;
import rsb.wrappers.RSItem;
import rsb.wrappers.RSObject;
import rsb.wrappers.WalkerTile;

import java.util.*;
import java.util.stream.Collectors;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PathObjectHandler {

    private static PathObjectHandler instance;

    private final TreeSet<String> sortedOptions, sortedBlackList, sortedBlackListOptions, sortedHighPriorityOptions;

    private PathObjectHandler() {
        sortedOptions = new TreeSet<>(Arrays.asList("Enter", "Cross", "Pass", "Open", "Close", "Walk-through", "Use", "Pass-through", "Exit",
                                                    "Walk-Across", "Go-through", "Walk-across", "Climb", "Climb-up", "Climb-down",
                                                    "Climb-over", "Climb over", "Climb-into", "Climb-through", "Board", "Jump-from",
                                                    "Jump-across", "Jump-to", "Squeeze-through", "Jump-over", "Pay-toll(10gp)", "Step-over",
                                                    "Walk-down", "Walk-up","Walk-Up", "Travel", "Get in", "Investigate", "Operate", "Climb-under",
                                                    "Jump", "Crawl-down", "Crawl-through", "Activate","Push", "Squeeze-past",
                                                    "Walk-Down", "Swing-on", "Climb up"));

        sortedBlackList = new TreeSet<>(Arrays.asList("Coffin", "Drawers", "null"));
        sortedBlackListOptions = new TreeSet<>(Arrays.asList("Chop down"));
        sortedHighPriorityOptions = new TreeSet<>(Arrays.asList("Pay-toll(10gp)", "Squeeze-past"));
    }

    private static PathObjectHandler getInstance() {
        return instance != null ? instance : (instance = new PathObjectHandler());
    }

    private static Filter <RSObject> createFilter(PathAnalyzer.DestinationDetails destinationDetails,
                                                  String name, String action) {
        return Filters.Objects.inArea(new RSArea(destinationDetails.getAssumed(), 1))
            .combine(Filters.Objects.nameEquals(name), true)
            .combine(Filters.Objects.actionsContains(action), true);
    }

    private enum SpecialObject {

        ROCKFALL("Rockfall", "Mine", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                RSObject o = Ctx.ctx.objects.getNearest(createFilter(destinationDetails, "Rockfall", "Mine"));
                return o != null && o.isClickable();
            }
        }),
        ROOTS("Roots", "Chop", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                RSObject o = Ctx.ctx.objects.getNearest(createFilter(destinationDetails, "Roots", "Chop"));
                return o != null && o.isClickable();
            }
        }),
        ROCK_SLIDE("Rockslide", "Climb-over", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                RSObject o = Ctx.ctx.objects.getNearest(createFilter(destinationDetails, "Rockslide", "Climb-over"));
                return o != null && o.isClickable();
            }
        }),
        ROOT("Root", "Step-over", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                RSObject o = Ctx.ctx.objects.getNearest(createFilter(destinationDetails, "Root", "Step-over"));
                return o != null && o.isClickable();
            }
        }),
        BRIMHAVEN_VINES("Vines", "Chop-down", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                RSObject o = Ctx.ctx.objects.getNearest(createFilter(destinationDetails, "Vines", "Chop-down"));
                return o != null && o.isClickable();
            }
        }),
        AVA_BOOKCASE ("Bookcase", "Search", Ctx.ctx.tiles.createWalkerTile(3097, 3359, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getDestination().getX() >= 3097 &&
                    destinationDetails.getAssumed().equals(Ctx.ctx.tiles.createWalkerTile(3097, 3359, 0));
            }
        }),
        AVA_LEVER ("Lever", "Pull", Ctx.ctx.tiles.createWalkerTile(3096, 3357, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getDestination().getX() < 3097 &&
                    destinationDetails.getAssumed().equals(Ctx.ctx.tiles.createWalkerTile(3097, 3359, 0));
            }
        }),
        ARDY_DOOR_LOCK_SIDE("Door", "Pick-lock", Ctx.ctx.tiles.createWalkerTile(2565, 3356, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return Ctx.getMyLocation().getX() >= 2565 &&
                    Ctx.getMyLocation().distanceTo(Ctx.ctx.tiles.createWalkerTile(2565, 3356, 0)) < 3;
            }
        }),
        ARDY_DOOR_UNLOCKED_SIDE("Door", "Open", Ctx.ctx.tiles.createWalkerTile(2565, 3356, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return Ctx.getMyLocation().getX() < 2565 &&
                    Ctx.getMyLocation().distanceTo(Ctx.ctx.tiles.createWalkerTile(2565, 3356, 0)) < 3;
            }
        }),
        YANILLE_DOOR_LOCK_SIDE("Door", "Pick-lock", Ctx.ctx.tiles.createWalkerTile(2601, 9482, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return Ctx.getMyLocation().getY() <= 9481 &&
                    Ctx.getMyLocation().distanceTo(Ctx.ctx.tiles.createWalkerTile(2601, 9482, 0)) < 3;
            }
        }),
        YANILLE_DOOR_UNLOCKED_SIDE("Door", "Open", Ctx.ctx.tiles.createWalkerTile(2601, 9482, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return Ctx.getMyLocation().getY() > 9481 &&
                    Ctx.getMyLocation().distanceTo(Ctx.ctx.tiles.createWalkerTile(2601, 9482, 0)) < 3;
            }
        }),
        EDGEVILLE_UNDERWALL_TUNNEL("Underwall tunnel", "Climb-into", Ctx.ctx.tiles.createWalkerTile(3138, 3516, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getAssumed().equals(Ctx.ctx.tiles.createWalkerTile(3138, 3516, 0));
            }
        }),
        VARROCK_UNDERWALL_TUNNEL("Underwall tunnel", "Climb-into", Ctx.ctx.tiles.createWalkerTile(3141, 3513, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getAssumed().equals(Ctx.ctx.tiles.createWalkerTile(3141, 3513, 0 ));
            }
        }),
        GAMES_ROOM_STAIRS("Stairs", "Climb-down", Ctx.ctx.tiles.createWalkerTile(2899, 3565, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getDestination().toWalkerTile().equals(Ctx.ctx.tiles.createWalkerTile(2899, 3565, 0)) &&
                    destinationDetails.getAssumed().equals(Ctx.ctx.tiles.createWalkerTile(2205, 4934, 1));
            }
        });

        private String name, action;
        private WalkerTile location;
        private SpecialCondition specialCondition;

        SpecialObject(String name, String action, WalkerTile location, SpecialCondition specialCondition) {
            this.name = name;
            this.action = action;
            this.location = location;
            this.specialCondition = specialCondition;
        }

        public String getName() {
            return name;
        }

        public String getAction() {
            return action;
        }

        public WalkerTile getLocation() {
            return location;
        }

        public boolean isSpecialCondition(PathAnalyzer.DestinationDetails destinationDetails){
            return specialCondition.isSpecialLocation(destinationDetails);
        }

        public static SpecialObject getValidSpecialObjects(PathAnalyzer.DestinationDetails destinationDetails){
            for (SpecialObject object : values()){
                if (object.isSpecialCondition(destinationDetails)){
                    return object;
                }
            }
            return null;
        }
    }

    private abstract static class SpecialCondition {
        abstract boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails);
    }

    public static boolean handle(PathAnalyzer.DestinationDetails destinationDetails,
                                 List<WalkerTile> path) {

        log.info("PathObjectHandler.handle() destinationDetails: " + destinationDetails.toString());
        DaxTile start = destinationDetails.getDestination();
        DaxTile end = destinationDetails.getNextTile();

        RSObject[] interactiveObjects = null;

        String action = null;
        SpecialObject specialObject = SpecialObject.getValidSpecialObjects(destinationDetails);
        if (specialObject == null) {
            if ((interactiveObjects = getInteractiveObjects(start.getX(), start.getY(), start.getZ(), destinationDetails)).length < 1 && end != null) {
                interactiveObjects = getInteractiveObjects(end.getX(), end.getY(), end.getZ(), destinationDetails);
            }
        } else {
            action = specialObject.getAction();
            Filter<RSObject> specialObjectFilter = Filters.Objects.nameEquals(specialObject.getName())
                    .combine(Filters.Objects.actionsContains(specialObject.getAction()), true)
                    .combine(Filters.Objects.inArea(new RSArea(specialObject.getLocation() != null ? specialObject.getLocation() : destinationDetails.getAssumed(), 1)), true);
            interactiveObjects = new RSObject[]{Ctx.ctx.objects.getNearest(15, specialObjectFilter)};
        }

        if (interactiveObjects.length == 0) {
            return false;
        }

        StringBuilder stringBuilder = new StringBuilder("Sort Order: ");
        Arrays.stream(interactiveObjects).forEach(rsObject -> stringBuilder.append(rsObject.getDef().getName()).append(" ").append(
                Arrays.asList(rsObject.getDef().getActions())).append(", "));
        log.info(stringBuilder.toString());

        return handle(path, interactiveObjects[0], destinationDetails, action, specialObject);
    }

    private static boolean handle(List<WalkerTile> path,
                                  RSObject object,
                                  PathAnalyzer.DestinationDetails destinationDetails,
                                  String action,
                                  SpecialObject specialObject) {
        PathAnalyzer.DestinationDetails current = PathAnalyzer.furthestReachableTile(path);

        if (current == null) {
            return false;
        }

        DaxTile currentFurthest = current.getDestination();
        if (!Ctx.ctx.players.getMyPlayer().isLocalPlayerMoving() &&
            (!object.isOnScreen() || !object.isClickable())) {

            if (!AccurateMouse.clickMinimap(destinationDetails.getDestination().toWalkerTile())) {

                return false;
            }
        }

        if (WaitFor.condition(StdRandom.uniform(5000, 8000), () -> object.isOnScreen() && object.isClickable() ? WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE) != WaitFor.Return.SUCCESS) {
            return false;
        }

        boolean successfulClick = false;

        if (specialObject != null) {
            log.info("Detected Special Object: " + specialObject);
            switch (specialObject){
                case ARDY_DOOR_LOCK_SIDE:
                case YANILLE_DOOR_LOCK_SIDE:
                    for (int i = 0; i < StdRandom.uniform(15, 25); i++) {
                        if (!clickOnObject(object, new String[]{specialObject.getAction()})){
                            continue;
                        }
                        if (new WalkerTile(new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation())).distanceTo(specialObject.getLocation()) > 1){
                            WaitFor.condition(StdRandom.uniform(3000, 4000), () -> new WalkerTile(new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation())).distanceTo(specialObject.getLocation()) <= 1 ? WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE);
                        }
                        if (new WalkerTile(new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation())).equals(Ctx.ctx.tiles.createWalkerTile(2564, 3356, 0))){
                            successfulClick = true;
                            break;
                        }
                    }
                    break;
                case VARROCK_UNDERWALL_TUNNEL:
                    if(!clickOnObject(object,specialObject.getAction())){
                        return false;
                    }
                    successfulClick = true;
                    WaitFor.condition(10000, () ->
                            SpecialObject.EDGEVILLE_UNDERWALL_TUNNEL.getLocation().equals(new WalkerTile(new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation()))) ?
                                    WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE);
                    break;
                case EDGEVILLE_UNDERWALL_TUNNEL:
                    if(!clickOnObject(object,specialObject.getAction())){
                        return false;
                    }
                    successfulClick = true;
                    WaitFor.condition(10000, () ->
                            SpecialObject.VARROCK_UNDERWALL_TUNNEL.getLocation().equals(new WalkerTile(new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation()))) ?
                                    WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE);
                    break;
            }
        }

        if (!successfulClick){
            String[] validOptions = action != null ? new String[]{action} : getViableOption(
                    Arrays.stream(object.getDef().getActions()).filter(getInstance().sortedOptions::contains).collect(
                            Collectors.toList()), destinationDetails);
            if (!clickOnObject(object, validOptions)) {
                return false;
            }
        }

        WaitFor.condition(StdRandom.uniform(8500, 11000), () -> {
            var destinationDetails1 = PathAnalyzer.furthestReachableTile(path);
            if (destinationDetails1 != null) {
                if (!destinationDetails1.getDestination().equals(currentFurthest)) {
                    return WaitFor.Return.SUCCESS;
                }
            }

            if (current.getNextTile() != null) {
                var hoverDetails = PathAnalyzer.furthestReachableTile(path, current.getNextTile());
                var myLoc = Ctx.getMyLocation();
                if (hoverDetails != null &&
                    hoverDetails.getDestination() != null &&
                    hoverDetails.getDestination().toWalkerTile().distanceTo(myLoc) > 7 &&
                    myLoc.distanceTo(object) <= 2) {
                    AccurateMouse.hoverMinimap(hoverDetails.getDestination());
                }
            }

            return WaitFor.Return.IGNORE;
        });

        return true;
    }

    public static RSObject[] getInteractiveObjects(int x, int y, int z, PathAnalyzer.DestinationDetails destinationDetails){
        RSObject[] objects = Ctx.ctx.objects.getAll(interactiveObjectFilter(x, y, z, destinationDetails));
        final WalkerTile base = Ctx.ctx.tiles.createWalkerTile(x, y, z);
        Arrays.sort(objects, (o1, o2) -> {
            int c = Integer.compare(o1.getLocation().distanceTo(base), o2.getLocation().distanceTo(base));
            int assumedZ = destinationDetails.getAssumedZ(), destinationZ = destinationDetails.getDestination().getZ();
            List<String> actions1 = Arrays.asList(o1.getDef().getActions());
            List<String> actions2 = Arrays.asList(o2.getDef().getActions());

            if (assumedZ > destinationZ){
                if (actions1.contains("Climb-up")){
                    return -1;
                }
                if (actions2.contains("Climb-up")){
                    return 1;
                }
            } else if (assumedZ < destinationZ){
                if (actions1.contains("Climb-down")){
                    return -1;
                }
                if (actions2.contains("Climb-down")){
                    return 1;
                }
            } else if(destinationDetails.getAssumed().distanceTo(destinationDetails.getDestination().toWalkerTile()) > 20){
                if(actions1.contains("Climb-up") || actions1.contains("Climb-down")){
                    return -1;
                } else if(actions2.contains("Climb-up") || actions2.contains("Climb-down")){
                    return 1;
                }
            } else if(actions1.contains("Climb-up") || actions1.contains("Climb-down")){
                return 1;
            } else if(actions2.contains("Climb-up") || actions2.contains("Climb-down")){
                return -1;
            }
            return c;
        });
        StringBuilder a = new StringBuilder("Detected: ");
        Arrays.stream(objects).forEach(object -> a.append(object.getDef().getName()).append(" "));
        log.info(a.toString());



        return objects;
    }

    /**
     * Filter that accepts only interactive objects to progress in path.
     *
     * @param x
     * @param y
     * @param z
     * @param destinationDetails context where destination is at
     * @return
     */
    private static Filter<RSObject> interactiveObjectFilter(int x, int y, int z, PathAnalyzer.DestinationDetails destinationDetails){
        final WalkerTile position = Ctx.ctx.tiles.createWalkerTile(x, y, z);
        return new Filter<RSObject>() {
            @Override
            public boolean test(RSObject rsObject) {
                ObjectDefinition def = rsObject.getDef();
                if (def == null){
                    return false;
                }
                String name = def.getName();
                if (getInstance().sortedBlackList.contains(name)) {
                    return false;
                }
                if (RSObjectHelper.getActionsList(rsObject).stream().anyMatch(s -> getInstance().sortedBlackListOptions.contains(s))){
                    return false;
                }
                if (rsObject.getLocation().distanceTo(destinationDetails.getDestination().toWalkerTile()) > 5) {
                    return false;
                }
                if (Arrays.stream(rsObject.getArea().getTileArray()).noneMatch(rsTile -> Ctx.ctx.tiles.createWalkerTile(rsTile).distanceTo(position) <= 2)) {
                    return false;
                }
                List<String> options = Arrays.asList(def.getActions());
                return options.stream().anyMatch(getInstance().sortedOptions::contains);
            }
        };
    }

    private static String[] getViableOption(Collection<String> collection, PathAnalyzer.DestinationDetails destinationDetails){
        Set<String> set = new HashSet<>(collection);
        if (set.retainAll(getInstance().sortedHighPriorityOptions) && set.size() > 0){
            return set.toArray(new String[set.size()]);
        }
        if (destinationDetails.getAssumedZ() > destinationDetails.getDestination().getZ()){
            if (collection.contains("Climb-up")){
                return new String[]{"Climb-up"};
            }
        }
        if (destinationDetails.getAssumedZ() < destinationDetails.getDestination().getZ()){
            if (collection.contains("Climb-down")){
                return new String[]{"Climb-down"};
            }
        }
        if (destinationDetails.getAssumedY() > 5000 && destinationDetails.getDestination().getZ() == 0 && destinationDetails.getAssumedZ() == 0){
            if (collection.contains("Climb-down")){
                return new String[]{"Climb-down"};
            }
        }
        String[] options = new String[collection.size()];
        collection.toArray(options);
        return options;
    }

    private static boolean clickOnObject(RSObject object, String... options){
        boolean result;

        if (isClosedTrapDoor(object, options)){
            result = handleTrapDoor(object);
        } else {
            result = InteractionHelper.click(object, options);
            log.info("Interacting with (" + RSObjectHelper.getName(object) + ") at " + object.getLocation() + " with options: " + Arrays.toString(options) + " " + (result ? "SUCCESS" : "FAIL"));
            WaitFor.milliseconds(250,800);
        }

        return result;
    }

    private static boolean isClosedTrapDoor(RSObject object, String[] options){
        return  (object.getDef().getName().equals("Trapdoor") && Arrays.asList(options).contains("Open"));
    }

    private static boolean handleTrapDoor(RSObject object){
        if (getActions(object).contains("Open")){
            if (!InteractionHelper.click(object, "Open", () -> {
                RSObject[] objects = new RSObject[]{Ctx.ctx.objects.getNearest(Filters.Objects.actionsContains("Climb-down").combine(Filters.Objects.inArea(new RSArea(object, 2)), true))};
                if (objects.length > 0 && getActions(objects[0]).contains("Climb-down")){
                    return WaitFor.Return.SUCCESS;
                }
                return WaitFor.Return.IGNORE;
            })){
                return false;
            } else {
                RSObject[] objects = new RSObject[] {Ctx.ctx.objects.getNearest(Filters.Objects.actionsContains("Climb-down").combine(Filters.Objects.inArea(new RSArea(object, 2)), true))};
                return objects.length > 0 && handleTrapDoor(objects[0]);
            }
        }
        log.info("Interacting with (" + object.getDef().getName() + ") at " + object.getLocation() + " with option: Climb-down");
        return InteractionHelper.click(object, "Climb-down");
    }

    public static List<String> getActions(RSObject object){
        List<String> list = new ArrayList<>();
        if (object == null){
            return list;
        }
        ObjectDefinition objectDefinition = object.getDef();
        if (objectDefinition == null){
            return list;
        }
        String[] actions = objectDefinition.getActions();
        if (actions == null){
            return list;
        }
        return Arrays.asList(actions);
    }

}
