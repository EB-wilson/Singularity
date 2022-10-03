package singularity.graphic;

import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import singularity.Singularity;

public class SglDrawConst{
  public static final TextureRegion[] EMP_REGIONS = new TextureRegion[0];

  //Colors
  public static final Color transColor = new Color(0, 0, 0, 0);
  public static final Color matrixNet = Color.valueOf("D3FDFF");
  public static final Color matrixNetDark = Color.valueOf("9ECBCD");
  public static final Color ion = Color.valueOf("#D1D19F");

  //Text colors
  public static final String COLOR_ACCENT = "[accent]";

  //Texture regions
  public static TextureRegion transparent, sglLaunchLogo, squareMarker, matrixArrow, sglIcon, artistIcon, codeIcon, translateIcon,
      soundsIcon,

  startIcon, databaseIcon, publicInfoIcon, aboutIcon, configureIcon, contributeIcon, debuggingIcon, nuclearIcon, matrixIcon,

  qqIcon, telegramIcon;

  public static void load(){
    transparent = Singularity.getModAtlas("transparent");
    sglLaunchLogo = Singularity.getModAtlas("launch_logo");
    squareMarker = Singularity.getModAtlas("square_marker");
    matrixArrow = Singularity.getModAtlas("matrix_arrow");
    sglIcon = Singularity.getModAtlas("sgl_icon");
    artistIcon = Singularity.getModAtlas("artist");
    codeIcon = Singularity.getModAtlas("code");
    translateIcon = Singularity.getModAtlas("translate");
    soundsIcon = Singularity.getModAtlas("sound");
    startIcon = Singularity.getModAtlas("icon_start");
    databaseIcon = Singularity.getModAtlas("icon_database");
    publicInfoIcon = Singularity.getModAtlas("icon_publicInfo");
    aboutIcon = Singularity.getModAtlas("icon_about");
    configureIcon = Singularity.getModAtlas("icon_configure");
    contributeIcon = Singularity.getModAtlas("icon_contribute");
    debuggingIcon = Singularity.getModAtlas("debugging");
    nuclearIcon = Singularity.getModAtlas("nuclear");
    matrixIcon = Singularity.getModAtlas("matrix");
    qqIcon = Singularity.getModAtlas("qq");
    telegramIcon = Singularity.getModAtlas("telegram");
  }
}
