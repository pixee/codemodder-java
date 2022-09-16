package io.pixee.codetl;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A componentized rule ID representing the 3 parts of the rule ID:
 *
 * the organization / namespace
 * the language
 * the rule category
 *
 * Consider the following example:
 * <code>pixee:java/reflected-xss</code>
 *
 * The organization: pixee
 * The language: java
 * The rule category: reflected-xss
 */
public final class RuleId {

    private final String namespace;
    private final String subjectLanguage;
    private final String category;

    private RuleId(final String namespace, final String subjectLanguage, final String category) {
        this.namespace = Objects.requireNonNull(namespace).trim();
        this.subjectLanguage = Objects.requireNonNull(subjectLanguage).trim();
        this.category = Objects.requireNonNull(category).trim();
    }

    public static RuleId fromRawRuleId(final String rawStringRuleId) {
        Matcher matcher = rulePattern.matcher(rawStringRuleId);
        if(!matcher.matches()) {
            throw new IllegalArgumentException("rule ID format invalid");
        }
        return new RuleId(matcher.group(1), matcher.group(2), matcher.group(3));
    }

    public String getNamespace() {
        return namespace;
    }

    public String getSubjectLanguage() {
        return subjectLanguage;
    }

    public String getCategory() {
        return category;
    }

    public String toIdentifier() {
        return namespace + ":" + subjectLanguage + "/" + category;
    }

    @Override
    public String toString() {
        return "RuleId{" +
                "namespace='" + namespace + '\'' +
                ", subjectLanguage='" + subjectLanguage + '\'' +
                ", category='" + category + '\'' +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RuleId ruleId = (RuleId) o;
        return namespace.equals(ruleId.namespace) && subjectLanguage.equals(ruleId.subjectLanguage) && category.equals(ruleId.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, subjectLanguage, category);
    }

    private static final Pattern rulePattern = Pattern.compile("([a-zA-Z0-9]+):(java|python|helloworld)/([a-zA-Z0-9\\-_]+)");
}
