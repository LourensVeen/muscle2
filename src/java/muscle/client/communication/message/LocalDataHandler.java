/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package muscle.client.communication.message;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import muscle.client.LocalManager;
import muscle.client.communication.Receiver;
import muscle.id.Identifier;
import muscle.util.concurrency.SafeTriggeredThread;

/**
 *
 * @author Joris Borgdorff
 */
public class LocalDataHandler extends SafeTriggeredThread implements IncomingMessageProcessor {
	private final Map<Identifier,Receiver> listener;
	private final Queue<Message> messages;
	private final static Logger logger = Logger.getLogger(LocalDataHandler.class.getName());

	public LocalDataHandler() {
		super("LocalDataHandler");
		listener = new ConcurrentHashMap<Identifier,Receiver>();
		messages = new ConcurrentLinkedQueue<Message>();
	}
	
	@Override
	protected void execute() throws InterruptedException {
		Message msg;
		while ((msg = messages.poll()) != null && !isDisposed()) {
			Identifier recipient = msg.getRecipient();
			Receiver recv;
			recv = listener.get(recipient);
			
			if (recv == null) {
				if (!msg.isSignal() || !(msg.getSignal() instanceof DetachConduitSignal)) {
					logger.log(Level.WARNING, "No receiver registered for message {0}.", msg);					
				}
			} else {
				recv.put(msg);
			}
		}
	}

	@Override
	public void addReceiver(Identifier id, Receiver recv) {
		listener.put(id, recv);
	}
	
	public final void put(Message msg) {
		messages.add(msg);
		this.trigger();
	}
	
	@Override
	protected void handleInterruption(InterruptedException ex) {
		if (!this.isDisposed()) {
			logger.log(Level.WARNING, "LocalDataHandler interrupted.", ex);
		}
	}
	
	@Override
	protected void handleException(Throwable ex) {
		logger.log(Level.SEVERE, "LocalDataHandler had a fatal exception.", ex);
		LocalManager.getInstance().shutdown(15);
	}

	@Override
	public void removeReceiver(Identifier id) {
		listener.remove(id);
	}
	
	@Override
	public void dispose() {
		super.dispose();
		for (Receiver recv : listener.values()) {
			recv.dispose();
		}
	}
}
