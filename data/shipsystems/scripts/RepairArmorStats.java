package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import org.lwjgl.util.vector.Vector2f;
import data.scripts.plugins.SunUtils;
import java.awt.Color;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import org.lazywizard.lazylib.MathUtils;

public class RepairArmorStats implements ShipSystemStatsScript
{
    static final float MIN_POTENCY = 0.5f;
    static final float MAX_POTENCY = 3.0f;
    static final float ARMOR_REPAIRED_PER_FLUX = 0.5f;
    static final float SECONDS_TO_MAX_POTENCY = 3.0f;
    static final float SECONDS_TO_MIN_POTENCY = 0.5f;
    
    static final Color SPARK_COLOR = new Color(255, 223, 128);
    static final float SPARK_DURATION = 0.3f;
    static final float SPARK_BRIGHTNESS = 0.8f;
    static final float SPARK_MAX_RADIUS = 10f;
    static final float SPARK_CHANCE = 1.0f;
    static final float SPARK_SPEED_MULTIPLIER = 100.0f;

    static Map potencies = new WeakHashMap();

    public static float getPotency(ShipAPI ship) {
        return potencies.containsKey(ship) ? (float)(Float)potencies.get(ship) : 0.0f;
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI)stats.getEntity();
        CombatEngineAPI engine = Global.getCombatEngine();
        float amount = engine.getElapsedInLastFrame();

        ArmorGridAPI armorGrid = ship.getArmorGrid();
        Random rng = new Random();
        float max = armorGrid.getMaxArmorInCell();
        float cellSize = armorGrid.getCellSize();
        int gridWidth = armorGrid.getGrid().length;
        int gridHeight = armorGrid.getGrid()[0].length;
        int cellCount = gridWidth * gridHeight;
        int candidates = 1 + cellCount / 10;

        // Increment Potency
        float potency = getPotency(ship);
        potency += (ship.getFluxTracker().getCurrFlux() == ship.getFluxTracker().getHardFlux())
                ? amount / SECONDS_TO_MAX_POTENCY
                : -amount / SECONDS_TO_MIN_POTENCY;
        potency = Math.min(1, Math.max(0, potency * effectLevel));
        potencies.put(ship, potency);
        potency = MIN_POTENCY + (MAX_POTENCY - MIN_POTENCY) * potency;

        // Determine which armor cell to try to repair
        int leaderX = 0, leaderY = 0;
        Vector2f leaderLoc = null;
        float bestRecord = Float.MAX_VALUE;

        for(int i = 0; i < candidates; ++i) {
            int x = rng.nextInt(gridWidth);
            int y = rng.nextInt(gridHeight);
            Vector2f cellLoc = SunUtils.getCellLocation(ship, x, y);
            float current = armorGrid.getArmorValue(x, y);
            float dist = MathUtils.getDistance(cellLoc , ship.getMouseTarget());

            if((dist < bestRecord) && (current < max)) {
                leaderLoc = cellLoc;
                bestRecord = dist;
                leaderX = x;
                leaderY = y;
            }
        }

        float fluxCost = potency * amount
                * ship.getMutableStats().getFluxDissipation().getBaseValue();

        // Repair the chosen armor cell
        if(bestRecord != Float.MAX_VALUE) {
            float current = armorGrid.getArmorValue(leaderX, leaderY);
            float increase = ARMOR_REPAIRED_PER_FLUX * fluxCost;


                armorGrid.setArmorValue(leaderX, leaderY, Math.min(max, current + increase));

                if(Math.random() < SPARK_CHANCE) {
                    leaderLoc.x += cellSize * 0.5f - cellSize * (float)Math.random();
                    leaderLoc.y += cellSize * 0.5f - cellSize * (float)Math.random();
                    
                    Vector2f vel = new Vector2f(ship.getVelocity());
                    vel.x += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;
                    vel.y += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;

                    engine.addHitParticle(
                            leaderLoc, vel,
                            (SPARK_MAX_RADIUS * (float)Math.random() + SPARK_MAX_RADIUS)
                                * (float)Math.sqrt(fluxCost / 30),
                            SPARK_BRIGHTNESS,
                            SPARK_DURATION * (float)Math.random() + SPARK_DURATION,
                            SPARK_COLOR);
                }
            
        }

        // Generate flux
        if(ship.getFluxTracker().getCurrFlux() + fluxCost > ship.getFluxTracker().getMaxFlux()) {
            ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
        } else {
            ship.getFluxTracker().increaseFlux(fluxCost, true);
        }

        // Apply mobility debuffs
        if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id);
			stats.getMaxTurnRate().unmodify(id);
		} else {
			stats.getMaxSpeed().modifyPercent(id, -20f * effectLevel);
			stats.getAcceleration().modifyPercent(id, -50f * effectLevel);
			stats.getDeceleration().modifyPercent(id, -50f * effectLevel);
            stats.getMaxTurnRate().modifyPercent(id, -20f * effectLevel);
			stats.getTurnAcceleration().modifyPercent(id, -50f * effectLevel);
		}
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);

        potencies.remove(stats.getEntity());
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) return new StatusData("repairing armor", false);
        else if(index == 1) return new StatusData("reduced engine power", false);
        
        return null;
    }
}