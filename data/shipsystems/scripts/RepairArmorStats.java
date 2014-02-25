package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import org.lwjgl.util.vector.Vector2f;
import data.scripts.plugins.SunUtils;
import java.awt.Color;
import java.util.Random;
import org.lazywizard.lazylib.MathUtils;

public class RepairArmorStats implements ShipSystemStatsScript
{
    static final float ARMOR_REPAIR_MULTIPLIER = 0.04f;
    static final Color SPARK_COLOR = new Color(255, 223, 128);
    static final float SPARK_DURATION = 0.5f;
    static final float SPARK_BRIGHTNESS = 0.8f;
    static final float SPARK_MAX_RADIUS = 10f;
    static final float SPARK_CHANCE = 1.0f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI)stats.getEntity();

        ArmorGridAPI armorGrid = ship.getArmorGrid();
        Random rng = new Random();
        float max = armorGrid.getMaxArmorInCell();
        float cellSize = armorGrid.getCellSize();
        int gridWidth = armorGrid.getGrid().length;
        int gridHeight = armorGrid.getGrid()[0].length;
        int cellCount = gridWidth * gridHeight;
        int candidates = 1 + cellCount / 10;

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

        if(bestRecord != Float.MAX_VALUE) {
            float current = armorGrid.getArmorValue(leaderX, leaderY);
            float increase = ARMOR_REPAIR_MULTIPLIER * effectLevel * cellCount
                    * max * Global.getCombatEngine().getElapsedInLastFrame();
            armorGrid.setArmorValue(leaderX, leaderY, Math.min(max, current + increase));

            if(Math.random() < SPARK_CHANCE) {
                leaderLoc.x += cellSize * 0.5f - cellSize * (float)Math.random();
                leaderLoc.y += cellSize * 0.5f - cellSize * (float)Math.random();
                Global.getCombatEngine().addHitParticle(
                        leaderLoc,
                        ship.getVelocity(),
                        SPARK_MAX_RADIUS * (float)Math.random() + SPARK_MAX_RADIUS,
                        SPARK_BRIGHTNESS,
                        SPARK_DURATION,
                        SPARK_COLOR);
            }
        }

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

        //ShipAPI ship = (ShipAPI)stats.getEntity();
        //ship.setSprite("sun_ice_ships", ship.getHullSpec().getHullId());
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) return new StatusData("repairing armor", false);
        else if(index == 1) return new StatusData("reduced engine power", false);
        return null;
    }
}