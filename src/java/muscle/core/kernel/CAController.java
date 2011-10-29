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

package muscle.core.kernel;

import muscle.core.CxADescription;


// coast sk:coast.cxa.test.sandbox.Kernel\("execute true"\) --cxa_file src/coast/cxa/test.sandbox --main

/**
JADE agent to wrap a kernel (e.g. CA or MABS)
@author Jan Hegewald
*/
public abstract class CAController extends muscle.core.kernel.RawKernel {


	/**
	returns path to the directory which contains CxA specific files<br>
	do not change signature! (used from native code)
	*/
	@Deprecated
	static public String getCxAPath() {
		
		muscle.logging.Logger.getLogger(CAController.class).fine("using deprecated method muscle.core.kernel.CAController#getCxAPath()");

		return CxADescription.ONLY.getPathProperty("cxa_path");
	}


	/**
	returns path to the directory which contains a class<br>
	returns an empty string if the class is bundled in a jar file
	*/
	@Deprecated
	static public String getKernelPath(Class cls) {

		java.net.URL rsrc = cls.getResource(""); // -> null if class is in jar bundle
		
		String path = "";
		if(rsrc != null)
			path = rsrc.getPath();
		else
			muscle.logging.Logger.getLogger(cls).warning("no kernel path ("+path+") for class ("+cls+")");
			
		cls.getResource("").getPath();
		if(cls.getPackage() == null) {
			muscle.logging.Logger.getLogger(cls).warning("ambiguous kernel path ("+path+") for class ("+cls+")");
		}
		
		return path;
	}


	/**
	returns path to the directory which contains files specific for this kernel<br>
	do not change signature! (used from native code)<br>
	if the current class is not assigned to a package, this method will return the first directory of the CLASSPATH
	*/
	public String getKernelPath() {

		return getKernelPath(getClass());
	}


   /**
	returns properties as ASCII<br>
	do not change signature! (used from native code)
	*/
	static public String getLegacyProperties() {

		return CxADescription.ONLY.getLegacyProperties();
	}

}