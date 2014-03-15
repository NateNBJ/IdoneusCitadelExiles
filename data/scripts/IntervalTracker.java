package data.scripts;

import com.fs.starfarer.api.Global;

public class IntervalTracker {
    float timeOfNextElapse, min, max;
    boolean includePausedTime;
    
    final void init(float min, float max, boolean includePausedTime) {
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
        this.includePausedTime = includePausedTime;
        this.timeOfNextElapse = 0;
    }
    float incrementInterval(float time) {
        return timeOfNextElapse = time + ((min == max)
                ? min : min + (max - min) * (float)Math.random());
    }
    
    public IntervalTracker() {
        init(1, 1, false);
    }
    public IntervalTracker(float intervalDuration) {
        init(intervalDuration, intervalDuration, false);
    }
    public IntervalTracker(float minIntervalDuration, float maxIntervalDuration) {
        init(minIntervalDuration, maxIntervalDuration, false);
    }
    public IntervalTracker(float minIntervalDuration, float maxIntervalDuration, boolean includePausedTime) {
        init(minIntervalDuration, maxIntervalDuration, includePausedTime);
    }

    public boolean intervalIsFixed() {
        return min == max;
    }
    public float getAverageInterval() {
        return (min + max) / 2;
    }
    public float getMinimumInterval() {
        return min;
    }
    public float getMaximumInterval() {
        return max;
    }
    public void setInterval(float intervalDuration) {
        min = max = intervalDuration;
    }
    public boolean intervalElapsed() {
        float time = Global.getCombatEngine().getTotalElapsedTime(includePausedTime);
        
        if(timeOfNextElapse <= time) {
            incrementInterval(timeOfNextElapse);
            if(timeOfNextElapse <= time) incrementInterval(time);
            return true;
        } else return false;
    }
}
