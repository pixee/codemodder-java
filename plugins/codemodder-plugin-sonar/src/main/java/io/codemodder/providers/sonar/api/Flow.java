package io.codemodder.providers.sonar.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Flow {
    @JsonProperty("locations")
    private List<Location> locations;

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }

    public static class Location {
        @JsonProperty("component")
        private String component;

        @JsonProperty("textRange")
        private TextRange textRange;

        @JsonProperty("msg")
        private String msg;

        public String getComponent() {
            return component;
        }

        public void setComponent(String component) {
            this.component = component;
        }

        public TextRange getTextRange() {
            return textRange;
        }

        public void setTextRange(TextRange textRange) {
            this.textRange = textRange;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
    }

    public static class TextRange {
        @JsonProperty("startLine")
        private int startLine;

        @JsonProperty("endLine")
        private int endLine;

        @JsonProperty("startOffset")
        private int startOffset;

        @JsonProperty("endOffset")
        private int endOffset;

        public int getStartLine() {
            return startLine;
        }

        public void setStartLine(int startLine) {
            this.startLine = startLine;
        }

        public int getEndLine() {
            return endLine;
        }

        public void setEndLine(int endLine) {
            this.endLine = endLine;
        }

        public int getStartOffset() {
            return startOffset;
        }

        public void setStartOffset(int startOffset) {
            this.startOffset = startOffset;
        }

        public int getEndOffset() {
            return endOffset;
        }

        public void setEndOffset(int endOffset) {
            this.endOffset = endOffset;
        }
    }
}

