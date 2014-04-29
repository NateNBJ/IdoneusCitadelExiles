package data.hullmods;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.util.WeakHashMap;
import org.lwjgl.util.vector.Vector2f;

public class GravityAnchor extends BaseHullMod {
    static WeakHashMap locations = new WeakHashMap();
    static WeakHashMap facings = new WeakHashMap();
    
    public static void anchorShip(FleetMemberAPI ship, Vector2f location, float angle) {
        locations.put(ship.getId(), location);
        facings.put(ship.getId(), angle);
    }
    public static void anchorShip(FleetMemberAPI ship, Vector2f location) {
        locations.put(ship.getId(), location);
    }
    public static void anchorShip(ShipAPI ship, Vector2f location, float angle) {
        locations.put(ship.getFleetMemberId(), location);
        facings.put(ship.getFleetMemberId(), angle);
    }
    public static void anchorShip(ShipAPI ship, Vector2f location) {
        locations.put(ship.getFleetMemberId(), location);
    }

    public void setLocation(CombatEntityAPI entity, Vector2f location) {
        Vector2f dif = new Vector2f(location);
        Vector2f.sub(location, entity.getLocation(), dif);
        Vector2f.add(entity.getLocation(), dif, entity.getLocation());
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        
        String id = ship.getFleetMemberId();
        
        if(!locations.containsKey(id)) return;
        else setLocation(ship, (Vector2f)locations.get(id));
        
        if(!facings.containsKey(id)) return;
        else ship.setFacing((Float)facings.get(id));
    }
	
    @Override
    public boolean isApplicableToShip(ShipAPI ship){
        return false;
    }
}