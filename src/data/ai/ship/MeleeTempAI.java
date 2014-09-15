package data.ai.ship;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.shipsystems.ai.EntropicInversionMatrixAI;
import data.tools.IceUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;

public class MeleeTempAI extends BaseShipAI {
    WeaponAPI tractorBeam;
    EntropicInversionMatrixAI systemAI;
    boolean facingBetterTarget = false;

    @Override
    public void evaluateCircumstances() {
        if(ship.getFluxTracker().isOverloadedOrVenting()
                || !tractorBeam.isFiring()
                || ship.getFluxTracker().getFluxLevel() > 0.9f
                || ship.getPhaseCloak().isActive()
                || ship.getShipTarget() == null
                || ship.getShipTarget().getOwner() == ship.getOwner()
                || tractorBeam.getRange() < MathUtils.getDistance(ship.getShipTarget(), tractorBeam.getLocation())
                || !ship.getShipTarget().isAlive()) {

            ship.resetDefaultAI();
            //SunUtils.print("Let's reconsider.");
        }
        
        if(ship.getSystem().isActive()) return;

        //boolean inMeleeRange = MathUtils.getDistance(ship, ship.getShipTarget()) <= 0;
        
        boolean targetDeathEminent = ship.getShipTarget().getHitpoints()
                <= IceUtils.estimateIncomingDamage(ship.getShipTarget(), 2f);
        
        boolean targetMayDieSoon = targetDeathEminent
                || ship.getShipTarget().getHitpoints() <= 3000f;

        boolean canPhase = ship.getPhaseCloak() != null
                && ship.getFluxTracker().getMaxFlux() - ship.getFluxTracker().getCurrFlux()
                    > ship.getPhaseCloak().getFluxPerUse() * 1.1f;

        float danger = IceUtils.estimateIncomingDamage(ship, 2)
                / (ship.getHitpoints() + ship.getMaxHitpoints());

        if(danger > 0.12f || targetDeathEminent) {
            boolean systemUsed = useSystem();
            if(!systemUsed && canPhase) {
                this.toggleDefenseSystem();
            } else if(!systemUsed) vent();
        } else if (danger > 0.03 && !targetMayDieSoon) {
            useSystem();
        }
    }

    public MeleeTempAI(ShipAPI ship, WeaponAPI tractorBeam) {
        super(ship);

        this.tractorBeam = tractorBeam;
        this.systemAI = new EntropicInversionMatrixAI();

        systemAI.init(ship, ship.getSystem(), null, Global.getCombatEngine());
        circumstanceEvaluationTimer.setInterval(0.1f);
        //SunUtils.print("Gobble time!");

        // TODO - find tractor beam group
        //selectWeaponGroup(0);
    }

    @Override
    public void advance(float amount) {
        if(circumstanceEvaluationTimer.intervalElapsed()) evaluateCircumstances();

        systemAI.advance(amount, null, null, ship.getShipTarget());
        
        float angleToFace = VectorUtils.getAngle(ship.getLocation(), ship.getShipTarget().getLocation());
        
        if(Math.abs(MathUtils.getShortestRotation(ship.getFacing(), angleToFace)) < 15)
            accelerate();
        if(MathUtils.getDistance(ship, ship.getShipTarget()) <= 0)
            strafeToward(ship.getShipTarget());
        turnToward(angleToFace);
        fireSelectedGroup(ship.getShipTarget().getLocation());
    }

    // TODO - find a way around this disgusting hack.
    @Override
    public boolean needsRefit() { return true; }
}
