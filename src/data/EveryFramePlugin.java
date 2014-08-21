package data;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.tools.SunUtils;
import java.util.List;
import java.util.Stack;

public class EveryFramePlugin implements EveryFrameCombatPlugin {
    public interface ProjectileEffectAPI {
        void advance(float amount);
    }

    CombatEngineAPI engine;
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
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }
}