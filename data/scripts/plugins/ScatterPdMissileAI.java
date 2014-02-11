
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import java.util.Random;

public class ScatterPdMissileAI implements MissileAIPlugin {
    MissileAPI missile;
    ShipCommand direction;
    ShipCommand strafe;
    float lifeTime;

    static final Random rand = new Random();

    public ScatterPdMissileAI() {}
    public ScatterPdMissileAI(MissileAPI missile) {
        this.missile = missile;
        this.lifeTime = rand.nextFloat() * 1.3f + 0.2f;
        this.direction = (rand.nextFloat() < 0.5)
                ? ShipCommand.TURN_LEFT
                : ShipCommand.TURN_RIGHT;
        this.strafe = (direction == ShipCommand.TURN_LEFT)
                ? ShipCommand.STRAFE_LEFT
                : ShipCommand.STRAFE_RIGHT;
    }

    @Override
    public void advance(float amount) {
        missile.giveCommand(direction);
        missile.giveCommand(strafe);
        missile.giveCommand(ShipCommand.ACCELERATE);

        lifeTime -= amount;

        if(missile.isFizzling() || lifeTime < 0) {
            Global.getCombatEngine().applyDamage(missile, missile.getLocation(),
                    999, DamageType.FRAGMENTATION, 0, true, true,
                    missile.getSource());


            Global.getCombatEngine().spawnProjectile(missile.getSource(),
                    null, "sun_ice_scatterpdhack",
                    missile.getLocation(), missile.getFacing(),
                    missile.getVelocity());
        }
    }

}
