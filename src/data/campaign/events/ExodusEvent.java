package data.campaign.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.MessagePriority;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.events.CampaignEventManagerAPI;
import com.fs.starfarer.api.campaign.events.CampaignEventPlugin;
import com.fs.starfarer.api.campaign.events.CampaignEventTarget;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.events.BaseEventPlugin;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.tools.IceUtils;
import data.world.Data;
import data.world.Ulterius;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ExodusEvent extends BaseEventPlugin {
    static final float DAYS_BETWEEN_TRADE_OFFERS = 16f;
    static final float DAYS_BEFORE_INITIAL_TRADE_OFFERS = 3f;
    
    float elapsedDays = 0f;
    float dayOfNextTradeOffer = DAYS_BEFORE_INITIAL_TRADE_OFFERS;
    float daysToStay = 60;
    boolean ended = false;
    boolean traveling = true;
    boolean hasEnteredHyperspace = false;
    String previousSystem = "another System";
    String timeFrame = "months";
    CampaignFleetAPI exiles;
    StarSystemAPI destination;
    SectorAPI sector;

    @Override
    public void init(String type, CampaignEventTarget eventTarget) {
        super.init(type, eventTarget);
        
        sector = Global.getSector();
        exiles = (CampaignFleetAPI)eventTarget.getEntity();
        chooseNewHome();
        
        report("start");
    }

    @Override
    public void startEvent() {
        super.startEvent();
    }

    @Override
    public void advance(float amount) {
        if (!isEventStarted()) { return; }
        if (isDone()) { return; }

        float days = sector.getClock().convertToDays(amount);
        elapsedDays += days;
        
        if(!exiles.isAlive() || theColonyShipDied()) {
            report("destroyed");
            endColonyFleetEvents();
            Ulterius.resetColonyFleetMarket();
            exiles.setMarket(null);
            exiles.setName("Vagrant Fleet");
            Data.ExileFleet = null;
            ended = true;
            if (exiles.isAlive())
                orderDespawn();
        } else if(traveling && hasEnteredHyperspace && exiles.getContainingLocation() == destination) {
            report("arrive");
            hasEnteredHyperspace = traveling = false;
            elapsedDays = 0;
            dayOfNextTradeOffer = DAYS_BEFORE_INITIAL_TRADE_OFFERS;
            daysToStay = 75 + (float)Math.random() * 50;
            IceUtils.offerSystemBountyIfApt(market, 0.6f);
        } else if(!traveling && elapsedDays > daysToStay) {
            previousSystem = destination.getBaseName();
            chooseNewHome();
            report("move");
            traveling = true;
        } else if(Data.SendTradeOffers && !traveling && elapsedDays >= dayOfNextTradeOffer
                && !sector.getCurrentLocation().isHyperspace()
                && exiles.getContainingLocation() == sector.getCurrentLocation()
                && sector.getFaction("sun_ice").getRelationshipLevel("player").isAtWorst(RepLevel.SUSPICIOUS)) {
            report("tradeOffer");
            dayOfNextTradeOffer = elapsedDays + DAYS_BETWEEN_TRADE_OFFERS;
        }
        
        if(exiles.isInHyperspace()) {
            hasEnteredHyperspace = true;
        } else if(exiles.getCargo().getFuel() < 10) {
            exiles.getCargo().addFuel(exiles.getCargo().getMaxFuel() * 0.3f);
        }
    }

    @Override
    public boolean isDone() {
        return ended;
    }

    @Override
    public String getEventName() {
        return "Colony Fleet Exiled from Idoneus Citadel";
    }

    @Override
    public boolean showAllMessagesIfOngoing() {
        return false;
    }
    
    @Override
    public Map<String, String> getTokenReplacements() {
        Map<String, String> map = super.getTokenReplacements();
        map.put("$refugeSystem", destination.getBaseName());
        map.put("$previousSystem", previousSystem);
        map.put("$timeFrame", timeFrame);
        return map;
    }
    
    void chooseNewHome() {
        List systems = new ArrayList(sector.getStarSystems());
        systems.remove(sector.getStarSystem("Ulterius"));
        systems.remove(exiles.getContainingLocation());
        destination = (StarSystemAPI) systems.get(new Random().nextInt(systems.size()));

        exiles.clearAssignments();
        exiles.addAssignment(FleetAssignment.GO_TO_LOCATION, destination.getHyperspaceAnchor(), 9999);
        exiles.addAssignment(FleetAssignment.RAID_SYSTEM, destination.getStar(), 9999);
    }
    
    void orderDespawn()
    {
        List<MarketAPI> markets = sector.getEconomy().getMarketsCopy();
        markets.remove(Data.ExileMarket);
        markets.remove(Data.CitadelMarket);
        
        WeightedRandomPicker<MarketAPI> picker = new WeightedRandomPicker<>();
        for (MarketAPI tryMarket : markets)
        {
            LocationAPI loc = tryMarket.getPrimaryEntity().getContainingLocation();
            float weight = 1;
            if (tryMarket.getFaction().isHostileTo(exiles.getFaction())) 
                weight = 0.01f;
            if (loc == exiles.getLocation())
                weight *= 100;
            picker.add(tryMarket, weight);
        }
        if (picker.isEmpty()) return;
        
        MarketAPI selectedMarket = picker.pick();
        SectorEntityToken token = selectedMarket.getPrimaryEntity();
        Global.getLogger(ExodusEvent.class).info("Exile colony fleet to despawn at: " + selectedMarket.getName());
        exiles.clearAssignments();
        exiles.addAssignment(FleetAssignment.GO_TO_LOCATION, token, 9999);
        exiles.addAssignment(FleetAssignment.ORBIT_PASSIVE, token, getDaysToOrbit(), "settling on " + token.getName());
        exiles.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, selectedMarket.getPrimaryEntity(), 9999);
        
        // probably a debug mode thing?
        /*
        // give them enough supplies to last a bit longer (so they don't throw error messages in UI)
        float supplyCost = exiles.getLogistics().getTotalSuppliesPerDay();
        exiles.getCargo().addSupplies(supplyCost * 10);
        
        // make "no resupply location" error message go away
        exiles.setPreferredResupplyLocation(selectedMarket.getPrimaryEntity());
        */
    }
    
    void report(String stage) {
        sector.reportEventStage(this, stage, exiles, MessagePriority.SECTOR);
    }
    boolean theColonyShipDied() {
        for(FleetMemberAPI m : exiles.getFleetData().getMembersListCopy()) {
            if(m.getHullId().equals("sun_ice_shalom")) return false;
        }
        
        return true;
    }
    
    public static void endColonyFleetEvents() {
        CampaignEventTarget target = new CampaignEventTarget(Data.ExileFleet);
        CampaignEventManagerAPI mgt = Global.getSector().getEventManager();
        String[] eventTypes = new String[] { "food_shortage", "system_bounty", 
            "investigation", "investigation_smuggling", "recent_unrest",
            "trade_disruption" };
        
        for(String type : eventTypes) {
            CampaignEventPlugin event = mgt.getOngoingEvent(target, type);
            if(event != null) {        
                event.getEventTarget().setEntity(Data.PhantomEntity);
                event.advance(Global.getSector().getClock().getSecondsPerDay() * 60);
                mgt.endEvent(event);
                //event.cleanup();    // probably a bad idea for random classes to call this
            }
        }
    }
    
    protected float getDaysToOrbit()
    {
        float daysToOrbit = 0.0F;
        if (exiles.getFleetPoints() <= 50.0F) {
            daysToOrbit += 2.0F;
        } else if (exiles.getFleetPoints() <= 100.0F) {
            daysToOrbit += 4.0F;
        } else if (exiles.getFleetPoints() <= 150.0F) {
            daysToOrbit += 6.0F;
        } else {
            daysToOrbit += 8.0F;
        }
        daysToOrbit *= (0.5F + (float)Math.random() * 0.5F);
        return daysToOrbit;
    }
}
