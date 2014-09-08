package data.ai.drone;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.DroneLauncherShipSystemAPI;
import com.fs.starfarer.api.combat.DroneLauncherShipSystemAPI.DroneOrders;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.ai.ship.BaseShipAI;
import data.tools.SunUtils;
import java.awt.Color;
import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;


public class MxDroneAI extends BaseShipAI {
    ShipAPI mothership;
    ShipAPI target;
    DroneLauncherShipSystemAPI system;
    Vector2f targetOffset;
    Point cellToFix = new Point();
    Random rng = new Random();
    ArmorGridAPI armorGrid;
    SoundAPI repairSound;
    float max, cellSize;
    int gridWidth, gridHeight, cellCount, timesLeftToMx = 0;
    boolean doingMx = false;
    boolean returning = false;
    float dontRestoreAmmoUntil = 0;
    float hpAtLastCheck;
    float targetFacingOffset = Float.MIN_VALUE;
    
    Vector2f getDestination() {
        return new Vector2f();
    }

    static HashMap peakCrRecovery = new HashMap();
    static HashMap mxAssistTracker = new HashMap();
    static HashMap mxPriorities = new HashMap();
    static float mxPriorityUpdateFrequency = 2f;
    static float timeOfMxPriorityUpdate = 2f;

    static final float REPAIR_RANGE = 400f;
    static final float ROAM_RANGE = 3000f;
    static final float REPAIR_AMOUNT = 0.5f;
    static final float CR_PEAK_TIME_RECOVERY_RATE = 3f;
    static final float FLUX_PER_MX_PERFORMED = 1f;
    static final float COOLDOWN_PER_OP_OF_AMMO_RESTORED = 15f; // In seconds
//    static final Color ARC_FRINGE_COLOR = new Color(255, 223, 128, 150);
//    static final Color ARC_CORE_COLOR = new Color(255, 191, 0, 200);
    

    static final Color SPARK_COLOR = new Color(255, 223, 128);
    static final String SPARK_SOUND_ID = "system_emp_emitter_loop";
    static final float SPARK_DURATION = 0.2f;
    static final float SPARK_BRIGHTNESS = 1.0f;
    static final float SPARK_MAX_RADIUS = 7f;
    static final float SPARK_CHANCE = 0.17f;
    static final float SPARK_SPEED_MULTIPLIER = 500.0f;
    static final float SPARK_VOLUME = 1.0f;
    static final float SPARK_PITCH = 1.0f;

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
        float fp = SunUtils.getFP(ship);
        fp = (ship.isFighter()) ? fp / ship.getWing().getWingMembers().size() : fp;
        float peakCrLeft = getSecondsTilCrLoss(ship);
        
        if(ship.getHullSpec().getHullId().startsWith("sun_ice_"))
            priority += 1.0f * (1 - SunUtils.getArmorPercent(ship)) * fp;
        priority += 0.5f * getExpendedOrdnancePoints(ship);

        if(ship.losesCRDuringCombat())
            priority += 1.0f * ((60 / (60 + peakCrLeft)) * (1 - peakCrLeft / ship.getHullSpec().getNoCRLossTime()) * fp);

        priority *= 2f / (2f + getMxAssistance(ship));

        if(ship == Global.getCombatEngine().getPlayerShip())
            priority *= 2;

        return priority;
    }

    @Override
    public void evaluateCircumstances() {
        --timesLeftToMx;
        if(!mothership.isAlive()) SunUtils.destroy(ship);
        
        if(timeOfMxPriorityUpdate <= Global.getCombatEngine().getTotalElapsedTime(false)
                || timeOfMxPriorityUpdate > Global.getCombatEngine().getTotalElapsedTime(false) + mxPriorityUpdateFrequency)
            updateMxPriorities();

        ShipAPI previousTarget = target;
        setTarget(chooseTarget());

        if(returning) {
            targetOffset = SunUtils.toRelative(target, system.getLandingLocation(ship));
        } else if(target != previousTarget || timesLeftToMx < 1) {
            timesLeftToMx = 5;
            
            do {
                targetOffset = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius());
            } while(!CollisionUtils.isPointWithinBounds(targetOffset, target));
            
            targetOffset = SunUtils.toRelative(target, targetOffset);

            armorGrid = target.getArmorGrid();
            max = armorGrid.getMaxArmorInCell();
            cellSize = armorGrid.getCellSize();
            gridWidth = armorGrid.getGrid().length;
            gridHeight = armorGrid.getGrid()[0].length;
            cellCount = gridWidth * gridHeight;
        }

        if ((target.getPhaseCloak() == null || !target.getPhaseCloak().isOn())
                && !returning
                && !(hpAtLastCheck < ship.getHitpoints())
                && MathUtils.getDistance(ship, target) < REPAIR_RANGE
                && mxPriorities.containsKey(target)
                && ((Float)mxPriorities.get(target)) > 0) {

            performMaintenance();
        } else {
            doingMx = false;
        }
        
        if(doingMx == ship.getPhaseCloak().isOn()) ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        
        hpAtLastCheck = ship.getHitpoints();
    }
    void performMaintenance() {
        for(int i = 0; i < (1 + cellCount / 5); ++i) {
            cellToFix.x = rng.nextInt(gridWidth);
            cellToFix.y = rng.nextInt(gridHeight);

            if(armorGrid.getArmorValue(cellToFix.x, cellToFix.y) < max) break;
        }
        
        Vector2f at = SunUtils.getCellLocation(target, cellToFix.x, cellToFix.y);

        for(int i = 0; (i < 10) && !CollisionUtils.isPointWithinBounds(at, target); ++i)
            at = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius());

//        Global.getCombatEngine().spawnEmpArc(ship, at, target, ship,
//                DamageType.ENERGY, 0, 0, REPAIR_RANGE,
//                "tachyon_lance_emp_impact", 12f,
//                ARC_FRINGE_COLOR,
//                ARC_CORE_COLOR);

        restoreAmmo();

        ship.getFluxTracker().setCurrFlux(ship.getFluxTracker().getCurrFlux() + FLUX_PER_MX_PERFORMED);

        doingMx = true;
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

        if(winner == null) {
            dontRestoreAmmoUntil = Global.getCombatEngine().getTotalElapsedTime(false) + 1;
            return;
        }
        
        float op = winner.getSpec().getOrdnancePointCost(null);
        int ammoToRestore = (int)Math.max(1, Math.floor(winner.getMaxAmmo() / op));
        ammoToRestore = Math.min(ammoToRestore, winner.getMaxAmmo() - winner.getAmmo());
        //Utils.print("%"+lowestAmmo*100+"   "+winner.getId()+"   "+ammoToRestore);
        winner.setAmmo(winner.getAmmo() + ammoToRestore);
        dontRestoreAmmoUntil = Global.getCombatEngine().getTotalElapsedTime(false)
                + COOLDOWN_PER_OP_OF_AMMO_RESTORED * ((ammoToRestore / (float)winner.getMaxAmmo()) * op);
    }
    void repairArmor() {
        if(cellToFix == null) return;

        for(int x = cellToFix.x - 1; x <= cellToFix.x + 1; ++ x) {
            if(x < 0 || x >= gridWidth) continue;

            for(int y = cellToFix.y - 1; y <= cellToFix.y + 1; ++ y) {
                if(y < 0 || y >= gridHeight) continue;
                
                float mult = (float)((3 - Math.abs(x - cellToFix.x) - Math.abs(y - cellToFix.y)) / 3f);

                armorGrid.setArmorValue(x, y, Math.min(max, armorGrid.getArmorValue(x, y) + REPAIR_AMOUNT * mult));
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

            peakTimeRecovered += amount * (CR_PEAK_TIME_RECOVERY_RATE + target.getHullSpec().getCRLossPerSecond());
            peakTimeRecovered = Math.min(peakTimeRecovered, target.getTimeDeployedForCRReduction());
            target.getMutableStats().getPeakCRDuration().modifyFlat("sun_ice_drone_mx_repair", peakTimeRecovered);

            peakCrRecovery.put(target, peakTimeRecovered);
        }
    }
    ShipAPI chooseTarget() {
        if(needsRefit() || system.getDroneOrders() == DroneOrders.RECALL) {
            returning = true;
            ship.getFluxTracker().setCurrFlux(ship.getFluxTracker().getMaxFlux());
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

            if(ship.getOwner() != this.ship.getOwner()) continue;
            if(ship.isDrone()) continue;

            float score = (Float)mxPriorities.get(ship) / (500 + MathUtils.getDistance(this.ship, ship));

            if(score > record) {
                record = score;
                leader = ship;
            }
        }

        return (leader == null) ? mothership : leader;
    }
    void setTarget(ShipAPI ship) {
        if(target == ship) return;
        this.ship.setShipTarget(target = ship);
    }
    void goToDestination() {
        Vector2f to = SunUtils.toAbsolute(target, targetOffset);
        float distance = MathUtils.getDistance(ship, to);
        
        if(doingMx) {
            if(distance < 100) {
                float f = (1 - distance / 100) * 0.2f;
                ship.getLocation().x = (to.x * f + ship.getLocation().x * (2 - f)) / 2;
                ship.getLocation().y = (to.y * f + ship.getLocation().y * (2 - f)) / 2;
                ship.getVelocity().x = (target.getVelocity().x * f + ship.getVelocity().x * (2 - f)) / 2;
                ship.getVelocity().y = (target.getVelocity().y * f + ship.getVelocity().y * (2 - f)) / 2;
            }
        }
        
        if(doingMx && distance < 25) {
            Global.getSoundPlayer().playLoop(SPARK_SOUND_ID, ship, SPARK_PITCH,
                    SPARK_VOLUME, ship.getLocation(), ship.getVelocity());

            if(targetFacingOffset == Float.MIN_VALUE) {
                targetFacingOffset = ship.getFacing() - target.getFacing();
            } else {
                ship.setFacing(MathUtils.clampAngle(targetFacingOffset + target.getFacing()));
            }
            
            if(Math.random() < SPARK_CHANCE) {
                Vector2f loc = new Vector2f(ship.getLocation());
                loc.x += cellSize * 0.5f - cellSize * (float) Math.random();
                loc.y += cellSize * 0.5f - cellSize * (float) Math.random();

                Vector2f vel = new Vector2f(ship.getVelocity());
                vel.x += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;
                vel.y += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;

                Global.getCombatEngine().addHitParticle(loc, vel,
                        (SPARK_MAX_RADIUS * (float) Math.random() + SPARK_MAX_RADIUS),
                        SPARK_BRIGHTNESS,
                        SPARK_DURATION * (float) Math.random() + SPARK_DURATION,
                        SPARK_COLOR);
            }
        } else {
            targetFacingOffset = Float.MIN_VALUE;
            float angleDif = MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), to));

            if(Math.abs(angleDif) < 30){
                accelerate();
            } else {
                turnToward(to);
                decelerate();
            }        
            strafeToward(to);
        }
    }
   
    public MxDroneAI(ShipAPI drone, ShipAPI mothership, DroneLauncherShipSystemAPI system) {
        super(drone);
        this.mothership = mothership;
        this.system = system;

        hpAtLastCheck = drone.getHitpoints();
        circumstanceEvaluationTimer.setInterval(0.8f, 1.2f);
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        
        if(target == null) return;
        
        if (doingMx) {
            if(target.getHullSpec().getHullId().startsWith("sun_ice_"))
                repairArmor();
            maintainCR(amount);
        } else if(returning && !ship.isLanding() && MathUtils.getDistance(ship, mothership) < mothership.getCollisionRadius())
            ship.beginLandingAnimation(mothership);

        goToDestination();
    }
    @Override
    public boolean needsRefit() {
        return ship.getFluxTracker().getFluxLevel() >= 1;
    }
}