package cli;

import database.*;
import service.HostScanner;
import util.Logger;

import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * CliInterface - Interactive MySQL-style command-line interface for Manager
 * 
 * Features:
 * - Command history (up/down arrows) with persistent storage
 * - Tab completion for commands, agents, sessions
 * - Syntax highlighting (colored commands and keywords)
 * - Command suggestions as you type
 * - Ctrl+C handling
 * - Fish-style autosuggestions
 */
public class CliInterface {
    
    private final CliContext context;
    private final CommandExecutor executor;
    private final ComputerManager computerManager;
    private final SessionManager sessionManager;
    private Terminal terminal;
    private LineReader lineReader;
    private boolean running;
    
    public CliInterface(ComputerManager computerManager, SessionManager sessionManager, 
                       ProcessManager processManager, HostScanner hostScanner) {
        this.context = new CliContext();
        this.executor = new CommandExecutor(context, computerManager, sessionManager, 
                                           processManager, hostScanner);
        this.computerManager = computerManager;
        this.sessionManager = sessionManager;
        this.running = false;
        
        try {
            initializeTerminal();
        } catch (IOException e) {
            System.err.println("Failed to initialize terminal: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Initialize JLine terminal with all advanced features
     */
    private void initializeTerminal() throws IOException {
        // Build terminal
        terminal = TerminalBuilder.builder()
            .system(true)
            .build();
        
        // Create parser
        Parser parser = new DefaultParser();
        
        // Create completer
        CliCompleter completer = new CliCompleter(computerManager, sessionManager, context);
        
        // Create highlighter
        CliHighlighter highlighter = new CliHighlighter();
        
        // Create history with persistent storage
        DefaultHistory history = new DefaultHistory();
        
        // Build line reader with all features enabled
        lineReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .parser(parser)
            .completer(completer)
            .highlighter(highlighter)
            .history(history)
            .option(LineReader.Option.AUTO_FRESH_LINE, true)
            .option(LineReader.Option.AUTO_GROUP, true)
            .option(LineReader.Option.AUTO_MENU_LIST, true)
            .option(LineReader.Option.AUTO_REMOVE_SLASH, false)
            .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
            .option(LineReader.Option.INSERT_TAB, false)
            .variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".manager_cli_history"))
            .variable(LineReader.COMPLETION_STYLE_LIST_GROUP, "fg:cyan")
            .variable(LineReader.COMPLETION_STYLE_DESCRIPTION, "fg:yellow,italic")
            .variable(LineReader.COMPLETION_STYLE_STARTING, "fg:green,bold")
            .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%P > ")
            .build();
        
        // Configure autosuggestion (fish-style)
        lineReader.setAutosuggestion(LineReader.SuggestionType.HISTORY);
    }
    
    /**
     * Enable verbose mode - logs will be shown with prompt redrawing
     */
    public void setVerboseMode(boolean verbose) {
        // Set up prompt redrawer in verbose mode
        if (verbose) {
            Logger.setPromptRedrawer(() -> {
                // Redraw the prompt after log message
                if (lineReader != null) {
                    lineReader.callWidget(LineReader.REDRAW_LINE);
                    lineReader.callWidget(LineReader.REDISPLAY);
                }
            });
        } else {
            Logger.setPromptRedrawer(null);
        }
    }
    
    /**
     * Start the interactive CLI loop
     */
    public void start() {
        running = true;
        printWelcome();
        printQuickTips();
        
        while (running) {
            try {
                // Read command with all JLine features
                String command = lineReader.readLine(context.getPrompt());
                
                // Trim command
                command = command.trim();
                
                // Skip empty lines
                if (command.isEmpty()) {
                    continue;
                }
                
                // Check for exit
                if (command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("quit")) {
                    running = false;
                    printGoodbye();
                    break;
                }
                
                // Execute command
                try {
                    String result = executor.execute(command);
                    if (result != null && !result.isEmpty()) {
                        terminal.writer().println(result);
                    }
                } catch (Exception e) {
                    terminal.writer().println("Error executing command: " + e.getMessage());
                    if (System.getProperty("debug") != null) {
                        e.printStackTrace(terminal.writer());
                    }
                }
                
                terminal.writer().println(); // Empty line after result
                terminal.flush();
                
            } catch (UserInterruptException e) {
                // Ctrl+C pressed - don't exit, just show new prompt
                terminal.writer().println("^C");
                terminal.writer().println("Type 'exit' or press Ctrl+D to quit");
                terminal.flush();
                
            } catch (EndOfFileException e) {
                // Ctrl+D pressed (EOF) - graceful exit
                running = false;
                printGoodbye();
                break;
            }
        }
        
        // Cleanup
        try {
            if (terminal != null) {
                terminal.close();
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }
    
    /**
     * Execute a single command (non-interactive mode)
     */
    public String executeCommand(String command) {
        return executor.execute(command);
    }
    
    /**
     * Stop the CLI
     */
    public void stop() {
        running = false;
    }
    
    /**
     * Print welcome message
     */
    private void printWelcome() {
        terminal.writer().println("╔══════════════════════════════════════════════════════════════╗");
        terminal.writer().println("║          Manager CLI - Enhanced Interactive Mode             ║");
        terminal.writer().println("║                                                              ║");
        terminal.writer().println("║  🎯 Type 'help' for available commands                       ║");
        terminal.writer().println("║  🚪 Type 'exit' or press Ctrl+D to quit                      ║");
        terminal.writer().println("╚══════════════════════════════════════════════════════════════╝");
        terminal.writer().println();
        terminal.flush();
    }
    
    /**
     * Print quick tips for new features
     */
    private void printQuickTips() {
        terminal.writer().println("✨ Enhanced Features:");
        terminal.writer().println("   • Press TAB for auto-completion");
        terminal.writer().println("   • Press ↑/↓ arrows for command history");
        terminal.writer().println("   • Commands are syntax-highlighted");
        terminal.writer().println("   • Fish-style suggestions as you type");
        terminal.writer().println("   • Press Ctrl+C to cancel current line");
        terminal.writer().println();
        terminal.flush();
    }
    
    /**
     * Print goodbye message
     */
    private void printGoodbye() {
        terminal.writer().println();
        terminal.writer().println("👋 Goodbye! Thank you for using Manager CLI.");
        terminal.flush();
    }
}
