
package data.campaign.econ;

import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;

public class IdoneusTech extends BaseMarketConditionPlugin {
    @Override
    public void apply(String id) {
        float pop = getPopulation(market);
        market.getCommodityData("sun_ice_tech").getSupply().modifyFlat(id, (pop + 100000) * 0.02f);
    }
    @Override
    public void unapply(String id) {
        market.getCommodityData("sun_ice_tech").getSupply().unmodify();
    }
}
