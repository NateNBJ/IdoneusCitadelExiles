package data.world;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.events.CampaignEventPlugin;
import com.fs.starfarer.api.campaign.events.CampaignEventTarget;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.tools.IceUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ICEEveryFrameScript extends BaseCampaignEventListener implements EveryFrameScript {
    static final float AVG_UPDATE_INTERVAL = 0.3f;
    static final float AVG_PILGRIMAGE_INTERVAL = 5.3f;
    static final float MIN_COLONY_FLEET_FP = 175;
    static final float AVG_PURSUIT_DAYS = 8f;
    
    float elapsedDays = 0f;
    float dayOfNextUpdate = 0;
    float dayOfNextExodusCheck = 3;
    float dayOfNextCitadelBounty = 0;
    float lastDayOfLiveColony = -30;
    SectorAPI sector;
    CampaignFleetAPI pursuer;
    
    public ICEEveryFrameScript()
    {
        super(true);
    }
    
    @Override
    public void advance(float amount) {
        sector = Global.getSector();
        
        if(sector.isInNewGameAdvance() || sector.isPaused()) return;
        
        float days = sector.getClock().convertToDays(amount);
        elapsedDays += days;
        
        if(Data.ExileFleet != null) lastDayOfLiveColony = elapsedDays;
        
        if(elapsedDays > dayOfNextUpdate) {
            dayOfNextUpdate = elapsedDays + AVG_UPDATE_INTERVAL * (0.5f + (float)Math.random());
            if(pursuer == null) Data.ExileMarket.reapplyConditions();
            detachPursuitFleetIfApt();
            spawnVagrantFleetIfApt();
            detachDuellistIfApt();
        }
        
        if(elapsedDays > dayOfNextCitadelBounty) {
            dayOfNextCitadelBounty = elapsedDays + 75 + (float)Math.random() * 50;
            IceUtils.offerSystemBountyIfApt(Data.CitadelMarket, 1f);
        }
        
        if(elapsedDays > dayOfNextExodusCheck) {
            dayOfNextExodusCheck = elapsedDays + 15 + (float)Math.random() * 15;
            
            if(Data.ExileFleet == null && elapsedDays - lastDayOfLiveColony > 30 && Data.CitadelMarket.getFactionId().equals("sun_ici")) {
                spawnColonyFleet();
            }
        }
        
        //doShalomVisibilityHack();
    }
    void spawnColonyFleet() {
        LocationAPI loc = Data.IdoneusCitadel.getContainingLocation();
        CampaignFleetAPI fleet = sector.createFleet("sun_ice", "refugees");
        loc.spawnFleet(Data.IdoneusCitadel, 0, 0, fleet);

        Data.ExileFleet = fleet;
        Data.ExileMarket.getConnectedEntities().clear();
        Data.ExileMarket.setPrimaryEntity(fleet);
        fleet.setMarket(Data.ExileMarket);
        fleet.setInteractionImage("illustrations", "cargo_loading");
        //Data.ExileMarket.setSize(Ulterius.EXILE_MARKET_SIZE);
        Data.ExileMarket.reapplyConditions();
        
        Global.getSector().getEventManager().startEvent(
                new CampaignEventTarget(fleet), "sun_ice_exodus", null);
    }
    void detachDuellistIfApt() {
        if(Data.ExileFleet == null || !Data.ExileFleet.isAlive() || Math.random() > 0.01f
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
        duelist.addAssignment(FleetAssignment.GO_TO_LOCATION,
                Data.ExileFleet, 9999, "returning to the Colony Fleet",
                new JoinMotherFleetScript(Data.ExileFleet, duelist));
    }
    void spawnVagrantFleetIfApt() {
        if(Data.ExileFleet == null) return;
        
        float time = sector.getClock().getElapsedDaysSince(0);
        
        float wave1 = (0.5f + (float)Math.sin(time / 257) * 0.5f);
        float wave2 = (0.5f + (float)Math.cos(time / 30) * 0.5f);
        float size = 1 - Data.ExileFleet.getFleetPoints() / 1000f;
        
        float chance = 0.2f * size * wave1 * wave2;
        
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
        
        pilgrims.addAssignment(FleetAssignment.GO_TO_LOCATION,
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
        pursuer.addAssignment(FleetAssignment.GO_TO_LOCATION,
                Data.ExileFleet, 9999, "returning to the Colony Fleet",
                new JoinMotherFleetScript(Data.ExileFleet, pursuer));
    }
    /*
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
    void fixFoodShortageCrashHack() {
        CampaignFleetAPI exiles = null;
        int score = 0;
        LocationAPI location = sector.getStarSystem("Askonia");
        for(CampaignFleetAPI f :location.getFleets()) {
            if(f.getFaction().getId().equals("sun_ice") && f.getFleetPoints() > score) {
                score = f.getFleetPoints();
                exiles = f;
            }
        }
        Data.ExileFleet = exiles;
        Data.ExileMarket.getConnectedEntities().clear();
        Data.ExileMarket.setPrimaryEntity(exiles);
        exiles.setMarket(Data.ExileMarket);
        
//        CampaignEventPlugin e = sector.getEventManager().getOngoingEvent(new CampaignEventTarget(Data.ExileFleet), "food_shortage");
//        sector.getEventManager().endEvent(e);
        
        Ulterius.resetColonyFleetMarket();
        exiles.setMarket(null);
        Data.ExileFleet = null;
    }*/
    
    @Override
    public boolean isDone() { return false; }

    @Override
    public boolean runWhilePaused() { return false; }
    
    // purge trade fleets coming from Ulterius
    // FIXME: can't remove food relief fleets without affecting the event
    // FIXME: doesn't do anything about trade fleets coming from the other end
    //    need to be able to figure out if destination is exile market somehow
    @Override
    public void reportFleetSpawned(CampaignFleetAPI fleet) {
        if (Data.ExileFleet != null) return;
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        String type = memory.contains(MemFlags.MEMORY_KEY_FLEET_TYPE) ? memory.getString(MemFlags.MEMORY_KEY_FLEET_TYPE) : null;
        if (type == null) return;
        if (!type.equals(FleetTypes.TRADE) && !type.equals(FleetTypes.TRADE_SMALL) && !type.equals(FleetTypes.TRADE_SMUGGLER))
            return;
        
        String sourceMarket = memory.contains(MemFlags.MEMORY_KEY_SOURCE_MARKET) ? memory.getString(MemFlags.MEMORY_KEY_SOURCE_MARKET) : null;
        if (sourceMarket != null && sourceMarket.equals(Data.ExileMarket.getId()))
        {
            Global.getLogger(this.getClass()).info("Force-despawning trade fleet " + fleet.getName());
            fleet.despawn(CampaignEventListener.FleetDespawnReason.NO_REASON_PROVIDED, null);
        }
    }

}
