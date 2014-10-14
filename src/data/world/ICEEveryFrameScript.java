package data.world;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;

public class ICEEveryFrameScript implements EveryFrameScript {
    static final float INTERVAL = 60.0f;
    float timeSinceLastCheck = 0f;

    public ICEEveryFrameScript() {}
    
    @Override
    public void advance(float amount) {
        if((timeSinceLastCheck += amount) >= INTERVAL) {
            Ulterius.resetStationCargo();
            timeSinceLastCheck = 0;
            
            if(Global.getSector().getCurrentLocation().isHyperspace()
                    || Global.getSector().getFaction("sun_ice").getRelationship("player") < 0)
                return;
            
            for(CampaignFleetAPI fleet : Global.getSector().getCurrentLocation().getFleets()) {
                if(fleet.getFullName().equals("Idoneus Refugee Fleet")) {
                    Global.getSector().getCampaignUI().addMessage("An Idoneus Refugee Fleet in this system requests trade");
                }
            }
        }
    }

    @Override
    public boolean isDone() { return false; }

    @Override
    public boolean runWhilePaused() { return false; }
}
