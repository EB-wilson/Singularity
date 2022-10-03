package singularity.world.blocks.drills;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Time;
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
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.StatValues;
import singularity.world.blocks.SglBlock;
import universecore.world.consumers.BaseConsumers;

import static mindustry.Vars.indexer;
import static mindustry.Vars.state;

/**基本的钻头，将钻头的材料消耗更改为使用重定义的consume，可在混合矿物地面选择一种进行开采*/
public class BaseDrill extends SglBlock{
  /**钻头硬度，决定钻头可以开采的矿物，只有硬度大于矿物的硬度才可以开采*/
  public int bitHardness = 0;
  /**基准采集时间，直接决定钻头的开采速度*/
  public float drillTime = 300f;
  /**钻头预热速度，决定钻头提升到最大效率的速度*/
  public float warmupSpeed = 0.02f;
  /**转子的旋转速度倍数*/
  public float rotationSpeed = 2f;
  public float maxRotationSpeed = 3f;

  public float hardMultiple = 50;
  
  /**每次采掘时触发的特效*/
  public Effect drillEffect = Fx.mine;
  /**钻头运行时产生的特效*/
  public Effect updateEffect = Fx.pulverizeSmall;
  /**钻头产生更新特效的频率*/
  public float updateEffectChance = 0.02f;
  
  public final Seq<ItemStack> ores = new Seq<>();
  
  /**基本的钻头，将钻头的材料消耗更改为使用重定义的consume，可在混合矿物地面选择一种进行开采*/
  public BaseDrill(String name){
    super(name);
    update = true;
    solid = true;
    sync = true;
    hasItems = true;
    outputItems = true;
    configurable = true;
    saveConfig = false;
    oneOfOptionCons = true;
    group = BlockGroup.drills;
    ambientSound = Sounds.drill;
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
        stats.add(Stat.boostEffect, increase, StatUnit.timesSpeed);
      }
    );
  }

  @Override
  public void setStats(){
    super.setStats();
    stats.add(Stat.drillTier, StatValues.blocks(b -> b instanceof Floor f && !f.wallOre && f.itemDrop != null &&
        f.itemDrop.hardness <= bitHardness && (indexer.isBlockPresent(f) || state.isMenu())));
    stats.add(Stat.drillSpeed, 60f / drillTime * size * size, StatUnit.itemsSecond);
  }
  
  @Override
  public boolean canPlaceOn(Tile tile, Team team, int rotate){
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

    getMines(tile, this, false, ores);

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

  public Seq<ItemStack> getMines(Tile tile, Block block){
    return getMines(tile, block, new Seq<>());
  }
  
  public Seq<ItemStack> getMines(Tile tile, Block block, Seq<ItemStack> seq){
    return getMines(tile, block, true, seq);
  }
  
  public Seq<ItemStack> getMines(Tile tile, Block block, boolean filtration, Seq<ItemStack> seq){
    seq.clear();
    if(isMultiblock()){
      for(Tile other : tile.getLinkedTilesAs(block, tempTiles)){
        if(filtration? canMine(other): hasMine(other)){
          boolean mark = false;
          for(ItemStack ores: seq){
            if(ores.item == other.drop()){
              ores.amount++;
              mark = true;
              break;
            }
          }
          if(!mark) seq.add(new ItemStack(other.drop(), 1));
        }
      }
    }
    else{
      if(filtration? canMine(tile): hasMine(tile)) seq.add(new ItemStack(tile.drop(), 1));
    }
    return seq;
  }
  
  public boolean canMine(Tile tile){
    return hasMine(tile) && tile.drop().hardness <= bitHardness;
  }
  
  public boolean hasMine(Tile tile){
    if(tile == null) return false;
    return tile.drop() != null;
  }

  @SuppressWarnings("ZeroLengthArrayAllocation")
  public class BaseDrillBuild extends SglBuilding{
    public float[] progress = {};
    
    public float consumeProgress = 0f;
    public float warmup = 0f;
    
    public boolean[] currentMines = {};
    
    public float rotatorAngle = 0f;
    
    public float efficiencyIncrease = 1;
    public float boostTime = 0f;
    
    public float[] lastDrillSpeed = {};
    
    public Seq<ItemStack> outputItems = new Seq<>();
    public ObjectSet<Item> mineOreItems = new ObjectSet<>();
    public float speed;

    @Override
    public BaseDrill block(){
      return (BaseDrill) block;
    }

    @Override
    public float warmup(){
      return warmup;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();
      getMines(tile, block, outputItems);
      boolean reset = mineOreItems.size != outputItems.size;
      if(!reset){
        for(ItemStack stack: outputItems){
          if(!mineOreItems.contains(stack.item)){
            reset = true;
            break;
          }
        }
      }
      if(reset){
        mineOreItems.clear();
        for(ItemStack stack: outputItems){
          mineOreItems.add(stack.item);
        }
        currentMines = new boolean[outputItems.size];
        progress = new float[outputItems.size];
        lastDrillSpeed = new float[outputItems.size];
        if(outputItems.size == 1) currentMines[0] = true;
      }
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
      if(updateValid()){
        speed = Mathf.lerpDelta(speed, efficiencyIncrease*consEfficiency(), warmupSpeed);
        warmup = Mathf.lerpDelta(warmup, 1, warmupSpeed);
        consumeProgress += consumer.consDelta()*warmup;
        if(Mathf.chanceDelta(updateEffectChance*warmup)){
          updateEffect.at(getX() + Mathf.range(size * 4f), getY() + Mathf.range(size * 4));
        }
      }
      else{
        speed = Mathf.lerpDelta(speed, 0, warmupSpeed*1.5f);
        warmup = Mathf.lerpDelta(warmup, 0, warmupSpeed*1.5f);
      }
      
      for(int index=0; index<outputItems.size; index++){
        if(!currentMines[index]) continue;
        ItemStack ore = outputItems.get(index);

        progress[index] += consumer.consDelta()*ore.amount*warmup*speed;
        lastDrillSpeed[index] = (ore.amount*warmup*speed)/(drillTime + ore.item.hardness*hardMultiple);

        float delay = drillTime + ore.item.hardness*hardMultiple;

        if(progress[index] >= delay){
          int i;
          items.add(ore.item, Math.min(i = (int)(progress[index]/delay), itemCapacity - items.total()));
          progress[index] -= i*delay;
          drillEffect.at(getX() + Mathf.range(size), getY() + Mathf.range(size), outputItems.get(index).item.color);
        }
        
        dump(outputItems.get(index).item);
      }
      
      rotatorAngle += Math.min(maxRotationSpeed, speed*warmup*Time.delta*rotationSpeed);
      
      if(consumeProgress >= 1){
        consumer.trigger();
        consumeProgress = 0;
      }
      
      if(boostTime > 0) boostTime -= 1f;
      if(boostTime <= 0) efficiencyIncrease = 1f;
    }

    public boolean isFull(){
      return items.total() >= block().itemCapacity;
    }
    
    @Override
    public boolean updateValid(){
      return consumeValid() && !isFull() && miningAny();
    }
  
    @Override
    public void write(Writes write) {
      super.write(write);
      write.f(consumeProgress);
      write.f(warmup);
      write.f(speed);

      write.i(outputItems.size);
      for(int i=0; i<outputItems.size; i++){
        write.i(outputItems.get(i).item.id);
        write.bool(currentMines[i]);
        write.f(progress[i]);
      }
    }
  
    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      consumeProgress = read.f();
      warmup = read.f();
      speed = read.f();

      int length = read.i();
      currentMines = new boolean[length];
      progress = new float[length];
      lastDrillSpeed = new float[length];
      mineOreItems.clear();
      for(int i=0; i<length; i++){
        mineOreItems.add(Vars.content.item(read.i()));
        currentMines[i] = read.bool();
        progress[i] = read.f();
      }
    }
  }
}
