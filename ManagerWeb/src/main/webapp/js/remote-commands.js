/**
 * Remote Commands Module
 * Handles sending remote commands to Agents via Manager
 */

const RemoteCommands = {
    /**
     * Send command to Manager's ExternalScanServer
     * 
     * @param {string} command - Command type (KILL_PROCESS, SHUTDOWN, SEND_MESSAGE)
     * @param {string} mac - MAC address of target Agent
     * @param {object} params - Additional parameters (pid, delay, message)
     * @returns {Promise<object>} - Response from server
     */
    async sendCommand(command, mac, params = {}) {
        try {
            const formData = new URLSearchParams();
            formData.append('command', command);
            formData.append('mac', mac);
            
            // Add command-specific parameters
            if (params.pid !== undefined) {
                formData.append('pid', params.pid);
            }
            if (params.delay !== undefined) {
                formData.append('delay', params.delay);
            }
            if (params.message !== undefined) {
                formData.append('message', params.message);
            }
            
            const response = await fetch('remote-command', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: formData.toString()
            });
            
            const result = await response.json();
            
            if (!response.ok) {
                throw new Error(result.message || 'Server error');
            }
            
            return result;
            
        } catch (error) {
            console.error('Error sending remote command:', error);
            throw error;
        }
    },
    
    /**
     * Kill process on Agent
     * 
     * @param {string} mac - MAC address of Agent
     * @param {number} pid - Process ID to kill
     * @returns {Promise<object>} - Response
     */
    async killProcess(mac, pid) {
        return this.sendCommand('KILL_PROCESS', mac, { pid });
    },
    
    /**
     * Shutdown Agent computer
     * 
     * @param {string} mac - MAC address of Agent
     * @param {number} delay - Delay in seconds (default 60)
     * @returns {Promise<object>} - Response
     */
    async shutdown(mac, delay = 60) {
        return this.sendCommand('SHUTDOWN', mac, { delay });
    },
    
    /**
     * Send message to Agent
     * 
     * @param {string} mac - MAC address of Agent
     * @param {string} message - Message to send
     * @returns {Promise<object>} - Response
     */
    async sendMessage(mac, message) {
        return this.sendCommand('SEND_MESSAGE', mac, { message });
    },
    
    /**
     * Show confirmation dialog and execute command
     * 
     * @param {string} title - Dialog title
     * @param {string} message - Confirmation message
     * @param {Function} commandFn - Command function to execute
     * @returns {Promise<void>}
     */
    async confirmAndExecute(title, message, commandFn) {
        if (!confirm(message)) {
            return;
        }
        
        try {
            const result = await commandFn();
            
            if (result.success) {
                alert('Success: ' + result.message);
            } else {
                alert('Failed: ' + result.message);
            }
            
        } catch (error) {
            alert('Error: ' + error.message);
        }
    },
    
    /**
     * Kill process with confirmation
     * 
     * @param {string} mac - MAC address
     * @param {number} pid - Process ID
     * @param {string} processName - Process name (for display)
     */
    async killProcessWithConfirm(mac, pid, processName = '') {
        const displayName = processName ? `${processName} (PID: ${pid})` : `PID: ${pid}`;
        const message = `Are you sure you want to kill process ${displayName} on Agent ${mac}?`;
        
        await this.confirmAndExecute(
            'Kill Process',
            message,
            () => this.killProcess(mac, pid)
        );
    },
    
    /**
     * Shutdown Agent with confirmation
     * 
     * @param {string} mac - MAC address
     * @param {number} delay - Delay in seconds
     */
    async shutdownWithConfirm(mac, delay = 60) {
        const message = `Are you sure you want to shutdown Agent ${mac} in ${delay} seconds?\n\nThis will turn off the computer.`;
        
        await this.confirmAndExecute(
            'Shutdown Agent',
            message,
            () => this.shutdown(mac, delay)
        );
    },
    
    /**
     * Send message with prompt
     * 
     * @param {string} mac - MAC address
     */
    async sendMessageWithPrompt(mac) {
        const message = prompt('Enter message to send to Agent ' + mac + ':');
        
        if (!message || message.trim() === '') {
            return;
        }
        
        try {
            const result = await this.sendMessage(mac, message.trim());
            
            if (result.success) {
                alert('Message sent successfully!');
            } else {
                alert('Failed to send message: ' + result.message);
            }
            
        } catch (error) {
            alert('Error: ' + error.message);
        }
    }
};

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = RemoteCommands;
}
