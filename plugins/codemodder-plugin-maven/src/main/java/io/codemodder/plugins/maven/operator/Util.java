package io.codemodder.plugins.maven.operator;

import com.github.zafarkhaja.semver.Version;
import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Text;
import org.dom4j.tree.DefaultText;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.dom4j.Dom4jXPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common Utilities */
class Util {

  private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

  /** Represents a Property Reference - as a regex */
  private static final Pattern PROPERTY_REFERENCE_PATTERN = Pattern.compile("^\\$\\{(.*)}$");

  /**
   * Method that easily allows to add an element inside another while retaining formatting
   *
   * @param element Element
   * @param d ProjectModel / Context
   * @param name new element ("tag") name
   * @return created element inside `this` object, already indented after and (optionally) before
   */
  static Element addIndentedElement(Element element, POMDocument d, String name) {
    List<Node> contentList = element.content();

    int indentLevel = findIndentLevel(element);

    String prefix = d.getEndl() + StringUtils.repeat(d.getIndent(), 1 + indentLevel);

    String suffix = d.getEndl() + StringUtils.repeat(d.getIndent(), indentLevel);

    if (!contentList.isEmpty() && contentList.get(contentList.size() - 1) instanceof Text) {
      Text lastElement = (Text) contentList.get(contentList.size() - 1);

      if (StringUtils.isWhitespace(lastElement.getText())) {
        contentList.remove(contentList.size() - 1);
      }
    }

    contentList.add(new DefaultText(prefix));

    Element newElement = element.addElement(name);

    contentList.add(new DefaultText(suffix));

    d.setDirty(true);

    return newElement;
  }

  /**
   * Guesses the current indent level of the nearest nodes
   *
   * @return indent level
   */
  private static int findIndentLevel(Element startingNode) {
    int level = 0;
    Element node = startingNode;

    while (node.getParent() != null) {
      level += 1;
      node = node.getParent();
    }

    return level;
  }

  /** Given a Version Node, upgrades a resulting POM */
  static void upgradeVersionNode(
      ProjectModel c, Element versionNode, POMDocument pomDocumentHoldingProperty) {
    if (c.isUseProperties()) {
      String propertyName = propertyName(c, versionNode);

      // Define property
      upgradeProperty(c, pomDocumentHoldingProperty, propertyName);

      versionNode.setText(escapedPropertyName(propertyName));
    } else {
      if (c.getDependency() != null && c.getDependency().getVersion() != null) {
        String nodeText = versionNode.getText();
        String trimmedText = (nodeText != null) ? nodeText.trim() : "";

        if (!trimmedText.equals(c.getDependency().getVersion())) {
          pomDocumentHoldingProperty.setDirty(true);
          versionNode.setText(c.getDependency().getVersion());
        }
      }
    }
  }

  /** Upserts a given property */
  private static void upgradeProperty(ProjectModel c, POMDocument d, String propertyName) {
    if (d.getResultPom().getRootElement().element("properties") == null) {
      addIndentedElement(d.getResultPom().getRootElement(), d, "properties");
    }

    Element parentPropertyElement = d.getResultPom().getRootElement().element("properties");

    if (parentPropertyElement.element(propertyName) == null) {
      addIndentedElement(parentPropertyElement, d, propertyName);
    } else {
      if (!c.isOverrideIfAlreadyExists()) {

        Pattern propertyReferencePattern = Pattern.compile("\\$\\{" + propertyName + "}");

        Matcher matcher = propertyReferencePattern.matcher(d.getResultPom().asXML());
        int numberOfAllCurrentMatches = 0;

        while (matcher.find()) {
          numberOfAllCurrentMatches++;
        }

        if (numberOfAllCurrentMatches > 1) {
          throw new IllegalStateException(
              "Property " + propertyName + " is already defined - and used more than once.");
        }
      }
    }

    Element propertyElement = parentPropertyElement.element(propertyName);

    String propertyText =
        (propertyElement.getText() != null) ? propertyElement.getText().trim() : "";

    if (c.getDependency() != null
        && c.getDependency().getVersion() != null
        && !propertyText.equals(c.getDependency().getVersion())) {
      propertyElement.setText(c.getDependency().getVersion());
      d.setDirty(true);
    }
  }

  /** Escapes a Property Name */
  private static String escapedPropertyName(String propertyName) {
    return "${" + propertyName + "}";
  }

  /** Creates a property Name */
  static String propertyName(ProjectModel c, Element versionNode) {
    String version = versionNode.getTextTrim();

    if (PROPERTY_REFERENCE_PATTERN.matcher(version).matches()) {
      Matcher matcher = PROPERTY_REFERENCE_PATTERN.matcher(version);

      if (matcher.find()) {
        return matcher.group(1);
      }
    }

    if (c.getDependency() != null) {
      c.getDependency().getArtifactId();
      return "versions." + c.getDependency().getArtifactId();
    }

    return "versions.default";
  }

  /** Identifies if an upgrade is needed */
  static boolean findOutIfUpgradeIsNeeded(ProjectModel c, Element versionNode) {
    String currentVersionNodeText = resolveVersion(c, versionNode.getText());

    Version currentVersion = Version.valueOf(currentVersionNodeText);
    Version newVersion = Version.valueOf(c.getDependency().getVersion());

    boolean versionsAreIncreasing = newVersion.greaterThan(currentVersion);

    return versionsAreIncreasing;
  }

  private static String resolveVersion(ProjectModel c, String versionText) {
    if (PROPERTY_REFERENCE_PATTERN.matcher(versionText).matches()) {
      StrSubstitutor substitutor = new StrSubstitutor(c.resolvedProperties());
      String resolvedVersion = substitutor.replace(versionText);
      return resolvedVersion;
    } else {
      return versionText;
    }
  }

  /**
   * Builds a Lookup Expression String for a given dependency
   *
   * @param dependency Dependency
   */
  static String buildLookupExpressionForDependency(Dependency dependency) {
    return "/m:project"
        + "/m:dependencies"
        + "/m:dependency"
        + "[./m:groupId[text()='"
        + dependency.getGroupId()
        + "'] and "
        + "./m:artifactId[text()='"
        + dependency.getArtifactId()
        + "']]";
  }

  /**
   * Builds a Lookup Expression String for a given dependency, but under the
   * &gt;dependencyManagement&gt; section
   *
   * @param dependency Dependency
   */
  static String buildLookupExpressionForDependencyManagement(Dependency dependency) {
    return "/m:project"
        + "/m:dependencyManagement"
        + "/m:dependencies"
        + "/m:dependency"
        + "[./m:groupId[text()='"
        + dependency.getGroupId()
        + "'] and "
        + "./m:artifactId[text()='"
        + dependency.getArtifactId()
        + "']]";
  }

  static File which(String path) {
    List<String> nativeExecutables;
    if (SystemUtils.IS_OS_WINDOWS) {
      nativeExecutables = new ArrayList<>();
      nativeExecutables.add("");
      nativeExecutables.add(".exe");
      nativeExecutables.add(".bat");
      nativeExecutables.add(".cmd");
      nativeExecutables.replaceAll(ext -> path + ext);
    } else {
      nativeExecutables = Arrays.asList(path);
    }

    String pathContentString = System.getenv("PATH");

    String[] pathElements = pathContentString.split(File.pathSeparator);

    List<File> possiblePaths = new ArrayList<>();
    for (String executable : nativeExecutables) {
      for (String pathElement : pathElements) {
        possiblePaths.add(new File(new File(pathElement), executable));
      }
    }

    Predicate<File> isCliCallable =
        SystemUtils.IS_OS_WINDOWS
            ? it -> it.exists() && it.isFile()
            : it -> it.exists() && it.isFile() && it.canExecute();

    File result =
        possiblePaths.stream()
            .filter(isCliCallable)
            .reduce((first, second) -> second) // Find last
            .orElse(null);

    if (result == null) {
      LOGGER.warn(
          "Unable to find mvn executable (execs: {}, path: {})",
          String.join("/", nativeExecutables),
          pathContentString);
    }

    return result;
  }

  /**
   * Extension Function to Select the XPath Nodes
   *
   * @param expression expression to use
   */
  static List<Node> selectXPathNodes(Node node, String expression) {
    try {
      return createXPathExpression(expression).selectNodes(node);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** Hard-Coded POM Namespace Map */
  private static final SimpleNamespaceContext namespaceContext;

  static {
    Map<String, String> namespaces = new HashMap<>();
    namespaces.put("m", "http://maven.apache.org/POM/4.0.0");
    namespaceContext = new SimpleNamespaceContext(namespaces);
  }

  /**
   * Creates a XPath Expression from a given expression string
   *
   * @param expression expression to create xpath from
   */
  private static Dom4jXPath createXPathExpression(String expression) throws Exception {
    Dom4jXPath xpath = new Dom4jXPath(expression);
    xpath.setNamespaceContext(namespaceContext);
    return xpath;
  }
}
