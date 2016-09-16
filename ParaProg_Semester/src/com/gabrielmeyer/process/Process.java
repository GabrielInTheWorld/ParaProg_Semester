package com.gabrielmeyer.process;

import java.util.ArrayList;
import java.util.List;

import com.gabrielmeyer.node.Node;

public class Process implements Node {

	private boolean initiator = false;
	private List<Node> neighbours = new ArrayList<Node>();
	// private List<Node> copyOfNeighbours = new ArrayList<Node>();
	private Node noteNode = null;

	public Process(boolean initiator) {
		this.initiator = initiator;
	}

	@Override
	public void hello(Node neighbour) {
		if (unknown(neighbour)) {
			neighbours.add(neighbour);
		}
	}

	@Override
	public void wakeup(Node neighbour) {
		noteNode = neighbour;
		for (Node node : neighbours) {
			if (!neighbour.equals(node)) {
				node.wakeup(this);
			}
		}
	}

	@Override
	public void echo(Node neighbour, Object data) {
		if (noteNode != null)
			neighbours.remove(noteNode);
		for (Node node : neighbours) {
			if (neighbour.equals(node)) {
				neighbours.remove(node);
			}
		}
		if (neighbours.isEmpty() && noteNode != null) {
			noteNode.echo(this, data);
		}
	}

	/**
	 * verify if the given neighbor is unknown to this node
	 * 
	 * @param neighbour
	 * @return true if the neighbor is unknown
	 */
	private boolean unknown(Node neighbour) {
		for (Node node : neighbours) {
			if (neighbour.equals(node)) {
				return false;
			}
		}
		return true;
	}
}
