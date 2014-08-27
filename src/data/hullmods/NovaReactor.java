package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import data.tools.IntervalTracker;
import data.tools.SunUtils;
import java.awt.Color;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import org.lwjgl.util.vector.Vector2f;

public class NovaReactor extends BaseHullMod
{    
    static final Color SPARK_COLOR = new Color(255, 223, 128);
    static final float SPARK_DURATION = 0.2f;
    static final float SPARK_BRIGHTNESS = 0.95f;
    static final float SPARK_MAX_RADIUS = 6f;
    static final float SPARK_CHANCE = 1.0f;
    static final float SPARK_SPEED_MULTIPLIER = 300.0f;
    
    static final String id = "sun_ice_nova_reactor";
    static final Random rand = new Random();
    static final float ARMOR_REPAIR_MULTIPLIER = 1000.0f;
    static final float TURN_ACCEL_MULTIPLIER = 4.0f;
    static final float TURN_SPEED_MULTIPLIER = 8.0f;
    static final float CLOAK_TOGGLE_THRESHHOLD = 100.0f;
    //static Map hardFlux = new WeakHashMap();
    static Map<ShipAPI, IntervalTracker> timers = new WeakHashMap();
    static Map<ShipAPI, Boolean> phasedLastTurn = new WeakHashMap();
    //static Map<ShipAPI, Float> rotationLastTurn = new WeakHashMap();
    
    void repairArmor(ShipAPI ship, float amount) {
        if(ship.getFluxTracker().isOverloadedOrVenting()
                || !ship.isAlive()) return;
  
        ArmorGridAPI armorGrid = ship.getArmorGrid();
        int width = armorGrid.getGrid().length;
        int height = armorGrid.getGrid()[0].length;
        int x = rand.nextInt(width);
        int y = rand.nextInt(height);
        float newArmor = armorGrid.getArmorValue(x, y);
        float cellSize = armorGrid.getCellSize();

        float reduction = 1;
        reduction *= 1 - Math.floor(Math.min(2, Math.abs(x - (width - 1) / 2f))) / 2f;
        reduction *= 1 - Math.floor(Math.min(2, Math.abs(y - (height + 3) / 2f) - 1)) / 2f;
        
        float limit = armorGrid.getMaxArmorInCell() * (1 - reduction);

        if(newArmor >= limit) return;

        newArmor += ARMOR_REPAIR_MULTIPLIER * amount;
        armorGrid.setArmorValue(x, y, Math.min(limit, newArmor));
        
         if(Math.random() < SPARK_CHANCE) {
            Vector2f cellLoc = SunUtils.getCellLocation(ship, x, y);
            cellLoc.x += cellSize * 0.5f - cellSize * (float)Math.random();
            cellLoc.y += cellSize * 0.5f - cellSize * (float)Math.random();

            Vector2f vel = new Vector2f(ship.getVelocity());
            vel.x += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;
            vel.y += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;

            Global.getCombatEngine().addHitParticle(
                    cellLoc, vel,
                    SPARK_MAX_RADIUS * (float)Math.random() + SPARK_MAX_RADIUS,
                    SPARK_BRIGHTNESS,
                    SPARK_DURATION * (float)Math.random() + SPARK_DURATION,
                    SPARK_COLOR);
        }    
    }
    void preventHardFluxDissapation(ShipAPI ship) {
        ship.getFluxTracker().setHardFlux(1000);
        ship.getFluxTracker().setCurrFlux(1500);
        //SunUtils.print("poo");
        
//        float topHf = (Float)hardFlux.get(ship);
//        float hf = ship.getFluxTracker().getHardFlux();
//
//        if(ship.getFluxTracker().isVenting()) hardFlux.put(ship, 0f);
//        else if (topHf < hf) hardFlux.put(ship, hf);
//        else if (topHf > hf) {
//            ship.getFluxTracker().setHardFlux(topHf);
//            //SunUtils.print(ship, "" + topHf);
//        }
    }
    void provideManueverabilityBoostDuringVent(ShipAPI ship) {
        // Force AI to Take advantage of turn rate bonus from venting
        if(ship.getAngularVelocity() > 0.95f * ship.getMutableStats().getMaxTurnRate().getModifiedValue()
                && ship.getShipAI() != null
                && !ship.getSystem().isActive()
                && ship.getFluxTracker().getFluxLevel() > 0.4f) {
            ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
        }
        
        if(ship.getFluxTracker().isVenting()) {
            ship.getMutableStats().getTurnAcceleration().modifyMult(id, TURN_ACCEL_MULTIPLIER);
            ship.getMutableStats().getMaxTurnRate().modifyMult(id, TURN_SPEED_MULTIPLIER);
        } else {
            ship.getMutableStats().getTurnAcceleration().unmodify(id);
            ship.getMutableStats().getMaxTurnRate().unmodify(id);
        }
    }
    void checkIfShouldToggleCloak(ShipAPI ship) {
        if(ship.getShipAI() == null || ship.getFluxTracker().isOverloadedOrVenting())
            return;
        
//        boolean tryingToTurn = ship.getAngularVelocity() > 0.95f
//                * ship.getMutableStats().getMaxTurnRate().getModifiedValue()
//                || ship.getAngularVelocity() > rotationLastTurn.get(ship);
        float flux = (float)Math.sqrt(ship.getFluxTracker().getFluxLevel());
        float damage = SunUtils.estimateIncomingDamage(ship, 1);
        float armor = (float)Math.pow(SunUtils.getArmorPercent(ship), 2);
        float phaseNecessity = (damage * (1.2f - armor)) * (1 - flux);
        //float phaseNecessity = (damage * (1.2f - armor) + (tryingToTurn ? 250 : 0)) * (1 - flux);
        boolean shouldToggle = ship.getPhaseCloak().isActive()
                ? phaseNecessity < CLOAK_TOGGLE_THRESHHOLD
                : phaseNecessity > CLOAK_TOGGLE_THRESHHOLD * 1.25f;
        
        if(shouldToggle) {
            SunUtils.print(ship, "" + phaseNecessity);
            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        }
    }
    
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        
        if(Global.getCombatEngine().isPaused()) return;


        for (int i = 0; i < 5; ++i) repairArmor(ship, amount);
        //preventHardFluxDissapation(ship);
        //provideManueverabilityBoostDuringVent(ship);
        
//        if(timers.get(ship).intervalElapsed()
//                || (ship.getPhaseCloak().isActive() && !phasedLastTurn.get(ship)))
//            checkIfShouldToggleCloak(ship);
//        
//        phasedLastTurn.put(ship, ship.getPhaseCloak().isActive());
        //rotationLastTurn.put(ship, ship.getAngularVelocity());
        
        float turnRate = ship.getAngularVelocity();
        float turnRateLimit = ship.getMutableStats().getMaxTurnRate().getModifiedValue();
        
        if(ship.getSystem().isCoolingDown() && Math.abs(turnRate) > turnRateLimit) {
            ship.setAngularVelocity(turnRate * (1 - amount * 2f));
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id);
        
        ShipAPI ship = (ShipAPI)stats.getEntity();
        timers.put(ship, new IntervalTracker(0.3f, 0.5f));
        //hardFlux.put(ship, 0f);
        phasedLastTurn.put(ship, false);
        //rotationLastTurn.put(ship, 0f);
        //SunUtils.setArmorPercentage(ship, -19876);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }
}