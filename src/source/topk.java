package source;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
 
import indexing.Btree;
import indexing.BtreeIterator;

public class topk {
	public static int k;
	public static int n;
	public static tfile[] tfiles;
	public static Btree[][] indices;

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println(
					"Error: wrong number of arguments. \nUsage: 'topk K N' where K and N are positive integers.");
		} else {
			try {
				k = Integer.parseInt(args[0]);
				n = Integer.parseInt(args[1]);
				if (k < 1 || n < 1) {
					System.out.println(
							"Error: wrong number of arguments. \nUsage: 'topk K N' where K and N are positive integers.");
					System.exit(1);
				}
			} catch (NumberFormatException e) {
				System.out.println("Error: invalid integer inputted.");
				System.exit(1);
			}
			Scanner in = new Scanner(System.in);

			// Wait for initialization
			while (true) {
				System.out.print("topk>");
				String cmd = in.nextLine();
				if (cmd.startsWith("init ")) {
					int status = init(cmd.substring(5).trim().split("( on )", 2)[0].replace(',', ' ').split(" +"));
					if (status == 1) { // One relation
						break;
					} else if (status == 2) { // Multiple relations
						if (cmd.substring(5).trim().split("( on )", 2).length == 1)
							parseJoin(null);
						else
							parseJoin(cmd.substring(5).trim().split("( on )", 2)[1]);
						break;
					} else { // An error occurred
						in.close();
						System.exit(1);
					}
				} else if (cmd.startsWith("run") || cmd.startsWith("join")) {
					System.out.println("Not initialized.");
				} else if (cmd.equals("exit")) {
					in.close();
					System.exit(0);
				} else {
					System.out.println("Unknown command.");
				}
			}

			// Wait for method (run1 - threshold algorithm; run2 - naive
			// approach; run3 - rank-join
			outerLoop: while (true) {
				System.out.print("topk>");
				String cmd = in.nextLine();
				if (cmd.startsWith("run1 ") || cmd.startsWith("run2 ") || cmd.startsWith("run3 ")) {
					String str_inputs[] = cmd.substring(5).trim().split(" +");
					if ((cmd.charAt(3) == '1' || cmd.charAt(3) == '2') && tfiles.length != 1) {
						System.out.println("Error: incompatible method for 1 file. Method 'run1' or 'run2' expected.");
					} else if (cmd.charAt(3) == '3' && tfiles.length == 1) {
						System.out.println("Error: incompatible method for multiple files. Method 'run3' expected.");
					} else if (str_inputs.length != n) {
						System.out.println("Error: incorrect number of inputs, expected " + n + ", but "
								+ str_inputs.length + " were input.");
					} else {
						int[] inputs = new int[str_inputs.length];
						for (int i = 0; i < inputs.length; i++) {
							try {
								inputs[i] = Integer.parseInt(str_inputs[i]);
								if (inputs[i] < 0) {
									System.out.println("Error: negative number inputted.");
									continue outerLoop;
								}
							} catch (NumberFormatException e) {
								System.out.println("Error: non-integer input.");
								continue outerLoop;
							}
						}
						if (cmd.charAt(3) == '1') {
							run1(inputs);
						} else if (cmd.charAt(3) == '2') {
							run2(inputs);
						} else if (cmd.charAt(3) == '3') {
							run3(inputs);
						}
						break;
					}
				} else if (cmd.startsWith("init ")) {
					System.out.println("Already initialized.");
				} else if (cmd.equals("exit")) {
					in.close();
					System.exit(0);
				} else {
					System.out.println("Unknown command.");
				}
			}
			in.close();
		}
	}

	// Read files into memory and create Btrees
	private static int init(String[] filenames) {
		tfiles = new tfile[filenames.length];
		indices = new Btree[filenames.length][];
		int attributeCount = 0;
		for (int i = 0; i < filenames.length; i++) {
			tfiles[i] = new tfile(filenames[i]);
			attributeCount += tfiles[i].numAttributes();
		}
		attributeCount -= filenames.length;
		if (attributeCount != n) {
			System.out.println("Error: Expected " + n + " non-ID attributes, but found " + attributeCount + ".");
			return 0;
		}

		// Btree Creation
		for (int i = 0; i < tfiles.length; i++) {
			indices[i] = new Btree[tfiles[i].numAttributes()];
			for (int j = 0; j < tfiles[i].numAttributes(); j++) {
				indices[i][j] = new Btree();
			}
			for (int k = 0; k < tfiles[i].numTuples(); k++) {
				int[] tuple = tfiles[i].getTuple(k);
				for (int l = 0; l < indices[i].length; l++) {
					// Add value tuple[l] and rowID k for k-th tuple in tfile
 					indices[i][l] = indices[i][l].add(tuple[l], k); 
				}
			}
		}
		return tfiles.length;
	}

	// Used to map tuples to scores
	public static class ScoreAndTuple {
		public int[] tuple;
		public int score;

		public ScoreAndTuple(int[] tuple, int score) {
			this.tuple = tuple;
			this.score = score;
		}
	}

	// Comparison based on score
	public static class SiCmp implements Comparator<ScoreAndTuple> {
		public int compare(ScoreAndTuple s1, ScoreAndTuple s2) {
			if (s1.score > s2.score) {
				return 1;
			} else if (s1.score < s2.score) {
				return -1;
			} else {
				return 0;
			}
		}
	}

	// Threshold algorithm of Fagan et al.
	private static void run1(int[] inputs) {
		if (k == 0 || tfiles[0].numTuples() == 0) {
			System.out.println(tfiles[0].getAttributes().toString() + "  [Score]");
			return;
		}

		// This priority queue stores the tuple with the k-th lowest score at the front
		PriorityQueue<ScoreAndTuple> resultQueue = new PriorityQueue<ScoreAndTuple>(k, new SiCmp());

		Set<Integer> idsOfCurrentResults = new HashSet<Integer>();
		BtreeIterator[] iterators = new BtreeIterator[n];
		for (int i = 0; i < n; i++) {
			iterators[i] = new BtreeIterator(indices[0][i + 1], false);
		}

		while (true) {
			int thresh = 0;

			// next set of values to consider
			for (int j = 0; j < n; j++) {
				BtreeIterator.ValueAndPtr vp = iterators[j].next();

				int[] tuple = tfiles[0].getTuple(vp.ptr);
				int score = 0;
				for (int l = 0; l < inputs.length; l++) {
					score += tuple[l + 1] * inputs[l];
				}
				if (resultQueue.size() < k) {
					if (!idsOfCurrentResults.contains(vp.ptr)) {
						resultQueue.add(new ScoreAndTuple(tuple, score));
						idsOfCurrentResults.add(vp.ptr);
					}
				} else if (score > resultQueue.peek().score && !idsOfCurrentResults.contains(vp.ptr)) {
					idsOfCurrentResults.remove(resultQueue.poll().tuple[0]);
					resultQueue.add(new ScoreAndTuple(tuple, score));
					idsOfCurrentResults.add(vp.ptr);
				}
				thresh += vp.value * inputs[j];
			}

			if (resultQueue.peek().score > thresh || !iterators[0].hasNext()) {
				break;
			}
		}
		printResults(tfiles[0].getAttributes(), resultQueue);
	}

	// Naive priority queue algorithm
	private static void run2(int[] inputs) {
		if (k == 0 || tfiles[0].numTuples() == 0) {
			System.out.println(tfiles[0].getAttributes().toString());
			return;
		}

		// This priority queue stores the tuple with the lowest seen score at the front
		PriorityQueue<ScoreAndTuple> resultQueue = new PriorityQueue<ScoreAndTuple>(k, new SiCmp());

		BtreeIterator iterator = new BtreeIterator(indices[0][0], true);
		while (iterator.hasNext()) {
			BtreeIterator.ValueAndPtr vp = iterator.next();
			int[] tuple = tfiles[0].getTuple(vp.ptr);
			int score = 0;
			for (int l = 0; l < inputs.length; l++) {
				score += tuple[l + 1] * inputs[l];
			}
			if (resultQueue.size() < k) {
				resultQueue.add(new ScoreAndTuple(tuple, score));
			} else if (score > resultQueue.peek().score) {
				resultQueue.poll();
				resultQueue.add(new ScoreAndTuple(tuple, score));
			}
		}
		printResults(tfiles[0].getAttributes(), resultQueue);
	}

	// Rank-join
	private static void run3(int[] inputs) {
		// Implement topk join algorithm
	}

	// Parses the join condition to be used in rank-join
	private static void parseJoin(String joinCondition) {

	}

	// Prints the results of run1, run2, or run3
	private static void printResults(String[] attributes, PriorityQueue<ScoreAndTuple> resultsQueue) {
		Stack<ScoreAndTuple> results = new Stack<ScoreAndTuple>();
		while (!resultsQueue.isEmpty()) {
			results.push(resultsQueue.poll());
		}
		System.out.println(Arrays.toString(attributes) + "  [Score]");
		int scoreOffset = Arrays.toString(attributes).length() + 2;
		while (!results.isEmpty()) {
			ScoreAndTuple next = results.pop();
			String tupleString = Arrays.toString(next.tuple);
			int numSpaces = scoreOffset - tupleString.length();
			if (numSpaces < 0)
				numSpaces = 0;
			System.out.print(tupleString);
			for (int i = 0; i < numSpaces; i++) {
				System.out.print(" ");
			}
			System.out.println("[" + next.score + "]");
		}
	}
}
