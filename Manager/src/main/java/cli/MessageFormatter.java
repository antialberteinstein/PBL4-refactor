package cli;

import util.Messages;

/**
 * MessageFormatter - Centralized CLI message and text formatting
 * 
 * Responsibilities:
 * - Provide consistent error messages
 * - Format success/info messages
 * - Generate help text
 * - Handle internationalization ready strings
 * 
 * Benefits:
 * - Easy to modify all messages in one place
 * - Consistent messaging across CLI
 * - Easier to add multiple language support later
 * - Separates presentation logic from business logic
 */
public class MessageFormatter {
    
    // ===== Error Messages =====
    
    public String errorNoAgentSelected() {
        return Messages.get("cli.error.no.agent");
    }
    
    public String errorNoSessionSelected() {
        return Messages.get("cli.error.no.session");
    }
    
    public String errorNoAgentOrSessionSelected() {
        return Messages.get("cli.error.no.agent.or.session");
    }
    
    public String errorInvalidMacAddress(String mac) {
        return Messages.get("cli.error.invalid.mac", mac);
    }
    
    public String errorInvalidSessionId(String id) {
        return Messages.get("cli.error.invalid.session.id", id);
    }
    
    public String errorInvalidLimit() {
        return Messages.get("cli.error.invalid.limit");
    }
    
    public String errorUnknownCommand(String command) {
        return Messages.get("cli.error.unknown.command", command);
    }
    
    public String errorUnknownListTarget(String target) {
        return Messages.get("cli.error.unknown.list.target", target);
    }
    
    public String errorAgentNotFound(String mac) {
        return Messages.get("cli.error.agent.not.found", mac);
    }
    
    public String errorSessionNotFound(int sessionId) {
        return Messages.get("cli.error.session.not.found", sessionId);
    }
    
    public String errorCommandFailed(String message) {
        return Messages.get("cli.error.command.failed", message);
    }
    
    // ===== Success/Info Messages =====
    
    public String successContextCleared() {
        return Messages.get("cli.success.context.cleared");
    }
    
    public String successAgentSelected(String mac) {
        return Messages.get("cli.success.agent.selected", mac);
    }
    
    public String successSessionSelected(int sessionId, String mac) {
        return Messages.get("cli.success.session.selected", sessionId, mac);
    }
    
    public String infoScanning() {
        return Messages.get("cli.info.scanning");
    }
    
    public String infoNoAgentsDiscovered() {
        return Messages.get("cli.info.no.agents");
    }
    
    public String infoNoSessionsFound(String mac) {
        return Messages.get("cli.info.no.sessions", mac);
    }
    
    public String infoNoProcessesFound(int sessionId) {
        return Messages.get("cli.info.no.processes", sessionId);
    }
    
    public String infoShowingTopProcesses(int count) {
        return Messages.get("cli.info.showing.top.processes", count);
    }
    
    public String infoShowingTopAgents(int count) {
        return Messages.get("cli.info.showing.top.agents", count);
    }
    
    public String infoShowingLatestSessions(int count) {
        return Messages.get("cli.info.showing.latest.sessions", count);
    }
    
    // ===== Help Text =====
    
    public String getHelpText() {
        StringBuilder help = new StringBuilder();
        help.append(Messages.get("cli.help.title")).append("\n\n");
        
        help.append(Messages.get("cli.help.discovery.title")).append("\n");
        help.append("  scan                          - ").append(Messages.get("cli.help.scan")).append("\n");
        help.append("  list agents                   - ").append(Messages.get("cli.help.list.agents")).append("\n\n");
        
        help.append(Messages.get("cli.help.context.title")).append("\n");
        help.append("  use agent <ID|MAC>            - ").append(Messages.get("cli.help.use.agent")).append("\n");
        help.append("  use session <ID>              - ").append(Messages.get("cli.help.use.session")).append("\n");
        help.append("  list sessions                 - ").append(Messages.get("cli.help.list.sessions.context")).append("\n");
        help.append("  list processes                - ").append(Messages.get("cli.help.list.processes.context")).append("\n");
        help.append("  clear                         - ").append(Messages.get("cli.help.clear")).append("\n\n");
        
        help.append(Messages.get("cli.help.explicit.title")).append("\n");
        help.append("  list sessions for agent <ID|MAC> - ").append(Messages.get("cli.help.list.sessions.agent")).append("\n");
        help.append("  list sessions for agent <ID|MAC> limit N - ").append(Messages.get("cli.help.list.sessions.limit")).append("\n");
        help.append("  list processes for session <ID> - ").append(Messages.get("cli.help.list.processes.session")).append("\n");
        help.append("  list processes for agent <ID|MAC> - ").append(Messages.get("cli.help.list.processes.agent")).append("\n");
        help.append("  list current                  - ").append(Messages.get("cli.help.list.current")).append("\n");
        help.append("  list current for <ID|MAC>     - ").append(Messages.get("cli.help.list.current.agent")).append("\n\n");
        
        help.append(Messages.get("cli.help.head.title")).append("\n");
        help.append("  head N agents                 - ").append(Messages.get("cli.help.head.agents")).append("\n");
        help.append("  head N sessions               - ").append(Messages.get("cli.help.head.sessions")).append("\n");
        help.append("  head N sessions for agent <ID|MAC> - ").append(Messages.get("cli.help.head.sessions.agent")).append("\n");
        help.append("  head N processes              - ").append(Messages.get("cli.help.head.processes")).append("\n");
        help.append("  head N processes for session <ID> - ").append(Messages.get("cli.help.head.processes.session")).append("\n");
        help.append("  head N processes for agent <ID|MAC> - ").append(Messages.get("cli.help.head.processes.agent")).append("\n\n");
        
        help.append(Messages.get("cli.help.tail.title")).append("\n");
        help.append("  tail N agents                 - ").append(Messages.get("cli.help.tail.agents")).append("\n");
        help.append("  tail N sessions               - ").append(Messages.get("cli.help.tail.sessions")).append("\n");
        help.append("  tail N sessions for agent <ID|MAC> - ").append(Messages.get("cli.help.tail.sessions.agent")).append("\n");
        help.append("  tail N processes              - ").append(Messages.get("cli.help.tail.processes")).append("\n");
        help.append("  tail N processes for session <ID> - ").append(Messages.get("cli.help.tail.processes.session")).append("\n");
        help.append("  tail N processes for agent <ID|MAC> - ").append(Messages.get("cli.help.tail.processes.agent")).append("\n\n");
        
        help.append(Messages.get("cli.help.pagination.title")).append("\n");
        help.append("  list sessions from A to B     - ").append(Messages.get("cli.help.list.sessions.range")).append("\n");
        help.append("  list sessions offset N limit M - ").append(Messages.get("cli.help.list.sessions.offset")).append("\n");
        help.append("  list sessions limit N         - ").append(Messages.get("cli.help.list.sessions.limit.only")).append("\n\n");
        
        help.append(Messages.get("cli.help.system.title")).append("\n");
        help.append("  status                        - ").append(Messages.get("cli.help.status")).append("\n");
        help.append("  help                          - ").append(Messages.get("cli.help.help")).append("\n");
        help.append("  exit                          - ").append(Messages.get("cli.help.exit")).append("\n\n");
        
        help.append(Messages.get("cli.help.note")).append("\n");
        help.append(Messages.get("cli.help.note.detail")).append("\n\n");
        
        help.append(getExamplesText());
        
        return help.toString();
    }
    
    public String getExamplesText() {
        StringBuilder examples = new StringBuilder();
        examples.append(Messages.get("cli.examples.title")).append("\n");
        examples.append("  Manager> scan\n");
        examples.append("  Manager> list agents\n");
        examples.append("  Manager> head 5 agents\n");
        examples.append("  Manager> tail 5 agents\n");
        examples.append("  Manager> use agent 1                    # ").append(Messages.get("cli.examples.use.id")).append("\n");
        examples.append("  Manager [AA:BB:CC:DD:EE:FF]> list sessions       # ").append(Messages.get("cli.examples.list.sessions")).append("\n");
        examples.append("  Manager [AA:BB:CC:DD:EE:FF]> list sessions from 0 to 50\n");
        examples.append("  Manager [AA:BB:CC:DD:EE:FF]> list sessions offset 100 limit 100\n");
        examples.append("  Manager [AA:BB:CC:DD:EE:FF]> head 10 sessions\n");
        examples.append("  Manager [AA:BB:CC:DD:EE:FF]> tail 10 sessions   # ").append(Messages.get("cli.examples.tail.sessions")).append("\n");
        examples.append("  Manager [AA:BB:CC:DD:EE:FF]> list processes\n");
        examples.append("  Manager [AA:BB:CC:DD:EE:FF]> head 5 processes\n");
        examples.append("  Manager [AA:BB:CC:DD:EE:FF]> tail 5 processes   # ").append(Messages.get("cli.examples.tail.processes")).append("\n");
        examples.append("  Manager [AA:BB:CC:DD:EE:FF]> list current\n");
        examples.append("  Manager [AA:BB:CC:DD:EE:FF]> use session 42\n");
        examples.append("  Manager [session:42]> list processes\n");
        examples.append("  Manager [session:42]> clear\n");
        examples.append("  Manager> list sessions for agent 1 limit 5  # ").append(Messages.get("cli.examples.use.id")).append("\n");
        examples.append("  Manager> list processes for agent 2         # ").append(Messages.get("cli.examples.use.id")).append("\n");
        examples.append("  Manager> head 10 processes for session 42\n");
        examples.append("  Manager> use agent AA:BB:CC:DD:EE:FF       # ").append(Messages.get("cli.examples.use.mac")).append("\n");
        return examples.toString();
    }
    
    // ===== Status Messages =====
    
    public String getStatusHeader() {
        return Messages.get("cli.status.title");
    }
    
    public String formatStatusLine(String label, String value) {
        return String.format("%-20s: %s", label, value);
    }
    
    public String formatContextInfo(String agentMac, Integer sessionId) {
        if (sessionId != null) {
            return formatStatusLine(Messages.get("cli.status.context"), 
                Messages.get("cli.status.session", sessionId));
        } else if (agentMac != null) {
            return formatStatusLine(Messages.get("cli.status.context"), 
                Messages.get("cli.status.agent", agentMac));
        } else {
            return formatStatusLine(Messages.get("cli.status.context"), 
                Messages.get("cli.status.none"));
        }
    }
    
    // ===== Utility Formatters =====
    
    /**
     * Format bytes to human-readable string
     */
    public String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    /**
     * Format percentage
     */
    public String formatPercentage(double value) {
        return String.format("%.1f%%", value);
    }
    
    /**
     * Format CPU cores (physical/logical)
     */
    public String formatCores(int physical, int logical) {
        return physical + "/" + logical;
    }
    
    /**
     * Format RAM usage fraction
     */
    public String formatRamUsage(long used, long total) {
        return formatBytes(used) + " / " + formatBytes(total);
    }
}
