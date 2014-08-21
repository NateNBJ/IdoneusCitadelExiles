package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.ai.ship.PhaseCruiseTempAI;
import data.tools.IntervalTracker;
import java.awt.Color;
import java.util.Map;
import java.util.WeakHashMap;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class GravimetricSensors extends BaseHullMod {
    public static final float EMP_ENERGY_DAMAGE = 0f;
    public static final float EMP_EMP_DAMAGE = 0f;
    public static final float EMP_RANGE = 250f;
    public static final float EMP_THICKNESS = 5f;
    public static final Color EMP_COLOR = new Color(0, 255, 220);
    
    
    public static final float PALANTIR_CLOAK_SECONDS = 10f;
    public static final float SIGHT_RADIUS_BONUS = 20f;
    public static final float TRACKING_PENALTY = -50f;
    private static final float MIN_REFRESH = 0.1f;
    private static final float MAX_REFRESH = 0.9f;
    private final Map<ShipAPI, IntervalTracker> intervalTrackers = new WeakHashMap();
    
    void doSkimAiHack(ShipAPI ship, float amount) {
        String id = ship.getHullSpec().getHullId();
        ShipSystemAPI cloak = (id.equals("sun_ice_abraxas") || id.equals("sun_ice_athame"))
                ? ship.getSystem()
                : ship.getPhaseCloak();

        if (cloak == null) return;

        if (cloak.isActive() && cloak.getFluxPerSecond() == 0) {
            BattleObjectiveAPI objective = AIUtils.getNearestObjective(ship);
            if (objective != null) {
                float dist = MathUtils.getDistance(ship, objective);

                if (dist < 1000) {
                    ship.getFluxTracker().increaseFlux(amount * (1 - dist / 1000f)
                            * ship.getFluxTracker().getMaxFlux()
                            * (1 / PALANTIR_CLOAK_SECONDS), true);
                }
            }
        }

        if(!intervalTrackers.get(ship).intervalElapsed()) return;

        float speed = (float) Math.sqrt(Math.pow(ship.getVelocity().x, 2)
                + Math.pow(ship.getVelocity().y, 2));
        AssignmentInfo task = Global.getCombatEngine().getFleetManager(ship.getOwner()).getAssignmentFor(ship);

        if (!cloak.isActive() && ship.getShipAI() != null
                && ship.getAngularVelocity() < 1f
                && !ship.getTravelDrive().isActive()
                && ship.getFluxTracker().getFluxLevel() == 0
                && speed >= ship.getMutableStats().getMaxSpeed().getModifiedValue() - 2
                && AIUtils.getNearbyEnemies(ship, 2000).isEmpty()
                && (task == null || task.getTarget() == null || MathUtils.getDistance(ship, task.getTarget().getLocation()) > 800)) {

            if(ship.getPhaseCloak() == cloak) {
                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
            } else {
                ship.useSystem();
            }
            
//            if (ship.getHullSpec().getHullId().equals("sun_ice_abraxas")) {
//                ship.useSystem();
//            } else {
//                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
//            }

            ship.setShipAI(new PhaseCruiseTempAI(ship));
        }
    }
    void doEmpDeathThrows(ShipAPI ship, float amount) {
        if(ship.isFighter() || !intervalTrackers.get(ship).intervalElapsed()) return;
        
        ShipAPI target = AIUtils.getNearestShip(ship);
        Vector2f point = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius() * 0.6f);
        float range = ship.getCollisionRadius()+ EMP_RANGE;
        
        if(target == null || MathUtils.getDistance(ship, target) > range)
            target = ship;
        
        Global.getCombatEngine().spawnEmpArc(ship, point, ship, target,
                DamageType.ENERGY, EMP_ENERGY_DAMAGE, EMP_EMP_DAMAGE, range,
                null, EMP_THICKNESS, Color.WHITE, EMP_COLOR);
    }
    
    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {
        super.advanceInCampaign(member, amount);
        
        member.getStatus().repairFully();
    }
    
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        
        if(ship.isAlive()) doSkimAiHack(ship, amount);
        else if(ship.isHulk()) doEmpDeathThrows(ship, amount);
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        intervalTrackers.put((ShipAPI)stats.getEntity(), new IntervalTracker(MIN_REFRESH, MAX_REFRESH));

        stats.getBallisticWeaponRangeBonus().modifyFlat(id, 300);
        stats.getBallisticWeaponRangeBonus().modifyPercent(id, -50f);

        stats.getSightRadiusMod().modifyPercent(id, SIGHT_RADIUS_BONUS);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

}
