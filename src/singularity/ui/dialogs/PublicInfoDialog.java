package singularity.ui.dialogs;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Cell;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import singularity.Sgl;
import universeCore.UncCore;
import universeCore.util.animLayout.CellAction;
import universeCore.util.animLayout.CellAnimateGroup;
import universeCore.util.animLayout.CellChangeColorAction;
import universeCore.util.ini.Ini;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static universeCore.util.ini.IniTypes.*;

public class PublicInfoDialog extends BaseListDialog{
  private static final Element row = new Element();
  private static final String langRegex = "#locale#";
  private static final Pattern imagePattern = Pattern.compile("<image *=.*>");
  
  Cons<Integer> buildInfo;
  
  public PublicInfoDialog(){
    super(Core.bundle.get("misc.publicInfo"));
    
    shown(this::rebuild);
    refresh();
  }
  
  public void refresh(){
    String directory = Sgl.publicInfo + "directory.ini";
    Seq<ItemEntry> itemEntrySeq = new Seq<>();
    
    Http.get(directory, request -> {
      String direResult = request.getResultAsString();
      Ini dire = new Ini();
      dire.parse(direResult);
      Log.info(direResult);
      
      dire.eachSection((name, sect) -> {
        Log.info(name + ", " + sect.get().toString());
        if(name.equals("") || sect.get().isEmpty()) return;
        Seq<Element> elements = new Seq<>();
        ObjectMap<String, TextureRegion> atlas = new ObjectMap<>();
        String[] title = {""};
        
        IniMap assets = (IniMap)sect.get("assets");
        Log.info(assets.get().toString());
        assets.each((n, image) -> {
          Log.info("loadAssets" + n);
          if(!atlas.containsKey(n)){
            if((((IniArray)image.get()).get().get(0).get() != ("modAtlas"))){
              IniDataStructure size = ((IniArray)image.get()).get().get(1).get();
              
              atlas.put(n, new TextureRegion(Core.atlas.find("nomap").texture, size.get().get(0).get(), size.get().get(1).get()));
              Http.get("https://" + ((IniArray)image.get()).get().get(0).get(), res -> {
                Pixmap pix = new Pixmap(res.getResult());
                Core.app.post(() -> {
                  try{
                    Texture tex = new Texture(pix);
                    tex.setFilter(Texture.TextureFilter.linear);
                    atlas.put(n, new TextureRegion(tex));
                    pix.dispose();
                  }catch(Exception e){
                    Log.err(e);
                  }
                });
              }, Log::err);
            }
            else atlas.put(n, Core.atlas.find(((IniArray)image.get()).get().get(1).get()));
          }
        });
        Log.info(2);
        IniArray arr = ((IniArray)sect.get("languages"));
        ObjectSet<String> languages = new ObjectSet<>();
        
        for(IniObject lang: arr){
          languages.add(lang.get());
        }
        
        String currLang = Core.settings.getString("locale");
        
        String language = !currLang.equals("en") && languages.contains(currLang)? currLang: "";
        String url = "https://" + ((String)sect.get("info").get()).replace(langRegex, language);
        
        int[] height = {0};
        
        Http.get(url, result -> {
          String[] strs = result.getResultAsString().split("\n");
          title[0] = strs[0];
          
          for(int i=1; i<strs.length; i++){
            if(imagePattern.matcher(strs[i]).matches()){
              String image = strs[i].replaceAll("<image *=", "").replace(">", "").replace(" ", "");
              Image region = new Image(atlas.get(image));
              
              float iWidth = Math.min(width - margin - itemBoardWidth, region.getImageWidth());
              float scl = iWidth/region.getImageWidth();
              
              region.setSize(region.getImageWidth()*scl,(height[0] += region.getImageHeight()*scl));
              
              elements.add(region);
            }
            else{
              Label text = new Label(strs[i]);
              text.setWidth(width - margin - itemBoardWidth);
              text.setEllipsis(true);
              text.layout();
              height[0] += text.getPrefHeight();
              Log.info(text.getPrefHeight());
              elements.add(text);
            }
            
            height[0] += pad;
          }
        }, e -> {
          infoTable.clearChildren();
          infoTable.add(Core.bundle.get("warn.publicInfo.connectFailed"));
          infoTable.row();
          infoTable.button(Core.bundle.get("misc.refresh"), this::refresh).size(140, 60);
          
          Log.err(e);
        });
        Log.info(3);
        
        itemEntrySeq.add(new ItemEntry(table -> table.add(title[0]), table -> {
          AtomicInteger i = new AtomicInteger();
          
          Log.info("height" + height[0]);
          table.table(in -> {
            in.defaults().top().margin(margin);
            buildInfo = index -> {
              if(index >= elements.size) return;
              Cell<?> cell = in.add(elements.get(index)).padTop(pad + 30).color(elements.get(index).color.cpy().a(0));
              in.row();
              UncCore.cellActions.add(new CellAnimateGroup(
                  new CellChangeColorAction(cell, in, elements.get(index).color.cpy().a(1), 6),
                  (Runnable)() -> buildInfo.get(i.getAndIncrement())
              ));
              UncCore.cellActions.add(new CellAction(cell, in, 6){
                float p = pad + 30;
                float to = pad;
                float curr = pad + 30;
    
                @Override
                public void action(){
                  curr = p + (to - p)*progress;
                  cell.padTop(curr);
                }
              }.gradient(0.2f));
            };
          }).height(height[0]).growX();
          
          buildInfo.get(i.getAndIncrement());
        }));
      });
      
      items = itemEntrySeq;
      rebuild();
    }, e -> {
      infoTable.clearChildren();
      infoTable.add(Core.bundle.get("warn.publicInfo.connectFailed"));
      infoTable.row();
      infoTable.button(Core.bundle.get("misc.refresh"), this::refresh).size(140, 60);
      
      Log.err(e);
    });
    
    rebuild();
  }
}
