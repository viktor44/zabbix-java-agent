package com.github.zabbix.agent;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Victor Kadachigov
 */
public class ZabbixAgentConfigTest
{
	@Test
	public void makeEnvKeyTest() throws Exception
	{
		ZabbixAgentConfig c = new ZabbixAgentConfig();
		Assert.assertEquals("ZBX_LOG_TYPE", c.makeEnvKey("LogType", "ZBX"));
		Assert.assertEquals("ZBX_LOG_TYPE_223", c.makeEnvKey("LogType223", "ZBX"));
		Assert.assertEquals("ZBX_223_LOG_TYPE", c.makeEnvKey("223LogType", "ZBX"));
		Assert.assertEquals("ZBX_LOG_TYPE", c.makeEnvKey("logType", "ZBX"));
		Assert.assertEquals("LOG_TYPE", c.makeEnvKey("logType", null));
	}
	
	@Test
	public void loadConfigTest() throws Exception
	{
		//FIXME: write out this test
	}
}
