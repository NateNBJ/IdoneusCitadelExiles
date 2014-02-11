package data.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;

public class ReverbDampeners extends BaseHullMod {

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);

//        if(ship.getShipAI() != null
//                && (ship.getHullSpec().getHullId().contains("sun_ice_kelpie")
//                || ship.getHullSpec().getHullId().contains("sun_ice_pentagram")))
//        {
//                WeaponAPI tb = (WeaponAPI)ship.getAllWeapons().get(0);
//                Vector2f end = tb.getLocation();
//                float theta = (float)((tb.getCurrAngle() / 360) * Math.PI * 2);
//                end.x += Math.cos(theta) * tb.getRange();
//                end.y += Math.sin(theta) * tb.getRange();
//
//                //Utils.print("" + tb.distanceFromArc(AIUtils.getNearestEnemy(ship).getLocation()));
//
//                //Global.getCombatEngine().addHitParticle(end, new Vector2f(), 10, 1, 1, Color.yellow);
//
//                ShipAPI hitShip = null;
//                float closestDist = Float.MAX_VALUE;
//
//                for (Iterator iter = CombatUtils.getShipsWithinRange(tb.getLocation(), tb.getRange()).iterator(); iter.hasNext();) {
//                    ShipAPI target = (ShipAPI)iter.next();
//
//                    if(target == ship
//                            || target.isHulk()
//                            || (target.getPhaseCloak() != null && target.getPhaseCloak().isActive()))
//                        continue;
//
//                    Vector2f intersect = CollisionUtils.getCollisionPoint(tb.getLocation(), end, ship);
//                    if(intersect == null) continue;
//                    float dist = MathUtils.getDistance(tb.getLocation(), intersect);
//
//                    if(dist < closestDist) {
//                        closestDist = dist;
//                        hitShip = target;
//                    }
//                }
//
//                if(hitShip == null
//                        || hitShip.getOwner() == ship.getOwner()
//                        || hitShip.isCapital())
//                    return;
//
////                if(Global.getCombatEngine().getFleetManager(hitShip.getOwner()).getDeployedFleetMember(hitShip).getMember().getFleetPointCost() > 14)
////                    tb.setRemainingCooldownTo(1);
//
//                ship.giveCommand(ShipCommand.SELECT_GROUP, null, 0);
//                ship.giveCommand(ShipCommand.USE_SELECTED_GROUP, ship.getMouseTarget(), 0);
//        }
    }

	public static final float SIGHT_RADIUS_BONUS = 20f;
	public static final float TRACKING_PENALTY = -50f;
    @Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
            //stats.getEnergyWeaponDamageMult().modifyMult(id, 100);

        stats.getHullDamageTakenMult().modifyMult(id, 0.5f);
        stats.getArmorDamageTakenMult().modifyMult(id, 0.5f);

//        stats.getEnergyDamageTakenMult().modifyMult(id, 100);
//        stats.getFragmentationDamageTakenMult().modifyMult(id, 100);
//        stats.getKineticDamageTakenMult().modifyMult(id, 100);

        //stats.getProjectileDamageTakenMult().modifyMult(id, 100);
        stats.getMissileDamageTakenMult().modifyMult(id, 4);
        stats.getBeamDamageTakenMult().modifyMult(id, 4);
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
//        !ship.getVariant().getHullMods().contains("sun_ice_reverb_dampeners_mod"));
    }

}