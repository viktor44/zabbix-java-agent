package com.github.zabbix.agent;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.json.JSONArray;
import org.json.JSONObject;

import com.github.zabbix.agent.data.CheckItem;
import com.github.zabbix.agent.data.CheckResult;
import com.github.zabbix.agent.data.ZabbixKey;

import lombok.extern.java.Log;

/**
 * @author Victor Kadachigov
 */
@Log(topic="com.github.zabbix.agent")
public class CheckerTask implements Runnable
{
	private enum DiscoveryMode
	{ 
		ATTRIBUTES,
		BEANS;
	}
	
	private final ZabbixAgentConfig config;
	private final Queue<CheckResult> resultsQueue;
	
	private Set<CheckItem> checkItems;
	private MBeanServer mbServer;

	public CheckerTask(Set<CheckItem> checkItems, ZabbixAgentConfig config, Queue<CheckResult> resultsQueue)
	{
		this.checkItems = checkItems;
		this.config = config;
		this.resultsQueue = resultsQueue;
	}

	@Override
	public void run()
	{
		log.info("Start checks");
		
		try
		{
			mbServer = ManagementFactory.getPlatformMBeanServer();
			
			Iterator<CheckItem> iterator = checkItems.iterator();
			while (iterator.hasNext())
			{
				CheckItem checkItem = iterator.next();
				String value = getStringValue(checkItem.getKey());
				if (value != null)
				{
					CheckResult checkResult = CheckResult.builder()
													.key(checkItem.getKey())
													.value(getStringValue(checkItem.getKey()))
													.clock(System.currentTimeMillis())
													.build();
					resultsQueue.offer(checkResult);
				}
				else
				{
					// something wrong. remove until next refresh
					log.log(Level.FINE, "Remove \"{0}\" from checks", checkItem.getKey().getKey());
					iterator.remove();
				}
			}
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, ex.getMessage(), ex);
		}
		
		log.info("End checks");
	}
	
	protected String getStringValue(ZabbixKey key) throws Exception
	{
		if (key.getKeyId().equals("jmx"))
		{
			if (key.getArgumentCount() != 2)
				throw new ZabbixException("required key format: jmx[<object name>,<attribute name>]");

			ObjectName objectName = new ObjectName(key.getArgument(1));
			String attributeName = key.getArgument(2);
			String realAttributeName;
			String fieldNames = "";

			// Attribute name and composite data field names are separated by dots. On the other hand the
			// name may contain a dot too. In this case user needs to escape it with a backslash. Also the
			// backslash symbols in the name must be escaped. So a real separator is unescaped dot and
			// separatorIndex() is used to locate it.

			int sep = separatorIndex(attributeName);

			if (sep >= 0)
			{
				log.log(Level.FINE, "\"{0}\" contains composite data", attributeName);

				realAttributeName = attributeName.substring(0, sep);
				fieldNames = attributeName.substring(sep + 1);
			}
			else
				realAttributeName = attributeName;

			// unescape possible dots or backslashes that were escaped by user
			realAttributeName = unescapeUserInput(realAttributeName);

			log.log(Level.FINE, "attributeName: \"{0}\"", realAttributeName);
			log.log(Level.FINE, "fieldNames: \"{0}\"", fieldNames);

			String result = null;
			try
			{
				result = getPrimitiveAttributeValue(mbServer.getAttribute(objectName, realAttributeName), fieldNames);
			}
			catch (OperationsException ex)
			{
				log.log(Level.FINE, "{0}: {1}", new Object[] {ex.getClass().getSimpleName(), ex.getMessage()});
			}
			return result;
		}
		else if (key.getKeyId().equals("jmx.discovery"))
		{
			log.log(Level.FINE, "Not implemented");
			
//			int argumentCount = key.getArgumentCount();
//			if (argumentCount > 2)
//				throw new ZabbixException("required key format: jmx.discovery[<discovery mode>,<object name>]");
//
//			JSONArray counters = new JSONArray();
//			ObjectName filter = (argumentCount == 2) ? new ObjectName(key.getArgument(2)) : null;
//
//			DiscoveryMode mode = DiscoveryMode.ATTRIBUTES;
//			if (0 != argumentCount)
//			{
//				String modeName = key.getArgument(1);
//
//				if (modeName.equals("beans"))
//					mode = DiscoveryMode.BEANS;
//				else if (!modeName.equals("attributes"))
//					throw new ZabbixException("invalid discovery mode: " + modeName);
//			}
//
//			for (ObjectName name : mbServer.queryNames(filter, null))
//			{
//				if (log.isLoggable(Level.FINE))
//					log.fine("discovered object '" + name + "'");
//
//				if (mode == DiscoveryMode.ATTRIBUTES)
//					discoverAttributes(counters, name);
//				else
//					discoverBeans(counters, name);
//			}
//
//			JSONObject mapping = new JSONObject();
//			mapping.put(ItemChecker.JSON_TAG_DATA, counters);
//			return mapping.toString();
		}
		else
			log.log(Level.FINE, "Key ID \"{0}\" is not supported", key.getKeyId());
		
		return null;
	}

	private String getPrimitiveAttributeValue(Object dataObject, String fieldNames) throws Exception
	{
		if (dataObject == null)
			throw new ZabbixException("data object is null");

		if (fieldNames.equals(""))
		{
			if (isPrimitiveAttributeType(dataObject))
				return dataObject.toString();
			else
				throw new ZabbixException("Data object type cannot be converted to string.");
		}

		if (dataObject instanceof CompositeData)
		{
			CompositeData comp = (CompositeData)dataObject;

			String dataObjectName;
			String newFieldNames = "";

			int sep = separatorIndex(fieldNames);
			if (sep >= 0)
			{
				dataObjectName = fieldNames.substring(0, sep);
				newFieldNames = fieldNames.substring(sep + 1);
			}
			else
				dataObjectName = fieldNames;

			// unescape possible dots or backslashes that were escaped by user
			dataObjectName = unescapeUserInput(dataObjectName);

			return getPrimitiveAttributeValue(comp.get(dataObjectName), newFieldNames);
		}
		else
			throw new ZabbixException("Unsupported data object type along the path: " + dataObject.getClass());
	}

	private boolean isPrimitiveAttributeType(Object obj) throws NoSuchMethodException
	{
		Class<?>[] primitiveClasses = 
								{
										Boolean.class, Character.class, Byte.class, 
										Short.class, Integer.class, Long.class,
										Float.class, Double.class, String.class, 
										BigDecimal.class, BigInteger.class, Date.class, 
										ObjectName.class, AtomicBoolean.class,
										AtomicInteger.class, AtomicLong.class
								};
		
		boolean isPrimitive = false;
		for (Class<?> c : primitiveClasses)
		{
			isPrimitive = c.equals(obj.getClass());
			if (isPrimitive) break;
		}

		// check if the type is either primitive or overrides toString()
		return isPrimitive 
				|| 	(	
						!(obj instanceof CompositeData) 
						&& !(obj instanceof TabularDataSupport) 
						&& obj.getClass().getMethod("toString").getDeclaringClass() != Object.class
					);
	}
	
	private int separatorIndex(String input)
	{
		for (int i = 0; i < input.length(); i++)
		{
			if (input.charAt(i) == '\\')
			{
				if (i + 1 < input.length() && (input.charAt(i + 1) == '\\' || input.charAt(i + 1) == '.'))
					i++;
			}
			else if (input.charAt(i) == '.')
				return i;
		}

		return -1;
	}

	private String unescapeUserInput(String input)
	{
		StringBuilder builder = new StringBuilder(input.length());

		for (int i = 0; i < input.length(); i++)
		{
			if (input.charAt(i) == '\\' && i + 1 < input.length() 
					&& (input.charAt(i + 1) == '\\' || input.charAt(i + 1) == '.'))
			{
				i++;
			}

			builder.append(input.charAt(i));
		}

		return builder.toString();
	}

	public synchronized void updateCheckItems(Set<CheckItem> checkItems)
	{
		this.checkItems = checkItems;
	}

}
