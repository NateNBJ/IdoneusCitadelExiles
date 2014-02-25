package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class SunUtils
{
    private static final Map baseOverloadTimes = new HashMap();
    private static final float SAFE_DISTANCE = 600f;
    static {
        baseOverloadTimes.put(HullSize.FIGHTER, 10f);
        baseOverloadTimes.put(HullSize.FRIGATE,  4f);
        baseOverloadTimes.put(HullSize.DESTROYER, 6f);
        baseOverloadTimes.put(HullSize.CRUISER, 8f);
        baseOverloadTimes.put(HullSize.CAPITAL_SHIP, 10f);
        baseOverloadTimes.put(HullSize.DEFAULT, 6f);
    }

    public static Vector2f getCellLocation(ShipAPI ship, float x, float y) {
        x -= ship.getArmorGrid().getGrid().length / 2f;
        y -= ship.getArmorGrid().getGrid()[0].length / 2f;
        float cellSize = ship.getArmorGrid().getCellSize();
        Vector2f cellLoc = new Vector2f();
        float theta = (float)(((ship.getFacing() - 90) / 350f) * (Math.PI * 2));
        cellLoc.x = (float)(x * Math.cos(theta) - y * Math.sin(theta)) * cellSize + ship.getLocation().x;
        cellLoc.y = (float)(x * Math.sin(theta) + y * Math.cos(theta)) * cellSize + ship.getLocation().y;

        return cellLoc;
    }
    public static void print(String str) {

        CombatEngineAPI engine = Global.getCombatEngine();
        ShipAPI ship = engine.getPlayerShip();

        if(ship != null)
            Global.getCombatEngine().addFloatingText(ship.getLocation(), str, 40, Color.green, ship, 1, 5);
    }
    public static float estimateIncomingDamage(ShipAPI ship) {
        float accumulator = 0f;
        DamagingProjectileAPI proj;
        
        for (Iterator iter = Global.getCombatEngine().getProjectiles().iterator(); iter.hasNext();) {
            proj = (DamagingProjectileAPI) iter.next();
            
            if(proj.getOwner() == ship.getOwner()) continue; // Ignore friendly projectiles

            float safeDistance = SAFE_DISTANCE + ship.getCollisionRadius();
            float threat = proj.getDamageAmount();
            
            if(ship.getShield() != null && ship.getShield().isWithinArc(proj.getLocation()))
                continue;

            accumulator += threat * Math.max(0, Math.min(1, Math.pow(1 - MathUtils.getDistance(proj, ship) / safeDistance, 2)));
        }
        
        return accumulator;
    }
    public static float estimateIncomingMissileDamage(ShipAPI ship) {
        float accumulator = 0f;
        DamagingProjectileAPI missile;

        for (Iterator iter = Global.getCombatEngine().getMissiles().iterator(); iter.hasNext();) {
            missile = (DamagingProjectileAPI) iter.next();

            if(missile.getOwner() == ship.getOwner()) continue; // Ignore friendly missiles

            float safeDistance = SAFE_DISTANCE + ship.getCollisionRadius();
            float threat = missile.getDamageAmount();

            if(ship.getShield() != null && ship.getShield().isWithinArc(missile.getLocation()))
                continue;

            accumulator += threat * Math.max(0, Math.min(1, Math.pow(1 - MathUtils.getDistance(missile, ship) / safeDistance, 2)));
        }

        return accumulator;
    }
    public static float getHitChance(WeaponAPI weapon, CombatEntityAPI target) {
        float estTimeTilHit = MathUtils.getDistance(target, weapon.getLocation())
                / weapon.getProjectileSpeed();

        Vector2f estTargetPosChange = new Vector2f(
                target.getVelocity().x * estTimeTilHit,
                target.getVelocity().y * estTimeTilHit);

        float estFacingChange = target.getAngularVelocity() * estTimeTilHit;

        double theta = weapon.getCurrAngle() * (Math.PI / 180);
        Vector2f projVelocity = new Vector2f(
                (float)Math.cos(theta) * weapon.getProjectileSpeed() + weapon.getShip().getVelocity().x,
                (float)Math.sin(theta) * weapon.getProjectileSpeed() + weapon.getShip().getVelocity().y);

        target.setFacing(target.getFacing() + estFacingChange);
        Vector2f.add(target.getLocation(), estTargetPosChange, target.getLocation());

        projVelocity.scale(estTimeTilHit * 3);
        Vector2f.add(projVelocity, weapon.getLocation(), projVelocity);
        Vector2f estHitLoc = CollisionUtils.getCollisionPoint(weapon.getLocation(),
                projVelocity, target);
        
        target.setFacing(target.getFacing() - estFacingChange);
        Vector2f.add(target.getLocation(), (Vector2f)estTargetPosChange.scale(-1),target.getLocation());

        if(estHitLoc == null) return 0;

        return 1;
    }
    public static float getFPWorthOfSupport(ShipAPI ship, float range) {
        float retVal = 0;
        
        for(Iterator iter = AIUtils.getNearbyAllies(ship, range).iterator(); iter.hasNext();) {
            ShipAPI ally = (ShipAPI)iter.next();
            float colDist = ship.getCollisionRadius() + ally.getCollisionRadius();
            float distance = Math.max(0, MathUtils.getDistance(ship, ally) - colDist);
            float maxRange = Math.max(1, range - colDist);
            
            retVal += getFP(ally) * (1 - distance / maxRange);
        }
        
        return retVal;
    }
    public static float getFPWorthOfEnemies(ShipAPI ship, float range) {
        float retVal = 0;

        for(Iterator iter = AIUtils.getNearbyEnemies(ship, range).iterator(); iter.hasNext();) {
            ShipAPI enemy = (ShipAPI)iter.next();
            float colDist = ship.getCollisionRadius() + enemy.getCollisionRadius();
            float distance = Math.max(0, MathUtils.getDistance(ship, enemy) - colDist);
            float maxRange = Math.max(1, range - colDist);

            retVal += getFP(enemy) * (1 - distance / maxRange);
        }

        return retVal;
    }
    public static float getFP(ShipAPI ship) {
        return Global.getCombatEngine().getFleetManager(ship.getOwner()).getDeployedFleetMember(ship).getMember().getFleetPointCost();
    }
    public static float getBaseOverloadDuration(ShipAPI ship) {
        return (Float)baseOverloadTimes.get(ship.getHullSize());
    }
    public static float estimateOverloadDurationOnHit(ShipAPI ship, float damage, DamageType type) {
        if(ship.getShield() == null) return 0;

        float fluxDamage = damage * type.getShieldMult()
                * ship.getMutableStats().getShieldAbsorptionMult().getModifiedValue();
        fluxDamage += ship.getFluxTracker().getCurrFlux()
                - ship.getFluxTracker().getMaxFlux();
        
        if(fluxDamage <= 0) return 0;

        return Math.min(15, getBaseOverloadDuration(ship) + fluxDamage / 25);
    }
    public static float getLifeExpectancy(ShipAPI ship) {
        float damage = estimateIncomingDamage(ship);
        return (damage <= 0) ? 3600 : ship.getHitpoints() / damage;
    }
}