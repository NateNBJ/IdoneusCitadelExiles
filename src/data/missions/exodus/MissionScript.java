package data.missions.exodus;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class MissionScript implements EveryFrameCombatPlugin {
    boolean isFirstFrame = true;
    
    CombatEngineAPI engine;
    
    static final Vector2f escortOffset = new Vector2f(6000, -1000);
    static final Set<String> escortGroup = new HashSet();
    static {
        escortGroup.add("sun_ice_shalom");
        escortGroup.add("sun_ice_voidreaver");
        escortGroup.add("sun_ice_soulbane");
//        escortGroup.add("sun_ice_shalom");
//        escortGroup.add("sun_ice_shalom");
//        escortGroup.add("sun_ice_shalom");
//        escortGroup.add("sun_ice_shalom");
//        escortGroup.add("sun_ice_shalom");
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if(isFirstFrame) {
            for(ShipAPI ship : AIUtils.getAlliesOnMap(engine.getPlayerShip())) {
                if(escortGroup.contains(ship.getHullSpec().getHullId())) {
                    Vector2f.add(ship.getLocation(), escortOffset, ship.getLocation());
                } else {
                    Vector2f.sub(ship.getLocation(), escortOffset, ship.getLocation());
                }
            }
            
            Vector2f.sub(engine.getPlayerShip().getLocation(), escortOffset, engine.getPlayerShip().getLocation());
            
            isFirstFrame = false;
        }
        

        //engine.getFleetManager(FleetSide.ENEMY).
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }
    
}
