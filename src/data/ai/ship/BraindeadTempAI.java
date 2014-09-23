package data.ai.ship;

import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;

public class BraindeadTempAI implements ShipAIPlugin {

    public BraindeadTempAI(ShipAPI ship) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void forceCircumstanceEvaluation() {
    }

    @Override
    public boolean needsRefit() {
        return false;
    }

    @Override
    public void setDoNotFireDelay(float amount) {
    }
}
