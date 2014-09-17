package data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.tools.JauntSession;
import data.tools.IceUtils;
import data.tools.RecallTracker;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class EveryFramePlugin implements EveryFrameCombatPlugin {
    public interface ProjectileEffectAPI {
        void advance(float amount);
    }
    
    static final String MODIFIER_KEY = "sun_ice_every_frame_modifiers";
    
    CombatEngineAPI engine;
    
    static Set<ShipAPI> shipsToGiveFluxRefund = new HashSet<ShipAPI>();
    static Set<ShipAPI> shipsToClearBonusesFrom = new HashSet<ShipAPI>();
    static public void tagForShieldUpkeepRefund(ShipAPI ship) {
        shipsToGiveFluxRefund.add(ship);
    }
    void clearBonuses() {
        for(ShipAPI ship : shipsToClearBonusesFrom) {
            ship.getMutableStats().getHardFluxDissipationFraction().unmodify(MODIFIER_KEY);
        }
        
        shipsToClearBonusesFrom.clear();
    }
    void refundShieldUpkeepFlux(float amount) {
        for(ShipAPI ship : shipsToGiveFluxRefund) {
            float upkeep;
            
            try {
                upkeep = IceUtils.getShieldUpkeep(ship);
            } catch(Exception e) {
                upkeep = 0;
            }
            
            upkeep *= ship.getMutableStats().getFluxDissipation().getModifiedValue();
            float arcReduction = 1 - Math.max(0,ship.getShield().getActiveArc())
                    / ship.getShield().getArc();
            ship.getFluxTracker().decreaseFlux(upkeep * arcReduction * amount);
            ship.getMutableStats().getHardFluxDissipationFraction().modifyFlat(
                    MODIFIER_KEY, 100);
            
            //engine.addFloatingDamageText(ship.getLocation(), upkeep * arcReduction * amount, Color.yellow, ship, ship);
        }
        shipsToClearBonusesFrom.addAll(shipsToGiveFluxRefund);
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
            }
            fissionDrillWeaponActivated = true;
        } else if(fissionDrillWeaponActivated && !weapon.isFiring()) {
            if(sys.isOn()) {
                ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
            }
            fissionDrillWeaponActivated = false;
        }
    }

    static List<RecallTracker> recalling = new LinkedList();
    public static void beginRecall(RecallTracker tracker) {
        recalling.add(tracker);
    }
    void advanceActiveRecalls(float amount) {
        List<RecallTracker> toRemove = new LinkedList();
        
        for(RecallTracker t : recalling) {
            t.advance(amount);
            
            if(t.isComplete()) toRemove.add(t);
        }
        
        for(RecallTracker t : toRemove) {
            recalling.remove(t);
        }
        toRemove.clear();
    }

    boolean playerCloakPreviouslyCoolingDown = false;
    void playPhaseCloakCooldownOverSoundForPlayer() {
        ShipAPI ship = Global.getCombatEngine().getPlayerShip();
        
        if(ship != null && !ship.isShuttlePod() && ship.getShipAI() == null
                && ship.getPhaseCloak() != null) {
            
            if(playerCloakPreviouslyCoolingDown != ship.getPhaseCloak().isCoolingDown()
                    && !ship.getPhaseCloak().isCoolingDown()) {

                Global.getSoundPlayer().playSound("engine_disabled", 3, 0.7f, ship.getLocation(), ship.getVelocity());
            }
            
            playerCloakPreviouslyCoolingDown = ship.getPhaseCloak().isCoolingDown();
        } else playerCloakPreviouslyCoolingDown = false;
    }
    
    static float elapsedTime = 0;
    public static float getElapsed() {
        return elapsedTime;
    }
    
    @Override
    public void advance(float amount, List events) {
        elapsedTime = amount;
        
        clearBonuses();
                
        if(engine.isPaused()) return;
        
        checkFissionDrillUsageByPlayer();
        refundShieldUpkeepFlux(amount);
        advanceActiveRecalls(amount);
        JauntSession.advanceAll(amount);
        playPhaseCloakCooldownOverSoundForPlayer();
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        shipsToGiveFluxRefund.clear();
        shipsToClearBonusesFrom.clear();
        recalling.clear();
        JauntSession.clearSessions();
    }
}