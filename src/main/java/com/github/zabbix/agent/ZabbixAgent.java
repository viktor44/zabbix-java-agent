package com.github.zabbix.agent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;

import com.github.zabbix.agent.log.LogConsoleHandler;
import com.github.zabbix.agent.log.LogFileHandler;
import com.github.zabbix.agent.log.LogFormatter;

import lombok.extern.java.Log;

/**
 * @author Victor Kadachigov
 */
@Log(topic="com.github.zabbix.agent")
public class ZabbixAgent
{
	private final ZabbixAgentConfig config;
	
	private List<ZabbixActiveAgent> activeAgents;
	
	public ZabbixAgent(String configFilePath)
	{
		config = new ZabbixAgentConfig(configFilePath);
		initLogger();
		log.log(Level.FINE, "{0}", config);
	}
	
	public void start()
	{
		boolean somethingStarted = false;
		
		if (config.isPassiveMode())
			somethingStarted |= startPassiveAgent();
		if (config.isActiveMode())
			somethingStarted |= startActiveAgent();
		
		if (!somethingStarted)
			log.warning("No Zabbix agents started");
	}

	private void initLogger()
	{
		Level level = null;
		switch (config.getDebugLevel())
		{
			case 0:
				level = Level.OFF;
				break;
			case 1:
				level = Level.SEVERE;
				break;
			case 2:
				level = Level.WARNING;
				break;
			case 3:
				level = Level.INFO;
				break;
			case 4:
				level = Level.FINE;
				break;
			case 5:
				level = Level.FINEST;
				break;
			default:
				throw new IllegalArgumentException("Invalid DebugLevel value " + config.getDebugLevel());
		}

//		log.setUseParentHandlers(false);
		
		try
		{
			String p = LogConsoleHandler.class.getName() + ".formatter=" + LogFormatter.class.getName() + "\n"
						+ LogFileHandler.class.getName() + ".formatter=" + LogFormatter.class.getName() + "\n";
			LogManager logManager = LogManager.getLogManager();
			logManager.readConfiguration(new ByteArrayInputStream(p.getBytes()));
			
			switch (config.getLogType())
			{
				case CONSOLE:
					log.addHandler(new LogConsoleHandler());
					break;
				case FILE:
					log.addHandler(new LogFileHandler(config.getLogFile()));
					break;
				case ALL:
					log.addHandler(new LogConsoleHandler());
					log.addHandler(new LogFileHandler(config.getLogFile()));
					break;
			}
		}
		catch (IOException | SecurityException ex)
		{
			System.err.println("Unable to write log to '" + config.getLogFile() + "': " + ex.getMessage());
		}
		
		log.setLevel(level);
		for (Handler handler : log.getHandlers())
			handler.setLevel(level);

//		for (Handler handler : Logger.getLogger("").getHandlers())
//			handler.setLevel(level);
	}

	private boolean startActiveAgent()
	{
		activeAgents = new ArrayList<>();
		
		for (int i = 0; i < config.getActiveServersCount(); i++)
		{
			if (i > 0) // start only one agent for now.
			{
				log.warning("Only first active server address was used");
				break;
			}
			ZabbixActiveAgent a = new ZabbixActiveAgent(config, i);
			activeAgents.add(a);
			Thread thread = new Thread(a);
			thread.setName(a.getName());
			thread.setDaemon(true);
			thread.start();
		}
		
		return !activeAgents.isEmpty();
	}
	
	private boolean startPassiveAgent()
	{
		log.warning("Passive mode is not implemented yet");
		
		return false;
	}
}
