package data.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;

public class GravimetricSensors extends BaseHullMod {
	public static final float SIGHT_RADIUS_BONUS = 20f;
	public static final float TRACKING_PENALTY = -50f;

    @Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
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