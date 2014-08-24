package data;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.tools.SunUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lwjgl.util.vector.Vector2f;

public class EveryFramePlugin implements EveryFrameCombatPlugin {
    public interface ProjectileEffectAPI {
        void advance(float amount);
    }
    
    CombatEngineAPI engine;
    
    static Set<ShipAPI> shipsToGiveFluxRefund = new HashSet<ShipAPI>();
    static public void tagForShieldUpkeepRefund(ShipAPI ship) {
        shipsToGiveFluxRefund.add(ship);
    }
    void refundShieldUpkeepFlux(float amount) {
        for(ShipAPI ship : shipsToGiveFluxRefund) {
            float upkeep = SunUtils.getShieldUpkeep(ship);
            upkeep *= ship.getMutableStats().getFluxDissipation().getModifiedValue();
            float arcReduction = 1 - Math.max(0, ship.getShield().getActiveArc()) / ship.getShield().getArc();
            ship.getFluxTracker().decreaseFlux(upkeep * arcReduction * amount);
            
            //engine.addFloatingDamageText(ship.getLocation(), upkeep * arcReduction * amount, Color.yellow, ship, ship);
        }
        
        shipsToGiveFluxRefund.clear();
    }
    
    boolean fissionDrillWeaponActivated = false;
    void checkFissionDrillUsageByPlayer() {
        ShipAPI ship = engine.getPlayerShip();
        ShipSystemAPI sys = ship.getSystem();
        
        if(!ship.getHullSpec().getHullId().equals("sun_ice_athame")
                || ship.getShipAI() != null)
            return;
        
        WeaponAPI weapon = ship.getAllWeapons().get(2);
        
        if(!fissionDrillWeaponActivated && weapon.isFiring()) {
            if(!sys.isOn()) {
                ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
                //SunUtils.print("on");
            }
            fissionDrillWeaponActivated = true;
        } else if(fissionDrillWeaponActivated && !weapon.isFiring()) {
            if(sys.isOn()) {
                ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
                //SunUtils.print("off");
            }
            fissionDrillWeaponActivated = false;
        }
    }

    
    @Override
    public void advance(float amount, List events) {
        checkFissionDrillUsageByPlayer();
        refundShieldUpkeepFlux(amount);
        
//        ShipAPI ship = engine.getPlayerShip();
//        
//        for(WeaponAPI w : ship.getAllWeapons()) {
//            Vector2f temp = SunUtils.toRelative(ship, w.getLocation());
//            //SunUtils.blink(w.getLocation());
//            SunUtils.blink(temp);
//            SunUtils.blink(SunUtils.toAbsolute(ship, temp));
//        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }
}