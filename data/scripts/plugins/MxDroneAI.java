package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DroneLauncherShipSystemAPI;
import com.fs.starfarer.api.combat.DroneLauncherShipSystemAPI.DroneOrders;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.WeaponAPI;
import java.awt.Color;
import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;


public class MxDroneAI implements ShipAIPlugin {
    ShipAPI drone;
    ShipAPI mothership;
    ShipAPI target;
    DroneLauncherShipSystemAPI system;
    float countdownToCircumstanceEvaluation = 0f;
    Vector2f destination;
    Point cellToFix = new Point();
    Random rng = new Random();
    ArmorGridAPI armorGrid;
    float max, cellSize;
    int gridWidth, gridHeight, cellCount;
    boolean performingMaintenance = false;
    boolean returning = false;
    float dontFireUntil = 0;
    float dontRestoreAmmoUntil = 0;
    float hpAtLastCheck;

    static HashMap peakCrRecovery = new HashMap();
    static HashMap mxAssistTracker = new HashMap();
    static HashMap mxPriorities = new HashMap();
    static float mxPriorityUpdateFrequency = 2f;
    static float timeOfMxPriorityUpdate = 2f;

    static final float circumstanceEvaluationFrequency = 1f;
    static final float repairRange = 400f;
    static final float roamRange = 3000f;
    static final float repairAmount = 0.5f;
    static final float crPeakTimeRecoveryRate = 1f;
    static final float fluxPerMxPerformed = 1f;
    static final float cooldownPerOpOfAmmoRestored = 5f; // In seconds
    static final Color ARC_FRINGE_COLOR = new Color(255, 223, 128, 150);
    static final Color ARC_CORE_COLOR = new Color(255, 191, 0, 200);

    static void updateMxPriorities() {
        mxAssistTracker.clear();
        mxPriorities.clear();
        
        for(Iterator iter = Global.getCombatEngine().getShips().iterator(); iter.hasNext(); ) {
            ShipAPI ship = (ShipAPI)iter.next();
            if(ship.isAlive() && ship.isDrone() && ship.getHullSpec().getHullId().equals("sun_ice_drone_mx")) {
                addMxAssistance(ship.getShipTarget(), 1);
            }
        }
        
        for(Iterator iter = Global.getCombatEngine().getShips().iterator(); iter.hasNext(); ) {
            ShipAPI ship = (ShipAPI)iter.next();
            if(!ship.isShuttlePod() && ship.isAlive() && !ship.isDrone() && !ship.isFighter()) {
                mxPriorities.put(ship, getMxPriority(ship));
                //Global.getCombatEngine().addFloatingText(ship.getLocation(), ((Float)mxPriorities.get(ship)).toString(), 40, Color.green, ship, 1, 5);
            }
        }

//        Utils.print(
//                "   Priority:" + (Float)mxPriorities.get(Global.getCombatEngine().getPlayerShip()) +
//                "   Armor:" + getArmorPercent(Global.getCombatEngine().getPlayerShip()) +
//                "   Ordnance:" + getExpendedOrdnancePoints(Global.getCombatEngine().getPlayerShip()) +
//                "   MxAssist:" + getMxAssistance(Global.getCombatEngine().getPlayerShip()) +
//                "   PeakCR:" + getSecondsTilCrLoss(Global.getCombatEngine().getPlayerShip()));

        timeOfMxPriorityUpdate = mxPriorityUpdateFrequency + Global.getCombatEngine().getTotalElapsedTime(false);
    }
    static void addMxAssistance(ShipAPI ship, int amount) {
        if(ship != null) {
            if(!mxAssistTracker.containsKey(ship)) mxAssistTracker.put(ship, (Integer)amount);
            else mxAssistTracker.put(ship, ((Integer) mxAssistTracker.get(ship)) + amount);
        }
    }
    static int getMxAssistance(ShipAPI ship) {
        return (mxAssistTracker.containsKey(ship)) ? (int)(Integer)mxAssistTracker.get(ship) : 0;
    }
    static float getSecondsTilCrLoss(ShipAPI ship) {
        float secondsTilCrLoss = 0;


        if(ship.losesCRDuringCombat()) {
            if(peakCrRecovery.containsKey(ship)) secondsTilCrLoss += (Float)peakCrRecovery.get(ship);

            secondsTilCrLoss += ship.getHullSpec().getNoCRLossTime() - ship.getTimeDeployedForCRReduction();

        } else secondsTilCrLoss = Float.MAX_VALUE;

        return Math.max(0, secondsTilCrLoss);
    }
    static float getArmorPercent(ShipAPI ship) {
        float acc = 0;
        int width = ship.getArmorGrid().getGrid().length;
        int height = ship.getArmorGrid().getGrid()[0].length;
        
        for(int x = 0; x < width; ++x) {
            for(int y = 0; y < height; ++y) {
                acc += ship.getArmorGrid().getArmorFraction(x, y);
            }
        }
        
        return acc / (width * height);
    }
    static float getExpendedOrdnancePoints(ShipAPI ship) {
        float acc = 0;

        for(Iterator iter = ship.getAllWeapons().iterator(); iter.hasNext();) {
            WeaponAPI weapon = (WeaponAPI)iter.next();

            acc += (weapon.usesAmmo() && weapon.getSpec().getAmmoPerSecond() == 0)
                    ? (1 - weapon.getAmmo() / (float)weapon.getMaxAmmo()) * weapon.getSpec().getOrdnancePointCost(null)
                    : 0;
        }

        return acc;
    }
    static float getMxPriority(ShipAPI ship) {
        float priority = 0;
        float fp = Global.getCombatEngine().getFleetManager(ship.getOwner()).getDeployedFleetMember(ship).getMember().getFleetPointCost();
        fp = (ship.isFighter()) ? fp / ship.getWingMembers().size() : fp;
        float peakCrLeft = getSecondsTilCrLoss(ship);
        
        priority += 1.0f * (1 - getArmorPercent(ship)) * fp;
        priority += 0.5f * getExpendedOrdnancePoints(ship);

        if(ship.losesCRDuringCombat())
            priority += 1.0f * ((60 / (60 + peakCrLeft)) * (1 - peakCrLeft / ship.getHullSpec().getNoCRLossTime()) * fp);

        priority *= 2f / (2f + getMxAssistance(ship));

        if(ship == Global.getCombatEngine().getPlayerShip())
            priority *= 2;

        return priority;
    }

    void evaluateCircumstances() {
        if(!mothership.isAlive()) die();
        
        if(timeOfMxPriorityUpdate <= Global.getCombatEngine().getTotalElapsedTime(false)
                || timeOfMxPriorityUpdate > Global.getCombatEngine().getTotalElapsedTime(false) + mxPriorityUpdateFrequency)
            updateMxPriorities();


        setTarget(chooseTarget());

        if(returning) destination = system.getLandingLocation(drone);
        else destination = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius() / 2f);
      
        //destination.x -= target.getLocation().x;
        //destination.y -= target.getLocation().y;
        
        armorGrid = target.getArmorGrid();
        max = armorGrid.getMaxArmorInCell();
        cellSize = armorGrid.getCellSize();
        gridWidth = armorGrid.getGrid().length;
        gridHeight = armorGrid.getGrid()[0].length;
        cellCount = gridWidth * gridHeight;

        if ((target.getPhaseCloak() == null || !target.getPhaseCloak().isOn())
                && !returning
                && !(hpAtLastCheck < drone.getHitpoints())
                && MathUtils.getDistance(drone, target) < repairRange
                && mxPriorities.containsKey(target)
                && ((Float)mxPriorities.get(target)) > 0) {

            performMaintenance();
        } else performingMaintenance = false;
        
        if(performingMaintenance == drone.getPhaseCloak().isOn()) drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);

        hpAtLastCheck = drone.getHitpoints();

        countdownToCircumstanceEvaluation = (circumstanceEvaluationFrequency / 2) + circumstanceEvaluationFrequency * (float)Math.random();
    }
    void performMaintenance() {
        for(int i = 0; i < (1 + cellCount / 5); ++i) {
            cellToFix.x = rng.nextInt(gridWidth);
            cellToFix.y = rng.nextInt(gridHeight);

            if(armorGrid.getArmorValue(cellToFix.x, cellToFix.y) < max) break;
        }
        
        Vector2f at = Utils.getCellLocation(target, cellToFix.x, cellToFix.y);

        for(int i = 0; (i < 10) && !CollisionUtils.isPointWithinBounds(at, target); ++i)
            at = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius());

        Global.getCombatEngine().spawnEmpArc(drone, at, target, drone,
                DamageType.ENERGY, 0, 0, repairRange,
                "tachyon_lance_emp_impact", 12f,
                ARC_FRINGE_COLOR,
                ARC_CORE_COLOR);

        restoreAmmo();

        drone.getFluxTracker().setCurrFlux(drone.getFluxTracker().getCurrFlux() + fluxPerMxPerformed);

        performingMaintenance = true;
    }
    void restoreAmmo() {
        if(dontRestoreAmmoUntil > Global.getCombatEngine().getTotalElapsedTime(false)) return;

        WeaponAPI winner = null;
        float lowestAmmo = 1;

        for(Iterator iter = target.getAllWeapons().iterator(); iter.hasNext();) {
            WeaponAPI weapon = (WeaponAPI)iter.next();
            
            if(!weapon.usesAmmo() || weapon.getSpec().getAmmoPerSecond() > 0) continue;
            
            float ammo = weapon.getAmmo() / (float)weapon.getMaxAmmo();

            if(ammo < lowestAmmo) {
                lowestAmmo = ammo;
                winner = weapon;
            }
        }

        if(winner == null) return;
        
        float op = winner.getSpec().getOrdnancePointCost(null);
        int ammoToRestore = (int)Math.max(1, Math.floor(winner.getMaxAmmo() / op));
        ammoToRestore = Math.min(ammoToRestore, winner.getMaxAmmo() - winner.getAmmo());
        //Utils.print("%"+lowestAmmo*100+"   "+winner.getId()+"   "+ammoToRestore);
        winner.setAmmo(winner.getAmmo() + ammoToRestore);
        dontRestoreAmmoUntil = Global.getCombatEngine().getTotalElapsedTime(false) + cooldownPerOpOfAmmoRestored * ((ammoToRestore / (float)winner.getMaxAmmo()) * op);
    }
    void repairArmor() {
        if(cellToFix == null) return;

        for(int x = cellToFix.x - 1; x <= cellToFix.x + 1; ++ x) {
            if(x < 0 || x >= gridWidth) continue;

            for(int y = cellToFix.y - 1; y <= cellToFix.y + 1; ++ y) {
                if(y < 0 || y >= gridHeight) continue;
                
                float mult = (float)((3 - Math.abs(x - cellToFix.x) - Math.abs(y - cellToFix.y)) / 3f);

                armorGrid.setArmorValue(x, y, Math.min(max, armorGrid.getArmorValue(x, y) + repairAmount * mult));
            }
        }
    }   
    void maintainCR(float amount) {
        if(target.losesCRDuringCombat()) {
            Float peakTimeRecovered = 0f;

            if(!peakCrRecovery.containsKey(target)) peakCrRecovery.put(target, 0f);
            else peakTimeRecovered = (Float)peakCrRecovery.get(target);

            float t = target.getTimeDeployedForCRReduction() - peakTimeRecovered - target.getHullSpec().getNoCRLossTime();

            peakTimeRecovered += (t > 0) ? t : 0;

            //if(t > 0) Utils.print("CR Loss Stopped!");

            peakTimeRecovered += amount * (crPeakTimeRecoveryRate + target.getHullSpec().getCRLossPerSecond());
            peakTimeRecovered = Math.min(peakTimeRecovered, target.getTimeDeployedForCRReduction());
            target.getMutableStats().getPeakCRDuration().modifyFlat("sun_ice_drone_mx_repair", peakTimeRecovered);

            peakCrRecovery.put(target, peakTimeRecovered);
        }
    }
    ShipAPI chooseTarget() {
        if(drone.getFluxTracker().getFluxLevel() >= 1 || system.getDroneOrders() == DroneOrders.RECALL) {
            returning = true;
            drone.getFluxTracker().setCurrFlux(drone.getFluxTracker().getMaxFlux());
            return mothership;
        } else returning = false;
        
        if(mothership.getShipTarget() != null && mothership.getOwner() == mothership.getShipTarget().getOwner()) {
            return mothership.getShipTarget();
        } else if (system.getDroneOrders() == DroneOrders.DEPLOY) {
            return mothership;
        }
        
        float record = 0;
        ShipAPI leader = null;

        for(Iterator iter = mxPriorities.keySet().iterator(); iter.hasNext();) {
            ShipAPI ship = (ShipAPI)iter.next();

            if(ship.getOwner() != drone.getOwner()) continue;
            if(ship.isDrone()) continue;

            float score = (Float)mxPriorities.get(ship) / (500 + MathUtils.getDistance(drone, ship));

            if(score > record) {
                record = score;
                leader = ship;
            }
        }

        return (leader == null) ? mothership : leader;
    }
    void setTarget(ShipAPI ship) {
        if(target == ship) return;
        drone.setShipTarget(target = ship);
    }
    void goToDestination() {
        Vector2f to = destination;
        //to.x += target.getLocation().x;
        //to.y += target.getLocation().y;

        float angleDif = MathUtils.getShortestRotation(drone.getFacing(), VectorUtils.getAngle(drone.getLocation(), to));

        if(Math.abs(angleDif) < 30){
            drone.giveCommand(ShipCommand.ACCELERATE, to, 0);
        } else {
            ShipCommand direction = (angleDif > 0) ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT;
            drone.giveCommand(direction, to, 0);
            drone.giveCommand(ShipCommand.DECELERATE, to, 0);
        }        
    }
    void die() {
        drone.setHitpoints(0);
    }

    public MxDroneAI() {}
    public MxDroneAI(ShipAPI drone, ShipAPI mothership, DroneLauncherShipSystemAPI system) {
        this.drone = drone;
        this.mothership = mothership;
        this.system = system;

        hpAtLastCheck = drone.getHitpoints();
    }

    @Override
    public void advance(float amount) {
        countdownToCircumstanceEvaluation -= amount;

        if(countdownToCircumstanceEvaluation < 0) evaluateCircumstances();
        
        if(target == null) return;
        
        if (performingMaintenance) {
            repairArmor();
            maintainCR(amount);
        } else if(returning && !drone.isLanding() && MathUtils.getDistance(drone, mothership) < mothership.getCollisionRadius())
            drone.beginLandingAnimation(mothership);

        goToDestination();
    }
    @Override
    public void forceCircumstanceEvaluation() {
        evaluateCircumstances();
    }
    @Override
    public boolean needsRefit() {
        return false;
    }
    @Override
    public void setDoNotFireDelay(float amount) {
        dontFireUntil = amount + Global.getCombatEngine().getTotalElapsedTime(false);
    }
}