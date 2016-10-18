package template;

import logist.task.Task;
import logist.topology.Topology.City;

public class ActionEdge {

	private StateNode lastState;
	private City moveTo;
	private boolean pickup;
	private Task task;

	public ActionEdge(StateNode s, City to, boolean pick, Task t) {

		lastState = s;
		moveTo = to;
		pickup = pick;
		task = t;

	}

	public StateNode getLastState() {
		return lastState;
	}

	public City getMoveTo() {
		return moveTo;
	}

	public boolean isPickup() {
		return pickup;
	}

	public Task getTask() {
		return task;
	}
}
