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
public class PhaseCruiseTempAI implements ShipAIPlugin {
    ShipAPI ship;
    CombatFleetManagerAPI fleet;
    AssignmentInfo task;
    float countdownToCircumstanceEvaluation = 0f;
    float dontFireUntil = 0;

    static final float CIRCUMSTANCE_EVALUATION_FREQUENCY = 0.5f;
    
//    boolean assignmentHasChanged() {
//        //return initialAssignment != fleet.getAssignmentFor(ship);
//        AssignmentInfo currentAssignment = fleet.getAssignmentFor(ship);
//
//        if(initialAssignment == currentAssignment) return false;
//        else if(initialAssignment == null || currentAssignment == null)
//            return true;
//
//        return initialAssignment.equals(currentAssignment);
//    }
//
    void evaluateCircumstances() {
        ShipSystemAPI cloak = ship.getPhaseCloak();

        if(cloak == null || ship.getFluxTracker().getFluxLevel() > 0.75f
                || ship.getPhaseCloak().isCoolingDown()
                || AIUtils.getNearbyEnemies(ship, 1800).size() > 0
                || (task != null && task.getTarget() != null
                    && MathUtils.getDistance(ship, task.getTarget().getLocation()) < 700)
                ) {
            
            ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            ship.resetDefaultAI();
            //SunUtils.print(ship, "back.");
        }

        countdownToCircumstanceEvaluation = (CIRCUMSTANCE_EVALUATION_FREQUENCY / 2)
                + CIRCUMSTANCE_EVALUATION_FREQUENCY * (float)Math.random();
    }
    void goToDestination(Vector2f to) {
        float angleDif = MathUtils.getShortestRotation(ship.getFacing(),
                VectorUtils.getAngle(ship.getLocation(), to));

        if(Math.abs(angleDif) < 30){
            ship.giveCommand(ShipCommand.ACCELERATE, to, 0);
        } else {
            ShipCommand direction = (angleDif > 0)
                    ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT;
            ship.giveCommand(direction, to, 0);
            ship.giveCommand(ShipCommand.DECELERATE, to, 0);
        }        
    }

    public PhaseCruiseTempAI() {}
    public PhaseCruiseTempAI(ShipAPI ship) {
        this.ship = ship;
        this.fleet = Global.getCombatEngine().getFleetManager(ship.getOwner());
        //this.initialAssignment = fleet.getAssignmentFor(ship);
    }

    @Override
    public void advance(float amount) {
        ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
        
        task = fleet.getAssignmentFor(ship);
        if(task != null && task.getTarget() != null) {
            goToDestination(task.getTarget().getLocation());
        } else ship.giveCommand(ShipCommand.ACCELERATE, null, 0);

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
