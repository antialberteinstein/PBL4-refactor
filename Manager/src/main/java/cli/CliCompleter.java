package cli;

import database.*;
import model.Computer;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.*;

/**
 * CliCompleter - Provides intelligent tab completion for CLI commands
 * 
 * Features:
 * - Command completion (list, use, head, tail, etc.)
 * - Context-aware suggestions (agents, sessions, processes)
 * - Dynamic completion based on available data
 */
public class CliCompleter implements Completer {
    
    private final ComputerManager computerManager;
    private final SessionManager sessionManager;
    private final CliContext context;
    
    // Base commands
    private static final List<String> BASE_COMMANDS = Arrays.asList(
        "help", "?", "exit", "quit", "scan", "status", "clear", "reset",
        "list", "show", "use", "head", "tail"
    );
    
    // List targets
    private static final List<String> LIST_TARGETS = Arrays.asList(
        "agents", "sessions", "processes", "current"
    );
    
    // Use targets
    private static final List<String> USE_TARGETS = Arrays.asList(
        "agent", "session"
    );
    
    public CliCompleter(ComputerManager computerManager, SessionManager sessionManager, CliContext context) {
        this.computerManager = computerManager;
        this.sessionManager = sessionManager;
        this.context = context;
    }
    
    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String buffer = line.line();
        String[] tokens = buffer.trim().split("\\s+");
        
        if (tokens.length == 0 || buffer.trim().isEmpty()) {
            // Suggest base commands
            addCandidates(candidates, BASE_COMMANDS, "command");
            return;
        }
        
        String firstToken = tokens[0].toLowerCase();
        
        // First token completion
        if (tokens.length == 1 && !buffer.endsWith(" ")) {
            addMatchingCandidates(candidates, BASE_COMMANDS, firstToken, "command");
            return;
        }
        
        // Context-specific completion
        switch (firstToken) {
            case "list":
            case "show":
                completeListCommand(tokens, buffer, candidates);
                break;
                
            case "use":
                completeUseCommand(tokens, buffer, candidates);
                break;
                
            case "head":
            case "tail":
                completeHeadTailCommand(tokens, buffer, candidates);
                break;
                
            default:
                // No completion for other commands
                break;
        }
    }
    
    /**
     * Complete LIST/SHOW commands
     */
    private void completeListCommand(String[] tokens, String buffer, List<Candidate> candidates) {
        // list <target>
        if (tokens.length == 2 && !buffer.endsWith(" ")) {
            addMatchingCandidates(candidates, LIST_TARGETS, tokens[1], "target");
            return;
        }
        
        if (tokens.length >= 2) {
            String target = tokens[1].toLowerCase();
            
            // list sessions [for agent <id>] [limit N] [offset N] [from A to B]
            if (target.equals("sessions")) {
                if (tokens.length == 3 && !buffer.endsWith(" ")) {
                    addMatchingCandidates(candidates, Arrays.asList("for", "limit", "offset", "from"), tokens[2], "keyword");
                } else if (tokens.length == 3 && buffer.endsWith(" ")) {
                    addCandidates(candidates, Arrays.asList("for", "limit", "offset", "from"), "keyword");
                } else if (tokens.length >= 4 && tokens[2].equalsIgnoreCase("for")) {
                    if (tokens.length == 4 && !buffer.endsWith(" ")) {
                        addMatchingCandidates(candidates, Arrays.asList("agent"), tokens[3], "keyword");
                    } else if (tokens.length == 4 && buffer.endsWith(" ")) {
                        addCandidates(candidates, Arrays.asList("agent"), "keyword");
                    } else if (tokens.length == 5 && tokens[3].equalsIgnoreCase("agent")) {
                        completeAgentIdentifier(tokens[4], buffer, candidates);
                    }
                } else if (tokens.length >= 4 && (tokens[2].equalsIgnoreCase("limit") || tokens[2].equalsIgnoreCase("offset"))) {
                    // Number expected, no completion
                    if (tokens.length == 4 && buffer.endsWith(" ")) {
                        candidates.add(new Candidate("10", "10", "number", "limit to 10", null, null, true));
                        candidates.add(new Candidate("50", "50", "number", "limit to 50", null, null, true));
                        candidates.add(new Candidate("100", "100", "number", "limit to 100", null, null, true));
                    }
                } else if (tokens.length >= 4 && tokens[2].equalsIgnoreCase("from")) {
                    // from A to B
                    if (tokens.length == 5 && !buffer.endsWith(" ")) {
                        addMatchingCandidates(candidates, Arrays.asList("to"), tokens[4], "keyword");
                    } else if (tokens.length == 5 && buffer.endsWith(" ")) {
                        addCandidates(candidates, Arrays.asList("to"), "keyword");
                    }
                }
            }
            
            // list processes [for {agent|session} <id>]
            else if (target.equals("processes")) {
                if (tokens.length == 3 && !buffer.endsWith(" ")) {
                    addMatchingCandidates(candidates, Arrays.asList("for"), tokens[2], "keyword");
                } else if (tokens.length == 3 && buffer.endsWith(" ")) {
                    addCandidates(candidates, Arrays.asList("for"), "keyword");
                } else if (tokens.length >= 4 && tokens[2].equalsIgnoreCase("for")) {
                    if (tokens.length == 4 && !buffer.endsWith(" ")) {
                        addMatchingCandidates(candidates, Arrays.asList("agent", "session"), tokens[3], "keyword");
                    } else if (tokens.length == 4 && buffer.endsWith(" ")) {
                        addCandidates(candidates, Arrays.asList("agent", "session"), "keyword");
                    } else if (tokens.length == 5 && tokens[3].equalsIgnoreCase("agent")) {
                        completeAgentIdentifier(tokens[4], buffer, candidates);
                    } else if (tokens.length == 5 && tokens[3].equalsIgnoreCase("session")) {
                        completeSessionIdentifier(tokens[4], buffer, candidates);
                    }
                }
            }
            
            // list current [for <agent>]
            else if (target.equals("current")) {
                if (tokens.length == 3 && !buffer.endsWith(" ")) {
                    addMatchingCandidates(candidates, Arrays.asList("for"), tokens[2], "keyword");
                } else if (tokens.length == 3 && buffer.endsWith(" ")) {
                    addCandidates(candidates, Arrays.asList("for"), "keyword");
                } else if (tokens.length == 4 && tokens[2].equalsIgnoreCase("for")) {
                    completeAgentIdentifier(tokens[3], buffer, candidates);
                }
            }
        }
    }
    
    /**
     * Complete USE commands
     */
    private void completeUseCommand(String[] tokens, String buffer, List<Candidate> candidates) {
        // use <agent|session>
        if (tokens.length == 2 && !buffer.endsWith(" ")) {
            addMatchingCandidates(candidates, USE_TARGETS, tokens[1], "target");
            return;
        }
        
        if (tokens.length == 2 && buffer.endsWith(" ")) {
            addCandidates(candidates, USE_TARGETS, "target");
            return;
        }
        
        // use agent <id|mac>
        if (tokens.length >= 3 && tokens[1].equalsIgnoreCase("agent")) {
            completeAgentIdentifier(tokens[2], buffer, candidates);
        }
        
        // use session <id>
        else if (tokens.length >= 3 && tokens[1].equalsIgnoreCase("session")) {
            completeSessionIdentifier(tokens[2], buffer, candidates);
        }
    }
    
    /**
     * Complete HEAD/TAIL commands
     */
    private void completeHeadTailCommand(String[] tokens, String buffer, List<Candidate> candidates) {
        // head <N> <target>
        if (tokens.length == 2 && buffer.endsWith(" ")) {
            // Suggest common numbers
            candidates.add(new Candidate("5", "5", "number", "top 5", null, null, true));
            candidates.add(new Candidate("10", "10", "number", "top 10", null, null, true));
            candidates.add(new Candidate("20", "20", "number", "top 20", null, null, true));
            return;
        }
        
        if (tokens.length == 3 && !buffer.endsWith(" ")) {
            addMatchingCandidates(candidates, LIST_TARGETS, tokens[2], "target");
            return;
        }
        
        if (tokens.length == 3 && buffer.endsWith(" ")) {
            addCandidates(candidates, LIST_TARGETS, "target");
            return;
        }
        
        // head N sessions [for agent <id>]
        if (tokens.length >= 4 && tokens[2].equalsIgnoreCase("sessions")) {
            if (tokens.length == 4 && !buffer.endsWith(" ")) {
                addMatchingCandidates(candidates, Arrays.asList("for"), tokens[3], "keyword");
            } else if (tokens.length == 4 && buffer.endsWith(" ")) {
                addCandidates(candidates, Arrays.asList("for"), "keyword");
            } else if (tokens.length >= 5 && tokens[3].equalsIgnoreCase("for")) {
                if (tokens.length == 5 && !buffer.endsWith(" ")) {
                    addMatchingCandidates(candidates, Arrays.asList("agent"), tokens[4], "keyword");
                } else if (tokens.length == 5 && buffer.endsWith(" ")) {
                    addCandidates(candidates, Arrays.asList("agent"), "keyword");
                } else if (tokens.length == 6 && tokens[4].equalsIgnoreCase("agent")) {
                    completeAgentIdentifier(tokens[5], buffer, candidates);
                }
            }
        }
        
        // head N processes [for {agent|session} <id>]
        else if (tokens.length >= 4 && tokens[2].equalsIgnoreCase("processes")) {
            if (tokens.length == 4 && !buffer.endsWith(" ")) {
                addMatchingCandidates(candidates, Arrays.asList("for"), tokens[3], "keyword");
            } else if (tokens.length == 4 && buffer.endsWith(" ")) {
                addCandidates(candidates, Arrays.asList("for"), "keyword");
            } else if (tokens.length >= 5 && tokens[3].equalsIgnoreCase("for")) {
                if (tokens.length == 5 && !buffer.endsWith(" ")) {
                    addMatchingCandidates(candidates, Arrays.asList("agent", "session"), tokens[4], "keyword");
                } else if (tokens.length == 5 && buffer.endsWith(" ")) {
                    addCandidates(candidates, Arrays.asList("agent", "session"), "keyword");
                } else if (tokens.length == 6 && tokens[4].equalsIgnoreCase("agent")) {
                    completeAgentIdentifier(tokens[5], buffer, candidates);
                } else if (tokens.length == 6 && tokens[4].equalsIgnoreCase("session")) {
                    completeSessionIdentifier(tokens[5], buffer, candidates);
                }
            }
        }
    }
    
    /**
     * Complete agent identifiers (IDs and MACs)
     */
    private void completeAgentIdentifier(String partial, String buffer, List<Candidate> candidates) {
        if (!buffer.endsWith(" ")) {
            List<Computer> agents = computerManager.getAllComputers();
            for (int i = 0; i < agents.size(); i++) {
                Computer agent = agents.get(i);
                int agentId = i + 1;
                String mac = agent.getMacAddress();
                String ip = agent.getIpAddress();
                
                // Add ID suggestion
                String idStr = String.valueOf(agentId);
                if (idStr.startsWith(partial)) {
                    candidates.add(new Candidate(
                        idStr, 
                        idStr, 
                        "agent", 
                        "Agent #" + agentId + " (" + ip + ")", 
                        null, 
                        null, 
                        true
                    ));
                }
                
                // Add MAC suggestion
                if (mac.toLowerCase().startsWith(partial.toLowerCase())) {
                    candidates.add(new Candidate(
                        mac, 
                        mac, 
                        "agent", 
                        "Agent #" + agentId + " (" + ip + ")", 
                        null, 
                        null, 
                        true
                    ));
                }
            }
        }
    }
    
    /**
     * Complete session identifiers
     */
    private void completeSessionIdentifier(String partial, String buffer, List<Candidate> candidates) {
        if (!buffer.endsWith(" ") && context.getCurrentAgent() != null) {
            // Get recent sessions for current agent
            List<model.Session> sessions = sessionManager.getSessionsByMac(
                context.getCurrentAgent(), 10
            );
            
            for (model.Session session : sessions) {
                String idStr = String.valueOf(session.getId());
                if (idStr.startsWith(partial)) {
                    candidates.add(new Candidate(
                        idStr, 
                        idStr, 
                        "session", 
                        "Session #" + session.getId(), 
                        null, 
                        null, 
                        true
                    ));
                }
            }
        }
    }
    
    /**
     * Add candidates from list
     */
    private void addCandidates(List<Candidate> candidates, List<String> options, String group) {
        for (String option : options) {
            candidates.add(new Candidate(
                option, 
                option, 
                group, 
                null, 
                null, 
                null, 
                true
            ));
        }
    }
    
    /**
     * Add matching candidates from list
     */
    private void addMatchingCandidates(List<Candidate> candidates, List<String> options, String partial, String group) {
        String lowerPartial = partial.toLowerCase();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lowerPartial)) {
                candidates.add(new Candidate(
                    option, 
                    option, 
                    group, 
                    null, 
                    null, 
                    null, 
                    true
                ));
            }
        }
    }
}
