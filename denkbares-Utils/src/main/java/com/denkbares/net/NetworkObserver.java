/*
 * Copyright (C) 2023 denkbares GmbH, Germany
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package com.denkbares.net;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that observes (polls) all available network interfaces and singals if the network configuration has
 * changed. It is useful to e.g. observe the computer if it is plugged into (or connected to) a LAN or WLAN network, to
 * check for available hosts or services afterwards.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 01.02.2023
 */
public class NetworkObserver {

	private static final Logger LOGGER = LoggerFactory.getLogger(NetworkObserver.class);
	private static final ScheduledThreadPoolExecutor service = new ScheduledThreadPoolExecutor(1);

	private CharSequence previousDump;

	private ScheduledFuture<?> polling = null;

	public NetworkObserver() {
		this.previousDump = dump();
	}

	private static void dump(StringBuilder result, NetworkInterface ni) {
		result.append(ni.getDisplayName());
		for (InterfaceAddress address : ni.getInterfaceAddresses()) {
			result.append("; ");
			result.append(address.getAddress().getHostAddress());
		}
		result.append('\n');
		ni.subInterfaces().forEach(sub -> dump(result, sub));
	}

	private static CharSequence dump() {
		var result = new StringBuilder(1024);
		try {
			for (NetworkInterface ni : NetworkInterface.networkInterfaces().toList()) {
//				if (!ni.isUp()) continue;
//				if (ni.isLoopback()) continue;
//				if (ni.isVirtual()) continue;
				dump(result, ni);
			}
		}
		catch (SocketException ignore) {
		}
		return result;
	}

	/**
	 * Checks if the network configuration has changed, compared to the preious check. If the configuration has changed,
	 * the listeners will be activated. Afterwards the method returns true. If there were no changes, the method returns
	 * false immediately.
	 */
	public boolean hasNetworkChanged() {
		var dump = dump();
		if (CharSequence.compare(dump, previousDump) == 0) return false;

		// store current network status and fire listeners
		previousDump = dump;
		return true;
	}

	private void pollOnce(Runnable listener) {
		if (hasNetworkChanged()) {
			if (listener == null) return;
			try {
				listener.run();
			}
			catch (Exception e) {
				LOGGER.warn("unexpected exception in network listener", e);
			}
		}
	}

	/**
	 * Start to poll for changes on the notwork configuration. If any change occur, the listener will be called. The
	 * polling continues, until {@link #stopPoll()} is called.
	 *
	 * @param pauseMillis the time between each poll
	 * @param listener    the listener to be called on each network change
	 * @return this observer, to chain method calls
	 * @throws IllegalStateException If the observer is already polling, and not stopped yet
	 */
	public synchronized NetworkObserver startPoll(long pauseMillis, Runnable listener) {
		if (polling != null) throw new IllegalStateException("already observing");
		polling = service.scheduleAtFixedRate(() -> pollOnce(listener), 0, pauseMillis, TimeUnit.MILLISECONDS);
		return this;
	}

	/**
	 * Stops to poll for changes on the notwork configuration. If the polling has not benn started yet, the method does
	 * nothing.
	 * <p>
	 * Note: If a network change is already detected, while calling this mehtod, the observing listener may be called a
	 * last time, even after this method has returned.
	 *
	 * @return this observer, to chain method calls
	 */
	public synchronized NetworkObserver stopPoll() {
		if (polling != null) {
			polling.cancel(false);
			polling = null;
		}
		return this;
	}

//	public static void main(String[] args) throws InterruptedException {
//		new NetworkObserver().startPoll(500, () -> System.out.println("network changed"));
//		while (true) {
//			Thread.sleep(1000);
//		}
//	}
}
