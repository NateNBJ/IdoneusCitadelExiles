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
    static final float ANGLE_FORCE_MULTIPLIER = 10f;
    static final float VELOCITY_FORCE_MULTIPLIER = 1000f;
    static final float MAX_RANGE_MULTIPLIER = 12f;
    
    static final Color SPARK_COLOR = new Color(220, 255, 0);
    static final float SPARK_DURATION = 0.6f;
    static final float SPARK_BRIGHTNESS = 1.8f;
    static final float SPARK_RADIUS = 9f;

    ShipAPI ship;
    CombatEngineAPI engine;
    JauntSession session;
    List<DamagingProjectileAPI> absorbed = new LinkedList();
    
    boolean shouldDenyPhase() {
        if(ship.getShipAI() == null) return false;
        
        return JauntSession.hasSession(ship)
                || ship.getFluxTracker().getFluxLevel() > 0.85f
                || SunUtils.estimateIncomingBeamDamage(ship, 1) < 50;
    }
    void absorbProjectile(DamagingProjectileAPI proj) {
        if (proj instanceof MissileAPI) {
            SunUtils.destroy(proj);
            return;
        }
        
        if(proj.getWeapon() != null) absorbed.add(proj);
        
        float sparkAngle = VectorUtils.getAngle(proj.getLocation(), ship.getLocation());
        sparkAngle *= Math.PI / 180f;
        Vector2f sparkVect = new Vector2f((float) Math.cos(sparkAngle), (float) Math.sin(sparkAngle));
        float distance = MathUtils.getDistance(proj, ship);
        float visualEffect = 1;

        sparkVect.scale(3 * distance / SPARK_DURATION);

        //Global.getSoundPlayer().playSound("system_scloak_absorb", 1, visualEffect, proj.getLocation(), sparkVect);

        engine.addHitParticle(proj.getLocation(), sparkVect, SPARK_RADIUS * visualEffect + SPARK_RADIUS, SPARK_BRIGHTNESS, SPARK_DURATION, SPARK_COLOR);
        engine.removeEntity(proj);
    }
    void suckInProjectile(DamagingProjectileAPI proj, State state, float effectLevel) {
        float fromToAngle = VectorUtils.getAngle(ship.getLocation(), proj.getLocation());
        float aheadOfNess = 1 - Math.abs(MathUtils.getShortestRotation(fromToAngle, ship.getFacing())) / 180;
        float angleDif = MathUtils.getShortestRotation(fromToAngle, MathUtils.clampAngle(proj.getFacing() + 180));
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        float distance = MathUtils.getDistance(ship.getLocation(), proj.getLocation());
        float force = (ship.getCollisionRadius() / distance) * effectLevel * (float)Math.pow(aheadOfNess, 2);
        float dAngle = angleDif * amount * force * ANGLE_FORCE_MULTIPLIER;
        fromToAngle = (float)Math.toRadians(fromToAngle);
        Vector2f speedUp = new Vector2f(
                (float) Math.cos(fromToAngle) * amount,
                (float) Math.sin(fromToAngle) * amount);
        speedUp.scale(force * VELOCITY_FORCE_MULTIPLIER);

        if (state != State.OUT) {
            dAngle = -dAngle;
            speedUp.scale(-1);
        }

        Vector2f.add(proj.getVelocity(), speedUp, proj.getVelocity());
        VectorUtils.rotate(proj.getVelocity(), dAngle, proj.getVelocity());
        proj.setFacing(MathUtils.clampAngle(proj.getFacing() + dAngle));
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
        
        if(state == ShipSystemStatsScript.State.IN
                && (session == null || session.isReturning())) {
            session = JauntSession.getSession(ship);
            session.stopGoingHome();
        } else if(state == ShipSystemStatsScript.State.OUT && session != null) {
            session.goHome();
        }

        if (state == State.OUT) {
            stats.getMaxSpeed().unmodify(id);
            
            if(!ship.getFluxTracker().isOverloadedOrVenting()) {
                releaseProjectiles();
            } else {
                absorbed.clear();
            }
        } else {
            stats.getMaxSpeed().modifyFlat(id, 150f * effectLevel);
            stats.getAcceleration().modifyFlat(id, 300f * effectLevel);
            stats.getDeceleration().modifyFlat(id, 150f * effectLevel);
        
            List<DamagingProjectileAPI> ordnance = new LinkedList();
            ordnance.addAll(CombatUtils.getProjectilesWithinRange(ship.getLocation(), ship.getCollisionRadius() * MAX_RANGE_MULTIPLIER));
            ordnance.addAll(CombatUtils.getMissilesWithinRange(ship.getLocation(), ship.getCollisionRadius() * MAX_RANGE_MULTIPLIER));

            for (DamagingProjectileAPI proj : ordnance) {
                if(proj == null || proj.getProjectileSpecId() == null
                        || proj.getProjectileSpecId().endsWith("_doppelganger")) {
                    continue;
                } else if(state != State.OUT && MathUtils.getDistance(ship, proj) <= 0) {
                    absorbProjectile(proj);
                } else {
                    suckInProjectile(proj, state, effectLevel);
                }
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        
        if(session != null) session.goHome();
        session = null;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
//        if (index == 0) {
//            return new StatusData("increased speed", false);
//        }
        return null;
    }
}
