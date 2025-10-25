/**
 * Minimal TypeScript schema for ANTLR4 Grammar data structure
 * Contains only the essential types needed to represent a parsed grammar
 */

// ============================================================================
// Core Grammar Data Structure
// ============================================================================

/** Main grammar representation */
type Grammar = {
  name: string;
  fileName?: string;

  // Grammar variant
  // lexer grammar MyLexer;
  // parser grammar MyParser;
  // grammar MyCombined;
  type:
  | ["lexer", {
    modes?: {[key: string]: string[]}; // mode name -> rule names
  }]
  | ["parser", {}]
  | ["combined", {
    implicitLexer?: Grammar;
  }];

  // Rules
  // expr : term '+' term ;
  // ID : [a-zA-Z]+ ;
  rules: {[key: string]: Rule};

  // Token vocabulary  
  // ID=1, PLUS=2, etc.
  tokenNameToTypeMap: {[key: string]: number};
  // "+"=2, "if"=3, etc.
  stringLiteralToTypeMap: {[key: string]: number};

  // Grammar-level actions
  // @header { import java.util.*; }
  namedActions?: {[key: string]: string}; // action name -> action code

  // Imports
  // import CommonLexerRules;
  importedGrammars?: Grammar[];
};

/** Grammar rule */
type Rule = {
  name: string;

  // Rule properties
  // fragment ID : [a-zA-Z]+ ;
  modifiers?: string[]; // ["fragment"], etc.
  // expr[int x] returns [int value] locals [int temp]
  args?: string; // rule arguments as string
  returns?: string; // return values as string  
  locals?: string; // local variables as string

  // Rule body
  // expr : term '+' term | term ;
  alternatives: Alternative[];

  // Rule-level actions
  // @init { int x = 0; } @after { cleanup(); }
  namedActions?: {[key: string]: string}; // @init, @after, etc.
  // catch [RecognitionException re] { recover(re); }
  exceptions?: string[]; // exception handlers
};

/** Rule alternative */
type Alternative = {
  // Alternative elements
  // term '+' term
  // 'if' expr 'then' stmt
  elements: Element[];

  // Alternative-level actions
  // term { $value = $term.value; } '+' term
  actions?: string[]; // embedded actions

  // Label for this alternative
  // expr : a=term '+' b=term  # AddExpr
  //                           ^^^^^^^^
  label?: string;
};

/** Grammar element (terminal, nonterminal, etc.) */
type Element =
  // ID, PLUS, EOF
  | ["token", {
    name: string;
    label?: string; // x=ID
  }]
  // expr, statement, term  
  | ["rule", {
    name: string;
    args?: string; // expr[5, true]
    label?: string; // e=expr
  }]
  // 'if', '+', 'while'
  | ["literal", {
    value: string; // "if", "'+'", etc.
    label?: string; // op='+'
  }]
  // { System.out.println("action"); }
  | ["action", {
    code: string;
  }]
  // {$x > 0}?
  | ["predicate", {
    code: string;
  }]
  // (expr | term)?, ('+' | '-')*
  | ["block", {
    alternatives: Alternative[];
    ebnf?: "optional" | "star" | "plus"; // ?, *, +
  }]
  // [a-zA-Z], ~('*' | newline)
  | ["set", {
    elements: Element[];
    negated?: boolean; // for ~
  }]
  // 'a'..'z', '\u0000'..'\uFFFE'
  | ["range", {
    from: string;
    to: string;
  }]
  // .
  | ["wildcard", {}];

export { Grammar, Rule, Alternative, Element };

const test: Grammar = {
  "name": "Simple",
  "fileName": "test-grammars/Simple.g4",
  "type": [
    "combined",
    {
      "implicitLexer": {
        "name": "SimpleLexer",
        "fileName": "test-grammars/Simple.g4",
        "type": [
          "lexer",
          {
          }
        ],
        "rules": {
          "T__0": {
            "name": "T__0",
            "alternatives": [
              {
                "elements": [
                  [
                    "token",
                    {
                      "name": "'+'"

                    }
                  ]
                ]
              }
            ]
          },
          "T__1": {
            "name": "T__1",
            "alternatives": [
              {
                "elements": [
                  [
                    "token",
                    {
                      "name": "'('"

                    }
                  ]
                ]
              }
            ]
          },
          "T__2": {
            "name": "T__2",
            "alternatives": [
              {
                "elements": [
                  [
                    "token",
                    {
                      "name": "')'"

                    }
                  ]
                ]
              }
            ]
          },
          "NUMBER": {
            "name": "NUMBER",
            "alternatives": [
              {
                "elements": [

                ]
              }
            ]
          },
          "WS": {
            "name": "WS",
            "alternatives": [
              {
                "elements": [

                ]
              }
            ]
          }
        },
        "tokenNameToTypeMap": {
          "EOF": -1,
          "T__0": 1,
          "T__1": 2,
          "T__2": 3,
          "NUMBER": 4,
          "WS": 5
        },
        "stringLiteralToTypeMap": {
          "'+'": 1,
          "'('": 2,
          "')'": 3
        }

      }
    }
  ],
  "rules": {
    "expr": {
      "name": "expr",
      "alternatives": [
        {
          "elements": [
            [
              "token",
              {
                "name": "'+'"

              }
            ],
            [
              "rule",
              {
                "name": "term"

              }
            ],
            [
              "rule",
              {
                "name": "term"

              }
            ]
          ],
          "actions": ["{}", "{precpred(_ctx, 2)}?"]

        }
      ]
    },
    "term": {
      "name": "term",
      "alternatives": [
        {
          "elements": [
            [
              "token",
              {
                "name": "'('"

              }
            ],
            [
              "token",
              {
                "name": "')'"

              }
            ],
            [
              "rule",
              {
                "name": "expr"

              }
            ]
          ]
        },
        {
          "elements": [
            [
              "token",
              {
                "name": "NUMBER"

              }
            ]
          ]
        }
      ]
    }
  },
  "tokenNameToTypeMap": {
    "EOF": -1,
    "T__0": 1,
    "T__1": 2,
    "T__2": 3,
    "NUMBER": 4,
    "WS": 5
  },
  "stringLiteralToTypeMap": {
    "'+'": 1,
    "'('": 2,
    "')'": 3
  }

}