package singularity.world.blocks.drills;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.game.Team;
import mindustry.gen.Sounds;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.StatValues;
import singularity.world.blocks.SglBlock;
import singularity.world.draw.DrawDrill;
import universecore.world.consumers.BaseConsumers;

import java.util.Arrays;

/**基本的钻头，将钻头的材料消耗更改为使用重定义的consume，可在混合矿物地面选择一种进行开采*/
public class BaseDrill extends SglBlock{
  /**钻头硬度，决定钻头可以开采的矿物，只有硬度大于矿物的硬度才可以开采*/
  public int bitHardness = 0;
  /**基准采集时间，直接决定钻头的开采速度*/
  public float drillTime = 300f;
  /**钻头预热速度，决定钻头提升到最大效率的速度*/
  public float warmupSpeed = 0.02f;
  /**转子的旋转速度倍数*/
  public float rotatorSpeed = 2f;

  public float hardMultiple = 50;
  
  /**轮缘(_rim后缀的贴图)的着色*/
  public Color heatColor = Color.valueOf("ff5512");
  public Color liquidColor = Color.valueOf("ff5512");
  
  /**每次采掘时触发的特效*/
  public Effect drillEffect = Fx.mine;
  /**钻头运行时产生的特效*/
  public Effect updateEffect = Fx.pulverizeSmall;
  /**钻头产生更新特效的频率*/
  public float updateEffectChance = 0.02f;
  
  public final Seq<ItemStack> ores = new Seq<>();
  public final Seq<Item> oresIndex = new Seq<>();
  
  /**基本的钻头，将钻头的材料消耗更改为使用重定义的consume，可在混合矿物地面选择一种进行开采*/
  public BaseDrill(String name){
    super(name);
    update = true;
    solid = true;
    sync = true;
    hasItems = true;
    configurable = true;
    saveConfig = false;
    oneOfOptionCons = true;
    group = BlockGroup.drills;
    ambientSound = Sounds.drill;
    draw = new DrawDrill<>(this);
    ambientSoundVolume = 0.018f;
  }
  
  @Override
  public void appliedConfig(){
    super.appliedConfig();
    config(boolean[].class, (BaseDrillBuild entity, boolean[] b) -> entity.currentMines = b);
    configClear((BaseDrillBuild e) -> e.currentMines = new boolean[e.currentMines.length]);
  }

  public BaseConsumers newBooster(float increase){
    return newOptionalConsume(
      (entity, cons) -> {
        ((BaseDrillBuild)entity).efficiencyIncrease = increase;
        ((BaseDrillBuild)entity).boostTime = cons.craftTime;
      },
      (stats, cons) -> {
        stats.add(Stat.boostEffect, increase * increase, StatUnit.timesSpeed);
      }
    );
  }
  
  @Override
  public void setStats(){
    super.setStats();
    stats.add(Stat.drillTier, StatValues.blocks(b -> b instanceof Floor && (b.itemDrop != null && b.itemDrop.hardness <= bitHardness)));
    stats.add(Stat.drillSpeed, 60f / drillTime * size * size, StatUnit.itemsSecond);
  }
  
  @Override
  public boolean canPlaceOn(Tile tile, Team team){
    if(isMultiblock()){
      boolean re = false;
      for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
        re |= canMine(other);
      }
    return re;
    }
    else{
      return canMine(tile);
    }
  }
  
  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid){
    Tile tile = Vars.world.tile(x, y);
    if(tile == null) return;

    getMines(tile, false);

    if(ores.size > 0){
      int line = 0;
      for(ItemStack stack: ores){
        float width = stack.item.hardness <= bitHardness?
          //可挖掘的矿物显示
          drawPlaceText(Core.bundle.formatFloat("bar.drillspeed", 60f / (drillTime + stack.item.hardness*hardMultiple) * stack.amount, 2), x, y - line, true):
          //不可挖掘的矿物显示
          drawPlaceText(Core.bundle.get("bar.drilltierreq"), x, y - line, false);
        float dx = x * Vars.tilesize + offset - width/2f - 4f, dy = y * Vars.tilesize + offset + size * Vars.tilesize / 2f + 5 - line*8f;
        Draw.mixcol(Color.darkGray, 1f);
        Draw.rect(stack.item.uiIcon, dx, dy - 1);
        Draw.reset();
        Draw.rect(stack.item.uiIcon, dx, dy);
        line++;
      }
    }
  }
  
  public Seq<ItemStack> getMines(Tile tile){
    return getMines(tile, true);
  }
  
  public Seq<ItemStack> getMines(Tile tile, boolean filtration){
    ores.clear();
    oresIndex.clear();
    if(isMultiblock()){
      for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
        if(filtration? canMine(other): hasMine(other)){
          int i = oresIndex.indexOf(other.drop());
          if(i == -1){
            ores.add(new ItemStack(other.drop(), 1));
            oresIndex.add(other.drop());
          }
          else{
            ores.get(i).amount++;
          }
        }
      }
    }
    else{
      if(filtration? canMine(tile): hasMine(tile)) ores.add(new ItemStack(tile.drop(), 1));
    }
    return ores;
  }
  
  public boolean canMine(Tile tile){
    return hasMine(tile) && tile.drop().hardness <= bitHardness;
  }
  
  public boolean hasMine(Tile tile){
    if(tile == null) return false;
    return tile.drop() != null;
  }
  
  public class BaseDrillBuild extends SglBuilding{
    public float[] progress;
    
    public float consumeProgress = 0f;
    public float warmup = 0f;
    
    public boolean[] currentMines;
    
    public float rotatorAngle = 0f;
    
    public float efficiencyIncrease = 1;
    public float boostTime = 0f;
    
    public float[] lastDrillSpeed;
    
    public Seq<ItemStack> outputItems;
    
    @Override
    public BaseDrill block(){
      return (BaseDrill) block;
    }
    
    @Override
    public void onProximityUpdate(){
      //Log.info("onProximityUpdate");
      super.onProximityUpdate();
      outputItems = getMines(tile).copy();
      if(currentMines == null || currentMines.length != outputItems.size) currentMines = new boolean[outputItems.size];
      progress = new float[outputItems.size];
      lastDrillSpeed = new float[outputItems.size];
      if(outputItems.size == 1) currentMines[0] = true;
      //Log.info("onProximityUpdate, data:[outputItems:" + outputItems.toString() + "]");
    }
    
    @Override
    public void displayBars(Table bars){
      super.displayBars(bars);
      for(int i=0; i<outputItems.size; i++){
        if(!currentMines[i]) continue;
        int finalI = i;
        bars.add(new Bar(
            () -> outputItems.get(finalI).item.localizedName + " : " + Core.bundle.format("bar.drillspeed", Strings.fixed(lastDrillSpeed[finalI] * 60 * timeScale(), 2)),
            () -> Pal.ammo,
            () -> warmup
        )).growX();
        bars.row();
      }
    }
  
    @Override
    public Seq<Item> outputItems() {
      return outputItems.map(e -> e.item);
    }
    
    @Override
    public boolean shouldConsume(){
      return super.shouldConsume() && !isFull() && miningAny();
    }
    
    public boolean miningAny(){
      for(boolean bool: currentMines){
        if(bool) return true;
      }
      return false;
    }
  
    @Override
    public void buildConfiguration(Table table){
      super.buildConfiguration(table);
      if(outputItems.size > 1){
        Table mines = new Table(Tex.buttonTrans);
        mines.defaults().grow().marginTop(0).marginBottom(0).marginRight(5).marginRight(5);
        mines.add(Core.bundle.get("fragment.buttons.selectMine")).padLeft(5).padTop(5).padBottom(5);
        mines.row();
        Table buttons = new Table();
        for(int i=0; i<outputItems.size; i++){
          int f = i;
          ItemStack stack = outputItems.get(i);
          ImageButton button = new ImageButton(stack.item.uiIcon, Styles.selecti);
          button.clicked(() -> {
            currentMines[f] = !currentMines[f];
            configure(currentMines);
          });
          button.update(() -> button.setChecked(currentMines[f]));
          buttons.add(button).size(50, 50);
          if((i+1) % 4 == 0) buttons.row();
        }
        mines.add(buttons);
        table.add(mines);
        table.row();
      }
    }
    
    @Override
    public void updateTile(){
      super.updateTile();
      boolean isSelect = false;
      for(boolean b: currentMines) isSelect |= b;
      if(!isSelect) return;
      
      float speed = 0;
      //Log.info("updating,data:[consValid:" + consValid() +",currentMines:" + Arrays.toString(currentMines) + "]");
      if(updateValid()){
        speed = efficiency() * efficiencyIncrease;
        warmup = Mathf.lerpDelta(warmup, speed, warmupSpeed);
        if(consumer != null && consumer.optionalCurr != null) consumeProgress += getProgressIncrease(consumer.optionalCurr.craftTime);
        if(Mathf.chanceDelta(updateEffectChance)){
          updateEffect.at(getX() + Mathf.range(size * 4f), getY() + Mathf.range(size * 4));
        }
      }
      else{
        Arrays.fill(lastDrillSpeed, 0f);
        warmup = Mathf.lerpDelta(warmup, 0, warmupSpeed*1.5f);
      }
      
      for(int index=0; index<outputItems.size; index++){
        if(!currentMines[index]) continue;
        ItemStack ore = outputItems.get(index);
    
        if(updateValid()){
          progress[index] += edelta() * ore.amount * efficiencyIncrease * warmup;
          lastDrillSpeed[index] = (speed * ore.amount * warmup) / (drillTime + ore.item.hardness*hardMultiple);
        }
        
        float delay = drillTime + ore.item.hardness*hardMultiple;
        
        if(progress[index] >= delay){
          items.add(ore.item, 1);
          progress[index] = 0f;
          drillEffect.at(getX() + Mathf.range(size), getY() + Mathf.range(size), outputItems.get(index).item.color);
        }
        
        dump(outputItems.get(index).item);
      }
      
      rotatorAngle += warmup * delta();
      
      if(consumeProgress >= 1){
        consumer.trigger();
        consumeProgress = 0;
      }
      
      if(boostTime > 0) boostTime -= 1f;
      if(boostTime <= 0) efficiencyIncrease = 1f;
    }
    
    public boolean isFull(){
      int totalMine = 0;
      for(ItemStack stack: outputItems) totalMine += items.get(stack.item);
      return totalMine >= block().itemCapacity;
    }
    
    @Override
    public boolean updateValid(){
      return consValid() && !isFull();
    }
  
    @Override
    public void write(Writes write) {
      super.write(write);
      write.f(consumeProgress);
      write.f(warmup);
      write.i(outputItems.size);
      
      for(int i=0; i<outputItems.size; i++){
        write.bool(currentMines[i]);
        write.f(progress[i]);
      }
    }
  
    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      consumeProgress = read.f();
      warmup = read.f();
      int length = read.i();
      currentMines = new boolean[length];
      progress = new float[length];
      for(int i=0; i<length; i++){
        currentMines[i] = read.bool();
        progress[i] = read.f();
      }
    }
  }
}
