package tools.jackson.databind.jsontype.subpackage;

import tools.jackson.databind.jsontype.TestSubtypesSubPackage;

//For [databind#4983]: `JsonTypeInfo.Id.MINIMAL_CLASS` generates invalid type on sub-package
public class SubCSubPackage extends TestSubtypesSubPackage.SuperType {
	public int c = 2;
}
