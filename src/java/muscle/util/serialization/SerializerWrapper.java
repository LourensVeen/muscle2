/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package muscle.util.serialization;

import java.io.IOException;
import java.io.Serializable;
import muscle.util.data.SerializableDatatype;

/**
 *
 * @author Joris Borgdorff
 */
public interface SerializerWrapper {
	public void writeInt(int num) throws IOException;
	public void writeBoolean(boolean bool) throws IOException;
	public void writeByteArray(byte[] bytes) throws IOException;
	public void writeString(String str) throws IOException;
	public void writeDouble(double d) throws IOException;
	public void writeValue(Serializable arr, SerializableDatatype type) throws IOException;
	public void flush() throws IOException;
	public void close() throws IOException;
}
