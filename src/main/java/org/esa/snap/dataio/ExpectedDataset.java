package org.esa.snap.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.util.StringUtils;

public class ExpectedDataset {

    @JsonProperty(required = true)
    private String id;

    @JsonProperty(required = true)
    private String decodeQualification;

    @JsonProperty
    private ExpectedContent expectedContent;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDecodeQualification(String decodeQualification) {
        this.decodeQualification = decodeQualification;
    }

    public ExpectedContent getExpectedContent() {
        return expectedContent;
    }

    public void setExpectedContent(ExpectedContent expectedContent) {
        this.expectedContent = expectedContent;
    }

    public DecodeQualification getDecodeQualification() {
        if (StringUtils.isNullOrEmpty(decodeQualification)) {
            return DecodeQualification.UNABLE;
        }

        if ("SUITABLE".equalsIgnoreCase(decodeQualification)) {
            return DecodeQualification.SUITABLE;
        }
        if ("INTENDED".equalsIgnoreCase(decodeQualification)) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }
}
