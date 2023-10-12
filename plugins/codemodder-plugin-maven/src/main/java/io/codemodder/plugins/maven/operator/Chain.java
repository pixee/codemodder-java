package io.codemodder.plugins.maven.operator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import javax.xml.stream.XMLStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a Chain of Responsibility Pattern
 *
 * @constructor commands: Commands to Use
 */
class Chain {
  private static final Logger LOGGER = LoggerFactory.getLogger(Chain.class);

  /** Internal ArrayList of the Commands */
  private List<Command> commandList;

  private Chain(Command... commands) {
    this.commandList = new ArrayList<>(Arrays.asList(commands));
  }

  public List<Command> getCommandList() {
    return commandList;
  }

  /**
   * Executes the Commands in the Chain of Responsibility
   *
   * @param c ProjectModel (context)
   * @return Boolean if successful
   */
  public boolean execute(ProjectModel c)
      throws URISyntaxException, IOException, XMLStreamException {
    boolean done = false;
    ListIterator<Command> listIterator = commandList.listIterator();

    while (!done && listIterator.hasNext()) {
      Command nextCommand = listIterator.next();
      done = nextCommand.execute(c);

      if (done) {
        if (c.getQueryType() == QueryType.NONE && !(nextCommand instanceof SupportCommand)) {
          c.setModifiedByCommand(true);
        }

        c.setFinishedByClass(nextCommand.getClass().getName());

        break;
      }
    }

    boolean result = done;

    /** Goes Reverse Order applying the filter pattern */
    while (listIterator.previousIndex() > 0) {
      Command nextCommand = listIterator.previous();
      done = nextCommand.postProcess(c);

      if (done) {
        break;
      }
    }

    return result;
  }

  /** Returns a Pre-Configured Chain with the Defaults for Modifying a POM */
  public static Chain createForModify() {
    return new Chain(
        CheckDependencyPresent.getInstance(),
        CheckParentPackaging.getInstance(),
        new FormatCommand(),
        DiscardFormatCommand.getInstance(),
        new CompositeDependencyManagement(),
        SimpleUpgrade.getInstance(),
        SimpleDependencyManagement.getInstance(),
        new SimpleInsert());
  }

  private static Chain filterByQueryType(
      List<Pair<QueryType, String>> commandList,
      QueryType queryType,
      List<AbstractQueryCommand> initialCommands,
      QueryTypeFilter queryTypeFilter) {
    List<Command> filteredCommands = new ArrayList<>();
    for (Pair<QueryType, String> pair : commandList) {
      if (queryTypeFilter.filter(pair.getFirst())) {
        String commandClassName = "io.codemodder.plugins.maven.operator." + pair.getSecond();

        try {
          Class<?> commandClass = Class.forName(commandClassName);
          Command command = (Command) commandClass.newInstance();
          filteredCommands.add(command);
        } catch (Throwable e) {
          LOGGER.warn("Creating class '{}': ", commandClassName, e);
        }
      }
    }

    List<Command> commands = new ArrayList<>();
    commands.addAll(initialCommands);
    commands.addAll(filteredCommands);

    if (commands.isEmpty()) {
      throw new IllegalStateException(
          "Unable to load any available strategy for " + queryType.name());
    }

    return new Chain(commands.toArray(new Command[0]));
  }

  /** returns a pre-configured chain with the defaults for Dependency Querying */
  public static Chain createForDependencyQuery(QueryType queryType) {
    return filterByQueryType(
        AVAILABLE_DEPENDENCY_QUERY_COMMANDS,
        queryType,
        Arrays.asList(CheckLocalRepositoryDirCommand.CheckParentDirCommand.getInstance()),
        it -> it == queryType);
  }

  /** returns a pre-configured chain for Version Query */
  public static Chain createForVersionQuery(QueryType queryType) {
    return filterByQueryType(
        AVAILABLE_QUERY_VERSION_COMMANDS,
        queryType,
        Collections.emptyList(),
        it -> it.ordinal() <= queryType.ordinal());
  }

  /**
   * Some classes won't have all available dependencies on the classpath during runtime for this
   * reason we'll use
   *
   * <pre>Class.forName</pre>
   *
   * and report issues creating
   */
  static final List<Pair<QueryType, String>> AVAILABLE_DEPENDENCY_QUERY_COMMANDS =
      new ArrayList<>(
          Arrays.asList(
              new Pair<>(QueryType.SAFE, "QueryByResolver"),
              new Pair<>(QueryType.SAFE, "QueryByParsing"),
              new Pair<>(QueryType.UNSAFE, "QueryByEmbedder"),
              new Pair<>(QueryType.UNSAFE, "QueryByInvoker")));

  /** List of Commands for Version Query */
  private static final List<Pair<QueryType, String>> AVAILABLE_QUERY_VERSION_COMMANDS =
      new ArrayList<>(
          Arrays.asList(
              new Pair<>(QueryType.NONE, "UnwrapEffectivePom"),
              new Pair<>(QueryType.SAFE, "VersionByCompilerDefinition"),
              new Pair<>(QueryType.SAFE, "VersionByProperty")));

  private interface QueryTypeFilter {
    boolean filter(QueryType queryType);
  }
}
