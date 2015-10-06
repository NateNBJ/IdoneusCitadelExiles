package data.campaign.econ;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import data.world.Data;

public class ColonyFleet extends BaseMarketConditionPlugin {
	public static final float FUEL_BASE = 50f;
	public static final float FUEL_MAX = 1000f;
	public static final float SUPPLIES = 100f;
	public static final float FUEL_MULT = 0.0001f;
	
	public static final float SUPPLY_BOOSTER_MULT = 0.5f;
    
    protected float getCommodityDemand(MarketAPI market, String commodity)
    {
        return market.getCommodityData(commodity).getDemand().getDemandValue();
    }
    
    protected void applySupplyBoosters(String id)
    {
        float mult = SUPPLY_BOOSTER_MULT;
        // generate enough food to prevent food shortage event (which used to lead to a crash and is generally screwy)
        // needs to be a pretty big surplus because at least some will be smuggled out
        float bonusFood = 0;	//200;	// just use the exemptFromFoodShortages tag    
        if (Data.ExileFleet == null)
        {
            //mult *= 2;
            //bonusFood = 100;
        }
        market.getCommodityData(Commodities.SUPPLIES).getSupply().modifyFlat(id, getCommodityDemand(market, Commodities.SUPPLIES) * mult);
        market.getCommodityData(Commodities.FUEL).getSupply().modifyFlat(id, getCommodityDemand(market, Commodities.FUEL) * mult);
        market.getCommodityData(Commodities.FOOD).getSupply().modifyFlat(id, getCommodityDemand(market, Commodities.FOOD) * mult + bonusFood);
        market.getCommodityData(Commodities.DOMESTIC_GOODS).getSupply().modifyFlat(id, getCommodityDemand(market, Commodities.DOMESTIC_GOODS) * mult);
        market.getCommodityData(Commodities.DRUGS).getSupply().modifyFlat(id, getCommodityDemand(market, Commodities.DRUGS) * mult);
        market.getCommodityData(Commodities.HAND_WEAPONS).getSupply().modifyFlat(id, getCommodityDemand(market, Commodities.HAND_WEAPONS) * mult);
        market.getCommodityData(Commodities.MARINES).getSupply().modifyFlat(id, getCommodityDemand(market, Commodities.MARINES) * mult);
        market.getCommodityData(Commodities.ORGANICS).getSupply().modifyFlat(id, getCommodityDemand(market, Commodities.ORGANICS) * mult);
        market.getCommodityData(Commodities.VOLATILES).getSupply().modifyFlat(id, getCommodityDemand(market, Commodities.VOLATILES) * mult);
        market.getCommodityData(Commodities.GREEN_CREW).getSupply().modifyFlat(id, getCommodityDemand(market, Commodities.GREEN_CREW) * mult * 0.5f);
        market.getCommodityData(Commodities.REGULAR_CREW).getSupply().modifyFlat(id, getCommodityDemand(market, Commodities.REGULAR_CREW) * mult * 0.3f);
        market.getCommodityData(Commodities.VETERAN_CREW).getSupply().modifyFlat(id, getCommodityDemand(market, Commodities.VETERAN_CREW) *  mult * 0.1f);
        market.getCommodityData(Commodities.ELITE_CREW).getSupply().modifyFlat(id, getCommodityDemand(market, Commodities.ELITE_CREW) * mult * 0.04f);
        market.getCommodityData(Commodities.HEAVY_MACHINERY).getSupply().modifyFlat(id, getCommodityDemand(market, Commodities.HEAVY_MACHINERY) * mult);
        market.getCommodityData(Commodities.METALS).getSupply().modifyFlat(id, getCommodityDemand(market, Commodities.METALS) * mult);
        market.getCommodityData(Commodities.RARE_METALS).getSupply().modifyFlat(id, getCommodityDemand(market, Commodities.RARE_METALS) * mult);
    }
    
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
            stabilityModification += ((CampaignFleetAPI)market.getPrimaryEntity()).getFleetPoints() / 130f;
        }
        
        applySupplyBoosters(id);
        
        market.getStability().modifyFlat(id, stabilityModification, "Fleet size");
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

        market.getCommodityData(Commodities.FOOD).getSupply().unmodify(id);
        market.getCommodityData(Commodities.DOMESTIC_GOODS).getSupply().unmodify(id);
        market.getCommodityData(Commodities.DRUGS).getSupply().unmodify(id);
        market.getCommodityData(Commodities.FUEL).getSupply().unmodify(id);
        market.getCommodityData(Commodities.HAND_WEAPONS).getSupply().unmodify(id);
        market.getCommodityData(Commodities.MARINES).getSupply().unmodify(id);
        market.getCommodityData(Commodities.ORGANICS).getSupply().unmodify(id);
        market.getCommodityData(Commodities.VOLATILES).getSupply().unmodify(id);
        market.getCommodityData(Commodities.GREEN_CREW).getSupply().unmodify(id);
        market.getCommodityData(Commodities.REGULAR_CREW).getSupply().unmodify(id);
        market.getCommodityData(Commodities.VETERAN_CREW).getSupply().unmodify(id);
        market.getCommodityData(Commodities.ELITE_CREW).getSupply().unmodify(id);
        market.getCommodityData(Commodities.HEAVY_MACHINERY).getSupply().unmodify(id);
        market.getCommodityData(Commodities.SUPPLIES).getSupply().unmodify(id);
        market.getCommodityData(Commodities.METALS).getSupply().unmodify(id);
        market.getCommodityData(Commodities.RARE_METALS).getSupply().unmodify(id);
        
        market.getStability().unmodify(id);
    }
}
