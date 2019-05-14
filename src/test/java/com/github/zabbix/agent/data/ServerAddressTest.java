package com.github.zabbix.agent.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.zabbix.agent.ZabbixAgentConfig;
import com.github.zabbix.agent.data.ServerAddress;

/**
 * @author Victor Kadachigov
 */
public class ServerAddressTest
{
	@Test
	public void test()
	{
		List<ServerAddress> list = ServerAddress.parse("127.0.0.1");
		assertThat(list)
				.hasSize(1)
				.containsExactly(new ServerAddress("127.0.0.1", ZabbixAgentConfig.DEFAULT_SERVER_PORT));

		list = ServerAddress.parse("127.0.0.1:10052");
		assertThat(list)
				.hasSize(1)
				.containsExactly(new ServerAddress("127.0.0.1", 10052));

		list = ServerAddress.parse("127.0.0.1:20051,zabbix.domain,[::1]:30051,::1,[12fc::1]");
		assertThat(list)
				.hasSize(5)
				.containsExactly(
						new ServerAddress("127.0.0.1", 20051),
						new ServerAddress("zabbix.domain", ZabbixAgentConfig.DEFAULT_SERVER_PORT),
						new ServerAddress("::1", 30051),
						new ServerAddress("::1", ZabbixAgentConfig.DEFAULT_SERVER_PORT),
						new ServerAddress("12fc::1", ZabbixAgentConfig.DEFAULT_SERVER_PORT)
				);

		list = ServerAddress.parse(null);
		assertThat(list)
			.isNotNull()
			.hasSize(0);

	}
}
