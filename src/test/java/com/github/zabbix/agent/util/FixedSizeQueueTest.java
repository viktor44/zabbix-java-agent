package com.github.zabbix.agent.util;

import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * @author Victor Kadachigov
 */
public class FixedSizeQueueTest
{
	@Test(expected=IllegalArgumentException.class)
	public void testZero() throws Exception
	{
		new FixedSizeQueue<>(0);
	}

	@Test
	public void testOffer() throws Exception
	{
		FixedSizeQueue<Integer> queue = new FixedSizeQueue<>(5);
		for (int i = 0; i < 10; i++)
			queue.offer(i + 1);
		
		Assertions.assertThat(queue)
			.containsExactly(6, 7, 8, 9, 10);
	}
	
	@Test
	public void testOfferExt() throws Exception
	{
		FixedSizeQueue<Integer> queue = new FixedSizeQueue<>(1);
		queue.offer(1);
		queue.offer(2);
		
		Assertions.assertThat(queue)
			.containsExactly(2);
	}

	
	@Test
	public void testAddAll() throws Exception
	{
		FixedSizeQueue<Integer> queue = new FixedSizeQueue<>(5);
		queue.addAll(Arrays.asList(1, 2, 3, 4, 5));
		
		Assertions.assertThat(queue)
			.containsExactly(1, 2, 3, 4, 5);
	}

	@Test
	public void testAddAllExt() throws Exception
	{
		FixedSizeQueue<Integer> queue = new FixedSizeQueue<>(5);
		queue.addAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
		
		Assertions.assertThat(queue)
			.containsExactly(6, 7, 8, 9, 10);
	}
}
