package util;

public class LiftInfo {
    private Integer skierId;
    private Integer liftId;
    private Integer minute;
    private Integer waitTime;

    public LiftInfo(Integer skierId, Integer liftId, Integer minute, Integer waitTime) {
        this.skierId = skierId;
        this.liftId = liftId;
        this.minute = minute;
        this.waitTime = waitTime;
    }

    public Integer getSkierId() {
        return skierId;
    }

    public void setSkierId(Integer skierId) {
        this.skierId = skierId;
    }

    public Integer getLiftId() {
        return liftId;
    }

    public void setLiftId(Integer liftId) {
        this.liftId = liftId;
    }

    public Integer getMinute() {
        return minute;
    }

    public void setMinute(Integer minute) {
        this.minute = minute;
    }

    public Integer getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(Integer waitTime) {
        this.waitTime = waitTime;
    }

    @Override
    public String toString() {
        return "util.LiftInfo{" +
                "skierId=" + skierId +
                ", liftId=" + liftId +
                ", minute=" + minute +
                ", waitTime=" + waitTime +
                '}';
    }
}
