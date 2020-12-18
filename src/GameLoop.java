import java.util.*;
//java SuperMinesweeperTester -exec "java -cp \"C:\Users\JL\Documents\Eclipse Workspace\SuperMinesweeper\bin\" GameLoop"

public class GameLoop {
	private static Scanner s;
	private static int N, M, D, d;
	private static int[][] board;
	private static final int UNCHECKED = -2;
	private static boolean gameRunning = true;
	
	public static void main(String args[]) {		
		initVariables();
		
		Thread loop = new Thread(new Runnable() {
			public void run() {
				while (gameRunning) {
					//TODO
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
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
		}
	}
}