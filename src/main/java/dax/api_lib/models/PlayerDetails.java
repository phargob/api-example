package dax.api_lib.models;

import dax.Ctx;

import net.runelite.api.Skill;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class PlayerDetails {

    // XXX this function should be move out of here?
    public static PlayerDetails generate() {

        List<IntPair> inventory = new ArrayList<IntPair>();
        // so we can at least use the toll gate in lumbridge
        for (var i: Ctx.ctx.inventory.getItems()) {
            if (i.getName().toLowerCase().contains("coins")) {
                var ip = new IntPair(i.getID(),
                                     Math.min(i.getStackSize(), 10));
                inventory.add(ip);
            }
        }

        // XXX ctx.equipment is currently broken anyway
        List<IntPair> equipment = new ArrayList<IntPair>();
        // List<IntPair> equipment = Arrays.stream(Ctx.ctx.equipment.getItems())
        //         .map(rsItem -> new IntPair(rsItem.getID(), rsItem.getStackSize())).collect(Collectors.toList());

        // would be good to send this, but i need to figure all there number default values
        List<IntPair> settings = Stream.of(10, 11, 17, 32, 63, 68, 71, 101, 111, 116, 131, 144, 145, 150, 165, 176,
            179, 273, 299, 302, 307, 314, 335, 347, 351, 365, 371, 399, 425, 437, 440, 482, 622, 655, 671, 794, 810,
            869, 896, 964, 1630, 1671, 1672)
                                       .map(value -> new IntPair(value, Ctx.ctx.clientLocalStorage.getVarpValueAt(value))).distinct().collect(Collectors.toList());

        List<IntPair> varbit = Arrays.stream(new int[]{
            192,
            199,
            357,
            2310,
            2328,
            3741,
            4538,
            4566,
            4885,
            4895,
            4897,
            5087,
            5088,
            5089,
            5090,
            5810
        })
                .mapToObj(value -> new IntPair(value, Ctx.ctx.proxy.getVarbitValue(value))).distinct().collect(
                        Collectors.toList());

        return new PlayerDetails(
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.ATTACK.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.DEFENCE.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.STRENGTH.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.HITPOINTS.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.RANGED.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.PRAYER.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.MAGIC.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.COOKING.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.WOODCUTTING.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.FLETCHING.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.FISHING.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.FIREMAKING.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.CRAFTING.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.SMITHING.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.MINING.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.HERBLORE.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.AGILITY.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.THIEVING.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.SLAYER.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.FARMING.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.RUNECRAFT.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.HUNTER.getName().toUpperCase())),
                10, // Ctx.ctx.skills.getRealLevel(Skills.getIndex(Skill.CONSTRUCTION.getName().toUpperCase())),
                settings,
                varbit,
                Ctx.ctx.worldHopper.isCurrentWorldMembers(),
                equipment,
                inventory
        );
    }



    private int attack;


    private int defence;


    private int strength;


    private int hitpoints;


    private int ranged;


    private int prayer;


    private int magic;


    private int cooking;


    private int woodcutting;


    private int fletching;


    private int fishing;


    private int firemaking;


    private int crafting;


    private int smithing;


    private int mining;


    private int herblore;


    private int agility;


    private int thieving;


    private int slayer;


    private int farming;


    private int runecrafting;


    private int hunter;


    private int construction;


    private List<IntPair> setting;


    private List<IntPair> varbit;


    private boolean member;


    private List<IntPair> equipment;


    private List<IntPair> inventory;

    public PlayerDetails() {

    }

    public PlayerDetails(int attack, int defence, int strength, int hitpoints, int ranged, int prayer, int magic, int cooking, int woodcutting, int fletching, int fishing, int firemaking, int crafting, int smithing, int mining, int herblore, int agility, int thieving, int slayer, int farming, int runecrafting, int hunter, int construction, List<IntPair> setting, List<IntPair> varbit, boolean member, List<IntPair> equipment, List<IntPair> inventory) {
        this.attack = attack;
        this.defence = defence;
        this.strength = strength;
        this.hitpoints = hitpoints;
        this.ranged = ranged;
        this.prayer = prayer;
        this.magic = magic;
        this.cooking = cooking;
        this.woodcutting = woodcutting;
        this.fletching = fletching;
        this.fishing = fishing;
        this.firemaking = firemaking;
        this.crafting = crafting;
        this.smithing = smithing;
        this.mining = mining;
        this.herblore = herblore;
        this.agility = agility;
        this.thieving = thieving;
        this.slayer = slayer;
        this.farming = farming;
        this.runecrafting = runecrafting;
        this.hunter = hunter;
        this.construction = construction;
        this.setting = setting;
        this.varbit = varbit;
        this.member = member;
        this.equipment = equipment;
        this.inventory = inventory;
    }

    public int getAttack() {
        return attack;
    }

    public int getDefence() {
        return defence;
    }

    public int getStrength() {
        return strength;
    }

    public int getHitpoints() {
        return hitpoints;
    }

    public int getRanged() {
        return ranged;
    }

    public int getPrayer() {
        return prayer;
    }

    public int getMagic() {
        return magic;
    }

    public int getCooking() {
        return cooking;
    }

    public int getWoodcutting() {
        return woodcutting;
    }

    public int getFletching() {
        return fletching;
    }

    public int getFishing() {
        return fishing;
    }

    public int getFiremaking() {
        return firemaking;
    }

    public int getCrafting() {
        return crafting;
    }

    public int getSmithing() {
        return smithing;
    }

    public int getMining() {
        return mining;
    }

    public int getHerblore() {
        return herblore;
    }

    public int getAgility() {
        return agility;
    }

    public int getThieving() {
        return thieving;
    }

    public int getSlayer() {
        return slayer;
    }

    public int getFarming() {
        return farming;
    }

    public int getRunecrafting() {
        return runecrafting;
    }

    public int getHunter() {
        return hunter;
    }

    public int getConstruction() {
        return construction;
    }

    public List<IntPair> getSetting() {
        return setting;
    }

    public List<IntPair> getVarbit() {
        return varbit;
    }

    public boolean isMember() {
        return member;
    }

    public List<IntPair> getEquipment() {
        return equipment;
    }

    public List<IntPair> getInventory() {
        return inventory;
    }

    public JsonElement toJson() {
        return new Gson().toJsonTree(this);
    }

}
