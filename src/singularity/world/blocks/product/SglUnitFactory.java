package singularity.world.blocks.product;

import arc.Core;
import arc.func.Cons2;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.util.Scaling;
import arc.util.Strings;
import arc.util.Structs;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.mod.Mods;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.PayloadStack;
import mindustry.type.UnitType;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.meta.Stat;
import singularity.Sgl;
import singularity.core.ModsInteropAPI;
import singularity.graphic.SglDrawConst;
import singularity.world.components.distnet.DistElementBlockComp;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.consumers.SglConsumers;
import singularity.world.distribution.DistBufferType;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.distribution.buffers.ItemsBuffer;
import singularity.world.distribution.request.DistRequestBase;
import singularity.world.meta.SglStat;
import singularity.world.modules.DistributeModule;
import singularity.world.products.Producers;
import universecore.annotations.Annotations;
import universecore.world.consumers.BaseConsumers;
import universecore.world.consumers.ConsumeType;
import universecore.world.producers.BaseProducers;
import universecore.world.producers.ProducePayload;
import universecore.world.producers.ProduceType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

import static mindustry.Vars.state;

@Annotations.ImplEntries
public class SglUnitFactory extends PayloadCrafter implements DistElementBlockComp {
  public enum ConfigCmd{
    CLEAR_TASK,
    ADD_TASK,
    SET_PRIORITY,
    QUEUE_MODE,
    SKIP_BLOCKED,
    ACTIVITY,
    REMOVE_TASK,
    RISE_TASK,
    DOWN_TASK,
    COMMAND
  }

  static ObjectMap<UnitType, UnitCostModel> costModels = new ObjectMap<>();

  static {
    /*添加mod交互式API模型，用于其他mod定义单位在单位建造机上的建造成本
    * 通常条目的格式：
    * ...
    * "unitFactoryCosts": {
    *   "$unitTypeName": {//选中单位的内部名称，mod名称前缀可选，默认选择本mod中的content，一般不建议跨mod配置单位数据
    *     "requirements": {"$itemName": #, "$itemName": #, ...},//键值对式声明，键为物品名称，值为数量
    *     //或者 "requirements": ["$itemName", #, "$itemName", #, ...],//数组式声明，第一个元素为物品名称，第二个元素为数量，以此类推
    *     //亦或者 ": ["$itemName/#", "$itemName/#", ...],//字符串声明模式，将物品名称和数量用‘/’进行连接
    *     "minLevel": #, //需要的工厂制造等级
    *     "baseBuildTime": # //基础建造时间
    *   },
    *   "$unitTypeName": {
    *     "requirements": {"$itemName": #, ...},
    *     "minLevel": #,
    *     "baseBuildTime": #
    *   },
    *   ...
    * }
    * ...
    *
    * 若要禁用某个单位的建造（默认情况下会自动分配单位的建造成本，可禁用），格式如下:
    * ...
    * "unitFactoryCosts": {
    *   "$unitTypeName": {
    *     "disabled": true
    *   },
    *   ...
    * }
    * ...
    * */
    if (Sgl.config.interopAssignUnitCosts){
      Sgl.interopAPI.addModel(new ModsInteropAPI.ConfigModel("unitFactoryCosts") {
        @Override
        public void parse(Mods.LoadedMod mod, Jval declaring) {
          for (ObjectMap.Entry<String, Jval> entry : declaring.asObject()) {
            UnitType unit = ModsInteropAPI.selectContent(ContentType.unit, entry.key, mod, true);

            UnitCostModel model = new UnitCostModel();

            if (entry.value.getBool("disabled", false)){
              model.disabled = true;
              costModels.put(unit, model);
              continue;
            }

            Jval.JsonArray arr = entry.value.get("requirements").asArray();

            model.requirements = new ItemStack[arr.size];
            for (int i = 0; i < arr.size; i++) {
              Jval stack = arr.get(i);
              if (stack.isObject()){
                Jval.JsonMap ent = stack.asObject();
                model.requirements[i] = new ItemStack(
                    ModsInteropAPI.selectContent(ContentType.item, ent.firstKey(), mod),
                    ent.firstValue().asInt()
                );
              }
              else if (stack.isArray()){
                Jval.JsonArray ar = stack.asArray();
                model.requirements[i] = new ItemStack(
                    ModsInteropAPI.selectContent(ContentType.item, ar.get(0).asString(), mod),
                    ar.get(1).asInt()
                );
              }
              else{
                String[] str = stack.asString().split("/");
                model.requirements[i] = new ItemStack(
                    ModsInteropAPI.selectContent(ContentType.item, str[0], mod),
                    Integer.parseInt(str[1])
                );
              }
            }
            model.baseBuildTime = entry.value.getFloat("baseBuildTime", 0);
            model.minLevel = entry.value.getInt("minLevel", 0);

            costModels.put(unit, model);
          }
        }

        @Override
        public void disable(Mods.LoadedMod mod) {
          for (UnitType unit : Vars.content.units()) {
            if (unit.minfo.mod == mod){
              UnitCostModel model = new UnitCostModel();
              model.disabled = true;
              costModels.put(unit, model);
            }
          }
        }
      }, false);
    }
  }

  public int maxTasks = 16;

  public float sizeLimit;
  public float healthLimit;
  public float timeMultiplier = 20;
  public float baseTimeScl = 0.3f;
  public int machineLevel;

  /**是否只能通过矩阵网络分配建造材料*/
  public boolean matrixDistributeOnly = false;

  public Cons2<UnitType, SglConsumers> consCustom;
  public Cons2<UnitType, Producers> byProduct;

  public static void setCost(UnitType unit, ItemStack[] req, int level, float baseTime){
    costModels.put(unit, new UnitCostModel(){{
      this.requirements = req;
      this.minLevel = level;
      this.baseBuildTime = baseTime;
    }});
  }

  public SglUnitFactory(String name) {
    super(name);
    autoSelect = false;
    canSelect = false;
    hasItems = true;

    configurable = true;
  }

  @Override
  public void appliedConfig() {
    config(IntSeq.class, (SglUnitFactoryBuild u, IntSeq i) -> {
      int handle = i.get(0);

      switch (ConfigCmd.values()[handle]){
        case CLEAR_TASK -> u.clearTask();
        case ADD_TASK -> u.appendTask(Vars.content.unit(i.get(1)), i.get(2), i.get(3));
        case SET_PRIORITY -> u.priority(i.get(1));
        case QUEUE_MODE -> u.queueMode = i.get(1) > 0;
        case SKIP_BLOCKED -> u.skipBlockedTask = i.get(1) > 0;
        case ACTIVITY -> u.activity = i.get(1) > 0;
        case REMOVE_TASK -> u.removeTask(u.getTask(i.get(1)));
        case RISE_TASK -> u.riseTask(u.getTask(i.get(1)));
        case DOWN_TASK -> u.downTask(u.getTask(i.get(1)));
        case COMMAND -> {
          SglUnitFactoryBuild.BuildTask t = u.getTask(i.get(1));
          t.targetPos = new Vec2(i.get(2)/1000f, i.get(3)/1000f);

          if (Vars.world.buildWorld(t.targetPos.x, t.targetPos.y) == u) {
            t.targetPos = null;
          }

          t.command = UnitCommand.all.get(i.get(4));
        }
        default -> throw new IllegalArgumentException("unknown operate code: " + handle);
      }
    });
  }

  @Override
  public void setStats() {
    super.setStats();

    stats.remove(Stat.itemCapacity);
    stats.remove(SglStat.recipes);
    stats.remove(SglStat.autoSelect);
    stats.remove(SglStat.controllable);

    stats.add(SglStat.sizeLimit, sizeLimit);
    stats.add(SglStat.healthLimit, healthLimit);
    stats.add(SglStat.buildLevel, machineLevel);

    stats.add(SglStat.recipes, t -> {
      t.left().defaults().left().growX();
      if (matrixDistributeOnly){
        t.row();
        t.add(Core.bundle.get("infos.matrixDistOnly")).color(Color.darkGray);
      }

      Table table = new Table(SglDrawConst.grayUIAlpha);
      Collapser coll = new Collapser(table, true);

      table.left().defaults().left().pad(3).size(185, 60);

      int c = 0;
      for (BaseProducers producer : producers()) {
        if (producer.cons.selectable.get() == BaseConsumers.Visibility.hidden) continue;

        ProducePayload<?> pd = producer.get(ProduceType.payload);

        if (pd == null || pd.payloads.length != 1) continue;

        Button button = new Button(Styles.clearNonei){
          {
            left().defaults().left().padLeft(4);
            image(pd.payloads[0].item.fullIcon).scaling(Scaling.fit).size(50);
            add(pd.payloads[0].item.localizedName).growX();

            clicked(() -> {
              Vars.ui.content.show(pd.payloads[0].item);
            });

            setDisabled(() -> producer.cons.selectable.get() == BaseConsumers.Visibility.unusable);
          }

          @Override
          public void draw() {
            super.draw();

            if (producer.cons.selectable.get() == BaseConsumers.Visibility.unusable) {
              Draw.color(Pal.darkerGray);
              Draw.alpha(0.8f*parentAlpha);

              Fill.rect(x + width/2, y + height/2, width, height);

              Draw.color(Color.lightGray, parentAlpha);
              Icon.lock.draw(x + width/2, y + height/2, 28, 28);
            }
          }
        };

        table.add(button);

        c++;
        if (c % 4 == 0){
          c = 0;
          table.row();
        }
      }

      t.row();
      t.table(inf -> {
        inf.left().defaults().left();

        inf.add(Core.bundle.get("infos.buildableList") + ": ");
        inf.button(Icon.downOpen, Styles.emptyi, () -> coll.toggle(false)).update(i -> i.getStyle().imageUp = (!coll.isCollapsed() ? Icon.upOpen : Icon.downOpen)).size(8).padLeft(16f).expandX();
      });
      t.row();
      t.add(coll);
    });
  }

  @Override
  public void init() {
    setupProducts();
    super.init();
  }

  @Override
  public void setBars() {
    super.setBars();

    addBar("progress", (SglUnitFactoryBuild entity) -> new Bar(
        () -> Core.bundle.format("bar.numprogress", Strings.autoFixed(entity.progress()*100, 1)),
        () -> Pal.ammo,
        entity::progress)
    );
  }

  public void setupProducts(){
    for (UnitType unit : Vars.content.units()) {
      if (unit.hitSize < sizeLimit && unit.health < healthLimit){
        ItemStack[] req;
        float buildTime;
        int level = 1;

        UnitCostModel costModel = costModels.get(unit);
        if (costModel != null){
          if (costModel.disabled) continue;

          req = costModel.requirements;
          buildTime = costModel.baseBuildTime*timeMultiplier*(baseTimeScl + (1 - baseTimeScl)*Mathf.sqrt((unit.hitSize/sizeLimit)*(unit.health/healthLimit)));
          level = costModel.minLevel;
        }
        else {
          req = unit.getTotalRequirements();
          buildTime = unit.getBuildTime()*timeMultiplier*(baseTimeScl + (1 - baseTimeScl)*Mathf.sqrt((unit.hitSize/sizeLimit)*(unit.health/healthLimit)));

          UnitType[] tmp = new UnitType[]{unit};
          while (tmp[0] != null) {
            UnitType[][] lis = new UnitType[1][];
            if (Vars.content.blocks().contains(b -> b instanceof Reconstructor rec && (lis[0] = rec.upgrades.find(u -> u[1] == tmp[0])) != null)) {
              tmp[0] = lis[0][0];
            } else tmp[0] = null;

            level++;
          }
        }

        if (level <= machineLevel){
          if (req.length == 0) continue;

          newConsume();
          consume.items(req).displayLim = -1;
          consume.time(buildTime);

          consume.selectable = () ->
              (!state.rules.hiddenBuildItems.isEmpty() && Structs.contains(req, i -> state.rules.hiddenBuildItems.contains(i.item))) || !unit.supportsEnv(state.rules.env)? BaseConsumers.Visibility.hidden:
              unit.unlockedNow()? BaseConsumers.Visibility.usable:
              BaseConsumers.Visibility.unusable;

          if (consCustom != null) consCustom.get(unit, consume);
          newProduce();
          produce.add(new ProducePayload<>(PayloadStack.with(unit, 1), (SglUnitFactoryBuild b, UnlockableContent c) -> b.payloads().total() <= 0));
          if (byProduct != null) byProduct.get(unit, produce);
        }
      }
    }
  }

  @Override
  public int topologyUse() {
    return 1;
  }

  @Override
  public float matrixEnergyUse() {
    return 0.5f;
  }

  public static class UnitCostModel{
    ItemStack[] requirements;
    float baseBuildTime;
    int minLevel;
    boolean disabled;
  }

  @Annotations.ImplEntries
  public class SglUnitFactoryBuild extends PayloadCrafterBuild implements DistElementBuildComp {
    public boolean activity;
    public boolean queueMode;
    public boolean skipBlockedTask;

    private int buildCount;

    private int taskCount;
    private BuildTask taskQueueHead, taskQueueLast;
    private BuildTask currentTask;

    protected float lastOutputProgress;

    public int priority;
    public DistributeModule distributor;
    public ItemsBuffer itemsBuffer;
    public PullMaterialRequest pullItemsRequest;

    public BuildTask getCurrentTask(){
      return currentTask;
    }

    public int buildCount(){
      return buildCount;
    }

    public int taskCount(){
      return taskCount;
    }

    @Override
    public SglUnitFactory block() {
      return SglUnitFactory.this;
    }

    public BuildTask popTask(){
      BuildTask res = taskQueueHead;
      taskQueueHead = taskQueueHead.next;
      if (taskQueueHead != null){
        taskQueueHead.pre = null;
      }
      else taskQueueLast = null;

      taskCount--;

      queueUpdated();

      return res;
    }

    public void clearTask(){
      taskQueueLast = taskQueueHead = null;
      taskCount = 0;
      queueUpdated();
    }

    public void riseTask(BuildTask task) {
      if (task.pre == null) return;

      if (task.pre == taskQueueHead){
        BuildTask next = task.next;

        task.next = taskQueueHead;
        taskQueueHead.pre = task;

        taskQueueHead.next = next;
        if (next != null) next.pre = taskQueueHead;

        taskQueueHead = task;
        taskQueueHead.pre = null;
      }
      else {
        BuildTask pre = task.pre, pre1 = pre.pre, next = task.next;

        pre1.next = task;
        task.pre = pre1;

        pre.next = next;
        if (next != null) next.pre = pre;

        task.next = pre;
        pre.pre = task;
      }

      if (task == taskQueueLast){
        taskQueueLast = task.next;
      }

      queueUpdated();
    }

    public void downTask(BuildTask task) {
      if (task.next == null) return;

      if (task.next == taskQueueLast){
        BuildTask pre = task.pre;

        task.pre = taskQueueLast;
        taskQueueLast.next = task;

        if (pre != null) pre.next = taskQueueLast;
        taskQueueLast.pre = pre;

        taskQueueLast = task;
        taskQueueLast.next = null;
      }
      else {
        BuildTask pre = task.pre, next = task.next, next1 = next.next;

        task.next = next1;
        next1.pre = task;

        if (pre != null) pre.next = next;
        next.pre = pre;

        next.next = task;
        task.pre = next;
      }

      if (task == taskQueueHead){
        taskQueueHead = task.pre;
      }

      queueUpdated();
    }

    public void removeTask(BuildTask task){
      if (taskQueueHead == task){
        taskQueueHead = taskQueueHead.next;
        if (taskQueueHead != null){
          taskQueueHead.pre = null;
        }
        else taskQueueLast = null;
      }
      else if (taskQueueLast == task){
        taskQueueLast = taskQueueLast.pre;
        if (taskQueueLast != null){
          taskQueueLast.next = null;
        }
        else taskQueueHead = null;
      }
      else {
        if (task.pre != null) task.pre.next = task.next;
        if (task.next != null) task.next.pre = task.pre;
      }

      taskCount--;
      Pools.free(task);
      queueUpdated();
    }

    public void pushTask(UnitType type, int amount, int factoryIndex){
      pushTask(BuildTask.make(type, amount, factoryIndex));
    }

    public void pushTask(BuildTask task){
      if (taskQueueHead == null){
        taskQueueHead = taskQueueLast = task;
      }
      else{
        task.next = taskQueueHead;
        taskQueueHead.pre = task;

        taskQueueHead = task;
        task.pre = null;
      }

      taskCount++;

      queueUpdated();
    }

    public void appendTask(UnitType type, int amount, int factoryIndex){
      appendTask(BuildTask.make(type, amount, factoryIndex));
    }

    public void appendTask(BuildTask task){
      if (taskQueueLast == null){
        taskQueueHead = taskQueueLast = task;
      }
      else{
        task.pre = taskQueueLast;
        taskQueueLast.next = task;

        taskQueueLast = task;
        task.next = null;
      }

      taskCount++;

      queueUpdated();
    }

    @Override
    public void priority(int priority) {
      this.priority = priority;
      distributor.network.priorityModified(this);
    }

    @Override
    public NormalCrafterBuild create(Block block, Team team) {
      super.create(block, team);
      distributor = new DistributeModule(this);
      distributor.setNet();

      itemsBuffer = new ItemsBuffer();
      itemsBuffer.capacity = Integer.MAX_VALUE;
      items = itemsBuffer.generateBindModule();
      return this;
    }

    @Override
    public void networkValided() {
      if (pullItemsRequest != null) pullItemsRequest.kill();

      pullItemsRequest = new PullMaterialRequest();
      pullItemsRequest.init(distributor.network);

      distributor.assign(pullItemsRequest);
    }

    @Override
    public Vec2 getCommandPosition(){
      if (currentTask == null) return null;
      return currentTask.targetPos;
    }

    @Override
    public void onCommand(Vec2 target){
      for (BuildTask task: taskQueueHead) {
        task.targetPos = target;
      }
    }

    @Override
    public void buildConfiguration(Table table) {
      table.button(Icon.settings, Styles.cleari, 40, () -> Sgl.ui.unitFactoryCfg.show(this)).size(56);
    }

    @Override
    public void updateTile() {
      super.updateTile();

      if (pullItemsRequest != null) pullItemsRequest.update();

      if (currentTask == null){
        recipeCurrent = -1;
        return;
      }

      if (buildCount >= currentTask.queueAmount || (skipBlockedTask && !Units.canCreate(team, currentTask.buildUnit))){
        BuildTask task = popTask();

        if (queueMode){
          appendTask(task);
        }
        else{
          Pools.free(task);
        }
      }
      else recipeCurrent = currentTask.factoryIndex;
    }

    protected void queueUpdated() {
      if (taskQueueHead != currentTask){
        buildCount = 0;
        progress(0);
        currentTask = taskQueueHead;
      }
    }

    @Override
    public boolean shouldConsume() {
      return super.shouldConsume() && activity && currentTask != null && buildCount < currentTask.queueAmount;
    }

    @Override
    public void craftTrigger() {
      super.craftTrigger();
      buildCount++;

      if (currentTask == null || getPayloads().isEmpty()) return;

      Unit unit = ((UnitPayload) getPayload()).unit;
      if(unit.isCommandable()){
        if (currentTask.targetPos != null){
          unit.command().commandPosition(currentTask.targetPos);
        }
        if (currentTask.command != null) unit.command().command(currentTask.command);
      }
    }

    @Override
    public float handleOutputPayload() {
      return lastOutputProgress = super.handleOutputPayload();
    }

    @Override
    public void updateFactory() {
      if(recipeCurrent == -1 || producer.current == null){
        warmup(Mathf.lerpDelta(warmup(), 0, stopSpeed()));
        return;
      }

      if(shouldConsume() && consumeValid()){
        progress(progress() + progressIncrease(consumer.current.craftTime));
        warmup(Mathf.lerpDelta(warmup(), 1, warmupSpeed()));

        onCraftingUpdate();
      }
      else if (!outputLocking() || lastOutputProgress >= 0.999f){
        warmup(Mathf.lerpDelta(warmup(), 0, stopSpeed()));
      }

      totalProgress(totalProgress() + consumer.consDelta());

      while(progress() >= 1){
        progress(progress() - 1);
        consumer().trigger();
        producer().trigger();

        craftTrigger();
      }
    }

    public String statusText() {
      if (!activity) return Core.bundle.get("infos.waiting");
      else if (currentTask == null) return Core.bundle.get("infos.noTask");
      else if (outputting() != null){
        if (lastOutputProgress >= 0.999f) return Core.bundle.get("infos.cannotDump");
        else return "...";
      }
      else if (!consumeValid()) return Core.bundle.get("infos.leakMaterial");
      else return Core.bundle.get("infos.working");
    }

    @Override
    public DistributeModule distributor() {
      return distributor;
    }

    public BuildTask getTask(int index) {
      if (index >= taskCount || index < 0)
        throw new IndexOutOfBoundsException("size: " + taskCount + ", index: " + index);

      BuildTask curr;

      if (index < taskCount/2) {
        curr = taskQueueHead;
        while (index > 0){
          curr = curr.next;
          index--;
        }
      }
      else{
        curr = taskQueueLast;
        while (index < taskCount - 1){
          curr = curr.pre;
          index++;
        }
      }

      return curr;
    }

    public int indexOfTask(BuildTask task){
      int id = 0;

      BuildTask t = taskQueueHead;
      while (t != null){
        if (task == t) return id;

        t = t.next;
        id++;
      }

      return -1;
    }

    public String serializeTasks(){
      StringBuilder builder = new StringBuilder();

      if (taskQueueHead == null) return "empty";
      for (BuildTask task : taskQueueHead) {
        if (builder.length() > 0) builder.append(Sgl.NL);
        builder.append(task.buildUnit.name).append(";")
            .append(task.queueAmount).append(";")
            .append(task.targetPos == null? "none": task.targetPos.x).append(";")
            .append(task.targetPos == null? "none": task.targetPos.y).append(";")
            .append(task.command == null? "none": task.command.name).append(";");
      }

      return builder.toString();
    }

    public void deserializeTask(String str, boolean append){
      if (str.equals("empty")) return;

      if (!append) clearTask();

      StringBuilder err = null;
      try(BufferedReader reader = new BufferedReader(new StringReader(str))) {
        String line;
        while ((line = reader.readLine()) != null) {
          try {
            String[] args = line.split(";");

            if (args.length != 5 && !(args.length == 6 && args[5].trim().isEmpty()))
              throw new IllegalArgumentException("illegal build task args length, must be 5");

            UnitType unit = Vars.content.unit(args[0]);
            int amount = Integer.parseInt(args[1]);

            int index = producers().indexOf(p -> p.get(ProduceType.payload) != null && p.get(ProduceType.payload).payloads[0].item == unit);

            if (index < 0)
              throw new IllegalArgumentException("invalid task, this factory cannot product unit " + unit.localizedName);

            BuildTask task = BuildTask.make(unit, amount, index);

            if (!args[2].equals("none")) {
              task.targetPos = new Vec2(Float.parseFloat(args[2]), Float.parseFloat(args[3]));
            }

            if (!args[4].equals("none")) {
              task.command = UnitCommand.all.find(c -> c.name.equals(args[4]));
            }

            appendTask(task);
          }catch (Throwable e){
            if (err == null) err = new StringBuilder();
            err.append(e).append(": ").append(e.getLocalizedMessage()).append(Sgl.NL);
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      if (err != null) Vars.ui.showErrorMessage(err.toString());
    }

    @Override
    public boolean acceptItem(Building source, Item item) {
      return (!matrixDistributeOnly || source == distributor.network.getCore()) && super.acceptItem(source, item);
    }

    @Override
    public void write(Writes write) {
      super.write(write);

      write.bool(activity);
      write.bool(queueMode);
      write.bool(skipBlockedTask);
      write.i(buildCount);
      write.i(priority);

      write.str(serializeTasks());
    }

    @Override
    public void read(Reads read, byte revision) {
      super.read(read, revision);

      activity = read.bool();
      queueMode = read.bool();
      skipBlockedTask = read.bool();
      buildCount = read.i();
      priority = read.i();

      int tmp1 = buildCount;
      float tmp2 = progress();
      deserializeTask(read.str(), false);
      buildCount = tmp1;
      progress(tmp2);
    }

    protected class PullMaterialRequest extends DistRequestBase{
      ObjectSet<Item> requests = new ObjectSet<>();
      ItemsBuffer source;

      public PullMaterialRequest() {
        super(SglUnitFactoryBuild.this);
      }

      @Override
      public void init(DistributeNetwork target) {
        super.init(target);
        source = target.getCore().getBuffer(DistBufferType.itemBuffer);
      }

      @Override
      protected boolean preHandleTask() {
        requests.clear();
        if (currentTask != null){
          tas: for (ItemStack stack : consumers.get(currentTask.factoryIndex).get(ConsumeType.item).consItems) {
            if (requests.add(stack.item) && items.get(stack.item) < stack.amount*itemCapacityMulti) {
              for (MatrixGrid grid : target.grids) {
                for (MatrixGrid.BuildingEntry<Building> entry: grid.<Building>get(GridChildType.container,
                    (e, c) -> e.block.hasItems && e.items != null && e.items.has(stack.item)
                        && c.get(GridChildType.container, stack.item))) {

                  entry.entity.removeStack(stack.item, 1);
                  source.put(stack.item, 1);
                  source.dePutFlow(stack.item, 1);

                  continue tas;
                }
              }
            }
          }
        }

        return true;
      }

      @Override
      protected boolean handleTask() {
        boolean tst = false;
        boolean allFull = true;

        for (Item item : Vars.content.items()) {
          if (requests.contains(item)) {
            if (acceptItem(target.getCore().getBuilding(), item)) {
              if (source.get(item) >= 1) {
                source.remove(item, 1);
                items.add(item, 1);
                tst = true;
              }

              allFull = false;
            }
          }
          else if (items.has(item)){
            items.remove(item, 1);
            source.put(item, 1);
          }
        }

        return tst || allFull;
      }

      @Override
      protected boolean afterHandleTask() {
        return true;
      }
    }

    public static class BuildTask implements Pool.Poolable, Iterable<BuildTask> {
      public UnitType buildUnit;
      public int factoryIndex;
      public Vec2 targetPos;
      public UnitCommand command = UnitCommand.moveCommand;
      public int queueAmount;

      public BuildTask pre, next;

      public static BuildTask make(UnitType type, int amount, int factoryIndex){
        BuildTask task = Pools.obtain(BuildTask.class, BuildTask::new);
        task.buildUnit = type;
        task.factoryIndex = factoryIndex;
        task.queueAmount = amount;

        task.command = task.buildUnit.defaultCommand != null? task.buildUnit.defaultCommand: task.buildUnit.commands[0];

        return task;
      }

      @Override
      public void reset() {
        buildUnit = null;
        factoryIndex = -1;
        targetPos = null;
        command = UnitCommand.moveCommand;
        queueAmount = 0;

        pre = null;
        next = null;
      }

      @Override
      public Iterator<BuildTask> iterator() {
        return new Iterator<>() {
          BuildTask curr = BuildTask.this;

          @Override
          public boolean hasNext() {
            return curr != null;
          }

          @Override
          public BuildTask next() {
            BuildTask res = curr;
            curr = curr.next;
            return res;
          }
        };
      }
    }
  }
}
