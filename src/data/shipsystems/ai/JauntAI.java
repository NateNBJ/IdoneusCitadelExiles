package data.shipsystems.ai;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import data.shipsystems.JauntHeavyStats;
import data.tools.IntervalTracker;
import data.tools.IceUtils;
import java.util.Map;
import java.util.WeakHashMap;
import org.lwjgl.util.vector.Vector2f;

public class JauntAI implements ShipSystemAIScript {
    static final float USE_THRESHHOLD = 100.0f;
    static final float ACTIVATION_SECONDS = 3f;
    //static final float MAX_AMMO = 4.0f;
    
    ShipSystemAPI system;
    ShipAPI ship;
    IntervalTracker timer = new IntervalTracker(0.1f, 0.7f);
    static Map<ShipAPI, Vector2f> origins = new WeakHashMap();
    int ticksWithoutDissipation = 0;
    
    public static void setOrigin(ShipAPI ship, Vector2f origin) {
        origins.put(ship, origin);
    }
    
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        FluxTrackerAPI reactor = ship.getFluxTracker();
        
        if(!timer.intervalElapsed() || ship == null
                || system.getAmmo() == 0
                || system.isCoolingDown()
                || reactor.isOverloadedOrVenting()
                || ship.getCollisionClass() == CollisionClass.NONE)
            return;
        
        float flux, phaseNecessity = 0;
        float damage = IceUtils.estimateIncomingDamage(ship, 1) * 1.0f;
        float armor = (float)Math.pow(IceUtils.getArmorPercent(ship), 3);
        //flux = (float)Math.sqrt(reactor.getFluxLevel());
        boolean noSoftFlux = reactor.getCurrFlux() == reactor.getHardFlux();
        boolean canTurn = IceUtils.getEngineFractionDisabled(ship) > 0;
        
        if(system.isOn()) {
            flux = 0;
            phaseNecessity += reactor.getFluxLevel() * USE_THRESHHOLD * 1.25f;
            //flux = (float)Math.pow(reactor.getFluxLevel(), 1);
            damage *= 0.5f;
            
            // No reason to stay here if we're not shooting anything or dissipating soft flux
            if(noSoftFlux) {
                ++ticksWithoutDissipation;
            } else ticksWithoutDissipation = 0;
            
            phaseNecessity += 20 * ticksWithoutDissipation;
            
            // Don't want to return if it's dangerous
            Vector2f temp = new Vector2f(ship.getLocation());
            ship.getLocation().set(origins.get(ship));
            damage -= IceUtils.estimateIncomingDamage(ship, 2);
            ship.getLocation().set(temp);
        } else {
            flux = (float)Math.sqrt(reactor.getHardFlux() / reactor.getMaxFlux());
            ticksWithoutDissipation = 0;
            
            // Prevent from using when it doesn't have enough flux to use
            if(reactor.getCurrFlux() > reactor.getMaxFlux()
                    - system.getFluxPerSecond() * ACTIVATION_SECONDS)
                return;
            
            // Check if we're in a good position to attack a distant target remotely
            if(reactor.getFluxLevel() < 0.7f) {
                int enemy = (ship.getOwner() + 1) % 2;
                float range = IceUtils.estimateOptimalRange(ship) * 0.8f;
                float fp = IceUtils.getFPStrength(ship);
                float hostilityInEminentRange = IceUtils.getStrengthInArea(
                        ship.getLocation(), range, enemy);
                float hostilityInRemoteRange = Math.min(fp, IceUtils.getStrengthInArea(
                        ship.getLocation(), range + JauntHeavyStats.MAX_RANGE,
                        enemy) - hostilityInEminentRange);
                boolean canJauntToBetterTargets = hostilityInEminentRange < fp / 6
                        && hostilityInEminentRange < hostilityInRemoteRange;

                if(canJauntToBetterTargets || !canTurn) {
                    phaseNecessity += USE_THRESHHOLD * 1.6f * (1 - reactor.getFluxLevel());
                }
            }
            
            //phaseNecessity -= USE_THRESHHOLD * (1 - system.getAmmo() / MAX_AMMO);
        }
        
        phaseNecessity += (damage * (1.2f - armor)) * (1 - flux);
        
        
        
        if(phaseNecessity >= USE_THRESHHOLD) {
            //SunUtils.print(ship, "" + phaseNecessity);
            ship.useSystem();
        }
    }
}
