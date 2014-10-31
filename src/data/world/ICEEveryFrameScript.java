package data.world;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

public class ICEEveryFrameScript implements EveryFrameScript {
    float elapsedDays = 0f;
    
    @Override
    public void advance(float amount) {
        SectorAPI sector = Global.getSector();
        float days = sector.getClock().convertToDays(amount);
        elapsedDays += days;
        
//        Data.ExileMarket.reapplyCondition("sun_ice_colony_fleet");
        Data.ExileMarket.reapplyConditions();
        
//        Data.ExileMarket.removeCondition("sun_ice_colony_fleet");
//        Data.ExileMarket.addCondition("sun_ice_colony_fleet");
        
//        if((timeSinceLastCheck += amount) >= INTERVAL) {
//            timeSinceLastCheck = 0;
//            
//            if(Global.getSector().getCurrentLocation().isHyperspace()
//                    || Global.getSector().getFaction("sun_ice").getRelationship("player") < 0)
//                return;
//            
//            for(CampaignFleetAPI fleet : Global.getSector().getCurrentLocation().getFleets()) {
//                if(fleet.getFullName().equals("Idoneus Refugee Fleet")) {
//                    Global.getSector().getCampaignUI().addMessage("The Exiled Idoneus Colony Fleet in this system requests trade");
//                }
//            }
//        }
        
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
