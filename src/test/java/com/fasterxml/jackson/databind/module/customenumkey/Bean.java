

package com.fasterxml.jackson.databind.module.customenumkey;

import java.io.File;
import java.util.Map;

/**
 *
 */
public class Bean {
    private File rootDirectory;
    private String licenseString;
    private Map<TestEnum, Map<String, String>> replacements;

    public File getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public String getLicenseString() {
        return licenseString;
    }

    public void setLicenseString(String licenseString) {
        this.licenseString = licenseString;
    }

    public Map<TestEnum, Map<String, String>> getReplacements() {
        return replacements;
    }

    public void setReplacements(Map<TestEnum, Map<String, String>> replacements) {
        this.replacements = replacements;
    }
}
