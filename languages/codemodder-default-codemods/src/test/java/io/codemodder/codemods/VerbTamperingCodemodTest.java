package io.codemodder.codemods;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.codemodder.ChangedFile;
import io.codemodder.CodemodInvoker;
import io.codemodder.FileWeavingContext;
import io.codemodder.IncludesExcludes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.xml.stream.XMLStreamException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

final class VerbTamperingCodemodTest {

  @ParameterizedTest
  @CsvSource({"on_own_line", "sharing_line", "sharing_line_with_others"})
  void it_removes_verb_tampering(final String webXmlDir) throws XMLStreamException, IOException {
    String dir = "src/test/resources/verb-tampering/" + webXmlDir;
    CodemodInvoker codemodInvoker =
        new CodemodInvoker(List.of(VerbTamperingCodemod.class), Path.of(dir));
    Path webxml = Path.of(dir, "web.xml");
    FileWeavingContext context =
        FileWeavingContext.createDefault(webxml.toFile(), IncludesExcludes.any());
    Optional<ChangedFile> changedFile = codemodInvoker.executeXmlFile(webxml, context);
    assertThat(changedFile.isPresent(), is(true));

    String modifiedFile = Files.readString(Path.of(changedFile.get().modifiedFile()));
    String expectedXml = Files.readString(Path.of(dir, "web-after-codemod.xml"));
    assertThat(modifiedFile, equalTo(expectedXml));
  }
}
