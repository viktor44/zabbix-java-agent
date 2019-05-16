# Zabbix Agent for Java

## Overview

Zabbix agent for Java applications. It supports export JMX metrics to Zabbix server in active mode.

## Usage

This application starts inside monitored JVM as Java agent.

To run use this command:

```
java -javaagent:/path/to/JavaAgent.jar=<Configuration file> 
```

Configuration file is a standard Zabbix agent configuration.

Supported configuration parameters:

| Parameter | Mandatory | Range | Default | Description |
|---|---|---|---|---|
| BufferSend | no | 1-3600 | 5 | Do not keep data longer than N seconds in buffer. |
| BufferSize | no | 2-65535 | 100 | Maximum number of values in a memory buffer. The agent will send all collected data to Zabbix server or proxy if the buffer is full. |
| DebugLevel | no | 0-5 | 3 | Specifies debug level:
0. - basic information about starting and stopping of Zabbix processes
1. - critical information
2. - error information
3. - warnings
4. - for debugging (produces lots of information)
5. - extended debugging (produces even more information) |
|   |   |   |   |   |
|   |   |   |   |   |
|   |   |   |   |   |

