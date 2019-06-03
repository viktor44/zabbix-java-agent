package com.github.zabbix.agent;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.zabbix.agent.data.CheckItem;
import com.github.zabbix.agent.data.CheckResult;
import com.github.zabbix.agent.data.ServerAddress;
import com.github.zabbix.agent.data.ZabbixKey;

import lombok.extern.java.Log;

/**
 * @author Victor Kadachigov
 */
@Log(topic="com.github.zabbix.agent")
public class Protocol
{
	private static final byte[] PROTOCOL_HEADER = {'Z', 'B', 'X', 'D', '\1'};
	private static final Charset DEFAULT_ENCODING = Charset.forName("UTF-8");
	
	public static final String JSON_TAG_REQUEST = "request";
	public static final String JSON_TAG_RESPONSE = "response";
	public static final String JSON_TAG_INFO = "info";
	public static final String JSON_TAG_DATA = "data";
	public static final String JSON_TAG_KEY = "key";
	public static final String JSON_TAG_HOST = "host";
	public static final String JSON_TAG_CLOCK = "clock";
	public static final String JSON_TAG_VALUE = "value";

	private static final String JSON_RESPONSE_FAILED = "failed";
	private static final String JSON_RESPONSE_SUCCESS = "success";
	
	private final ZabbixAgentConfig config;
	private final ServerAddress serverAddress;
	
	public Protocol(ServerAddress serverAddress, ZabbixAgentConfig config)
	{
		this.config = config;
		this.serverAddress = serverAddress;
	}

	public Set<CheckItem> refreshActiveChecks() throws ZabbixException
	{
		Socket socket = null;
        try 
        {
			socket = openSocket();
			
			JSONObject requestJson = new JSONObject();
			requestJson.put(JSON_TAG_REQUEST, "active checks");
			requestJson.put(JSON_TAG_HOST, config.getHostname());
			requestJson.putOpt("host_metadata", config.getHostMetadata());
			
			String msg = requestJson.toString();
			
			log.log(Level.FINE, "sending {0}", msg);
			
			byte message[] = toZbxMessage(msg);
			socket.getOutputStream().write(message);
			
			Set<CheckItem> result = new HashSet<>();
    		
			JSONObject responseJson = checkResponse(read(socket.getInputStream()), "active checks");
			JSONArray dataJson = responseJson.getJSONArray(JSON_TAG_DATA);
			for (int i = 0; i < dataJson.length(); i++)
			{
				JSONObject itemJson = dataJson.getJSONObject(i);
				CheckItem item = CheckItem.builder()
											.key(new ZabbixKey(itemJson.getString(JSON_TAG_KEY)))
											.delay(itemJson.getInt("delay"))
											.lastlogsize(itemJson.optInt("lastlogsize"))
											.mtime(itemJson.optInt("mtime"))
											.build();
				result.add(item);
			}
			
			return result;
		} 
        catch (JSONException | IOException ex) 
        {
        	throw new ZabbixException("Error while refreshing checks: " + ex.getMessage());
		} 
        finally 
        {
			closeSocket(socket);
		}
	}
	
	private JSONObject checkResponse(String responseString, String requestTitle) throws ZabbixException, JSONException
	{
		JSONObject result = new JSONObject(responseString);
		String s = result.getString(JSON_TAG_RESPONSE);
		if (!JSON_RESPONSE_SUCCESS.equals(s))
			throw new ZabbixException(requestTitle + " failed: " + result.optString(JSON_TAG_INFO));
		return result;
	}
	
	private Socket openSocket() throws ZabbixException
	{
		try
		{
			log.log(Level.FINE, "Connecting to {0}", serverAddress);

			Socket result = new Socket();
			result.connect(serverAddress.getSocketAddress(), config.getTimeout() * 1000);
//			result.setSoTimeout(config.getTimeout() * 1000);
			
			return result;
		}
        catch (IOException ex) 
        {
        	throw new ZabbixException("Failed to connect to " + serverAddress + ". Will try to connect later. " + ex.getMessage());
		} 
	}
	
	private String read(InputStream inputStream) throws IOException
	{
		DataInputStream dis = new DataInputStream(inputStream);

		byte[] data;

		log.fine("reading Zabbix protocol header");
		
		data = new byte[5];
		dis.readFully(data);

		if (!Arrays.equals(data, PROTOCOL_HEADER))
			throw new RuntimeException(
							new Formatter().format(
									"bad protocol header: %02X %02X %02X %02X %02X", data[0], data[1], data[2], data[3], data[4]
							).toString()
					);

		log.fine("reading 8 bytes of data length");
		
		data = new byte[8];
		dis.readFully(data);

		ByteBuffer buffer = ByteBuffer.wrap(data);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		long length = buffer.getLong();

		if (!(0 <= length && length <= Integer.MAX_VALUE))
			throw new RuntimeException("bad data length: " + length);

		log.log(Level.FINE, "reading {0} bytes from server", length);
		
		data = new byte[(int)length];
		dis.readFully(data);
		
//		saveToFile(data, null);

		String responseString = new String(data, DEFAULT_ENCODING);
		
		log.log(Level.FINE, "got {0}", responseString);
		
		return responseString;
	}
	
	private void saveToFile(byte[] data, String fileName)
	{
		OutputStream os = null;
		try
		{
			File file = fileName != null ? new File(fileName) : File.createTempFile("zbx_msg", ".json");
			os = new FileOutputStream(file);
			os.write(data);
			log.log(Level.INFO, "data saved to {0}", file.getAbsolutePath());
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			try
			{
				if (os != null)
					os.close();
			}
			catch (Exception ex)
			{
			}
		}
	}
	
	
	private void closeSocket(Socket socket)
	{
		if (socket != null) 
		{
			try 
			{
				socket.close();
			} 
			catch (IOException ex) 
			{
				log.log(Level.SEVERE, "Error closing socket: {0}", ex.getMessage());
			}
		}
	}

	private byte[] toZbxMessage(String message)
	{
		byte data[] = message.getBytes(DEFAULT_ENCODING);
		byte header[] = new byte[] 
						{
							'Z', 'B', 'X', 'D', '\1',
							(byte)(data.length & 0xFF),
							(byte)((data.length >> 8) & 0xFF),
							(byte)((data.length >> 16) & 0xFF),
							(byte)((data.length >> 24) & 0xFF),
							'\0', '\0', '\0', '\0'
						};
		byte[] result = new byte[header.length + data.length];
		System.arraycopy(header, 0, result, 0, header.length);
		System.arraycopy(data, 0, result, header.length, data.length);
		return result;
	}

	public void sendCheckResults(List<CheckResult> checkResults) throws ZabbixException
	{
		Socket socket = null;
        try 
        {
        	socket = openSocket();
        	
			JSONObject requestJson = new JSONObject();
			requestJson.put(JSON_TAG_REQUEST, "agent data");
			requestJson.put(JSON_TAG_CLOCK, toZabbixClock(System.currentTimeMillis()));
			JSONArray dataJson = new JSONArray();
			for (CheckResult cr : checkResults)
			{
				JSONObject crJson = new JSONObject();
				crJson.put(JSON_TAG_KEY, cr.getKey().getKey());
				crJson.put(JSON_TAG_HOST, config.getHostname());
				crJson.put(JSON_TAG_VALUE, cr.getValue());
				crJson.put(JSON_TAG_CLOCK, toZabbixClock(cr.getClock()));
				dataJson.put(crJson);
			}
			requestJson.put(JSON_TAG_DATA, dataJson);
			
			String msg = requestJson.toString();
			
			log.log(Level.FINE, "sending {0}", msg);
			
			byte message[] = toZbxMessage(msg);
			socket.getOutputStream().write(message);
			
			JSONObject responseJson = checkResponse(read(socket.getInputStream()), "agent data");
		} 
        catch (JSONException | IOException ex) 
        {
        	throw new ZabbixException("Failed to connect to " + serverAddress + ". Will try to connect later. " + ex.getMessage());
		} 
        finally 
        {
			closeSocket(socket);
		}
	}
	
	private long toZabbixClock(long timestamp)
	{
		return timestamp / 1000;
	}
}
