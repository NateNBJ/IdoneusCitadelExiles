package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.plugins.PhaseCruiseTempAI;
import java.util.Map;
import java.util.WeakHashMap;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

public class GravimetricSensors extends BaseHullMod {    
//    @Override
//    public void advanceInCampaign(FleetMemberAPI member, float amount) {
//        CampaignFleetAPI fleet = member.getFleetData().getFleet();
//        fleet.getCargo().addSupplies(fleet.getLogistics().getPersonnelSuppliesPerDay() / 2);
//        // Refunds half the supplies used by personell each day. Don't ask how.
//    }
	public static final float PALANTIR_CLOAK_SECONDS = 10f;
	public static final float SIGHT_RADIUS_BONUS = 20f;
	public static final float TRACKING_PENALTY = -50f;
    private static final float REFRESH_FREQUENCY = 0.5f;
    private Map timeOfNextRefresh = new WeakHashMap();

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);

        ShipSystemAPI cloak = ship.getPhaseCloak();
        
        if (cloak == null) return;

        if(cloak.isActive() && cloak.getFluxPerSecond() == 0) {
            BattleObjectiveAPI objective = AIUtils.getNearestObjective(ship);
            if(objective != null) {
                float dist = MathUtils.getDistance(ship, objective);

                if(dist < 1000) {
                    ship.getFluxTracker().increaseFlux(amount * (1 - dist / 1000f)
                            * ship.getFluxTracker().getMaxFlux()
                            * (1 / PALANTIR_CLOAK_SECONDS), true);
                }
            }
        }

        if((Float)timeOfNextRefresh.get(ship) < Global.getCombatEngine().getTotalElapsedTime(false)) {
            timeOfNextRefresh.put(ship, (Float)timeOfNextRefresh.get(ship)
                    + REFRESH_FREQUENCY * (float)Math.random() * 2);
        } else return;

        float speed = (float)Math.sqrt(Math.pow(ship.getVelocity().x, 2)
                + Math.pow(ship.getVelocity().y, 2));
        AssignmentInfo task = Global.getCombatEngine().getFleetManager(ship.getOwner()).getAssignmentFor(ship);


        if (!cloak.isActive() && ship.getShipAI() != null
                && ship.getAngularVelocity() < 1f
                && !ship.getTravelDrive().isActive()
                && ship.getFluxTracker().getFluxLevel() == 0
                && speed >= ship.getMutableStats().getMaxSpeed().getModifiedValue() - 2
                && AIUtils.getNearbyEnemies(ship, 2000).isEmpty()
                && (task == null || task.getTarget() == null || MathUtils.getDistance(ship, task.getTarget().getLocation()) > 800)
                ) {

            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
            ship.setShipAI(new PhaseCruiseTempAI(ship));
        }
    }

    @Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        timeOfNextRefresh.put(stats.getEntity(), 0.0f);
        
		stats.getBallisticWeaponRangeBonus().modifyFlat(id, 300);
		stats.getBallisticWeaponRangeBonus().modifyPercent(id, -50f);

        stats.getSightRadiusMod().modifyPercent(id, SIGHT_RADIUS_BONUS);
	}
	
    @Override
    public boolean isApplicableToShip(ShipAPI ship){
        return false;
    }

}