package io.codemodder.plugins.maven;

import io.codemodder.DependencyGAV;
import io.codemodder.DependencyUpdateResult;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.dom4j.DocumentException;

interface POMDependencyUpdater {

  DependencyUpdateResult execute(Path projectDir, Path file, List<DependencyGAV> dependencies)
      throws IOException, XMLStreamException, DocumentException, URISyntaxException;
}
