package xqtr.model;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import xqtr.Application;

public abstract class ModelNode {

	private static FileWriter fw; //No se si hace falta tenerlo en una variable para que no se lo lleve el gc.
	private static PrintWriter errorPw = null;
	protected HashMap<String, String> attributes = new HashMap<String, String>();

	protected static final String programTag = "program";
	protected static final String profileTag = "profile";
	protected static final String parameterTag = "parameter";
	protected static final String variableTag = "var";
	protected static final String xmlVersion = "1.0";

	protected String getAttribute(String key) {
		return attributes.get(key);
	}

	protected void setAttribute(String key, String value) {
		attributes.put(key, value);
	}

	protected Boolean hasAttribute(String attributeName) {
		return attributes.containsKey(attributeName);
	}

	protected Boolean openErrorLogFile() {
		
		File errorLogFile = new File(Application.properties.get("error.log.path"));

		try {
			fw = new FileWriter(errorLogFile, true); //True para usar append
			errorPw = new PrintWriter(fw);
		} catch (Exception e) {
			errorPw = null;
			return false;
		}

		return true;
	}

	protected void closeErrorLogFile() {
		if(errorPw != null) errorPw.close();
	}

	protected void logError(String error) {
		if(errorPw != null) {
			errorPw.println(LocalDateTime.now().toString() + ":" + error);
		}
	}

	protected String replaceVariables(String string, HashMap<String, String> variables){

		StringBuffer replacedString = new StringBuffer();
		Pattern variablePattern = Pattern.compile("(?:\\{)([^}]*)(?:\\})"),
				idPattern = Pattern.compile("(_?[A-Za-z][A-Za-z0-9]*)"),
				equationPattern = Pattern.compile("^([0-9]+\\s*[-+*/]\\s*[0-9]+(\\s*[-+*/]\\s*[0-9]+)*)$");
		ScriptEngineManager mgr = new ScriptEngineManager();
	    ScriptEngine engine = mgr.getEngineByName("JavaScript");
		Matcher variableMatcher = variablePattern.matcher(string); 

		while(variableMatcher.find()) {

			StringBuffer resultBuffer = new StringBuffer();
			String resultReplacement;
			Matcher idMatcher = idPattern.matcher(variableMatcher.group(1)), equationMatcher;

			//Reemplazo las variables con los ids que tenga en el HashMap.
			while(idMatcher.find()) {
				if(variables.containsKey(idMatcher.group(1)))
					idMatcher.appendReplacement(resultBuffer, variables.get(idMatcher.group(1)));
				else {
					this.setUnexecutable("la variable " + idMatcher.group(1) + " no pudo ser reemplazada");
					idMatcher.appendReplacement(resultBuffer, "");
				}
			}

			idMatcher.appendTail(resultBuffer);
			resultReplacement = resultBuffer.toString();

			//Si lo que quedo es una ecuacion, resuelvo la ecuacion.
			equationMatcher = equationPattern.matcher(resultBuffer);
			if(equationMatcher.matches())
				try {
					resultReplacement = engine.eval(resultReplacement).toString();
				} catch (ScriptException e1) {
					e1.printStackTrace();
					this.setUnexecutable(e1.getMessage());
				}

			variableMatcher.appendReplacement(replacedString, resultReplacement);
		}

		variableMatcher.appendTail(replacedString);

		return replacedString.toString();
	}

	protected Boolean isValidVariableId(String id) {
		Pattern pattern = Pattern.compile("^(_?[A-Za-z][A-Za-z0-9]*)$");
	    return pattern.matcher(id).matches();
	}

	protected HashMap<String, String> getVariables(Element node){

		HashMap<String, String> variables = new HashMap<String, String>();

		this.elementList(node.getElementsByTagName(variableTag)).forEach((variable) -> {
			
			String id = variable.getAttribute("id"),
				value = variable.getAttribute("value");

			/*TODO encontrar la manera de tener aca las vriables
			 * anteriores para poder reemplazar en las actuales. Por ahora
			 * no puede haber variables de variables.
			 */
			if(this.isValidVariableId(id))
				variables.put(id, this.replaceVariables(value, variables));
			else
				this.logError("Invalid variable Id: " + id);
		});

		return variables;
	}

	protected List<Element> elementList(NodeList list) {

		List<Element> elementList = new LinkedList<Element>();

		for(int i=0; i<list.getLength(); i++) {
			elementList.add((Element) list.item(i));
		}

		return elementList;
	}

	protected HashMap<String, String> deepCopyVariables (HashMap<String, String> originalVariables){

		HashMap<String, String> newVariables = new HashMap<String, String>();

		for(Entry<String, String> e : originalVariables.entrySet()) {

			 newVariables.put(e.getKey(), e.getValue());
		}

		return newVariables;
	}

	protected void initializeAttributes(Element node, HashMap<String, String> variables) {

		this.attributesKeys().forEach(attributeKey -> {
			if(node.hasAttribute(attributeKey))
				attributes.put(attributeKey, this.replaceVariables(node.getAttribute(attributeKey), variables));
		});

	}

	protected  List<String> attributesKeys() {

		LinkedList<String> attributesKeys = new LinkedList<String>();

		attributesKeys.add("name");
		attributesKeys.add("class");

		return attributesKeys;
	}

	protected String getCommand() {
		//Este metodo se redefine en RootNode, Program y Profile. No se usa en Parameter.
		return "";
	}

	//Por polimorfismo para el reemplazo de variables
	protected void setUnexecutable(String motivo) {
	}

}
