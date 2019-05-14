package com.github.zabbix.agent;

import java.lang.instrument.Instrumentation;

/**
 * @author Victor Kadachigov
 */
public class JavaAgent
{
    public static void agentmain(String agentArgument, Instrumentation instrumentation) throws Exception 
    {
        premain(agentArgument, instrumentation);
    }

    public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception 
    {
    	try
    	{
    		(new ZabbixAgent(agentArgument)).start();
    	}
    	catch (IllegalArgumentException ex)
    	{
    		System.err.println("Error: " + ex.getMessage());
    		System.err.println("");
            System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=<Zabbix agent configuration file>");
            System.exit(1);
    	}
    }
}
