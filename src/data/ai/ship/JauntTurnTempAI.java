package data.ai.ship;

import com.fs.starfarer.api.combat.ShipAPI;
import data.tools.JauntSession;
import data.tools.SunUtils;
import org.lazywizard.lazylib.VectorUtils;

public class JauntTurnTempAI extends BaseShipAI {
    ShipAPI target;
    JauntSession jaunt;
    
    public JauntTurnTempAI(ShipAPI ship, ShipAPI target, JauntSession jaunt) {
        super(ship);
        this.target = target;
        this.jaunt = jaunt;
        
        SunUtils.print(ship, "on");
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        
            SunUtils.print(ship, ".");
        turnToward(VectorUtils.getAngle(jaunt.getDestination(), target.getLocation()));
        
        if(!jaunt.isWarping()) {
            ship.resetDefaultAI();
        
            SunUtils.print(ship, "off");
        }
    }
}
