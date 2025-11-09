// ============================================================================== //
//                          DASHBOARD.JS - Agent Grid View                       //
// ============================================================================== //

(function() {
    'use strict';
    
    // ============================================================================ //
    //                              CONFIGURATION                                   //
    // ============================================================================ //
    
    const CONFIG = {
        refreshInterval: 5000,  // Refresh agent list every 5 seconds
        apiEndpoints: {
            agents: (window.contextPath || '') + '/api/agents',
            scan: (window.contextPath || '') + '/api/scan',
            sessions: (window.contextPath || '') + '/api/sessions'
        }
    };
    
    // Store agent sessions data
    let agentSessions = {};

    // ============================================================================ //
    //                              INITIALIZATION                                  //
    // ============================================================================ //
    
    $(document).ready(function() {
        // 1. Load agents on page load
        loadAgents();
        
        // 2. Setup periodic refresh
        setInterval(loadAgents, CONFIG.refreshInterval);
        
        // 3. Setup scan button
        $('#scanBtn').on('click', triggerScan);
    });
    
    // ============================================================================ //
    //                              LOAD AGENTS                                     //
    // ============================================================================ //
    
    /**
     * Load all agents from API and display in grid.
     */
    function loadAgents() {
        const url = CONFIG.apiEndpoints.agents;
        console.log('Loading agents from:', url);
        
        $.ajax({
            url: url,
            method: 'GET',
            dataType: 'json',
            success: function(agents) {
                console.log('Agents loaded successfully:', agents);
                
                // Load latest session for each agent
                loadAgentSessions(agents, function() {
                    renderAgentGrid(agents);
                    updateAgentCount(agents.length);
                });
            },
            error: function(xhr, status, error) {
                console.error('Failed to load agents:', {
                    status: xhr.status,
                    statusText: xhr.statusText,
                    error: error,
                    response: xhr.responseText
                });
                showError('Failed to load agents. Please refresh the page.');
            }
        });
    }
    
    /**
     * Load latest sessions for all agents.
     */
    function loadAgentSessions(agents, callback) {
        if (agents.length === 0) {
            callback();
            return;
        }
        
        let loaded = 0;
        const total = agents.length;
        
        agents.forEach(function(agent) {
            const url = CONFIG.apiEndpoints.sessions + 
                        '?mac=' + encodeURIComponent(agent.macAddress) + 
                        '&latest=true';
            
            $.ajax({
                url: url,
                method: 'GET',
                dataType: 'json',
                success: function(session) {
                    agentSessions[agent.macAddress] = session;
                },
                error: function() {
                    agentSessions[agent.macAddress] = null;
                },
                complete: function() {
                    loaded++;
                    if (loaded === total) {
                        callback();
                    }
                }
            });
        });
    }
    
    // ============================================================================ //
    //                              RENDER AGENT GRID                               //
    // ============================================================================ //
    
    /**
     * Render agent cards in grid layout.
     * 
     * @param {Array} agents - Array of agent objects
     */
    function renderAgentGrid(agents) {
        const $grid = $('#agentGrid');
        
        if (agents.length === 0) {
            $grid.html(`
                <div class="text-center py-5 text-muted">
                    <i class="fas fa-server fa-3x mb-3"></i>
                    <p>No agents found. Run a scan to discover agents.</p>
                </div>
            `);
            return;
        }
        
        // Build grid HTML
        let gridHtml = '';
        agents.forEach(function(agent) {
            gridHtml += buildAgentCard(agent);
        });
        
        $grid.html(gridHtml);
        
        // Set colors for progress bars
        agents.forEach(function(agent) {
            const session = agentSessions[agent.macAddress];
            const cpuUsage = session ? session.cpuUsage : 0;
            const ramUsageBytes = session ? session.ramUsage : 0;
            const totalRamBytes = session ? session.totalRam : 1;
            const ramUsage = totalRamBytes > 0 ? (ramUsageBytes / totalRamBytes * 100) : 0;
            
            const cpuCanvasId = 'cpu-gauge-' + agent.macAddress.replace(/:/g, '');
            const ramCanvasId = 'ram-gauge-' + agent.macAddress.replace(/:/g, '');
            
            setProgressBarColor(cpuCanvasId, cpuUsage, 'CPU');
            setProgressBarColor(ramCanvasId, ramUsage, 'RAM');
        });
        
        // Attach click handlers
        $('.agent-card').on('click', function() {
            const mac = $(this).data('mac');
            navigateToAgentDetail(mac);
        });
    }
    
    // ============================================================================ //
    //                              BUILD AGENT CARD                                //
    // ============================================================================ //
    
    /**
     * Build HTML for a single agent card.
     * 
     * @param {Object} agent - Agent object from API
     * @returns {String} HTML string for agent card
     */
    function buildAgentCard(agent) {
        // Determine OS icon
        const osIcon = getOSIcon(agent.os);
        
        // Format CPU frequency
        const cpuFreq = formatFrequency(agent.cpuMaxFreq);
        
        // Get latest session data
        const session = agentSessions[agent.macAddress];
        const cpuUsage = session ? session.cpuUsage : 0;
        const ramUsageBytes = session ? session.ramUsage : 0;
        const totalRamBytes = session ? session.totalRam : 1;
        const ramUsage = totalRamBytes > 0 ? (ramUsageBytes / totalRamBytes * 100) : 0;
        
        // Generate unique IDs for canvas elements
        const cpuCanvasId = 'cpu-gauge-' + agent.macAddress.replace(/:/g, '');
        const ramCanvasId = 'ram-gauge-' + agent.macAddress.replace(/:/g, '');
        
        return `
            <div class="agent-card" data-mac="${agent.macAddress}">
                <div class="agent-card-header">
                    <div class="agent-os-icon">
                        <i class="${osIcon}"></i>
                    </div>
                    <div class="agent-status online">
                        <i class="fas fa-circle"></i>
                    </div>
                </div>
                <div class="agent-card-body">
                    <h5 class="agent-hostname">${escapeHtml(agent.hostname)}</h5>
                    <p class="agent-ip text-muted mb-2">
                        <i class="fas fa-network-wired"></i> ${escapeHtml(agent.ipAddress)}
                    </p>
                    
                    <!-- Horizontal Gauges -->
                    <div class="mb-3">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <small class="text-muted">
                                <i class="fas fa-microchip"></i> CPU
                            </small>
                            <small class="fw-bold" id="${cpuCanvasId}-value">${cpuUsage.toFixed(1)}%</small>
                        </div>
                        <div class="progress" style="height: 8px;">
                            <div id="${cpuCanvasId}" class="progress-bar" role="progressbar" 
                                 style="width: ${cpuUsage}%;" 
                                 aria-valuenow="${cpuUsage}" aria-valuemin="0" aria-valuemax="100">
                            </div>
                        </div>
                    </div>
                    
                    <div class="mb-3">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <small class="text-muted">
                                <i class="fas fa-memory"></i> RAM
                            </small>
                            <small class="fw-bold" id="${ramCanvasId}-value">${ramUsage.toFixed(1)}%</small>
                        </div>
                        <div class="progress" style="height: 8px;">
                            <div id="${ramCanvasId}" class="progress-bar" role="progressbar" 
                                 style="width: ${ramUsage}%;" 
                                 aria-valuenow="${ramUsage}" aria-valuemin="0" aria-valuemax="100">
                            </div>
                        </div>
                    </div>
                    
                    <div class="agent-specs">
                        <small class="text-muted d-block">
                            <i class="fas fa-microchip"></i> ${escapeHtml(agent.cpuName)}
                        </small>
                        <small class="text-muted d-block">
                            <i class="fas fa-tachometer-alt"></i> ${cpuFreq}
                        </small>
                        <small class="text-muted d-block">
                            <i class="fas fa-server"></i> ${agent.physicalCores} cores / ${agent.logicalCores} threads
                        </small>
                    </div>
                </div>
                <div class="agent-card-footer text-muted">
                    <small>
                        <i class="fas fa-fingerprint"></i> ${formatMAC(agent.macAddress)}
                    </small>
                </div>
            </div>
        `;
    }
    
    // ============================================================================ //
    //                              HELPER FUNCTIONS                                //
    // ============================================================================ //
    
    /**
     * Set progress bar color based on usage level.
     * 
     * @param {String} barId - Progress bar element ID
     * @param {Number} usage - Usage percentage (0-100)
     * @param {String} type - Chart type ('CPU' or 'RAM')
     */
    function setProgressBarColor(barId, usage, type) {
        const $bar = $('#' + barId);
        if (!$bar.length) return;
        
        usage = Math.min(100, Math.max(0, usage || 0));
        
        // Remove all bg classes
        $bar.removeClass('bg-primary bg-success bg-warning bg-danger');
        
        // Add appropriate class based on usage level
        if (usage >= 80) {
            $bar.addClass('bg-danger');
        } else if (usage >= 60) {
            $bar.addClass('bg-warning');
        } else {
            if (type === 'CPU') {
                $bar.addClass('bg-primary');
            } else {
                $bar.addClass('bg-success');
            }
        }
    }
    
    /**
     * Get Font Awesome icon class for OS.
     */
    function getOSIcon(os) {
        const osLower = (os || '').toLowerCase();
        if (osLower.includes('windows')) return 'fab fa-windows';
        if (osLower.includes('mac') || osLower.includes('darwin')) return 'fab fa-apple';
        if (osLower.includes('linux')) return 'fab fa-linux';
        return 'fas fa-desktop';
    }
    
    /**
     * Format frequency in Hz to GHz.
     */
    function formatFrequency(hz) {
        if (!hz) return 'N/A';
        const ghz = (hz / 1000000000).toFixed(2);
        return `${ghz} GHz`;
    }
    
    /**
     * Format MAC address with colons.
     */
    function formatMAC(mac) {
        if (!mac) return 'N/A';
        return mac.replace(/(.{2})(?=.)/g, '$1:').toUpperCase();
    }
    
    /**
     * Escape HTML to prevent XSS.
     */
    function escapeHtml(text) {
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return (text || '').replace(/[&<>"']/g, m => map[m]);
    }
    
    /**
     * Update agent count badge.
     */
    function updateAgentCount(count) {
        $('#agentCount').text(count);
    }
    
    /**
     * Navigate to agent detail page.
     */
    function navigateToAgentDetail(macAddress) {
        window.location.href = `${window.contextPath || ''}/agent-detail.jsp?mac=${encodeURIComponent(macAddress)}`;
    }
    
    /**
     * Show error message.
     */
    function showError(message) {
        $('#agentGrid').html(`
            <div class="alert alert-danger" role="alert">
                <i class="fas fa-exclamation-triangle"></i> ${escapeHtml(message)}
            </div>
        `);
    }
    
    // ============================================================================ //
    //                              SCAN FUNCTIONALITY                              //
    // ============================================================================ //
    
    /**
     * Trigger network scan via API.
     */
    function triggerScan() {
        const $btn = $('#scanBtn');
        const $status = $('#scanStatus');
        
        // Disable button and show loading
        $btn.prop('disabled', true);
        $btn.html('<i class="fas fa-spinner fa-spin"></i> Scanning...');
        
        $.ajax({
            url: CONFIG.apiEndpoints.scan,
            method: 'POST',
            dataType: 'json',
            success: function(response) {
                if (response.success) {
                    $status.html('<span class="text-success"><i class="fas fa-check-circle"></i> ' + 
                                escapeHtml(response.message) + '</span>');
                    // Reload agents after scan
                    setTimeout(loadAgents, 2000);
                } else {
                    $status.html('<span class="text-danger"><i class="fas fa-times-circle"></i> ' + 
                                escapeHtml(response.message) + '</span>');
                }
            },
            error: function(xhr, status, error) {
                $status.html('<span class="text-danger"><i class="fas fa-times-circle"></i> ' +
                            'Scan failed. Make sure Manager is running.</span>');
            },
            complete: function() {
                // Re-enable button
                $btn.prop('disabled', false);
                $btn.html('<i class="fas fa-sync-alt"></i> Trigger Scan');
                
                // Clear status after 5 seconds
                setTimeout(function() {
                    $status.html('');
                }, 5000);
            }
        });
    }
    
})();
