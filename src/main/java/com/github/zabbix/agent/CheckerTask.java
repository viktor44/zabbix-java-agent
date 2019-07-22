package com.github.zabbix.agent;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularDataSupport;

import org.json.JSONArray;
import org.json.JSONException;
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
	private int delay;

	public CheckerTask(Set<CheckItem> checkItems, ZabbixAgentConfig config, Queue<CheckResult> resultsQueue, int delay)
	{
		this.checkItems = checkItems;
		this.config = config;
		this.resultsQueue = resultsQueue;
		this.delay = delay;
	}
	
	@Override
	public void run()
	{
		long start = 0;
		if (log.isLoggable(Level.INFO))
		{
			start = System.currentTimeMillis();
			log.log(Level.INFO, "Start {0} checks. Period {1}s", new Object[] { checkItems.size(), delay } );
		}
		
		try
		{
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
		
		if (log.isLoggable(Level.INFO))
		{
			long end = System.currentTimeMillis();
			log.log(Level.INFO, "End checks. Period {0}s. Work time: {1}", new Object[] { delay, timeToLog(end - start) });
		}
	}
	
	private String timeToLog(long interval)
	{
        long ms = interval % 1000;
        interval /= 1000;
        long sec = interval % 60;
        interval /= 60;
        long min = interval % 60;
        long hr = interval / 60;
        StringBuilder sb = new StringBuilder();
        if (hr > 0)
        {
        	if (hr < 10) sb.append('0');
       		sb.append(hr).append(':');
        }
        if (min < 10) sb.append('0');
       	sb.append(min).append(':');
        if (sec < 10) sb.append('0');
       	sb.append(sec).append('.');
        if (ms < 10) 
        	sb.append("00");
        else if (ms < 100)
        	sb.append('0');
        sb.append(ms);
		return sb.toString();
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
				log.log(Level.FINEST, "\"{0}\" contains composite data", attributeName);

				realAttributeName = attributeName.substring(0, sep);
				fieldNames = attributeName.substring(sep + 1);
			}
			else
				realAttributeName = attributeName;

			// unescape possible dots or backslashes that were escaped by user
			realAttributeName = unescapeUserInput(realAttributeName);

			log.log(Level.FINEST, "attributeName: \"{0}\"", realAttributeName);
			log.log(Level.FINEST, "fieldNames: \"{0}\"", fieldNames);

			String result = null;
			try
			{
				result = getPrimitiveAttributeValue(getMbServer().getAttribute(objectName, realAttributeName), fieldNames);
			}
			catch (OperationsException ex)
			{
				log.log(Level.FINE, "{0}: {1}", new Object[] {ex.getClass().getSimpleName(), ex.getMessage()});
			}
			return result;
		}
		else if (key.getKeyId().equals("jmx.discovery"))
		{
			int argumentCount = key.getArgumentCount();
			if (argumentCount > 2)
				throw new ZabbixException("required key format: jmx.discovery[<discovery mode>,<object name>]");

			JSONArray counters = new JSONArray();
			ObjectName filter = (argumentCount == 2) ? new ObjectName(key.getArgument(2)) : null;

			DiscoveryMode mode = DiscoveryMode.ATTRIBUTES;
			if (0 != argumentCount)
			{
				String modeName = key.getArgument(1);

				if (modeName.equals("beans"))
					mode = DiscoveryMode.BEANS;
				else if (!modeName.equals("attributes"))
					throw new ZabbixException("invalid discovery mode: " + modeName);
			}

			for (ObjectName name : getMbServer().queryNames(filter, null))
			{
				log.log(Level.FINEST, "discovered object \"{0}\"", name);

				if (mode == DiscoveryMode.ATTRIBUTES)
					discoverAttributes(counters, name);
				else
					discoverBeans(counters, name);
			}

			JSONObject mapping = new JSONObject();
			mapping.put(Protocol.JSON_TAG_DATA, counters);
			return mapping.toString();
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

	private void discoverAttributes(JSONArray counters, ObjectName name) throws Exception
	{
		for (MBeanAttributeInfo attrInfo : getMbServer().getMBeanInfo(name).getAttributes())
		{
			log.log(Level.FINEST, "discovered attribute \"{0}\"", attrInfo.getName());

			if (!attrInfo.isReadable())
			{
				log.log(Level.FINEST, "attribute not readable, skipping");
				continue;
			}

			try
			{
				log.log(Level.FINEST, "looking for attributes of primitive types");
				String descr = (attrInfo.getName().equals(attrInfo.getDescription()) ? null : attrInfo.getDescription());
				findPrimitiveAttributes(counters, name, descr, attrInfo.getName(), getMbServer().getAttribute(name, attrInfo.getName()));
			}
			catch (Exception e)
			{
				Object[] logInfo = {name, attrInfo.getName(), getRootCauseMessage(e)};
				log.log(Level.WARNING, "attribute processing \"{0},{1}\" failed: {2}", logInfo);
				log.log(Level.FINE, "error caused by", e);
			}
		}
	}

	private void findPrimitiveAttributes(JSONArray counters, ObjectName name, String descr, String attrPath, Object attribute) throws NoSuchMethodException, JSONException
	{
		log.log(Level.FINEST, "drilling down with attribute path \"{0}\"", attrPath);

		if (isPrimitiveAttributeType(attribute))
		{
			log.log(Level.FINEST, "found attribute of a primitive type: {0}", attribute.getClass());

			JSONObject counter = new JSONObject();

			counter.put("{#JMXDESC}", null == descr ? name + "," + attrPath : descr);
			counter.put("{#JMXOBJ}", name);
			counter.put("{#JMXATTR}", attrPath);
			counter.put("{#JMXTYPE}", attribute.getClass().getName());
			counter.put("{#JMXVALUE}", attribute.toString());

			counters.put(counter);
		}
		else if (attribute instanceof CompositeData)
		{
			log.log(Level.FINEST, "found attribute of a composite type: {0}", attribute.getClass());

			CompositeData comp = (CompositeData)attribute;

			for (String key : comp.getCompositeType().keySet())
				findPrimitiveAttributes(counters, name, descr, attrPath + "." + key, comp.get(key));
		}
		else if (attribute instanceof TabularDataSupport || attribute.getClass().isArray())
		{
			log.log(Level.FINEST, "found attribute of a known, unsupported type: {0}", attribute.getClass());
		}
		else
			log.log(Level.FINEST, "found attribute of an unknown, unsupported type: {0}", attribute.getClass());
	}

	private void discoverBeans(JSONArray counters, ObjectName name)
	{
		try
		{
			HashSet<String> properties = new HashSet<>();
			JSONObject counter = new JSONObject();

			// Default properties are added.
			counter.put("{#JMXOBJ}", name);
			counter.put("{#JMXDOMAIN}", name.getDomain());
			properties.add("OBJ");
			properties.add("DOMAIN");

			for (Map.Entry<String, String> property : name.getKeyPropertyList().entrySet())
			{
				String key = property.getKey().toUpperCase();

				// Property key should only contain valid characters and should not be already added to attribute list.
				if (key.matches("^[A-Z0-9_\\.]+$") && !properties.contains(key))
				{
					counter.put("{#JMX" + key + "}" , property.getValue());
					properties.add(key);
				}
				else
					log.log(Level.FINE, "bean \"{0}\" property \"{1}\" was ignored", new Object[] { name, property.getKey() });
			}

			counters.put(counter);
		}
		catch (Exception e)
		{
			log.log(Level.WARNING, "bean processing \"{0}\" failed: {1}", new Object[] { name, getRootCauseMessage(e) });
			log.log(Level.FINE, "error caused by", e);
		}
	}

	Throwable getRootCause(Throwable e)
	{
		Throwable cause = null;
		Throwable result = e;

		while (((cause = result.getCause()) != null) && result != cause)
			result = cause;

		return result;
	}
	
	String getRootCauseMessage(Throwable e)
	{
		if (e != null)
			return getRootCause(e).getMessage();

		return null;
	}
	
	private MBeanServer getMbServer()
	{
		if (mbServer == null)
			mbServer = ManagementFactory.getPlatformMBeanServer();
		return mbServer;
	}

}
