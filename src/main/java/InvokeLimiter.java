public class InvokeLimiter {

    private long minInvokePeriod;
    private long lastTime = 0;
    private long timer = 0;

    public InvokeLimiter(long minInvokePeriod) {
        this.minInvokePeriod = minInvokePeriod;
    }

    public boolean isInvokable() {
        timer += getInvokeInterval();
        if (timer >= minInvokePeriod) {
            reset();
            return true;
        } else {
            return false;
        }
    }

    private long getInvokeInterval() {
        long thisTime = now();
        if (lastTime == 0) {
            lastTime = thisTime;
            return 0;
        } else {
            long interval = (thisTime - lastTime);
            lastTime = thisTime;
            return interval;
        }
    }

    private void reset() {
        lastTime = 0;
        timer = 0;
    }

    private long now() {
        return System.currentTimeMillis();
    }

}
