package data.world;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.events.CampaignEventManagerAPI;
import com.fs.starfarer.api.campaign.events.CampaignEventTarget;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Events;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ICEEveryFrameScript implements EveryFrameScript {
    static final float AVG_UPDATE_INTERVAL = 0.3f;
    static final float AVG_PILGRIMAGE_INTERVAL = 5.3f;
    static final float MIN_COLONY_FLEET_FP = 150;
    static final float AVG_PURSUIT_DAYS = 8f;
    
    float elapsedDays = 0f;
    float dayOfNextUpdate = 0;
    float dayOfNextExodus = 3;
    float dayOfNextPilgrimage = 0;
    SectorAPI sector;
    CampaignFleetAPI pursuer;
    
    @Override
    public void advance(float amount) {
        sector = Global.getSector();
        
        if(sector.isInNewGameAdvance() || sector.isPaused()) return;
        
        float days = sector.getClock().convertToDays(amount);
        elapsedDays += days;
        
        if(elapsedDays > dayOfNextUpdate) {
            dayOfNextUpdate = elapsedDays + AVG_UPDATE_INTERVAL * (0.5f + (float)Math.random());
            
            Data.ExileMarket.reapplyConditions();
            
            detachPursuitFleetIfApt();
            spawnVagrantFleetIfApt();
            detachDuellistIfApt();
            offerBountyIfApt(Data.ExileMarket);
            offerBountyIfApt(Data.CitadelMarket);
        }
        
        doShalomVisibilityHack();
    }
    
    void offerBountyIfApt(MarketAPI market) {
        CampaignEventManagerAPI mgt = Global.getSector().getEventManager();
        
        if(market != null && !market.getPrimaryEntity().isInHyperspace()
                && !mgt.isOngoing(new CampaignEventTarget(market), Events.SYSTEM_BOUNTY)) {
            mgt.startEvent(new CampaignEventTarget(market.getPrimaryEntity()), Events.SYSTEM_BOUNTY, null);
        }
    }
    void detachDuellistIfApt() {
        if(Data.ExileFleet == null || !Data.ExileFleet.isAlive() || Math.random() < 0.95f
                || Data.ExileFleet.getFleetPoints() < MIN_COLONY_FLEET_FP + 20
                || Data.ExileFleet.isInHyperspace())
            return;
                
        Random rand = new Random();
        List<FleetMemberAPI> candidates = Data.ExileFleet.getFleetData().getCombatReadyMembersListCopy();
        FleetMemberAPI winner;
        
        while(true) {
            int index = rand.nextInt(candidates.size());
            FleetMemberAPI m = candidates.get(index);
            candidates.remove(index);
            
            if(!m.isCivilian() && !m.isCarrier() && !m.isFighterWing()
                    && (m.getFuelCapacity() + m.getCargoCapacity()) / m.getFleetPointCost() < 25) {
                
                winner = m;
                break;
            }
            
            if(candidates.isEmpty()) return;
        }
        
        
        CampaignFleetAPI duelist = Global.getFactory().createEmptyFleet("sun_ice", "Duelist", true);
        
        duelist.setPreferredResupplyLocation(Data.ExileFleet);
        duelist.getFleetData().addFleetMember(winner);
        Data.ExileFleet.getFleetData().removeFleetMember(winner);
        
        Data.ExileFleet.getContainingLocation().spawnFleet(Data.ExileFleet, 0, 0, duelist);
        
        duelist.addAssignment(FleetAssignment.RAID_SYSTEM, Data.ExileFleet,
                AVG_PURSUIT_DAYS * (0.5f + (float)Math.random()), "looking for a fight");
        duelist.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                Data.ExileFleet, 9999, "returning to the Colony Fleet",
                new JoinMotherFleetScript(Data.ExileFleet, duelist));
    }
    void spawnVagrantFleetIfApt() {
        if(Data.ExileFleet == null) return;
        
        float time = sector.getClock().getElapsedDaysSince(0);
        float chance = 0.2f
            * (Data.ExileFleet.getFleetPoints() / 1000f)
            * (0.25f + (float)Math.sin(time * 257) * 0.75f)
            * (0.35f + (float)Math.cos(time * 30) * 0.65f);
        
        if(Math.random() > chance) return;
        
        CampaignFleetAPI pilgrims = Global.getFactory().createEmptyFleet("sun_ice", "Vagrant Fleet", true);
        
        WeightedRandomPicker<String> roles = new WeightedRandomPicker<String>();
        roles.add("interceptor", 3);
        roles.add("fighter", 6);
        roles.add("bomber", 2);
        roles.add("fastAttack", 1);
        roles.add("escortSmall", 1);
        roles.add("escortMedium", 1);
        roles.add("combatSmall", 4);
        roles.add("combatMedium", 3);
        roles.add("combatLarge", 2);
        roles.add("combatCapital", 1);
        roles.add("combatFreighterSmall", 3);
        roles.add("combatFreighterMedium", 2);
        roles.add("combatFreighterLarge", 1);
        roles.add("carrierMedium", 2);
        roles.add("carrierLarge", 1);
        roles.add("personnelMedium", 1);
        roles.add("freighterSmall", 1);
        roles.add("freighterMedium", 1);
        roles.add("tankerSmall", 1);
        roles.add("tankerMedium", 1);
        
        //Random rand = new Random();
        for(int minFP = 15 + (int)(Math.pow(Math.random(), 2.5f) * 100); pilgrims.getFleetPoints() < minFP;) {
            Data.Exiles.pickShipAndAddToFleet(roles.pick(), 0.8f, pilgrims);
        }
        
        pilgrims.getFleetData().sort();
        Data.IdoneusCitadel.getContainingLocation().spawnFleet(Data.IdoneusCitadel, 0, 0, pilgrims);
        
        pilgrims.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                Data.ExileFleet, 9999, "rendezvousing with the Colony Fleet",
                new JoinMotherFleetScript(Data.ExileFleet, pilgrims));
    }
    void detachPursuitFleetIfApt() {
        float REQUIRED_PURSUIT_RATIO = 1.5f;
        
        CampaignFleetAPI target;
        
        if((pursuer == null || !pursuer.isAlive()) && Data.ExileFleet != null
                && Data.ExileFleet.getInteractionTarget() instanceof CampaignFleetAPI) {
            target = (CampaignFleetAPI)Data.ExileFleet.getInteractionTarget();
            if(!target.getFaction().isAtBest("sun_ice", RepLevel.HOSTILE)) return;
        } else if(Data.Exiles.getRelationshipLevel("player") == RepLevel.VENGEFUL
                && sector.getCurrentLocation() == Data.ExileFleet.getContainingLocation()) {
            target = sector.getPlayerFleet();
        } else return;
        
        float neededFP = target.getFleetPoints() * REQUIRED_PURSUIT_RATIO;
        float neededSpeed = target.getFleetData().getBurnLevel() + 1;
        
        // Avoid stretching the colony fleet thin
        if(Data.ExileFleet.getFleetPoints() - neededFP < MIN_COLONY_FLEET_FP) return;
        
        Random rand = new Random();
        List<FleetMemberAPI> candidates = Data.ExileFleet.getFleetData().getCombatReadyMembersListCopy();
        List<FleetMemberAPI> winners = new ArrayList<FleetMemberAPI>();
        int fp = 0;
        boolean acceptFighters = false;
        
        while(true) {
            int index = rand.nextInt(candidates.size());
            FleetMemberAPI m = candidates.get(index);
            candidates.remove(index);
            
            if(m.getStats().getMaxBurnLevel().getModifiedValue() >= neededSpeed
                    && !m.isCivilian() && (acceptFighters || !m.isFighterWing())) {
                winners.add(m);
                fp += m.getFleetPointCost();
                if(m.isCarrier()) acceptFighters = true;
            }
            
            if(fp > neededFP) break;
            else if(candidates.isEmpty()) return;
        }
        
        pursuer = Global.getFactory().createEmptyFleet("sun_ice", "Pursuit Fleet", true);
        
        for(FleetMemberAPI m : winners) {
            pursuer.getFleetData().addFleetMember(m);
            Data.ExileFleet.getFleetData().removeFleetMember(m);
        }
        
        pursuer.getFleetData().sort();
        Data.ExileFleet.getContainingLocation().spawnFleet(Data.ExileFleet, 0, 0, pursuer);
        
        pursuer.addAssignment(FleetAssignment.INTERCEPT, target,
                AVG_PURSUIT_DAYS * (0.5f + (float)Math.random()));
        pursuer.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                Data.ExileFleet, 9999, "returning to the Colony Fleet",
                new JoinMotherFleetScript(Data.ExileFleet, pursuer));
    }
    void doShalomVisibilityHack() {
//        if(Ulterius.COLONY_FLEET_MARKET.getPrimaryEntity() instanceof CampaignFleetAPI) {
//            CampaignFleetAPI exiles = (CampaignFleetAPI)Ulterius.COLONY_FLEET_MARKET.getPrimaryEntity();
//            
//            if(Ulterius.SHALOM.getContainingLocation() != exiles.getContainingLocation()) {
//                Ulterius.SHALOM.getContainingLocation().removeEntity(Ulterius.SHALOM);
//                exiles.getContainingLocation().addEntity( Ulterius.SHALOM);
//            }
//            
//            if(exiles.isInHyperspaceTransition()) {
//                //Global.getSector().doHyperspaceTransition(exiles, exiles, null);
//            }
//            
//            Ulterius.SHALOM.getLocation().set(exiles.getLocation());
//            Ulterius.SHALOM.setFacing(Ulterius.SHALOM.getFacing() + amount * 10);
//        }
    }

    @Override
    public boolean isDone() { return false; }

    @Override
    public boolean runWhilePaused() { return false; }
}
