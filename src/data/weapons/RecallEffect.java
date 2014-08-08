package data.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.tools.SunUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class RecallEffect implements EveryFrameWeaponEffectPlugin {
    final float PADDING = 100;
    boolean onFireEffectIsReady = true;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
//        ShipAPI ship = weapon.getShip();
//        Vector2f mouse = engine.getPlayerShip().getMouseTarget();
//        Vector2f.sub(ship.getMouseTarget(), ship.getLocation(), mouse);
//        VectorUtils.rotate(mouse, ship.getFacing(), mouse);
//        weapon.getSprite().setCenter(mouse.x, mouse.y);
        
        if (onFireEffectIsReady && weapon.getCooldownRemaining() > 0) {
            ShipAPI ship = weapon.getShip();
            Vector2f mouse = ship.getMouseTarget();
            ShipAPI target = null;//ship.getShipTarget();
            int ammo = weapon.getAmmo() + 1;
            
            for(DamagingProjectileAPI proj : engine.getProjectiles()) {
                if(proj.getWeapon() == weapon) engine.removeEntity(proj);
            }
            
            float lowest = Float.MAX_VALUE;
            
            for(ShipAPI s : engine.getShips()) {
                float distance = MathUtils.getDistance(s, mouse);
                        
                if(!s.isAlive()
                        || s.getOwner() != ship.getOwner()
                        || s == ship
                        || s.isDrone()
                        || distance > s.getCollisionRadius() + 100
                        || distance >= lowest) continue;
                
                lowest = distance;
                target = s;
            }
            
            if(target != null) {
                ammo -= SunUtils.getFP(target);
                SunUtils.print(target, "RECALL!!! " + target.getHullSpec().getHullName());
                
                float range = ship.getCollisionRadius() + target.getCollisionRadius() + PADDING;
                
                Vector2f dest = VectorUtils.getDirectionalVector(ship.getLocation(), target.getLocation());
                dest.x *= range;
                dest.y *= range;
                Vector2f.add(dest, ship.getLocation(), dest);
                
                target.getVelocity().set(ship.getVelocity());
                target.getLocation().set(dest);
            } else {
                SunUtils.print(mouse, "Mouse over a friendly ship and use this weapon to recall it.");
            }
            
            weapon.setAmmo(ammo);

            onFireEffectIsReady = false;
        } else if (!onFireEffectIsReady && weapon.getCooldownRemaining() == 0) {
            onFireEffectIsReady = true;
        }
    }
}
