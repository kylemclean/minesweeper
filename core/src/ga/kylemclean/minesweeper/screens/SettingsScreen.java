package ga.kylemclean.minesweeper.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import ga.kylemclean.minesweeper.Minesweeper;

public class SettingsScreen implements Screen {

    private Minesweeper game;

    private Stage stage;
    private Skin skin;
    private Table table;

    private TextButton closeButton;

    // TODO checkbox to enable question marks
    public SettingsScreen(Minesweeper game) {
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

        table.add(new Label("No settings yet.", skin));
        table.row();

        closeButton = new TextButton("Close", skin);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

            }
        });
        table.add(closeButton);
        table.setFillParent(true);
        stage.addActor(table);
        stage.setDebugAll(false);
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
