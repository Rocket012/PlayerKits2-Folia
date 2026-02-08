package pk.ajneb97.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Utility class to handle scheduling tasks for both Folia and non-Folia servers.
 * Folia requires region-specific or entity-specific task scheduling instead of
 * the traditional global scheduler.
 */
public class FoliaScheduler {

    private static Boolean isFolia = null;

    /**
     * Checks if the server is running Folia.
     * @return true if Folia is detected, false otherwise
     */
    public static boolean isFolia() {
        if (isFolia == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {
                isFolia = false;
            }
        }
        return isFolia;
    }

    /**
     * Runs a task synchronously. On Folia, this runs on the global region scheduler.
     * @param plugin The plugin instance
     * @param task The task to run
     */
    public static void runTask(Plugin plugin, Runnable task) {
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Runs a task asynchronously.
     * @param plugin The plugin instance
     * @param task The task to run
     */
    public static void runTaskAsync(Plugin plugin, Runnable task) {
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    /**
     * Runs a task later synchronously. On Folia, this runs on the global region scheduler.
     * @param plugin The plugin instance
     * @param task The task to run
     * @param delayTicks The delay in ticks
     */
    public static void runTaskLater(Plugin plugin, Runnable task, long delayTicks) {
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Runs a task later asynchronously.
     * @param plugin The plugin instance
     * @param task The task to run
     * @param delayTicks The delay in ticks (converted to milliseconds for Folia)
     */
    public static void runTaskLaterAsync(Plugin plugin, Runnable task, long delayTicks) {
        if (isFolia()) {
            // Convert ticks to milliseconds (1 tick = 50ms)
            long delayMs = delayTicks * 50L;
            Bukkit.getAsyncScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayMs, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        }
    }

    /**
     * Runs a repeating task synchronously. On Folia, this runs on the global region scheduler.
     * @param plugin The plugin instance
     * @param task The task to run
     * @param delayTicks The initial delay in ticks (Folia requires minimum of 1)
     * @param periodTicks The period in ticks
     * @return A ScheduledTaskWrapper that can be used to cancel the task
     */
    public static ScheduledTaskWrapper runTaskTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            // Folia requires minimum delay of 1 tick for runAtFixedRate
            io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduledTask =
                    Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), Math.max(delayTicks, 1), periodTicks);
            return new ScheduledTaskWrapper(scheduledTask);
        } else {
            int taskId = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks).getTaskId();
            return new ScheduledTaskWrapper(taskId);
        }
    }

    /**
     * Runs a repeating task asynchronously.
     * @param plugin The plugin instance
     * @param task The task to run
     * @param delayTicks The initial delay in ticks
     * @param periodTicks The period in ticks
     * @return A ScheduledTaskWrapper that can be used to cancel the task
     */
    public static ScheduledTaskWrapper runTaskTimerAsync(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            // Convert ticks to milliseconds (1 tick = 50ms)
            // Folia requires minimum delay of 1ms for runAtFixedRate
            long delayMs = Math.max(delayTicks * 50L, 1);
            long periodMs = periodTicks * 50L;
            io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduledTask =
                    Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> task.run(), 
                            delayMs, periodMs, TimeUnit.MILLISECONDS);
            return new ScheduledTaskWrapper(scheduledTask);
        } else {
            int taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks).getTaskId();
            return new ScheduledTaskWrapper(taskId);
        }
    }

    /**
     * Runs a task on the entity's scheduler. On non-Folia, this falls back to the global scheduler.
     * @param plugin The plugin instance
     * @param entity The entity to run the task for
     * @param task The task to run
     * @param retired The task to run if the entity is retired (removed/dead) - can be null
     */
    public static void runAtEntity(Plugin plugin, Entity entity, Runnable task, Runnable retired) {
        if (isFolia()) {
            entity.getScheduler().run(plugin, scheduledTask -> task.run(), retired);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Runs a task later on the entity's scheduler. On non-Folia, this falls back to the global scheduler.
     * @param plugin The plugin instance
     * @param entity The entity to run the task for
     * @param task The task to run
     * @param retired The task to run if the entity is retired (removed/dead) - can be null
     * @param delayTicks The delay in ticks
     */
    public static void runAtEntityLater(Plugin plugin, Entity entity, Runnable task, Runnable retired, long delayTicks) {
        if (isFolia()) {
            entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), retired, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Wrapper class for scheduled tasks that works with both Folia and non-Folia servers.
     */
    public static class ScheduledTaskWrapper {
        private Object task;
        private boolean isFoliaTask;

        public ScheduledTaskWrapper(io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
            this.task = task;
            this.isFoliaTask = true;
        }

        public ScheduledTaskWrapper(int taskId) {
            this.task = taskId;
            this.isFoliaTask = false;
        }

        /**
         * Cancels the scheduled task.
         */
        public void cancel() {
            if (isFoliaTask) {
                ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) task).cancel();
            } else {
                Bukkit.getScheduler().cancelTask((int) task);
            }
        }

        /**
         * Checks if the task has been cancelled.
         * @return true if cancelled, false otherwise
         */
        public boolean isCancelled() {
            if (isFoliaTask) {
                return ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) task).isCancelled();
            } else {
                return !Bukkit.getScheduler().isCurrentlyRunning((int) task) && 
                       !Bukkit.getScheduler().isQueued((int) task);
            }
        }
    }
}
