package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import java.util.Random;
import org.lwjgl.util.vector.Vector2f;

public class TempMeleeAI implements ShipAIPlugin {
    ShipAPI drone;
    ShipAPI target;
    float countdownToCircumstanceEvaluation = 0f;
    Vector2f destination;
    Random rng = new Random();
    float dontFireUntil = 0;

    @Override
    public void advance(float amount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void forceCircumstanceEvaluation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean needsRefit() {
        return false;
    }

    @Override
    public void setDoNotFireDelay(float amount) {
        dontFireUntil = amount + Global.getCombatEngine().getTotalElapsedTime(false);
    }

}
