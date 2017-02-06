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

    private Label boardWidthLabel, boardHeightLabel, minesLabel;
    private Slider boardWidthSlider, boardHeightSlider, minesSlider;
    private TextButton playButton;

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

        boardWidthSlider = new Slider(9, 30, 1, false, skin);
        boardWidthSlider.setName("width");
        boardHeightSlider = new Slider(9, 24, 1, false, skin);
        boardHeightSlider.setName("height");
        minesSlider = new Slider(10, getMaxMines(9, 9), 1, false, skin);
        minesSlider.setValue(10);
        minesSlider.setName("mines");
        playButton = new TextButton("Play", skin);
        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game,
                        (int) boardWidthSlider.getValue(),
                        (int) boardHeightSlider.getValue(),
                        (int) minesSlider.getValue()));
            }
        });
        boardWidthLabel = new Label("Width: " + (int) boardWidthSlider.getValue(), skin);
        boardHeightLabel = new Label("Height: " + (int) boardHeightSlider.getValue(), skin);
        minesLabel = new Label("Mines: " + (int) minesSlider.getValue(), skin);

        table.add(boardWidthLabel);
        table.add(boardWidthSlider).width(400);
        table.row();
        table.add(boardHeightLabel);
        table.add(boardHeightSlider).width(400);
        table.row();
        table.add(minesLabel);
        table.add(minesSlider).width(400);
        table.row();
        table.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Update labels for sliders.
                boardWidthLabel.setText("Width: " + (int) boardWidthSlider.getValue());
                boardHeightLabel.setText("Height: " + (int) boardHeightSlider.getValue());
                minesLabel.setText("Mines: " + (int) minesSlider.getValue());
                // Set the max value for the mines slider.
                float maxMines = getMaxMines((int) (boardWidthSlider.getValue()), (int) (boardHeightSlider.getValue()));
                minesSlider.setRange(10, maxMines);
            }
        });
        table.add(playButton).colspan(2);

        table.setFillParent(true);
        stage.addActor(table);

        //stage.setDebugAll(true);

    }

    /**
     * Get the maximum number of mines based on the boardWidth and boardHeight.
     */
    private int getMaxMines(int boardWidth, int boardHeight) {
        double n = (Math.ceil(Math.sqrt(boardHeight * boardWidth)) - 1);
        return (int) MathUtils.clamp(n * n, 64, 667);
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
