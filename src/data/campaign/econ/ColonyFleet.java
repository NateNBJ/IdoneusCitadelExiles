package data.campaign.econ;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;

public class ColonyFleet extends BaseMarketConditionPlugin {
	public static final float FUEL_BASE = 50f;
	public static final float FUEL_MAX = 1000f;
	public static final float SUPPLIES = 100f;
	public static final float FUEL_MULT = 0.0001f;
    
    public void apply(String id) {
        float pop = getPopulation(market);

        float fuelDemand = FUEL_BASE + pop * FUEL_MULT;
        if (fuelDemand > FUEL_MAX) {
            fuelDemand = FUEL_MAX;
        }

        market.getDemand(Commodities.SUPPLIES).getDemand().modifyFlat(id, SUPPLIES);
        market.getDemand(Commodities.FUEL).getDemand().modifyFlat(id, fuelDemand);

        market.getDemand(Commodities.SUPPLIES).getNonConsumingDemand().modifyFlat(id, SUPPLIES * 0.5f);
        market.getDemand(Commodities.FUEL).getNonConsumingDemand().modifyFlat(id, fuelDemand * 0.5f);
        
        float stabilityModification = -1;
        
        if(market.getPrimaryEntity() instanceof CampaignFleetAPI) {
            stabilityModification += ((CampaignFleetAPI)market.getPrimaryEntity()).getFleetPoints() / 100f;
        }
        
        market.getStability().modifyFlat(id, stabilityModification, "Fleet Size");
    }

    public void unapply(String id) {
//        market.getCommodityData(Commodities.GREEN_CREW).getSupply().unmodify(id);
//        market.getCommodityData(Commodities.REGULAR_CREW).getSupply().unmodify(id);
//        market.getCommodityData(Commodities.VETERAN_CREW).getSupply().unmodify(id);
//
//        market.getDemand(Commodities.REGULAR_CREW).getDemand().unmodify(id);
//        market.getDemand(Commodities.REGULAR_CREW).getNonConsumingDemand().unmodify(id);

        market.getDemand(Commodities.SUPPLIES).getDemand().unmodify(id);
        market.getDemand(Commodities.FUEL).getDemand().unmodify(id);

        market.getDemand(Commodities.SUPPLIES).getNonConsumingDemand().unmodify(id);
        market.getDemand(Commodities.FUEL).getNonConsumingDemand().unmodify(id);

        market.getStability().unmodify(id);
    }
}
