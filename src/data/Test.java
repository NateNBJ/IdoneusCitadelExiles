package data;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.List;

public class Test implements EveryFrameCombatPlugin {
    CombatEngineAPI engine;
    ShipAPI lastFlagship;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if(engine.getPlayerShip().isShuttlePod()) {
            //SunUtils.setLocation(engine.getPlayerShip(), );
            //SunUtils.print("merp");
            
            engine.getPlayerShip().getSpriteAPI().setSize(0, 0);
            engine.getPlayerShip().getMutableStats().getAcceleration().modifyMult("r", 10);
            engine.getPlayerShip().getMutableStats().getDeceleration().modifyMult("r", 10);
            engine.getPlayerShip().getMutableStats().getMaxSpeed().modifyMult("r", 10);
            engine.getPlayerShip().getMutableStats().getTurnAcceleration().modifyMult("r", 10);
            engine.getPlayerShip().getMutableStats().getMaxTurnRate().modifyMult("r", 10);
        }
        
        
        
        
        
        for(ShipAPI ship : engine.getShips()) {
//            if(ship.getFluxTracker().isVenting()) continue;
//            
//            ship.getFluxTracker().setHardFlux(1000);
//            ship.getFluxTracker().setCurrFlux(1500);
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }
    
}
