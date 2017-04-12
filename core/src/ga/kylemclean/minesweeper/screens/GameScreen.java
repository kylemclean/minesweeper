package ga.kylemclean.minesweeper.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;

import ga.kylemclean.minesweeper.Minesweeper;
import ga.kylemclean.minesweeper.game.Cell;

public class GameScreen implements Screen, InputProcessor {

    private enum GameState {
        NOT_STARTED, PLAYING, PAUSED, WON, LOST
    }

    private Minesweeper game;
    private SpriteBatch batch;
    private OrthographicCamera gameCamera, fixedCamera;
    private ShapeRenderer shapeRenderer;

    private TextureAtlas cellTextures, uiTextures;
    private BitmapFont font;

    private Vector3 gameCameraTargetPosition;
    private float gameCameraTargetZoom;

    private Vector3 touchPos, screenTouchDownPos;
    private boolean panningCamera;
    private float defaultZoom;

    private int cellSize = 40;
    private int boardHeight;
    private int boardWidth;
    private Rectangle boardWorldRectangle, zoomRectangle;
    private int mines;
    private Cell[][] board;
    private Vector2 pressingCell;
    private Vector2 chordingCell;
    private int cellsFlagged;
    private int cellsOpened;

    private GameState gameState;
    private GameState gameStateBeforePause;
    private float gameTime;

    private GlyphLayout minesLayout, timeLayout;
    private Vector2 minesDisplayPosition, timeDisplayPosition;

    /**
     * Initialize the GameScreen.
     *
     * @param game        A reference to the Game object.
     * @param boardWidth  The width of the board in cells.
     * @param boardHeight The height of the board in cells.
     * @param mines       The number of mines to be generated on the board.
     */
    public GameScreen(Minesweeper game, int boardWidth, int boardHeight, int mines) {
        this.game = game;
        batch = this.game.batch;
        gameCamera = this.game.gameCamera;
        fixedCamera = this.game.fixedCamera;
        shapeRenderer = this.game.shapeRenderer;

        // Get assets from game AssetManager.
        cellTextures = game.assets.get("textures/cells/pack.atlas", TextureAtlas.class);
        uiTextures = game.assets.get("textures/ui/pack.atlas", TextureAtlas.class);
        font = game.assets.get("ui/arial-32.fnt", BitmapFont.class);

        gameCameraTargetPosition = new Vector3();

        Gdx.input.setInputProcessor(this);
        touchPos = new Vector3();
        screenTouchDownPos = new Vector3();

        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.mines = mines;

        board = new Cell[this.boardWidth][this.boardHeight];
        createCells(this.boardWidth, this.boardHeight);
        pressingCell = new Vector2(-1, -1);
        chordingCell = new Vector2(-1, -1);
        cellsFlagged = 0;
        cellsOpened = 0;

        boardWorldRectangle = new Rectangle(0, 0, boardWidth * cellSize, boardHeight * cellSize);
        zoomRectangle = new Rectangle(0, 0, boardWidth * cellSize, boardHeight * cellSize);
        // Set rectangle to 16:9 aspect ratio.
        if (zoomRectangle.getAspectRatio() >= (16F / 9F)) {
            zoomRectangle.height = (9F / 16F) * zoomRectangle.width;
        } else {
            zoomRectangle.width = (16F / 9F) * zoomRectangle.height;
        }
        // Pad rectangle.
        zoomRectangle.width += 16 * 9;
        zoomRectangle.height += 9 * 4;
        // Center rectangle on board.
        zoomRectangle.x = -((zoomRectangle.width - (boardWidth * cellSize / 2)) / 2);
        zoomRectangle.y = -((zoomRectangle.height - (boardHeight * cellSize / 2)) / 2);
        gameCameraTargetPosition.set(boardWidth * cellSize / 2, boardHeight * cellSize / 2, 0);
        // Have the camera snap to the target position at first
        gameCamera.position.set(gameCameraTargetPosition.cpy());
        //fixedCamera.position.set(gameCamera.position.cpy());
        // Zoom in at first
        gameCamera.zoom = 0.5f;
        gameCameraTargetZoom = zoomRectangle.width / 1280;
        defaultZoom = gameCameraTargetZoom;

        gameState = GameState.NOT_STARTED;
        gameTime = 0;

        minesLayout = new GlyphLayout();
        timeLayout = new GlyphLayout();
        minesDisplayPosition = new Vector2(24, 720 - 24);
        timeDisplayPosition = new Vector2(1280 - 24, 720 - 24);
    }

    /**
     * Create the cell objects.
     *
     * @param boardWidth  The width of the board.
     * @param boardHeight The height of the board.
     */
    private void createCells(int boardWidth, int boardHeight) {
        for (int y = 0; y < boardHeight; y++) {
            for (int x = 0; x < boardWidth; x++) {
                board[x][y] = new Cell(
                        cellTextures.findRegion("cell_normal_up"));
            }
        }
    }

    /**
     * Fills the board with mines.
     * Mines will not be generated in a 3x3 space around the cell the user clicked.
     *
     * @param amount   The amount of mines to create on the board.
     * @param initialX The x position on the board of the first cell the user clicked.
     * @param initialY The y position on the board of the first cell the user clicked.
     */
    private void generateMines(int amount, int initialX, int initialY) {
        for (int m = 0; m < amount; ) {
            int randX = MathUtils.random(boardWidth - 1);
            int randY = MathUtils.random(boardHeight - 1);
            if (!board[randX][randY].isMine && !(
                    (randX >= initialX - 1 && randX <= initialX + 1) &&
                            (randY >= initialY - 1 && randY <= initialY + 1))) {
                // Set as a mine as long as it isn't already a mine and it is
                // not in a 3x3 space around the cell the user clicked.
                board[randX][randY].isMine = true;
                m++;
            }
        }
    }

    /**
     * Generates the labels for each cell based on their surrounding mines.
     */
    private void generateCellLabels() {
        for (int y = 0; y < boardHeight; y++) {
            for (int x = 0; x < boardWidth; x++) {
                if (!board[x][y].isMine) {
                    int surroundingMines = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (x + dx >= 0 && y + dy >= 0 &&
                                    x + dx < boardWidth && y + dy < boardHeight) {
                                if (board[x + dx][y + dy].isMine) {
                                    surroundingMines++;
                                }
                            }
                        }
                    }
                    board[x][y].surroundingMines = surroundingMines;
                }
            }
        }
    }

    /**
     * Open the cell at the specified location. If the cell has no surrounding mines,
     * open all surrounding cells as well.
     *
     * @param x The x-coordinate of the cell to open.
     * @param y The y-coordinate of the cell to open.
     */
    private void openCell(int x, int y) {
        if (!board[x][y].opened && !board[x][y].flagged) {
            // If the cell is not a mine or flagged
            if (board[x][y].open()) {
                if (board[x][y].surroundingMines > 0) {
                    board[x][y].texture = cellTextures
                            .findRegion("cell" + board[x][y].surroundingMines);
                } else {
                    // There are no surrounding mines
                    board[x][y].texture = cellTextures
                            .findRegion("cell_empty");
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (x + dx >= 0 && y + dy >= 0 &&
                                    x + dx < boardWidth && y + dy < boardHeight) {
                                openCell(x + dx, y + dy);
                            }
                        }
                    }
                }
                cellsOpened++;
                if (cellsOpened == boardWidth * boardHeight - mines) {
                    winGame();
                }
            } else {
                board[x][y].texture = cellTextures.findRegion("cell_mine");
                loseGame();
            }
        }
    }

    /**
     * Flag or unflag the cell at the specified location.
     *
     * @param x The x-coordinate of the cell to (un)flag.
     * @param y The y-coordinate of the cell to (un)flag.
     */
    private void toggleFlagCell(int x, int y) {
        if (!board[x][y].opened) {
            if (!board[x][y].flagged) {
                board[x][y].flagged = true;
                board[x][y].texture = cellTextures.findRegion("cell_flag_up");
                cellsFlagged++;
            } else {
                board[x][y].flagged = false;
                board[x][y].texture = cellTextures.findRegion("cell_normal_up");
                cellsFlagged--;
            }
        }
    }

    /**
     * Chord (open all cells in 3x3 box around) a given cell.
     *
     * @param cellX The x-coordinate of the cell to chord.
     * @param cellY The y-coordinate of the cell to chord.
     */
    private void chordCell(int cellX, int cellY) {
        int surroundingFlags = 0;
        int surroundingMines = board[cellX][cellY].surroundingMines;
        for (int dy = -1; dy < 2; dy++) {
            for (int dx = -1; dx < 2; dx++) {
                if (!(dx == 0 && dy == 0)) { // Don't check the chording cell
                    // Make sure the cell we are checking is on the board
                    if (cellX + dx >= 0 && cellY + dy >= 0 &&
                        cellX + dx < boardWidth && cellY + dy < boardHeight) {
                        if (board[cellX + dx][cellY + dy].flagged) {
                            surroundingFlags++;
                        }
                    }
                }
            }
        }
        
        // If there are the right amount of flags, open the surrounding cells
        if (surroundingFlags == surroundingMines) {
            for (int dy = -1; dy < 2; dy++) {
                for (int dx = -1; dx < 2; dx++) {
                    if (!(dx == 0 && dy == 0)) { // Don't check the chording cell
                        // Make sure the cell we are checking is on the board
                        if (cellX + dx >= 0 && cellY + dy >= 0 &&
                                cellX + dx < boardWidth && cellY + dy < boardHeight) {
                            if (!board[cellX + dx][cellY + dy].flagged) {
                                openCell(cellX + dx, cellY + dy);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Win the game.
     */
    private void winGame() {
        gameState = GameState.WON;
    }

    /**
     * Lose the game.
     */
    private void loseGame() {
        gameState = GameState.LOST;
        // Show all mines on the board
        for (int y = 0; y < boardHeight; y++) {
            for (int x = 0; x < boardWidth; x++) {
                if (board[x][y].isMine && !board[x][y].flagged) {
                    board[x][y].texture = cellTextures.findRegion("cell_mine");
                }
                if (!board[x][y].isMine && board[x][y].flagged) {
                    board[x][y].texture = cellTextures.findRegion("cell_flag_wrong");
                }
            }
        }
    }

    /**
     * Reset the game.
     */
    private void resetGame() {
        createCells(boardWidth, boardHeight);
        cellsFlagged = 0;
        cellsOpened = 0;
        gameState = GameState.NOT_STARTED;
        gameTime = 0;
    }

    @Override
    public void render(float delta) {
        Gdx.gl20.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (gameState == GameState.PLAYING) {
            gameTime += delta;
        }

        interpolateCamera(delta);

        gameCamera.update();
        fixedCamera.update();

        batch.setProjectionMatrix(gameCamera.combined);
        batch.begin();
        for (int y = 0; y < boardHeight; y++) {
            for (int x = 0; x < boardWidth; x++) {
                batch.draw(board[x][y].texture, x * cellSize, y * cellSize, cellSize, cellSize);
            }
        }
        batch.end();

        // Draw rectangles behind the mines counter and timer
        Gdx.gl20.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(fixedCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.75f);
        shapeRenderer.rect(minesDisplayPosition.x - 8, minesDisplayPosition.y + 8,
                minesLayout.width + 8 * 2, -minesLayout.height - 8 * 2);
        shapeRenderer.rect(timeDisplayPosition.x + 8, timeDisplayPosition.y + 8,
                -timeLayout.width - 8 * 2, -timeLayout.height - 8 * 2);
        shapeRenderer.end();
        Gdx.gl20.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(fixedCamera.combined);
        batch.begin();
        // Draw mines remaining
        minesLayout.setText(font, ((mines - cellsFlagged) < 100 ? "0" : "") +
                ((mines - cellsFlagged) < 10 ? "0" : "") + (mines - cellsFlagged));
        font.draw(batch, minesLayout,
                minesDisplayPosition.x, minesDisplayPosition.y);
        // Draw time elapsed
        timeLayout.setText(font, (int) gameTime / 60 + ":" + ((int) gameTime % 60 < 10 ? "0" : "") + (int) gameTime % 60);
        font.draw(batch, timeLayout,
        /* right aligned */timeDisplayPosition.x - timeLayout.width, timeDisplayPosition.y);


        // Draw title if game is over
        if (gameState == GameState.WON || gameState == GameState.LOST) {
            if (gameState == GameState.WON) {
                batch.draw(uiTextures.findRegion("win"),
                        1280 / 2 - uiTextures.findRegion("win").originalWidth / 2,
                        720 / 2 - uiTextures.findRegion("win").originalHeight / 2);
            } else if (gameState == GameState.LOST) {
                batch.draw(uiTextures.findRegion("lose"),
                        1280 / 2 - uiTextures.findRegion("lose").originalWidth / 2,
                        720 / 2 - uiTextures.findRegion("lose").originalHeight / 2);
            }
            for (int i = 0; i < 2; i++) {
                if (i == 0) {
                    font.setColor(Color.BLACK);
                } else {
                    font.setColor(Color.WHITE);
                }
                font.draw(
                        batch, "Press SPACE to play again\nPress ESC to change settings",
                        1280 / 2 + i * -2, 120 + i * 2, 0, Align.center, false);
            }
        }

        batch.end();
    }

    /**
     * Interpolate the camera's position and zoom to a target position and zoom.
     * (gameCameraTargetPosition and gameCameraTargetZoom)
     *
     * @param delta The change in time in seconds since the last frame.
     */
    private void interpolateCamera(float delta) {
        gameCamera.position.x += (gameCameraTargetPosition.x - gameCamera.position.x) * 10 * delta;
        gameCamera.position.y += (gameCameraTargetPosition.y - gameCamera.position.y) * 10 * delta;
        gameCamera.zoom += (gameCameraTargetZoom - gameCamera.zoom) * 10 * delta;
    }

    /**
     * Quit the game and return the player to the menu.
     */
    private void returnToMenu() {
        dispose();
        game.setScreen(new MenuScreen(game));
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        touchPos.set(screenX, screenY, 0);
        screenTouchDownPos = touchPos.cpy();
        gameCamera.unproject(touchPos);

        if (gameState == GameState.PLAYING || gameState == GameState.NOT_STARTED) {
            if (boardWorldRectangle.contains(touchPos.x, touchPos.y)) {
                int cellX = (int) touchPos.x / cellSize;
                int cellY = (int) touchPos.y / cellSize;

                // Make sure that the cell coordinates are on the board
                if ((cellX >= 0 && cellX <= boardWidth) && (cellY >= 0 && cellY <= boardHeight)) {

                    if (!board[cellX][cellY].opened) {
                        // Cell is not yet open
                        pressingCell.set(cellX, cellY);
                        if (!board[cellX][cellY].flagged) {
                            board[cellX][cellY].texture = cellTextures.findRegion("cell_normal_down");
                        } else {
                            board[cellX][cellY].texture = cellTextures.findRegion("cell_flag_down");
                        }
                    } else {
                        // Cell is already open
                        chordingCell.set(cellX, cellY);
                    }
                }

                return true;
            }
        }
        return false;

    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        boolean returnTrue = false;

        // Set touchPos to world coordinates of touchUp position
        touchPos.set(screenX, screenY, 0);
        gameCamera.unproject(touchPos);

        if (gameState == GameState.PLAYING || gameState == GameState.NOT_STARTED) {
            // cellX and cellY represent the boards coordinates of the touched cell
            int cellX = (int) touchPos.x / cellSize;
            int cellY = (int) touchPos.y / cellSize;

            // Make sure the cell position is on the board
            if ((cellX >= 0 && cellX < boardWidth) && (cellY >= 0 && cellY < boardHeight)) {

                // Pressing cell logic
                if (pressingCell.x != -1 && pressingCell.y != -1) {
                    if (cellX == pressingCell.x && cellY == pressingCell.y && !panningCamera) {
                        if (button == 0) {
                            if (gameState == GameState.NOT_STARTED) {
                                generateMines(this.mines, cellX, cellY);
                                generateCellLabels();
                                gameState = GameState.PLAYING;
                            }
                            if (!board[cellX][cellY].flagged) {
                                openCell(cellX, cellY);
                            } else {
                                board[cellX][cellY].texture = cellTextures.findRegion("cell_flag_up");
                            }
                        } else if (button == 1) {
                            toggleFlagCell(cellX, cellY);
                        }
                        returnTrue = true;
                    } else {
                        // Dragged off the cell
                        if (pressingCell.x != -1 && pressingCell.y != -1) {
                            board[(int) pressingCell.x][(int) pressingCell.y].texture =
                                    !board[(int) pressingCell.x][(int) pressingCell.y].flagged ?
                                            cellTextures.findRegion("cell_normal_up") :
                                            cellTextures.findRegion("cell_flag_up");
                        }
                    }
                }

                // Chording cell logic
                if (chordingCell.x != -1 && chordingCell.y != -1) {
                    if (cellX == chordingCell.x && cellY == chordingCell.y && !panningCamera) {
                        chordCell(cellX, cellY);
                    } else {
                        // Dragged off cell
                    }
                }

            }

        }
        pressingCell.set(-1, -1);
        chordingCell.set(-1, -1);
        panningCamera = false;
        return returnTrue;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (gameCameraTargetZoom < defaultZoom - 0.1f && (
                Math.abs(screenTouchDownPos.x - screenX) >= 20 ||
                        Math.abs(screenTouchDownPos.y - screenY) >= 20 || panningCamera)) {
            panningCamera = true;
            gameCamera.translate((screenTouchDownPos.x - screenX) / 4f,
                    -(screenTouchDownPos.y - screenY) / 4f);
            gameCameraTargetPosition.set(gameCamera.position.cpy());
            gameCameraTargetPosition.x = MathUtils.clamp(gameCameraTargetPosition.x,
                    boardWorldRectangle.x, boardWorldRectangle.x + boardWorldRectangle.width);
            gameCameraTargetPosition.y = MathUtils.clamp(gameCameraTargetPosition.y,
                    boardWorldRectangle.y, boardWorldRectangle.y + boardWorldRectangle.height);
            screenTouchDownPos.set(screenX, screenY, 0);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.F2 ||
                (gameState == GameState.LOST || gameState == GameState.WON)
                        && keycode == Input.Keys.SPACE) {
            resetGame();
        }
        if (keycode == Input.Keys.ESCAPE) {
            returnToMenu();
        }
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        gameCameraTargetZoom += amount / 20F;
        gameCameraTargetZoom = MathUtils.clamp(gameCameraTargetZoom, 0.2f, defaultZoom);
        if (gameCameraTargetZoom >= defaultZoom - 0.1f) {
            gameCameraTargetPosition.set(boardWidth * cellSize / 2, boardHeight * cellSize / 2, 0);
        }
        return true;
    }

    @Override
    public void show() {

    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {
        gameStateBeforePause = gameState;
        gameState = GameState.PAUSED;
    }

    @Override
    public void resume() {
        gameState = gameStateBeforePause;
    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }

}
