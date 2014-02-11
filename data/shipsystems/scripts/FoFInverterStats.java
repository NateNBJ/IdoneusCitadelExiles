package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

public class FoFInverterStats implements ShipSystemStatsScript
{
    private static float RANGE = 1200f;
    private static float MAX_MISSILE_HP = 500f;
    private final Color GLOW_COLOR = new Color(255, 191, 0);
    private List missiles = null;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        ShipAPI ship = (ShipAPI)stats.getEntity();

        //if (ship == null) return;
        

        if(missiles == null) missiles = AIUtils.getNearbyEnemyMissiles(ship, RANGE);

        if(missiles.isEmpty()) return;
        
        MissileAPI missile = (MissileAPI)missiles.get((int)Math.floor(Math.pow(Math.random(), 3) * missiles.size()));
        float ecmChance = 0.25f;
        ecmChance *= 1f - Math.min(MAX_MISSILE_HP, missile.getHitpoints()) / (MAX_MISSILE_HP * 1.2f);
        ecmChance *= (missile.getSource().getVariant().getHullMods().contains("eccm")) ?
            0.25f : 1;
        
        if(Math.random() < ecmChance) {
            missile.setSource(ship);
            missile.setOwner(ship.getOwner());
            Global.getCombatEngine().addSmoothParticle(missile.getLocation(), missile.getVelocity(), missile.getHitpoints(), 0.8f, 0.8f, GLOW_COLOR);
            Global.getSoundPlayer().playSound("collision_asteroids", 1, missile.getHitpoints() / 30f, missile.getLocation(), missile.getVelocity());
            missiles.remove(missile);
        }
    }
    public void _apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        ShipAPI ship = (ShipAPI)stats.getEntity();

        if (ship == null) return;

        DamagingProjectileAPI proj;
        MissileAPI missile = null;

        for (Iterator iter = Global.getCombatEngine().getProjectiles().iterator(); iter.hasNext();) {
            proj = (DamagingProjectileAPI) iter.next();


            if(!(proj instanceof MissileAPI)) continue; // Ignore non-missiles
            if(proj.getOwner() == ship.getOwner()) continue; // Ignore friendly projectiles
            if(MathUtils.getDistance(proj, ship) > RANGE) continue; // Ignore too-distant projectiles

            missile = (MissileAPI)proj;

            // 50% chance to fail against missiles with ECCM
            if(missile.getSource().getVariant().getHullMods().contains("eccm") && (Math.random() < 0.5)) continue;

            missile.setSource(ship);
            missile.setOwner(ship.getOwner());
            Global.getCombatEngine().addSmoothParticle(missile.getLocation(), missile.getVelocity(), (missile.getHitpoints() + missile.getDamageAmount()) / 5f, 0.8f, 0.8f, GLOW_COLOR);

        }

        if(missile != null)
            Global.getSoundPlayer().playSound("collision_asteroids", 1, 1, missile.getLocation(), missile.getVelocity());
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        missiles = null;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }
}