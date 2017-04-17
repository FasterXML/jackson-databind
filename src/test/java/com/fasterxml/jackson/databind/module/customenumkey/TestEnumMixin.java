

package com.fasterxml.jackson.databind.module.customenumkey;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = TestEnumSerializer.class, keyUsing = TestEnumKeySerializer.class)
@JsonDeserialize(using = TestEnumDeserializer.class, keyUsing = TestEnumKeyDeserializer.class)
public enum TestEnumMixin {
}
