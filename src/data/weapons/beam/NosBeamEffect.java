package data.weapons.beam;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.tools.SunUtils;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.Point;
import org.lwjgl.util.vector.Vector2f;

public class NosBeamEffect implements BeamEffectPlugin {
    static final float MAX_RADIUS = 70f;
    static final float INNER_RADIUS = 25f;
    static final float REPAIR_RATE = 100f;
    
    Map<Point, Float> cellMap;
    
    void buildCellMap(BeamAPI beam) {
        cellMap = new WeakHashMap();
        
        ArmorGridAPI grid = beam.getSource().getArmorGrid();

        Vector2f relLoc = new Vector2f(beam.getWeapon().getLocation());
        Vector2f.sub(relLoc, beam.getSource().getLocation(), relLoc);
        VectorUtils.rotate(relLoc, -(float)Math.toRadians(beam.getSource().getFacing() - 90), relLoc);
        Vector2f.add(relLoc, new Vector2f(
                (grid.getLeftOf() - 0.5f) * grid.getCellSize(),
                (grid.getBelow() - 0.5f) * grid.getCellSize()), relLoc);

        for(int x = 0; x < grid.getGrid().length; ++x) {
            for(int y = 0; y < grid.getGrid()[0].length; ++y) {
                Vector2f loc = new Vector2f(x * grid.getCellSize(), y * grid.getCellSize());
                float dist = MathUtils.getDistance(relLoc, loc);

                if(dist > MAX_RADIUS) continue;

                float effect = (dist <= INNER_RADIUS) ? 1
                        : 1 - (dist - INNER_RADIUS) / (MAX_RADIUS - INNER_RADIUS);

                cellMap.put(new Point(x, y), effect);
            }
        }
    }
    
    //static boolean temp = false;
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam)
    {
//        if(temp == false) {
//            temp = true;
//            SunUtils.setArmorPercentage(beam.getSource(), 0);
//        }
        
        
        CombatEntityAPI target = beam.getDamageTarget();
        
        if(!beam.didDamageThisFrame()
                || !(target instanceof ShipAPI)
                || (target.getShield() != null && target.getShield().isWithinArc(beam.getTo()))
            ) return;
            
            
        if(cellMap == null) buildCellMap(beam);
        
        ArmorGridAPI grid = beam.getSource().getArmorGrid();
        
        for(Entry<Point, Float> pair : cellMap.entrySet()) {
            float newArmorVal = grid.getArmorValue(pair.getKey().getX(), pair.getKey().getY());
            newArmorVal += Math.sqrt(pair.getValue()) * amount * REPAIR_RATE;
            newArmorVal = Math.min(newArmorVal, grid.getMaxArmorInCell());
            
            grid.setArmorValue(pair.getKey().getX(), pair.getKey().getY(), newArmorVal);
        }
    }	
}