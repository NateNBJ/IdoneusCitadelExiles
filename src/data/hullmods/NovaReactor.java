package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import data.tools.SunUtils;
import java.awt.Color;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import org.lwjgl.util.vector.Vector2f;

public class NovaReactor extends BaseHullMod
{
    static final String id = "sun_ice_nova_reactor";
    static final Random rand = new Random();
    static final float ARMOR_REPAIR_MULTIPLIER = 1000.0f;
    static final float TURN_ACCEL_MULTIPLIER = 4.0f;
    static final float TURN_SPEED_MULTIPLIER = 8.0f;
    static Map hardFlux = new WeakHashMap();
    
    static final Color SPARK_COLOR = new Color(255, 223, 128);
    static final float SPARK_DURATION = 0.3f;
    static final float SPARK_BRIGHTNESS = 0.8f;
    static final float SPARK_MAX_RADIUS = 10f;
    static final float SPARK_CHANCE = 1.0f;
    static final float SPARK_SPEED_MULTIPLIER = 100.0f;
    
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
        if(ship.getFluxTracker().isVenting()) {
            ship.getMutableStats().getTurnAcceleration().modifyMult(id, TURN_ACCEL_MULTIPLIER);
            ship.getMutableStats().getMaxTurnRate().modifyMult(id, TURN_SPEED_MULTIPLIER);
        } else {
            ship.getMutableStats().getTurnAcceleration().unmodify(id);
            ship.getMutableStats().getMaxTurnRate().unmodify(id);
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        
        if(Global.getCombatEngine().isPaused()) return;

        // Force AI to Take advantage of turn rate bonus from venting
        if(ship.getAngularVelocity() > 0.9f * ship.getMutableStats().getMaxTurnRate().getModifiedValue()
                && ship.getShipAI() != null
                && !ship.getSystem().isActive()
                && ship.getFluxTracker().getFluxLevel() > 0.4f) {
            ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
        }

        for (int i = 0; i < 3; ++i) repairArmor(ship, amount);
        //preventHardFluxDissapation(ship);
        provideManueverabilityBoostDuringVent(ship);
        
        //Global.getSettings().loadCSV("eaf").
    }


    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id)
    {
        hardFlux.put(ship, 0f);
        SunUtils.setArmorPercentage(ship, -19876);
    }


    @Override
    public boolean isApplicableToShip(ShipAPI ship)
    {
        return false;
    }
}