package pk.ajneb97.tasks;

import pk.ajneb97.PlayerKits2;
import pk.ajneb97.utils.FoliaScheduler;

public class PlayerDataSaveTask {

	private PlayerKits2 plugin;
	private FoliaScheduler.ScheduledTaskWrapper taskWrapper;
	public PlayerDataSaveTask(PlayerKits2 plugin) {
		this.plugin = plugin;
	}
	
	public void end() {
		if (taskWrapper != null) {
			taskWrapper.cancel();
			taskWrapper = null;
		}
	}
	
	public void start(int seconds) {
		long ticks = seconds * 20L;
		
		taskWrapper = FoliaScheduler.runTaskTimerAsync(plugin, this::execute, 0L, ticks);
	}
	
	public void execute() {
		plugin.getConfigsManager().getPlayersConfigManager().saveConfigs();
	}
}
