package data.ai.ship;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import data.tools.JauntSession;
import data.tools.SunUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class JauntTurnTempAI extends BaseShipAI {
    ShipAPI target;
    JauntSession jaunt;
    
    public JauntTurnTempAI(ShipAPI ship, ShipAPI target, JauntSession jaunt) {
        super(ship);
        this.target = target;
        this.jaunt = jaunt;
        
        //SunUtils.print(ship, "on");
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        
        if((!jaunt.isWarping() && !jaunt.isReturning())
                 || target == null
                 || !JauntSession.hasSession(ship)) {
        //if(false) { 
            ship.resetDefaultAI();
            //SunUtils.print(ship, "off");
        } else {
            Vector2f to = jaunt.isReturning() ? jaunt.getOrigin() : jaunt.getDestination();
//            turnToward(VectorUtils.getAngle(to, target.getLocation()));
//            ship.setFacing(VectorUtils.getAngle(to, target.getLocation()));
//            turnToward(VectorUtils.getAngle(ship.getLocation(), Global.getCombatEngine().getPlayerShip().getMouseTarget()));

            fakeTurnToAngle(VectorUtils.getAngle(to, target.getLocation()));
            
//            SunUtils.blink(to);
//            SunUtils.blink(target.getLocation());
         }
    }
    
    // Can't get ship to turn as quickly as it should during warp for some reason.
    // This is my hacky workaround.
    float angleVel = Float.NaN;
    void fakeTurnToAngle(float degrees) {
        if(Float.isNaN(angleVel))
            angleVel = ship.getAngularVelocity();
        
        float angleDif = MathUtils.getShortestRotation(ship.getFacing(), degrees);
        float secondsTilDesiredFacing = angleDif / ship.getAngularVelocity();
        boolean goLeft = angleDif > 0;
        if(secondsTilDesiredFacing > 0) {
            float turnAcc = ship.getMutableStats().getTurnAcceleration().getModifiedValue();
            float rotValWhenAt = Math.abs(ship.getAngularVelocity()) - secondsTilDesiredFacing * turnAcc;
            if(rotValWhenAt > 0) goLeft = !goLeft;
        }
        
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        float turnAcc = ship.getMutableStats().getTurnAcceleration().getModifiedValue();
        float maxTurn = ship.getMutableStats().getMaxTurnRate().getModifiedValue();
        float dAngleVel = turnAcc * (goLeft ? 1 : -1) * amount;
        float newAngleVel = angleVel + dAngleVel;
        
        angleVel = Math.max(-maxTurn, Math.min(maxTurn, newAngleVel));
        ship.setAngularVelocity(angleVel);
        ship.setFacing(ship.getFacing() + angleVel * amount);
    }
}
