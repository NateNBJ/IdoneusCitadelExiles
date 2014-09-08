package data.ai.ship;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import data.tools.IntervalTracker;
import data.tools.SunUtils;

public class DontPhaseTempAI implements ShipAIPlugin {
    ShipAPI ship;
    IntervalTracker circumstanceEvaluationTimer = new IntervalTracker(0.05f, 0.15f);
    float dontFireUntil = 0;

    void evaluateCircumstances() {
        if(!ship.getSystem().isActive()) {
            ship.resetDefaultAI();
            //SunUtils.print("Feel free to phase");
        }
    }

    public DontPhaseTempAI(ShipAPI ship) {
        this.ship = ship;
        //SunUtils.print("Don't phase!");
    }

    @Override
    public void advance(float amount) {
        if(circumstanceEvaluationTimer.intervalElapsed()) evaluateCircumstances();
    }

    @Override
    public void forceCircumstanceEvaluation() {
        evaluateCircumstances();
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
