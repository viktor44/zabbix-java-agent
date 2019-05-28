package com.github.zabbix.agent;

import java.util.Map;

/**
 * @author Victor Kadachigov
 */
public class TestApp
{
	public static void main(String[] args) throws Exception
	{
		System.out.println("Test Application");
		
//		for (Map.Entry<String, String> e : System.getenv().entrySet())
//			System.out.println(e.getKey() + "=" + e.getValue());
//		System.out.println("==================");
//		for (Object k :	System.getProperties().keySet())
//			System.out.println("" + k + "=" + System.getProperty(k.toString()));
		
		ZabbixAgent client = new ZabbixAgent("D:\\Projects\\zabbix-java-agent\\zabbix-java-agent\\etc\\zabbix_agentd.conf");
		client.start();
		
		System.in.read();
	}
}
