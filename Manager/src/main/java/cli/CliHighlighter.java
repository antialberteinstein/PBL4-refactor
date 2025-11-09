package cli;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * CliHighlighter - Provides syntax highlighting for CLI commands
 * 
 * Color scheme:
 * - Commands: CYAN (list, use, head, tail, etc.)
 * - Keywords: YELLOW (for, agent, session, limit, etc.)
 * - Numbers: MAGENTA
 * - Strings/IDs: GREEN
 * - Errors: RED
 */
public class CliHighlighter implements Highlighter {
    
    // Command keywords (cyan)
    private static final Set<String> COMMANDS = new HashSet<>(Arrays.asList(
        "list", "show", "use", "head", "tail", "scan", "status", 
        "help", "exit", "quit", "clear", "reset"
    ));
    
    // Target keywords (yellow)
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "for", "agent", "session", "agents", "sessions", "processes", 
        "current", "limit", "offset", "from", "to"
    ));
    
    // Number pattern
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\d+$");
    
    // MAC address pattern
    private static final Pattern MAC_PATTERN = Pattern.compile(
        "^[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}$"
    );
    
    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        
        if (buffer == null || buffer.isEmpty()) {
            return builder.toAttributedString();
        }
        
        String[] tokens = buffer.split("\\s+");
        int currentPos = 0;
        
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            
            // Find the actual position of this token in the buffer
            int tokenStart = buffer.indexOf(token, currentPos);
            
            // Add any whitespace before this token
            if (tokenStart > currentPos) {
                builder.append(buffer.substring(currentPos, tokenStart));
            }
            
            // Colorize the token
            AttributedStyle style = getStyleForToken(token, i);
            builder.styled(style, token);
            
            currentPos = tokenStart + token.length();
        }
        
        // Add any remaining characters (trailing whitespace)
        if (currentPos < buffer.length()) {
            builder.append(buffer.substring(currentPos));
        }
        
        return builder.toAttributedString();
    }
    
    /**
     * Get the appropriate style for a token based on its position and content
     */
    private AttributedStyle getStyleForToken(String token, int position) {
        String lowerToken = token.toLowerCase();
        
        // First token is always a command
        if (position == 0) {
            if (COMMANDS.contains(lowerToken)) {
                return AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN).bold();
            } else {
                // Unknown command - red
                return AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
            }
        }
        
        // Keywords (for, agent, session, etc.)
        if (KEYWORDS.contains(lowerToken)) {
            return AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
        }
        
        // Numbers (limits, offsets, IDs)
        if (NUMBER_PATTERN.matcher(token).matches()) {
            return AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA);
        }
        
        // MAC addresses
        if (MAC_PATTERN.matcher(token).matches()) {
            return AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
        }
        
        // Default style for other tokens (agent IDs, etc.)
        return AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
    }
    
    @Override
    public void setErrorPattern(Pattern errorPattern) {
        // Not used
    }
    
    @Override
    public void setErrorIndex(int errorIndex) {
        // Not used
    }
}
