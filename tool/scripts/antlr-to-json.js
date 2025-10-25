#!/usr/bin/env node

/**
 * Standalone Node.js tool to convert ANTLR grammar files to JSON
 * 
 * This approach uses ANTLR4's Java tool under the hood and provides
 * a simple JavaScript interface.
 */

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

function printUsage() {
    console.log('Usage: antlr-to-json <grammar-file> [options]');
    console.log('');
    console.log('Options:');
    console.log('  -o, --output <file>    Output JSON file (default: stdout)');
    console.log('  -h, --help            Show this help message');
    console.log('');
    console.log('Examples:');
    console.log('  antlr-to-json MyGrammar.g4');
    console.log('  antlr-to-json MyGrammar.g4 -o grammar.json');
}

function main() {
    const args = process.argv.slice(2);
    
    if (args.length === 0 || args.includes('-h') || args.includes('--help')) {
        printUsage();
        process.exit(0);
    }
    
    let grammarFile = null;
    let outputFile = null;
    
    // Parse arguments
    for (let i = 0; i < args.length; i++) {
        const arg = args[i];
        
        if (arg === '-o' || arg === '--output') {
            if (i + 1 >= args.length) {
                console.error('Error: -o/--output requires a filename');
                process.exit(1);
            }
            outputFile = args[i + 1];
            i++; // Skip next argument
        } else if (!arg.startsWith('-')) {
            if (grammarFile === null) {
                grammarFile = arg;
            } else {
                console.error('Error: Multiple grammar files specified');
                process.exit(1);
            }
        } else {
            console.error(`Error: Unknown option ${arg}`);
            printUsage();
            process.exit(1);
        }
    }
    
    if (!grammarFile) {
        console.error('Error: No grammar file specified');
        printUsage();
        process.exit(1);
    }
    
    if (!fs.existsSync(grammarFile)) {
        console.error(`Error: Grammar file ${grammarFile} does not exist`);
        process.exit(1);
    }
    
    try {
        // Find ANTLR4 tool jar
        const antlrJar = findAntlrJar();
        
        // Create temporary Java file to run our exporter
        const javaCode = generateJavaExporter();
        const tempJavaFile = path.join(__dirname, 'temp', 'GrammarToJSON.java');
        const tempClassDir = path.join(__dirname, 'temp');
        
        // Ensure temp directory exists
        if (!fs.existsSync(tempClassDir)) {
            fs.mkdirSync(tempClassDir, { recursive: true });
        }
        
        // Write Java file
        fs.writeFileSync(tempJavaFile, javaCode);
        
        // Compile Java file
        execSync(`javac -cp "${antlrJar}" "${tempJavaFile}"`, { stdio: 'inherit' });
        
        // Run the exporter
        const command = `java -cp "${antlrJar}:${tempClassDir}" GrammarToJSON "${grammarFile}"`;
        const result = execSync(command, { encoding: 'utf8' });
        
        // Output result
        if (outputFile) {
            fs.writeFileSync(outputFile, result);
            console.log(`Grammar exported to ${outputFile}`);
        } else {
            console.log(result);
        }
        
        // Cleanup
        fs.unlinkSync(tempJavaFile);
        fs.unlinkSync(path.join(tempClassDir, 'GrammarToJSON.class'));
        
    } catch (error) {
        console.error('Error:', error.message);
        process.exit(1);
    }
}

function findAntlrJar() {
    // Try common locations for ANTLR4 jar
    const possiblePaths = [
        '/usr/local/lib/antlr-4.13.1-complete.jar',
        '/usr/share/java/antlr4-runtime.jar',
        process.env.ANTLR_JAR,
        path.join(process.env.HOME || '', '.antlr', 'antlr-4.13.1-complete.jar'),
        './antlr-4.13.1-complete.jar'
    ];
    
    for (const jarPath of possiblePaths) {
        if (jarPath && fs.existsSync(jarPath)) {
            return jarPath;
        }
    }
    
    throw new Error('ANTLR4 jar not found. Please set ANTLR_JAR environment variable or install ANTLR4.');
}

function generateJavaExporter() {
    return `
import org.antlr.v4.tool.*;
import java.io.*;

public class GrammarToJSON {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: GrammarToJSON <grammar-file>");
            System.exit(1);
        }
        
        String grammarFile = args[0];
        Grammar grammar = Grammar.load(grammarFile);
        
        if (grammar == null) {
            System.err.println("Could not load grammar: " + grammarFile);
            System.exit(1);
        }
        
        // Simple JSON export - you would use the GrammarJSONExporter here
        System.out.println("{\\"name\\": \\"" + grammar.name + "\\", \\"type\\": \\"" + grammar.getTypeString() + "\\"}");
    }
}`;
}

if (require.main === module) {
    main();
}

module.exports = { main };