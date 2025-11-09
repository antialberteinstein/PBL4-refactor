package cli;

import database.*;
import model.*;
import service.*;
import util.CliStateManager;
import util.Messages;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * CommandExecutor - Executes CLI commands and returns formatted output
 * Supports both context-based and explicit commands
 */
public class CommandExecutor {
    
    private final CliContext context;
    private final ComputerManager computerManager;
    private final SessionManager sessionManager;
    private final ProcessManager processManager;
    private final HostScanner hostScanner;
    private final MessageFormatter messages;
    private final CliStateManager stateManager;
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public CommandExecutor(CliContext context, ComputerManager computerManager, 
                          SessionManager sessionManager, ProcessManager processManager,
                          HostScanner hostScanner) {
        this.context = context;
        this.computerManager = computerManager;
        this.sessionManager = sessionManager;
        this.processManager = processManager;
        this.hostScanner = hostScanner;
        this.messages = new MessageFormatter();
        this.stateManager = CliStateManager.getInstance();
    }
    
    /**
     * Execute a command and return result as string
     */
    public String execute(String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return "";
        }
        
        String cmd = commandLine.trim().toLowerCase();
        String[] parts = commandLine.trim().split("\\s+");
        
        try {
            // Help command
            if (cmd.equals("help") || cmd.equals("?")) {
                return showHelp();
            }
            
            // Clear context
            if (cmd.equals("clear") || cmd.equals("reset")) {
                context.clear();
                return messages.successContextCleared();
            }
            
            // Status command
            if (cmd.equals("status")) {
                return showStatus();
            }
            
            // Scan command
            if (cmd.equals("scan")) {
                return executeScan();
            }
            
            // Use commands (context-based)
            if (parts.length >= 3 && parts[0].equalsIgnoreCase("use")) {
                return executeUse(parts);
            }
            
            // Head commands (show first N results)
            if (parts.length >= 3 && parts[0].equalsIgnoreCase("head")) {
                return executeHead(parts);
            }
            
            // Tail commands (show last N results)
            if (parts.length >= 3 && parts[0].equalsIgnoreCase("tail")) {
                return executeTail(parts);
            }
            
            // List commands (both context-based and explicit)
            if (parts[0].equalsIgnoreCase("list") || parts[0].equalsIgnoreCase("show")) {
                return executeList(parts);
            }
            
            return messages.errorUnknownCommand(parts[0]);
            
        } catch (Exception e) {
            return messages.errorCommandFailed(e.getMessage());
        }
    }
    
    /**
     * Execute SCAN command
     */
    private String executeScan() {
        hostScanner.scan();
        return messages.infoScanning();
    }
    
    /**
     * Execute USE command
     */
    private String executeUse(String[] parts) {
        if (parts.length < 3) {
            return Messages.get("cli.exec.usage.use");
        }
        
        String type = parts[1].toLowerCase();
        String identifier = parts[2];
        
        if (type.equals("agent")) {
            // Resolve agent identifier (can be ID or MAC)
            String macAddress = stateManager.resolveAgent(identifier);
            if (macAddress == null) {
                return Messages.get("cli.exec.error.agent.not.found", identifier);
            }
            
            // Check if agent exists in database
            Computer agent = computerManager.getComputerByMac(macAddress);
            if (agent == null) {
                return Messages.get("cli.exec.error.agent.not.in.db", identifier);
            }
            
            context.useAgent(macAddress);
            
            // Show agent ID in the response if using MAC
            Integer agentId = stateManager.getIdByMac(macAddress);
            if (agentId != null) {
                return Messages.get("cli.exec.using.agent.with.id", agentId, macAddress, agent.getIpAddress());
            } else {
                return Messages.get("cli.exec.using.agent", macAddress, agent.getIpAddress());
            }
            
        } else if (type.equals("session")) {
            try {
                int sessionId = Integer.parseInt(identifier);
                Session session = sessionManager.getSessionById(sessionId);
                if (session == null) {
                    return Messages.get("cli.exec.error.session.not.found", sessionId);
                }
                context.useSession(sessionId);
                return Messages.get("cli.exec.using.session", sessionId);
            } catch (NumberFormatException e) {
                return Messages.get("cli.exec.error.session.id.number");
            }
            
        } else {
            return Messages.get("cli.exec.error.unknown.type", type);
        }
    }
    
    /**
     * Execute LIST/SHOW commands
     */
    private String executeList(String[] parts) {
        if (parts.length < 2) {
            return "Usage: list {agents|sessions|processes|current}";
        }
        
        String what = parts[1].toLowerCase();
        
        // LIST AGENTS
        if (what.equals("agents")) {
            return listAgents();
        }
        
        // LIST SESSIONS (context-based or explicit)
        if (what.equals("sessions")) {
            return listSessions(parts);
        }
        
        // LIST PROCESSES (context-based or explicit)
        if (what.equals("processes")) {
            return listProcesses(parts);
        }
        
        // LIST CURRENT (show current processes for an agent)
        if (what.equals("current")) {
            return listCurrentProcesses(parts);
        }
        
        return messages.errorUnknownListTarget(what);
    }
    
    /**
     * LIST AGENTS - Show all discovered Agents with IDs
     */
    private String listAgents() {
        List<Computer> agents = computerManager.getAllComputers();
        
        if (agents.isEmpty()) {
            return messages.infoNoAgentsDiscovered();
        }
        
        // Update agent mappings in CliStateManager
        stateManager.updateAgents(agents);
        
        TableFormatter table = new TableFormatter(
            "ID", "MAC Address", "IP Address", "Hostname", "OS", "CPU", "Cores"
        );
        
        for (Computer agent : agents) {
            Integer agentId = stateManager.getIdByMac(agent.getMacAddress());
            String idStr = agentId != null ? String.valueOf(agentId) : "?";
            
            table.addRow(
                idStr,
                agent.getMacAddress(),
                agent.getIpAddress(),
                agent.getHostname(),
                agent.getOs(),
                agent.getCpuName(),
                agent.getPhysicalCores() + "/" + agent.getLogicalCores()
            );
        }
        
        return table.build();
    }
    
    /**
     * LIST SESSIONS - Show sessions with pagination support
     * Supports:
     * - list sessions (uses context) - shows all sessions with auto-pagination
     * - list sessions for <ID|MAC> - shows all sessions
     * - list sessions for agent <ID|MAC> - shows all sessions
     * - list sessions limit N - shows N most recent sessions
     * - list sessions from A to B - shows sessions from index A to B
     * - list sessions offset N limit M - shows M sessions starting from offset N
     */
    private String listSessions(String[] parts) {
        String macAddress = null;
        Integer limit = null;
        Integer offset = 0;
        Integer fromIndex = null;
        Integer toIndex = null;
        
        // Parse: list sessions for agent <ID|MAC> [limit N]
        if (parts.length >= 5 && parts[2].equalsIgnoreCase("for") && parts[3].equalsIgnoreCase("agent")) {
            String identifier = parts[4];
            macAddress = stateManager.resolveAgent(identifier);
            if (macAddress == null) {
                return "Error: Agent '" + identifier + "' not found. Use 'list agents' to see available agents.";
            }
        }
        // Parse: list sessions for <ID|MAC> [limit N] (backward compatible)
        else if (parts.length >= 4 && parts[2].equalsIgnoreCase("for")) {
            String identifier = parts[3];
            macAddress = stateManager.resolveAgent(identifier);
            if (macAddress == null) {
                return "Error: Agent '" + identifier + "' not found. Use 'list agents' to see available agents.";
            }
        } 
        // Parse: list sessions [limit N] (use context)
        else if (context.hasAgent()) {
            macAddress = context.getCurrentAgent();
        } else {
            return messages.errorNoAgentSelected();
        }
        
        // Parse parameters: limit, offset, from/to
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equalsIgnoreCase("limit")) {
                try {
                    limit = Integer.parseInt(parts[i + 1]);
                } catch (NumberFormatException e) {
                    return messages.errorInvalidLimit();
                }
            } else if (parts[i].equalsIgnoreCase("offset")) {
                try {
                    offset = Integer.parseInt(parts[i + 1]);
                } catch (NumberFormatException e) {
                    return "Error: Invalid offset value. Must be a number.";
                }
            } else if (parts[i].equalsIgnoreCase("from") && i + 2 < parts.length && parts[i + 2].equalsIgnoreCase("to")) {
                try {
                    fromIndex = Integer.parseInt(parts[i + 1]);
                    toIndex = Integer.parseInt(parts[i + 3]);
                    if (fromIndex < 0 || toIndex < fromIndex) {
                        return "Error: Invalid range. 'from' must be >= 0 and <= 'to'.";
                    }
                } catch (NumberFormatException e) {
                    return "Error: Invalid range values. Must be numbers.";
                }
            }
        }
        
        // Get all sessions first to know total count
        List<Session> allSessions = sessionManager.getSessionsByMac(macAddress, -1);
        
        if (allSessions.isEmpty()) {
            return messages.infoNoSessionsFound(macAddress);
        }
        
        int totalCount = allSessions.size();
        List<Session> sessionsToDisplay;
        int displayedFrom;
        int displayedTo;
        
        // Handle different query modes
        if (fromIndex != null && toIndex != null) {
            // Range mode: from X to Y
            displayedFrom = Math.min(fromIndex, totalCount - 1);
            displayedTo = Math.min(toIndex, totalCount - 1);
            sessionsToDisplay = allSessions.subList(displayedFrom, displayedTo + 1);
        } else if (limit != null) {
            // Limit mode with optional offset
            displayedFrom = Math.min(offset, totalCount - 1);
            displayedTo = Math.min(displayedFrom + limit - 1, totalCount - 1);
            sessionsToDisplay = allSessions.subList(displayedFrom, displayedTo + 1);
        } else {
            // Auto-pagination mode: show up to 100 sessions
            final int MAX_DISPLAY = 100;
            if (totalCount <= MAX_DISPLAY) {
                sessionsToDisplay = allSessions;
                displayedFrom = 0;
                displayedTo = totalCount - 1;
            } else {
                sessionsToDisplay = allSessions.subList(0, MAX_DISPLAY);
                displayedFrom = 0;
                displayedTo = MAX_DISPLAY - 1;
            }
        }
        
        // Build table
        TableFormatter table = new TableFormatter(
            "Index", "ID", "CPU Usage", "RAM Usage", "Total RAM", "Timestamp", "Processes"
        );
        
        for (int i = 0; i < sessionsToDisplay.size(); i++) {
            Session session = sessionsToDisplay.get(i);
            int processCount = processManager.getProcessesBySessionId((int) session.getId()).size();
            int actualIndex = displayedFrom + i;
            
            table.addRow(
                String.valueOf(actualIndex),
                String.valueOf(session.getId()),
                String.format("%.1f%%", session.getCpuUsage()),
                String.format("%.2f GB", session.getRamUsage() / (1024.0 * 1024 * 1024)),
                String.format("%.2f GB", session.getTotalRam() / (1024.0 * 1024 * 1024)),
                dateFormat.format(new Date(session.getTimestamp())),
                String.valueOf(processCount)
            );
        }
        
        StringBuilder result = new StringBuilder();
        result.append(table.build());
        
        // Add pagination info
        result.append("\n");
        result.append(String.format("Showing sessions %d-%d of %d total sessions.", 
            displayedFrom, displayedTo, totalCount));
        
        if (displayedTo < totalCount - 1) {
            int remaining = totalCount - displayedTo - 1;
            result.append(String.format("\n%d more sessions available.", remaining));
            result.append("\nUse 'list sessions offset " + (displayedTo + 1) + " limit 100' to see next batch.");
            result.append("\nOr 'list sessions from " + (displayedTo + 1) + " to " + (Math.min(displayedTo + 100, totalCount - 1)) + "'");
        }
        
        return result.toString();
    }
    
    /**
     * LIST PROCESSES - Show processes for a session or latest processes for an agent
     * Supports:
     * - list processes (uses context - session OR agent)
     * - list processes for session <SESSION_ID>
     * - list processes for agent <ID|MAC>
     * - list processes for <SESSION_ID> (backward compatible)
     */
    private String listProcesses(String[] parts) {
        Integer sessionId = null;
        
        // Parse: list processes for session <SESSION_ID>
        if (parts.length >= 5 && parts[2].equalsIgnoreCase("for") && parts[3].equalsIgnoreCase("session")) {
            try {
                sessionId = Integer.parseInt(parts[4]);
            } catch (NumberFormatException e) {
                return "Error: Session ID must be a number.";
            }
        }
        // Parse: list processes for agent <ID|MAC>
        else if (parts.length >= 5 && parts[2].equalsIgnoreCase("for") && parts[3].equalsIgnoreCase("agent")) {
            String identifier = parts[4];
            String macAddress = stateManager.resolveAgent(identifier);
            if (macAddress == null) {
                return "Error: Agent '" + identifier + "' not found. Use 'list agents' to see available agents.";
            }
            
            List<Session> sessions = sessionManager.getSessionsByMac(macAddress, 1);
            
            if (sessions.isEmpty()) {
                return "No sessions found for Agent: " + identifier;
            }
            
            sessionId = (int) sessions.get(0).getId();
        }
        // Parse: list processes for <SESSION_ID> (backward compatible)
        else if (parts.length >= 4 && parts[2].equalsIgnoreCase("for")) {
            try {
                sessionId = Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                return "Error: Session ID must be a number.";
            }
        }
        // Use session context
        else if (context.hasSession()) {
            sessionId = context.getCurrentSession();
        }
        // Use agent context - show latest session's processes
        else if (context.hasAgent()) {
            String macAddress = context.getCurrentAgent();
            List<Session> sessions = sessionManager.getSessionsByMac(macAddress, 1);
            
            if (sessions.isEmpty()) {
                return "No sessions found for Agent: " + macAddress;
            }
            
            sessionId = (int) sessions.get(0).getId();
        } else {
            return "Error: No Session or Agent selected. Use 'use session <ID>' or 'use agent <ID|MAC>'";
        }
        
        // Get processes
        List<model.Process> processes = processManager.getProcessesBySessionId(sessionId);
        
        if (processes.isEmpty()) {
            return "No processes found for Session: " + sessionId;
        }
        
        TableFormatter table = new TableFormatter(
            "ID", "PID", "Name", "CPU Usage", "RAM Usage"
        );
        
        for (model.Process process : processes) {
            table.addRow(
                String.valueOf(process.getId()),
                String.valueOf(process.getPid()),
                process.getName(),
                messages.formatPercentage(process.getCpuUsage()),
                messages.formatBytes(process.getRamUsage())
            );
        }
        
        return table.build();
    }
    
    /**
     * LIST CURRENT - Show current running processes for an Agent
     * Uses the latest session to show real-time process snapshot
     * Supports:
     * - list current (uses context)
     * - list current for <ID|MAC>
     */
    private String listCurrentProcesses(String[] parts) {
        String macAddress = null;
        
        // Parse: list current for <ID|MAC>
        if (parts.length >= 4 && parts[2].equalsIgnoreCase("for")) {
            String identifier = parts[3];
            macAddress = stateManager.resolveAgent(identifier);
            if (macAddress == null) {
                return "Error: Agent '" + identifier + "' not found. Use 'list agents' to see available agents.";
            }
        }
        // Use context
        else if (context.hasAgent()) {
            macAddress = context.getCurrentAgent();
        } else {
            return "Error: No Agent selected. Use 'use agent <ID|MAC>' or 'list current for <ID|MAC>'";
        }
        
        // Get latest session
        List<Session> sessions = sessionManager.getSessionsByMac(macAddress, 1);
        if (sessions.isEmpty()) {
            return "No sessions found for Agent: " + macAddress;
        }
        
        Session latestSession = sessions.get(0);
        List<model.Process> processes = processManager.getProcessesBySessionId((int) latestSession.getId());
        
        if (processes.isEmpty()) {
            return "No processes in latest session for Agent: " + macAddress;
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Current processes for Agent: ").append(macAddress).append("\n");
        result.append("Snapshot time: ").append(dateFormat.format(new Date(latestSession.getTimestamp()))).append("\n");
        result.append("System: CPU ").append(String.format("%.1f%%", latestSession.getCpuUsage()));
        result.append(", RAM ").append(String.format("%.1f%%", (latestSession.getRamUsage() * 100.0) / latestSession.getTotalRam()));
        result.append("\n\n");
        
        TableFormatter table = new TableFormatter(
            "PID", "Name", "CPU Usage", "RAM Usage"
        );
        
        for (model.Process process : processes) {
            table.addRow(
                String.valueOf(process.getPid()),
                process.getName(),
                messages.formatPercentage(process.getCpuUsage()),
                messages.formatBytes(process.getRamUsage())
            );
        }
        
        result.append(table.build());
        return result.toString();
    }
    
    /**
     * Show system status
     */
    private String showStatus() {
        int agentCount = computerManager.getAllComputers().size();
        // Get total session count across all agents
        int sessionCount = 0;
        for (Computer agent : computerManager.getAllComputers()) {
            sessionCount += sessionManager.getSessionsByMac(agent.getMacAddress(), Integer.MAX_VALUE).size();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== Manager Status ===\n");
        sb.append("Agents discovered: ").append(agentCount).append("\n");
        sb.append("Total sessions: ").append(sessionCount).append("\n");
        if (context.hasAgent()) {
            sb.append("Current Agent: ").append(context.getCurrentAgent()).append("\n");
        }
        if (context.hasSession()) {
            sb.append("Current Session: ").append(context.getCurrentSession()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Execute HEAD command - Show top N items
     * Supports:
     * - head N agents       - Show first N agents
     * - head N sessions     - Show latest N sessions (requires agent context or use 'for <MAC>')
     * - head N processes    - Show top N processes by resource usage (requires session/agent context)
     */
    private String executeHead(String[] parts) {
        if (parts.length < 3) {
            return "Usage: head <N> {agents|sessions|processes} [for <ID>]";
        }
        
        int limit;
        try {
            limit = Integer.parseInt(parts[1]);
            if (limit <= 0) {
                return "Error: N must be a positive number.";
            }
        } catch (NumberFormatException e) {
            return "Error: N must be a number.";
        }
        
        String what = parts[2].toLowerCase();
        
        // HEAD N AGENTS
        if (what.equals("agents")) {
            return headAgents(limit);
        }
        
        // HEAD N SESSIONS
        if (what.equals("sessions")) {
            return headSessions(limit, parts);
        }
        
        // HEAD N PROCESSES
        if (what.equals("processes")) {
            return headProcesses(limit, parts);
        }
        
        return "Unknown head target: '" + what + "'. Use 'agents', 'sessions', or 'processes'.";
    }
    
    /**
     * Execute TAIL command - Show last N items
     * Supports:
     * - tail N agents       - Show last N agents
     * - tail N sessions     - Show oldest N sessions (requires agent context or use 'for <MAC>')
     * - tail N processes    - Show bottom N processes by resource usage (requires session/agent context)
     */
    private String executeTail(String[] parts) {
        if (parts.length < 3) {
            return "Usage: tail <N> {agents|sessions|processes} [for <ID>]";
        }
        
        int limit;
        try {
            limit = Integer.parseInt(parts[1]);
            if (limit <= 0) {
                return "Error: N must be a positive number.";
            }
        } catch (NumberFormatException e) {
            return "Error: N must be a number.";
        }
        
        String what = parts[2].toLowerCase();
        
        // TAIL N AGENTS
        if (what.equals("agents")) {
            return tailAgents(limit);
        }
        
        // TAIL N SESSIONS
        if (what.equals("sessions")) {
            return tailSessions(limit, parts);
        }
        
        // TAIL N PROCESSES
        if (what.equals("processes")) {
            return tailProcesses(limit, parts);
        }
        
        return "Unknown tail target: '" + what + "'. Use 'agents', 'sessions', or 'processes'.";
    }
    
    /**
     * Show first N agents
     */
    private String headAgents(int limit) {
        List<Computer> agents = computerManager.getAllComputers();
        
        if (agents.isEmpty()) {
            return "No Agents discovered yet. Run 'scan' to discover Agents.";
        }
        
        // Update agent mappings
        stateManager.updateAgents(agents);
        
        // Limit results
        int count = Math.min(limit, agents.size());
        List<Computer> limitedAgents = agents.subList(0, count);
        
        TableFormatter table = new TableFormatter(
            "ID", "MAC Address", "IP Address", "Hostname", "OS", "CPU", "Cores"
        );
        
        for (Computer agent : limitedAgents) {
            Integer agentId = stateManager.getIdByMac(agent.getMacAddress());
            String idStr = agentId != null ? String.valueOf(agentId) : "?";
            
            table.addRow(
                idStr,
                agent.getMacAddress(),
                agent.getIpAddress(),
                agent.getHostname(),
                agent.getOs(),
                agent.getCpuName(),
                agent.getPhysicalCores() + "/" + agent.getLogicalCores()
            );
        }
        
        return "Showing " + count + " of " + agents.size() + " agents:\n" + table.build();
    }
    
    /**
     * Show latest N sessions
     * Supports:
     * - head N sessions (uses agent context)
     * - head N sessions for agent <ID|MAC>
     * - head N sessions for <ID|MAC> (backward compatible)
     */
    private String headSessions(int limit, String[] parts) {
        String macAddress = null;
        
        // Parse: head N sessions for agent <ID|MAC>
        if (parts.length >= 6 && parts[3].equalsIgnoreCase("for") && parts[4].equalsIgnoreCase("agent")) {
            String identifier = parts[5];
            macAddress = stateManager.resolveAgent(identifier);
            if (macAddress == null) {
                return "Error: Agent '" + identifier + "' not found. Use 'list agents' to see available agents.";
            }
        }
        // Parse: head N sessions for <ID|MAC> (backward compatible)
        else if (parts.length >= 5 && parts[3].equalsIgnoreCase("for")) {
            String identifier = parts[4];
            macAddress = stateManager.resolveAgent(identifier);
            if (macAddress == null) {
                return "Error: Agent '" + identifier + "' not found. Use 'list agents' to see available agents.";
            }
        }
        // Use agent context
        else if (context.hasAgent()) {
            macAddress = context.getCurrentAgent();
        } else {
            return messages.errorNoAgentSelected();
        }
        
        // Get sessions
        List<Session> sessions = sessionManager.getSessionsByMac(macAddress, limit);
        
        if (sessions.isEmpty()) {
            return "No sessions found for Agent: " + macAddress;
        }
        
        TableFormatter table = new TableFormatter(
            "ID", "CPU Usage", "RAM Used", "Total RAM", "Timestamp", "Processes"
        );
        
        for (Session session : sessions) {
            int processCount = processManager.getProcessCount((int) session.getId());
            table.addRow(
                String.valueOf(session.getId()),
                String.format("%.1f%%", session.getCpuUsage()),
                String.format("%.2f GB", session.getRamUsage() / (1024.0 * 1024 * 1024)),
                String.format("%.2f GB", session.getTotalRam() / (1024.0 * 1024 * 1024)),
                dateFormat.format(new Date(session.getTimestamp())),
                String.valueOf(processCount)
            );
        }
        
        return "Showing latest " + sessions.size() + " sessions for Agent " + macAddress + ":\n" + table.build();
    }
    
    /**
     * Show top N processes by resource usage (CPU + RAM)
     * Supports:
     * - head N processes (uses session or agent context)
     * - head N processes for session <SESSION_ID>
     * - head N processes for agent <ID|MAC>
     * - head N processes for <SESSION_ID> (backward compatible)
     */
    private String headProcesses(int limit, String[] parts) {
        Integer sessionId = null;
        
        // Parse: head N processes for session <SESSION_ID>
        if (parts.length >= 6 && parts[3].equalsIgnoreCase("for") && parts[4].equalsIgnoreCase("session")) {
            try {
                sessionId = Integer.parseInt(parts[5]);
            } catch (NumberFormatException e) {
                return "Error: Session ID must be a number.";
            }
        }
        // Parse: head N processes for agent <ID|MAC>
        else if (parts.length >= 6 && parts[3].equalsIgnoreCase("for") && parts[4].equalsIgnoreCase("agent")) {
            String identifier = parts[5];
            String macAddress = stateManager.resolveAgent(identifier);
            if (macAddress == null) {
                return "Error: Agent '" + identifier + "' not found. Use 'list agents' to see available agents.";
            }
            
            List<Session> sessions = sessionManager.getSessionsByMac(macAddress, 1);
            
            if (sessions.isEmpty()) {
                return "No sessions found for Agent: " + identifier;
            }
            
            sessionId = (int) sessions.get(0).getId();
        }
        // Parse: head N processes for <SESSION_ID> (backward compatible)
        else if (parts.length >= 5 && parts[3].equalsIgnoreCase("for")) {
            try {
                sessionId = Integer.parseInt(parts[4]);
            } catch (NumberFormatException e) {
                return "Error: Session ID must be a number.";
            }
        }
        // Use session context
        else if (context.hasSession()) {
            sessionId = context.getCurrentSession();
        }
        // Use agent context - get latest session
        else if (context.hasAgent()) {
            String macAddress = context.getCurrentAgent();
            List<Session> sessions = sessionManager.getSessionsByMac(macAddress, 1);
            
            if (sessions.isEmpty()) {
                return "No sessions found for Agent: " + macAddress;
            }
            
            sessionId = (int) sessions.get(0).getId();
        } else {
            return "Error: No Session or Agent selected. Use 'use session <ID>' or 'use agent <ID|MAC>'";
        }
        
        // Get all processes
        List<model.Process> processes = processManager.getProcessesBySessionId(sessionId);
        
        if (processes.isEmpty()) {
            return "No processes found for Session: " + sessionId;
        }
        
        // Sort by resource usage (CPU + normalized RAM)
        processes.sort((p1, p2) -> {
            double score1 = p1.getCpuUsage() + (p1.getRamUsage() / (1024.0 * 1024 * 100)); // RAM in MB / 100
            double score2 = p2.getCpuUsage() + (p2.getRamUsage() / (1024.0 * 1024 * 100));
            return Double.compare(score2, score1); // Descending order
        });
        
        // Limit results
        int count = Math.min(limit, processes.size());
        processes = processes.subList(0, count);
        
        TableFormatter table = new TableFormatter(
            "ID", "PID", "Name", "CPU Usage", "RAM Usage"
        );
        
        for (model.Process process : processes) {
            table.addRow(
                String.valueOf(process.getId()),
                String.valueOf(process.getPid()),
                process.getName(),
                messages.formatPercentage(process.getCpuUsage()),
                messages.formatBytes(process.getRamUsage())
            );
        }
        
        return messages.infoShowingTopProcesses(count) + "\n" + table.build();
    }
    
    /**
     * Show last N agents
     */
    private String tailAgents(int limit) {
        List<Computer> agents = computerManager.getAllComputers();
        
        if (agents.isEmpty()) {
            return "No Agents discovered yet. Run 'scan' to discover Agents.";
        }
        
        // Update agent mappings
        stateManager.updateAgents(agents);
        
        // Get last N agents
        int count = Math.min(limit, agents.size());
        int startIndex = Math.max(0, agents.size() - count);
        List<Computer> limitedAgents = agents.subList(startIndex, agents.size());
        
        TableFormatter table = new TableFormatter(
            "ID", "MAC Address", "IP Address", "Hostname", "OS", "CPU", "Cores"
        );
        
        for (Computer agent : limitedAgents) {
            Integer agentId = stateManager.getIdByMac(agent.getMacAddress());
            String idStr = agentId != null ? String.valueOf(agentId) : "?";
            
            table.addRow(
                idStr,
                agent.getMacAddress(),
                agent.getIpAddress(),
                agent.getHostname(),
                agent.getOs(),
                agent.getCpuName(),
                agent.getPhysicalCores() + "/" + agent.getLogicalCores()
            );
        }
        
        return "Showing last " + count + " of " + agents.size() + " agents:\n" + table.build();
    }
    
    /**
     * Show oldest N sessions
     * Supports:
     * - tail N sessions (uses agent context)
     * - tail N sessions for agent <ID|MAC>
     * - tail N sessions for <ID|MAC> (backward compatible)
     */
    private String tailSessions(int limit, String[] parts) {
        String macAddress = null;
        
        // Parse: tail N sessions for agent <ID|MAC>
        if (parts.length >= 6 && parts[3].equalsIgnoreCase("for") && parts[4].equalsIgnoreCase("agent")) {
            String identifier = parts[5];
            macAddress = stateManager.resolveAgent(identifier);
            if (macAddress == null) {
                return "Error: Agent '" + identifier + "' not found. Use 'list agents' to see available agents.";
            }
        }
        // Parse: tail N sessions for <ID|MAC> (backward compatible)
        else if (parts.length >= 5 && parts[3].equalsIgnoreCase("for")) {
            String identifier = parts[4];
            macAddress = stateManager.resolveAgent(identifier);
            if (macAddress == null) {
                return "Error: Agent '" + identifier + "' not found. Use 'list agents' to see available agents.";
            }
        }
        // Use agent context
        else if (context.hasAgent()) {
            macAddress = context.getCurrentAgent();
        } else {
            return messages.errorNoAgentSelected();
        }
        
        // Get all sessions and take the oldest N
        List<Session> allSessions = sessionManager.getSessionsByMac(macAddress, -1);
        
        if (allSessions.isEmpty()) {
            return "No sessions found for Agent: " + macAddress;
        }
        
        // Get oldest N sessions
        int count = Math.min(limit, allSessions.size());
        List<Session> sessions = allSessions.subList(0, count);
        
        TableFormatter table = new TableFormatter(
            "Index", "ID", "CPU Usage", "RAM Used", "Total RAM", "Timestamp", "Processes"
        );
        
        for (int i = 0; i < sessions.size(); i++) {
            Session session = sessions.get(i);
            int processCount = processManager.getProcessCount((int) session.getId());
            table.addRow(
                String.valueOf(i),
                String.valueOf(session.getId()),
                String.format("%.1f%%", session.getCpuUsage()),
                String.format("%.2f GB", session.getRamUsage() / (1024.0 * 1024 * 1024)),
                String.format("%.2f GB", session.getTotalRam() / (1024.0 * 1024 * 1024)),
                dateFormat.format(new Date(session.getTimestamp())),
                String.valueOf(processCount)
            );
        }
        
        return "Showing oldest " + sessions.size() + " of " + allSessions.size() + " sessions for Agent " + macAddress + ":\n" + table.build();
    }
    
    /**
     * Show bottom N processes by resource usage (lowest usage)
     * Supports:
     * - tail N processes (uses session or agent context)
     * - tail N processes for session <SESSION_ID>
     * - tail N processes for agent <ID|MAC>
     * - tail N processes for <SESSION_ID> (backward compatible)
     */
    private String tailProcesses(int limit, String[] parts) {
        Integer sessionId = null;
        
        // Parse: tail N processes for session <SESSION_ID>
        if (parts.length >= 6 && parts[3].equalsIgnoreCase("for") && parts[4].equalsIgnoreCase("session")) {
            try {
                sessionId = Integer.parseInt(parts[5]);
            } catch (NumberFormatException e) {
                return "Error: Session ID must be a number.";
            }
        }
        // Parse: tail N processes for agent <ID|MAC>
        else if (parts.length >= 6 && parts[3].equalsIgnoreCase("for") && parts[4].equalsIgnoreCase("agent")) {
            String identifier = parts[5];
            String macAddress = stateManager.resolveAgent(identifier);
            if (macAddress == null) {
                return "Error: Agent '" + identifier + "' not found. Use 'list agents' to see available agents.";
            }
            
            List<Session> sessions = sessionManager.getSessionsByMac(macAddress, 1);
            
            if (sessions.isEmpty()) {
                return "No sessions found for Agent: " + identifier;
            }
            
            sessionId = (int) sessions.get(0).getId();
        }
        // Parse: tail N processes for <SESSION_ID> (backward compatible)
        else if (parts.length >= 5 && parts[3].equalsIgnoreCase("for")) {
            try {
                sessionId = Integer.parseInt(parts[4]);
            } catch (NumberFormatException e) {
                return "Error: Session ID must be a number.";
            }
        }
        // Use session context
        else if (context.hasSession()) {
            sessionId = context.getCurrentSession();
        }
        // Use agent context - get latest session
        else if (context.hasAgent()) {
            String macAddress = context.getCurrentAgent();
            List<Session> sessions = sessionManager.getSessionsByMac(macAddress, 1);
            
            if (sessions.isEmpty()) {
                return "No sessions found for Agent: " + macAddress;
            }
            
            sessionId = (int) sessions.get(0).getId();
        } else {
            return "Error: No Session or Agent selected. Use 'use session <ID>' or 'use agent <ID|MAC>'";
        }
        
        // Get all processes
        List<model.Process> processes = processManager.getProcessesBySessionId(sessionId);
        
        if (processes.isEmpty()) {
            return "No processes found for Session: " + sessionId;
        }
        
        // Sort by resource usage (CPU + normalized RAM) - ASCENDING for tail
        processes.sort((p1, p2) -> {
            double score1 = p1.getCpuUsage() + (p1.getRamUsage() / (1024.0 * 1024 * 100)); // RAM in MB / 100
            double score2 = p2.getCpuUsage() + (p2.getRamUsage() / (1024.0 * 1024 * 100));
            return Double.compare(score1, score2); // Ascending order - lowest first
        });
        
        // Limit results
        int count = Math.min(limit, processes.size());
        processes = processes.subList(0, count);
        
        TableFormatter table = new TableFormatter(
            "ID", "PID", "Name", "CPU Usage", "RAM Usage"
        );
        
        for (model.Process process : processes) {
            table.addRow(
                String.valueOf(process.getId()),
                String.valueOf(process.getPid()),
                process.getName(),
                messages.formatPercentage(process.getCpuUsage()),
                messages.formatBytes(process.getRamUsage())
            );
        }
        
        return "Showing " + count + " processes with lowest resource usage:\n" + table.build();
    }
    
    /**
     * Show help message
     */
    private String showHelp() {
        return messages.getHelpText();
    }
}
