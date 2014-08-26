package data.shipsystems.ai;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import data.tools.IntervalTracker;
import data.tools.SunUtils;
import java.util.Map;
import java.util.WeakHashMap;
import org.lwjgl.util.vector.Vector2f;

public class JauntAI implements ShipSystemAIScript {
    static final float USE_THRESHHOLD = 100.0f;
    
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
        
        // TODO - Activate to approach distant target
        
        if(!timer.intervalElapsed() || ship == null
                || reactor.isOverloadedOrVenting()
                || ship.getCollisionClass() == CollisionClass.NONE)
            return;
        
        float phaseNecessity = 0;
        float flux = (float)Math.sqrt(reactor.getHardFlux() / reactor.getMaxFlux());
        float damage = SunUtils.estimateIncomingDamage(ship, 1) * 0.7f;
        float armor = (float)Math.pow(SunUtils.getArmorPercent(ship), 2);
        
        if(system.isOn()) {
            flux = 0;
            damage *= 0.2f;
            
            // No reason to stay here if we're not shooting anything or dissipating soft flux
            if(reactor.getCurrFlux() == reactor.getHardFlux()) {
                ++ticksWithoutDissipation;
            } else ticksWithoutDissipation = 0;
            
            phaseNecessity += 20 * ticksWithoutDissipation;
            
            // Don't want to return if it's dangerous
            Vector2f temp = new Vector2f(ship.getLocation());
            ship.getLocation().set(origins.get(ship));
            damage -= SunUtils.estimateIncomingBeamDamage(ship, 3);
            ship.getLocation().set(temp);
        } else ticksWithoutDissipation = 0;
        
        phaseNecessity += (damage * (1.2f - armor)) * (1 - flux);
        
        if(phaseNecessity >= USE_THRESHHOLD) {
            SunUtils.print(ship, "" + phaseNecessity);
            ship.useSystem();
        }
    }
}
