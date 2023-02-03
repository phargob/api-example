package rsb.methods;

import rsb.globval.WidgetIndices;
import rsb.globval.enums.Skill;
import rsb.wrappers.RSItem;
import rsb.wrappers.RSNPC;
import rsb.wrappers.RSWidget;

import java.util.function.BooleanSupplier;

public class RandomEvents {
    private MethodContext ctx;
    RandomEvents(final MethodContext ctx) {
        this.ctx = ctx;
    }

    public boolean isGenieNearby() {
        RSNPC genie = ctx.npcs.getNearest("Genie");
        return genie != null && ctx.calc.distanceTo(genie) <= 4;
    }

    public void solveGenieLamp(Skill skillToLevel) {
        RSItem genieLamp = ctx.inventory.getFirstWithAction("Rub");
        if (genieLamp != null) {
            genieLamp.doAction("Rub");

            // XXX seems very dangerous - could loop forever
            while (true) {
                if (ctx.proxy.getWidget(WidgetIndices.GenieLampWindow.GROUP_INDEX,
                                            WidgetIndices.GenieLampWindow.PARENT_CONTAINER) != null) {
                    break;
                }

                ctx.sleepRandom(300, 600);
            }

            switch (skillToLevel) {
                case ATTACK:
                    RSWidget skillAttack = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.ATTACK_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillAttack != null)
                        skillAttack.doClick();
                case STRENGTH:
                    RSWidget skillStrength = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.STRENGHT_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillStrength != null)
                        skillStrength.doClick();
                case DEFENCE:
                    RSWidget skillDefence = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.DEFENSE_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillDefence != null)
                        skillDefence.doClick();
                case RANGED:
                    RSWidget skillRanged = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.RANGED_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillRanged != null)
                        skillRanged.doClick();
                case PRAYER:
                    RSWidget skillPrayer = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.PRAYER_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillPrayer != null)
                        skillPrayer.doClick();
                case MAGIC:
                    RSWidget skillMagic = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.MAGIC_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillMagic != null)
                        skillMagic.doClick();
                case RUNECRAFT:
                    RSWidget skillRunecraft = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.RUNECRAFTING_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillRunecraft != null)
                        skillRunecraft.doClick();
                case CONSTRUCTION:
                    RSWidget skillConstruction = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.CONSTRUCTION_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillConstruction != null)
                        skillConstruction.doClick();
                case HITPOINTS:
                    RSWidget skillHitpoints = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.HITPOINTS_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillHitpoints != null)
                        skillHitpoints.doClick();
                case AGILITY:
                    RSWidget skillAgility = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.AGILITY_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillAgility != null)
                        skillAgility.doClick();
                case HERBLORE:
                    RSWidget skillHerblore = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.HERBOLORE_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillHerblore != null)
                        skillHerblore.doClick();
                case THIEVING:
                    RSWidget skillThieving = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.THIEVING_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillThieving != null)
                        skillThieving.doClick();
                case CRAFTING:
                    RSWidget skillCrafting = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.CRAFTING_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillCrafting != null)
                        skillCrafting.doClick();
                case FLETCHING:
                    RSWidget skillFletching = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.FLETCHING_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillFletching != null)
                        skillFletching.doClick();
                case SLAYER:
                    RSWidget skillSlayer = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.SLAYER_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillSlayer != null)
                        skillSlayer.doClick();
                case HUNTER:
                    RSWidget skillHunter = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.HUNTER_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillHunter != null)
                        skillHunter.doClick();
                case MINING:
                    RSWidget skillMining = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.MINING_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillMining != null)
                        skillMining.doClick();
                case SMITHING:
                    RSWidget skillSmithing = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.SMITHING_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillSmithing != null)
                        skillSmithing.doClick();
                case FISHING:
                    RSWidget skillFishing = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.FISHING_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillFishing != null)
                        skillFishing.doClick();
                case COOKING:
                    RSWidget skillCooking = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.COOKING_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillCooking != null)
                        skillCooking.doClick();
                case FIREMAKING:
                    RSWidget skillFiremaking = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.FIREMAKING_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillFiremaking != null)
                        skillFiremaking.doClick();
                case WOODCUTTING:
                    RSWidget skillWoodcutting = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.WOODCUTTING_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillWoodcutting != null)
                        skillWoodcutting.doClick();
                case FARMING:
                    RSWidget skillFarming = ctx.interfaces.getComponent(
                            WidgetIndices.GenieLampWindow.GROUP_INDEX,
                            WidgetIndices.GenieLampWindow.FARMING_DYNAMIC_CONTAINER)
                            .getDynamicComponent(9);
                    if (skillFarming != null)
                        skillFarming.doClick();
            }
        }
    }
}
