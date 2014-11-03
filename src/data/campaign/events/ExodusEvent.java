package data.campaign.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.MessagePriority;
import com.fs.starfarer.api.campaign.events.CampaignEventTarget;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.events.BaseEventPlugin;
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
            Ulterius.resetColonyFleetMarket();
            exiles.setMarket(null);
            exiles.setName("Vagrant Fleet");
            Data.ExileFleet = null;
            ended = true;
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
        exiles.addAssignment(FleetAssignment.GO_TO_LOCATION, destination.getStar(), 9999);
        exiles.addAssignment(FleetAssignment.RAID_SYSTEM, destination.getStar(), 9999);
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
}
