package data.missions.testbed;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.List;

public class MissionScript implements EveryFrameCombatPlugin {
    boolean isFirstFrame = true;
    
    CombatEngineAPI engine;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if(isFirstFrame) {
            
            isFirstFrame = false;
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }
    
}
