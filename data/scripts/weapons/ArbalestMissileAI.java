
package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;

public class ArbalestMissileAI implements MissileAIPlugin {
    static final float[] STAGE_DURATION = { 3, 1.5f, 3600 };
    static final String SOUND_ID = "engine_accelerate";
    static final float SOUND_PITCH = 0.5f;
    static final float SOUND_VOLUME = 3.0f;
    static final float FAKE_TURN_MODIFIER = 0.1f;
    
    MissileAPI missile;
    int stage = 0; // 0:Drift, 1:Burn, 2:Cruise
    ShipAPI target;
    float duration = STAGE_DURATION[stage];
    int tick = 0;

    public ArbalestMissileAI() {}
    public ArbalestMissileAI(MissileAPI missile) {
        this.missile = missile;
    }

    public void findTarget() {
        target = missile.getSource().getShipTarget();
        
        if(target == null
                || !target.isAlive()
                || target.getOwner() == missile.getOwner()) {
            target = AIUtils.getNearestEnemy(missile);
        }
    }
    public void turn() {
        float degreeAngle = VectorUtils.getAngle(missile.getLocation(), target.getLocation());
    
        float angleDif = MathUtils.getShortestRotation(missile.getFacing(), degreeAngle);

        //if(Math.abs(angleDif) < 2) return;

        if(angleDif > 0) {
            missile.giveCommand(ShipCommand.TURN_LEFT);
            //missile.giveCommand(ShipCommand.STRAFE_LEFT);
        } else {
            missile.giveCommand(ShipCommand.TURN_RIGHT);
            //missile.giveCommand(ShipCommand.STRAFE_RIGHT);
        }
    }
    
    @Override
    public void advance(float amount) {
        if(target == null) {
            findTarget();
            return;
        }

        duration -= amount;

        if(duration <= 0) {
            ++stage;
            duration += STAGE_DURATION[stage];

            if(stage == 1) {
                Global.getSoundPlayer().playSound(SOUND_ID, SOUND_PITCH,
                        SOUND_VOLUME, missile.getLocation(), missile.getVelocity());
            }
        }

        if(stage == 1) {
            missile.giveCommand(ShipCommand.ACCELERATE);
        } else if(stage < 2) {
            turn();
        } else if(stage == 2) {
            float angleDif = MathUtils.getShortestRotation(missile.getFacing(),
                    VectorUtils.getAngle(missile.getLocation(), target.getLocation()));

            float dAngle = Math.signum(angleDif) * FAKE_TURN_MODIFIER * amount;

            VectorUtils.rotate(missile.getVelocity(), dAngle, missile.getVelocity());
            missile.setFacing(MathUtils.clampAngle(missile.getFacing() + dAngle * (float)(180 / Math.PI)));
        }
    }

}
