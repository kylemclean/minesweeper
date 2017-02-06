package ga.kylemclean.minesweeper.game;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Cell {

    public TextureRegion texture;
    public boolean isMine;
    public boolean opened;
    public boolean flagged;
    public int surroundingMines;

    public Cell(TextureRegion texture) {
        this.texture = texture;
    }

    /**
     * Opens the cell.
     * @return true if the cell is not a mine or flagged.
     */
    public boolean open() {
        opened = true;

        return !isMine && !flagged;
    }

}
