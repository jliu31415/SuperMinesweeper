import java.util.*;
//java SuperMinesweeperTester -exec "java -cp \"C:\Users\JL\Documents\Eclipse Workspace\SuperMinesweeper\bin\" GameLoop"

public class GameLoop {
	private static Scanner s;
	private static int N, M, D, d;
	private static int[][] board;
	private static final int MINE = -1, UNCHECKED = -2;
	private static ArrayList<int[]> borderCells;
	private static boolean gameRunning = true;
	private static boolean debug = true;
	
	public static void main(String args[]) {		
		initVariables();
		
		Thread loop = new Thread(new Runnable() {
			public void run() {
				while (gameRunning) {
					debug("" + borderCells.size());
					for (int i = borderCells.size()-1; i >= 0; i--) {	//traverse backward in case of removal
						compute(borderCells.get(i));
					}
					
					if (M == 0) {
						gameRunning = false;
						System.out.println("STOP");
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
		d = (int) Math.pow(D, .5);	//root of D, floored
		board = new int[N][N];		
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
			s.next();	//clear debug output
			waitForData();
		}
	}
	
	public static void compute(int[] borderCell) {
		int row = borderCell[0];
		int col = borderCell[1];
		ArrayList<int[]> unchecked = getAdjUnchecked(row, col);
		int adjMines = countAdjMines(row, col);
		if (board[row][col] == adjMines + unchecked.size()) {	//surroundings are mines
			for (int[] cell : unchecked) {
				board[cell[0]][cell[1]] = MINE;
				isMine(cell[0], cell[1]);
				M--;	//decrement total mine count
			}
			borderCells.remove(borderCell);
		} else if (board[row][col] == adjMines) {	//surroundings are safe
			for (int[] cell : unchecked) {
				guess(cell[0], cell[1]);
			}
			borderCells.remove(borderCell);
		}
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
		if (board[row][col] != UNCHECKED) return;	//ignore if cell has been checked
		System.out.println("G " + row + " " + col);
		waitForData();
		if (s.hasNext("BOOM!")) {
			s.next();	//clear runtime feedback
		} else {
			board[row][col] = s.nextInt();
			s.next();	//clear runtime feedback
			
			if (board[row][col] == 0) isZero(row, col);
			else borderCells.add(new int[] {row, col});
		}
	}
	
	public static void isMine(int row, int col) {
		System.out.println("F " + row + " " + col);
	}
	
	public static int countAdjMines(int row, int col) {
		int count = 0;
		for (int i = Math.max(row-d, 0); i < Math.min(row+d+1, N); i++) {
			for (int j = Math.max(col-d, 0); j < Math.min(col+d+1, N); j++) {
				if (board[i][j] == MINE) count++;
			}
		}
		return count;
	}
	
	public static ArrayList<int[]> getAdjUnchecked(int row, int col) {
		ArrayList<int[]> adjacent = new ArrayList<int[]>();
		for (int i = Math.max(row-d, 0); i < Math.min(row+d+1, N); i++) {
			for (int j = Math.max(col-d, 0); j < Math.min(col+d+1, N); j++) {
				if (board[i][j] == UNCHECKED) adjacent.add(new int[] {i, j});
			}
		}
		return adjacent;
	}
	
	public static void debug(String s) {
		if (debug) System.out.println("Debug " + s);
	}
}