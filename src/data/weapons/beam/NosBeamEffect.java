package data.weapons.beam;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.Point;
import org.lwjgl.util.vector.Vector2f;

public class NosBeamEffect implements BeamEffectPlugin {
    float maxRadius, innerRadius, repairRate;
    
    Map<Point, Float> cellMap;
    
    void setHealStats(String id) {
        if(id.equals("sun_ice_chupacabra")) {
            maxRadius = 80;
            innerRadius = 25;
            repairRate = 80;
        } else if(id.equals("sun_ice_nos")) {
            maxRadius = 120;
            innerRadius = 40;
            repairRate = 100;
        }
    }
    void buildCellMap(BeamAPI beam) {
        cellMap = new WeakHashMap();
        ArmorGridAPI grid = beam.getSource().getArmorGrid();
        int[] center = grid.getCellAtLocation(beam.getWeapon().getLocation());
        Vector2f relLoc = new Vector2f(center[0] * grid.getCellSize(), center[1] * grid.getCellSize());

        for(int x = 0; x < grid.getGrid().length; ++x) {
            for(int y = 0; y < grid.getGrid()[0].length; ++y) {
                Vector2f loc = new Vector2f(x * grid.getCellSize(), y * grid.getCellSize());
                float dist = MathUtils.getDistance(relLoc, loc);

                if(dist > maxRadius) continue;

                float effect = (dist <= innerRadius) ? 1
                        : 1 - (dist - innerRadius) / (maxRadius - innerRadius);

                cellMap.put(new Point(x, y), effect);
            }
        }
    }
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        CombatEntityAPI target = beam.getDamageTarget();
        
        if(!beam.didDamageThisFrame()
                || !(target instanceof ShipAPI)
                || !((ShipAPI)target).isAlive()
                || (target.getShield() != null && target.getShield().isWithinArc(beam.getTo()))
            ) return;
            
            
        if(cellMap == null) {
            setHealStats(beam.getWeapon().getId());
            buildCellMap(beam);
        }
        
        ArmorGridAPI grid = beam.getSource().getArmorGrid();
        
        for(Entry<Point, Float> pair : cellMap.entrySet()) {
            float currentVal = grid.getArmorValue(pair.getKey().getX(), pair.getKey().getY());
            float newVal = currentVal + (float)Math.sqrt(pair.getValue()) * amount * repairRate;
            newVal = Math.min(newVal, grid.getMaxArmorInCell() * pair.getValue());
            newVal = Math.max(newVal, currentVal);
            
            grid.setArmorValue(pair.getKey().getX(), pair.getKey().getY(), newVal);
        }
    }	
}