import java.util.*;
//java SuperMinesweeperTester -exec "java -cp \"C:\Users\JL\Documents\Eclipse Workspace\SuperMinesweeper\bin\" GameLoop"

public class GameLoop {
	private static Scanner s;
	private static int N, M, D, d;
	private static int[][] board;
	private static final int MINE = -1, UNCHECKED = -2;
	private static ArrayList<int[]> borderCells;
	private static ArrayList<int[][]> borderMatrices;
	private static int[][] adjMinesFound;	//number of adjacent mines
	private static boolean gameRunning = true;
	private static boolean stuck = true;
	private static boolean debug = true;
	
	public static void main(String args[]) {		
		initVariables();
		
		Thread loop = new Thread(new Runnable() {
			public void run() {
				int debugCount = -1;
				while (gameRunning) {					
					stuck = true;
					
					for (int i = borderCells.size()-1; i >= 0; i--) {	//traverse backward in case of removal
						//boolean stuck will toggle if guess or flag output
						compute(borderCells.get(i));
					}
					
					if (M == 0) {
						gameRunning = false;
						System.out.println("STOP");
					}
					
					//we are logically stuck
					if (stuck) {
						borderMatrices = calculateMatrices(borderCells);
						if (++debugCount > 0) debug = false;
					}
					
					debugLoop(0);
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
		d = (int) Math.pow(D, .5);	//root of D, floored
		board = new int[N][N];
		adjMinesFound = new int[N][N];
		for (int[] a : board) Arrays.fill(a, UNCHECKED);
		
		//initial row and column with zero value
		waitForData();
		int initRow = s.nextInt();
		waitForData();	
		int initCol = s.nextInt();
		board[initRow][initCol] = 0;
		borderCells = new ArrayList<int[]>();
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
	
	public static void compute(int[] borderCell) {
		int row = borderCell[0];
		int col = borderCell[1];
		ArrayList<int[]> unchecked = getAdj(row, col, false);
		
		if (board[row][col] == adjMinesFound[row][col] + unchecked.size()) {	//surroundings are mines
			for (int[] cell : unchecked) {
				board[cell[0]][cell[1]] = MINE;
				isMine(cell[0], cell[1], true);
				M--;	//decrement total mine count
			}
			borderCells.remove(borderCell);
		} else if (board[row][col] == adjMinesFound[row][col]) {	//surroundings are safe
			for (int[] cell : unchecked) {
				guess(cell[0], cell[1]);
			}
			borderCells.remove(borderCell);
		}
	}
	
	public static ArrayList<int[][]> calculateMatrices(ArrayList<int[]> borderCells) {
		ArrayList<int[][]> matrices = new ArrayList<int[][]>();
		boolean[][] cellsScanned = new boolean[N][N];	//keep track if cell has already been dealt with
		int row, col, cellCount = 0;
		while (cellCount < borderCells.size()) {
			for (int[] borderCell : borderCells) {
				row = borderCell[0];
				col = borderCell[1];
				if (cellsScanned[row][col]) {
					continue;
				} else {
					ArrayList<ArrayList<int[]>> cellLinks = getCellLinks(row, col, cellsScanned);	//cellsScanned will update accordingly
					debug("checked");
					for (int[] a : cellLinks.get(0)) debug(Arrays.toString(a));
					debug("unchecked");
					for (int[] a : cellLinks.get(1)) debug(Arrays.toString(a));
					matrices.add(getLinkedMatrix(cellLinks.get(0), cellLinks.get(1)));
				}
			}
		}
		return matrices;
	}
	
	//return matrix system that corresponds to specified cell links
	public static int[][] getLinkedMatrix(ArrayList<int[]> cLink, ArrayList<int[]> ucLink) {
		int[][] matrix = new int[ucLink.size()][cLink.size()];
		return matrix;
	}
	
	//returns chain that links to specified row and column
	//return is an ArrayList that consists of two links
	public static ArrayList<ArrayList<int[]>> getCellLinks(int row, int col, boolean[][] cellsScanned) {
		ArrayList<int[]> cLink = new ArrayList<int[]>();	//link containing checked cells
		ArrayList<int[]> ucLink = new ArrayList<int[]>();	//link containing unchecked cells
		
		for (int[] uc : getAdj(row, col, false)) {	//get unchecked adjacent to border
			if (cellsScanned[uc[0]][uc[1]]) continue;	//skip if already scanned
			cellsScanned[uc[0]][uc[1]] = true;
			ucLink.add(uc);
				for (int[] c : getAdj(uc[0], uc[1], true)) {	//get checked adjacent to unchecked adjacent
					if (cellsScanned[c[0]][c[1]]) continue;		//skip if already scanned
					cLink.add(c);
					cellsScanned[c[0]][c[1]] = true;
					ArrayList<ArrayList<int[]>> recurLinks = getCellLinks(c[0], c[1], cellsScanned);
					cLink.addAll(recurLinks.get(0));
					ucLink.addAll(recurLinks.get(1));
				}
		}
		
		ArrayList<ArrayList<int[]>> ret = new ArrayList<ArrayList<int[]>>();
		ret.add(cLink);
		ret.add(ucLink);
		return ret;
	}
	
	public static void isZero(int row, int col) {
		for (int i = Math.max(row-d, 0); i < Math.min(row+d+1, N); i++) {
			for (int j = Math.max(col-d, 0); j < Math.min(col+d+1, N); j++) {
				if (Math.pow(row-i, 2) + Math.pow(col-j, 2) <= D) {
					guess(i, j);
				}
			}
		}
	}
	
	public static void guess(int row, int col) {
		stuck = false;
		if (board[row][col] != UNCHECKED) return;	//ignore if cell has been checked
		System.out.println("G " + row + " " + col);
		waitForData();
		if (s.hasNext("BOOM!")) {
			s.nextLine();	//clear runtime feedback
			isMine(row, col, false);	//do not print output
		} else {
			board[row][col] = s.nextInt();
			s.next();	//clear runtime feedback
			if (board[row][col] == 0) isZero(row, col);
			else borderCells.add(new int[] {row, col});
		}
	}
	
	public static void isMine(int row, int col, boolean output) {
		if (output) {
			stuck = false;
			System.out.println("F " + row + " " + col);
		}
		//update adjacent mines array
		for (int i = Math.max(row-d, 0); i < Math.min(row+d+1, N); i++) {
			for (int j = Math.max(col-d, 0); j < Math.min(col+d+1, N); j++) {
				if (Math.pow(row-i, 2) + Math.pow(col-j, 2) <= D) {
					adjMinesFound[i][j]++;
				}
			}
		}
	}
	
	public static ArrayList<int[]> getAdj(int row, int col, boolean checked) {
		ArrayList<int[]> adjacent = new ArrayList<int[]>();
		for (int i = Math.max(row-d, 0); i < Math.min(row+d+1, N); i++) {
			for (int j = Math.max(col-d, 0); j < Math.min(col+d+1, N); j++) {
				if (Math.pow(row-i, 2) + Math.pow(col-j, 2) <= D) {
					if (checked && board[i][j] != UNCHECKED && board[i][j] != MINE) adjacent.add(new int[] {i, j});
					if (!checked && board[i][j] == UNCHECKED) adjacent.add(new int[] {i, j});	
				}
			}
		}
		return adjacent;
	}
	
	public static void debugLoop(int delayMillis) {
		debug("----------");
		try {
			Thread.sleep(delayMillis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void debug(String s) {
		if (debug) System.out.println("Debug " + s);
	}
}