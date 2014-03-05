package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import data.scripts.IntervalTracker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

@SuppressWarnings("unchecked")
public abstract class BaseShipAI implements ShipAIPlugin {
    protected ShipAPI ship;
    protected float dontFireUntil = 0f;
    protected IntervalTracker circumstanceEvaluationTimer = new IntervalTracker(0.05f, 0.15f);

    public void evaluateCircumstances() { }

    public ShipCommand turnToward(float degreeAngle) {
        float angleDif = MathUtils.getShortestRotation(ship.getFacing(), degreeAngle);
        
        if(Math.abs(angleDif) < 5) return null;

        ShipCommand direction = (angleDif > 0) ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT;
        ship.giveCommand(direction, null, 0);
        
        return direction;
    }
    public ShipCommand turnToward(Vector2f location) {
        return turnToward(VectorUtils.getAngle(ship.getLocation(), location));
    }
    public ShipCommand turnToward(CombatEntityAPI entity) {
        return turnToward(entity.getLocation());
    }
    public void accelerate() {
        ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
    }
    public void accelerateBackward() {
        ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null, 0);
    }
    public void decelerate() {
        ship.giveCommand(ShipCommand.DECELERATE, null, 0);
    }
    public void turnLeft() {
        ship.giveCommand(ShipCommand.TURN_LEFT, null, 0);
    }
    public void strafeLeft() {
        ship.giveCommand(ShipCommand.STRAFE_LEFT, null, 0);
    }
    public void strafeRight() {
        ship.giveCommand(ShipCommand.STRAFE_RIGHT, null, 0);
    }
    public void vent() {
        ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
    }
    public void useSystem() {
        ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
    }
    public void toggleDefenseSystem() {
        ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
    }
    public void fireSelectedGroup(Vector2f at) {
        ship.giveCommand(ShipCommand.USE_SELECTED_GROUP, at, 0);
    }
    public void toggleAutofire(int group) {
        ship.giveCommand(ShipCommand.TOGGLE_AUTOFIRE, null, group);
    }
    public void selectWeaponGroup(int group) {
        ship.giveCommand(ShipCommand.SELECT_GROUP, null, group);
    }

    public BaseShipAI() {}
    public BaseShipAI(ShipAPI ship) { this.ship = ship; }

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