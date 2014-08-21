package data.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.tools.IntervalTracker;
import java.util.Iterator;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;

public class FissionDrillAI implements ShipSystemAIScript
{
    IntervalTracker timer = new IntervalTracker(0.3f);
    float timeOfNextRefresh = 0;
    ShipSystemAPI system;
    ShipAPI ship;
    ShipAPI victim;
    float timeOfTargetAquisition;
    boolean retreating = false;

    float getScore(ShipAPI self, ShipAPI victim) {
        if(!victim.isAlive()) return 0;
        
        return Math.max(0, (victim.getCollisionRadius() - 0) / MathUtils.getDistance(self, victim));
    }
    
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        float time = Global.getCombatEngine().getTotalElapsedTime(false);

        if(system.isActive() || timer.intervalElapsed()) {
            WeaponAPI drill = (WeaponAPI)ship.getAllWeapons().get(2);

            // Can we get a better target?
            List candidates = WeaponUtils.getEnemiesInArc(drill);

            if(candidates.isEmpty()) timeOfTargetAquisition = time;

            float score = (victim == null || !candidates.contains(victim)) ? 0 : getScore(ship, victim);

            for(Iterator iter = candidates.iterator(); iter.hasNext();) {
                ShipAPI newVictim = (ShipAPI)iter.next();
                float newScore = getScore(ship, newVictim);

                if(newScore > 0 && newScore > score) {
                    victim = newVictim;
                    score = newScore;
                    timeOfTargetAquisition = time;
                }
            }


            // Nothing to kill...
            if(victim == null) return;

            boolean wantActive = (
                        !drill.isDisabled() && score > 0
                        && time - timeOfTargetAquisition > 1)
                    || (
                        system.isActive()
                        && MathUtils.getDistance(ship, victim) < ship.getCollisionRadius() + victim.getCollisionRadius() + 300);

            // Prevent ship from strafing before activation.
            if(!system.isActive() && time != timeOfTargetAquisition) {
                ship.getMutableStats().getMaxSpeed().modifyMult("preventStrafeHack", 0.2f);
            } else ship.getMutableStats().getMaxSpeed().unmodify("preventStrafeHack");

            if(system.isActive() && !wantActive) {
                ship.useSystem(); // Turn off
                victim = null;
            } else if(!system.isActive() && wantActive && ship.getFluxTracker().getFluxLevel() < 0.5f) {
                ship.useSystem(); // Turn on
            }else if(system.isActive()) {
                Vector2f to = victim.getLocation();

                float angleDif = MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), to));
                ShipCommand direction = (angleDif > 0) ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT;
                ship.giveCommand(direction, to, 0);
            }
        }
    }
}
