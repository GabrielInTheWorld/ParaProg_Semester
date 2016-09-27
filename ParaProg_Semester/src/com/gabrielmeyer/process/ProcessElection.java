package com.gabrielmeyer.process;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.gabrielmeyer.node.Node;
import com.gabrielmeyer.node.NodeAbstract;

public class ProcessElection extends NodeAbstract {

	private AtomicInteger strength = new AtomicInteger();
	private AtomicBoolean countedDown = new AtomicBoolean(false);
	private String sInitiator;
	private Set<Node> noteNodes = Collections.synchronizedSet(new HashSet<>());
	private Set<Node> sendToNodes = Collections.synchronizedSet(new HashSet<>());
	private AtomicBoolean running = new AtomicBoolean(false);
	private AtomicBoolean suspending = new AtomicBoolean(false);
	private AtomicBoolean restart = new AtomicBoolean(false);

	public ProcessElection(String name, boolean initiator, CountDownLatch startLatch) {
		super(name, initiator, startLatch);
		if (initiator) {
			sInitiator = this.toString();
			strength.set((int) (Math.random() * 100) + 1);
		}

		System.out.println("Process: " + this.toString() + " has strength: " + this.strength.get());
	}

	@Override
	public void hello(Node neighbour) {
		neighbours.add(neighbour);
	}

	@Override
	public void wakeup(Node neighbour) {
		// noteNodes.add(neighbour);
		synchronized (this) {
			if (!this.isAlive() && running.compareAndSet(false, true)) {
				suspending.set(true);
				this.start();
			}
		}
	}

	/**
	 * calls to send messages to neighbours
	 */
	public void wakingTheDemon() {
		System.out.println("waking the demon(): " + this.toString());
		synchronized (neighbours) {
			for (Node node : neighbours) {
				if (!noteNodes.contains(node) && !sendToNodes.contains(node)) {
					ProcessElection e = (ProcessElection) node;
					e.woken(this, strength.get(), sInitiator);
					sendToNodes.add(node);
					System.out.println("wakingTheDemon(): " + this.toString() + ": send to node " + node.toString());
				}
			}
		}
	}

	/**
	 * method to compare strength and send the message to the next node if the
	 * strength is equals or greater, otherwise the strength from incoming node
	 * will be overwritten and send back
	 * 
	 * this method should be called every time instead of "wakeup()"
	 * 
	 * @param neighbour
	 * @param strength
	 * @param initiator
	 */
	public void woken(Node neighbour, int strength, String initiator) {
		System.out.println(
				"woken(): " + this.toString() + " woken by " + neighbour.toString() + " with strength: " + strength);

		/**
		 * case if the node is called first time
		 */
		if (this.strength.compareAndSet(0, strength)) {
			System.out.println("woken() - this has no own strength: " + this.toString() + " compareAndSet.");
			this.sInitiator = initiator;
			noteNodes.add(neighbour);
			this.wakeup(neighbour);

			/**
			 * case if the node gets a message from a node with equals strength
			 */
		} else if (this.strength.get() == strength) {
			System.out.println(this.toString() + " equals strength from " + neighbour.toString());
			if (!sendToNodes.contains(neighbour)) {
				System.out.println("woken() - incoming strength is equals to own strength: " + this.toString()
						+ " noteNodes add.");
				noteNodes.add(neighbour);
			}
			if (noMoreNeighbours()) {
				System.out.println("woken() - incoming strength is equals to own strength: " + this.toString()
						+ " no more neighbours.");
				suspending.set(false);
				synchronized (this) {
					notifyAll();
				}
			} else {
				System.out.println("woken() - incoming strength is equals to own strength: " + this.toString()
						+ " wakingTheDemon is called.");
				wakingTheDemon();
			}

			/**
			 * case if the node gets a message from a node with greater strength
			 */
		} else if (this.strength.get() < strength) {
			System.out.println(
					"woken() - incoming strength is greater than own strength: " + this.toString() + " begin.");
			this.strength.set(strength);
			this.sInitiator = initiator;
			synchronized (noteNodes) {
				System.out.println("woken() - incoming strength is greater than own strength: " + this.toString()
						+ "  noteNodes.");
				noteNodes.clear();
				noteNodes.add(neighbour);
			}
			synchronized (sendToNodes) {
				System.out.println("woken() - incoming strength is greater than own strength: " + this.toString()
						+ " clear sendToNodes.");
				sendToNodes.clear();
			}
			if (!noMoreNeighbours()) {
				System.out.println("woken() - incoming strength is greater than own strength: " + this.toString()
						+ " wakingTheDemon is called.");
				wakingTheDemon();
			} else {
				System.out.println("woken() - incoming strength is greater than own strength: " + this.toString()
						+ " will be not waiting anymore.");
				suspending.set(false);
				synchronized (this) {
					notifyAll();
				}
			}
		} else if (this.strength.get() > strength) {
			System.out.println("woken() - incoming strength is lower than own strength: " + this.toString()
					+ " send message back to " + neighbour.toString());
			ProcessElection e = (ProcessElection) neighbour;
			e.woken(this, this.strength.get(), sInitiator);
			sendToNodes.add(neighbour);
		}
	}

	@Override
	public void echo(Node neighbour, Object data) {
		// nodesToDelete.add(neighbour);
		System.out.println("echo(): " + this.toString() + " echo from " + neighbour.toString());
		synchronized (sendToNodes) {
			sendToNodes.remove(neighbour);
			System.out.println("echo(): " + this.toString() + ": sendToNodes.size() = " + sendToNodes.size());
		}
		if (sendToNodes.isEmpty())
			suspending.set(false);
		synchronized (this) {
			notifyAll();
		}
	}

	@Override
	public void setupNeighbours(Node... neighbours) {
		for (Node neighbour : neighbours) {
			this.neighbours.add(neighbour);
			neighbour.hello(this);
		}

		if (countedDown.compareAndSet(false, true)) {
			startLatch.countDown();
			System.out.println("counted startLatch down: " + startLatch.getCount());
		}
	}

	@Override
	public void run() {
		if (initiator) {
			try {
				startLatch.await();
			} catch (Exception e) {
				e.printStackTrace();
			}

			for (Node neighbour : neighbours) {
				ProcessElection e = (ProcessElection) neighbour;
				e.woken(this, strength.get(), sInitiator);
				sendToNodes.add(neighbour);
				// neighbour.wakeup(this);
			}

			suspending.set(true);
		}

		if (!initiator) {
			wakingTheDemon();
		}

		try {
			synchronized (this) {
				while (suspending.get()) {
					System.out.println(this.toString() + " is waiting.");
					wait();
				}
			}
			System.out.println(this.toString() + " is not waiting anymore.");
			synchronized (noteNodes) {
				for (Iterator<Node> iterator = noteNodes.iterator(); iterator.hasNext();) {
					Node node = iterator.next();
					node.echo(this, null);
					System.out.println("run(): " + this.toString() + " echo to " + node.toString());
				}
			}
		} catch (Exception e) {

		}
		// }
		// }
		// running = false;
		System.out.println(this.toString() + ": run() ends");
		if (initiator && sInitiator.equalsIgnoreCase(this.toString())) {
			System.out.println("run the echo-algorithm with node " + this.toString());
		}
		while (!restart.get()) {
			synchronized (this) {
				System.out.println("run() at the end - before restarting: " + this.toString() + ", noteNodes: "
						+ noteNodes.isEmpty() + ", sendToNodes: " + sendToNodes.isEmpty());
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// public boolean isRunning() {
	// return running;
	// }

	/**
	 * 
	 * @return true if there are no more neighbours else which are not in the
	 *         noteNodes or sendToNodes
	 */
	private boolean noMoreNeighbours() {
		System.out.println(this.toString() + "no more neighbours");
		for (Node node : neighbours) {
			if (noteNodes.contains(node) || sendToNodes.contains(node)) {
				return true;
			}
		}
		return false;
	}

	public String getInitiator() {
		return sInitiator;
	}

	public void setInitiator(String sInitiator) {
		this.sInitiator = sInitiator;
	}

}
