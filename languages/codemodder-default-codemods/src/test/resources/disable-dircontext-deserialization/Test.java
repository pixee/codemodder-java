package something;

import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;

public final class Test {

  public void directInstantiation() {
    DirContext ctx = new InitialDirContext();
    ctx.search("query", "filter", new SearchControls(0, 0, 0, null, true, false));
  }

  public void doublyDirectInstantiation() {
    new InitialDirContext()
        .search("query", "filter", new SearchControls(0, 0, 0, null, true, false));
  }

  public void indirectInstantiation() {
    DirContext ctx = new InitialDirContext();
    SearchControls controls = new SearchControls(0, 0, 0, null, true, false);
    ctx.search("query", "filter", controls);
  }

  public void anotherTypeOfCall() {
    DirContext ctx = new InitialDirContext();
    SearchControls controls = new SearchControls(0, 0, 0, null, true, false);
    ctx.search("query", "filter", null, controls);
  }

  public void outsideControls(SearchControls controls) {
    DirContext ctx = new InitialDirContext();
    ctx.search("query", "filter", controls);
  }

  public void noControlsNoFix() {
    DirContext ctx = new InitialDirContext();
    ctx.search("name", null, new String[0]);
  }

  public void indirectInstantiation() {
    DirContext ctx = new InitialDirContext();
    SearchControls controls = new SearchControls(0, 0, 0, null, false, false);
    ctx.search("query", "filter", controls);
  }

}
