<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:pom="http://maven.apache.org/POM/4.0.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output omit-xml-declaration="yes"/>
    <xsl:preserve-space elements="*"/>
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/pom:project[not(dependencies)]">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:text disable-output-escaping="yes">%PIXEE_DEPENDENCY_INFO%
            </xsl:text>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="dependencies[not(dependency[@groupId='io.github.pixee'])]">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:text disable-output-escaping="yes">%PIXEE_DEPENDENCY_INFO%
            </xsl:text>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>