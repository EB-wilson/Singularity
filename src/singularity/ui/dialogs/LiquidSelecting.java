package singularity.ui.dialogs;

import singularity.Singularity;
import singularity.type.SglLiquidStack;
import singularity.world.blocks.product.NormalCrafter.NormalCrafterBuild;
import arc.Core;
import arc.math.Mathf;
import arc.scene.ui.Image;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.Liquid;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import universeCore.util.UncLiquidStack;

public class LiquidSelecting extends ImageButton{
  int selectDirection = 0;

  public LiquidSelecting(NormalCrafterBuild entity, UncLiquidStack[] outputLiquids){
    super(Icon.settings, Styles.clearTransi);
    clicked(() -> showDialog(entity, outputLiquids));
  }

  @SuppressWarnings("CodeBlock2Expr")
  private void showDialog(NormalCrafterBuild entity, UncLiquidStack[] outputLiquids){
    BaseDialog dialog = new BaseDialog(Core.bundle.get("dialog.liquidSelecting.title"));
    String[] direction = {"arrow_above","arrow_left","arrow_down","arrow_right"};
    int[] index = {0, 1, 3, 2};

    Table table = new Table();
    Table selectBoard = new Table(Tex.button);
    Table liquidBoard = new Table(Tex.button);

    Image blank = new Image(Singularity.getModAtlas("transparent"));

    table.add(Core.bundle.get("dialog.liquidSelecting.text")).padLeft(10);
    table.row();
    table.add(Core.bundle.get("dialog.liquidSelecting.output") + ":").padLeft(10);
    table.row();
    
    for(int dire = 0; dire < 4; dire++){
      if(dire == 0 || dire == 3) selectBoard.add(blank).size(80, 80);
      int direct = index[dire];
      ImageButton button = new ImageButton(Singularity.getModAtlas("" + direction[direct]), Styles.clearToggleTransi);
      ImageButton reset;
      button.clicked(() -> {
        selectDirection = direct;
      });
      button.resizeImage(40);
      button.update(() -> button.setChecked(selectDirection == direct));
      selectBoard.add(button);
      if(dire == 0 || dire == 3) selectBoard.add(blank).size(80, 80);
      if(dire == 0 || dire == 2) selectBoard.row();
      if(dire == 1){
        reset = new ImageButton(Singularity.getModAtlas("reset"), Styles.clearToggleTransi);
        int finalDire = dire;
        reset.clicked(() -> {
          entity.selectLiquid[finalDire] = null;
        });
        reset.resizeImage(40);
        reset.update(() -> reset.setChecked(false));
        selectBoard.add(reset);
      }
    }
    table.add(selectBoard).size(240f, 240f).padLeft(20);
    table.row();
    for(UncLiquidStack stack: outputLiquids){
      Liquid liquid = stack.liquid;
      ImageButton button = new ImageButton(liquid.uiIcon, Styles.clearToggleTransi);
      button.clicked(() -> entity.selectLiquid[Mathf.mod(selectDirection + 1, 4)] = liquid);
      button.resizeImage(40);
      button.update(() -> {
        button.setChecked(entity.selectLiquid[Mathf.mod(selectDirection + 1, 4)] == liquid);
      });
      liquidBoard.add(button).size(60, 60);
    }
    table.add(liquidBoard).size(240f, 80f).padLeft(20);
    table.row();
    table.button(Core.bundle.get("misc.exit"), dialog::hide).size(100, 60);

    dialog.cont.clear();
    dialog.cont.add(table);
    dialog.show();
  }
}
