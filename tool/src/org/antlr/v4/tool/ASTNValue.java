package org.antlr.v4.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON/ASTN DOM Value types for serialization.
 * 
 * This provides a reusable set of value types that can serialize to both
 * JSON and ASTN (Abstract Syntax Tree Notation) formats.
 */
public class ASTNValue {
    
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
}