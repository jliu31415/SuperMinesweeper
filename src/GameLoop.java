import java.util.*;
//java SuperMinesweeperTester -exec "java -cp \"C:\Users\JL\Documents\Eclipse Workspace\SuperMinesweeper\bin\" GameLoop"

public class GameLoop {
	private static Scanner s;
	private static int N, M, D;
	private static double d;
	private static int[][] board;
	private static final int MINE = -1, HIDDEN = -2;
	private static final int THRESHOLD = 20;	//threshold exponent for guessing (2^threshold)
	private static final double TOLERANCE = Math.pow(10, -6);	//tolerance when dealing with double matrices (round off error)
	private static ArrayList<ArrayList<int[]>> revealedLinks; 
	private static ArrayList<ArrayList<int[]>> hiddenLinks;
	private static ArrayList<int[]> revealedBorder;
	private static ArrayList<double[][]> matrices;
	private static boolean gameRunning = true;
	private static boolean output = true;
	private static boolean debug = true;
	
	public static void main(String args[]) {
		Thread loop = new Thread(new Runnable() {
			public void run() {
				initVariables();
				while (gameRunning) {
					if (M == 0 || !output) endGame();
					
					do {
						updateBorder();
						output = false;
						//concurrent modification: must iterate through revealed border as such (guess() may append cells)
						for (int i = 0; i < revealedBorder.size(); i++) {
							compute(revealedBorder.get(i)[0], revealedBorder.get(i)[1]);	//use basic logic method
						}
					} while (output);
					
					updateLinks();
					updateMatrices();
					
					linAlg();	//use linear algebra method
					if (!output) {
						endGame();
						//debug("GUESSING");
						//localMinGuess();
					}
				}
			}
		});
		
		loop.run();
	}
	
	public static void initVariables() {
		s = new Scanner(System.in);
		waitForData();
		N = s.nextInt();	//size of grid
		waitForData();
		M = s.nextInt();	//number of mines
		waitForData();
		D = s.nextInt();	//squared distance threshold
		d = Math.pow(D, .5);	//root of D, floored
		waitForData();
		int initRow = s.nextInt();
		waitForData();	
		int initCol = s.nextInt();
		
		revealedLinks = new ArrayList<ArrayList<int[]>>();
		hiddenLinks = new ArrayList<ArrayList<int[]>>();
		revealedBorder = new ArrayList<int[]>();
		matrices = new ArrayList<double[][]>();
		
		board = new int[N][N];
		for (int[] a : board) Arrays.fill(a, HIDDEN);		
		board[initRow][initCol] = 0;
		isZero(initRow, initCol);
	}
	
	public static void waitForData() {
		while (!s.hasNext()) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (s.hasNext("Debug")) {
			s.nextLine();	//clear debug output
			waitForData();
		}
	}
	
	//resolve with basic logic
	public static void compute(int row, int col) {
		ArrayList<int[]> hidden = getAdj(row, col, false);
		int numMines = numMines(row, col);
		if (board[row][col] == numMines + hidden.size()) {	//surroundings are mines
			for (int[] mine : hidden) {
				isMine(mine[0], mine[1], true);
			}
		} else if (board[row][col] == numMines) {	//surroundings are safe
			for (int[] safe : hidden) {
				guess(safe[0], safe[1]);
			}
		}
	}
	
	//resolve with linear algebra
	public static void linAlg() {
		for (int i = 0; i < matrices.size(); i++) {
			double[][] matrix = matrices.get(i);
			ArrayList<int[]> hLink = hiddenLinks.get(i);
			ArrayList<int[]> pCells = new ArrayList<int[]>();	//coordinates of positive entries
			ArrayList<int[]> nCells = new ArrayList<int[]>();	//coordinates of negative entries
			
			for (int row = 0; row < matrix.length; row++) {
				double pos = 0, neg = 0;
				pCells.clear();
				nCells.clear();
				
				for (int col = 0; col < matrix[0].length-1; col++) {
					if (matrix[row][col] > TOLERANCE) {
						pos += matrix[row][col];
						pCells.add(hLink.get(col));
					} else if (matrix[row][col] < -TOLERANCE) {
						neg += matrix[row][col];
						nCells.add(hLink.get(col));
					}
				}
				
				double val = matrix[row][matrix[0].length-1];
				if (pCells.size() + nCells.size() > 0) {	//if not zero row
					if (Math.abs(val-pos) < TOLERANCE) {	//all positive entries are mines (1), all negative entries are safe (0)
						for (int[] p : pCells) {
							isMine(p[0], p[1], true);
						}
						for (int[] n : nCells) {
							guess(n[0], n[1]);
						}
					} else if (Math.abs(val-neg) < TOLERANCE) {	//all negative entries are mines (1), all positive entries are safe (0)
						for (int[] p : pCells) {
							guess(p[0], p[1]);
						}
						for (int[] n : nCells) {
							isMine(n[0], n[1], true);
						}
					}	//else, row does not give information with certainty
				}
			}
		}
	}
	
	public static double[] bruteForce(double[][] matrix) {
		double[] probs = new double[matrix[0].length-1];
		
		int cases = 0;
		String binArrange;	//binary arrangement
		for (int bin = 0; bin < Math.pow(2, probs.length); bin++) {
			boolean success = true;
	
			binArrange = Integer.toBinaryString(bin);
			StringBuilder padding = new StringBuilder();
			for (int pad = 0; pad < probs.length-binArrange.length(); pad++) padding.append("0");	//pad with zeros
			binArrange  = padding.toString() + binArrange;
			
			for (int i = 0; i < matrix.length; i++) {
				double val = 0;
				for (int j = 0; j < probs.length; j++) {
					if (binArrange.charAt(j) == '1')
						val += matrix[i][j];
				}
				if (Math.abs(val-matrix[i][matrix[0].length-1]) > TOLERANCE) {
					success = false;	//binary arrangement failed
					break;
				}
			}
			
			if (success) {
				cases++;
				for (int index = 0; index < probs.length; index++) {
					if (binArrange.charAt(index) == '1') {
						probs[index]++;
					}
				}
			}
		}
		
		if (cases != 0) {
			for (int index = 0; index < probs.length; index++) {
				probs[index] /= cases;
			}
		}
		
		return probs;	//cell appearances divided by total cases
	}
	
	//guess using local probabilities
	public static void localMinGuess() {
		int[] guess = hiddenLinks.get(0).get(0);
		double probability = 1;	//probability of a mine; search for minimum
		for (ArrayList<int[]> link : hiddenLinks) {
			for (int[] hidden : link) {
				double p = 0;	//find worst case probability
				for (int[] revealed : getAdj(hidden[0], hidden[1], true)) {
					double numerator = board[revealed[0]][revealed[1]] - numMines(revealed[0], revealed[1]);
					double denominator = getAdj(revealed[0], revealed[1], false).size();
					p = Math.max(p, numerator/denominator);
				}
				if (p < probability) {
					probability = p;
					guess = hidden;
				}
			}
		}
		guess(guess[0], guess[1]);
	}
	
	//remove cell if no longer on border
	public static void updateBorder() {
		for (int i = revealedBorder.size()-1; i >= 0; i--) {	//traverse backwards to prevent concurrent modification
			//if cell no longer has hidden adjacent cells
			if (getAdj(revealedBorder.get(i)[0], revealedBorder.get(i)[1], false).size() == 0)
				revealedBorder.remove(i);
		}
	}
	
	//return matrix system that corresponds to specified cell links, must be double[][] return type for row reduction
	public static void updateMatrices() {
		matrices.clear();
		ArrayList<int[]> rLink;
		ArrayList<int[]> hLink;
		
		for (int index = 0; index < revealedLinks.size(); index++) {
			rLink = revealedLinks.get(index);
			hLink = hiddenLinks.get(index);
			double[][] matrix = new double[rLink.size()][hLink.size()+1];
			
			for (int i = 0; i < rLink.size(); i++) {
				for (int j = 0; j < hLink.size(); j++) {
					int xDist = rLink.get(i)[0] - hLink.get(j)[0];
					int yDist = rLink.get(i)[1] - hLink.get(j)[1];
					if (Math.hypot(xDist, yDist) <= d) matrix[i][j] = 1;
				}
			}
			
			//fill in last column of augmented matrix with given values (number of remaining mines)
			for (int i = 0; i < rLink.size(); i++) {
				int row = rLink.get(i)[0];
				int col = rLink.get(i)[1];
				matrix[i][hLink.size()] = board[row][col] - numMines(row, col);
			}
			
			matrices.add(rref(matrix, 0, 0));
		}
	}
	
	public static void updateLinks() {
		revealedLinks.clear();
		hiddenLinks.clear();
		boolean[][] scanned = new boolean[N][N];	//keep track if cell has already been dealt with
		int row, col, cellCount = 0;

		while (cellCount < revealedBorder.size()) {
			for (int[] borderCell : revealedBorder) {
				row = borderCell[0];
				col = borderCell[1];
				if (scanned[row][col]) {
					continue;
				} else {
					ArrayList<ArrayList<int[]>> cellLinks = getCellLinks(row, col, scanned);	//scanned will update accordingly
					revealedLinks.add(cellLinks.get(0));
					hiddenLinks.add(cellLinks.get(1));
					cellCount += cellLinks.get(0).size();
				}
			}
		}
	}
	
	//add links containing specified row and column
	public static ArrayList<ArrayList<int[]>> getCellLinks(int row, int col, boolean[][] scanned) {
		ArrayList<int[]> rLink = new ArrayList<int[]>();	//link containing revealed cells
		ArrayList<int[]> hLink = new ArrayList<int[]>();	//link containing hidden cells

		for (int[] hidden : getAdj(row, col, false)) {	//get hidden cells adjacent to revealed border cell
			if (scanned[hidden[0]][hidden[1]]) continue;	//skip if already scanned
			scanned[hidden[0]][hidden[1]] = true;
			hLink.add(hidden);
			for (int[] revealed : getAdj(hidden[0], hidden[1], true)) {	//get revealed cells adjacent to hidden cell
				if (scanned[revealed[0]][revealed[1]]) continue;		//skip if already scanned
				rLink.add(revealed);
				scanned[revealed[0]][revealed[1]] = true;
				ArrayList<ArrayList<int[]>> recurLinks = getCellLinks(revealed[0], revealed[1], scanned);
				rLink.addAll(recurLinks.get(0));
				hLink.addAll(recurLinks.get(1));
			}
		}
		
		ArrayList<ArrayList<int[]>> ret = new ArrayList<ArrayList<int[]>>();
		ret.add(rLink);
		ret.add(hLink);
		return ret;
	}
	
	//reveal all surrounding cells
	public static void isZero(int row, int col) {
		for (int i = (int) Math.max(row-d, 0); i < Math.min(row+d+1, N); i++) {
			for (int j = (int) Math.max(col-d, 0); j < Math.min(col+d+1, N); j++) {
				if (Math.hypot(row-i, col-j) <= d) {
					guess(i, j);
				}
			}
		}
	}
	
	public static void guess(int row, int col) {
		if (!(board[row][col] == HIDDEN)) return;	//ignore if cell has been revealed
		output = true;
		System.out.println("G " + row + " " + col);
		waitForData();
		
		if (s.hasNext("BOOM!")) {
			isMine(row, col, false);
			s.next();	//clear runtime feedback
			s.next();
		} else {
			board[row][col] = s.nextInt();
			s.next();	//clear runtime feedback
			
			if (board[row][col] == 0) isZero(row, col);		//recursive zero-out
			else revealedBorder.add(new int[] {row, col});	//add new (potential) border cell
		}
	}
	
	public static void isMine(int row, int col, boolean print) {
		if (!(board[row][col] == HIDDEN)) return;
		board[row][col] = MINE;
		M--;
		if (print) {
			output = true;
			System.out.println("F " + row + " " + col);
		}
	}
	
	public static int numMines(int row, int col) {
		int count = 0;
		for (int i = (int) Math.max(row-d, 0); i < Math.min(row+d+1, N); i++) {
			for (int j = (int) Math.max(col-d, 0); j < Math.min(col+d+1, N); j++) {
				if (Math.hypot(row-i, col-j) <= d) {
					if (board[i][j] == MINE) count++;
				}
			}
		}
		return count;
	}

	public static ArrayList<int[]> getAdj(int row, int col, boolean revealed) {
		ArrayList<int[]> adjacent = new ArrayList<int[]>();
		for (int i = (int) Math.max(row-d, 0); i < Math.min(row+d+1, N); i++) {
			for (int j = (int) Math.max(col-d, 0); j < Math.min(col+d+1, N); j++) {
				if (Math.hypot(row-i, col-j) <= d) {
					if (row == i && col == j) continue;		//skip if same cell
					//find revealed adjacent cells
					if (revealed && board[i][j] != HIDDEN && board[i][j] != MINE) adjacent.add(new int[] {i, j});
					//find hidden adjacent cells
					if (!revealed && board[i][j] == HIDDEN) adjacent.add(new int[] {i, j});	
				}
			}
		}
		return adjacent;
	}
	
	public static void endGame() {
		debug("END");
		System.out.println("STOP");

		delay(1000);	//wait for other program to end
		gameRunning = false;
	}
	
	//in this game, row reduction entries are all integers (double[][] unneeded)
	public static double[][] rref(double[][] matrix, int pivotR, int pivotC) {
		if (pivotR >= matrix.length || pivotC >= matrix[0].length) return matrix;
		
		//find partial pivot
		double max = 0;	//maximum absolute value
		int swapIndex = 0;
		for (int i = pivotR; i < matrix.length; i++) {
			if (Math.abs(matrix[i][pivotC]) > Math.abs(max)) {
				max = matrix[i][pivotC];
				swapIndex = i;
			}
		}
		if (Math.abs(max) <= TOLERANCE) return rref(matrix, pivotR, pivotC+1);	//skip zero column
		
		//swap pivot row (bring to top)
		if (pivotR != swapIndex) {
			for (int j = pivotC; j < matrix[0].length; j++) {
				matrix[pivotR][j] += matrix[swapIndex][j];
				matrix[swapIndex][j] = matrix[pivotR][j] - matrix[swapIndex][j];
				matrix[pivotR][j] -= matrix[swapIndex][j];
			}
		}
		
		//normalize pivot row (traverse backwards to retain first value)
		for (int j = matrix[0].length-1; j >= pivotC ; j--) {
			matrix[pivotR][j] /= matrix[pivotR][pivotC];
		}
		
		//zero out entries
		for (int i = 0; i < matrix.length; i++) {
			if (i == pivotR) continue;	//skip pivot row
			//traverse backwards to retain first value
			for (int j = matrix[0].length-1; j >= pivotC ; j--) {
				matrix[i][j] -= matrix[i][pivotC]*matrix[pivotR][j];	//subtract rows
			}
		}
		
		return rref(matrix, pivotR+1, pivotC+1);
	}
	
	public static void debug(String s) {
		if (debug) System.out.println("Debug " + s);
	}
	
	public static void debugInfo() {
		for (double[][] m : matrices) {
			debug("MATRIX");
			for (double[] a : m)
				debug(Arrays.toString(a));
		}
		
		for (int i = 0; i < revealedLinks.size(); i++) {
			debug("REVEALED LINK");
			for (int[] r : revealedLinks.get(i))
				debug(Arrays.toString(r));
			debug("HIDDEN LINKS");
			for (int[] h : hiddenLinks.get(i))
				debug(Arrays.toString(h));
		}
		
		debug("BORDER");
		for (int[] b : revealedBorder) {
			debug(Arrays.toString(b));
		}
		
		delay(1000);
	}
	
	public static void delay(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}