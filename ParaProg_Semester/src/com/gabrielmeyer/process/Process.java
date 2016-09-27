package com.gabrielmeyer.process;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import com.gabrielmeyer.node.Node;
import com.gabrielmeyer.node.NodeAbstract;

public class Process extends NodeAbstract {

	private boolean running = false, suspending = false;
	private Set<Node> noteNodes = Collections.synchronizedSet(new HashSet<>());
	private Set<Node> sendToNodes = Collections.synchronizedSet(new HashSet<>());
	private AtomicBoolean countedDown = new AtomicBoolean(false);

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
		System.out.println("this node " + this.toString() + ", neighbour: " + neighbour.toString());
		noteNodes.add(neighbour);
		synchronized (this) {
			if (!this.isAlive() && !isRunning()) {
				System.out.println("this.node: " + this.toString() + " woke up by: " + neighbour.toString());
				for (Node node : neighbours) {
					System.out.println(this.toString() + " neighbours: " + node.toString());
				}
				System.out.println(this.toString() + " wakeUp from " + neighbour.toString());
				suspending = true;
				running = true;
				this.start();
			}
		}
	}

	@Override
	public void echo(Node neighbour, Object data) {
		System.out.println(this.toString() + " echo from " + neighbour.toString());
		synchronized (sendToNodes) {
			sendToNodes.remove(neighbour);
		}
		if (sendToNodes.isEmpty())
			suspending = false;
		synchronized (this) {
			notify();
		}
	}

	@Override
	public void run() {
		System.out.println("initiator?: " + initiator);
		if (initiator) {
			try {
				System.out.println("before await()");
				startLatch.await();
				System.out.println("after await()");
				System.out.println("amount of neighbours: " + neighbours.size() + ", send call from " + this.getName());
				for (Node node : neighbours) {
					System.out.println("neighbour: " + node.toString());
					node.wakeup(this);
					sendToNodes.add(node);
				}
				suspending = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		synchronized (neighbours) {
			if (!initiator) {
				if (allNeighboursAreWaken())
					suspending = false;
				else {
					for (Node node : neighbours) {
						if (!noteNodes.contains(node)) {
							System.out.println("not initiator and neighbour: " + node.toString());
							node.wakeup(this);
							sendToNodes.add(node);
						}
					}
				}
			}
			try {

				synchronized (this) {
					while (suspending) {
						System.out.println(this.toString() + " waiting() " + isSuspending());
						wait();
					}
				}
				System.out.println(this.toString() + ": is not waiting anymore Yeah");
				synchronized (noteNodes) {

					for (Iterator<Node> iterator = noteNodes.iterator(); iterator.hasNext();) {
						Node node = iterator.next();
						node.echo(this, null);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println(this.toString() + ": run() ends");
	}

	public boolean isRunning() {
		return running;
	}

	public boolean isSuspending() {
		return suspending;
	}

	public synchronized boolean notWaker(Node node) {
		for (Node n : noteNodes) {
			if (node.equals(n)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public synchronized void setupNeighbours(Node... neighbours) {
		for (Node neighbour : neighbours) {
			System.out.println("setupNeighbour: " + neighbour.toString());
			this.neighbours.add(neighbour);
			neighbour.hello(this);
		}

		if (countedDown.compareAndSet(false, true)) {
			startLatch.countDown();
			System.out.println("counted startLatch down: " + startLatch.getCount());
		}
	}

	private boolean allNeighboursAreWaken() {
		synchronized (neighbours) {
			for (Node node : neighbours) {
				synchronized (noteNodes) {
					if (!noteNodes.contains(node)) {
						return false;
					}
				}
			}
			return true;
		}
	}
}
