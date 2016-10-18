package template;

import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class StateNode {

	private City currentCity;
	private TaskSet carriedTasks;
	private TaskSet remainingTasks;
	private int currentWeight = 0;

	private ActionEdge action;

	public StateNode(City curC, TaskSet carT, TaskSet remT, ActionEdge a) {

		currentCity = curC;
		carriedTasks = TaskSet.copyOf(carT);
		for(Task t : carriedTasks) {
			currentWeight += t.weight;
		}

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

	public int getWeight() {
		return currentWeight;
	}

	public boolean isFinalState() {
		return carriedTasks.isEmpty() && remainingTasks.isEmpty();
	}

	@Override
	public int hashCode() {
		return currentCity.hashCode() + carriedTasks.hashCode()
				+ remainingTasks.hashCode() + (action == null ? 0 : action.hashCode());
	}

	@Override
	public boolean equals(Object o) {
		return hashCode() == o.hashCode();
	}

	public String toString() {
		String carried = "";
		for (Task t : carriedTasks) {
			carried += t.toString() + "\n";
		}
		String remain = "";
		for (Task t : remainingTasks) {
			remain += t.toString() + "\n";
		}
		return "State " + hashCode() + " : \n City : "+currentCity+ "  \n Carry : \n" + carried
				+ " Remain : \n" + remain;
	}

}
