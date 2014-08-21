package data.world;

import com.fs.starfarer.api.EveryFrameScript;
import data.world.Ulterius;

public class ICEEveryFrameScript implements EveryFrameScript {
    static final float INTERVAL = 60.0f;
    float timeSinceLastCheck = 0f;

    @Override
    public void advance(float amount) {
        if((timeSinceLastCheck += amount) >= INTERVAL) {
            Ulterius.resetStationCargo();
            timeSinceLastCheck = 0;
        }
    }

    @Override
    public boolean isDone() { return false; }

    @Override
    public boolean runWhilePaused() { return false; }
}
