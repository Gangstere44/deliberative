package template;

import logist.task.TaskSet;
import logist.topology.Topology.City;

public class StateNode {

	private City currentCity;
	private TaskSet carriedTasks = null;
	private TaskSet remainingTasks = null;

	private ActionEdge action;

	public StateNode(City curC, TaskSet carT, TaskSet remT, ActionEdge a) {

		currentCity = curC;
		if(carT != null) {
			carriedTasks = TaskSet.copyOf(carT);
		}
		if(remT != null) {
			remainingTasks =  TaskSet.copyOf(remT);
		}
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
		return (carriedTasks == null || carriedTasks.isEmpty()) && (remainingTasks == null || remainingTasks.isEmpty());
	}

	@Override
	public int hashCode() {
		return currentCity.hashCode() + (carriedTasks == null ? 0 : carriedTasks.hashCode()) + (remainingTasks == null ? 0 : remainingTasks.hashCode());
	}

	@Override
	public boolean equals(Object o) {
		return hashCode() == o.hashCode();
	}

}
