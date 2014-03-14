package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.plugins.PhaseCruiseTempAI;
import data.scripts.plugins.SunUtils;
import java.util.Map;
import java.util.WeakHashMap;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

public class GravimetricSensors extends BaseHullMod {
	public static final float SIGHT_RADIUS_BONUS = 20f;
	public static final float TRACKING_PENALTY = -50f;
    private static final float REFRESH_FREQUENCY = 0.5f;
    private Map timeOfNextRefresh = new WeakHashMap();

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {
        // ICE armor is repaired instantly in campaign
//        float hull = member.getStatus().getHullFraction();
//
//        if(hull < 1) {
//            member.getStatus().repairFully();
//            member.getStatus().applyHullFractionDamage(1 - hull);
//        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);

        if((Float)timeOfNextRefresh.get(ship) < Global.getCombatEngine().getTotalElapsedTime(false)) {
            timeOfNextRefresh.put(ship, (Float)timeOfNextRefresh.get(ship)
                    + REFRESH_FREQUENCY * (float)Math.random() * 2);
        } else return;

        ShipSystemAPI cloak = ship.getPhaseCloak();
        float speed = (float)Math.sqrt(Math.pow(ship.getVelocity().x, 2)
                + Math.pow(ship.getVelocity().y, 2));
        AssignmentInfo task = Global.getCombatEngine().getFleetManager(ship.getOwner()).getAssignmentFor(ship);

        if(cloak != null && !cloak.isActive() && ship.getShipAI() != null
                && ship.getAngularVelocity() < 1f
                && !ship.getTravelDrive().isActive()
                && ship.getFluxTracker().getFluxLevel() == 0
                && speed >= ship.getMutableStats().getMaxSpeed().getModifiedValue() - 2
                && AIUtils.getNearbyEnemies(ship, 2000).isEmpty()
                && (task == null || task.getTarget() == null || MathUtils.getDistance(ship, task.getTarget().getLocation()) > 800)
                ) {

            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);

            //if(cloak.getId().contains("sun_ice_phasecloak"))
                ship.setShipAI(new PhaseCruiseTempAI(ship));
        }
    }

    @Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        timeOfNextRefresh.put(stats.getEntity(), 0.0f);
        
		stats.getBallisticWeaponRangeBonus().modifyFlat(id, 300);
		stats.getBallisticWeaponRangeBonus().modifyPercent(id, -50f);
		
//		stats.getEnergyWeaponRangeBonus().modifyFlat(id, 300);
//		stats.getEnergyWeaponRangeBonus().modifyPercent(id, -50f);

        //stats.getWeaponTurnRateBonus().modifyPercent(id, TRACKING_PENALTY);

        stats.getSightRadiusMod().modifyPercent(id, SIGHT_RADIUS_BONUS);


        
//        stats.getArmorDamageTakenMult().modifyMult(id, 0.01f);
//
//            stats.getEnergyWeaponDamageMult().modifyMult(id, 100);
//
//        stats.getHighExplosiveDamageTakenMult().modifyMult(id, 100);

//        stats.getEnergyDamageTakenMult().modifyMult(id, 100);
//        stats.getFragmentationDamageTakenMult().modifyMult(id, 100);
//        stats.getKineticDamageTakenMult().modifyMult(id, 100);
//
//        stats.getProjectileDamageTakenMult().modifyMult(id, 100);
//        stats.getMissileDamageTakenMult().modifyMult(id, 100);
//        stats.getBeamDamageTakenMult().modifyMult(id, 100);
	}

    @Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		//if (index == 0) return "" + 0.5f;
		return null;
	}
	
    @Override
    public boolean isApplicableToShip(ShipAPI ship)
    {
        return false;
        // Allows any ship with a ICE hull id
//        return ( ship.getHullSpec().getHullId().startsWith("sun_ice_") &&
//        !ship.getVariant().getHullMods().contains("sun_ice_gravitonic_sensors_mod"));
    }

}