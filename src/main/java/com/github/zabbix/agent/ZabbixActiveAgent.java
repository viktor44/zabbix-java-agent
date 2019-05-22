package com.github.zabbix.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.github.zabbix.agent.data.CheckItem;
import com.github.zabbix.agent.data.CheckResult;
import com.github.zabbix.agent.data.ServerAddress;
import com.github.zabbix.agent.util.DaemonThreadFactory;
import com.github.zabbix.agent.util.FixedSizeQueue;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

/**
 * @author Victor Kadachigov
 */
@Log(topic="com.github.zabbix.agent")
public class ZabbixActiveAgent implements Runnable 
{
	private enum State
	{
		STOPPED,
		ACTIVE,
		STOPPING
	};
	
	private final ZabbixAgentConfig config;
	private final int serverIndex;
	private final ServerAddress serverAddress;
	private final Protocol protocol;
	private final Queue<CheckResult> resultsQueue = new FixedSizeQueue<>(500000); // <100Mb
	private final Map<Integer, Pair<CheckerTask, ScheduledFuture<?>>> checkerTasks = new HashMap<>();
	
	private State state = State.STOPPED;
	private ScheduledExecutorService scheduler;
	private long lastRefreshCheckTime = 0;
	private long lastResultsSendTime;
	private boolean connected = false;
	
	public ZabbixActiveAgent(ZabbixAgentConfig config, int serverIndex)
	{
		this.config = config;
		this.serverIndex = serverIndex;
		this.serverAddress = config.getActiveServer(serverIndex);
		this.protocol = new Protocol(serverAddress, config);
	}
	
	@Override
	public void run()
	{
		state = State.ACTIVE;
		lastResultsSendTime = System.currentTimeMillis();
		scheduler = Executors.newScheduledThreadPool(0, new DaemonThreadFactory("zabbix-agent-pool"));
		
		while (state == State.ACTIVE)
		{
			refreshChecks();
			sendResults();
			
			try
			{
				Thread.sleep(200L);
			}
			catch (InterruptedException ex)
			{
				Thread.currentThread().interrupt();
			}
		}
	}
	
	private void sendResults()
	{
		if (!connected 
				|| System.currentTimeMillis() - lastResultsSendTime < config.getBufferSend() * 1000L 
						&& resultsQueue.size() < config.getBufferSize())
			return;

		try
		{
			List<CheckResult> checksToSend = new ArrayList<>();
			checksToSend.addAll(resultsQueue);
//			if (!resultsQueue.isEmpty())  // one by one
//				checksToSend.add(resultsQueue.iterator().next());
			
			if (!checksToSend.isEmpty())
			{
				log.log(Level.FINE, "{0} items to send", checksToSend.size());
				
				protocol.sendCheckResults(checksToSend);
				resultsQueue.removeAll(checksToSend);
			}
			else
				log.fine("No data to send");
		}
		catch (ZabbixException ex) 
		{
			log.severe(ex.getMessage());
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, "{0}: {1}", new Object[] { ex.getClass().getName(), ex.getMessage() });
			log.log(Level.FINE, ex.getMessage(), ex);
		}
		
		lastResultsSendTime = System.currentTimeMillis();
	}

	private void refreshChecks()
	{
		long t = 60000; // 60s - timeout while not connected
		if (connected) 
			t = config.getRefreshActiveChecks() * 1000L;
		if (System.currentTimeMillis() - lastRefreshCheckTime < t)
			return;

		try
		{
			Set<CheckItem> checkItems = protocol.refreshActiveChecks();
			connected = true;
			scheduleChecks(checkItems);
		}
		catch (ZabbixException ex) 
		{
			if (connected)
				log.severe(ex.getMessage());
			connected = false;
		}
		catch (Exception ex)
		{
			if (connected)
			{
				log.log(Level.SEVERE, "{0}: {1}", new Object[] { ex.getClass().getName(), ex.getMessage() });
				log.log(Level.FINE, ex.getMessage(), ex);
			}
			connected = false;
		}
		
		lastRefreshCheckTime = System.currentTimeMillis();
	}

	private void scheduleChecks(Set<CheckItem> checkItems)
	{
		log.log(Level.FINE, "Schedule {0} checks", checkItems.size());
		
		Map<Integer, Set<CheckItem>> map = new HashMap<>();
		for (CheckItem item : checkItems)
		{
			Set<CheckItem> set = map.get(item.getDelay());
			if (set == null)
			{
				set = new HashSet<>();
				map.put(item.getDelay(), set);
			}
			set.add(item);
		}
		
		// delete
		Set<Integer> toDelete = new HashSet<>();
		for (Map.Entry<Integer, Pair<CheckerTask, ScheduledFuture<?>>> entry : checkerTasks.entrySet())
		{
			if (!map.containsKey(entry.getKey()))
			{
				// stop task
				log.log(Level.FINE, "Stop task with delay {0}", entry.getKey());
				
				entry.getValue().getValue().cancel(false);
				toDelete.add(entry.getKey());
			}
			else if (entry.getValue().getValue().isCancelled()) // something happened
			{
				log.log(Level.FINE, "Something wrong. Task with delay {0} already canceled", entry.getKey());
				
				toDelete.add(entry.getKey());
			}
		}
		for (Integer d : toDelete)
			checkerTasks.remove(d);
		
		// insert, update
		for (Map.Entry<Integer, Set<CheckItem>> entry : map.entrySet())
		{
			Pair<CheckerTask, ScheduledFuture<?>> task = checkerTasks.get(entry.getKey());
			if (task == null) // insert
			{
				log.log(Level.FINE, "Start {0} checks with delay {1}s", new Object[] {entry.getValue().size(), entry.getKey()});
				task = new Pair<>();
				task.setKey(new CheckerTask(checkItems, config, resultsQueue));
				task.setValue(scheduler.scheduleAtFixedRate(task.getKey(), 1, entry.getKey(), TimeUnit.SECONDS));
			}
			else // update
				task.getKey().updateCheckItems(entry.getValue());
		}
	}

	public void stop()
	{
		log.info("ZabbixActiveAgent is stopping...");
		
		state = State.STOPPING;
	}

	public String getName()
	{
		return "zabbix-agent-main-" + (serverIndex + 1);
	}
	
	private class Pair<K,V>
	{
		@Getter
		@Setter
	    private K key;
		@Getter
		@Setter
	    private V value;
	}
}
