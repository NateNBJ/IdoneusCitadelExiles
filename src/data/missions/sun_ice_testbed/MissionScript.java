package data.missions.sun_ice_testbed;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.List;

public class MissionScript implements EveryFrameCombatPlugin {
    @Override
    public void renderInWorldCoords(ViewportAPI viewport) { return; }
    @Override
    public void renderInUICoords(ViewportAPI viewport) { return; }
    
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
