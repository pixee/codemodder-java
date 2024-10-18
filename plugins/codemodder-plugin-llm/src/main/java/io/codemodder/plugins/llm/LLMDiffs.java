package io.codemodder.plugins.llm;

import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utilities for working with diff patches returned by an LLM. */
public final class LLMDiffs {

  private static final Pattern HUNK_RANGE_PATTERN =
      Pattern.compile("^@@\\s+-(\\d+)(?:,\\d+)?\\s+\\+\\d+(?:,\\d+)?\\s+@@");

  private static final int MAX_FUZZ = 3;

  private LLMDiffs() {} // Prevent instantiation.

  /**
   * Applies a diff in unified format to {@code target}.
   *
   * <p>If the LLM was handed code with unusual indentation, it can struggle to create a diff patch
   * with that indentation preserved. This will attempt to fix the indentation of the original lines
   * in the diff patch before passing it to {@link UnifiedDiffUtils} for parsing.
   *
   * @param target The target.
   * @param diff The diff.
   * @return The patched target.
   * @throws IllegalArgumentException If the diff cannot be applied to {@code target}.
   */
  public static List<String> applyDiff(final List<String> target, final String diff) {
    try {
      List<String> fixedDiff = fixDiffWhitespace(target, List.of(diff.strip().split("\n")));
      Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(fixedDiff);
      return patch.applyFuzzy(target, MAX_FUZZ);
    } catch (PatchFailedException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Fixes the whitespace in a diff so that it matches the target.
   *
   * @param target The target.
   * @param diff The diff.
   * @return The diff with its whitespace fixed.
   */
  private static List<String> fixDiffWhitespace(
      final List<String> target, final List<String> diff) {
    List<String> fixedDiff = new ArrayList<>();

    boolean inHeader = true;
    List<String> hunk = new ArrayList<>();
    int start = 0;

    for (String line : diff) {
      if (inHeader) {
        // Append the header.
        fixedDiff.add(line);
        if (line.startsWith("+++")) {
          inHeader = false;
        }
        continue;
      }

      Matcher m = HUNK_RANGE_PATTERN.matcher(line);
      if (m.find()) {
        // If we've found a new hunk, fix and append the previous hunk.
        if (!hunk.isEmpty()) {
          fixedDiff.addAll(fixHunkWhitespace(target, hunk, start));
          hunk.clear();
        }

        // Append the hunk range (minus the section heading, which UnifiedDiffUtils chokes on).
        fixedDiff.add(m.group(0));

        // Save the starting line number for the new hunk.
        start = Integer.parseInt(m.group(1));
      } else {
        hunk.add(!line.isEmpty() ? line : " ");
      }
    }

    // Fix and append the final hunk.
    fixedDiff.addAll(fixHunkWhitespace(target, hunk, start));

    return List.copyOf(fixedDiff);
  }

  /**
   * Fixes the whitespace in a change hunk so that it matches the target.
   *
   * <p>For example, given these inconsistently-indented lines from the target (3 spaces, 9 spaces,
   * and 2 spaces respectively):
   *
   * <pre>
   *    else {
   *          response.sendRedirect("LoginPage?error=Unauthorized Access");
   *   }
   * </pre>
   *
   * <p>And this consistently-indented hunk from the LLM:
   *
   * <pre>
   *      else {
   * +        System.out.println("Failed login attempt for user: " + user);
   *          response.sendRedirect("LoginPage?error=Unauthorized Access");
   *      }
   * </pre>
   *
   * <p>This will restore the inconsistent indentation of the target so that the hunk can be
   * applied:
   *
   * <pre>
   *     else {
   * +        System.out.println("Failed login attempt for user: " + user);
   *           response.sendRedirect("LoginPage?error=Unauthorized Access");
   *    }
   * </pre>
   *
   * @param target The target.
   * @param hunk The change hunk.
   * @param start The starting line number (1-based).
   * @return The hunk with its whitespace fixed.
   */
  private static List<String> fixHunkWhitespace(
      final List<String> target, final List<String> hunk, final int start) {
    int index = -1;

    // The starting line number from the LLM can be slightly off -- this is usually observed when
    // a file starts with blank lines -- so we need to find the actual starting line number.
    for (int i = start > MAX_FUZZ ? start - MAX_FUZZ - 1 : 0;
        i < start + MAX_FUZZ && i < target.size();
        i++) {
      if (isStart(target, hunk, i)) {
        index = i;
        break;
      }
    }

    // We couldn't find the matching lines. Let the patch fail. :(
    if (index == -1) {
      return hunk;
    }

    List<String> fixedHunk = new ArrayList<>(hunk.size());
    for (String line : hunk) {
      if (line.startsWith("+")) {
        fixedHunk.add(line);
      } else {
        fixedHunk.add(line.charAt(0) + target.get(index++));
      }
    }

    return List.copyOf(fixedHunk);
  }

  /**
   * Determines whether {@code index} is the starting index of a change hunk.
   *
   * @param target The target.
   * @param hunk The change hunk.
   * @param index The index to check (0-based).
   * @return {@code true} if {@code index} is the starting index; otherwise, {@code false}.
   */
  private static boolean isStart(
      final List<String> target, final List<String> hunk, final int index) {
    int adds = 0;

    for (int i = 0; i < hunk.size(); i++) {
      // Skip adds.
      if (hunk.get(i).startsWith("+")) {
        adds++;
        continue;
      }

      int targetIndex = index + i - adds;
      if (targetIndex >= target.size()) {
        return false;
      }

      // If the lines don't match with whitespace ignored, it wasn't the starting index.
      if (!hunk.get(i).substring(1).strip().equals(target.get(targetIndex).strip())) {
        return false;
      }
    }

    return true;
  }
}
