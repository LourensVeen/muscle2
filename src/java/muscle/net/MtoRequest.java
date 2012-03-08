/*
 * 
 */

package muscle.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author Mariusz Mamonski
 */
public class MtoRequest {
	static final byte TYPE_REGISTER = 1;
	static final byte TYPE_CONNECT = 2;
	static final byte TYPE_CONNECT_RESPONSE = 3;
	public byte type = 0;
	public InetAddress srcA;
	public InetAddress dstA;
	public short srcP = 0;
	public short dstP = 0;
	public int sessionId = 0;
	{
		try {
			srcA = InetAddress.getLocalHost();
			dstA = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			throw new RuntimeException("This is why we don't like Java", e);
		}
	}

	public String toString() {
		return "Type: " + type + "; src: " + srcA.toString() + ":" + srcP + "; dst: " + dstA.toString() + ":" + dstP;
	}

	public void setSource(InetSocketAddress isa) {
		srcA = isa.getAddress();
		srcP = (short) isa.getPort();
	}

	public void setDestination(InetSocketAddress isa) {
		dstA = isa.getAddress();
		dstP = (short) isa.getPort();
	}

	public ByteBuffer write() {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(17);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			buffer.put(type);
			buffer.put(srcA.getAddress()[3]);
			buffer.put(srcA.getAddress()[2]);
			buffer.put(srcA.getAddress()[1]);
			buffer.put(srcA.getAddress()[0]);
			buffer.putShort(srcP);
			buffer.put(dstA.getAddress()[3]);
			buffer.put(dstA.getAddress()[2]);
			buffer.put(dstA.getAddress()[1]);
			buffer.put(dstA.getAddress()[0]);
			buffer.putShort(dstP);
			buffer.putInt(sessionId);
			assert (buffer.remaining() == 0);
			return buffer;
		} catch (Throwable t) {
			throw new IllegalArgumentException("Could not serialise the request", t);
		}
	}

	public static int byteSize() {
		return 17;
	}

	public static MtoRequest read(ByteBuffer buffer) {
		try {
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			MtoRequest r = new MtoRequest();
			r.type = buffer.get();
			byte[] addr = new byte[4];
			addr[3] = buffer.get();
			addr[2] = buffer.get();
			addr[1] = buffer.get();
			addr[0] = buffer.get();
			r.srcA = InetAddress.getByAddress(addr);
			r.srcP = buffer.getShort();
			addr[3] = buffer.get();
			addr[2] = buffer.get();
			addr[1] = buffer.get();
			addr[0] = buffer.get();
			r.dstA = InetAddress.getByAddress(addr);
			r.dstP = buffer.getShort();
			r.sessionId = buffer.getInt();
			return r;
		} catch (UnknownHostException e) {
			throw new RuntimeException("This is why we don't like Java", e);
		} catch (Throwable t) {
			throw new IllegalArgumentException("Could not deserialise the request", t);
		}
	}

}