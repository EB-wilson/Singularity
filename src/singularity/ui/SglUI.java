package singularity.ui;

import arc.Core;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.WindowedMean;
import arc.math.geom.Vec3;
import arc.scene.actions.Actions;
import arc.scene.event.ElementGestureListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.TextButton;
import arc.util.Align;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.graphics.g3d.PlanetParams;
import mindustry.graphics.g3d.PlanetRenderer;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.meta.StatUnit;
import singularity.Sgl;
import singularity.core.UpdatePool;
import singularity.graphic.Blur;
import singularity.graphic.SglDrawConst;
import singularity.graphic.renders.SglPlanetRender;
import singularity.ui.dialogs.*;
import singularity.ui.dialogs.ModConfigDialog.ConfigButton;
import singularity.ui.dialogs.ModConfigDialog.ConfigCheck;
import singularity.ui.dialogs.ModConfigDialog.ConfigSepLine;
import singularity.ui.dialogs.ModConfigDialog.ConfigSlider;
import singularity.ui.fragments.*;
import singularity.ui.fragments.override.SglMenuFrag;

@SuppressWarnings("DuplicatedCode")
public class SglUI{
  //ui相关
  public EntityInfoFrag entityInfoFrag;

  /**主菜单*/
  public MainMenu mainMenu;
  public AboutModDialog aboutDialog;
  public PublicInfoDialog publicInfo;
  public ContributorsDialog contributors;
  public ModConfigDialog config;

  public DistNetMonitorDialog bufferStat;

  public UnitFactoryCfgDialog unitFactoryCfg;

  public ToolBarFrag toolBar;

  public DebugInfos debugInfos;

  public static Blur uiBlur = new Blur(Blur.DEf_B);

  static {
    UpdatePool.receive("syncUIBlurCfg", () -> {
      uiBlur.blurScl = Sgl.config.blurLevel;
      uiBlur.blurSpace = Sgl.config.backBlurLen;

      SglStyles.blurBack.stageBackground = Sgl.config.enableBlur? SglStyles.BLUR_BACK: Styles.black9;
      Styles.defaultDialog.stageBackground = Sgl.config.enableBlur? SglStyles.BLUR_BACK: Styles.black9;
    });
  }

  private static final Object[][] grapPreset = {
      {1, false, 128, false, false, 64, false},
      {2, true, 256, false, true, 256, false},
      {2, true, 512, false, true, 512, true},
      {3, true, 1024, true, true, 1024, true},
      {3, true, 2048, true, true, 4096, true},
  };

  public BaseDialog setPosDialog = new BaseDialog(Core.bundle.get("settings.setCamPos")){
    private final PlanetParams params = new PlanetParams();
    private final Vec3 camRight = new Vec3(1, 0, 0);

    private final PlanetRenderer renderer = new SglPlanetRender();

    private float lastDx, lastDy, speed;
    private boolean controlling, paning = false;
    private final WindowedMean dxMean = new WindowedMean(12), dyMean = new WindowedMean(12);

    {
      buttons.button(Core.bundle.get("misc.sure"), Icon.ok, () -> {
        hide();
        Sgl.config.defaultCameraPos = new float[]{
            params.camPos.x,
            params.camPos.y,
            params.camPos.z,
            Tmp.v1.set(params.camDir.x, params.camDir.z).angle(),
            Tmp.v2.set(Tmp.v1.len(), params.camDir.y).angle()
        };
      }).size(210f, 64f);

      cont.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());
      cont.fill((x, y, w, h) -> {
        renderer.render(params);
      });
      shown(() -> {
        float[] arr = Sgl.config.defaultCameraPos;
        params.camPos.set(arr[0], arr[1], arr[2]);

        params.camDir.set(1, 0, 0);
        params.camUp.set(0, 1, 0);
        camRight.set(0, 0, 1);

        params.camDir.rotate(params.camUp, arr[3]);
        camRight.rotate(params.camUp, arr[3]);

        params.camDir.rotate(camRight, arr[4]);
        params.camUp.rotate(camRight, arr[4]);
      });

      resized(() -> cont.setSize(Core.graphics.getWidth(), Core.graphics.getHeight()));

      update(() -> {
        if (Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true) == this){
          Core.scene.setScrollFocus(this);
        }

        float dx = lastDx/9*Time.delta, dy = lastDy/9*Time.delta;
        if(!controlling){
          params.camDir.rotate(camRight, dy);
          params.camUp.rotate(camRight, dy);

          params.camDir.rotate(params.camUp, dx);
          camRight.rotate(params.camUp, dx);
          lastDx = Mathf.lerpDelta(lastDx, 0, 0.035f);
          lastDy = Mathf.lerpDelta(lastDy, 0, 0.035f);
        }

        speed = Mathf.lerpDelta(speed, 0, 0.05f);
        if (Math.abs(speed) > 0.001f) {
          params.camPos.add(
              params.camDir.cpy().setLength(speed).scl(speed > 0? 1 : -1)
          );
        }

        if (paning){
          paning = false;
        }
        else {
          dxMean.add(0);
          dyMean.add(0);
        }
      });

      scrolled(d -> {
        speed = Mathf.clamp(speed - d*0.25f, -1, 1);
      });

      addCaptureListener(new ElementGestureListener(){
        @Override
        public void touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
          controlling = true;
          Core.graphics.restoreCursor();

          super.touchDown(event, x, y, pointer, button);
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
          controlling = false;
          lastDx = dxMean.rawMean();
          lastDy = dyMean.rawMean();

          super.touchUp(event, x, y, pointer, button);
        }

        @Override
        public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
          paning = true;
          params.camDir.rotate(camRight, deltaY/9);
          params.camUp.rotate(camRight, deltaY/9);

          params.camDir.rotate(params.camUp, deltaX/9);
          camRight.rotate(params.camUp, deltaX/9);

          dxMean.add(deltaX);
          dyMean.add(deltaY);
          super.pan(event, x, y, deltaX, deltaY);
        }

        @Override
        public void zoom(InputEvent event, float initialDistance, float distance){
          speed = Mathf.clamp((distance - initialDistance)/450, -1, 1);

          super.zoom(event, initialDistance, distance);
        }
      });
    }
  };

  public void init(){
    entityInfoFrag = new EntityInfoFrag();
    entityInfoFrag.displayMatcher.put(new UnitHealthDisplay<>(), e -> e instanceof Unit);
    entityInfoFrag.displayMatcher.put(new UnitStatusDisplay<>(), e -> e instanceof Unit);

    mainMenu = new MainMenu();
    aboutDialog = new AboutModDialog();
    publicInfo = new PublicInfoDialog();
    contributors = new ContributorsDialog();
    config = new ModConfigDialog();
    bufferStat = new DistNetMonitorDialog();
    unitFactoryCfg = new UnitFactoryCfgDialog();

    toolBar = new ToolBarFrag();

    debugInfos = new DebugInfos();

    entityInfoFrag.build(Vars.ui.hudGroup);

    mainMenu.build();
    aboutDialog.build();
    contributors.build();
    unitFactoryCfg.build();

    toolBar.init();

    if (Sgl.config.debugMode){
      debugInfos.build(Vars.ui.hudGroup);
    }

    if(!Sgl.config.disableModMainMenu){
      Vars.ui.menufrag = new SglMenuFrag();
      Vars.ui.menufrag.build(Vars.ui.menuGroup);
    }

    setConfigItems();
  }

  void setConfigItems(){
    config.addConfig("general", Icon.settings,
        new ConfigSepLine("mainMenu", Core.bundle.get("infos.mainMenu")),
        new ConfigCheck("disableModMainMenu", b -> Sgl.config.disableModMainMenu = b, () -> Sgl.config.disableModMainMenu){{
          str = () -> Sgl.config.disableModMainMenu? Core.bundle.get("infos.mustModifyFileWarn"): "";
        }},
        new ConfigCheck("showModMenuWenLaunch", b -> Sgl.config.showModMenuWenLaunch = b, () -> Sgl.config.showModMenuWenLaunch),
        new ConfigCheck("mainMenuUniverseBackground", b -> Sgl.config.mainMenuUniverseBackground = b, () -> Sgl.config.mainMenuUniverseBackground),
        new ConfigCheck("staticMainMenuBackground", b -> Sgl.config.staticMainMenuBackground = b, () -> Sgl.config.staticMainMenuBackground),
        new ConfigButton("defaultCameraPos", () -> new TextButton(Core.bundle.get("settings.setCamPos"), Styles.flatt){{
          clicked(setPosDialog::show);
        }}),
        new ConfigCheck("movementCamera", b -> Sgl.config.movementCamera = b, () -> Sgl.config.movementCamera),
        new ConfigSepLine("infoDisplay", Core.bundle.get("infos.infoDisplay")),
        new ConfigCheck("showInfos", b -> Sgl.config.showInfos = b, () -> Sgl.config.showInfos),
        new ConfigSlider(
            "flushInterval",
            f -> Sgl.config.flushInterval = f,
            () -> Sgl.config.flushInterval,
            0, 60, 1
        ){{
          str = () -> Strings.autoFixed(Sgl.config.flushInterval/60, 2) + StatUnit.seconds.localized();
        }},
        new ConfigSlider(
            "maxDisplay",
            f -> Sgl.config.maxDisplay = (int) f,
            () -> Sgl.config.maxDisplay,
            4, 64, 1
        ),
        new ConfigSlider(
            "showInfoScl",
            f -> Sgl.config.showInfoScl = f,
            () -> Sgl.config.showInfoScl,
            0.5f, 4, 0.1f
        ),
        new ConfigSlider(
            "holdDisplayRange",
            f -> Sgl.config.holdDisplayRange = (int) f,
            () -> Sgl.config.holdDisplayRange,
            64, 512, 1
        ),
        new ConfigButton("healthBarStyle", () -> new TextButton("", Styles.flatt){
          final TextButton self = this;

          {
            clicked(() -> {
              Sgl.ui.config.setHover(t -> {
                t.setSize(220, 0);
                t.update(() -> {
                  t.setPosition(self.getX(Align.bottom), self.getY(Align.bottom), Align.bottomRight);
                  t.setTransform(true);
                });
                t.visible = true;

                t.top().pane(Styles.noBarPane, p -> {
                  p.defaults().top().growX().height(45);
                  for(HealthBarStyle style: HealthBarStyle.values()){
                    p.button(b -> b.add(style.name()), Styles.underlineb, () -> {
                      Sgl.config.healthBarStyle = style;
                      t.clearActions();
                      t.actions(
                          Actions.parallel(Actions.alpha(0, 0.5f), Actions.sizeTo(t.getWidth(), 0, 0.5f)),
                          Actions.run(() -> Sgl.ui.config.clearHover())
                      );
                    }).update(b -> b.setChecked(Sgl.config.healthBarStyle == style));
                    p.row();
                  }
                }).growX().fillY().maxHeight(380).pad(-5).top().get().setScrollingDisabledX(true);
                t.clearActions();
                t.actions(
                    Actions.alpha(0),
                    Actions.parallel(Actions.alpha(1, 0.5f), Actions.sizeTo(t.getWidth(), 380, 0.5f))
                );
              });
            });

            update(() -> setText(Sgl.config.healthBarStyle.name()));
          }
        }),
        new ConfigSlider(
            "statusSize",
            f -> Sgl.config.statusSize = f,
            () -> Sgl.config.statusSize,
            10, 40, 1
        ),
        new ConfigCheck("showStatusTime", b -> Sgl.config.showStatusTime = b, () -> Sgl.config.showStatusTime)
    );
    config.addConfig("graphic", Icon.image,
        new ConfigSepLine("uiView", Core.bundle.get("misc.uiView")),
        new ConfigCheck("enableBlur", b -> Sgl.config.enableBlur = b, () -> Sgl.config.enableBlur),
        new ConfigSlider(
            "blurLevel",
            f -> Sgl.config.blurLevel = (int) f,
            () -> Sgl.config.blurLevel,
            1, 8, 1
        ),
        new ConfigSlider(
            "backBlurLen",
            f -> Sgl.config.backBlurLen = f,
            () -> Sgl.config.backBlurLen,
            0.5f, 8, 0.25f
        ),

        new ConfigSepLine("animateView", Core.bundle.get("misc.animateView")),
        new ConfigSlider(
            "preset",
            f -> {
              if(f >= 0 && f < grapPreset.length){
                Object[] a = grapPreset[(int) f];
                Sgl.config.animateLevel = ((Number) a[0]).intValue();
                Sgl.config.enableShaders = (Boolean) a[1];
                Sgl.config.mathShapePrecision = ((Number) a[2]).intValue();
                Sgl.config.enableDistortion = (Boolean) a[3];
                Sgl.config.enableParticle = (Boolean) a[4];
                Sgl.config.maxParticleCount = ((Number) a[5]).intValue();
                Sgl.config.enableLightning = (Boolean) a[6];
              }
            },
            this::matchLevel,
            0, grapPreset.length, 1
        ){{
          str = () -> Core.bundle.get("settings.graph_" + matchLevel());
        }},
        new ConfigSlider(
            "animateLevel",
            f -> Sgl.config.animateLevel = (int) f,
            () -> Sgl.config.animateLevel,
            1, 3, 1
        ),
        new ConfigCheck("enableShaders", b -> Sgl.config.enableShaders = b, () -> Sgl.config.enableShaders),
        new ConfigSlider(
            "mathShapePrecision",
            f -> Sgl.config.mathShapePrecision = (int)f,
            () -> Sgl.config.mathShapePrecision,
            128, 2048, 8
        ),
        new ConfigCheck("enableDistortion", b -> Sgl.config.enableDistortion = b, () -> Sgl.config.enableDistortion),
        new ConfigCheck("enableParticle", b -> Sgl.config.enableParticle = b, () -> Sgl.config.enableParticle),
        new ConfigSlider(
            "maxParticleCount",
            f -> Sgl.config.maxParticleCount = (int) f,
            () -> Sgl.config.maxParticleCount,
            0f, 4096, 8
        ),
        new ConfigCheck("enableLightning", b -> Sgl.config.enableLightning = b, () -> Sgl.config.enableLightning)
    );
    config.addConfig("advance", SglDrawConst.configureIcon,
        new ConfigSepLine("reciprocal", Core.bundle.get("infos.override")),
        new ConfigCheck("modReciprocal", b -> Sgl.config.modReciprocal = b, () -> Sgl.config.modReciprocal){{
          str = () -> Sgl.config.modReciprocal? "": Core.bundle.get("infos.reciprocalWarn");
        }},
        new ConfigCheck("modReciprocalContent", b -> Sgl.config.modReciprocalContent = b, () -> Sgl.config.modReciprocalContent){{
          str = () -> Sgl.config.modReciprocalContent? "": Core.bundle.get("infos.reciprocalWarn");
        }},
        new ConfigSepLine("reciprocal", Core.bundle.get("infos.debug")),
        new ConfigCheck("loadInfo", b -> Sgl.config.loadInfo = b, () -> Sgl.config.loadInfo),
        new ConfigCheck("debugMode", b -> Sgl.config.debugMode = b, () -> Sgl.config.debugMode){{
          str = () -> Core.bundle.get("infos.unusableDebugButton");
          disabled = () -> true;
        }}
    );
  }

  int matchLevel(){
    for(int i = 0; i < grapPreset.length; i++){
      Object[] a = grapPreset[i];

      if(a[0].equals(Sgl.config.animateLevel)
      && a[1].equals(Sgl.config.enableShaders)
      && a[2].equals(Sgl.config.mathShapePrecision)
      && a[3].equals(Sgl.config.enableDistortion)
      && a[4].equals(Sgl.config.enableParticle)
      && a[5].equals(Sgl.config.maxParticleCount)
      && a[6].equals(Sgl.config.enableLightning)
      ) return i;
    }
    return grapPreset.length;
  }
}
