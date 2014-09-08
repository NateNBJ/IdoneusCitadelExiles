package data.ai.ship;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class PhaseCruiseTempAI extends BaseShipAI {
    CombatFleetManagerAPI fleet;
    AssignmentInfo task;

    @Override
    public void evaluateCircumstances() {
        ShipSystemAPI cloak = ship.getPhaseCloak();

        if(cloak == null || ship.getFluxTracker().getFluxLevel() > 0.75f
                || ship.getPhaseCloak().isCoolingDown()
                || AIUtils.getNearbyEnemies(ship, 1800).size() > 0
                || (task != null && task.getTarget() != null
                    && MathUtils.getDistance(ship, task.getTarget().getLocation()) < 700)
                ) {
            
            ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            ship.resetDefaultAI();
        }
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

    public PhaseCruiseTempAI(ShipAPI ship) {
        super(ship);
        this.fleet = Global.getCombatEngine().getFleetManager(ship.getOwner());
        circumstanceEvaluationTimer.setInterval(0.4f, 0.6f);
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
        
        task = fleet.getAssignmentFor(ship);
        if(task != null && task.getTarget() != null) {
            this.turnToward(task.getTarget().getLocation());
            this.strafeToward(task.getTarget().getLocation());
        }
    }
}