package com.gabrielmeyer.process;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.gabrielmeyer.node.Node;
import com.gabrielmeyer.node.NodeAbstract;

public class Process extends NodeAbstract {

	private boolean running = false, suspending = false;
	private Set<Node> noteNodes = new HashSet<>();

	public Process(String name, boolean initiator, CountDownLatch startLatch) {
		super(name, initiator, startLatch);
	}

	@Override
	public void hello(Node neighbour) {
		System.out.println(this.toString() + " get greetings from: " + neighbour.toString());
		neighbours.add(neighbour);
	}

	@Override
	public void wakeup(Node neighbour) {
		noteNodes.add(neighbour);
		synchronized (this) {
			if (!this.isAlive()) {
				System.out.println("this.node: " + this.toString() + " woke up by: " + neighbour.toString());
				for (Node node : neighbours) {
					System.out.println(this.toString() + " neighbours: " + node.toString());
				}
				System.out.println(this.toString() + " wakeUp from " + neighbour.toString());
				// Thread t = new Thread(this);
				suspending = true;
				this.start();
				// suspending = true;
			}
		}
	}

	@Override
	public void echo(Node neighbour, Object data) {
		System.out.println(this.toString() + " echo from " + neighbour.toString());
		noteNodes.remove(neighbour);
		if (noteNodes.isEmpty())
			suspending = false;
		// notifyAll();
	}

	@Override
	public void run() {
		System.out.println("initiator?: " + initiator);
		if (initiator) {
			try {
				System.out.println("before await()");
				startLatch.await();
				System.out.println("after await()");
				for (Node node : neighbours) {
					node.wakeup(this);
					noteNodes.add(node);
				}
				suspending = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		synchronized (this) {
			if (!initiator)
				for (Node node : neighbours) {
					if (!noteNodes.contains(node)) {
						node.wakeup(this);
					} else {
						suspending = false;
					}
				}
			try {

				while (suspending) {
					System.out.println(this.toString() + " waiting() " + isSuspending());
					wait();
				}
				System.out.println(this.toString() + ": is not waiting anymore Yeah");
				for (Node node : noteNodes) {
					node.echo(this, null);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println(this.toString() + ": run() ends");
	}

	// public boolean isRunning() {
	// return running;
	// }

	public boolean isSuspending() {
		return suspending;
	}

	public boolean notWaker(Node node) {
		for (Node n : noteNodes) {
			if (node.equals(n)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void setupNeighbours(Node... neighbours) {
		System.out.println(this.toString() + " neighbours: " + neighbours.toString());
		// TODO improve method
		for (Node neighbour : neighbours) {
			System.out.println("setupNeighbour: " + neighbour.toString());
			this.neighbours.add(neighbour);
			neighbour.hello(this);
		}
		// if (!initiator) {
		startLatch.countDown();
		// }
	}
}
