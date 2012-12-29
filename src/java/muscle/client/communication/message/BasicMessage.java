/*
 * 
 */
package muscle.client.communication.message;

import java.io.Serializable;
import muscle.core.model.Observation;
import muscle.core.model.Timestamp;
import muscle.id.Identifier;

/**
 *
 * @author Joris Borgdorff
 */
public class BasicMessage<E extends Serializable> implements Message<E>, Serializable {
	private static final long serialVersionUID = 1L;
	private final Observation<E> obs;
	private final Identifier recv;
	private final Signal signal;
	
	public BasicMessage(Signal s, Identifier recipient) {
		this.signal = s;
		this.recv = recipient;
		this.obs = null;
	}
	
	public BasicMessage(Observation<E> obs, Identifier recipient) {
		this.obs = obs;
		this.recv = recipient;
		this.signal = null;
	}

	public E getRawData() {
		return this.obs.getData();
	}

	public Observation<E> getObservation() {
		return this.obs;
	}

	public Identifier getRecipient() {
		return this.recv;
	}

	@Override
	public boolean isSignal() {
		return this.signal != null;
	}

	@Override
	public Signal getSignal() {
		return this.signal;
	}
	
	public String toString() {
		if (isSignal()) {
			return "BasicMessage<" + this.signal + "," + this.recv + ">";
		} else {
			return "BasicMessage<" + this.obs + "," + this.recv + ">";
		}
	}
}
