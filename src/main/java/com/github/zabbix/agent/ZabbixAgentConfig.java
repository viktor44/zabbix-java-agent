package com.github.zabbix.agent;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.github.zabbix.agent.data.ServerAddress;

import lombok.Getter;
import lombok.ToString;

/**
 * @author Victor Kadachigov
 */
@ToString(doNotUseGetters=true)
public class ZabbixAgentConfig
{
	private static final int DEFAULT_LISTEN_PORT = 10050;
	private static final int DEFAULT_REFRESH_ACTIVE_CHECKS = 120;
	private static final int DEFAULT_BUFFER_SEND = 5;
	private static final int DEFAULT_BUFFER_SIZE = 100;
	private static final int DEFAULT_TIMEOUT = 3;
	private static final int DEFAULT_DEBUG_LEVEL = 3;

	public static final int DEFAULT_SERVER_PORT = 10051;

	public enum LogType
	{
		CONSOLE,
		FILE,
		ALL;
	}
	
	@Getter
	private List<ServerAddress> servers;
	@Getter
	private int listenPort;
	private List<ServerAddress> activeServers;
	@Getter
	private String hostname;
	@Getter
	private String hostnameItem;
	@Getter
	private String hostMetadata;
	@Getter
	private String hostMetadataItem;
	/**
	 * How often list of active checks is refreshed, in seconds.<br>
	 * Range: 60-3600
	 */
	@Getter
	private int refreshActiveChecks;
	/** 
	 * Do not keep data longer than N seconds in buffer<br>
	 * Range: 1-3600 
	 */
	@Getter
	private int bufferSend;
	/**
	 * Maximum number of values in a memory buffer. The agent will send
	 * all collected data to Zabbix Server or Proxy if the buffer is full.<br>
	 * Range: 2-65535
	 */
	@Getter
	private int bufferSize;
	/**
	 * Spend no more than Timeout seconds on processing<br>
	 * Range: 1-30
	 */
	@Getter
	private int timeout;
	/**
	 * Specifies debug level:
	 * <ul>
	 * <li>0 - basic information about starting and stopping of Zabbix processes
	 * <li>1 - critical information
	 * <li>2 - error information
	 * <li>3 - warnings
	 * <li>4 - for debugging (produces lots of information)
	 * <li>5 - extended debugging (produces even more information)
	 * </ul>
	 * Range: 0-5
	 */
	@Getter
	private int debugLevel;
	@Getter
	private LogType logType;
	@Getter
	private String logFile;

	// for test only
	ZabbixAgentConfig()
	{
	}
	
	public ZabbixAgentConfig(String configFilePath)
	{
		Map<String, String> configItems = Collections.emptyMap();
		Reader reader = null;
		try
		{
			reader = new FileReader(configFilePath);
			configItems = loadConfig(reader);
		}
		catch (IOException ex)
		{
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
		finally 
		{
			if (reader != null)
				try
				{
					reader.close();
				}
				catch (Exception ex)
				{
					// DO NOTHING
				}
		}
		
		servers = ServerAddress.parse(getStringParam(configItems, "Server"));
		listenPort = getIntParam(configItems, "ListenPort", DEFAULT_LISTEN_PORT);
		hostname = getStringParam(configItems, "Hostname");
		if (hostname == null)
			hostname = getComputerHostname();
		hostnameItem = getStringParam(configItems, "HostnameItem");
		hostMetadata = getStringParam(configItems, "HostMetadata");
		hostMetadataItem = getStringParam(configItems, "HostMetadataItem");
		refreshActiveChecks = getIntParam(configItems, "RefreshActiveChecks", DEFAULT_REFRESH_ACTIVE_CHECKS);
		bufferSend = getIntParam(configItems, "BufferSend", DEFAULT_BUFFER_SEND);
		bufferSize = getIntParam(configItems, "BufferSize", DEFAULT_BUFFER_SIZE);
		activeServers = ServerAddress.parse(getStringParam(configItems, "ServerActive"));
		timeout = getIntParam(configItems, "Timeout", DEFAULT_TIMEOUT);
		debugLevel = getIntParam(configItems, "DebugLevel", DEFAULT_DEBUG_LEVEL);
		logType = LogType.valueOf(getStringParam(configItems, "HostMetadataItem", LogType.CONSOLE.name()).toUpperCase());
		logFile = getStringParam(configItems, "LogFile");
		if ((logType == LogType.FILE || logType == LogType.ALL)
				&& (logFile == null || "".equals(logFile)))
			throw new IllegalArgumentException("Missing required config parameter 'LogFile'");
	}
	
	private Map<String, String> loadConfig(Reader reader) throws IOException
	{
		Map<String, String> result = new HashMap<>();
		
		BufferedReader bufferedReader = new BufferedReader(reader);
		int lineNum = 0;
		String line;
		while ((line = bufferedReader.readLine()) != null)
		{
			lineNum++;
			if (isComment(line))
				continue;
			if ("".equals(line.trim()))
				continue;
			int index = line.indexOf('=');
			if (index < 0)
				throw new IllegalArgumentException("Invalid config file syntax at line " + lineNum);
			String key = line.substring(0, index).trim();
			String value = line.substring(index + 1).trim();
			if (result.containsKey(key))
				throw new IllegalArgumentException("Duplicate parameter '" + key + "' at line " + lineNum);
			result.put(key, value);
		}
		
		return result;
	}
	
	private int getIntParam(Map<String, String> configItems, String paramName, int defaultValue)
	{
		int result = defaultValue;
		String value = configItems.get(paramName);
		if (value != null)
		{
			try
			{
				result = Integer.parseInt(value.trim());
			}
			catch (Exception ex)
			{
				throw new IllegalArgumentException("Invalid number value for parameter '" + paramName + "'");
			}
		}
		return result;
 	}

	private String getStringParam(Map<String, String> configItems, String paramName)
	{
		return getStringParam(configItems, paramName, null);
	}
	
	private String getStringParam(Map<String, String> configItems, String paramName, String defaultValue)
	{
		String result = configItems.get(paramName);
		String envKey = makeEnvKey(paramName, "ZBX");
		String envValue = System.getenv(envKey);
		if (envValue != null)
		{
			System.out.println("Parameter '" + paramName + "' was replaced from environment variable '" + envKey + "'");
			result = envValue;
		}
		if (result == null) 
			result = defaultValue;
		else
			result = result.trim();
		return result;
	}

	private boolean isComment(String line)
	{
		boolean result = false;
		for (int i = 0; i < line.length(); i++)
		{
			char c = line.charAt(i);
			if (Character.isWhitespace(c))
				continue;
			result = (c == '#');
			break;
		}
		return result;
	}
	
	public boolean isActiveMode()
	{
		return !activeServers.isEmpty();
	}
	
	public int getActiveServersCount()
	{
		return activeServers.size();
	}
	
	public ServerAddress getActiveServer(int index)
	{
		return activeServers.get(index);
	}

	String makeEnvKey(String key, String prefix)
	{
		StringBuilder envKey = new StringBuilder(prefix != null ? prefix : "");
		if (envKey.length() > 0)
			envKey.append('_');
		boolean secondDigit = false;
		for (int i = 0; i < key.length(); i++)
		{
			char ch = key.charAt(i);
			if (i > 0 
					&& (Character.isUpperCase(ch)
							|| (Character.isDigit(ch) && !secondDigit)))
				envKey.append('_');
			secondDigit = Character.isDigit(ch);
			envKey.append(Character.toUpperCase(ch));
		}
		return envKey.toString();
	}

//	public static void main(String[] args)
//	{
//		System.out.println("Name: " + getComputerHostname());
//	}
	
	private String getComputerHostname()
	{
		// try InetAddress.LocalHost first;
		// NOTE -- InetAddress.getLocalHost().getHostName() will not work in certain environments.
		try
		{
			String result = InetAddress.getLocalHost().getHostName();
			if (result != null && !"".equals(result.trim())) 
				return result;
		}
		catch (UnknownHostException e)
		{
			// failed; try alternate means.
		}

		// try environment properties.
		String host = System.getenv("COMPUTERNAME");
		if (host != null) 
			return host;
		host = System.getenv("HOSTNAME");
		if (host != null) 
			return host;

		// undetermined.
		return null;
	}
}
