/*
Copyright 2008,2009 Complex Automata Simulation Technique (COAST) consortium

GNU Lesser General Public License

This file is part of MUSCLE (Multiscale Coupling Library and Environment).

MUSCLE is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MUSCLE is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with MUSCLE.  If not, see <http://www.gnu.org/licenses/>.
 */
package muscle.core;

import java.util.logging.Level;
import java.util.logging.Logger;
import muscle.core.ident.PortalID;
import java.util.Queue;
import muscle.core.conduit.communication.Transmitter;
import muscle.core.conduit.filter.QueueConsumer;
import muscle.core.kernel.InstanceController;
import muscle.core.messaging.Message;
import muscle.core.messaging.signal.DetachConduitSignal;

/**
this is the (remote) head of a conduit,
an entrance sends data to the conduit exit through a transmitter
@author Jan Hegewald
 */
public class ConduitEntranceController<T> extends Portal<T> implements QueueConsumer<Message<T>> {// generic T will be the underlying unwrapped data, e.g. double[]
	private ConduitEntrance<T> conduitEntrance;
	private boolean shouldPause;
	private Transmitter<T, ?,?,?> transmitter;
	private Queue<Message<T>> queue;
	private final static Logger logger = Logger.getLogger(ConduitEntranceController.class.getName());
	
	public ConduitEntranceController(PortalID newPortalID, InstanceController newOwnerAgent, int newRate, DataTemplate newDataTemplate, EntranceDependency... newDependencies) {
		super(newPortalID, newOwnerAgent, newRate, newDataTemplate);
		this.shouldPause = false;
		this.transmitter = null;
		this.queue = null;
		this.conduitEntrance = null;
	}

	public synchronized void setTransmitter(Transmitter<T,?,?,?> trans) {
		logger.log(Level.FINE, "ConduitEntrance <{0}> is now attached.", portalID);
		this.transmitter = trans;
		this.notifyAll();
	}
	
	public void setEntrance(ConduitEntrance<T> entrance) {		
		this.conduitEntrance = entrance;
	}
	
	public ConduitEntrance<T> getEntrance() {
		return this.conduitEntrance;
	}

	public void setIncomingQueue(Queue<Message<T>> queue) {
		this.queue = queue;
	}

	public void apply() {
		this.trigger();
	}
	
	protected void execute() throws InterruptedException {
		while (!queue.isEmpty()) {
			this.send(queue.remove());
		}
	}
	
	@Override
	protected void handleInterruption(InterruptedException ex) {
		logger.log(Level.SEVERE, "ConduitEntranceController {0} interrupted: {1}", new Object[]{portalID, ex});
	}
	
	/** Waits for a resume call if the thread was paused. Returns true if the thread is no longer paused and false if the thread should stop. */
	private synchronized Transmitter<T, ?,?,?> waitForTransmitter() throws InterruptedException {
		while (!isDone && (shouldPause || transmitter == null)) {
			if (logger.isLoggable(Level.FINE)) {
				String msg = "ConduitEntrance <" + portalID + "> is waiting for connection to transmit ";
				if (queue != null) {
					msg += queue.size() + " messages ";
				}
				msg += "over.";
				logger.fine(msg);
			}
			this.wait(WAIT_FOR_ATTACHMENT_MILLIS);
		}
		return transmitter;
	}

	public synchronized void pause() {
		shouldPause = true;
		this.notifyAll();
	}

	public synchronized void unpause() {
		shouldPause = false;
		this.notifyAll();
	}
	
	@Override
	public synchronized void dispose() {
		// if we are connected to a conduit, tell conduit to detach this exit
		if (transmitter != null) {
			transmitter.signal(new DetachConduitSignal());
			transmitter.dispose();
			transmitter = null;
		}

		super.dispose();
	}
	
	/**
	pass raw unwrapped data to this entrance
	 */
	private void send(Message<T> msg) throws InterruptedException {
		Transmitter<T, ?,?,?> trans = waitForTransmitter();
		if (trans != null) {
			this.customSITime = msg.getObservation().getTimestamp();
			trans.transmit(msg);
			increment();
		}
	}
}
