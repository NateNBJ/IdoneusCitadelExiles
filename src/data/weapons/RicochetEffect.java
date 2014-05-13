package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
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

    void hit(DamagingProjectileAPI proj, CombatEntityAPI target) {
        Global.getCombatEngine().applyDamage(target, proj.getLocation(),
                proj.getDamageAmount(), proj.getDamageType(),
                proj.getEmpAmount(), false, true, proj.getSource());

        Global.getCombatEngine().removeEntity(proj);
    }

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
                        || target == proj.getSource())
//                        || shield == null
//                        || shield.isOff()
//                        || !shield.isWithinArc(proj.getLocation())
//                        || MathUtils.getDistance(proj, shield.getLocation()) > shield.getRadius() * 0.9f)
                    continue;
                
                if (shield != null && !shield.isOff()
                        && shield.isWithinArc(proj.getLocation())
                        && MathUtils.getDistance(proj, shield.getLocation()) < shield.getRadius() * 0.9f) {

                    float angle = VectorUtils.getAngle(shield.getLocation(), proj.getLocation());
                    float shortestRotation = MathUtils.getShortestRotation(proj.getFacing(), angle);
                    if(Math.abs(shortestRotation) < 90) continue;

                    float newAngle = MathUtils.clampAngle(proj.getFacing() + shortestRotation * 2 + 180);

                    engine.spawnProjectile(ship, weapon, weapon.getId(),
                            proj.getLocation(), newAngle, ship.getVelocity());

                    hit(proj, target);

//                    engine.applyDamage(target, proj.getLocation(),
//                            proj.getDamageAmount(), proj.getDamageType(),
//                            proj.getEmpAmount(), false, true, ship);
//
//                    engine.removeEntity(proj);
                } else if (CollisionUtils.isPointWithinBounds(proj.getLocation(), target)) {
                    hit(proj, target);
                }
            }

            for(Iterator s = engine.getAsteroids().iterator(); s.hasNext();) {
                CombatEntityAPI target = (CombatEntityAPI)s.next();

                if(CollisionUtils.isPointWithinCollisionCircle(proj.getLocation(), target)) {
                    hit(proj, target);
                }
            }
        }
    }
}