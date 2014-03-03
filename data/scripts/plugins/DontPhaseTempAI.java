/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author Nate
 */
public class DontPhaseTempAI implements ShipAIPlugin {
    ShipAPI ship;
    float countdownToCircumstanceEvaluation = 0f;
    float dontFireUntil = 0;

    static final float CIRCUMSTANCE_EVALUATION_FREQUENCY = 0.1f;

    void evaluateCircumstances() {

        if(!ship.getSystem().isActive()) {
            ship.resetDefaultAI();
            //SunUtils.print("Feel free to phase");
        }

        countdownToCircumstanceEvaluation = (CIRCUMSTANCE_EVALUATION_FREQUENCY / 2)
                + CIRCUMSTANCE_EVALUATION_FREQUENCY * (float)Math.random();
    }

    public DontPhaseTempAI() {}
    public DontPhaseTempAI(ShipAPI ship) {
        this.ship = ship;
        //SunUtils.print("Don't phase!");
    }

    @Override
    public void advance(float amount) {
        countdownToCircumstanceEvaluation -= amount;

        if(countdownToCircumstanceEvaluation < 0) evaluateCircumstances();
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
