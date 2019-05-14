package com.github.zabbix.agent.data;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Victor Kadachigov
 */
@Builder
@EqualsAndHashCode(doNotUseGetters=true, onlyExplicitlyIncluded=true)
@ToString(doNotUseGetters=true)
public class CheckResult 
{
	@Getter
	@EqualsAndHashCode.Include
	private ZabbixKey key;
	@Getter
	private String value;
	@Getter
	@EqualsAndHashCode.Include
	private long clock;
}

