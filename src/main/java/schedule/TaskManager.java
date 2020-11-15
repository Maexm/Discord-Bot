package schedule;

import java.util.HashMap;
import java.util.Timer;
import java.util.UUID;

import exceptions.IllegalMagicException;

public class TaskManager<TaskType extends RefinedTimerTask> {
    
    private HashMap<UUID, TaskType> tasks;
    private Timer timer;
    private final boolean isDaemon;

    public TaskManager(boolean isDaemon){
        this.isDaemon = isDaemon;
        this.timer = new Timer(this.isDaemon);
        this.tasks = new HashMap<>();
    }

    public TaskManager(){
        this(false);
    }

    UUID getFreeUUID(){
        UUID ret = UUID.randomUUID();

        while(this.tasks.containsKey(ret)){
            ret = UUID.randomUUID();
        }
        return ret;
    }

    public void addTask(TaskType task){

        task.setUUID(this.getFreeUUID());

        // Correct scheduling

            // ########## No period ##########
        if(task.getPeriod() == null){
            if(task.getStartTime() != null){
                this.timer.schedule(task, task.getStartTime());
            }
            else if(task.getDelay() != null){
                this.timer.schedule(task, task.getDelay());
            }
            else{
                throw new IllegalMagicException("RefinedTimerTask delay and startDate properties are both null, how did this happen?!");
            }
        }
            // ########## Periodic task ##########
        else{
            if(task.getStartTime() != null){
                this.timer.scheduleAtFixedRate(task, task.getStartTime(), task.getPeriod());
            }
            else if(task.getDelay() != null){
                this.timer.scheduleAtFixedRate(task, task.getDelay(), task.getPeriod());
            }
            else{
                throw new IllegalMagicException("RefinedTimerTask delay and startDate properties are both null, how did this happen?!");
            }
        }
        this.tasks.put(task.getUUID(), task); // Won't be added to tasks, if scheduling failed
    }

    public TaskType getTaskByUUID(UUID uuid){
        return this.tasks.get(uuid);
    }

    /**
     * Invoked by a RefinedTimerTask on cleanCancel()
     * RefinedTimerTask has already canceled itsself. onTaskCancelsItsself() will now purge the timer object and remove the corresponding RefinedTimerTask from the HashMap.
     * @param taskUUID Identifier for the corresponding RefinedTimerTask instance
     */
    void onTaskCancelsItsself(UUID taskUUID){
        this.timer.purge();
        this.tasks.remove(taskUUID);
    }
}
