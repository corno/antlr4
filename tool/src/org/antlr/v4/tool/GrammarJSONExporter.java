package org.antlr.v4.tool;

import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.tool.ast.ActionAST;
import org.antlr.v4.tool.ast.GrammarAST;
import org.antlr.v4.tool.ast.TerminalAST;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Exports ANTLR Grammar objects to JSON format conforming to the TypeScript schema
 * defined in grammar-types.ts.
 * 
 * This implementation generates JSON manually without external dependencies.
 * 
 * Usage:
 *   GrammarJSONExporter exporter = new GrammarJSONExporter();
 *   String json = exporter.exportGrammar(grammar);
 */
public class GrammarJSONExporter {
    
    private int indentLevel = 0;
    private static final String INDENT = "  ";
    
    /**
     * Convert a Grammar object to JSON string
     */
    public String exportGrammar(Grammar grammar) {
        StringBuilder sb = new StringBuilder();
        convertGrammar(grammar, sb);
        return sb.toString();
    }
    
    /**
     * Convert Grammar to JSON
     */
    private void convertGrammar(Grammar grammar, StringBuilder sb) {
        sb.append("{\n");
        increaseIndent();
        
        // Basic properties
        addProperty(sb, "name", quote(grammar.name), true);
        if (grammar.fileName != null) {
            addProperty(sb, "fileName", quote(grammar.fileName), true);
        }
        
        // Grammar type
        addProperty(sb, "type", convertGrammarType(grammar), true);
        
        // Rules
        sb.append(getCurrentIndent()).append("\"rules\": {\n");
        increaseIndent();
        boolean firstRule = true;
        for (Rule rule : grammar.rules.values()) {
            if (!firstRule) sb.append(",\n");
            sb.append(getCurrentIndent()).append(quote(rule.name)).append(": ");
            convertRule(rule, sb);
            firstRule = false;
        }
        decreaseIndent();
        sb.append("\n").append(getCurrentIndent()).append("},\n");
        
        // Token vocabulary
        addProperty(sb, "tokenNameToTypeMap", convertStringToIntMap(grammar.tokenNameToTypeMap), true);
        addProperty(sb, "stringLiteralToTypeMap", convertStringToIntMap(grammar.stringLiteralToTypeMap), false);
        
        // Named actions (optional)
        if (grammar.namedActions != null && !grammar.namedActions.isEmpty()) {
            sb.append(",\n");
            addProperty(sb, "namedActions", convertActionMap(grammar.namedActions), false);
        }
        
        // Imported grammars (optional)
        if (grammar.importedGrammars != null && !grammar.importedGrammars.isEmpty()) {
            sb.append(",\n");
            sb.append(getCurrentIndent()).append("\"importedGrammars\": [\n");
            increaseIndent();
            for (int i = 0; i < grammar.importedGrammars.size(); i++) {
                if (i > 0) sb.append(",\n");
                sb.append(getCurrentIndent());
                convertGrammar(grammar.importedGrammars.get(i), sb);
            }
            decreaseIndent();
            sb.append("\n").append(getCurrentIndent()).append("]");
        }
        
        decreaseIndent();
        sb.append("\n").append(getCurrentIndent()).append("}");
    }
    
    /**
     * Convert grammar type to JSON array format: ["lexer"|"parser"|"combined", {...}]
     */
    private String convertGrammarType(Grammar grammar) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        increaseIndent();
        
        if (grammar.isLexer()) {
            sb.append(getCurrentIndent()).append("\"lexer\",\n");
            sb.append(getCurrentIndent()).append("{\n");
            increaseIndent();
            // Add lexer-specific data like modes if available
            if (grammar instanceof LexerGrammar) {
                LexerGrammar lexer = (LexerGrammar) grammar;
                if (lexer.modes.size() > 1) { // DEFAULT_MODE is always present
                    sb.append(getCurrentIndent()).append("\"modes\": {\n");
                    increaseIndent();
                    boolean firstMode = true;
                    for (String mode : lexer.modes.keySet()) {
                        if (!firstMode) sb.append(",\n");
                        sb.append(getCurrentIndent()).append(quote(mode)).append(": []"); // TODO: get rules for mode
                        firstMode = false;
                    }
                    decreaseIndent();
                    sb.append("\n").append(getCurrentIndent()).append("}\n");
                }
            }
            decreaseIndent();
            sb.append(getCurrentIndent()).append("}");
        } else if (grammar.isParser()) {
            sb.append(getCurrentIndent()).append("\"parser\",\n");
            sb.append(getCurrentIndent()).append("{}");
        } else if (grammar.isCombined()) {
            sb.append(getCurrentIndent()).append("\"combined\",\n");
            sb.append(getCurrentIndent()).append("{\n");
            increaseIndent();
            // Add implicit lexer if available
            if (grammar.getImplicitLexer() != null) {
                sb.append(getCurrentIndent()).append("\"implicitLexer\": ");
                convertGrammar(grammar.getImplicitLexer(), sb);
                sb.append("\n");
            }
            decreaseIndent();
            sb.append(getCurrentIndent()).append("}");
        }
        
        decreaseIndent();
        sb.append("\n").append(getCurrentIndent()).append("]");
        return sb.toString();
    }
    
    /**
     * Convert Rule to JSON
     */
    private void convertRule(Rule rule, StringBuilder sb) {
        sb.append("{\n");
        increaseIndent();
        
        addProperty(sb, "name", quote(rule.name), true);
        
        // Rule modifiers (fragment, etc.)
        List<String> modifiers = extractRuleModifiers(rule);
        if (!modifiers.isEmpty()) {
            addProperty(sb, "modifiers", convertStringArray(modifiers), true);
        }
        
        // Rule arguments, returns, locals
        if (rule.args != null && rule.args.toString() != null) {
            addProperty(sb, "args", quote(rule.args.toString()), true);
        }
        if (rule.retvals != null && rule.retvals.toString() != null) {
            addProperty(sb, "returns", quote(rule.retvals.toString()), true);
        }
        if (rule.locals != null && rule.locals.toString() != null) {
            addProperty(sb, "locals", quote(rule.locals.toString()), true);
        }
        
        // Alternatives
        sb.append(getCurrentIndent()).append("\"alternatives\": [\n");
        increaseIndent();
        for (int i = 1; i <= rule.numberOfAlts; i++) { // ANTLR uses 1-based indexing
            if (i > 1) sb.append(",\n");
            sb.append(getCurrentIndent());
            convertAlternative(rule.alt[i], sb);
        }
        decreaseIndent();
        sb.append("\n").append(getCurrentIndent()).append("]");
        
        // Named actions
        if (rule.namedActions != null && !rule.namedActions.isEmpty()) {
            sb.append(",\n");
            addProperty(sb, "namedActions", convertActionMap(rule.namedActions), false);
        }
        
        // Exception handlers
        if (!rule.exceptions.isEmpty()) {
            sb.append(",\n");
            List<String> exceptionStrings = new ArrayList<>();
            for (GrammarAST exception : rule.exceptions) {
                exceptionStrings.add(exception.getText());
            }
            addProperty(sb, "exceptions", convertStringArray(exceptionStrings), false);
        }
        
        decreaseIndent();
        sb.append("\n").append(getCurrentIndent()).append("}");
    }
    
    /**
     * Convert Alternative to JSON
     */
    private void convertAlternative(Alternative alt, StringBuilder sb) {
        sb.append("{\n");
        increaseIndent();
        
        // Elements - walk the AST to get proper order and structure
        sb.append(getCurrentIndent()).append("\"elements\": [\n");
        increaseIndent();
        
        List<String> elementJsons = new ArrayList<>();
        
        // If we have the AST, walk it to get proper element order
        if (alt.ast != null) {
            for (int i = 0; i < alt.ast.getChildCount(); i++) {
                Object child = alt.ast.getChild(i);
                if (child instanceof GrammarAST) {
                    String elementJson = convertElementFromAST((GrammarAST) child);
                    if (elementJson != null) {
                        elementJsons.add(elementJson);
                    }
                }
            }
        } else {
            // Fallback: use the collections (less accurate but better than nothing)
            for (List<TerminalAST> tokens : alt.tokenRefs.values()) {
                for (TerminalAST token : tokens) {
                    StringBuilder tokenSb = new StringBuilder();
                    convertTokenElement(token, tokenSb);
                    elementJsons.add(tokenSb.toString());
                }
            }
            for (List<GrammarAST> rules : alt.ruleRefs.values()) {
                for (GrammarAST rule : rules) {
                    StringBuilder ruleSb = new StringBuilder();
                    convertRuleElement(rule, ruleSb);
                    elementJsons.add(ruleSb.toString());
                }
            }
        }
        
        // Output all elements
        for (int i = 0; i < elementJsons.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append(getCurrentIndent()).append(elementJsons.get(i));
        }
        
        decreaseIndent();
        sb.append("\n").append(getCurrentIndent()).append("]");
        
        // Actions (embedded actions in the alternative)
        if (!alt.actions.isEmpty()) {
            sb.append(",\n");
            List<String> actionTexts = new ArrayList<>();
            for (ActionAST action : alt.actions) {
                actionTexts.add(action.getText());
            }
            addProperty(sb, "actions", convertStringArray(actionTexts), false);
        }
        
        // Label - check if the alternative AST has a label
        if (alt.ast != null && alt.ast.altLabel != null) {
            sb.append(",\n");
            addProperty(sb, "label", quote(alt.ast.altLabel.getText()), false);
        }
        
        decreaseIndent();
        sb.append("\n").append(getCurrentIndent()).append("}");
    }
    
    /**
     * Convert an AST node to a JSON element string
     */
    private String convertElementFromAST(GrammarAST ast) {
        if (ast == null) return null;
        
        StringBuilder sb = new StringBuilder();
        
        int nodeType = ast.getType();
        switch (nodeType) {
            case ANTLRParser.TOKEN_REF:
            case ANTLRParser.STRING_LITERAL:
                convertTokenElementFromAST(ast, sb);
                break;
            case ANTLRParser.RULE_REF:
                convertRuleElementFromAST(ast, sb);
                break;
            case ANTLRParser.ACTION:
                convertActionElement(ast, sb);
                break;
            case ANTLRParser.SEMPRED:
                convertPredicateElement(ast, sb);
                break;
            case ANTLRParser.SET:
                convertSetElement(ast, sb);
                break;
            case ANTLRParser.RANGE:
                convertRangeElement(ast, sb);
                break;
            case ANTLRParser.WILDCARD:
                convertWildcardElement(sb);
                break;
            case ANTLRParser.BLOCK:
                convertBlockElement(ast, sb);
                break;
            case ANTLRParser.PLUS:
            case ANTLRParser.STAR:
            case ANTLRParser.OPTIONAL:
                // These are EBNF suffixes, handle the child with suffix
                if (ast.getChildCount() > 0) {
                    GrammarAST child = (GrammarAST) ast.getChild(0);
                    convertEBNFElement(ast, child, sb);
                }
                break;
            default:
                // Skip unknown node types or try to convert children
                return null;
        }
        
        return sb.length() > 0 ? sb.toString() : null;
    }
    
    /**
     * Convert token element to JSON array format: ["token", {...}]
     */
    private void convertTokenElement(TerminalAST token, StringBuilder sb) {
        convertTokenElementFromAST(token, sb);
    }
    
    private void convertTokenElementFromAST(GrammarAST ast, StringBuilder sb) {
        sb.append("[\n");
        increaseIndent();
        sb.append(getCurrentIndent()).append("\"token\",\n");
        sb.append(getCurrentIndent()).append("{\n");
        increaseIndent();
        addProperty(sb, "name", quote(ast.getText()), false);
        // TODO: Extract label if present
        decreaseIndent();
        sb.append("\n").append(getCurrentIndent()).append("}\n");
        decreaseIndent();
        sb.append(getCurrentIndent()).append("]");
    }
    
    /**
     * Convert rule element to JSON array format: ["rule", {...}]
     */
    private void convertRuleElement(GrammarAST rule, StringBuilder sb) {
        convertRuleElementFromAST(rule, sb);
    }
    
    private void convertRuleElementFromAST(GrammarAST ast, StringBuilder sb) {
        sb.append("[\n");
        increaseIndent();
        sb.append(getCurrentIndent()).append("\"rule\",\n");
        sb.append(getCurrentIndent()).append("{\n");
        increaseIndent();
        addProperty(sb, "name", quote(ast.getText()), false);
        // TODO: Extract arguments and label if present
        decreaseIndent();
        sb.append("\n").append(getCurrentIndent()).append("}\n");
        decreaseIndent();
        sb.append(getCurrentIndent()).append("]");
    }
    
    /**
     * Convert action element: ["action", {...}]
     */
    private void convertActionElement(GrammarAST ast, StringBuilder sb) {
        sb.append("[\n");
        increaseIndent();
        sb.append(getCurrentIndent()).append("\"action\",\n");
        sb.append(getCurrentIndent()).append("{\n");
        increaseIndent();
        addProperty(sb, "code", quote(ast.getText()), false);
        decreaseIndent();
        sb.append("\n").append(getCurrentIndent()).append("}\n");
        decreaseIndent();
        sb.append(getCurrentIndent()).append("]");
    }
    
    /**
     * Convert predicate element: ["predicate", {...}]
     */
    private void convertPredicateElement(GrammarAST ast, StringBuilder sb) {
        sb.append("[\n");
        increaseIndent();
        sb.append(getCurrentIndent()).append("\"predicate\",\n");
        sb.append(getCurrentIndent()).append("{\n");
        increaseIndent();
        addProperty(sb, "code", quote(ast.getText()), false);
        decreaseIndent();
        sb.append("\n").append(getCurrentIndent()).append("}\n");
        decreaseIndent();
        sb.append(getCurrentIndent()).append("]");
    }
    
    /**
     * Convert set element: ["set", {...}]
     */
    private void convertSetElement(GrammarAST ast, StringBuilder sb) {
        sb.append("[\n");
        increaseIndent();
        sb.append(getCurrentIndent()).append("\"set\",\n");
        sb.append(getCurrentIndent()).append("{\n");
        increaseIndent();
        
        // Convert child elements
        List<String> childElements = new ArrayList<>();
        for (int i = 0; i < ast.getChildCount(); i++) {
            Object child = ast.getChild(i);
            if (child instanceof GrammarAST) {
                String childJson = convertElementFromAST((GrammarAST) child);
                if (childJson != null) {
                    childElements.add(childJson);
                }
            }
        }
        
        sb.append(getCurrentIndent()).append("\"elements\": [\n");
        increaseIndent();
        for (int i = 0; i < childElements.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append(getCurrentIndent()).append(childElements.get(i));
        }
        decreaseIndent();
        sb.append("\n").append(getCurrentIndent()).append("]\n");
        
        decreaseIndent();
        sb.append(getCurrentIndent()).append("}\n");
        decreaseIndent();
        sb.append(getCurrentIndent()).append("]");
    }
    
    /**
     * Convert range element: ["range", {...}]
     */
    private void convertRangeElement(GrammarAST ast, StringBuilder sb) {
        sb.append("[\n");
        increaseIndent();
        sb.append(getCurrentIndent()).append("\"range\",\n");
        sb.append(getCurrentIndent()).append("{\n");
        increaseIndent();
        
        if (ast.getChildCount() >= 2) {
            addProperty(sb, "from", quote(ast.getChild(0).toString()), true);
            addProperty(sb, "to", quote(ast.getChild(1).toString()), false);
        }
        
        decreaseIndent();
        sb.append("\n").append(getCurrentIndent()).append("}\n");
        decreaseIndent();
        sb.append(getCurrentIndent()).append("]");
    }
    
    /**
     * Convert wildcard element: ["wildcard", {}]
     */
    private void convertWildcardElement(StringBuilder sb) {
        sb.append("[\n");
        increaseIndent();
        sb.append(getCurrentIndent()).append("\"wildcard\",\n");
        sb.append(getCurrentIndent()).append("{}\n");
        decreaseIndent();
        sb.append(getCurrentIndent()).append("]");
    }
    
    /**
     * Convert EBNF element: wraps child element with EBNF operator info
     */
    private void convertEBNFElement(GrammarAST ebnfNode, GrammarAST child, StringBuilder sb) {
        // If the child is a block, we need to create a block element with EBNF
        if (child.getType() == ANTLRParser.BLOCK) {
            sb.append("[\n");
            increaseIndent();
            sb.append(getCurrentIndent()).append("\"block\",\n");
            sb.append(getCurrentIndent()).append("{\n");
            increaseIndent();
            
            // Convert block alternatives (simplified for now)
            sb.append(getCurrentIndent()).append("\"alternatives\": [],\n");
            
            // Add EBNF operator
            String ebnfType = getEBNFType(ebnfNode.getType());
            if (ebnfType != null) {
                addProperty(sb, "ebnf", quote(ebnfType), false);
            }
            
            decreaseIndent();
            sb.append(getCurrentIndent()).append("}\n");
            decreaseIndent();
            sb.append(getCurrentIndent()).append("]");
        } else {
            // For simple elements, we convert the child and note that it has EBNF
            // Since our schema doesn't support EBNF on individual tokens/rules,
            // we'll wrap it in a block with a single alternative
            sb.append("[\n");
            increaseIndent();
            sb.append(getCurrentIndent()).append("\"block\",\n");
            sb.append(getCurrentIndent()).append("{\n");
            increaseIndent();
            
            // Create a single alternative containing the child element
            sb.append(getCurrentIndent()).append("\"alternatives\": [\n");
            increaseIndent();
            sb.append(getCurrentIndent()).append("{\n");
            increaseIndent();
            sb.append(getCurrentIndent()).append("\"elements\": [\n");
            increaseIndent();
            
            String childJson = convertElementFromAST(child);
            if (childJson != null) {
                sb.append(getCurrentIndent()).append(childJson);
            }
            
            decreaseIndent();
            sb.append("\n").append(getCurrentIndent()).append("]\n");
            decreaseIndent();
            sb.append(getCurrentIndent()).append("}\n");
            decreaseIndent();
            sb.append(getCurrentIndent()).append("],\n");
            
            // Add EBNF operator
            String ebnfType = getEBNFType(ebnfNode.getType());
            if (ebnfType != null) {
                addProperty(sb, "ebnf", quote(ebnfType), false);
            }
            
            decreaseIndent();
            sb.append(getCurrentIndent()).append("}\n");
            decreaseIndent();
            sb.append(getCurrentIndent()).append("]");
        }
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
     * Convert block element: ["block", {...}]
     */
    private void convertBlockElement(GrammarAST ast, StringBuilder sb) {
        sb.append("[\n");
        increaseIndent();
        sb.append(getCurrentIndent()).append("\"block\",\n");
        sb.append(getCurrentIndent()).append("{\n");
        increaseIndent();
        
        // TODO: Convert alternatives within the block
        sb.append(getCurrentIndent()).append("\"alternatives\": []\n");
        
        decreaseIndent();
        sb.append(getCurrentIndent()).append("}\n");
        decreaseIndent();
        sb.append(getCurrentIndent()).append("]");
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
    
    // Helper methods for JSON generation
    private String getCurrentIndent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            sb.append(INDENT);
        }
        return sb.toString();
    }
    
    private void increaseIndent() {
        indentLevel++;
    }
    
    private void decreaseIndent() {
        indentLevel--;
    }
    
    private void addProperty(StringBuilder sb, String key, String value, boolean addComma) {
        sb.append(getCurrentIndent()).append(quote(key)).append(": ").append(value);
        if (addComma) sb.append(",");
        sb.append("\n");
    }
    
    private String quote(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
    
    private String convertStringArray(List<String> strings) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < strings.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(quote(strings.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }
    
    private String convertStringToIntMap(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        increaseIndent();
        if (map != null) {
            boolean first = true;
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                if (!first) sb.append(",\n");
                sb.append(getCurrentIndent()).append(quote(entry.getKey())).append(": ").append(entry.getValue());
                first = false;
            }
            sb.append("\n");
        }
        decreaseIndent();
        sb.append(getCurrentIndent()).append("}");
        return sb.toString();
    }
    
    private String convertStringToStringMap(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        increaseIndent();
        if (map != null) {
            boolean first = true;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (!first) sb.append(",\n");
                sb.append(getCurrentIndent()).append(quote(entry.getKey())).append(": ").append(quote(entry.getValue()));
                first = false;
            }
            sb.append("\n");
        }
        decreaseIndent();
        sb.append(getCurrentIndent()).append("}");
        return sb.toString();
    }
    
    private String convertActionMap(Map<String, ActionAST> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        increaseIndent();
        if (map != null) {
            boolean first = true;
            for (Map.Entry<String, ActionAST> entry : map.entrySet()) {
                if (!first) sb.append(",\n");
                sb.append(getCurrentIndent()).append(quote(entry.getKey())).append(": ").append(quote(entry.getValue().getText()));
                first = false;
            }
            sb.append("\n");
        }
        decreaseIndent();
        sb.append(getCurrentIndent()).append("}");
        return sb.toString();
    }
}