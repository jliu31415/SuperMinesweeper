import java.util.*;
//java SuperMinesweeperTester -exec "java -cp \"C:\Users\JL\Documents\Eclipse Workspace\SuperMinesweeper\bin\" GameLoop"

public class GameLoop {
	private static Scanner s;
	private static int N, M, D, d;
	private static int[][] board;
	private static final int MINE = -1, UNCHECKED = -2;
	private static ArrayList<int[]> borderCells;
	private static ArrayList<ArrayList<int[]>> borderLinks;
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
						//create a copy so we can edit the list
						borderLinks = calculateBorderLinks(new ArrayList<int[]>(borderCells));
						if (++debugCount < borderLinks.size()) debug(borderLinks.get(debugCount).size()+"");
//							for (int[] a : borderLinks.get(debugCount)) debug(Arrays.toString(a));
						else debug = false;
					}
					
					//debug(Arrays.deepToString(board));
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
			s.next();	//clear debug output
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
				isMine(cell[0], cell[1]);
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
	
	public static ArrayList<ArrayList<int[]>> calculateBorderLinks(ArrayList<int[]> borderCells) {
		ArrayList<ArrayList<int[]>> links = new ArrayList<ArrayList<int[]>>();
		boolean[][] cellsScanned = new boolean[N][N];	//keep track if cell has already been dealt with
		int row, col;
		while (borderCells.size() > 0) {
			for (int i = borderCells.size()-1; i >= 0; i--) {
				row = borderCells.get(i)[0];
				col = borderCells.get(i)[1];
				if (cellsScanned[row][col]) {
					borderCells.remove(i);
				} else {
					cellsScanned[row][col] = true;
					links.add(getCellLink(row, col, cellsScanned));
				}
			}
		}
		return links;
	}
	
	public static ArrayList<int[]> getCellLink(int row, int col, boolean[][] cellsScanned) {
		ArrayList<int[]> cellLink = new ArrayList<int[]>();
		for (int[] checked : getAdj(row, col, true)) {
			if (!cellsScanned[checked[0]][checked[1]]) {
				cellsScanned[checked[0]][checked[1]] = true;
				for (int[] link : getAdj(checked[0], checked[1], false)) {
					if (!cellsScanned[link[0]][link[1]]) {
						cellLink.add(link);
						cellsScanned[link[0]][link[1]] = true;
						cellLink.addAll(getCellLink(link[0], link[1], cellsScanned));
					}
				}
			}
		}
		return cellLink;
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
		} else {
			board[row][col] = s.nextInt();
			s.next();	//clear runtime feedback
			if (board[row][col] == 0) isZero(row, col);
			else borderCells.add(new int[] {row, col});
		}
	}
	
	public static void isMine(int row, int col) {
		stuck = false;
		System.out.println("F " + row + " " + col);
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
					if (checked && board[i][j] != UNCHECKED) adjacent.add(new int[] {i, j});
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