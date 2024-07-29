package org.esa.snap.dataio;


import com.fasterxml.jackson.annotation.JsonProperty;

class TestProduct {

    @JsonProperty(required = true)
    private String id;
    @JsonProperty(required = true)
    private String relativePath;
    @JsonProperty()
    private String description;
    @JsonProperty()
    private String[] disabledForPlatforms;

    private transient boolean exists = true;

    String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    String getRelativePath() {
        return relativePath;
    }

    String getDescription() {
        return description;
    }

    void exists(boolean exists) {
        this.exists = exists;
    }

    boolean exists() {
        return exists;
    }

    boolean isDifferent(TestProduct other) {
        if (!(id.equals(other.getId()) &&
                relativePath.equals(other.getRelativePath()))) {
            return true;
        }
        return false;
    }

    boolean isEnabled() {
        for (String disabledForPlatform : this.disabledForPlatforms) {
            if (System.getProperty("os.arch").equalsIgnoreCase(disabledForPlatform)) {
                return false;
            }
        }
        return true;
    }
}
