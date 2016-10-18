package template;

/* import table */
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm {
		BFS, ASTAR
	}

	/* Environment */
	Topology topology;
	TaskDistribution td;

	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;

		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class,
				"ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

		// ...
	}

	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			plan = bfsPlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		return plan;
	}

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {

		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}

	private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) {

		System.out.println("All tasks : ");
		for(Task t : tasks) {
			System.out.println(t.toString());
		}


		int capacity = vehicle.capacity();
		City firstCity = vehicle.getCurrentCity();

		// create an empty task set
		TaskSet emptySet = TaskSet.copyOf(tasks);
		emptySet.removeAll(tasks);

		StateNode currentState = new StateNode(firstCity, emptySet, tasks, null);

		HashSet<StateNode> visited = new HashSet<StateNode>();
		LinkedList<StateNode> toVisit = new LinkedList<StateNode>();
		toVisit.add(currentState);

		System.out.println("Compute BFS ...");

		do {

			Scanner sc = new Scanner(System.in);
			String next = "";
/*			do {
			 next = sc.nextLine();
			} while(next.equals("next"));
	*/		System.out.println("*********************************************************");

			System.out.println(toVisit.size());

			currentState = toVisit.poll();

		//	System.out.println("Current state : " + currentState.toString());

			if (currentState == null) {
				throw (new java.lang.IllegalArgumentException(
						"Unreachable city(ies)"));
			}

			if (currentState.isFinalState()) {
				break;
			}

			visited.add(currentState);

			// create the move action
			for (City n : currentState.getCurrentCity().neighbors()) {

				ActionEdge a = new ActionEdge(currentState, n, false, null);
				StateNode nextState = new StateNode(n,
						currentState.getCarriedTasks(),
						currentState.getRemainingTasks(), a);
			//	System.out.println("Next state : " + nextState);

				if (!visited.contains(nextState)) {
					toVisit.add(nextState);
				//	System.out.println("Accepted !!!");
				} else {
					//System.out.println("Discarded ...");
					sc.nextInt();
				}
			}

			// create the delivery action
			if (currentState.getCarriedTasks() != null) {
				for (Task t : currentState.getCarriedTasks()) {

					if (t.deliveryCity.equals(currentState.getCurrentCity())) {

						ActionEdge a = new ActionEdge(currentState, null,
								false, t);
						TaskSet newTaskSet = TaskSet.copyOf(currentState
								.getCarriedTasks());
						newTaskSet.remove(t);
						StateNode nextState = new StateNode(
								currentState.getCurrentCity(), newTaskSet,
								currentState.getRemainingTasks(), a);
						//System.out.println("Next state : " + nextState);
						/*
						if (!visited.contains(nextState)) {
							toVisit.add(nextState);
							System.out.println("Accepted !!!");
						} else {
							System.out.println("Discarded ...");
						}*/
						toVisit.add(nextState);
						break;
					}
				}
			}

			// create the pickup action
			if (currentState.getRemainingTasks() != null) {
				for (Task t : currentState.getRemainingTasks()) {
					if (t.pickupCity.equals(currentState.getCurrentCity())) {
						ActionEdge a = new ActionEdge(currentState, null, true,
								t);
						TaskSet newRemainingTaskSet = TaskSet
								.copyOf(currentState.getRemainingTasks());
						newRemainingTaskSet.remove(t);
						TaskSet newCarriedTaskSet = TaskSet.copyOf(currentState
								.getCarriedTasks());

						newCarriedTaskSet.add(t);

						StateNode nextState = new StateNode(
								currentState.getCurrentCity(),
								newCarriedTaskSet, newRemainingTaskSet, a);
						//System.out.println("Next state : " + nextState);
						//System.out.println("pickup : " + !visited.contains(nextState)  + " and " + (capacity >= currentState.getWeight() + t.weight));
						/*if (!visited.contains(nextState) && capacity >= currentState.getWeight() + t.weight) {
							toVisit.add(nextState);
							System.out.println("Accepted !!!");
						} else {
							System.out.println("Discarded ...");
						}*/
						toVisit.add(nextState);
						break;
					}
				}
			}



		} while (true);

		System.out.println("Compute plan ...");

		StateNode finalState = currentState;
		LinkedList<ActionEdge> stackOfActions = new LinkedList<ActionEdge>();
		do {

			stackOfActions.add(currentState.getAction());
			currentState = currentState.getAction().getLastState();

		} while (currentState.getAction() != null);

		Plan plan = new Plan(firstCity);
		ActionEdge currentAction;
		do {

			currentAction = stackOfActions.pollLast();

			if (currentAction.getMoveTo() != null) {
				plan.appendMove(currentAction.getMoveTo());
			} else {
				if (currentAction.isPickup()) {
					plan.appendPickup(currentAction.getTask());
				} else {
					plan.appendDelivery(currentAction.getTask());
				}
			}

		} while (!stackOfActions.isEmpty());

		return plan;
	}

}
