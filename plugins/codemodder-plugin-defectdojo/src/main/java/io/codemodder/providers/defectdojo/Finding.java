package io.codemodder.providers.defectdojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/** Represents a finding in DefectDojo. */
public class Finding {

  @JsonProperty("id")
  private int id;

  @JsonProperty("title")
  private String title;

  @JsonProperty("description")
  private String description;

  @JsonProperty("severity")
  private String severity;

  @JsonProperty("cwe")
  private String cwe;

  @JsonProperty("file_path")
  private String filePath;

  @JsonProperty("line")
  private Integer line;

  @JsonProperty("unique_id_from_tool")
  private String uniqueIdFromTool;

  @JsonProperty("vuln_id_from_tool")
  private String vulnIdFromTool;

  @JsonProperty("references")
  private String references;

  @JsonProperty("sast_source_line")
  private Integer sastSourceLine;

  @JsonProperty("sast_source_file_path")
  private String sastSourceFilePath;

  public int getId() {
    return id;
  }

  public String getSeverity() {
    return severity;
  }

  public Integer getLine() {
    return line;
  }

  public String getReferences() {
    return references;
  }

  public String getCwe() {
    return cwe;
  }

  public String getDescription() {
    return description;
  }

  public String getUniqueIdFromTool() {
    return uniqueIdFromTool;
  }

  /** Appears to be the vulnerability's rule ID in the original vendor. */
  public String getVulnIdFromTool() {
    return vulnIdFromTool;
  }

  /** Get the file path associated with this finding. */
  public String getFilePath() {
    return filePath;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Finding finding = (Finding) o;
    return id == finding.id
        && Objects.equals(title, finding.title)
        && Objects.equals(description, finding.description)
        && Objects.equals(severity, finding.severity)
        && Objects.equals(cwe, finding.cwe)
        && Objects.equals(filePath, finding.filePath)
        && Objects.equals(line, finding.line)
        && Objects.equals(uniqueIdFromTool, finding.uniqueIdFromTool)
        && Objects.equals(vulnIdFromTool, finding.vulnIdFromTool)
        && Objects.equals(references, finding.references)
        && Objects.equals(sastSourceLine, finding.sastSourceLine)
        && Objects.equals(sastSourceFilePath, finding.sastSourceFilePath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        title,
        description,
        severity,
        cwe,
        filePath,
        line,
        uniqueIdFromTool,
        vulnIdFromTool,
        references,
        sastSourceLine,
        sastSourceFilePath);
  }
}
