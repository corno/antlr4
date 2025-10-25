package org.antlr.v4.tool;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Command-line tool to convert ANTLR grammar files to JSON or ASTN format.
 * 
 * Usage:
 *   java GrammarToJSON [--json] input.g4 [output.json]
 * 
 * Options:
 *   --json      Output in JSON format (default is ASTN format)
 *   input.g4    ANTLR grammar file to convert
 *   output.json Output file (optional, defaults to stdout)
 */
public class GrammarToJSON {
    
    public static void main(String[] args) {
        boolean jsonFormat = false;
        String inputFile = null;
        String outputFile = null;
        
        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--json".equals(arg)) {
                jsonFormat = true;
            } else if (inputFile == null) {
                inputFile = arg;
            } else if (outputFile == null) {
                outputFile = arg;
            } else {
                System.err.println("Too many arguments");
                printUsage();
                System.exit(1);
            }
        }
        
        if (inputFile == null) {
            System.err.println("Missing input file");
            printUsage();
            System.exit(1);
        }
        
        try {
            // Load the grammar using ANTLR's built-in functionality
            Grammar grammar = Grammar.load(inputFile);
            
            if (grammar == null) {
                System.err.println("Error: Could not load grammar from " + inputFile);
                System.exit(1);
            }
            
            // Convert to JSON or ASTN
            GrammarJSONExporterDOM exporter = new GrammarJSONExporterDOM();
            String output = jsonFormat ? 
                exporter.exportGrammar(grammar) : 
                exporter.exportGrammarAstn(grammar);
            
            // Output the result
            if (outputFile != null) {
                try (FileWriter writer = new FileWriter(outputFile)) {
                    writer.write(output);
                    String format = jsonFormat ? "JSON" : "ASTN";
                    System.out.println("Grammar exported to " + outputFile + " in " + format + " format");
                }
            } else {
                System.out.println(output);
            }
            
        } catch (Exception e) {
            System.err.println("Error processing grammar: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.err.println("Usage: java GrammarToJSON [--json] input.g4 [output.file]");
        System.err.println("Options:");
        System.err.println("  --json      Output in JSON format (default is ASTN format)");
        System.err.println("  input.g4    ANTLR grammar file to convert");
        System.err.println("  output.file Output file (optional, defaults to stdout)");
    }
}