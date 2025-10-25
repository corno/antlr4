package org.antlr.v4.tool;

import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.tool.ast.ActionAST;
import org.antlr.v4.tool.ast.GrammarAST;
import org.antlr.v4.tool.ast.TerminalAST;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exports ANTLR Grammar objects to JSON format using a DOM-based approach.
 * 
 * This implementation builds up a JSON DOM structure first, then serializes it.
 * Tagged union states are converted to arrays only during serialization.
 */
public class GrammarJSONExporterDOM {
    
    /**
     * JSON DOM Value types
     */
    public static abstract class Value {
        public abstract String serialize(int indentLevel);
        public abstract String serializeAstn(int indentLevel);
        
        protected String indent(int level) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < level; i++) {
                sb.append("  ");
            }
            return sb.toString();
        }
    }
    
    public static class ArrayValue extends Value {
        private final List<Value> elements = new ArrayList<>();
        
        public void add(Value value) {
            elements.add(value);
        }
        
        @Override
        public String serialize(int indentLevel) {
            if (elements.isEmpty()) {
                return "[]";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < elements.size(); i++) {
                if (i > 0) sb.append(",\n");
                sb.append(indent(indentLevel + 1));
                sb.append(elements.get(i).serialize(indentLevel + 1));
            }
            sb.append("\n").append(indent(indentLevel)).append("]");
            return sb.toString();
        }
        
        @Override
        public String serializeAstn(int indentLevel) {
            if (elements.isEmpty()) {
                return "[]";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < elements.size(); i++) {
                if (i > 0) sb.append("\n");  // No commas in ASTN
                sb.append(indent(indentLevel + 1));
                sb.append(elements.get(i).serializeAstn(indentLevel + 1));
            }
            sb.append("\n").append(indent(indentLevel)).append("]");
            return sb.toString();
        }
    }
    
    public static class StringValue extends Value {
        private final String value;
        
        public StringValue(String value) {
            this.value = value;
        }
        
        @Override
        public String serialize(int indentLevel) {
            if (value == null) return "null";
            return "\"" + value.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
        }
        
        @Override
        public String serializeAstn(int indentLevel) {
            if (value == null) return "null";
            return "\"" + value.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
        }
    }
    
    public static class NumberValue extends Value {
        private final Number value;
        
        public NumberValue(Number value) {
            this.value = value;
        }
        
        @Override
        public String serialize(int indentLevel) {
            return value.toString();
        }
        
        @Override
        public String serializeAstn(int indentLevel) {
            return value.toString();
        }
    }
    
    public static class VerboseTypeValue extends Value {
        private final Map<String, Value> properties = new LinkedHashMap<>();
        
        public void put(String key, Value value) {
            properties.put(key, value);
        }
        
        public void put(String key, String value) {
            properties.put(key, new StringValue(value));
        }
        
        public void put(String key, Number value) {
            properties.put(key, new NumberValue(value));
        }
        
        @Override
        public String serialize(int indentLevel) {
            if (properties.isEmpty()) {
                return "{}";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<String, Value> entry : properties.entrySet()) {
                if (i > 0) sb.append(",\n");
                sb.append(indent(indentLevel + 1));
                sb.append("\"").append(entry.getKey()).append("\": ");
                sb.append(entry.getValue().serialize(indentLevel + 1));
                i++;
            }
            sb.append("\n").append(indent(indentLevel)).append("}");
            return sb.toString();
        }
        
        @Override
        public String serializeAstn(int indentLevel) {
            if (properties.isEmpty()) {
                return "()";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("(\n");
            int i = 0;
            for (Map.Entry<String, Value> entry : properties.entrySet()) {
                if (i > 0) sb.append("\n");  // No commas in ASTN
                sb.append(indent(indentLevel + 1));
                sb.append("'").append(entry.getKey()).append("': ");  // Apostrophes for keys
                sb.append(entry.getValue().serializeAstn(indentLevel + 1));
                i++;
            }
            sb.append("\n").append(indent(indentLevel)).append(")");
            return sb.toString();
        }
    }
    
    public static class DictionaryValue extends Value {
        private final Map<String, Value> entries = new LinkedHashMap<>();
        
        public void put(String key, Value value) {
            entries.put(key, value);
        }
        
        public void put(String key, String value) {
            entries.put(key, new StringValue(value));
        }
        
        public void put(String key, Number value) {
            entries.put(key, new NumberValue(value));
        }
        
        @Override
        public String serialize(int indentLevel) {
            if (entries.isEmpty()) {
                return "{}";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<String, Value> entry : entries.entrySet()) {
                if (i > 0) sb.append(",\n");
                sb.append(indent(indentLevel + 1));
                sb.append("\"").append(entry.getKey()).append("\": ");
                sb.append(entry.getValue().serialize(indentLevel + 1));
                i++;
            }
            sb.append("\n").append(indent(indentLevel)).append("}");
            return sb.toString();
        }
        
        @Override
        public String serializeAstn(int indentLevel) {
            if (entries.isEmpty()) {
                return "{}";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<String, Value> entry : entries.entrySet()) {
                if (i > 0) sb.append("\n");  // No commas in ASTN
                sb.append(indent(indentLevel + 1));
                sb.append("`").append(entry.getKey()).append("`: ");  // Backticks for dictionary keys
                sb.append(entry.getValue().serializeAstn(indentLevel + 1));
                i++;
            }
            sb.append("\n").append(indent(indentLevel)).append("}");
            return sb.toString();
        }
    }
    
    public static class StateValue extends Value {
        private final String name;
        private final Value value;
        
        public StateValue(String name, Value value) {
            this.name = name;
            this.value = value;
        }
        
        @Override
        public String serialize(int indentLevel) {
            // Serialize tagged union state as array: [name, value]
            ArrayValue array = new ArrayValue();
            array.add(new StringValue(name));
            array.add(value);
            return array.serialize(indentLevel);
        }
        
        @Override
        public String serializeAstn(int indentLevel) {
            // Serialize tagged union state as: | 'state_name' ...data...
            StringBuilder sb = new StringBuilder();
            sb.append("| '").append(name).append("' ");
            sb.append(value.serializeAstn(indentLevel));
            return sb.toString();
        }
    }
    
    /**
     * Convert a Grammar object to JSON string
     */
    public String exportGrammar(Grammar grammar) {
        Value grammarValue = convertGrammar(grammar);
        return grammarValue.serialize(0);
    }
    
    /**
     * Convert a Grammar object to ASTN string
     */
    public String exportGrammarAstn(Grammar grammar) {
        Value grammarValue = convertGrammar(grammar);
        return grammarValue.serializeAstn(0);
    }
    
    /**
     * Convert Grammar to Value DOM
     */
    private VerboseTypeValue convertGrammar(Grammar grammar) {
        VerboseTypeValue obj = new VerboseTypeValue();
        
        // Basic properties
        obj.put("name", grammar.name);
        if (grammar.fileName != null) {
            obj.put("fileName", grammar.fileName);
        }
        
        // Grammar type (tagged union)
        obj.put("type", convertGrammarType(grammar));
        
        // Rules (dictionary)
        DictionaryValue rulesDict = new DictionaryValue();
        for (Rule rule : grammar.rules.values()) {
            rulesDict.put(rule.name, convertRule(rule));
        }
        obj.put("rules", rulesDict);
        
        // Token vocabulary (dictionaries)
        obj.put("tokenNameToTypeMap", convertStringToNumberMap(grammar.tokenNameToTypeMap));
        obj.put("stringLiteralToTypeMap", convertStringToNumberMap(grammar.stringLiteralToTypeMap));
        
        // Named actions (optional dictionary)
        if (grammar.namedActions != null && !grammar.namedActions.isEmpty()) {
            obj.put("namedActions", convertActionMap(grammar.namedActions));
        }
        
        // Imported grammars (optional)
        if (grammar.importedGrammars != null && !grammar.importedGrammars.isEmpty()) {
            ArrayValue importedArray = new ArrayValue();
            for (Grammar imported : grammar.importedGrammars) {
                importedArray.add(convertGrammar(imported));
            }
            obj.put("importedGrammars", importedArray);
        }
        
        return obj;
    }
    
    /**
     * Convert grammar type to StateValue (tagged union)
     */
    private StateValue convertGrammarType(Grammar grammar) {
        VerboseTypeValue typeData = new VerboseTypeValue();
        
        if (grammar.isLexer()) {
            // Add lexer-specific data like modes if available
            if (grammar instanceof LexerGrammar) {
                LexerGrammar lexer = (LexerGrammar) grammar;
                if (lexer.modes.size() > 1) { // DEFAULT_MODE is always present
                    DictionaryValue modesDict = new DictionaryValue();
                    for (String mode : lexer.modes.keySet()) {
                        ArrayValue modeRules = new ArrayValue();
                        // TODO: get actual rules for mode
                        modesDict.put(mode, modeRules);
                    }
                    typeData.put("modes", modesDict);
                }
            }
            return new StateValue("lexer", typeData);
        } else if (grammar.isParser()) {
            return new StateValue("parser", typeData);
        } else if (grammar.isCombined()) {
            // Add implicit lexer if available
            if (grammar.getImplicitLexer() != null) {
                typeData.put("implicitLexer", convertGrammar(grammar.getImplicitLexer()));
            }
            return new StateValue("combined", typeData);
        }
        
        return new StateValue("unknown", typeData);
    }
    
    /**
     * Convert Rule to VerboseTypeValue
     */
    private VerboseTypeValue convertRule(Rule rule) {
        VerboseTypeValue obj = new VerboseTypeValue();
        
        obj.put("name", rule.name);
        
        // Rule modifiers (fragment, etc.)
        List<String> modifiers = extractRuleModifiers(rule);
        if (!modifiers.isEmpty()) {
            ArrayValue modifiersArray = new ArrayValue();
            for (String modifier : modifiers) {
                modifiersArray.add(new StringValue(modifier));
            }
            obj.put("modifiers", modifiersArray);
        }
        
        // Rule arguments, returns, locals
        if (rule.args != null && rule.args.toString() != null) {
            obj.put("args", rule.args.toString());
        }
        if (rule.retvals != null && rule.retvals.toString() != null) {
            obj.put("returns", rule.retvals.toString());
        }
        if (rule.locals != null && rule.locals.toString() != null) {
            obj.put("locals", rule.locals.toString());
        }
        
        // Alternatives
        ArrayValue alternativesArray = new ArrayValue();
        for (int i = 1; i <= rule.numberOfAlts; i++) { // ANTLR uses 1-based indexing
            alternativesArray.add(convertAlternative(rule.alt[i]));
        }
        obj.put("alternatives", alternativesArray);
        
        // Named actions (dictionary)
        if (rule.namedActions != null && !rule.namedActions.isEmpty()) {
            obj.put("namedActions", convertActionMap(rule.namedActions));
        }
        
        // Exception handlers
        if (!rule.exceptions.isEmpty()) {
            ArrayValue exceptionsArray = new ArrayValue();
            for (GrammarAST exception : rule.exceptions) {
                exceptionsArray.add(new StringValue(exception.getText()));
            }
            obj.put("exceptions", exceptionsArray);
        }
        
        return obj;
    }
    
    /**
     * Convert Alternative to VerboseTypeValue
     */
    private VerboseTypeValue convertAlternative(Alternative alt) {
        VerboseTypeValue obj = new VerboseTypeValue();
        
        // Elements - walk the AST to get proper order and structure
        ArrayValue elementsArray = new ArrayValue();
        
        // If we have the AST, walk it to get proper element order
        if (alt.ast != null) {
            for (int i = 0; i < alt.ast.getChildCount(); i++) {
                Object child = alt.ast.getChild(i);
                if (child instanceof GrammarAST) {
                    Value elementValue = convertElementFromAST((GrammarAST) child);
                    if (elementValue != null) {
                        elementsArray.add(elementValue);
                    }
                }
            }
        } else {
            // Fallback: use the collections (less accurate but better than nothing)
            for (List<TerminalAST> tokens : alt.tokenRefs.values()) {
                for (TerminalAST token : tokens) {
                    elementsArray.add(convertTokenElement(token));
                }
            }
            for (List<GrammarAST> rules : alt.ruleRefs.values()) {
                for (GrammarAST rule : rules) {
                    elementsArray.add(convertRuleElement(rule));
                }
            }
        }
        
        obj.put("elements", elementsArray);
        
        // Actions (embedded actions in the alternative)
        if (!alt.actions.isEmpty()) {
            ArrayValue actionsArray = new ArrayValue();
            for (ActionAST action : alt.actions) {
                actionsArray.add(new StringValue(action.getText()));
            }
            obj.put("actions", actionsArray);
        }
        
        // Label - check if the alternative AST has a label
        if (alt.ast != null && alt.ast.altLabel != null) {
            obj.put("label", alt.ast.altLabel.getText());
        }
        
        return obj;
    }
    
    /**
     * Convert an AST node to a Value (potentially StateValue for elements)
     */
    private Value convertElementFromAST(GrammarAST ast) {
        if (ast == null) return null;
        
        int nodeType = ast.getType();
        switch (nodeType) {
            case ANTLRParser.TOKEN_REF:
            case ANTLRParser.STRING_LITERAL:
                return convertTokenElementFromAST(ast);
            case ANTLRParser.RULE_REF:
                return convertRuleElementFromAST(ast);
            case ANTLRParser.ACTION:
                return convertActionElement(ast);
            case ANTLRParser.SEMPRED:
                return convertPredicateElement(ast);
            case ANTLRParser.SET:
                return convertSetElement(ast);
            case ANTLRParser.RANGE:
                return convertRangeElement(ast);
            case ANTLRParser.WILDCARD:
                return convertWildcardElement();
            case ANTLRParser.BLOCK:
                return convertBlockElement(ast);
            case ANTLRParser.PLUS:
            case ANTLRParser.STAR:
            case ANTLRParser.OPTIONAL:
                // These are EBNF suffixes, handle the child with suffix
                if (ast.getChildCount() > 0) {
                    GrammarAST child = (GrammarAST) ast.getChild(0);
                    return convertEBNFElement(ast, child);
                }
                break;
            case ANTLRParser.ALT:
                // Alternative nodes - recurse into children
                if (ast.getChildCount() == 1) {
                    return convertElementFromAST((GrammarAST) ast.getChild(0));
                }
                // Multiple children - convert as block
                return convertBlockElement(ast);
            case ANTLRParser.ELEMENT_OPTIONS:
                // Skip element options wrapper, get the actual element
                if (ast.getChildCount() > 0) {
                    return convertElementFromAST((GrammarAST) ast.getChild(0));
                }
                break;
            default:
                // For any other type, try to recurse into children if there's exactly one
                if (ast.getChildCount() == 1 && ast.getChild(0) instanceof GrammarAST) {
                    return convertElementFromAST((GrammarAST) ast.getChild(0));
                }
                // Skip unknown node types
                return null;
        }
        
        return null;
    }
    
    /**
     * Convert token element to StateValue: ["token", {...}]
     */
    private StateValue convertTokenElement(TerminalAST token) {
        return convertTokenElementFromAST(token);
    }
    
    private StateValue convertTokenElementFromAST(GrammarAST ast) {
        VerboseTypeValue data = new VerboseTypeValue();
        data.put("name", ast.getText());
        // TODO: Extract label if present
        return new StateValue("token", data);
    }
    
    /**
     * Convert rule element to StateValue: ["rule", {...}]
     */
    private StateValue convertRuleElement(GrammarAST rule) {
        return convertRuleElementFromAST(rule);
    }
    
    private StateValue convertRuleElementFromAST(GrammarAST ast) {
        VerboseTypeValue data = new VerboseTypeValue();
        data.put("name", ast.getText());
        // TODO: Extract arguments and label if present
        return new StateValue("rule", data);
    }
    
    /**
     * Convert action element: ["action", {...}]
     */
    private StateValue convertActionElement(GrammarAST ast) {
        VerboseTypeValue data = new VerboseTypeValue();
        data.put("code", ast.getText());
        return new StateValue("action", data);
    }
    
    /**
     * Convert predicate element: ["predicate", {...}]
     */
    private StateValue convertPredicateElement(GrammarAST ast) {
        VerboseTypeValue data = new VerboseTypeValue();
        data.put("code", ast.getText());
        return new StateValue("predicate", data);
    }
    
    /**
     * Convert set element: ["set", {...}]
     */
    private StateValue convertSetElement(GrammarAST ast) {
        VerboseTypeValue data = new VerboseTypeValue();
        
        // Convert child elements
        ArrayValue childElements = new ArrayValue();
        for (int i = 0; i < ast.getChildCount(); i++) {
            Object child = ast.getChild(i);
            if (child instanceof GrammarAST) {
                Value childValue = convertElementFromAST((GrammarAST) child);
                if (childValue != null) {
                    childElements.add(childValue);
                }
            }
        }
        data.put("elements", childElements);
        
        return new StateValue("set", data);
    }
    
    /**
     * Convert range element: ["range", {...}]
     */
    private StateValue convertRangeElement(GrammarAST ast) {
        VerboseTypeValue data = new VerboseTypeValue();
        
        if (ast.getChildCount() >= 2) {
            data.put("from", ast.getChild(0).toString());
            data.put("to", ast.getChild(1).toString());
        }
        
        return new StateValue("range", data);
    }
    
    /**
     * Convert wildcard element: ["wildcard", {}]
     */
    private StateValue convertWildcardElement() {
        VerboseTypeValue data = new VerboseTypeValue();
        return new StateValue("wildcard", data);
    }
    
    /**
     * Convert block element: ["block", {...}]
     */
    private StateValue convertBlockElement(GrammarAST ast) {
        VerboseTypeValue data = new VerboseTypeValue();
        
        // Convert alternatives within the block
        ArrayValue alternatives = new ArrayValue();
        
        for (int i = 0; i < ast.getChildCount(); i++) {
            Object child = ast.getChild(i);
            if (child instanceof GrammarAST) {
                GrammarAST childAST = (GrammarAST) child;
                if (childAST.getType() == ANTLRParser.ALT) {
                    // This is an alternative - convert it
                    VerboseTypeValue altObj = new VerboseTypeValue();
                    ArrayValue elements = new ArrayValue();
                    
                    // Process all children of the ALT node
                    for (int j = 0; j < childAST.getChildCount(); j++) {
                        Object altChild = childAST.getChild(j);
                        if (altChild instanceof GrammarAST) {
                            Value elementValue = convertElementFromAST((GrammarAST) altChild);
                            if (elementValue != null) {
                                elements.add(elementValue);
                            }
                        }
                    }
                    
                    altObj.put("elements", elements);
                    alternatives.add(altObj);
                }
            }
        }
        
        data.put("alternatives", alternatives);
        
        return new StateValue("block", data);
    }
    
    /**
     * Convert EBNF element: wraps child element with EBNF operator info
     */
    private StateValue convertEBNFElement(GrammarAST ebnfNode, GrammarAST child) {
        VerboseTypeValue data = new VerboseTypeValue();
        
        // Create a single alternative containing the child element
        ArrayValue alternatives = new ArrayValue();
        VerboseTypeValue alternative = new VerboseTypeValue();
        ArrayValue elements = new ArrayValue();
        
        Value childValue = convertElementFromAST(child);
        if (childValue != null) {
            elements.add(childValue);
        }
        
        alternative.put("elements", elements);
        alternatives.add(alternative);
        data.put("alternatives", alternatives);
        
        // Add EBNF operator
        String ebnfType = getEBNFType(ebnfNode.getType());
        if (ebnfType != null) {
            data.put("ebnf", ebnfType);
        }
        
        return new StateValue("block", data);
    }
    
    /**
     * Convert ANTLR EBNF type to string
     */
    private String getEBNFType(int ebnfType) {
        switch (ebnfType) {
            case ANTLRParser.PLUS:
                return "plus";
            case ANTLRParser.STAR:
                return "star";
            case ANTLRParser.OPTIONAL:
                return "optional";
            default:
                return null;
        }
    }
    
    /**
     * Extract rule modifiers like "fragment"
     */
    private List<String> extractRuleModifiers(Rule rule) {
        List<String> modifiers = new ArrayList<>();
        // Check if this is a fragment rule in a lexer grammar
        if (rule.g.isLexer() && rule.ast != null) {
            // Look for fragment keyword in rule definition
            String ruleText = rule.ast.toStringTree();
            if (ruleText.contains("fragment")) {
                modifiers.add("fragment");
            }
        }
        return modifiers;
    }
    
    /**
     * Convert Map<String, Integer> to DictionaryValue
     */
    private DictionaryValue convertStringToNumberMap(Map<String, Integer> map) {
        DictionaryValue dict = new DictionaryValue();
        if (map != null) {
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                dict.put(entry.getKey(), entry.getValue());
            }
        }
        return dict;
    }
    
    /**
     * Convert Map<String, ActionAST> to DictionaryValue
     */
    private DictionaryValue convertActionMap(Map<String, ActionAST> map) {
        DictionaryValue dict = new DictionaryValue();
        if (map != null) {
            for (Map.Entry<String, ActionAST> entry : map.entrySet()) {
                dict.put(entry.getKey(), entry.getValue().getText());
            }
        }
        return dict;
    }
}