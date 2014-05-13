package data.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import data.tools.SunUtils;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.combat.AIUtils;

public class EntropicInversionMatrixAI implements ShipSystemAIScript {
    private static final float REFRESH_FREQUENCY = 0.25f;
    private static final float USE_SYSTEM_THRESHOLD = 0.03f;

    private float timeOfNextRefresh = 0;
    private ShipAPI ship;
    
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if(timeOfNextRefresh < Global.getCombatEngine().getTotalElapsedTime(false)) {
            timeOfNextRefresh = Global.getCombatEngine().getTotalElapsedTime(false) + REFRESH_FREQUENCY;
        } else return;
        
        //SunUtils.print("" + SunUtils.estimateIncomingDamage(ship, 1));

        if(AIUtils.canUseSystemThisFrame(ship) && !ship.getPhaseCloak().isActive()
                && (SunUtils.estimateIncomingDamage(ship, 1) /
                (ship.getMaxHitpoints() + ship.getHitpoints())) > USE_SYSTEM_THRESHOLD) {

            ship.useSystem();
            //ship.setShipAI(new DontPhaseTempAI(ship));
        }
    }
}