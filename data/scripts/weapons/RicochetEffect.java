package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import java.util.Iterator;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;

public class RicochetEffect implements EveryFrameWeaponEffectPlugin {

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        
        for(Iterator p = engine.getProjectiles().iterator(); p.hasNext();) {
            DamagingProjectileAPI proj = (DamagingProjectileAPI)p.next();

            if(proj.getWeapon() != weapon) continue;

            for(Iterator s = engine.getShips().iterator(); s.hasNext();) {
                ShipAPI target = (ShipAPI)s.next();
                ShieldAPI shield = target.getShield();
                
                if(!CollisionUtils.isPointWithinCollisionCircle(proj.getLocation(), target)
                        || shield == null
                        || shield.isOff()
                        || !shield.isWithinArc(proj.getLocation())
                        || MathUtils.getDistance(proj, shield.getLocation()) >= shield.getRadius() - 10)
                    continue;

//                if(DefenseUtils.getDefenseAtPoint(ship, proj.getLocation()) != DefenseType.SHIELD)
//                    continue;
                    
                float angle = VectorUtils.getAngle(proj.getLocation(), shield.getLocation());
                float shortestRotation = MathUtils.getShortestRotation(proj.getFacing(), angle);

                if(Math.abs(shortestRotation) > 90) continue;

                float newAngle = MathUtils.clampAngle(proj.getFacing() + shortestRotation * 2);

                newAngle = MathUtils.clampAngle(proj.getFacing() + 180);

                engine.spawnProjectile(ship, weapon, weapon.getId(),
                        proj.getLocation(), newAngle, ship.getVelocity());

                engine.applyDamage(target, proj.getLocation(),
                        proj.getDamageAmount(), proj.getDamageType(),
                        proj.getEmpAmount(), false, true, ship);
                
                engine.removeEntity(proj);
            }
        }
    }
}