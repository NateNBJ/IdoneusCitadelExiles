package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.tools.IntervalTracker;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class PhaseShiftStats implements ShipSystemStatsScript {
    public static final int ZAPS_BETWEEN_REFRESH = 10;
    public static final float EMP_ENERGY_DAMAGE = 50f;
    public static final float EMP_EMP_DAMAGE = 50f;
    public static final float EMP_THICKNESS = 1f;
    public static final Color EMP_COLOR = new Color(0, 255, 220);
    public static final Color EMP_POP_COLOR = new Color(124, 255, 233, 64);
    
    IntervalTracker zapTimer = new IntervalTracker(0.1f, 0.3f);
    ArrayList<CombatEntityAPI> targets = new ArrayList();
    Random rand = new Random();
    int zaps = 0;
//    Vector2f destination = null;
//    Vector2f origin = null;
    

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI)stats.getEntity();
        
        if(zapTimer.intervalElapsed()) {
            float range = ship.getCollisionRadius() * 1.5f;
            Vector2f point = MathUtils.getRandomPointInCircle(ship.getLocation(),
                    ship.getCollisionRadius() * 0.7f);
            
            if(zaps % ZAPS_BETWEEN_REFRESH == 0) {
                targets.clear();
                targets.addAll(AIUtils.getNearbyEnemyMissiles(ship, range));
                targets.addAll(AIUtils.getNearbyEnemyMissiles(ship, range));
            }
            
            if(!targets.isEmpty()) {
                CombatEntityAPI target = targets.get(rand.nextInt(targets.size()));

                Global.getCombatEngine().spawnEmpArc(ship, point, ship, target,
                        DamageType.ENERGY, EMP_ENERGY_DAMAGE, EMP_EMP_DAMAGE, range,
                        null, EMP_THICKNESS, EMP_POP_COLOR, EMP_COLOR);
            }
        }
        
        if (state == ShipSystemStatsScript.State.OUT) {
            stats.getMaxTurnRate().unmodify(id);
            float sign = Math.signum(ship.getAngularVelocity());
            float from = Math.abs(ship.getAngularVelocity());
            float to = ship.getMutableStats().getMaxTurnRate().getModifiedValue();
            ship.setAngularVelocity((from * 1.9f + to * 0.1f) / 2 * sign);
            
        } else {
            stats.getTurnAcceleration().modifyFlat(id, 100 * effectLevel);
            stats.getMaxTurnRate().modifyFlat(id, 75 * effectLevel);
            
//
//        //ship.getSpriteAPI().setAlphaMult(1 - effectLevel);
//
//        if(destination == null) {
//            origin = new Vector2f(ship.getLocation());
//            destination = new Vector2f(ship.getMouseTarget());
//        }
//        
//        //ship.getLocation().set(SunUtils.getMidpoint(origin, destination, effectLevel));
//
//        if(effectLevel >= 1) {
//            //ship.setFacing(VectorUtils.getAngle(destination, ship.getMouseTarget()));
//            //ship.getLocation().set(destination);
//            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
//        }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        //destination = origin = null;
	stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("increased turn speed", false);
        }
        return null;
    }
}
