package org.antlr.v4.tool;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Command-line tool to convert ANTLR grammar files to JSON or ASTN format.
 * Generates both AST and ATN representations as separate files.
 * 
 * Usage:
 *   java GrammarToJSON [--json] input.g4 [output.base]
 * 
 * Options:
 *   --json        Output in JSON format (default is ASTN format)
 *   input.g4      ANTLR grammar file to convert
 *   output.base   Output base path (generates .ast.astn and .atn.astn files)
 *                 If not provided, outputs to stdout (AST only for compatibility)
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
            
            // Create exporters
            GrammarJSONExporterDOM astExporter = new GrammarJSONExporterDOM();
            GrammarATNExporterDOM atnExporter = new GrammarATNExporterDOM();
            
            // Generate outputs
            String astOutput = jsonFormat ? 
                astExporter.exportGrammar(grammar) : 
                astExporter.exportGrammarAstn(grammar);
            
            String atnOutput = jsonFormat ? 
                atnExporter.exportATN(grammar) : 
                atnExporter.exportATNAstn(grammar);
            
            // Output the results
            if (outputFile != null) {
                String extension = jsonFormat ? ".json" : ".astn";
                String astFile = outputFile.replaceAll("\\.[^.]*$", "") + ".ast" + extension;
                String atnFile = outputFile.replaceAll("\\.[^.]*$", "") + ".atn" + extension;
                
                // Write AST file
                try (FileWriter writer = new FileWriter(astFile)) {
                    writer.write(astOutput);
                    String format = jsonFormat ? "JSON" : "ASTN";
                    System.out.println("Grammar AST exported to " + astFile + " in " + format + " format");
                }
                
                // Write ATN file
                try (FileWriter writer = new FileWriter(atnFile)) {
                    writer.write(atnOutput);
                    String format = jsonFormat ? "JSON" : "ASTN";
                    System.out.println("Grammar ATN exported to " + atnFile + " in " + format + " format");
                }
            } else {
                // For backward compatibility, output AST to stdout when no output file specified
                System.out.println(astOutput);
            }
            
        } catch (Exception e) {
            System.err.println("Error processing grammar: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.err.println("Usage: java GrammarToJSON [--json] input.g4 [output.base]");
        System.err.println("Options:");
        System.err.println("  --json        Output in JSON format (default is ASTN format)");
        System.err.println("  input.g4      ANTLR grammar file to convert");
        System.err.println("  output.base   Output base path (generates .ast.astn and .atn.astn files)");
        System.err.println("                If not provided, outputs AST to stdout for compatibility");
    }
}