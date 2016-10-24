package template;

/* import table */

import template.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

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

	TaskSet carriedTaskAfterCancellation;

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

		 algorithm = Algorithm.ASTAR;
		// ...

		carriedTaskAfterCancellation = null;
	}

	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = aStarPlan(vehicle, tasks);
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

			carriedTaskAfterCancellation = carriedTasks;
		} else {
			carriedTaskAfterCancellation = null;
		}
	}

	private Plan aStarPlan(Vehicle vehicle, TaskSet tasks) {

		int capacity = vehicle.capacity();
		City firstCity = vehicle.getCurrentCity();

		// create an empty task set
		TaskSet emptySet = TaskSet.copyOf(tasks);
		emptySet.removeAll(tasks);

		StateNode currentState = new StateNode(
				firstCity,
				(carriedTaskAfterCancellation != null ? carriedTaskAfterCancellation
						: emptySet), tasks, null);
		currentState.setG(0);
		currentState.setH(0);

		PriorityQueue<StateNode> c = new PriorityQueue<StateNode>(
				new Comparator<StateNode>() {

					@Override
					public int compare(StateNode o1, StateNode o2) {

						if (o1.getF() > o2.getF())
							return 1;
						else if (o1.getF() < o2.getF())
							return -1;
						else
							return 0;
					}
				});

		// allow us to know quickly if a state deserve to be put inside the
		// priority queue
		HashMap<StateNode, Double> f = new HashMap<StateNode, Double>();

		c.add(currentState);
		f.put(currentState, currentState.getF());

		System.out.println("Deliberative with a*");
		do {

			currentState = c.poll();

			if (currentState == null) {
				throw (new java.lang.IllegalArgumentException(
						"Unreachable city(ies)"));
			}

			if (currentState.isFinalState()) {
				break;
			}

			boolean hasDeliver = false;
			// create the delivery action
			for (Task t : currentState.getCarriedTasks()) {

				if (t.deliveryCity.equals(currentState.getCurrentCity())) {

					hasDeliver = true;

					ActionEdge a = new ActionEdge(currentState, null, false, t);
					TaskSet newTaskSet = TaskSet.copyOf(currentState
							.getCarriedTasks());
					newTaskSet.remove(t);
					StateNode nextState = new StateNode(
							currentState.getCurrentCity(), newTaskSet,
							currentState.getRemainingTasks(), a);
					nextState.setG(currentState.getG());
					nextState.setH(heuristic(nextState, capacity));

					if (!f.containsKey(nextState)) {
						f.put(nextState, nextState.getF());
						c.add(nextState);
					} else if (f.get(nextState) >= nextState.getF()) {
						f.put(nextState, nextState.getF());
						c.add(nextState);
					}
					break;
				}

			}

			// if we can deliver it doesn't make sense to look for others
			// possible action at this state, it will only make our queue
			// bigger for nothing
			if (hasDeliver) {
				continue;
			}

			// create the move action
			for (City n : currentState.getCurrentCity().neighbors()) {

				ActionEdge a = new ActionEdge(currentState, n, false, null);
				StateNode nextState = new StateNode(n,
						currentState.getCarriedTasks(),
						currentState.getRemainingTasks(), a);
				nextState.setG(currentState.getG()
						+ currentState.getCurrentCity().distanceTo(n));
				nextState.setH(heuristic(nextState, capacity));

				if (!f.containsKey(nextState)) {
					f.put(nextState, nextState.getF());
					c.add(nextState);
				} else if (f.get(nextState) <= nextState.getF()) {
					f.put(nextState, nextState.getF());
					c.add(nextState);
				}
			}

			// create the pickup action
			for (Task t : currentState.getRemainingTasks()) {
				if (t.pickupCity.equals(currentState.getCurrentCity())) {

					if (t.weight <= capacity - currentState.getWeight()) {
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
						nextState.setG(currentState.getG());
						nextState.setH(heuristic(nextState, capacity));

						if (!f.containsKey(nextState)) {
							f.put(nextState, nextState.getF());
							c.add(nextState);
						} else if (f.get(nextState) <= nextState.getF()) {
							f.put(nextState, nextState.getF());
							c.add(nextState);
						}
					}
				}
			}

		} while (!c.isEmpty());

		return constructPlan(currentState, firstCity);
	}

	private double heuristic(StateNode s, int maxWeight) {
		return heuristic2(s, maxWeight);
		/*
		// we want to deliver asap
		if (s.getAction().getMoveTo() == null && !s.getAction().isPickup()) {
			return 0.0;
		}

		// do we really want to take each time ?
		if (s.getAction().getMoveTo() == null && s.getAction().isPickup()) {
			return 0.0;
		}

		// we carry a task(s), go deliver the closest one
		if (s.getWeight() > 0) {

			double minDist = 0.0;
			boolean first = true;
			for (Task t : s.getCarriedTasks()) {
				double tmpDist = s.getCurrentCity().distanceTo(t.deliveryCity);
				if (first) {
					first = false;
					minDist = tmpDist;
				} else if (minDist > tmpDist) {
					minDist = tmpDist;
				}
			}

			return minDist;

			// we don't carry a task, go take the closest one
		} else {

			double minDist = 0.0;
			boolean first = true;
			for (Task t : s.getRemainingTasks()) {
				double tmpDist = s.getCurrentCity().distanceTo(t.deliveryCity);
				if (first) {
					first = false;
					minDist = tmpDist;
				} else if (minDist > tmpDist) {
					minDist = tmpDist;
				}
			}

			return minDist;
		}
*/
	}

	private double heuristic2(StateNode s, int maxWeight) {

		HashSet<City> neededCities = new HashSet<Topology.City>();

		for (Task t : s.getCarriedTasks()) {
			neededCities.addAll(t.deliveryCity.pathTo(s.getCurrentCity()));
		}

		for (Task t : s.getRemainingTasks()) {
			neededCities.addAll(t.deliveryCity.pathTo(t.pickupCity));
		}

		PriorityQueue<EdgeCity> p = new PriorityQueue<EdgeCity>(
				new Comparator<EdgeCity>() {

					@Override
					public int compare(EdgeCity o1, EdgeCity o2) {

						if (o1.distance < o2.distance)
							return -1;
						else if (o1.distance > o2.distance)
							return 1;
						else
							return 0;
					}
				});

		HashSet<EdgeCity> e = new HashSet<EdgeCity>();

		City cur = neededCities.iterator().next();

		HashSet<EdgeCity> result = new HashSet<EdgeCity>();

		while (!neededCities.isEmpty()) {


			neededCities.remove(cur);

			for (City c : cur.neighbors()) {
				if (neededCities.contains(c)) {
					EdgeCity newEdge = new EdgeCity(c, cur, c.distanceTo(cur));
					if (!e.contains(newEdge)) {
						e.add(newEdge);
						p.add(newEdge);
					}
				}
			}

			EdgeCity curEdge = p.poll();

			result.add(curEdge);

			cur = curEdge.from == cur ? curEdge.to : curEdge.from;
		}

		double sum = 0.0;
		for(EdgeCity c : result)
			sum += c.distance;

		return sum;
	}

	private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) {

		int capacity = vehicle.capacity();
		City firstCity = vehicle.getCurrentCity();

		// create an empty task set
		TaskSet emptySet = TaskSet.copyOf(tasks);
		emptySet.removeAll(tasks);

		StateNode currentState = new StateNode(
				firstCity,
				(carriedTaskAfterCancellation != null ? carriedTaskAfterCancellation
						: emptySet), tasks, null);

		HashSet<StateNode> visited = new HashSet<StateNode>();
		LinkedList<StateNode> toVisit = new LinkedList<StateNode>();
		toVisit.add(currentState);

		System.out.println("Deliberative with BFS");
		do {

			currentState = toVisit.poll();

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
				nextState.setG(currentState.getG()
						+ currentState.getCurrentCity().distanceTo(n));

				if (!visited.contains(nextState)) {
					toVisit.add(nextState);
				}
			}

			// create the delivery action
			for (Task t : currentState.getCarriedTasks()) {

				if (t.deliveryCity.equals(currentState.getCurrentCity())) {

					ActionEdge a = new ActionEdge(currentState, null, false, t);
					TaskSet newTaskSet = TaskSet.copyOf(currentState
							.getCarriedTasks());
					newTaskSet.remove(t);
					StateNode nextState = new StateNode(
							currentState.getCurrentCity(), newTaskSet,
							currentState.getRemainingTasks(), a);
					nextState.setG(currentState.getG());

					if (!visited.contains(nextState)) {
						toVisit.add(nextState);
					}
					break;
				}

			}

			// create the pickup action
			for (Task t : currentState.getRemainingTasks()) {
				if (t.pickupCity.equals(currentState.getCurrentCity())) {
					ActionEdge a = new ActionEdge(currentState, null, true, t);
					TaskSet newRemainingTaskSet = TaskSet.copyOf(currentState
							.getRemainingTasks());
					newRemainingTaskSet.remove(t);
					TaskSet newCarriedTaskSet = TaskSet.copyOf(currentState
							.getCarriedTasks());
					newCarriedTaskSet.add(t);
					StateNode nextState = new StateNode(
							currentState.getCurrentCity(), newCarriedTaskSet,
							newRemainingTaskSet, a);
					nextState.setG(currentState.getG());

					if (!visited.contains(nextState)
							&& capacity >= currentState.getWeight() + t.weight) {
						toVisit.add(nextState);
					}

				}

			}

		} while (!toVisit.isEmpty());

		return constructPlan(currentState, firstCity);
	}

	private Plan constructPlan(StateNode s, City fc) {
		System.out.println("Distance travelled : " + s.getG());
		System.out.println("Compute plan ...");

		StateNode finalState = s;
		LinkedList<ActionEdge> stackOfActions = new LinkedList<ActionEdge>();
		do {

			stackOfActions.add(s.getAction());
			s = s.getAction().getLastState();

		} while (s.getAction() != null);

		Plan plan = new Plan(fc);
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
