package ga.kylemclean.minesweeper.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
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
    private OrthographicCamera camera;

    private TextureAtlas textures;
    private BitmapFont font;

    private Vector3 cameraTargetPosition;
    private float cameraTargetZoom;

    private Vector3 touchPos, screenTouchDownPos;
    private boolean panningCamera;
    private float defaultZoom;

    private int cellSize = 40;
    private int boardHeight;
    private int boardWidth;
    private Rectangle boardRectangle, zoomRectangle;
    private int mines;
    private Cell[][] board;
    private Vector2 pressingCell;
    private int cellsFlagged;
    private int cellsOpened;

    private GameState gameState;
    private GameState gameStateBeforePause;
    private float gameTime;

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
        camera = this.game.camera;

        textures = game.assets.get("textures/pack.atlas", TextureAtlas.class);
        font = game.assets.get("ui/arial-32.fnt", BitmapFont.class);

        cameraTargetPosition = new Vector3();

        Gdx.input.setInputProcessor(this);
        touchPos = new Vector3();
        screenTouchDownPos = new Vector3();

        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.mines = mines;

        board = new Cell[this.boardHeight][this.boardWidth];
        createCells(this.boardWidth, this.boardHeight);
        pressingCell = new Vector2(-1, -1);
        cellsFlagged = 0;
        cellsOpened = 0;

        boardRectangle = new Rectangle(0, 0, boardWidth * cellSize, boardHeight * cellSize);
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
        cameraTargetPosition.set(boardWidth * cellSize / 2, boardHeight * cellSize / 2, 0);
        cameraTargetZoom = zoomRectangle.width / 1280;
        defaultZoom = cameraTargetZoom;

        gameState = GameState.NOT_STARTED;
        gameTime = 0;
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
                board[y][x] = new Cell(
                        textures.findRegion("cell_normal_up"));
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
            int randY = MathUtils.random(boardHeight - 1);
            int randX = MathUtils.random(boardWidth - 1);
            if (!board[randY][randX].isMine && !(
                    (randX >= initialX - 1 && randX <= initialX + 1) &&
                            (randY >= initialY - 1 && randY <= initialY + 1))) {
                // Set as a mine as long as it isn't already a mine and it is
                // not in a 3x3 space around the cell the user clicked.
                board[randY][randX].isMine = true;
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
                if (!board[y][x].isMine) {
                    int surroundingMines = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (x + dx >= 0 && y + dy >= 0 &&
                                    x + dx < boardWidth && y + dy < boardHeight) {
                                if (board[y + dy][x + dx].isMine) {
                                    surroundingMines++;
                                }
                            }
                        }
                    }
                    board[y][x].surroundingMines = surroundingMines;
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
        if (!board[y][x].opened && !board[y][x].flagged) {
            // If the cell is not a mine or flagged
            if (board[y][x].open()) {
                if (board[y][x].surroundingMines > 0) {
                    board[y][x].texture = textures
                            .findRegion("cell" + board[y][x].surroundingMines);
                } else {
                    // No surrounding mines
                    board[y][x].texture = textures
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
                board[y][x].texture = textures.findRegion("cell_mine");
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
        if (!board[y][x].opened) {
            if (!board[y][x].flagged) {
                board[y][x].flagged = true;
                board[y][x].texture = textures.findRegion("cell_flag_up");
                cellsFlagged++;
            } else {
                board[y][x].flagged = false;
                board[y][x].texture = textures.findRegion("cell_normal_up");
                cellsFlagged--;
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
        Gdx.gl20.glClearColor(0, 0, 0, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (gameState == GameState.PLAYING) {
            gameTime += delta;
        }

        interpolateCamera(delta);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (int y = 0; y < boardHeight; y++) {
            for (int x = 0; x < boardWidth; x++) {
                batch.draw(board[y][x].texture, x * cellSize, y * cellSize, cellSize, cellSize);
            }
        }
        // Draw mines remaining
        font.draw(batch,
                ((mines - cellsFlagged) < 100 ? "0" : "") +
                        ((mines - cellsFlagged) < 10 ? "0" : "") + (mines - cellsFlagged),
                boardRectangle.x - 8, boardRectangle.y + boardRectangle.height,
                0, Align.right, false);
        // Draw time elapsed
        font.draw(batch,
                (int) gameTime / 60 + ":" + ((int) gameTime % 60 < 10 ? "0" : "") + (int) gameTime % 60,
                boardRectangle.x + boardRectangle.width + 8,
                boardRectangle.y + boardRectangle.height);
        // Draw title if game is over
        if (gameState == GameState.WON || gameState == GameState.LOST) {
            if (gameState == GameState.WON) {
                batch.draw(textures.findRegion("win"),
                        boardRectangle.width / 2 - textures.findRegion("win").originalWidth / 2,
                        boardRectangle.height / 2 - textures.findRegion("win").originalHeight / 2);
            } else if (gameState == GameState.LOST) {
                batch.draw(textures.findRegion("lose"),
                        boardRectangle.width / 2 - textures.findRegion("lose").originalWidth / 2,
                        boardRectangle.height / 2 - textures.findRegion("lose").originalHeight / 2);
            }
            font.draw(
                    batch, "Press SPACE to play again",
                    boardRectangle.width / 2, 120, 0, Align.center, false);
        }
        batch.end();
    }

    /**
     * Interpolate the camera's position and zoom to a target position and zoom.
     * (cameraTargetPosition and cameraTargetZoom)
     *
     * @param delta The change in time in seconds since the last frame.
     */
    private void interpolateCamera(float delta) {
        camera.position.x += (cameraTargetPosition.x - camera.position.x) * 10 * delta;
        camera.position.y += (cameraTargetPosition.y - camera.position.y) * 10 * delta;
        camera.zoom += (cameraTargetZoom - camera.zoom) * 10 * delta;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        touchPos.set(screenX, screenY, 0);
        screenTouchDownPos = touchPos.cpy();
        camera.unproject(touchPos);

        if (gameState == GameState.PLAYING || gameState == GameState.NOT_STARTED) {
            if (boardRectangle.contains(touchPos.x, touchPos.y)) {
                int cellX = (int) touchPos.x / cellSize;
                int cellY = (int) touchPos.y / cellSize;

                if (!board[cellY][cellX].opened) {
                    pressingCell.set(cellX, cellY);
                    if (!board[cellY][cellX].flagged) {
                        board[cellY][cellX].texture = textures.findRegion("cell_normal_down");
                    } else {
                        board[cellY][cellX].texture = textures.findRegion("cell_flag_down");
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
        touchPos.set(screenX, screenY, 0);
        camera.unproject(touchPos);

        if (gameState == GameState.PLAYING || gameState == GameState.NOT_STARTED) {
            int cellX = (int) touchPos.x / cellSize;
            int cellY = (int) touchPos.y / cellSize;
            if (cellX == pressingCell.x && cellY == pressingCell.y && !panningCamera) {
                if (button == 0) {
                    if (gameState == GameState.NOT_STARTED) {
                        generateMines(this.mines, cellX, cellY);
                        generateCellLabels();
                        gameState = GameState.PLAYING;
                    }
                    if (!board[cellY][cellX].flagged) {
                        openCell(cellX, cellY);
                    } else {
                        board[cellY][cellX].texture = textures.findRegion("cell_flag_up");
                    }
                } else if (button == 1) {
                    toggleFlagCell(cellX, cellY);
                }
                returnTrue = true;
            } else {
                if (pressingCell.x != -1 && pressingCell.y != -1) {
                    board[(int) pressingCell.y][(int) pressingCell.x].texture =
                            !board[(int) pressingCell.y][(int) pressingCell.x].flagged ?
                                    textures.findRegion("cell_normal_up") :
                                    textures.findRegion("cell_flag_up");
                }
            }
        }
        pressingCell.set(-1, -1);
        panningCamera = false;
        return returnTrue;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (cameraTargetZoom < defaultZoom - 0.1f && (
                Math.abs(screenTouchDownPos.x - screenX) >= 20 ||
                        Math.abs(screenTouchDownPos.y - screenY) >= 20 || panningCamera)) {
            panningCamera = true;
            camera.translate((screenTouchDownPos.x - screenX) / 4f,
                    -(screenTouchDownPos.y - screenY) / 4f);
            cameraTargetPosition.set(camera.position.cpy());
            cameraTargetPosition.x = MathUtils.clamp(cameraTargetPosition.x, boardRectangle.x, boardRectangle.x + boardRectangle.width);
            cameraTargetPosition.y = MathUtils.clamp(cameraTargetPosition.y, boardRectangle.y, boardRectangle.y + boardRectangle.height);
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
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        cameraTargetZoom += amount / 20F;
        cameraTargetZoom = MathUtils.clamp(cameraTargetZoom, 0.2f, defaultZoom);
        if (cameraTargetZoom >= defaultZoom - 0.1f) {
            cameraTargetPosition.set(boardWidth * cellSize / 2, boardHeight * cellSize / 2, 0);
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
