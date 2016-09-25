package com.gabrielmeyer.process;

import java.util.concurrent.CountDownLatch;

public class StartApplication {

	CountDownLatch startLatch;

	Process[] processors;

	public StartApplication(String kindOfGraph, int count) {
		processors = new Process[count];
		startLatch = new CountDownLatch(count / 2);
		switch (kindOfGraph) {
		case "C":
			startCircle(count);
			break;
		case "K":
			startFullGraph(count);
			break;
		case "BT":
			startBinaryTree(count);
			break;
		case "T":
			startTree(count);
			break;
		case "L":
			startLoop(count);
			break;
		default:
			break;
		}
		// Process[] processors = new Process[count];
		// for (int i = 0; i < count; ++i) {
		// processors[i] = new Process(String.valueOf(i), false, startLatch);
		// }
		// processors[count / 2] = new Process(String.valueOf(count / 2), true,
		// startLatch);
		// Thread t = new Thread(processors[count / 2]);
		// t.start();
		// processors[count / 2].setupNeighbours(processors[count / 2 - 2],
		// processors[count / 2 + 2]);
		// processors[count / 4].setupNeighbours(processors[count / 2 - 1],
		// processors[count / 2 - 3]);
		// processors[count / 2 + 2].setupNeighbours(processors[count / 2 + 1],
		// processors[count / 2 + 3]);

		// for (int i = 1; i < count; ++i) {
		// processors[i] = new Process(String.valueOf(i), false, startLatch);
		// }

		// for (int i = 0; i < processors.length; ++i) {
		// // processors[i].setupNeighbours(processors[(i + 1) % count]);
		// }

	}

	/**
	 * 
	 * @param count
	 */
	private void startCircle(int count) {
		// startLatch = new CountDownLatch(count);

		// initialize initiator
		processors[0] = new Process("0", true, startLatch);

		// initialize other processes
		for (int i = 1; i < count; ++i) {
			processors[i] = new Process(String.valueOf(i), false, startLatch);
		}

		// initiator starts
		processors[0].start();

		// setup neighbours
		for (int i = 0; i < processors.length; ++i) {
			processors[i].setupNeighbours(processors[(i + 1) % count]);
		}
	}

	private void startFullGraph(int count) {
		// startLatch = new CountDownLatch(count);

		processors[0] = new Process("0", true, startLatch);

		for (int i = 1; i < count; ++i) {
			processors[i] = new Process(String.valueOf(i), false, startLatch);
		}

		processors[0].start();
	}

	private void startBinaryTree(int count) {
		// startLatch = new CountDownLatch(count / 2);
		Process[] processors = new Process[count];
		for (int i = 0; i < count; ++i) {
			processors[i] = new Process(String.valueOf(i), false, startLatch);
		}
		processors[count / 2] = new Process(String.valueOf(count / 2), true, startLatch);
		// Thread t = new Thread(processors[count / 2]);
		processors[count / 2].start();
		processors[count / 2].setupNeighbours(processors[count / 2 - 2], processors[count / 2 + 2]);
		processors[count / 4].setupNeighbours(processors[count / 2 - 1], processors[count / 2 - 3]);
		processors[count / 2 + 2].setupNeighbours(processors[count / 2 + 1], processors[count / 2 + 3]);
	}

	private void startTree(int count) {
		processors[0] = new Process("0", true, startLatch);

		for (int i = 1; i < count; ++i) {
			processors[i] = new Process(String.valueOf(i), false, startLatch);
		}

		processors[0].start();

		for (int i = 0; i < count; ++i) {
			int iNumberOfNeighbours = (int) (Math.random() * count);
			if (iNumberOfNeighbours > 0) {
				for (int j = 0; j < iNumberOfNeighbours; ++j) {
					int iNeighbour = (int) (Math.random() * count);
					processors[i].setupNeighbours(processors[iNeighbour]);
				}
			} else {
				processors[i].setupNeighbours();
			}
		}
	}

	private void startLoop(int count) {

	}

	public static void main(String[] args) {
		for (String a : args) {
			System.out.println("a: " + a);
		}
		new StartApplication(args[0], Integer.valueOf(args[1]));
	}

}
