package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import data.scripts.IntervalTracker;
import java.awt.Point;
import java.util.Iterator;
import java.util.Random;
import org.lazywizard.lazylib.combat.AIUtils;

public class RepairAoE implements ShipSystemStatsScript
{
    static final float ARMOR_HEAL_PER_SECOND = 300.0f;
    static final float HULL_HEAL_PER_SECOND = 300.0f;
    static final float RANGE = 800.0f;
    static final Random rand = new Random();

    IntervalTracker tracker = new IntervalTracker(0.05f);

    void heal(ShipAPI ship) {
        ArmorGridAPI armorGrid = ship.getArmorGrid();
        float max = armorGrid.getMaxArmorInCell();
        int gridWidth = armorGrid.getGrid().length;
        int gridHeight = armorGrid.getGrid()[0].length;
        Point cellToFix = new Point(rand.nextInt(gridWidth), rand.nextInt(gridHeight));

        ship.setHitpoints((float)Math.min(ship.getMaxHitpoints(),
                ship.getHitpoints() + HULL_HEAL_PER_SECOND * tracker.getAverageInterval()));

        for(int x = cellToFix.x - 1; x <= cellToFix.x + 1; ++ x) {
            if(x < 0 || x >= gridWidth) continue;

            for(int y = cellToFix.y - 1; y <= cellToFix.y + 1; ++ y) {
                if(y < 0 || y >= gridHeight) continue;

                float mult = (float)((3 - Math.abs(x - cellToFix.x) - Math.abs(y - cellToFix.y)) / 3f);

                armorGrid.setArmorValue(x, y, Math.min(max, armorGrid.getArmorValue(x, y)
                        + ARMOR_HEAL_PER_SECOND * tracker.getAverageInterval() * mult));
            }
        }
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if(!tracker.intervalElapsed()) return;
        
        heal((ShipAPI)stats.getEntity());

        for(Iterator iter = AIUtils.getNearbyAllies(stats.getEntity(), RANGE).iterator(); iter.hasNext();) {
            heal((ShipAPI)iter.next());
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) return new StatusData("healing nearby allies", false);
        
        return null;
    }
}