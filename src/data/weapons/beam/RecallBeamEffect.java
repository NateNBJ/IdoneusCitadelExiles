package data.weapons.beam;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.ShipAPI;
import data.EveryFramePlugin;
import data.tools.IntervalTracker;
import data.tools.SunUtils;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class RecallBeamEffect implements BeamEffectPlugin {
    public class RecallTracker {
        public int priority = 0;
        public ShipAPI ally;
        public Vector2f destination, idealRecallLoc, recallLoc;
        public float progress = -1;
        
        public RecallTracker(ShipAPI ally) {
            this.ally = ally;
            
            AssignmentInfo asgnmt = Global.getCombatEngine().getFleetManager(
                    ally.getOwner()).getAssignmentFor(ally);

            if(asgnmt == null || asgnmt.getTarget() == null) return;

            destination = asgnmt.getTarget().getLocation();
            idealRecallLoc = VectorUtils.getDirectionalVector(ship.getLocation(), destination);
            idealRecallLoc.scale(Math.min(MAX_RANGE, MathUtils.getDistance(ship, destination)));
            Vector2f.add(idealRecallLoc, ship.getLocation(), idealRecallLoc);

            float distFromCurrent = MathUtils.getDistance(ally, destination);
            float distFromRecalled = MathUtils.getDistance(idealRecallLoc, destination);
            
            priority = (int)((distFromCurrent - distFromRecalled - 1000) / 500);
        }
    }
    
    static final float MAX_RANGE = 1000f;  
    
    ShipAPI ship;
    TreeMap<Integer, List<RecallTracker>> recallQueue;
    IntervalTracker timer = new IntervalTracker(0.3f, 0.5f);

    void collectRecallRequests() {
        recallQueue = new TreeMap();
        
        for(ShipAPI ally : AIUtils.getAlliesOnMap(ship)) {
            RecallTracker t = new RecallTracker(ally);
            if(t.priority > 0) {
                if(!recallQueue.containsKey(t.priority)) {
                    recallQueue.put(t.priority, new LinkedList());
                }
                
                recallQueue.get(t.priority).add(t);
            }
        }
    }
    void selectAnAllyToRecall() {
        for(Integer key : recallQueue.descendingKeySet()) {
            for(RecallTracker t : recallQueue.get(key)) {
                t.recallLoc = t.idealRecallLoc;
                EveryFramePlugin.beginRecall(t);
            }
        }
    }
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        ship = beam.getSource();
        
        if(recallQueue == null) collectRecallRequests();
        
        if(timer.intervalElapsed()) selectAnAllyToRecall();
    }
}
