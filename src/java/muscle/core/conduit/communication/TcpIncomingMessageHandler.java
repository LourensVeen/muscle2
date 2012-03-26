/*
 * 
 */

package muscle.core.conduit.communication;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import muscle.core.ident.IDType;
import muscle.core.ident.Identifier;
import muscle.core.ident.Resolver;
import muscle.core.ident.ResolverFactory;
import muscle.core.messaging.BasicMessage;
import muscle.core.messaging.Timestamp;
import muscle.core.messaging.serialization.DeserializerWrapper;
import muscle.core.messaging.serialization.SerializerWrapper;
import muscle.core.messaging.signal.DetachConduitSignal;
import muscle.core.messaging.signal.Signal;
import muscle.core.messaging.signal.SignalEnum;
import muscle.net.ProtocolHandler;
import utilities.data.SerializableData;

/**
 *
 * @author Joris Borgdorff
 */
public class TcpIncomingMessageHandler extends ProtocolHandler<Boolean,Map<Identifier,Receiver>> {
	private final static Logger logger = Logger.getLogger(TcpIncomingMessageHandler.class.getName());
	private final static SignalEnum[] signals = SignalEnum.values();
	private final static TcpDataProtocol[] msgType = TcpDataProtocol.values();
	private final DataConnectionHandler connectionHandler;
	private final Resolver resolver;
	
	public TcpIncomingMessageHandler(Socket s, Map<Identifier,Receiver> receivers, ResolverFactory rf, DataConnectionHandler handler) throws InterruptedException {
		super(s, receivers);
		this.resolver = rf.getResolver();
		this.connectionHandler = handler;
	}
	
	@Override
	protected Boolean executeProtocol(DeserializerWrapper in, SerializerWrapper out) throws IOException {
		boolean success = false;

		in.refresh();
		int protoNum = in.readInt();
		// Protocol not executed.
		if (protoNum == -1) {
			this.socket.close();
			return null;
		}
		SignalEnum sigEnum = null;
		if (protoNum < 0 || protoNum >= msgType.length) {
			logger.log(Level.WARNING, "Unrecognized message type number {0} received.", protoNum);
		} else {
			TcpDataProtocol proto = msgType[protoNum];
			String owner_name = in.readString();
			IDType idType = IDType.values()[in.readInt()];
			Identifier recipient = resolver.getIdentifier(owner_name, idType);

			if (recipient != null) {
				switch (proto) {
					case OBSERVATION: {
						Timestamp time = new Timestamp(in.readDouble());
						Timestamp nextTime = new Timestamp(in.readDouble());
						SerializableData data = SerializableData.parseData(in);
						BasicMessage<SerializableData> msg = new BasicMessage<SerializableData>(data, time, nextTime, recipient);
						logger.log(Level.FINEST, "Message for {0} received with type {1}, size {2}, at time {3}.", new Object[]{recipient, data.getType(), data.getSize(), time});
						Receiver recv = listener.get(recipient);
						if (recv == null) {
							logger.log(Level.SEVERE, "No receiver registered for message for {0} received with type {1}, size {2}, at time {3}.", new Object[]{recipient, data.getType(), data.getSize(), time});	
						} else {
							recv.put(msg);
							success = true;					
						}
					} break;
					case SIGNAL: {
						Signal sig = null;
						int sigNum = in.readInt();
						if (sigNum < 0 || sigNum >= signals.length) {
								logger.log(Level.WARNING, "Unrecognized signal number {0} received for {1}.", new Object[]{sigNum, recipient});
						} else {
							sigEnum = signals[sigNum];
							switch (sigEnum) {
								case DETACH_CONDUIT:
									sig = new DetachConduitSignal();
									break;
								default:
									logger.log(Level.WARNING, "Unrecognized signal {0} received for {1}.", new Object[]{sigEnum, recipient});
							}
							if (sig != null) {
								Receiver recv = listener.get(recipient);
								if (recv == null) {
									logger.log(Level.SEVERE, "No receiver registered for signal {1} intended for {0}.", new Object[]{recipient, sig});	
								} else {
									recv.put(new BasicMessage<SerializableData>(sig,recipient));
									success = true;					
								}
							}
						}
					} break;
					default:
						break;
				}
			}
		}

		if (sigEnum != SignalEnum.DETACH_CONDUIT) {
			this.connectionHandler.resubmit(this);
		}
		
		return success;
	}
}
