package rsb.methods;

import net.runelite.api.Actor;
import net.runelite.api.Skill;
import rsb.globval.VarpIndices;
import rsb.globval.GlobalWidgetInfo;
import rsb.globval.VarpValues;
import rsb.globval.enums.InterfaceTab;
import rsb.wrappers.*;

import java.util.Arrays;

/**
 * Combat related operations.
 */
public class Combat {

    private MethodContext ctx;
    public Combat(MethodContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Turns auto-retaliate on or off in the combat tab.
     *
     * @param enable <code>true</code> to enable; <code>false</code> to disable.
     */
    public void setAutoRetaliate(final boolean enable) {
        final RSWidget autoRetaliate = ctx.interfaces.getComponent(GlobalWidgetInfo.COMBAT_AUTO_RETALIATE);
        if (isAutoRetaliateEnabled() != enable) {
            if (ctx.game.getCurrentTab() != InterfaceTab.COMBAT) {
                ctx.game.openTab(InterfaceTab.COMBAT);
            }
            if (ctx.game.getCurrentTab() == InterfaceTab.COMBAT && autoRetaliate != null) {
                autoRetaliate.doClick();
            }
        }
    }

    /**
     * Returns whether or not the auto-retaliate option is enabled.
     *
     * @return <code>true</code> if retaliate is enabled; otherwise <code>false</code>.
     */
    public boolean isAutoRetaliateEnabled() {
        return ctx.clientLocalStorage.getVarpValueAt(VarpIndices.TOGGLE_AUTO_RETALIATE)
                == VarpValues.AUTO_RETALIATE_ENABLED.getValue();
    }

    /**
     * Gets the attack mode.
     *
     * @return The current fight mode setting.
     */
    public int getFightMode() {
        return ctx.clientLocalStorage.getVarpValueAt(VarpIndices.COMBAT_STYLE);
    }

    /**
     * Sets the attack mode.
     *
     * @param fightMode The fight mode to set it to. From 0-3 corresponding to the 4
     *                  attacking modes; Else if there is only 3 attacking modes then,
     *                  from 0-2 corresponding to the 3 attacking modes
     * @return <code>true</code> if the interface was clicked; otherwise
     *         <code>false</code>.
     * @see #getFightMode()
     */
    public boolean setFightMode(int fightMode) {
        if (fightMode != getFightMode()) {
            ctx.game.openTab(InterfaceTab.COMBAT);
            if (fightMode == VarpValues.COMBAT_STYLE_FIRST.getValue()) {
                return ctx.interfaces.getComponent(GlobalWidgetInfo.COMBAT_STYLE_ONE).doClick();
            } else if (fightMode == VarpValues.COMBAT_STYLE_SECOND.getValue()) {
                return ctx.interfaces.getComponent(GlobalWidgetInfo.COMBAT_STYLE_TWO).doClick();
            } else if (fightMode == VarpValues.COMBAT_STYLE_THIRD.getValue()) {
                return ctx.interfaces.getComponent(GlobalWidgetInfo.COMBAT_STYLE_THREE).doClick();
            } else if (fightMode == VarpValues.COMBAT_STYLE_FOURTH.getValue()) {
                return ctx.interfaces.getComponent(GlobalWidgetInfo.COMBAT_STYLE_FOUR).doClick();
            }
        }
        return false;
    }

    /**
     * Gets the current Wilderness Level.
     *
     * @return The current wilderness level otherwise, 0.
     */
    public int getWildernessLevel() {
        RSWidget widget = ctx.interfaces.getComponent(GlobalWidgetInfo.PVP_WILDERNESS_LEVEL);
        return (widget.isValid() && widget.isVisible())
                ? Integer.parseInt(widget.getText().replace("Level: ", "").trim())
                : 0;
    }

    /**
     * Gets the current player's life points.
     *
     * @return The current life points if the interface is valid; otherwise 0.
     */
    public int getLifePoints() {
        try {
            return Integer.parseInt(ctx.interfaces.getComponent(GlobalWidgetInfo.MINIMAP_HEALTH_ORB_TEXT)
                    .getText());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * Returns whether we're poisoned.
     *
     * @return <code>true</code> if poisoned; otherwise <code>false</code>.
     */
    public boolean isPoisoned() {
        return 0 < ctx.clientLocalStorage.getVarpValueAt(VarpIndices.POISON)
                && ctx.clientLocalStorage.getVarpValueAt(VarpIndices.POISON) < 1000000;
    }

    /**
     * Returns the damage we're taking from poison
     *
     * @return the poison damage if poisoned; otherwise 0;
     */
    public double getPoisonDamage() {
        if (isPoisoned())
            return Math.ceil(ctx.clientLocalStorage.getVarpValueAt(VarpIndices.POISON) / 5.0f);
        return 0;
    }

    /**
     * Returns whether we're immune to poison.
     *
     * @return <code>true</code> if immune; otherwise <code>false</code>.
     */
    public boolean isPoisonImmune() {
        return ctx.clientLocalStorage.getVarpValueAt(VarpIndices.POISON) == -1;
    }

    /**
     * Returns whether we're envenomed
     *
     * @return  <code>true</code> if the local player is envenomed; otherwise <code>false</code>
     */
    public boolean isEnvenomed() {
        return ctx.clientLocalStorage.getVarpValueAt(VarpIndices.POISON) >= 1000000;
    }

    /**
     * Returns the damage we're taking from venom
     *
     * @return the venom damage if envenomed; otherwise 0;
     */
    public int getVenomDamage() {
        if (isEnvenomed())
            return (ctx.clientLocalStorage.getVarpValueAt(VarpIndices.POISON) - 999997) * 2;
        return 0;
    }

    /**
     * Returns whether the special attack option is enabled.
     *
     * @return <code>true</code> if special attack is enabled; otherwise <code>false</code>.
     */
    public boolean isSpecialAttackEnabled() {
        return ctx.clientLocalStorage.getVarpValueAt(VarpIndices.TOGGLE_SPECIAL_ATTACK)
                == VarpValues.SPECIAL_ATTACK_ENABLED.getValue();
    }

    /**
     * Returns current special attack energy in percent.
     *
     * @return 0 - 100
     */
    public int getSpecialAttackEnergy() {
        return ctx.clientLocalStorage.getVarpValueAt(VarpIndices.SPECIAL_ATTACK_ENERGY);
    }

    /**
     * Gets the special bar energy amount.
     *
     * @return The current spec energy.
     */
    public int getSpecialBarEnergy() {
        try {
            return Integer.parseInt(ctx.interfaces.getComponent(GlobalWidgetInfo.MINIMAP_SPEC_ORB_TEXT)
                    .getText()
                    .trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * Gets the current player's prayer points.
     *
     * @return The current prayer points if the interface is valid; otherwise 0.
     */
    public int getPrayerPoints() {
        try {
            return Integer.parseInt(ctx.interfaces.getComponent(GlobalWidgetInfo.MINIMAP_QUICK_PRAYER_ORB_TEXT)
                    .getText()
                    .trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * Gets the current player's health as a percentage of full health.
     *
     * @return The current percentage health remaining.
     */
    //public int getHealth() {
    //  return ((getLifePoints() * 100) / ctx.skills.getRealLevel(Skill.HITPOINTS.ordinal()));
    //}

    /**
     * Checks if your character is interacting with an Npc.
     *
     * @param npc The Npc we want to fight.
     * @return <code>true</code> if interacting; otherwise <code>false</code>.
     */
    public boolean isAttacking(final RSNPC npc) {
        // Helpful for new scripters confused by the function of isInCombat()
        Actor interact = ctx.players.getMyPlayer().getInteracting();
        return interact != null && interact.equals(npc.getAccessor());
    }

    public boolean isHPZero(final RSNPC npc) {
        return npc.getHPPercent() == 0;
    }

    public boolean isAlive(final RSNPC npc) {
        return npc.getHPPercent() > 0 || (isHPZero(npc) && isFinishable(npc));
    }

    /**
     * Checks whether the desired Npc is dead.
     *
     * @param npc The RSNPC to check.
     * @return <code>true</code> if the Npc is dead or dying; otherwise
     *         <code>false</code>.
     */
    //TODO: this will need investigation as there will always be special cases
    public boolean isDead(final RSNPC npc) {
        return isHPZero(npc) && !isFinishable(npc);
    }

    public boolean isBelowHP(final RSNPC npc, final double healthPercentage) {
        return npc.getHPPercent() < healthPercentage;
    }

    public boolean isBelowOrAtHP(final RSNPC npc, final double healthPercentage) {
        return npc.getHPPercent() <= healthPercentage;
    }

    public boolean isAboveHP(final RSNPC npc, final double healthPercentage) {
        return npc.getHPPercent() > healthPercentage;
    }

    public boolean isAboveOrAtHP(final RSNPC npc, final double healthPercentage) {
        return npc.getHPPercent() >= healthPercentage;
    }

    public boolean isLowHealth(final RSNPC npc) {
        return isAboveHP(npc, 0) && isBelowHP(npc, 15);
    }

    public boolean isFinishable(final RSNPC npc) {
        String mobName = npc.getName();
        if (mobName == null || mobName.equals(""))
            return false;
        String[] finishableMobs = {
                "Rockslug", "Desert Lizard", "Small Lizard", "Lizard", "Mutated Zygomite", "Ancient Zygomite", "Gargoyle"
        };
        return Arrays.asList(finishableMobs).contains(mobName);
    }

    public double getFinishableHP(double maxHP, double finishHP) {
        return Math.floor(finishHP/(maxHP/100));
    }

    public boolean canBeFinished(final RSNPC npc) {
        String mobName = npc.getName();
        if (mobName == null || mobName.equals(""))
            return false;
        switch (mobName) {
            case "Ancient Zygomite" -> isBelowOrAtHP(npc, getFinishableHP(150, 7));
            case "Gargoyle" -> isBelowOrAtHP(npc, getFinishableHP(105, 8));
            case "Zygomite" -> isBelowOrAtHP(npc, getFinishableHP(75, 7));
            case "Lizard" -> isBelowOrAtHP(npc, getFinishableHP(40, 4));
            case "Rockslug" -> isBelowOrAtHP(npc, getFinishableHP(27, 5));
            case "Desert Lizard" -> isBelowOrAtHP(npc, getFinishableHP(25, 4));
            case "Small Lizard" -> isBelowOrAtHP(npc, getFinishableHP(15, 4));
            default -> throw new IllegalStateException("Unexpected value: " + mobName);
        }
        return false;
    }
}
