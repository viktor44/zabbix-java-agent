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
| DebugLevel | no | 0-5 | 3 | Specifies debug level:<br>0 - basic information about starting and stopping of Zabbix processes<br>1 - critical information<br>2 - error information<br>3 - warnings<br>4 - for debugging (produces lots of information)<br>5 - extended debugging (produces even more information) |
| HostMetadata | no | 0-255 characters |   | Optional parameter that defines host metadata. Host metadata is used only at host auto-registration process (active agent). If not defined, the value will be acquired from HostMetadataItem. An agent will issue an error and not start if the specified value is over the limit or a non-UTF-8 string. |
| Hostname | no |   |   | Unique, case sensitive hostname. Required for active checks and must match hostname as configured on the server. <br>Allowed characters: alphanumeric, '.', ' ', '_' and '-'. <br>Maximum length: 64 |
| LogFile | yes, if LogType is set to _file_, otherwise no |   |   | Name of log file. |
| LogType | no |   | file | Log output type:<br>_file_ - write log to file specified by LogFile parameter,<br>_console_ - write log to standard output,<br>_all_ - _file_ + _console_ |
| RefreshActiveChecks | no | 60-3600 | 120 | How often list of active checks is refreshed, in seconds. Note that after failing to refresh active checks the next refresh will be attempted after 60 seconds. |
| ServerActive | no |   |   | IP:port (or hostname:port) of Zabbix server or Zabbix proxy for active checks. Multiple comma-delimited addresses can be provided to use several independent Zabbix servers in parallel. Spaces are allowed.<br>If port is not specified, default port is used.<br>IPv6 addresses must be enclosed in square brackets if port for that host is specified.<br>If port is not specified, square brackets for IPv6 addresses are optional.<br>If this parameter is not specified, active checks are disabled. |
| Timeout | no | 1-30 | 3 | Spend no more than Timeout seconds on processing |
|   |   |   |   |   |



