/*
 * 
 */

package muscle.core.kernel;

import muscle.core.ConduitEntranceController;

/**
 *
 * @author Joris Borgdorff
 */
public class DuplicationMapper extends FanOutMapper {
	@SuppressWarnings("unchecked")
	protected void sendAll() {
		for (ConduitEntranceController ec : this.entrances.values()) {
			ec.getEntrance().send(value);
		}
	}
}
