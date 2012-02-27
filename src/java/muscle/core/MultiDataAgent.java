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

import java.util.logging.Logger;
import muscle.core.ident.JadeAgentID;
import muscle.core.ident.JadeLocation;
import muscle.core.kernel.InstanceController;
import muscle.core.messaging.jade.IncomingMessageProcessor;

/**
JADE agent which filters incoming data messages and passes them to multiple message sinks
@author Jan Hegewald
 */
public abstract class MultiDataAgent extends jade.core.Agent implements InstanceController {
	protected transient IncomingMessageProcessor messageProcessor;
	private static final transient Logger logger = Logger.getLogger(MultiDataAgent.class.getName());

	@Override
	public JadeAgentID getIdentifier() {
		return new JadeAgentID(this.getLocalName(), this.getAID(), new JadeLocation(here()));
	}
	
	@Override
	public void takeDown() {
		messageProcessor = null;
	}

	@Override
	protected void setup() {
		initTransients();
	}

	@Override
	protected void afterMove() {
		initTransients();
	}

	protected void initTransients() {
		messageProcessor = new IncomingMessageProcessor(this);
		this.addBehaviour(messageProcessor);
	}
}
