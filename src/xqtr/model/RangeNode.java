package xqtr.model;

import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Element;

public class RangeNode extends ParameterNode {

	RangeNode(Element parameterNode, HashMap<String, String> variables) {
		this.initializeAttributes(parameterNode, variables);
	}

	protected  List<String> attributesKeys() {
		return super.attributesKeys();
	}

}