package chess;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import board.Board;
import board.Piece;
import board.Position;
import chess.pieces.Bishop;
import chess.pieces.King;
import chess.pieces.Knight;
import chess.pieces.Pawn;
import chess.pieces.Queen;
import chess.pieces.Rook;

public class ChessMatch {

	private Board board;
	private int turn;
	private Color currentPlayer;
	private List<Piece> piecesOnTheBoard = new ArrayList<>();
	private List<Piece> capturedPieces = new ArrayList<>();
	private boolean check;
	private boolean checkMate;
	private ChessPiece enPassantVulnerable;
	private ChessPiece promoted;

	public ChessMatch() {
		board = new Board(8, 8);
		turn = 1;
		currentPlayer = Color.WHITE;
		check = false;
		checkMate = false;
		enPassantVulnerable = null;
		promoted = null;
		initialSetup();
	}

	public int getTurn() {
		return turn;
	}

	public Color getCurrentPlayer() {
		return currentPlayer;
	}

	public boolean getCheck() {
		return check;
	}

	public boolean getCheckMate() {
		return checkMate;
	}

	public ChessPiece getEnPassantVulnerable() {
		return enPassantVulnerable;
	}
	
	public ChessPiece getPromoted() {
		return promoted;
	}

	public ChessPiece[][] getPieces() {
		ChessPiece[][] p = new ChessPiece[board.getRows()][board.getColumns()];

		for (int i = 0; i < board.getRows(); i++)
			for (int j = 0; j < board.getColumns(); j++)
				p[i][j] = (ChessPiece) board.piece(i, j);

		return p;
	}

	public boolean[][] possibleMoves(ChessPosition source) {
		Position p = source.toPosition();
		validateSourcePosition(p);
		return board.piece(p).possibleMoves();
	}

	public ChessPiece performChessMove(ChessPosition sourceChessPosition, ChessPosition targetChessPosition) {
		Position source = sourceChessPosition.toPosition();
		Position target = targetChessPosition.toPosition();
		validateSourcePosition(source);
		validateTargetPosition(source, target);
		Piece captured = makeMove(source, target);

		if (testCheck(currentPlayer)) {
			undoMove(source, target, captured);
			throw new ChessException("You can't put yourself in check");
		}

		ChessPiece movedPiece = (ChessPiece) board.piece(target);

		check = testCheck(opponent(currentPlayer));
		
		// promotion
		promoted = null;
		if (movedPiece instanceof Pawn)
			if ((movedPiece.getColor() == Color.WHITE && target.getRow() == 0) || (movedPiece.getColor() == Color.BLACK && target.getRow() == 7)) {
				promoted = (ChessPiece)board.piece(target);
				promoted = replacePromotedPiece("Q");
			}
		
		if (testCheckMate(opponent(currentPlayer)))
			checkMate = true;
		else
			nextTurn();

		// en passant
		if (movedPiece instanceof Pawn && source.getRow() - 2 == target.getRow()
				|| source.getRow() + 2 == target.getRow())
			enPassantVulnerable = movedPiece;
		else
			enPassantVulnerable = null;

		return (ChessPiece) captured;
	}

	private void validateSourcePosition(Position position) {
		if (!board.thereIsAPiece(position))
			throw new ChessException("There is no piece on source position");

		if (currentPlayer != ((ChessPiece) board.piece(position)).getColor())
			throw new ChessException("The chosen piece is not yours");

		if (!board.piece(position).isThereAnyPossibleMove())
			throw new ChessException("There are no possible moves for the chosen piece");
	}

	private void validateTargetPosition(Position source, Position target) {
		if (!board.piece(source).possibleMove(target))
			throw new ChessException("The chosen piece can't move to target position");

	}

	private void nextTurn() {
		turn++;
		currentPlayer = currentPlayer == Color.WHITE ? Color.BLACK : Color.WHITE;
	}
	
	public ChessPiece replacePromotedPiece(String type) {
		if (promoted == null)
			throw new IllegalStateException("There is no piece to be promoted");
		if (!type.equals("B") && !type.equals("N") && !type.equals("Q") && !type.equals("R"))
			return promoted;
		
		Position pos = promoted.getChessPosition().toPosition();
		Piece p = board.removePiece(pos);
		piecesOnTheBoard.remove(p);
		
		ChessPiece newPiece = newPiece(type, promoted.getColor());
		board.placePiece(newPiece, pos);
		piecesOnTheBoard.add(newPiece);
		return newPiece;
	}
	
	private ChessPiece newPiece(String type, Color color) {
		if (type.equals("B")) return new Bishop(board, color);
		if (type.equals("N")) return new Knight(board, color);
		if (type.equals("Q")) return new Queen(board, color);
		return new Rook(board, color);
	}

	private Piece makeMove(Position source, Position target) {
		ChessPiece p = (ChessPiece) board.removePiece(source);
		p.increaseMoveCount();
		Piece captured = board.removePiece(target);
		board.placePiece(p, target);
		
		if (captured != null) {
			piecesOnTheBoard.remove(captured);
			capturedPieces.add(captured);
		}

		// kingside castling
		if (p instanceof King && source.getColumn() + 2 == target.getColumn()) {
			Position rookSource = new Position(source.getRow(), source.getColumn() + 3);
			Position rookTarget = new Position(source.getRow(), source.getColumn() + 1);
			ChessPiece rook = (ChessPiece) board.removePiece(rookSource);
			board.placePiece(rook, rookTarget);
			rook.increaseMoveCount();
		}

		// queenside castling
		if (p instanceof King && source.getColumn() - 2 == target.getColumn()) {
			Position rookSource = new Position(source.getRow(), source.getColumn() - 4);
			Position rookTarget = new Position(source.getRow(), source.getColumn() - 1);
			ChessPiece rook = (ChessPiece) board.removePiece(rookSource);
			board.placePiece(rook, rookTarget);
			rook.increaseMoveCount();
		}
		
		// en passant
		if (p instanceof Pawn) {
			if (source.getColumn() != target.getColumn() && captured == null) {
				Position pawnPos;
				if (p.getColor() == Color.WHITE)
					pawnPos = new Position(target.getRow() + 1, target.getColumn());
				else
					pawnPos = new Position(target.getRow() - 1, target.getColumn());
				
				captured = board.removePiece(pawnPos);
				capturedPieces.add(captured);
				piecesOnTheBoard.remove(captured);
			}
		}

		return captured;
	}

	private void undoMove(Position source, Position target, Piece captured) {
		ChessPiece p = (ChessPiece) board.removePiece(target);
		p.decreaseMoveCount();
		board.placePiece(p, source);

		if (captured != null) {
			board.placePiece(captured, target);
			capturedPieces.remove(captured);
			piecesOnTheBoard.add(captured);
		}

		// kingside castling
		if (p instanceof King && source.getColumn() + 2 == target.getColumn()) {
			Position rookSource = new Position(source.getRow(), source.getColumn() + 3);
			Position rookTarget = new Position(source.getRow(), source.getColumn() + 1);
			ChessPiece rook = (ChessPiece) board.removePiece(rookTarget);
			board.placePiece(rook, rookSource);
			rook.decreaseMoveCount();
		}

		// queenside castling
		if (p instanceof King && source.getColumn() - 2 == target.getColumn()) {
			Position rookSource = new Position(source.getRow(), source.getColumn() - 4);
			Position rookTarget = new Position(source.getRow(), source.getColumn() - 1);
			ChessPiece rook = (ChessPiece) board.removePiece(rookTarget);
			board.placePiece(rook, rookSource);
			rook.decreaseMoveCount();
		}

		// en passant
		if (p instanceof Pawn) {
			if (source.getColumn() != target.getColumn() && captured == enPassantVulnerable) {
				ChessPiece pawn = (ChessPiece)board.removePiece(target);
				Position pawnPos;
				if (p.getColor() == Color.WHITE)
					pawnPos = new Position(3, target.getColumn());
				else
					pawnPos = new Position(4, target.getColumn());
				
				board.placePiece(pawn, pawnPos);
				captured = board.removePiece(pawnPos);
			}
		}
	}

	private Color opponent(Color color) {
		return color == Color.WHITE ? Color.BLACK : Color.WHITE;
	}

	private ChessPiece king(Color color) {
		List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece) x).getColor() == color)
				.collect(Collectors.toList());

		for (Piece p : list)
			if (p instanceof King)
				return (ChessPiece) p;

		throw new IllegalStateException("There is no " + color + " king on the board");
	}

	private boolean testCheck(Color color) {
		Position kingPosition = king(color).getChessPosition().toPosition();
		List<Piece> opponentPieces = piecesOnTheBoard.stream()
				.filter(x -> ((ChessPiece) x).getColor() == opponent(color)).collect(Collectors.toList());
		for (Piece p : opponentPieces) {
			boolean[][] m = p.possibleMoves();
			if (m[kingPosition.getRow()][kingPosition.getColumn()])
				return true;
		}
		return false;
	}

	private boolean testCheckMate(Color color) {
		if (!testCheck(color))
			return false;

		List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece) x).getColor() == color)
				.collect(Collectors.toList());
		for (Piece p : list) {
			boolean[][] m = p.possibleMoves();
			for (int i = 0; i < board.getRows(); i++)
				for (int j = 0; j < board.getColumns(); j++)
					if (m[i][j]) {
						Position source = ((ChessPiece) p).getChessPosition().toPosition();
						Position target = new Position(i, j);
						Piece captured = makeMove(source, target);
						boolean testCheck = testCheck(color);
						undoMove(source, target, captured);
						if (!testCheck)
							return false;
					}

		}
		return true;
	}

	private void placeNewPiece(char column, int row, ChessPiece piece) {
		board.placePiece(piece, new ChessPosition(column, row).toPosition());
		piecesOnTheBoard.add(piece);
	}

	private void initialSetup() {
		placeNewPiece('a', 1, new Rook(board, Color.WHITE));
		placeNewPiece('b', 1, new Knight(board, Color.WHITE));
		placeNewPiece('c', 1, new Bishop(board, Color.WHITE));
		placeNewPiece('d', 1, new Queen(board, Color.WHITE));
		placeNewPiece('e', 1, new King(board, Color.WHITE, this));
		placeNewPiece('f', 1, new Bishop(board, Color.WHITE));
		placeNewPiece('g', 1, new Knight(board, Color.WHITE));
		placeNewPiece('h', 1, new Rook(board, Color.WHITE));
		placeNewPiece('a', 2, new Pawn(board, Color.WHITE, this));
		placeNewPiece('b', 2, new Pawn(board, Color.WHITE, this));
		placeNewPiece('c', 2, new Pawn(board, Color.WHITE, this));
		placeNewPiece('d', 2, new Pawn(board, Color.WHITE, this));
		placeNewPiece('e', 2, new Pawn(board, Color.WHITE, this));
		placeNewPiece('f', 2, new Pawn(board, Color.WHITE, this));
		placeNewPiece('g', 2, new Pawn(board, Color.WHITE, this));
		placeNewPiece('h', 2, new Pawn(board, Color.WHITE, this));

		placeNewPiece('a', 8, new Rook(board, Color.BLACK));
		placeNewPiece('b', 8, new Knight(board, Color.BLACK));
		placeNewPiece('c', 8, new Bishop(board, Color.BLACK));
		placeNewPiece('d', 8, new Queen(board, Color.BLACK));
		placeNewPiece('e', 8, new King(board, Color.BLACK, this));
		placeNewPiece('f', 8, new Bishop(board, Color.BLACK));
		placeNewPiece('g', 8, new Knight(board, Color.BLACK));
		placeNewPiece('h', 8, new Rook(board, Color.BLACK));
		placeNewPiece('a', 7, new Pawn(board, Color.BLACK, this));
		placeNewPiece('b', 7, new Pawn(board, Color.BLACK, this));
		placeNewPiece('c', 7, new Pawn(board, Color.BLACK, this));
		placeNewPiece('d', 7, new Pawn(board, Color.BLACK, this));
		placeNewPiece('e', 7, new Pawn(board, Color.BLACK, this));
		placeNewPiece('f', 7, new Pawn(board, Color.BLACK, this));
		placeNewPiece('g', 7, new Pawn(board, Color.BLACK, this));
		placeNewPiece('h', 7, new Pawn(board, Color.BLACK, this));

	}

}
