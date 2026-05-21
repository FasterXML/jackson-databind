package tools.jackson.databind;

import java.util.function.Function;

public class JRefSetterFunction extends JRefSetter {

	private final Function<Object,Object> settingFunction;
	
	public JRefSetterFunction(Function<Object,Object> func) {
		this.settingFunction = func;
	}

	@Override
	protected Object setInstanceToValue(Object value) throws Throwable {
		return this.settingFunction.apply(value);
	}

}
