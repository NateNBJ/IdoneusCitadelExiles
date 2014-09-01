package data.ai.missile;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import data.tools.IntervalTracker;
import data.tools.SunUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

@SuppressWarnings("unchecked")
public abstract class BaseMissileAI implements MissileAIPlugin, GuidedMissileAI {
    static final float DEFAULT_FLARE_VULNERABILITY_RANGE = 500;
    static final float DEFAULT_FACING_THRESHHOLD = 5;

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }
    
    protected MissileAPI missile;
    protected CombatEntityAPI target;
    protected IntervalTracker circumstanceEvaluationTimer = new IntervalTracker(0.05f, 0.15f);

    public CombatEntityAPI findFlareTarget(float range) {
        List<MissileAPI> flares = new ArrayList();
        
        for(MissileAPI m :  AIUtils.getNearbyEnemyMissiles(missile, range)) {
            if(m.isFlare()) flares.add(m);
        }
        
        return target = (flares.isEmpty()) ? target : flares.get((new Random()).nextInt(flares.size()));
        
    }
    public CombatEntityAPI findTarget() {
        findFlareTarget(DEFAULT_FLARE_VULNERABILITY_RANGE);
        
        if(targetIsFlare()) return target;
        
        target = missile.getSource().getShipTarget();

        if(target == null
                || (!(target instanceof ShipAPI) || !((ShipAPI)target).isAlive())
                || target.getOwner() == missile.getOwner()) {
            target = AIUtils.getNearestEnemy(missile);
        }
        
        return target;
    }
    public boolean isFacing(CombatEntityAPI target) {
        return isFacing(target.getLocation(), DEFAULT_FACING_THRESHHOLD);
    }
    public boolean isFacing(CombatEntityAPI target, float threshholdDegrees) {
        return isFacing(target.getLocation(), threshholdDegrees);
    }
    public boolean isFacing(Vector2f point) {
        return isFacing(point, DEFAULT_FACING_THRESHHOLD);
    }
    public boolean isFacing(Vector2f point, float threshholdDegrees) {
        return (Math.abs(getAngleTo(point)) <= threshholdDegrees);
    }
    public float getAngleTo(CombatEntityAPI entity) {
        return getAngleTo(entity.getLocation());
    }
    public float getAngleTo(Vector2f point) {
        float angleTo = VectorUtils.getAngle(missile.getLocation(), point);
        return MathUtils.getShortestRotation(missile.getFacing(), angleTo);
    }
    
    public boolean targetIsFlare() {
        return (target instanceof MissileAPI)
                && ((MissileAPI)target).isFlare()
                && !((MissileAPI)target).isFizzling()
                && !((MissileAPI)target).isFading();
    }
    public void evaluateCircumstances() {
        findTarget();
    }

    public ShipCommand strafe(float degreeAngle, boolean strafeAway) {
        float angleDif = MathUtils.getShortestRotation(missile.getFacing(), degreeAngle);

        if((!strafeAway && Math.abs(angleDif) < DEFAULT_FACING_THRESHHOLD)
                || (strafeAway && Math.abs(angleDif) > 180 - DEFAULT_FACING_THRESHHOLD))
            return null;

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
        return strafeToward(VectorUtils.getAngle(missile.getLocation(), location));
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

        if((!turnAway && Math.abs(angleDif) < DEFAULT_FACING_THRESHHOLD)
                || (turnAway && Math.abs(angleDif) > 180 - DEFAULT_FACING_THRESHHOLD))
            return null;

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
        return turnAway(VectorUtils.getAngle(missile.getLocation(), location));
    }
    public ShipCommand turnAway(CombatEntityAPI entity) {
        return turnAway(entity.getLocation());
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