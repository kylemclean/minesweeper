package ga.kylemclean.minesweeper.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class MenuScreen implements Screen {

    private ga.kylemclean.minesweeper.Minesweeper game;

    private Stage stage;
    private Skin skin;
    private Table table;

    private Label widthNameLabel, heightNameLabel, minesNameLabel;
    private Label widthValueLabel, heightValueLabel, minesValueLabel;
    private Slider boardWidthSlider, boardHeightSlider, minesSlider;
    private TextButton playButton;

    private final int MIN_BOARD_WIDTH = 9;
    private final int MAX_BOARD_WIDTH = 30;
    private final int MIN_BOARD_HEIGHT = 9;
    private final int MAX_BOARD_HEIGHT = 16;
    private final int MIN_MINES = 10;
    private final int MAX_MINES = 667; // Hard limit, actual limit calculated in getMaxMines()

    public MenuScreen(ga.kylemclean.minesweeper.Minesweeper game) {
        this.game = game;

        setupUi();
        Gdx.input.setInputProcessor(stage);
    }

    /**
     * Setup the UI elements.
     */
    private void setupUi() {
        stage = new Stage();
        skin = game.assets.get("ui/uiskin.json", Skin.class);
        table = new Table(skin);

        boardWidthSlider = new Slider(MIN_BOARD_WIDTH, MAX_BOARD_WIDTH, 1, false, skin);
        boardHeightSlider = new Slider(MIN_BOARD_HEIGHT, MAX_BOARD_HEIGHT, 1, false, skin);
        minesSlider = new Slider(
                MIN_MINES, getMaxMines(MIN_BOARD_WIDTH, MIN_BOARD_HEIGHT), 1, false, skin);
        playButton = new TextButton("Play", skin);
        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                startGame(
                        (int) boardWidthSlider.getValue(),
                        (int) boardHeightSlider.getValue(),
                        (int) minesSlider.getValue());
            }
        });
        widthNameLabel = new Label("Width: ", skin);
        heightNameLabel = new Label("Height: ", skin);
        minesNameLabel = new Label("Mines: ", skin);
        widthValueLabel = new Label("" + (int) boardWidthSlider.getValue(), skin);
        heightValueLabel = new Label("" + (int) boardHeightSlider.getValue(), skin);
        minesValueLabel = new Label("" + (int) minesSlider.getValue(), skin);

        table.add(widthNameLabel).left();
        table.add(widthValueLabel);
        table.add(boardWidthSlider).width(400);
        table.row();
        table.add(heightNameLabel).left();
        table.add(heightValueLabel);
        table.add(boardHeightSlider).width(400);
        table.row();
        table.add(minesNameLabel).left();
        table.add(minesValueLabel).width(80);
        table.add(minesSlider).width(400);
        table.row();
        table.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Update labels for sliders.
                widthValueLabel.setText("" + (int) boardWidthSlider.getValue());
                heightValueLabel.setText("" + (int) boardHeightSlider.getValue());
                minesValueLabel.setText("" + (int) minesSlider.getValue());
                // Set the max value for the mines slider.
                float maxMines = getMaxMines((int) (boardWidthSlider.getValue()), (int) (boardHeightSlider.getValue()));
                minesSlider.setRange(10, maxMines);
            }
        });
        table.add(playButton).colspan(3);

        table.setFillParent(true);
        stage.addActor(table);
    }

    /**
     * Get the maximum number of mines based on the boardWidth and boardHeight.
     * @param boardWidth The width of the board in cells.
     * @param boardHeight The height of the board in cells.
     */
    private int getMaxMines(int boardWidth, int boardHeight) {
        double n = (Math.ceil(Math.sqrt(boardHeight * boardWidth)) - 1);
        return (int) MathUtils.clamp(n * n, 64, MAX_MINES);
    }

    /**
     * Start a game with the specified properties.
     * @param boardWidth The width of the board in cells.
     * @param boardHeight The height of the board in cells.
     * @param mines The number of mines on the board to be generated.
     */
    private void startGame(int boardWidth, int boardHeight, int mines) {
        dispose();
        game.setScreen(new GameScreen(game, boardWidth, boardHeight, mines));
    }

    @Override
    public void render(float delta) {
        Gdx.gl20.glClearColor(0, 0, 0, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void show() {

    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().apply();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
