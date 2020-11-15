package schedule;

import java.util.Date;
import java.util.TimerTask;
import java.util.UUID;

import exceptions.IllegalMagicException;

public abstract class RefinedTimerTask extends TimerTask {
    
    private final Long delay;
    private final Long period;
    private final Date startDate;
    private TaskManager manager;
    private UUID uuid;

    public RefinedTimerTask(Long delay, Long period, Date startDate, TaskManager manager){

        if(manager == null){
            throw new NullPointerException("Manager must not be null");
        }

        if(delay == null && startDate == null){
            throw new NullPointerException("Either delay or startDate must not be null");
        }

        this.delay = delay;
        this.period = period;
        this.startDate = startDate;
        this.manager = manager;

        this.uuid = null;
    }

    @Override
    public void run(){
        this.runTask();
        
        // Perform clean cancel, if task is not periodic
        if(!this.isPeriodic()){
            this.cleanCancel();
        }
    }

    public abstract void runTask();

    // ########## GETTER ##########

    public Long getDelay(){
        return this.delay;
    }

    public Long getPeriod(){
        return this.period;
    }

    public Date getStartTime(){
        return this.startDate;
    }

    public UUID getUUID(){
        return this.uuid;
    }

    public boolean isPeriodic(){
        return this.getPeriod() != null;
    }

    // ########## OTHER ##########

    /**
     * Cancels itsself and informs manager about this (will perform purge() and remove task from HashMap)
     */
    public void cleanCancel(){
        this.cancel();
        this.manager.onTaskCancelsItsself(this.getUUID());
        this.manager = null;
    }

    void setUUID(UUID uuid){
        if(this.getUUID() != null){
            throw new IllegalMagicException("UUID of RefinedTimerTask should not be set more than once!");
        }
        this.uuid = uuid;
    }


}
