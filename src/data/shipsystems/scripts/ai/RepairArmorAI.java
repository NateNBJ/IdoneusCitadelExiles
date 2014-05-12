package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import data.scripts.plugins.SunUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class RepairArmorAI implements ShipSystemAIScript
{
    private static final float ACTIVATION_THRESHOLD = 0.1f;
    private static final float DEACTIVATION_THRESHOLD = 0.5f;
    private static final float REFRESH_FREQUENCY = 1f;
    private float timeOfNextRefresh = 0;
    private ShipSystemAPI system;
    private ShipAPI ship;
    

    private float getEnginePerformance() {
        List engines = ship.getEngineController().getShipEngines();

        float acc = 0;
        int count = 0;

        for(Iterator iter = engines.iterator(); iter.hasNext();) {
            ShipEngineAPI engine = (ShipEngineAPI)iter.next();

            if(engine.isSystemActivated()) continue;

            acc += engine.isDisabled() ? 0 : 1;

            ++count;
        }

        return acc / count;
    }
    private float getArmorState() {
        ArmorGridAPI armorGrid = ship.getArmorGrid();
        Random rng = new Random();
        float armorState = 0;
        int gridWidth = armorGrid.getGrid().length;
        int gridHeight = armorGrid.getGrid()[0].length;
        int candidates = 1 + (gridWidth * gridHeight) / 10;

        for(int i = 0; i < candidates; ++i) {
            int x = rng.nextInt(gridWidth);
            int y = rng.nextInt(gridHeight);
            
            armorState += armorGrid.getArmorFraction(x, y);
        }
        
        armorState /= candidates;      
        
        return armorState;
    }
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
//        if(!system.isActive() && AIUtils.canUseSystemThisFrame(ship)) {
//            ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
//        }

        if(!AIUtils.canUseSystemThisFrame(ship)) return;

        if(timeOfNextRefresh < Global.getCombatEngine().getTotalElapsedTime(false)) {
            timeOfNextRefresh += REFRESH_FREQUENCY;
        } else return;

        // low  [0] - Tested armor was at least half depleted.
        // high [1] - No armor damage found
        float armorState = Math.max(0, (getArmorState() - 0.5f) * 2f);

        // low  [0] - No flux
        // high [1] - Full flux
        float fluxLevel = ship.getFluxTracker().getFluxLevel();

        // low  [0] - flameout
        // high [1] - no offline engines
        float enginePerformance = getEnginePerformance();

        // low  [0] - No danger nearby
        // high [1] - Really need to move
        float danger = SunUtils.estimateIncomingDamage(ship) / 500f;

        // low  [0] - don't need to activate
        // high [1] - need to activate
        float wantActive = (1 - armorState);
        wantActive *= (1 - danger * enginePerformance);
        wantActive *= (1 - (float)Math.pow(fluxLevel, 2));

        if(!system.isActive() && (wantActive > ACTIVATION_THRESHOLD)) ship.useSystem();
        else if(system.isActive() && (wantActive < DEACTIVATION_THRESHOLD)) ship.useSystem();
    }
}
