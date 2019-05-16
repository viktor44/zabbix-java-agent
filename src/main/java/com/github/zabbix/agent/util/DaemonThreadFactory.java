package com.github.zabbix.agent.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Victor Kadachigov
 */
public class DaemonThreadFactory implements ThreadFactory 
{
    private String namePrefix;
    private AtomicInteger counter;
    private ThreadFactory dtf = Executors.defaultThreadFactory();

    public DaemonThreadFactory(String namePrefix) 
    {
        this.namePrefix = namePrefix;
        this.counter = new AtomicInteger(0);
    }

    @Override
    public Thread newThread(Runnable r) 
    {
        Thread t = dtf.newThread(r);
        t.setDaemon(true);
        t.setName(namePrefix + "-" + counter.incrementAndGet());
        return t;
    }
}
