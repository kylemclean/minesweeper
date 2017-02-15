package ga.kylemclean.minesweeper;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import ga.kylemclean.minesweeper.screens.MenuScreen;

public class Minesweeper extends Game {

	public AssetManager assets;

	public SpriteBatch batch;
	public OrthographicCamera gameCamera, fixedCamera;
	public ScreenViewport viewport;
	public ShapeRenderer shapeRenderer;

	private boolean finishedLoadingAssets;

	@Override
	public void create() {
		// Create asset manager and load assets.
		assets = new AssetManager();
		loadAssets();
		assets.finishLoading();
		onFinishLoadingAssets();

		// Initialize SpriteBatch, ScreenViewport, and OrthographicCamera.
		batch = new SpriteBatch();
		gameCamera = new OrthographicCamera();
		gameCamera.setToOrtho(false, 1280, 720);
		fixedCamera = new OrthographicCamera();
		fixedCamera.setToOrtho(false, 1280, 720);
		viewport = new ScreenViewport(gameCamera);
		// Initialize ShapeRenderer
		shapeRenderer = new ShapeRenderer();
		setScreen(new MenuScreen(this));
	}

	@Override
	public void render() {
		super.render();

		assets.update();
	}

	/**
	 * Loads all game assets.
	 */
	private void loadAssets() {
		assets.load("textures/cells/pack.atlas", TextureAtlas.class);
		assets.load("textures/ui/pack.atlas", TextureAtlas.class);
		assets.load("ui/uiskin.json", Skin.class);
		assets.load("ui/arial-32.fnt", BitmapFont.class);
	}

	/**
	 * Starts game. Called once when the AssetManager finishes loading assets
	 * for the first time.
	 */
	private void onFinishLoadingAssets() {
		finishedLoadingAssets = true;

		Gdx.app.log("AssetManager", "finished loading assets");
	}

	@Override
	public void pause() {
		super.pause();
	}

	@Override
	public void resume() {
		super.resume();
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
	}

	@Override
	public void dispose() {
		super.dispose();
		batch.dispose();
		shapeRenderer.dispose();
		assets.dispose();
	}
}
