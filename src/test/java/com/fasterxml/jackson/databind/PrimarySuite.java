package com.fasterxml.jackson.databind;

import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.ExcludePackages;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("com.fasterxml.jackson.databind")
@ExcludePackages("com.fasterxml.jackson.databind.typepollution")
@ExcludeClassNamePatterns({
        "com\\.fasterxml\\.jackson\\.databind\\.MapperFootprintTest"
})
public class PrimarySuite {
}
