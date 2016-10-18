package template;

import logist.task.TaskSet;
import logist.topology.Topology.City;

public class StateNode {

	private City currentCity;
	private TaskSet carriedTasks;
	private TaskSet remainingTasks;

	private ActionEdge action;

	public StateNode(City curC, TaskSet carT, TaskSet remT, ActionEdge a) {

		currentCity = curC;
		carriedTasks = TaskSet.copyOf(carT);

		remainingTasks = TaskSet.copyOf(remT);

		action = a;

	}

	public City getCurrentCity() {
		return currentCity;
	}

	public TaskSet getCarriedTasks() {
		return carriedTasks;
	}

	public TaskSet getRemainingTasks() {
		return remainingTasks;
	}

	public ActionEdge getAction() {
		return action;
	}

	public boolean isFinalState() {
		return carriedTasks.isEmpty() && remainingTasks.isEmpty();
	}

	@Override
	public int hashCode() {
		return currentCity.hashCode() + carriedTasks.hashCode()
				+ remainingTasks.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return hashCode() == o.hashCode();
	}

}
