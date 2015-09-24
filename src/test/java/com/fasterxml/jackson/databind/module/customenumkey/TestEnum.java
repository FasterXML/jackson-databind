/******************************************************************************
 * * This data and information is proprietary to, and a valuable trade secret
 * * of, Basis Technology Corp.  It is given in confidence by Basis Technology
 * * and may only be used as permitted under the license agreement under which
 * * it has been distributed, and in no other way.
 * *
 * * Copyright (c) 2015 Basis Technology Corporation All rights reserved.
 * *
 * * The technical data and information provided herein are provided with
 * * `limited rights', and the computer software provided herein is provided
 * * with `restricted rights' as those terms are defined in DAR and ASPR
 * * 7-104.9(a).
 ******************************************************************************/

package com.fasterxml.jackson.databind.module.customenumkey;

/**
 *
 */
public enum TestEnum {
    RED("red"),
    GREEN("green");

    private final String code;

    TestEnum(String code) {
        this.code = code;
    }

    public static TestEnum lookup(String lower) {
        for (TestEnum item : values()) {
            if (item.code().equals(lower)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Invalid code " + lower);
        }

    public String code() {
        return code;
    }
}
