package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.tools.JauntSession;
import data.tools.SunUtils;
import java.awt.Color;
import java.util.LinkedList;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class AdvPhaseWarpStats implements ShipSystemStatsScript {
    static final float ANGLE_FORCE_MULTIPLIER = 3.5f;
    static final float VELOCITY_FORCE_MULTIPLIER = 350f;
    static final float MAX_RANGE_MULTIPLIER = 10f;
    
    static final Color SPARK_COLOR = new Color(220, 255, 0);
    static final float SPARK_DURATION = 0.6f;
    static final float SPARK_BRIGHTNESS = 1.8f;
    static final float SPARK_RADIUS = 9f;

    ShipAPI ship;
    CombatEngineAPI engine;
    List<DamagingProjectileAPI> absorbed = new LinkedList();
    List<DamagingProjectileAPI> ordnance;
    Vector2f wormhole;
    
    void absorbProjectile(DamagingProjectileAPI proj) {
        if(!engine.isEntityInPlay(proj)) return;
        
        ship.getFluxTracker().setCurrFlux(Math.max(ship.getFluxTracker().getHardFlux(), ship.getFluxTracker().getCurrFlux() - proj.getDamageAmount()));
        
        if (proj instanceof MissileAPI) {
            SunUtils.destroy(proj);
            return;
        }
        
        //if(proj.getWeapon() != null) absorbed.add(proj);
        
        float sparkAngle = VectorUtils.getAngle(proj.getLocation(), ship.getLocation());
        sparkAngle *= Math.PI / 180f;
        Vector2f sparkVect = new Vector2f((float) Math.cos(sparkAngle), (float) Math.sin(sparkAngle));
        float distance = MathUtils.getDistance(proj, ship);
        float visualEffect = 1;

        sparkVect.scale(3 * distance / SPARK_DURATION);

        //Global.getSoundPlayer().playSound("system_scloak_absorb", 1, visualEffect, proj.getLocation(), sparkVect);

        //engine.addHitParticle(proj.getLocation(), sparkVect, SPARK_RADIUS * visualEffect + SPARK_RADIUS, SPARK_BRIGHTNESS, SPARK_DURATION, SPARK_COLOR);
        engine.removeEntity(proj);
    }
    void suckInProjectile(DamagingProjectileAPI proj) {
        float fromToAngle = VectorUtils.getAngle(wormhole, proj.getLocation());
        float angleDif = MathUtils.getShortestRotation(fromToAngle, MathUtils.clampAngle(proj.getFacing() + 180));
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        float distance = MathUtils.getDistance(wormhole, proj.getLocation());
        float force = (ship.getCollisionRadius() * MAX_RANGE_MULTIPLIER) / distance;
        float dAngle = -angleDif * amount * force * ANGLE_FORCE_MULTIPLIER;
        fromToAngle = (float)Math.toRadians(fromToAngle);
        Vector2f speedUp = new Vector2f(
                (float) Math.cos(fromToAngle) * amount,
                (float) Math.sin(fromToAngle) * amount);
        speedUp.scale(-force * VELOCITY_FORCE_MULTIPLIER);

        Vector2f.add(proj.getVelocity(), speedUp, proj.getVelocity());
        VectorUtils.rotate(proj.getVelocity(), dAngle, proj.getVelocity());
        proj.setFacing(MathUtils.clampAngle(proj.getFacing() + dAngle));
        
        SunUtils.blink(wormhole);
    }
    void releaseProjectiles() {
        for(DamagingProjectileAPI proj : absorbed) {
            CombatEntityAPI created = engine.spawnProjectile(ship, null, proj.getWeapon().getId(),
                    ship.getLocation(), ship.getFacing() + ((float)Math.random() - 0.5f) * 30,
                    ship.getVelocity());
            
            created.getVelocity().scale((float)Math.random() * 0.4f + 0.6f);
        }
        
        // TODO - play sound
        
        absorbed.clear();
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ship = (ShipAPI)stats.getEntity();
        engine = Global.getCombatEngine();

        stats.getMaxSpeed().modifyFlat(id, 250f * effectLevel);
        stats.getAcceleration().modifyFlat(id, 400f * effectLevel);
        stats.getDeceleration().modifyFlat(id, 200f * effectLevel);
        stats.getFluxDissipation().modifyMult(id, 0);
        
        if(wormhole == null) {
            wormhole = new Vector2f(ship.getLocation());
            ordnance = new LinkedList();
            ordnance.addAll(CombatUtils.getProjectilesWithinRange(wormhole, ship.getCollisionRadius() * MAX_RANGE_MULTIPLIER));
            ordnance.addAll(CombatUtils.getMissilesWithinRange(wormhole, ship.getCollisionRadius() * MAX_RANGE_MULTIPLIER));
        }

        for (DamagingProjectileAPI proj : ordnance) {
            if(proj == null || proj.getProjectileSpecId() == null
                    || proj.getProjectileSpecId().endsWith("_doppelganger")) {
                continue;
            } else if(MathUtils.getDistance(proj, wormhole) <= ship.getCollisionRadius() / 2) {
                absorbProjectile(proj);
            } else {
                suckInProjectile(proj);
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getFluxDissipation().unmodify(id);
        
        wormhole = null;
        ordnance = null;
        if(JauntSession.hasSession(ship)) ship.useSystem();
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
//        if (index == 0) {
//            return new StatusData("increased speed", false);
//        }
        return null;
    }
}
