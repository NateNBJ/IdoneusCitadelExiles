package data.weapons.beam;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.EveryFramePlugin;
import data.tools.IntervalTracker;
import data.tools.RecallTracker;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import org.lazywizard.lazylib.combat.AIUtils;

public class RecallBeamEffect implements BeamEffectPlugin {
    public static final float CHARGE_TIME = 2f;  
    
    ShipAPI ship;
    TreeMap<Integer, List<RecallTracker>> recallQueue;
    IntervalTracker doRecallTimer = new IntervalTracker(0.1f, 0.5f);
    CombatEngineAPI engine;

    void collectRecallRequests(WeaponAPI weapon) {
        recallQueue = new TreeMap();
        
        for(ShipAPI ally : AIUtils.getAlliesOnMap(ship)) {
            RecallTracker t = new RecallTracker(ally, weapon);
            if(t.getPriority() > 0) {
                if(!recallQueue.containsKey(t.getPriority())) {
                    recallQueue.put(t.getPriority(), new LinkedList());
                }
                
                recallQueue.get(t.getPriority()).add(t);
            }
        }
    }
    void selectAnAllyToRecall() {
        if(recallQueue.isEmpty()) return;
        
        List<RecallTracker> candidates = recallQueue.lastEntry().getValue();
        int index = (new Random()).nextInt(candidates.size());
        RecallTracker winner = candidates.get(index);
        
        EveryFramePlugin.beginRecall(winner);
        Global.getSoundPlayer().playSound("system_phase_cloak_activate", 2, 1,
                winner.getAlly().getLocation(), winner.getAlly().getVelocity());
        
        candidates.remove(winner);
        if(candidates.isEmpty()) {
            recallQueue.remove(recallQueue.lastKey());
        }
    }
    
    public static int getCumulativeRecallPriority(WeaponAPI wpn) {
        int acc = 0;
        
        for(ShipAPI ally : AIUtils.getAlliesOnMap(wpn.getShip())) {
            RecallTracker t = new RecallTracker(ally, wpn);
            acc += t.getPriority(); 
        }
        
        return acc;
    }
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        this.engine = engine;
        this.ship = beam.getSource();
        
        if(recallQueue == null) collectRecallRequests(beam.getWeapon());
        
        if(doRecallTimer.intervalElapsed()) selectAnAllyToRecall();
    }
}
