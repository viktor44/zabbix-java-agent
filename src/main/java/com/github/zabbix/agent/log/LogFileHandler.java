package com.github.zabbix.agent.log;

import java.io.IOException;
import java.util.logging.FileHandler;

public class LogFileHandler extends FileHandler
{
    public LogFileHandler(String pattern) throws IOException, SecurityException 
    {
    	super(pattern);
    }
}
