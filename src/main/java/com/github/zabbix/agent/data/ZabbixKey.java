package com.github.zabbix.agent.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Victor Kadachigov
 */
@EqualsAndHashCode(doNotUseGetters=true, onlyExplicitlyIncluded=true)
@ToString(doNotUseGetters=true, onlyExplicitlyIncluded=true)
public class ZabbixKey
{
	@Getter
	@EqualsAndHashCode.Include
	@ToString.Include
	private final String key;
	@Getter
	private final String keyId;
	private final List<String> args;

	public ZabbixKey(String key)
	{
		if (key == null)
			throw new IllegalArgumentException("Key must not be null");

		int bracket = key.indexOf('[');
		if (bracket >= 0)
		{
			if (key.charAt(key.length() - 1) != ']')
				throw new IllegalArgumentException("no terminating ']' in key: '" + key + "'");

			keyId = key.substring(0, bracket);
			args = parseArguments(key.substring(bracket + 1, key.length() - 1));
		}
		else
		{
			keyId = key;
			args = Collections.emptyList();
		}

		if (keyId.length() == 0)
			throw new IllegalArgumentException("key ID is empty in key: '" + key + "'");

		for (int i = 0; i < keyId.length(); i++)
			if (!isValidKeyIdChar(keyId.charAt(i)))
				throw new IllegalArgumentException("Bad key ID char '" + keyId.charAt(i) + "' in key: '" + key + "'");

		this.key = key;
	}

	public String getArgument(int index)
	{
		if (index < 1 || index > args.size())
			throw new IndexOutOfBoundsException("Bad argument index for key '" + key + "': " + index);
		else
			return args.get(index - 1);
	}

	public int getArgumentCount()
	{
		return args.size();
	}

	private List<String> parseArguments(String keyArgs)
	{
		List<String> args = new ArrayList<>();

		while (true)
		{
			if (keyArgs.length() == 0)
			{
				args.add("");
				break;
			}
			else if (' ' == keyArgs.charAt(0))
			{
				keyArgs = keyArgs.substring(1);
			}
			else if (keyArgs.charAt(0) == '"')
			{
				int index = 1;

				while (index < keyArgs.length())
				{
					if (keyArgs.charAt(index) == '"' && keyArgs.charAt(index - 1) != '\\')
						break;
					else
						index++;
				}

				if (index == keyArgs.length())
					throw new IllegalArgumentException("quoted argument not terminated: '" + key + "'");

				args.add(keyArgs.substring(1, index).replace("\\\"", "\""));

				for (index++; index < keyArgs.length() && keyArgs.charAt(index) == ' '; index++);

				if (index == keyArgs.length())
					break;

				if (keyArgs.charAt(index) != ',')
					throw new IllegalArgumentException("quoted argument not followed by comma: '" + key + "'");

				keyArgs = keyArgs.substring(index + 1);
			}
			else
			{
				int index = 0;

				while (index < keyArgs.length() && keyArgs.charAt(index) != ',')
					index++;

				args.add(keyArgs.substring(0, index));

				if (index == keyArgs.length())
					break;

				keyArgs = keyArgs.substring(index + 1);
			}
		}

		return args;
	}

	private boolean isValidKeyIdChar(char ch)
	{
		return ('a' <= ch && ch <= 'z') 
				|| ('A' <= ch && ch <= 'Z') 
				|| ('0' <= ch && ch <= '9') 
				|| ch == '.' || ch == '_' || ch == '-';
	}
}
