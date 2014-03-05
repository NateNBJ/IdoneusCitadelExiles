package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.BaseShipAI;
import data.shipsystems.scripts.ai.EntropicInversionMatrixAI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;

public class MeleeTempAI extends BaseShipAI{
    WeaponAPI tractorBeam;
    EntropicInversionMatrixAI systemAI;

    @Override
    public void evaluateCircumstances() {
        if(ship.getFluxTracker().isOverloadedOrVenting()
                || !tractorBeam.isFiring()
                || ship.getShipTarget() == null
                || !ship.getShipTarget().isAlive()) {

            ship.resetDefaultAI();
            SunUtils.print("Let's reconsider.");
        }

        if(!AIUtils.canUseSystemThisFrame(ship)) {
            // TODO - Consider using phase cloak
        }

        // TODO - Track hull deterioration of target
    }

    public MeleeTempAI() {}
    public MeleeTempAI(ShipAPI ship, WeaponAPI tractorBeam) {
        super(ship);

        this.tractorBeam = tractorBeam;
        this.systemAI = new EntropicInversionMatrixAI();

        systemAI.init(ship, ship.getSystem(), null, Global.getCombatEngine());
        circumstanceEvaluationTimer.setInterval(0.1f);
        SunUtils.print("Gobble time!");

        // TODO - find tractor beam group
        //selectWeaponGroup(0);
    }

    @Override
    public void advance(float amount) {
        if(circumstanceEvaluationTimer.intervalElapsed()) evaluateCircumstances();

        systemAI.advance(amount, null, null, ship.getShipTarget());
        
        float angleToFace = VectorUtils.getAngle(ship.getLocation(), ship.getShipTarget().getLocation());
        
        if(Math.abs(MathUtils.getShortestRotation(ship.getFacing(), angleToFace)) < 20)
            accelerate();
        turnToward(angleToFace);
        fireSelectedGroup(ship.getShipTarget().getLocation());
    }

    // TODO - find a way around this disgusting hack.
    @Override
    public boolean needsRefit() { return true; }
}
