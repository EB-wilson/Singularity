package singularity.ui;

import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.scene.Element;
import arc.scene.ui.Label;
import arc.util.Align;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;

public class Markdown extends Element{
  private final Node node;
  private Markdown.MarkdownStyle style;

  public Markdown(String md, MarkdownStyle style){
    node = Parser.builder().build().parse(md);
    this.style = style;
  }

  @Override
  public void draw() {
    node.accept(new AbstractVisitor() {
      int padding;
      float rendOff = 0, lineOff = 0;

      @Override
      public void visit(BlockQuote blockQuote) {
        padding += 4;
        super.visit(blockQuote);
        padding -= 4;
      }

      @Override
      public void visit(BulletList bulletList) {
        padding += 2;
        super.visit(bulletList);
        padding -= 2;
      }

      @Override
      public void visit(Code code) {
        throw new IllegalArgumentException("not support raw html");
      }

      @Override
      public void visit(Emphasis emphasis) {
        
      }

      @Override
      public void visit(FencedCodeBlock fencedCodeBlock) {
        super.visit(fencedCodeBlock);
      }

      @Override
      public void visit(HardLineBreak hardLineBreak) {
        super.visit(hardLineBreak);
      }

      @Override
      public void visit(Heading heading) {
        super.visit(heading);
      }

      @Override
      public void visit(ThematicBreak thematicBreak) {
        super.visit(thematicBreak);
      }

      @Override
      public void visit(HtmlInline htmlInline) {
        throw new IllegalArgumentException("not support raw html");
      }

      @Override
      public void visit(HtmlBlock htmlBlock) {
        throw new IllegalArgumentException("not support raw html");
      }

      @Override
      public void visit(Image image) {
        super.visit(image);
      }

      @Override
      public void visit(IndentedCodeBlock indentedCodeBlock) {
        super.visit(indentedCodeBlock);
      }

      @Override
      public void visit(Link link) {
        super.visit(link);
      }

      @Override
      public void visit(ListItem listItem) {
        super.visit(listItem);
      }

      @Override
      public void visit(OrderedList orderedList) {
        super.visit(orderedList);
      }

      @Override
      public void visit(Paragraph paragraph) {
        super.visit(paragraph);
      }

      @Override
      public void visit(SoftLineBreak softLineBreak) {
        super.visit(softLineBreak);
      }

      @Override
      public void visit(StrongEmphasis strongEmphasis) {
        super.visit(strongEmphasis);
      }

      @Override
      public void visit(Text text) {
        super.visit(text);
      }

      @Override
      public void visit(LinkReferenceDefinition linkReferenceDefinition) {
        throw new UnsupportedOperationException("unsupported");
      }
    });
    super.draw();
  }

  public static class MarkdownStyle {
    public Font font;
    public float linesPadding;
  }
}
