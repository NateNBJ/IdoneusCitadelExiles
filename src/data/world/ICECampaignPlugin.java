package data.world;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;

public class ICECampaignPlugin extends BaseCampaignPlugin {
    
    @Override
    public String getId() {
        return null;
    }

    @Override
    public boolean isTransient() {
        return false;
    }

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
        String name = interactionTarget.getFullName();
        if (interactionTarget instanceof CampaignFleetAPI && name.equals("Exiled Idoneus Colony Fleet")) {
            return new PluginPick<InteractionDialogPlugin>(new RefugeeFleetInteractionDialogPlugin(), PickPriority.MOD_SPECIFIC);
        }
        return null;
    }
}
