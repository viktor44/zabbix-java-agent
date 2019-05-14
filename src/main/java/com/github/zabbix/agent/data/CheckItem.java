package com.github.zabbix.agent.data;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Victor Kadachigov
 */
@ToString
@Builder
@EqualsAndHashCode(doNotUseGetters=true, onlyExplicitlyIncluded=true)
public class CheckItem 
{
    /** Item key */
	@Getter
	@EqualsAndHashCode.Include
	private ZabbixKey key;
    /** Item iterval (seconds) */
	@Getter
	private int delay;
    /** Last position (if applicable) */
	@Getter
	private int lastlogsize;
    /** Last item modification time */
	@Getter
	private int mtime;
}
