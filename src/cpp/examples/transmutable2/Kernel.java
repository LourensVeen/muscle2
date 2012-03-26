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

package examples.transmutable2;

import muscle.core.DataTemplate;
import muscle.core.Scale;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import muscle.core.ConduitEntrance;
import muscle.core.ConduitExit;
import muscle.core.JNIConduitEntrance;
import muscle.core.JNIConduitExit;
import muscle.core.EntranceDependency;
import muscle.core.CxADescription;
import muscle.core.ident.PortalID;
import muscle.core.standalone.NativeKernel;
import muscle.exception.MUSCLERuntimeException;
import utilities.jni.JNIMethod;
import utilities.MiscTool;
import javatool.ArraysTool;
import java.math.BigDecimal;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;
import muscle.core.messaging.serialization.DataConverter;
import muscle.core.messaging.serialization.DoubleStringConverter;


/**
example of a kernel which is using native code to send and receive data to itself<br>
this example uses a Transmutable to map cpp data to a Java class
@author Jan Hegewald
@author mmamonski
*/
public class Kernel extends NativeKernel {

	private JNIConduitEntrance<double[],String> entrance;
	private JNIConduitExit<String,double[]> exit;

	//
	public muscle.core.Scale getScale() {
		DecimalMeasure<Duration> dt = DecimalMeasure.valueOf(new BigDecimal(1), SI.SECOND);
		DecimalMeasure<Length> dx = DecimalMeasure.valueOf(new BigDecimal(1), SI.METER);
		return new Scale(dt,dx);
	}

	
	public void addPortals() {
		DataConverter<double[], String> dc = new DoubleStringConverter();
		entrance = addJNIEntrance("writer", 1, double[].class, String.class, dc);

		exit = addJNIExit("reader", 1, String.class, double[].class, dc);
	}

}