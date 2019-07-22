package com.github.zabbix.agent;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.zabbix.agent.data.CheckItem;
import com.github.zabbix.agent.data.CheckResult;
import com.github.zabbix.agent.data.ZabbixKey;

import lombok.extern.java.Log;

/**
 * @author Victor Kadachigov
 */
@Log(topic="com.github.zabbix.agent")
public class CheckerTaskTest
{
	@BeforeClass
	public static void init()
	{
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %2$s %5$s%6$s%n");
		Level level = Level.FINE;
		log.setUseParentHandlers(false);
		log.addHandler(new ConsoleHandler());
		log.setLevel(level);
		for (Handler handler : log.getHandlers())
			handler.setLevel(level);
	}
	
	@Test
	public void testJmx() throws Exception
	{
		Queue<CheckResult> resultsQueue = new ArrayBlockingQueue<>(1000);
		Set<CheckItem> checkItems = new HashSet<>();

		String keys[] = new String[] 
				{
					"jmx[\"java.lang:type=OperatingSystem\",ProcessCpuLoad]", 
					"jmx[\"java.lang:type=Runtime\",VmName]", 
					"jmx[\"java.lang:type=Threading\",DaemonThreadCount]", 
					"jmx[\"java.lang:type=Threading\",PeakThreadCount]", 
					"jmx[\"java.lang:type=Threading\",ThreadCount]", 
					"jmx[\"java.lang:type=Threading\",TotalStartedThreadCount]", 
					"jmx[\"java.lang:type=ClassLoading\",LoadedClassCount]", 
					"jmx[\"java.lang:type=ClassLoading\",TotalLoadedClassCount]", 
					"jmx[\"java.lang:type=ClassLoading\",UnloadedClassCount]", 
					"jmx[\"java.lang:type=Compilation\",Name]", 
					"jmx[\"java.lang:type=Compilation\",TotalCompilationTime]", 
					"jmx[\"java.lang:type=GarbageCollector,name=ConcurrentMarkSweep\",CollectionCount]", 
					"jmx[\"java.lang:type=GarbageCollector,name=ConcurrentMarkSweep\",CollectionTime]", 
					"jmx[\"java.lang:type=GarbageCollector,name=Copy\",CollectionCount]", 
					"jmx[\"java.lang:type=GarbageCollector,name=Copy\",CollectionTime]", 
					"jmx[\"java.lang:type=GarbageCollector,name=MarkSweepCompact\",CollectionCount]", 
					"jmx[\"java.lang:type=GarbageCollector,name=MarkSweepCompact\",CollectionTime]", 
					"jmx[\"java.lang:type=GarbageCollector,name=ParNew\",CollectionCount]", 
					"jmx[\"java.lang:type=GarbageCollector,name=ParNew\",CollectionTime]", 
					"jmx[\"java.lang:type=GarbageCollector,name=PS MarkSweep\",CollectionCount]", 
					"jmx[\"java.lang:type=GarbageCollector,name=PS MarkSweep\",CollectionTime]", 
					"jmx[\"java.lang:type=GarbageCollector,name=PS Scavenge\",CollectionCount]", 
					"jmx[\"java.lang:type=GarbageCollector,name=PS Scavenge\",CollectionTime]", 
					"jmx[\"java.lang:type=Memory\",HeapMemoryUsage.committed]", 
					"jmx[\"java.lang:type=Memory\",HeapMemoryUsage.max]", 
					"jmx[\"java.lang:type=Memory\",HeapMemoryUsage.used]", 
					"jmx[\"java.lang:type=Memory\",NonHeapMemoryUsage.committed]", 
					"jmx[\"java.lang:type=Memory\",NonHeapMemoryUsage.max]", 
					"jmx[\"java.lang:type=Memory\",NonHeapMemoryUsage.used]", 
					"jmx[\"java.lang:type=Memory\",ObjectPendingFinalizationCount]", 
					"jmx[\"java.lang:type=MemoryPool,name=CMS Old Gen\",Usage.committed]", 
					"jmx[\"java.lang:type=MemoryPool,name=CMS Old Gen\",Usage.max]", 
					"jmx[\"java.lang:type=MemoryPool,name=CMS Old Gen\",Usage.used]", 
					"jmx[\"java.lang:type=MemoryPool,name=CMS Perm Gen\",Usage.committed]", 
					"jmx[\"java.lang:type=MemoryPool,name=CMS Perm Gen\",Usage.max]", 
					"jmx[\"java.lang:type=MemoryPool,name=CMS Perm Gen\",Usage.used]", 
					"jmx[\"java.lang:type=MemoryPool,name=Code Cache\",Usage.committed]", 
					"jmx[\"java.lang:type=MemoryPool,name=Code Cache\",Usage.max]", 
					"jmx[\"java.lang:type=MemoryPool,name=Code Cache\",Usage.used]", 
					"jmx[\"java.lang:type=MemoryPool,name=Perm Gen\",Usage.committed]", 
					"jmx[\"java.lang:type=MemoryPool,name=Perm Gen\",Usage.max]", 
					"jmx[\"java.lang:type=MemoryPool,name=Perm Gen\",Usage.used]", 
					"jmx[\"java.lang:type=MemoryPool,name=PS Old Gen\",Usage.committed]", 
					"jmx[\"java.lang:type=MemoryPool,name=PS Old Gen\",Usage.max]", 
					"jmx[\"java.lang:type=MemoryPool,name=PS Old Gen\",Usage.used]", 
					"jmx[\"java.lang:type=MemoryPool,name=PS Perm Gen\",Usage.committed]", 
					"jmx[\"java.lang:type=MemoryPool,name=PS Perm Gen\",Usage.max]", 
					"jmx[\"java.lang:type=MemoryPool,name=PS Perm Gen\",Usage.used]", 
					"jmx[\"java.lang:type=MemoryPool,name=Tenured Gen\",Usage.committed]", 
					"jmx[\"java.lang:type=MemoryPool,name=Tenured Gen\",Usage.max]", 
					"jmx[\"java.lang:type=MemoryPool,name=Tenured Gen\",Usage.used]", 
					"jmx[\"java.lang:type=OperatingSystem\",MaxFileDescriptorCount]", 
					"jmx[\"java.lang:type=OperatingSystem\",OpenFileDescriptorCount]", 
					"jmx[\"java.lang:type=Runtime\",Uptime]", 
					"jmx[\"java.lang:type=Runtime\",VmVersion]"
				};

		for (String key : keys)
			checkItems.add(
					CheckItem.builder().key(new ZabbixKey(key)).build()
			);
		
		CheckerTask checkerTask = new CheckerTask(checkItems, null, resultsQueue, 0);
		checkerTask.run();

		//FIXME: do checks
	}
	
	@Test
	public void testJmxDiscovery() throws Exception
	{
		Queue<CheckResult> resultsQueue = new ArrayBlockingQueue<>(1000);
		Set<CheckItem> checkItems = new HashSet<>();

		String keys[] = new String[] 
				{
					"jmx.discovery[attributes, \"metrics:name=service*\"]", 
				};

		for (String key : keys)
			checkItems.add(
					CheckItem.builder().key(new ZabbixKey(key)).build()
			);
		
		CheckerTask checkerTask = new CheckerTask(checkItems, null, resultsQueue, 0);
		checkerTask.run();

		//FIXME: do checks
	}
	
	@Test
	public void testJmxDiscoveryAttributes() throws Exception
	{
		Queue<CheckResult> resultsQueue = new ArrayBlockingQueue<>(1000);
		Set<CheckItem> checkItems = new HashSet<>();

		ZabbixKey key = new ZabbixKey("jmx.discovery[attributes, \"java.lang:type=MemoryPool,name=PS*\"]");
		CheckerTask checkerTask = new CheckerTask(checkItems, null, resultsQueue, 0);
		
		String s = checkerTask.getStringValue(key);
		log.info("Result: " + s);
		
		//FIXME: do checks
	}
	
	@Test
	public void testJmxDiscoveryBeans() throws Exception
	{
		Queue<CheckResult> resultsQueue = new ArrayBlockingQueue<>(1000);
		Set<CheckItem> checkItems = new HashSet<>();

		ZabbixKey key = new ZabbixKey("jmx.discovery[beans, \"java.lang:type=MemoryPool,name=PS*\"]");
		CheckerTask checkerTask = new CheckerTask(checkItems, null, resultsQueue, 0);
		
		String s = checkerTask.getStringValue(key);
		log.info("Result: " + s);

		//FIXME: do checks
	}
}
