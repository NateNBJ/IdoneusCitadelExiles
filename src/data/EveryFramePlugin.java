package data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.tools.JauntSession;
import data.tools.SunUtils;
import data.weapons.beam.RecallBeamEffect;
import java.awt.Color;
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
                upkeep = SunUtils.getShieldUpkeep(ship);
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

    static List<RecallBeamEffect.RecallTracker> recalling = new LinkedList();
    public static void beginRecall(RecallBeamEffect.RecallTracker tracker) {
        recalling.add(tracker);
    }
    void advanceActiveRecalls(float amount) {
        List<RecallBeamEffect.RecallTracker> toRemove = new LinkedList();
        
        for(RecallBeamEffect.RecallTracker t : recalling) {
            if(t.progress < 0 && t.progress + amount >= 0) {
                t.ally.getLocation().set(t.recallLoc);
            }
            
            t.progress += amount;
            t.ally.getSpriteAPI().setColor(new Color(1,1,1, Math.min(1, Math.abs(t.progress))));
            
            if(t.progress >= 1) toRemove.add(t);
        }
        
        for(RecallBeamEffect.RecallTracker t : toRemove) {
            recalling.remove(t);
        }
    }
    
    boolean playerCloakPreviouslyCoolingDown = false;
    void playPhaseCloakCooldownOverSoundForPlayer() {
        ShipAPI ship = Global.getCombatEngine().getPlayerShip();
        
        if(ship != null && ship.getShipAI() == null && ship.getPhaseCloak() != null) {
            if(playerCloakPreviouslyCoolingDown != ship.getPhaseCloak().isCoolingDown()
                    && !ship.getPhaseCloak().isCoolingDown()) {

                Global.getSoundPlayer().playSound("engine_disabled", 3, 0.7f, ship.getLocation(), ship.getVelocity());
            }
            
            playerCloakPreviouslyCoolingDown = ship.getPhaseCloak().isCoolingDown();
        } else playerCloakPreviouslyCoolingDown = false;
    }
    
    @Override
    public void advance(float amount, List events) {
        clearBonuses();
        
        if(engine.isPaused()) return;
        
        checkFissionDrillUsageByPlayer();
        refundShieldUpkeepFlux(amount);
        advanceActiveRecalls(amount);
        JauntSession.advanceAll();
        playPhaseCloakCooldownOverSoundForPlayer();
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }
}