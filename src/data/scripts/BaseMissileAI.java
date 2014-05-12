package data.scripts;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

@SuppressWarnings("unchecked")
public abstract class BaseMissileAI implements MissileAIPlugin {
    protected MissileAPI missile;
    protected ShipAPI target;
    protected IntervalTracker circumstanceEvaluationTimer = new IntervalTracker(0.05f, 0.15f);

    public void findTarget() {
        target = missile.getSource().getShipTarget();

        if(target == null
                || !target.isAlive()
                || target.getOwner() == missile.getOwner()) {
            target = AIUtils.getNearestEnemy(missile);
        }
    }
    public void evaluateCircumstances() { }

    public ShipCommand strafe(float degreeAngle, boolean strafeAway) {
        float angleDif = MathUtils.getShortestRotation(missile.getFacing(), degreeAngle);

        if(Math.abs(angleDif) < 1) return null;

        ShipCommand direction = (angleDif > 0 ^ strafeAway)
                ? ShipCommand.STRAFE_LEFT
                : ShipCommand.STRAFE_RIGHT;

        missile.giveCommand(direction);

        return direction;
    }
    public ShipCommand strafe(Vector2f location, boolean strafeAway) {
        return strafe(VectorUtils.getAngle(missile.getLocation(), location), strafeAway);
    }
    public ShipCommand strafe(CombatEntityAPI entity, boolean strafeAway) {
        return strafe(entity.getLocation(), strafeAway);
    }
    public ShipCommand strafeToward(float degreeAngle) {
        return strafe(degreeAngle, false);
    }
    public ShipCommand strafeToward(Vector2f location) {
        return strafeAway(VectorUtils.getAngle(missile.getLocation(), location));
    }
    public ShipCommand strafeToward(CombatEntityAPI entity) {
        return strafeToward(entity.getLocation());
    }
    public ShipCommand strafeAway(float degreeAngle) {
        return strafe(degreeAngle, true);
    }
    public ShipCommand strafeAway(Vector2f location) {
        return strafeAway(VectorUtils.getAngle(missile.getLocation(), location));
    }
    public ShipCommand strafeAway(CombatEntityAPI entity) {
        return strafeAway(entity.getLocation());
    }
    public ShipCommand turn(float degreeAngle, boolean turnAway) {
        float angleDif = MathUtils.getShortestRotation(missile.getFacing(), degreeAngle);

        if(Math.abs(angleDif) < 1) return null;

        ShipCommand direction = (angleDif > 0 ^ turnAway)
                ? ShipCommand.TURN_LEFT
                : ShipCommand.TURN_RIGHT;

        missile.giveCommand(direction);

        return direction;
    }
    public ShipCommand turn(Vector2f location, boolean strafeAway) {
        return turn(VectorUtils.getAngle(missile.getLocation(), location), strafeAway);
    }
    public ShipCommand turn(CombatEntityAPI entity, boolean strafeAway) {
        return turn(entity.getLocation(), strafeAway);
    }
    public ShipCommand turnToward(float degreeAngle) {
        return turn(degreeAngle, false);
    }
    public ShipCommand turnToward(Vector2f location) {
        return turnToward(VectorUtils.getAngle(missile.getLocation(), location));
    }
    public ShipCommand turnToward(CombatEntityAPI entity) {
        return turnToward(entity.getLocation());
    }
    public ShipCommand turnAway(float degreeAngle) {
        return turn(degreeAngle, true);
    }
    public ShipCommand turnAway(Vector2f location) {
        return turnToward(VectorUtils.getAngle(missile.getLocation(), location));
    }
    public ShipCommand turnAway(CombatEntityAPI entity) {
        return turnToward(entity.getLocation());
    }
    public void accelerate() {
        missile.giveCommand(ShipCommand.ACCELERATE);
    }
    public void accelerateBackward() {
        missile.giveCommand(ShipCommand.ACCELERATE_BACKWARDS);
    }
    public void decelerate() {
        missile.giveCommand(ShipCommand.DECELERATE);
    }
    public void turnLeft() {
        missile.giveCommand(ShipCommand.TURN_LEFT);
    }
    public void turnRight() {
        missile.giveCommand(ShipCommand.TURN_RIGHT);
    }
    public void strafeLeft() {
        missile.giveCommand(ShipCommand.STRAFE_LEFT);
    }
    public void strafeRight() {
        missile.giveCommand(ShipCommand.STRAFE_RIGHT);
    }

    public BaseMissileAI() {}
    public BaseMissileAI(MissileAPI missile) { this.missile = missile; }

    @Override
    public void advance(float amount) {
        if(circumstanceEvaluationTimer.intervalElapsed()) evaluateCircumstances();
    }
}