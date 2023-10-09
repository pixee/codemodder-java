package io.codemodder.plugins.maven.operator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import javax.xml.stream.XMLStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Chain {
  private static final Logger LOGGER = LoggerFactory.getLogger(Chain.class);

  /** Internal ArrayList of the Commands */
  private List<Command> commandList;

  // Constructor that takes an array of CommandJ
  public Chain(Command... commands) {
    this.commandList = new ArrayList<>(Arrays.asList(commands));
  }

  public List<Command> getCommandList() {
    return commandList;
  }

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

  public static Chain filterByQueryType(
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

  public static Chain createForDependencyQuery(QueryType queryType) {
    return filterByQueryType(
        AVAILABLE_DEPENDENCY_QUERY_COMMANDS,
        queryType,
        List.of(CheckLocalRepositoryDirCommand.CheckParentDirCommand.getInstance()),
        it -> it == queryType);
  }

  public static Chain createForVersionQuery(QueryType queryType) {
    return filterByQueryType(
        AVAILABLE_QUERY_VERSION_COMMANDS,
        queryType,
        Collections.emptyList(),
        it -> it.ordinal() <= queryType.ordinal());
  }

  public static final List<Pair<QueryType, String>> AVAILABLE_DEPENDENCY_QUERY_COMMANDS =
      new ArrayList<>(
          Arrays.asList(
              new Pair<>(QueryType.SAFE, "QueryByResolver"),
              new Pair<>(QueryType.SAFE, "QueryByParsing"),
              new Pair<>(QueryType.UNSAFE, "QueryByEmbedder"),
              new Pair<>(QueryType.UNSAFE, "QueryByInvoker")));

  public static final List<Pair<QueryType, String>> AVAILABLE_QUERY_VERSION_COMMANDS =
      new ArrayList<>(
          Arrays.asList(
              new Pair<>(QueryType.NONE, "UnwrapEffectivePom"),
              new Pair<>(QueryType.SAFE, "VersionByCompilerDefinition"),
              new Pair<>(QueryType.SAFE, "VersionByProperty")));

  public interface QueryTypeFilter {
    boolean filter(QueryType queryType);
  }
}
