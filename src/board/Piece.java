package board;

public abstract class Piece {
	
	private Board board;
	protected Position position;

	public Piece(Board board) {
		this.board = board;
		position = null;
	}

	protected Board getBoard() {
		return board;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}
	
	public abstract boolean[][] possibleMoves();
	
	public boolean possibleMove(Position position) {
		return possibleMoves()[position.getRow()][position.getColumn()];
	}
	
	public boolean isThereAnyPossibleMove() {
		boolean[][] b = possibleMoves();
		
		for (int i = 0; i < b.length; i++)
			for (int j = 0; j < b.length; j++)
					if (b[i][j])
						return true;
		return false;
	}
}
