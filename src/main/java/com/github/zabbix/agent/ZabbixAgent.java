package com.github.zabbix.agent;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

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
		log.fine("" + config);
	}
	
	public void start()
	{
		if (config.isActiveMode())
			startActiveAgent();
	}

	private void initLogger()
	{
		if (System.getProperty("java.util.logging.SimpleFormatter.format") == null)
			System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %2$s %5$s%6$s%n");
		
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
			case 5:
				level = Level.FINE;
				break;
			default:
				throw new IllegalArgumentException("Invalid DebugLevel value " + config.getDebugLevel());
		}

		log.setUseParentHandlers(false);
		try
		{
			switch (config.getLogType())
			{
				case CONSOLE:
					log.addHandler(new ConsoleHandler());
					break;
				case FILE:
					log.addHandler(new FileHandler(config.getLogFile()));
					break;
				case ALL:
					log.addHandler(new ConsoleHandler());
					log.addHandler(new FileHandler(config.getLogFile()));
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

	private void startActiveAgent()
	{
		activeAgents = new ArrayList<>();
		
		for (int i = 0; i < config.getActiveServersCount(); i++)
		{
			ZabbixActiveAgent a = new ZabbixActiveAgent(config, i);
			activeAgents.add(a);
			Thread thread = new Thread(a);
			thread.setName(a.getName());
			thread.setDaemon(true);
			thread.start();
			
			// start only one agent for now.
			break;
		}
	}
	
	
}
