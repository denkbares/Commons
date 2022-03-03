/*
 * Copyright (C) 2019 denkbares GmbH, Germany
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
package com.denkbares.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A very simple EventManager. Events are represented by Classes.
 *
 * @author Jochen Reutelshöfer
 */
public final class EventManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventManager.class);

	private static EventManager instance;

	/**
	 * There are two types of listener registrations, PERSISTENT and WEAK. PERSISTENT is as you expect, you have to
	 * {@link #unregister(EventListener)} manually to clean up, once the listener is no longer needed. With type WEAK,
	 * the listener will be cleaned up automatically by the garbage collector, once there are no other references to the
	 * listener besides the once in the {@link EventManager}.
	 */
	public enum RegistrationType {
		/**
		 * Registration will be weak, the listener will be cleaned up automatically by the garbage collector, once
		 * there are no other references to the listener besides the once in the {@link EventManager}. Use this type, if
		 * you do not have full control over the life cycle of the listener.
		 */
		WEAK,
		/**
		 * Default registration type. The listener will not be cleaned up (unregistered) from the {@link EventManager}
		 * unless {@link #unregister(EventListener)} is called.
		 */
		PERSISTENT
	}

	public static EventManager getInstance() {
		if (instance == null) {
			instance = new EventManager();
		}
		return instance;
	}

	private final Map<Class<? extends Event>, WeakHashMap<EventListener, Object>> listeners = new HashMap<>();

	/**
	 * This set is only used to ensure the listener is not removed from the weak hash map, in case it was registered as
	 * a persistent listener.
	 */
	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private final Set<EventListener> persistentListeners = new HashSet<>();

	/**
	 * Creates the listener map by fetching all EventListener extensions from
	 * the PluginManager
	 */
	private EventManager() {
	}

	/**
	 * Registers the given listener in this EventManager. Make sure to unregister the listener with the method {@link
	 * #unregister(EventListener)}, once it is no longer used.
	 *
	 * @param listener the listener to register
	 */
	public synchronized void registerListener(EventListener listener) {
		registerListener(listener, RegistrationType.PERSISTENT);
	}

	/**
	 * Registers the given listener in this EventManager. Make sure to unregister the listener with the method {@link
	 * #unregister(EventListener)}, once it is no longer used.<br>
	 * If parameter <tt>registerWeak</tt> is set to false, the listener will be cleaned up automatically by the JVM
	 * garbage collector once there is no other reference to the {@link EventListener} besides this manager.
	 *
	 * @param listener         the listener to register
	 * @param registrationType determines the type of registrations, see {@link RegistrationType}
	 */
	public synchronized void registerListener(EventListener listener, RegistrationType registrationType) {
		// Get the classes of the events
		Collection<Class<? extends Event>> eventClasses = listener.getEvents();

		for (Class<? extends Event> eventClass : eventClasses) {
			// Register the listener for the event's class
			listeners.computeIfAbsent(eventClass, k -> new WeakHashMap<>()).put(listener, null);
			if (registrationType == RegistrationType.PERSISTENT) {
				persistentListeners.add(listener);
			}
		}
	}

	/**
	 * Unregisters the given listener from the EventManager.
	 *
	 * @param listener the listener to unregister
	 */
	public synchronized void unregister(EventListener listener) {
		// Get the classes of the events
		Collection<Class<? extends Event>> eventClasses = listener.getEvents();

		for (Class<? extends Event> eventClass : eventClasses) {
			// unregister the listener for the event's class
			listeners.get(eventClass).remove(listener);
		}
		persistentListeners.remove(listener);
	}

	/**
	 * Fires events; the calls are distributed in the system where the
	 * corresponding events should be fired (also plugin may fire events)
	 *
	 * @param event the fired event
	 */
	public void fireEvent(Event event) {

		ArrayList<EventListener> allListeners = new ArrayList<>();
		synchronized (this) {
			Class<?> eventClass = event.getClass();
			while (Event.class.isAssignableFrom(eventClass)) {
				@SuppressWarnings("SuspiciousMethodCalls")
				WeakHashMap<EventListener, Object> listeners = this.listeners.get(eventClass);
				if (listeners != null) {
					allListeners.addAll(listeners.keySet());
				}
				eventClass = eventClass.getSuperclass();
			}
		}
		for (EventListener eventListener : allListeners) {
			try {
				eventListener.notify(event);
			}
			catch (Exception e) {
				LOGGER.error("Catched exception in EventListener", e);
			}
		}

	}
}
