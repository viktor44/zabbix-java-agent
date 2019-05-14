package com.github.zabbix.agent.data;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.zabbix.agent.ZabbixAgentConfig;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Victor Kadachigov
 */
@ToString(doNotUseGetters=true)
@EqualsAndHashCode(doNotUseGetters=true, onlyExplicitlyIncluded=true)
public class ServerAddress
{
	@Getter
	@EqualsAndHashCode.Include
	private String host;
	@Getter
	@EqualsAndHashCode.Include
	private int port;
	@ToString.Exclude
	private SocketAddress socketAddress;

	ServerAddress(String host, int port)
	{
		this.host = host;
		this.port = port;
	}

	public static List<ServerAddress> parse(String line)
	{
		if (line == null)
			return Collections.emptyList();
		
		List<ServerAddress> result = new ArrayList<>();
		
		String servers[] = line.split(",");
		for (String s : servers)
		{
			s = s.trim();
			if ("".equals(s))
				continue;
			if (s.startsWith("[")) // IPv6
			{
				String host;
				int port;
				int n = s.indexOf(']');
				if (n < 0)
					throw new IllegalArgumentException("Wrong server address format. ']' expected");
				host = s.substring(1, n);
				if (n == s.length() - 1)
					port = ZabbixAgentConfig.DEFAULT_SERVER_PORT;
				else
					port = Integer.parseInt(s.substring(n + 2));
				result.add(new ServerAddress(host, port));
			}
			else
			{
				int i = s.indexOf(':');
				if (i < 0)
					result.add(new ServerAddress(s, ZabbixAgentConfig.DEFAULT_SERVER_PORT));
				else if (s.substring(i + 1).indexOf(':') >= 0) // IPv6 without brackets 
					result.add(new ServerAddress(s, ZabbixAgentConfig.DEFAULT_SERVER_PORT));
				else
					result.add(new ServerAddress(s.substring(0, i), Integer.parseInt(s.substring(i + 1))));
			}
		}
		return result;
	}
	
	public SocketAddress getSocketAddress()
	{
		if (socketAddress == null)
		{
			try
			{
				InetAddress addr = InetAddress.getByName(host); // check hostname
				socketAddress = new InetSocketAddress(addr, port);
			}
			catch (UnknownHostException ex)
			{
				throw new RuntimeException(ex);
			}
		}
		return socketAddress;
	}
}
