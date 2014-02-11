package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import java.awt.Color;
import java.util.Iterator;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class Utils
{
    private static final float SAFE_DISTANCE = 600f;

    public static Vector2f getCellLocation(ShipAPI ship, float x, float y) {
        x -= ship.getArmorGrid().getGrid().length / 2f;
        y -= ship.getArmorGrid().getGrid()[0].length / 2f;
        float cellSize = ship.getArmorGrid().getCellSize();
        Vector2f cellLoc = new Vector2f();
        float theta = (float)(((ship.getFacing() - 90) / 350f) * (Math.PI * 2));
        cellLoc.x = (float)(x * Math.cos(theta) - y * Math.sin(theta)) * cellSize + ship.getLocation().x;
        cellLoc.y = (float)(x * Math.sin(theta) + y * Math.cos(theta)) * cellSize + ship.getLocation().y;

        return cellLoc;
    }
    public static void print(String str) {

        CombatEngineAPI engine = Global.getCombatEngine();
        ShipAPI ship = engine.getPlayerShip();

        if(ship != null)
            Global.getCombatEngine().addFloatingText(ship.getLocation(), str, 40, Color.green, ship, 1, 5);
    }
    public static float estimateIncomingDamage(ShipAPI ship) {
        float accumulator = 0f;
        DamagingProjectileAPI proj;
        
        for (Iterator iter = Global.getCombatEngine().getProjectiles().iterator(); iter.hasNext();) {
            proj = (DamagingProjectileAPI) iter.next();
            
            if(proj.getOwner() == ship.getOwner()) continue; // Ignore friendly projectiles

            float safeDistance = SAFE_DISTANCE + ship.getCollisionRadius();
            float threat = proj.getDamageAmount() + proj.getEmpAmount();

            accumulator += threat * Math.max(0, Math.min(1, Math.pow(1 - MathUtils.getDistance(proj, ship) / safeDistance, 2)));
        }
        
        return accumulator;
    }
}