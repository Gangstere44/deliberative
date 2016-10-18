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

	@Override
	public int hashCode() {
		return (task == null ? 12 : task.hashCode()) + (lastState == null ? 6 : lastState.hashCode()) + (moveTo == null ? 2 : moveTo.hashCode()) + (pickup ? 1 : 0);
	}

	@Override
	public boolean equals(Object o) {
		ActionEdge other = (ActionEdge) o;
		return other.getLastState().equals(lastState) && other.getMoveTo().equals(moveTo) && other.pickup == pickup && other.task.equals(task);
	}

	public String toString() {
		return "";
	}
}
