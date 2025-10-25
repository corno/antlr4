#!/bin/bash

# Script to convert all ANTLR grammars from grammars-v4 to ASTN format
# Maintains the same directory structure in ./grammars

set -e  # Exit on any error

# Configuration
SOURCE_DIR="../grammars-v4"
OUTPUT_DIR="../grammars-astn"
JAR_PATH="tool/target/antlr4-4.13.3-SNAPSHOT-complete.jar"
CONVERTER_CLASS="org.antlr.v4.tool.GrammarToJSON"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Check if source directory exists
if [ ! -d "$SOURCE_DIR" ]; then
    print_status $RED "Error: Source directory $SOURCE_DIR not found!"
    print_status $YELLOW "Please make sure grammars-v4 is checked out as a sibling directory."
    exit 1
fi

# Check if JAR exists
if [ ! -f "$JAR_PATH" ]; then
    print_status $RED "Error: ANTLR tool JAR not found at $JAR_PATH"
    print_status $YELLOW "Please run 'mvn package' in the tool directory first."
    exit 1
fi

# Create output directory
mkdir -p "$OUTPUT_DIR"

print_status $BLUE "Starting grammar conversion..."
print_status $BLUE "Source: $SOURCE_DIR"
print_status $BLUE "Output: $OUTPUT_DIR"
echo

# Counters
total_files=0
converted_files=0
failed_files=0

# Find all .g4 files and process them
while IFS= read -r -d '' grammar_file; do
    # Get relative path from source directory
    relative_path="${grammar_file#$SOURCE_DIR/}"
    
    # Create output path with .astn extension
    output_file="$OUTPUT_DIR/${relative_path%.g4}.astn"
    
    # Create output directory if it doesn't exist
    output_dir=$(dirname "$output_file")
    mkdir -p "$output_dir"
    
    total_files=$((total_files + 1))
    
    print_status $YELLOW "Processing: $relative_path"
    
    # Copy the original .g4 file
    g4_output_file="$OUTPUT_DIR/$relative_path"
    cp "$grammar_file" "$g4_output_file"
    
    # Convert grammar to ASTN format
    if java -cp "$JAR_PATH" "$CONVERTER_CLASS" "$grammar_file" "$output_file" 2>/dev/null; then
        converted_files=$((converted_files + 1))
        print_status $GREEN "  ✓ Converted to: ${output_file#./}"
        print_status $GREEN "  ✓ Copied to: ${g4_output_file#./}"
    else
        failed_files=$((failed_files + 1))
        print_status $RED "  ✗ Failed to convert: $relative_path"
        print_status $GREEN "  ✓ Copied to: ${g4_output_file#./}"
        
        # Create empty file with error message for failed conversions
        echo "# Conversion failed for $relative_path" > "$output_file"
    fi
    
done < <(find "$SOURCE_DIR" -name "*.g4" -type f -print0)

echo
print_status $BLUE "Conversion complete!"
print_status $GREEN "Successfully converted: $converted_files files"
if [ $failed_files -gt 0 ]; then
    print_status $RED "Failed conversions: $failed_files files"
fi
print_status $BLUE "Total processed: $total_files files"

# Generate index file
index_file="$OUTPUT_DIR/index.astn"
print_status $BLUE "Generating index file: $index_file"

{
    echo "()"
    echo "  'summary': ("
    echo "    'total_grammars': $total_files"
    echo "    'successful_conversions': $converted_files"
    echo "    'failed_conversions': $failed_files"
    echo "    'source_directory': '$SOURCE_DIR'"
    echo "    'generated_at': '$(date -Iseconds)'"
    echo "  )"
    echo "  'grammars': ["
    
    # List all converted files
    first=true
    while IFS= read -r -d '' astn_file; do
        relative_astn="${astn_file#$OUTPUT_DIR/}"
        if [ "$first" = true ]; then
            first=false
        else
            echo ""
        fi
        echo "    ("
        echo "      'path': '$relative_astn'"
        echo "      'original': '${relative_astn%.astn}.g4'"
        echo "    )"
    done < <(find "$OUTPUT_DIR" -name "*.astn" -not -name "index.astn" -type f -print0 | sort -z)
    
    echo ""
    echo "  ]"
    echo ")"
} > "$index_file"

print_status $GREEN "Index file generated: $index_file"

# Show some statistics
echo
print_status $BLUE "Directory structure created:"
find "$OUTPUT_DIR" -type d | head -10 | while read dir; do
    echo "  📁 ${dir#./}"
done

if [ $(find "$OUTPUT_DIR" -type d | wc -l) -gt 10 ]; then
    echo "  ... and $(($(find "$OUTPUT_DIR" -type d | wc -l) - 10)) more directories"
fi

echo
print_status $GREEN "✨ All done! Check the '$OUTPUT_DIR' directory for converted grammars."