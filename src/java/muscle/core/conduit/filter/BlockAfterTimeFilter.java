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

package muscle.core.conduit.filter;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import muscle.core.model.Observation;
import muscle.core.model.Timestamp;

/**
ignores data after a given timestep, only recommended for debugging purposes
@author Jan Hegewald
*/
public class BlockAfterTimeFilter<E extends Serializable> extends AbstractFilter<E,E> {
	private final Timestamp maxTime;
	private final static Logger logger = Logger.getLogger(BlockAfterTimeFilter.class.getName());

	/** @param newMaxTime seconds after which the filter blocks */
	public BlockAfterTimeFilter(double newMaxTime) {
		super();
		maxTime = new Timestamp(newMaxTime);
	}

	protected void apply(Observation<E> subject) {
		if(subject.getTimestamp().compareTo(maxTime) < 1) {
			put(subject);
		}
		else {
			logger.log(Level.WARNING, "blocking data for time <{0}>", subject.getTimestamp());
		}
	}
}
