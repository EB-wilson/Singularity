package singularity.ui.dialogs;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Nullable;
import arc.util.Scaling;
import arc.util.Time;
import arc.util.serialization.Jval;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import singularity.Singularity;
import singularity.graphic.SglDrawConst;
import singularity.ui.SglStyles;
import universecore.ui.elements.markdown.Markdown;
import universecore.util.UrlDownloader;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class PublicInfoDialog extends BaseDialog {
  protected static final String directory = "https://raw.githubusercontent.com/EB-wilson/Singularity/master/publicInfo/directory.hjson";
  protected static final String langDir = "https://raw.githubusercontent.com/EB-wilson/Singularity/master/publicInfo/$locale$/$docName$";

  protected static final String localePart = "$locale$";
  protected static final String docNamePart = "$docName$";

  boolean loading;
  @Nullable Throwable error;
  Table mainLayout, listView, messageView;

  MsgEntry current;

  protected Seq<MsgEntry> messages = new Seq<>();
  protected ObjectMap<String, Markdown> documents = new ObjectMap<>();

  public PublicInfoDialog(){
    super(Core.bundle.get("misc.publicInfo"));

    addCloseButton();

    mainLayout = cont.table().grow().get();
    cont.row();
    cont.image().color(Pal.accent).growX().height(4).colspan(2).pad(0).margin(0);

    rebuild();

    shown(this::refresh);
    resized(this::rebuild);
  }

  protected void refresh(){
    loading = true;
    setupInfos();
    Http.get(directory, res -> {
      if (!loading) return;

      messages.clear();
      for (Jval jval : Jval.read(res.getResultAsString()).get("pages").asArray()) {
        messages.add(new MsgEntry(jval));
      }

      Core.app.post(() -> {
        loading = false;
        if (current == null) current = messages.first();
        setupInfos();
      });
    }, this::handleError);
  }

  protected void handleError(Throwable e) {
    error = e;
    loading = false;
  }

  protected void rebuild() {
    mainLayout.clearChildren();
    if (Core.graphics.isPortrait()){
      mainLayout.table(SglDrawConst.grayUI, msg -> {
        messageView = msg;
      }).grow().pad(4);
      mainLayout.row();

      Collapser coll = new Collapser(t -> t.pane(p -> {
        listView = p;
        listView.defaults().growX().height(74).padBottom(6);
      }).growX().fillY(), true).setDuration(0.5f);
      mainLayout.button(Icon.up, Styles.clearNonei, 32, () -> {
        coll.setCollapsed(!coll.isCollapsed(), true);
      }).growX().height(40).update(i -> i.getStyle().imageUp = coll.isCollapsed() ? Icon.upOpen : Icon.downOpen);
      mainLayout.row();
      mainLayout.table(SglDrawConst.grayUI, t -> t.add(coll).growX().fillY()).growX().fillY();
    }
    else {
      mainLayout.table(SglDrawConst.grayUI).growY().width(420).padLeft(40).get().top().pane(list -> {
        listView = list;
        listView.defaults().growX().height(74).padLeft(4).padRight(4).padBottom(6);
      }).growX().fillY().top();
      mainLayout.image().padLeft(5).padRight(5).color(Color.lightGray).width(3).growY();
      mainLayout.table(SglDrawConst.grayUI, msg -> {
        messageView = msg;
      }).grow().padRight(40);
    }

    setupInfos();
  }

  protected void setupInfos() {
    if (loading){
      messageView.clearChildren();
      messageView.table(ld -> {
        ld.center().defaults().center();
        ld.add(new Element(){
          @Override
          public void draw() {
            super.draw();
            Draw.color(Pal.accent);
            Fill.square(x + width/2, y + height/2, width/2/Mathf.sqrt2, Time.time);
            Fill.square(x + width/2, y + height/2, width/2/Mathf.sqrt2, 45 + 2*Time.time);
          }
        }).size(80);
        ld.row();
        ld.add(Core.bundle.get("misc.loading"));
      }).grow().name("loading");
    }
    else if (current != null){
      messageView.clearChildren();

      Markdown md = documents.get(current.docName);
      if (md == null){
        loading = true;
        setupInfos();

        MsgEntry curr = current;
        Http.get(current.getDocURL(Core.bundle.getLocale()), response -> {
          if (!loading) return;
          String doc = response.getResultAsString();

          Core.app.post(() -> {
            documents.put(current.docName, new Markdown(doc, SglStyles.defaultMD));
            if (current == curr){
              loading = false;
              setupInfos();
            }
          });
        }, this::handleError);
      }
      else messageView.table().grow().pad(9).top().get().pane(Styles.smallPane, md).grow().top().scrollX(false).get().setFillParent(true);
    }

    listView.clearChildren();
    for (MsgEntry entry : messages) {
      listView.add(buildEntryButton(entry));
      listView.row();
    }
  }

  protected Button buildEntryButton(MsgEntry entry){
    return new Button(SglStyles.underline){{
      left().defaults().left();

      table(img -> {
        img.image().growY().width(40f).color(entry.color);
        img.row();
        img.image().height(6).width(40f).color(entry.color.cpy().mul(0.8f, 0.8f, 0.8f, 1f));
      }).growY().fillX().padLeft(-12).padBottom(-11);

      table(t -> {
        t.defaults().left().growX();
        t.add(entry.getTitle(Core.bundle.getLocale())).color(Pal.accent);
        t.row();
        t.add(entry.date == null? "no date": entry.date.toString()).color(Pal.gray);
      }).grow().padLeft(5);

      update(() -> setChecked(entry == current));

      clicked(() -> current = entry);
    }};
  }

  protected static class MsgEntry{
    public String docName;
    public Date date;
    public Color color;
    public String[] languages;
    public String[] titles;

    public MsgEntry(Jval entry){
      docName = entry.getString("docName");

      try {
        date = DateFormat.getInstance().parse(entry.getString("date"));
      } catch (ParseException e) {
        date = null;
      }

      color = Color.valueOf(entry.getString("color", "#ffd37f"));

      Jval.JsonArray lgs = entry.get("languages").asArray();
      languages = new String[lgs.size];
      for (int i = 0; i < lgs.size; i++) {
        languages[i] = lgs.get(i).asString();
      }

      Jval.JsonArray tits = entry.get("title").asArray();
      titles = new String[tits.size];
      for (int i = 0; i < tits.size; i++) {
        titles[i] = tits.get(i).asString();
      }
    }

    public String getDocURL(Locale locale){
      String loc = locale.toString().replace("en_US", "").replace("en", "");

      for (String language : languages) {
        if (language.equals(loc)) return langDir.replace(localePart, loc).replace(docNamePart, docName);
      }

      return langDir.replace(localePart, "").replace(docNamePart, docName);
    }

    public String getTitle(Locale locale){
      String loc = locale.toString().replace("en_US", "").replace("en", "");

      for (int i = 0; i < languages.length; i++) {
        if (languages[i].equals(loc)) return titles[i];
      }

      return getTitle(Locale.US);
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) return true;
      if (object == null || getClass() != object.getClass()) return false;
      MsgEntry msgEntry = (MsgEntry) object;
      return Objects.equals(docName, msgEntry.docName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(docName);
    }
  }
}
