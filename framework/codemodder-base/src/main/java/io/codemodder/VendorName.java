package io.codemodder;

public enum VendorName {
  SEMGREP("Semgrep"),
  SONAR("Sonar"),
  DEFECT_DOJO("DefectDojo / Semgrep");

  private final String name;

  VendorName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
