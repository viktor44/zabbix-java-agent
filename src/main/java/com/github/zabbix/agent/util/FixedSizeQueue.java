package com.github.zabbix.agent.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Victor Kadachigov
 */
public class FixedSizeQueue<E> extends ConcurrentLinkedQueue<E>
{
	private final int maxSize;
	
	public FixedSizeQueue(int maxSize)
	{
		super();

		if (maxSize < 1)
			throw new IllegalArgumentException("maxSize must be greater than 0");
		this.maxSize = maxSize;
	}

    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        if (c == this)
            // As historically specified in AbstractQueue#addAll
            throw new IllegalArgumentException();
        
        int sz = size();
        int csz = c.size();
        int exceeding = sz + csz - maxSize;
        if (exceeding > 0)
        {
        	if (exceeding > sz)
        	{
        		clear();
        		exceeding -= sz;
        		List list = new ArrayList<>();
        		for (Iterator<? extends E> it = c.iterator(); it.hasNext();)
        		{
        			Object a = it.next();
        			if (exceeding > 0)
        				exceeding--;
        			else
        				list.add(a);
        		}
        		c = list;
        	}
        	else
        	{
            	for (int i = 0; i < exceeding; i++)
            		remove();
        	}
        }
    	return super.addAll(c);
    }
    
    @Override
    public boolean offer(E e)
    {
        if (e == null)
            throw new NullPointerException();
        
        if (size() + 1 > maxSize)
       		remove();
        
    	return super.offer(e);
    }
}
