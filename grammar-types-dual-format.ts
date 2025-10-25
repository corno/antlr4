/**
 * TypeScript type definitions for ANTLR Grammar JSON/ASTN export
 * 
 * This file defines types for both JSON and ASTN formats:
 * - JSON format: Standard JSON with tagged unions as arrays: ["type", data]
 * - ASTN format: Alternative syntax with () instead of {}, apostrophes for keys, 
 *   no commas, and tagged unions as: | type_name data
 */

// Core Grammar structure (same for both formats)
export interface Grammar {
  name: string;
  fileName?: string;
  type: GrammarType;
  rules: { [ruleName: string]: Rule };
  tokenNameToTypeMap: { [tokenName: string]: number };
  stringLiteralToTypeMap: { [literal: string]: number };
  namedActions?: { [actionName: string]: string };
  importedGrammars?: Grammar[];
}

// Grammar types (tagged unions)
// JSON format: ["lexer", {...}], ["parser", {...}], ["combined", {...}]
// ASTN format: | lexer (...), | parser (...), | combined (...)
export type GrammarType = 
  | ["lexer", LexerData]
  | ["parser", ParserData]
  | ["combined", CombinedData];

export interface LexerData {
  modes?: { [modeName: string]: string[] };
}

export interface ParserData {
  // Parser-specific data can be added here
}

export interface CombinedData {
  implicitLexer?: Grammar;
}

// Rule structure
export interface Rule {
  name: string;
  modifiers?: string[];
  args?: string;
  returns?: string;
  locals?: string;
  alternatives: Alternative[];
  namedActions?: { [actionName: string]: string };
  exceptions?: string[];
}

// Alternative structure
export interface Alternative {
  elements: Element[];
  actions?: string[];
  label?: string;
}

// Element types (tagged unions)
// JSON format: ["token", {...}], ["rule", {...}], etc.
// ASTN format: | token (...), | rule (...), etc.
export type Element = 
  | ["token", TokenElement]
  | ["rule", RuleElement]
  | ["action", ActionElement]
  | ["predicate", PredicateElement]
  | ["set", SetElement]
  | ["range", RangeElement]
  | ["wildcard", WildcardElement]
  | ["block", BlockElement];

export interface TokenElement {
  name: string;
  label?: string;
}

export interface RuleElement {
  name: string;
  args?: string;
  label?: string;
}

export interface ActionElement {
  code: string;
}

export interface PredicateElement {
  code: string;
}

export interface SetElement {
  elements: Element[];
}

export interface RangeElement {
  from: string;
  to: string;
}

export interface WildcardElement {
  // Empty interface for wildcard
}

export interface BlockElement {
  alternatives: Alternative[];
  ebnf?: "optional" | "plus" | "star";
}

/**
 * Format Examples
 * 
 * JSON Format:
 * {
 *   "name": "Example",
 *   "type": ["combined", {}],
 *   "rules": {
 *     "expr": {
 *       "name": "expr",
 *       "alternatives": [
 *         {
 *           "elements": [
 *             ["token", { "name": "ID" }],
 *             ["block", { 
 *               "alternatives": [{"elements": [["token", {"name": "PLUS"}]]}],
 *               "ebnf": "optional"
 *             }]
 *           ]
 *         }
 *       ]
 *     }
 *   }
 * }
 * 
 * ASTN Format:
 * (
 *   'name': 'Example'
 *   'type': | combined ()
 *   'rules': (
 *     'expr': (
 *       'name': 'expr'
 *       'alternatives': [
 *         (
 *           'elements': [
 *             | token (
 *               'name': 'ID'
 *             )
 *             | block (
 *               'alternatives': [
 *                 (
 *                   'elements': [
 *                     | token (
 *                       'name': 'PLUS'
 *                     )
 *                   ]
 *                 )
 *               ]
 *               'ebnf': 'optional'
 *             )
 *           ]
 *         )
 *       ]
 *     )
 *   )
 * )
 */

// Example usage and test data for JSON format
export const exampleGrammar: Grammar = {
  name: "ExampleGrammar",
  fileName: "Example.g4",
  type: ["combined", {}],
  rules: {
    "start": {
      name: "start",
      alternatives: [
        {
          elements: [
            ["rule", { name: "expression" }]
          ]
        }
      ]
    },
    "expression": {
      name: "expression",
      alternatives: [
        {
          elements: [
            ["token", { name: "ID" }],
            ["block", {
              alternatives: [
                {
                  elements: [
                    ["token", { name: "PLUS" }],
                    ["rule", { name: "expression" }]
                  ]
                }
              ],
              ebnf: "optional"
            }]
          ]
        }
      ]
    }
  },
  tokenNameToTypeMap: {
    "ID": 1,
    "PLUS": 2
  },
  stringLiteralToTypeMap: {
    "'+' ": 2
  }
};

// Utility type for JSON DOM Value (internal to exporter)
export type JSONValue = 
  | ["array", JSONValue[]]
  | ["string", string]
  | ["number", number]
  | ["object", { [key: string]: JSONValue }]
  | ["state", { name: string; value: JSONValue }];

// Helper functions for working with tagged unions
export function isGrammarType(value: any, expectedType: string): boolean {
  return Array.isArray(value) && value.length === 2 && value[0] === expectedType;
}

export function getGrammarTypeData<T>(grammarType: GrammarType): T {
  return grammarType[1] as T;
}

export function isElementType(element: Element, expectedType: string): boolean {
  return Array.isArray(element) && element.length === 2 && element[0] === expectedType;
}

export function getElementData<T>(element: Element): T {
  return element[1] as T;
}

// Type guards for working with elements
export function isTokenElement(element: Element): element is ["token", TokenElement] {
  return isElementType(element, "token");
}

export function isRuleElement(element: Element): element is ["rule", RuleElement] {
  return isElementType(element, "rule");
}

export function isBlockElement(element: Element): element is ["block", BlockElement] {
  return isElementType(element, "block");
}

export function isEBNFBlock(element: Element): boolean {
  if (!isBlockElement(element)) return false;
  const blockData = getElementData<BlockElement>(element);
  return blockData.ebnf !== undefined;
}

/**
 * ASTN Format Specification
 * 
 * The ASTN (Alternative Syntax Tree Notation) format uses:
 * 1. Parentheses () for objects instead of braces {}
 * 2. Square brackets [] for arrays (same as JSON)
 * 3. Apostrophes ' for string keys instead of double quotes "
 * 4. No commas between array/object elements
 * 5. Tagged unions as: | state_name data... instead of ["state_name", data]
 * 6. Proper indentation for readability
 * 
 * This format is more human-readable and can be useful for:
 * - Configuration files
 * - Tree visualization
 * - Debugging grammar structures
 * - Alternative serialization format
 */