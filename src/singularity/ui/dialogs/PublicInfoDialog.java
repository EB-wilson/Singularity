package singularity.ui.dialogs;

import arc.Core;
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
import arc.util.Interval;
import arc.util.Log;
import singularity.Sgl;
import universeCore.UncCore;
import universeCore.util.animLayout.CellAction;
import universeCore.util.animLayout.CellChangeColorAction;
import universeCore.util.ini.Ini;

import java.util.regex.Pattern;

import static universeCore.util.ini.IniTypes.*;

public class PublicInfoDialog extends BaseListDialog{
  private static final String langRegex = "#locale#";
  private static final Pattern imagePattern = Pattern.compile("<image *=.*>");
  
  public PublicInfoDialog(){
    super(Core.bundle.get("misc.publicInfo"));
    
    shown(this::rebuild);
    build();
  }
  
  public void build(){
    String directory = Sgl.publicInfo + "directory.ini";
    Seq<ItemEntry> itemEntrySeq = new Seq<>();
    
    Http.get(directory, request -> {
      Ini dire = new Ini();
      dire.parse(request.getResultAsString());
      
      dire.eachAll((name, sect) -> {
        Seq<Element> elements = new Seq<>();
        ObjectMap<String, TextureRegion> atlas = new ObjectMap<>();
        String[] title = {""};
  
        IniMap assets = (IniMap)sect.get("assets");
        assets.each((n, image) -> {
          if(((String)image.get()).contains("https://")){
            Http.get(image.get(), result -> {
              if(!atlas.containsKey(n)){
                atlas.put(n, Core.atlas.find("nomap"));
                Http.get(image.get(), res -> {
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
                }, err -> {});
              }
            }, e -> {});
          }
          else atlas.put(n, Core.atlas.find(image.get()));
        });
        
        IniArray arr = ((IniArray)sect.get("languages"));
        ObjectSet<String> languages = new ObjectSet<>();
        
        for(IniObject lang: arr){
          languages.add(lang.get());
        }
        
        String currLang = Core.settings.getString("locale");
        
        String language = !currLang.equals("en") && languages.contains(currLang)? currLang: "";
        String url = ((String)sect.get("info").get()).replace(langRegex, language);
        
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
              elements.add(text);
            }
            
            height[0] += pad;
          }
        }, e -> {});
        
        itemEntrySeq.add(new ItemEntry(table -> table.add(title[0]), table -> {
          Interval timer = new Interval();
          table.setHeight(height[0]);
          int i = 0;
          while(i < elements.size){
            if(timer.get(15)){
              Cell<?> cell = table.add(elements.get(i)).padTop(pad + 30).color(elements.get(i).color.cpy().a(0));
              table.row();
              UncCore.cellActions.add(new CellChangeColorAction(cell, table, elements.get(i).color.cpy().a(1), 10));
              UncCore.cellActions.add(new CellAction(){
                float p = pad + 30;
                float to = pad;
                float curr = pad + 30;
                
                @Override
                public void action(){
                  curr = p + (to - p)*progress;
                  cell.padTop(curr);
                }
              }.gradient(0.2f));
              i++;
            }
          }
        }));
      });
    }, e -> {});
    
    rebuild();
  }
}
