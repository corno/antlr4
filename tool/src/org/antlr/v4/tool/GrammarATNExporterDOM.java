package org.antlr.v4.tool;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.tool.ASTNValue.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Exports ANTLR Grammar ATN (Augmented Transition Network) to JSON/ASTN format using a DOM-based approach.
 * 
 * This implementation builds up a JSON DOM structure first, then serializes it.
 * Provides detailed ATN state and transition information.
 */
public class GrammarATNExporterDOM {
    
    /**
     * Convert a Grammar's ATN to JSON string
     */
    public String exportATN(Grammar grammar) {
        Value atnValue = convertATN(grammar);
        return atnValue.serialize(0);
    }
    
    /**
     * Convert a Grammar's ATN to ASTN string
     */
    public String exportATNAstn(Grammar grammar) {
        Value atnValue = convertATN(grammar);
        return atnValue.serializeAstn(0);
    }
    
    /**
     * Convert ATN to Value DOM
     */
    private VerboseTypeValue convertATN(Grammar grammar) {
        ATN atn = grammar.getATN();
        VerboseTypeValue obj = new VerboseTypeValue();
        
        // Basic ATN properties
        obj.put("grammarName", grammar.name);
        obj.put("grammarType", convertATNType(atn.grammarType));
        obj.put("maxTokenType", atn.maxTokenType);
        
        // States array
        ArrayValue statesArray = new ArrayValue();
        for (int i = 0; i < atn.states.size(); i++) {
            ATNState state = atn.states.get(i);
            if (state != null) {
                statesArray.add(convertATNState(state, i));
            } else {
                statesArray.add(convertNullState(i));
            }
        }
        obj.put("states", statesArray);
        
        // Decision states
        ArrayValue decisionArray = new ArrayValue();
        for (int i = 0; i < atn.decisionToState.size(); i++) {
            DecisionState decisionState = atn.decisionToState.get(i);
            decisionArray.add(convertDecisionState(decisionState, i));
        }
        obj.put("decisionToState", decisionArray);
        
        // Rule mappings
        if (atn.ruleToStartState != null) {
            ArrayValue ruleStartArray = new ArrayValue();
            for (int i = 0; i < atn.ruleToStartState.length; i++) {
                RuleStartState startState = atn.ruleToStartState[i];
                if (startState != null) {
                    VerboseTypeValue ruleMapping = new VerboseTypeValue();
                    ruleMapping.put("ruleIndex", i);
                    ruleMapping.put("startStateNumber", startState.stateNumber);
                    if (grammar.getRule(i) != null) {
                        ruleMapping.put("ruleName", grammar.getRule(i).name);
                    }
                    ruleStartArray.add(ruleMapping);
                }
            }
            obj.put("ruleToStartState", ruleStartArray);
        }
        
        if (atn.ruleToStopState != null) {
            ArrayValue ruleStopArray = new ArrayValue();
            for (int i = 0; i < atn.ruleToStopState.length; i++) {
                RuleStopState stopState = atn.ruleToStopState[i];
                if (stopState != null) {
                    VerboseTypeValue ruleMapping = new VerboseTypeValue();
                    ruleMapping.put("ruleIndex", i);
                    ruleMapping.put("stopStateNumber", stopState.stateNumber);
                    if (grammar.getRule(i) != null) {
                        ruleMapping.put("ruleName", grammar.getRule(i).name);
                    }
                    ruleStopArray.add(ruleMapping);
                }
            }
            obj.put("ruleToStopState", ruleStopArray);
        }
        
        // Mode information for lexers
        if (!atn.modeNameToStartState.isEmpty()) {
            DictionaryValue modesDict = new DictionaryValue();
            for (Map.Entry<String, TokensStartState> entry : atn.modeNameToStartState.entrySet()) {
                VerboseTypeValue modeInfo = new VerboseTypeValue();
                modeInfo.put("stateNumber", entry.getValue().stateNumber);
                modesDict.put(entry.getKey(), modeInfo);
            }
            obj.put("modeNameToStartState", modesDict);
        }
        
        // Token type information for lexers
        if (atn.ruleToTokenType != null) {
            ArrayValue tokenTypeArray = new ArrayValue();
            for (int i = 0; i < atn.ruleToTokenType.length; i++) {
                VerboseTypeValue mapping = new VerboseTypeValue();
                mapping.put("ruleIndex", i);
                mapping.put("tokenType", atn.ruleToTokenType[i]);
                if (grammar.getRule(i) != null) {
                    mapping.put("ruleName", grammar.getRule(i).name);
                }
                tokenTypeArray.add(mapping);
            }
            obj.put("ruleToTokenType", tokenTypeArray);
        }
        
        // Lexer actions for lexer grammars
        if (atn.lexerActions != null) {
            ArrayValue actionsArray = new ArrayValue();
            for (int i = 0; i < atn.lexerActions.length; i++) {
                LexerAction action = atn.lexerActions[i];
                if (action != null) {
                    actionsArray.add(convertLexerAction(action, i));
                }
            }
            obj.put("lexerActions", actionsArray);
        }
        
        return obj;
    }
    
    /**
     * Convert ATN type to string
     */
    private String convertATNType(ATNType type) {
        switch (type) {
            case LEXER: return "lexer";
            case PARSER: return "parser";
            default: return "unknown";
        }
    }
    
    /**
     * Convert ATN state to StateValue (tagged union)
     */
    private StateValue convertATNState(ATNState state, int stateNumber) {
        VerboseTypeValue data = new VerboseTypeValue();
        data.put("stateNumber", stateNumber);
        data.put("ruleIndex", state.ruleIndex);
        
        // Add transitions
        ArrayValue transitionsArray = new ArrayValue();
        for (int i = 0; i < state.getNumberOfTransitions(); i++) {
            Transition transition = state.transition(i);
            transitionsArray.add(convertTransition(transition));
        }
        data.put("transitions", transitionsArray);
        
        // Determine state type and add specific information
        String stateType = getATNStateType(state);
        
        if (state instanceof DecisionState) {
            DecisionState decisionState = (DecisionState) state;
            data.put("decision", decisionState.decision);
            data.put("nonGreedy", decisionState.nonGreedy);
        }
        
        if (state instanceof RuleStartState) {
            RuleStartState ruleStart = (RuleStartState) state;
            data.put("isLeftRecursiveRule", ruleStart.isLeftRecursiveRule);
            if (ruleStart.stopState != null) {
                data.put("stopStateNumber", ruleStart.stopState.stateNumber);
            }
        }
        
        if (state instanceof RuleStopState) {
            // RuleStopState doesn't have additional fields beyond base ATNState
        }
        
        if (state instanceof TokensStartState) {
            // TokensStartState for lexer modes
        }
        
        if (state instanceof PlusBlockStartState) {
            PlusBlockStartState plusStart = (PlusBlockStartState) state;
            if (plusStart.loopBackState != null) {
                data.put("loopBackStateNumber", plusStart.loopBackState.stateNumber);
            }
        }
        
        if (state instanceof StarBlockStartState) {
            // StarBlockStartState for * loops
        }
        
        if (state instanceof BlockEndState) {
            BlockEndState blockEnd = (BlockEndState) state;
            if (blockEnd.startState != null) {
                data.put("startStateNumber", blockEnd.startState.stateNumber);
            }
        }
        
        return new StateValue(stateType, data);
    }
    
    /**
     * Convert null state entry
     */
    private StateValue convertNullState(int stateNumber) {
        VerboseTypeValue data = new VerboseTypeValue();
        data.put("stateNumber", stateNumber);
        return new StateValue("null", data);
    }
    
    /**
     * Get ATN state type as string
     */
    private String getATNStateType(ATNState state) {
        if (state instanceof RuleStartState) return "ruleStart";
        if (state instanceof RuleStopState) return "ruleStop";
        if (state instanceof TokensStartState) return "tokensStart";
        if (state instanceof PlusBlockStartState) return "plusBlockStart";
        if (state instanceof StarBlockStartState) return "starBlockStart";
        if (state instanceof PlusLoopbackState) return "plusLoopback";
        if (state instanceof StarLoopbackState) return "starLoopback";
        if (state instanceof LoopEndState) return "loopEnd";
        if (state instanceof BlockStartState) return "blockStart";
        if (state instanceof BlockEndState) return "blockEnd";
        if (state instanceof BasicState) return "basic";
        if (state instanceof DecisionState) return "decision";
        return "unknown";
    }
    
    /**
     * Convert transition to StateValue
     */
    private StateValue convertTransition(Transition transition) {
        VerboseTypeValue data = new VerboseTypeValue();
        data.put("target", transition.target.stateNumber);
        
        String transitionType = getTransitionType(transition);
        
        if (transition instanceof AtomTransition) {
            AtomTransition atom = (AtomTransition) transition;
            data.put("label", atom.label);
        } else if (transition instanceof SetTransition) {
            SetTransition set = (SetTransition) transition;
            data.put("set", set.set.toString());
        } else if (transition instanceof RangeTransition) {
            RangeTransition range = (RangeTransition) transition;
            data.put("from", range.from);
            data.put("to", range.to);
        } else if (transition instanceof RuleTransition) {
            RuleTransition rule = (RuleTransition) transition;
            data.put("ruleIndex", rule.ruleIndex);
            data.put("precedence", rule.precedence);
            if (rule.followState != null) {
                data.put("followState", rule.followState.stateNumber);
            }
        } else if (transition instanceof PredicateTransition) {
            PredicateTransition pred = (PredicateTransition) transition;
            data.put("ruleIndex", pred.ruleIndex);
            data.put("predIndex", pred.predIndex);
            data.put("isCtxDependent", pred.isCtxDependent);
        } else if (transition instanceof ActionTransition) {
            ActionTransition action = (ActionTransition) transition;
            data.put("ruleIndex", action.ruleIndex);
            data.put("actionIndex", action.actionIndex);
            data.put("isCtxDependent", action.isCtxDependent);
        } else if (transition instanceof EpsilonTransition) {
            // Epsilon transitions have no additional data
        } else if (transition instanceof WildcardTransition) {
            // Wildcard transitions have no additional data
        }
        
        return new StateValue(transitionType, data);
    }
    
    /**
     * Get transition type as string
     */
    private String getTransitionType(Transition transition) {
        if (transition instanceof AtomTransition) return "atom";
        if (transition instanceof SetTransition) return "set";
        if (transition instanceof RangeTransition) return "range";
        if (transition instanceof RuleTransition) return "rule";
        if (transition instanceof PredicateTransition) return "predicate";
        if (transition instanceof ActionTransition) return "action";
        if (transition instanceof EpsilonTransition) return "epsilon";
        if (transition instanceof WildcardTransition) return "wildcard";
        return "unknown";
    }
    
    /**
     * Convert decision state info
     */
    private VerboseTypeValue convertDecisionState(DecisionState state, int decisionIndex) {
        VerboseTypeValue obj = new VerboseTypeValue();
        obj.put("decisionIndex", decisionIndex);
        obj.put("stateNumber", state.stateNumber);
        obj.put("decision", state.decision);
        obj.put("nonGreedy", state.nonGreedy);
        return obj;
    }
    
    /**
     * Convert lexer action
     */
    private VerboseTypeValue convertLexerAction(LexerAction action, int index) {
        VerboseTypeValue obj = new VerboseTypeValue();
        obj.put("actionIndex", index);
        obj.put("actionType", action.getActionType().toString());
        obj.put("isPositionDependent", action.isPositionDependent());
        
        // Add action-specific data
        if (action instanceof LexerChannelAction) {
            LexerChannelAction channelAction = (LexerChannelAction) action;
            obj.put("channel", channelAction.getChannel());
        } else if (action instanceof LexerModeAction) {
            LexerModeAction modeAction = (LexerModeAction) action;
            obj.put("mode", modeAction.getMode());
        } else if (action instanceof LexerTypeAction) {
            LexerTypeAction typeAction = (LexerTypeAction) action;
            obj.put("type", typeAction.getType());
        } else if (action instanceof LexerPushModeAction) {
            LexerPushModeAction pushAction = (LexerPushModeAction) action;
            obj.put("mode", pushAction.getMode());
        }
        // LexerPopModeAction and LexerSkipAction have no additional data
        
        return obj;
    }
}