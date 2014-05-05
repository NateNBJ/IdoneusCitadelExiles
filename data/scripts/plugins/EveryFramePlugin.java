package data.scripts.plugins;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.List;

public class EveryFramePlugin implements EveryFrameCombatPlugin {
    public interface ProjectileEffectAPI {
        void advance(float amount);
    }

    CombatEngineAPI engine;

    @Override
    public void advance(float amount, List events) {
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }
}