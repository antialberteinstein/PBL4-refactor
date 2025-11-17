// ============================================================================== //
//                      AGENT-DETAIL.JS - Real-time Charts                       //
// ============================================================================== //

(function() {
    'use strict';
    
    // ============================================================================ //
    //                              CONFIGURATION                                   //
    // ============================================================================ //
    
    const CONFIG = {
        updateInterval: 2000,   // Update charts every 2 seconds
        maxProcessDisplay: 100, // Limit process display to 100 for performance
        apiEndpoints: {
            sessions: (window.contextPath || '') + '/api/sessions',
            processes: (window.contextPath || '') + '/api/processes'
        },
        // Time slot configurations
        timeSlotSizes: {
            '30m': { 
                label: '30 Minutes', 
                minutes: 30, 
                dataInterval: 10000,  // 10 seconds between points
                maxPoints: 180,       // 30 min / 10s = 180 points
                slotsPerDay: 48
            },
            '1h': { 
                label: '1 Hour', 
                minutes: 60, 
                dataInterval: 20000,  // 20 seconds
                maxPoints: 180,       // 60 min / 20s = 180 points
                slotsPerDay: 24
            },
            '2h': { 
                label: '2 Hours', 
                minutes: 120, 
                dataInterval: 40000,  // 40 seconds
                maxPoints: 180,       // 120 min / 40s = 180 points
                slotsPerDay: 12
            },
            '3h': { 
                label: '3 Hours', 
                minutes: 180, 
                dataInterval: 60000,  // 1 minute
                maxPoints: 180,       // 180 min / 1min = 180 points
                slotsPerDay: 8
            },
            '6h': { 
                label: '6 Hours', 
                minutes: 360, 
                dataInterval: 120000, // 2 minutes
                maxPoints: 180,       // 360 min / 2min = 180 points
                slotsPerDay: 4
            }
        }
    };
    
    // Global variables
    let macAddress = null;
    let cpuChart = null;
    let ramChart = null;
    let processCpuChart = null;
    let processRamChart = null;
    let updateTimer = null;
    let latestSessionId = null;
    let totalRamBytes = 0; // Store total RAM in bytes for chart calculations
    
    // Time slot state
    let selectedDate = new Date(); // Current date by default
    let selectedSlotSize = '1h';   // Default slot size
    let currentSlotIndex = null;   // Current time slot (null = realtime)
    let allTimeSlots = [];         // All time slots for selected date
    let isRealTimeMode = true;     // Real-time mode flag
    
    // Process table state
    let allProcesses = [];
    let currentSort = 'cpu-desc';
    let currentFilter = 'all';
    let currentSearch = '';
    let processViewMode = 'table'; // 'table' or 'charts'
    let topNProcesses = 10;        // Default top 10
    let currentRamUnit = 'auto';   // Default auto unit selection
    
    // Others drill-down state
    let othersDetailChart = null;
    let currentOthersData = null;  // Stores processes in "Others"
    let currentOthersType = null;  // 'cpu' or 'ram'
    let othersTopN = 10;           // Top N for others view
    
    // ============================================================================ //
    //                              INITIALIZATION                                  //
    // ============================================================================ //
    
    $(document).ready(function() {
        // 1. Get MAC address from URL
        macAddress = getURLParameter('mac');
        if (!macAddress) {
            showError('No agent specified');
            return;
        }
        
        // 2. Load agent info
        loadAgentInfo();
        
        // 3. Initialize charts
        initializeCharts();
        
        // 4. Initialize process charts
        initializeProcessCharts();
        
        // 5. Initialize others detail chart
        initializeOthersChart();
        
        // 6. Setup time slot controls
        setupTimeSlotControls();
        
        // 7. Generate time slots for today
        generateTimeSlots();
        
        // 8. Load initial data (real-time mode)
        loadChartData();
        
        // 9. Setup real-time updates
        startRealTimeUpdates();
        
        // 10. Setup chart type dropdowns
        setupChartTypeHandlers();
        
        // 11. Setup process table controls
        setupProcessTableControls();
        
        // 12. Setup others modal controls
        setupOthersModalControls();
        
        // 13. Setup remote command buttons
        setupRemoteCommandButtons();
    });
    
    // ============================================================================ //
    //                              LOAD AGENT INFO                                 //
    // ============================================================================ //
    
    function loadAgentInfo() {
        $.ajax({
            url: (window.contextPath || '') + '/api/agents',
            method: 'GET',
            dataType: 'json',
            success: function(agents) {
                const agent = agents.find(a => a.macAddress === macAddress);
                if (agent) {
                    renderAgentInfo(agent);
                } else {
                    showError('Agent not found');
                }
            },
            error: function() {
                showError('Failed to load agent information');
            }
        });
    }
    
    function renderAgentInfo(agent) {
        const osIcon = getOSIcon(agent.os);
        const cpuFreq = formatFrequency(agent.cpuMaxFreq);
        
        // Store total RAM for chart calculations
        totalRamBytes = agent.totalRam || 0;
        
        // Reinitialize RAM chart with new options if total RAM is now available
        if (ramChart && totalRamBytes > 0) {
            ramChart.options = getChartOptions('RAM Usage (%)', 0, 100, true);
            ramChart.update('none');
        }
        
        $('#agentInfo').html(`
            <div class="row">
                <div class="col-md-6">
                    <h4><i class="${osIcon}"></i> ${escapeHtml(agent.hostname)}</h4>
                    <p class="text-muted mb-0">
                        <i class="fas fa-network-wired"></i> <strong>IP:</strong> ${escapeHtml(agent.ipAddress)}
                    </p>
                    <p class="text-muted mb-0">
                        <i class="fas fa-fingerprint"></i> <strong>MAC:</strong> ${formatMAC(agent.macAddress)}
                    </p>
                    <p class="text-muted mb-0">
                        <i class="fas fa-laptop"></i> <strong>OS:</strong> ${escapeHtml(agent.os)} (${escapeHtml(agent.architecture)})
                    </p>
                </div>
                <div class="col-md-6">
                    <p class="text-muted mb-0">
                        <i class="fas fa-microchip"></i> <strong>CPU:</strong> ${escapeHtml(agent.cpuName)}
                    </p>
                    <p class="text-muted mb-0">
                        <i class="fas fa-tachometer-alt"></i> <strong>Frequency:</strong> ${cpuFreq}
                    </p>
                    <p class="text-muted mb-0">
                        <i class="fas fa-server"></i> <strong>Cores:</strong> ${agent.physicalCores} physical / ${agent.logicalCores} logical
                    </p>
                    <p class="text-muted mb-0">
                        <i class="fas fa-building"></i> <strong>Manufacturer:</strong> ${escapeHtml(agent.manufacturer)}
                    </p>
                </div>
            </div>
        `);
    }
    
    // ============================================================================ //
    //                              INITIALIZE CHARTS                               //
    // ============================================================================ //
    
    function initializeCharts() {
        // CPU Chart
        const cpuCtx = document.getElementById('cpuChart').getContext('2d');
        cpuChart = new Chart(cpuCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    label: 'CPU Usage (%)',
                    data: [],
                    borderColor: 'rgb(75, 192, 192)',
                    backgroundColor: 'rgba(75, 192, 192, 0.2)',
                    tension: 0.4,
                    fill: false,
                    // Hide points by default, show on hover
                    pointRadius: 0,
                    pointHoverRadius: 6,
                    pointHitRadius: 10,
                    pointBorderWidth: 2,
                    pointHoverBorderWidth: 2
                }]
            },
            options: getChartOptions('CPU Usage (%)', 0, 100)
        });
        
        // RAM Chart
        const ramCtx = document.getElementById('ramChart').getContext('2d');
        ramChart = new Chart(ramCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    label: 'RAM Usage (%)',
                    data: [],
                    borderColor: 'rgb(255, 99, 132)',
                    backgroundColor: 'rgba(255, 99, 132, 0.2)',
                    tension: 0.4,
                    fill: false,
                    // Hide points by default, show on hover
                    pointRadius: 0,
                    pointHoverRadius: 6,
                    pointHitRadius: 10,
                    pointBorderWidth: 2,
                    pointHoverBorderWidth: 2
                }]
            },
            options: getChartOptions('RAM Usage (%)', 0, 100, true) // true = isRAMChart
        });
    }
    
    function initializeProcessCharts() {
        // Process CPU Chart
        const processCpuCtx = document.getElementById('processCpuChart').getContext('2d');
        processCpuChart = new Chart(processCpuCtx, {
            type: 'doughnut',
            data: {
                labels: [],
                datasets: [{
                    label: 'CPU Usage (%)',
                    data: [],
                    backgroundColor: [],
                    borderColor: '#fff',
                    borderWidth: 2
                }]
            },
            options: getProcessChartOptions('CPU Usage by Process')
        });
        
        // Process RAM Chart
        const processRamCtx = document.getElementById('processRamChart').getContext('2d');
        processRamChart = new Chart(processRamCtx, {
            type: 'doughnut',
            data: {
                labels: [],
                datasets: [{
                    label: 'RAM Usage (MB)',
                    data: [],
                    backgroundColor: [],
                    borderColor: '#fff',
                    borderWidth: 2
                }]
            },
            options: getProcessChartOptions('RAM Usage by Process')
        });
    }
    
    function getProcessChartOptions(title) {
        return {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: true,
                    position: 'right',
                    labels: {
                        boxWidth: 12,
                        padding: 8,
                        font: {
                            size: 10
                        }
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.parsed || 0;
                            const dataset = context.dataset.data;
                            const total = dataset.reduce((a, b) => a + b, 0);
                            const percentage = ((value / total) * 100).toFixed(1);
                            
                            // Add click hint for "Others"
                            let tooltip = '';
                            if (context.dataset.label.includes('CPU')) {
                                tooltip = `${label}: ${value.toFixed(2)}% (${percentage}% of total)`;
                            } else {
                                tooltip = `${label}: ${value.toFixed(2)} MB (${percentage}% of total)`;
                            }
                            
                            // Add hint for Others
                            if (label === 'Others') {
                                tooltip += '\n💡 Click to view details';
                            }
                            
                            return tooltip;
                        }
                    }
                }
            },
            onClick: (event, activeElements) => {
                if (activeElements.length > 0) {
                    const index = activeElements[0].index;
                    const label = event.chart.data.labels[index];
                    
                    // Check if clicked on "Others"
                    if (label === 'Others') {
                        const chartType = event.chart.canvas.id === 'processCpuChart' ? 'cpu' : 'ram';
                        const N = parseInt(topNProcesses);
                        
                        // Get the "others" processes
                        let othersProcesses;
                        if (chartType === 'cpu') {
                            const sortedByCpu = [...allProcesses].sort((a, b) => b.cpuUsage - a.cpuUsage);
                            othersProcesses = sortedByCpu.slice(N);
                        } else {
                            const sortedByRam = [...allProcesses].sort((a, b) => b.ramUsage - a.ramUsage);
                            othersProcesses = sortedByRam.slice(N);
                        }
                        
                        // Show others detail modal
                        showOthersDetail(chartType, othersProcesses);
                    }
                }
            },
            onHover: (event, activeElements) => {
                // Change cursor for "Others"
                if (activeElements.length > 0) {
                    const index = activeElements[0].index;
                    const label = event.chart.data.labels[index];
                    event.native.target.style.cursor = label === 'Others' ? 'pointer' : 'default';
                } else {
                    event.native.target.style.cursor = 'default';
                }
            }
        };
    }
    
    function renderProcessCharts(processes) {
        if (!processes || processes.length === 0) {
            // Clear charts
            processCpuChart.data.labels = [];
            processCpuChart.data.datasets[0].data = [];
            processCpuChart.data.datasets[0].backgroundColor = [];
            processCpuChart.update();
            
            processRamChart.data.labels = [];
            processRamChart.data.datasets[0].data = [];
            processRamChart.data.datasets[0].backgroundColor = [];
            processRamChart.update();
            return;
        }
        
        const N = parseInt(topNProcesses);
        
        // Show warning if N > 10
        showTopNWarning(N);
        
        // Sort by CPU and RAM separately
        const sortedByCpu = [...processes].sort((a, b) => b.cpuUsage - a.cpuUsage);
        const sortedByRam = [...processes].sort((a, b) => b.ramUsage - a.ramUsage);
        
        // Get top N and calculate "Others"
        const topCpuProcesses = sortedByCpu.slice(0, N);
        const othersCpuUsage = sortedByCpu.slice(N).reduce((sum, p) => sum + p.cpuUsage, 0);
        
        const topRamProcesses = sortedByRam.slice(0, N);
        const othersRamUsage = sortedByRam.slice(N).reduce((sum, p) => sum + p.ramUsage, 0);
        
        // Generate colors
        const colors = generateChartColors(N + 1); // +1 for "Others"
        
        // Prepare CPU chart data
        const cpuLabels = topCpuProcesses.map(p => p.name);
        const cpuData = topCpuProcesses.map(p => p.cpuUsage);
        
        if (othersCpuUsage > 0) {
            cpuLabels.push('Others');
            cpuData.push(othersCpuUsage);
        }
        
        // Prepare RAM chart data
        const ramLabels = topRamProcesses.map(p => p.name);
        const ramData = topRamProcesses.map(p => p.ramUsage / (1024 * 1024)); // Convert to MB
        
        if (othersRamUsage > 0) {
            ramLabels.push('Others');
            ramData.push(othersRamUsage / (1024 * 1024)); // Convert to MB
        }
        
        // Update CPU chart
        processCpuChart.data.labels = cpuLabels;
        processCpuChart.data.datasets[0].data = cpuData;
        processCpuChart.data.datasets[0].backgroundColor = colors.slice(0, cpuLabels.length);
        processCpuChart.update('none');
        
        // Update RAM chart
        processRamChart.data.labels = ramLabels;
        processRamChart.data.datasets[0].data = ramData;
        processRamChart.data.datasets[0].backgroundColor = colors.slice(0, ramLabels.length);
        processRamChart.update('none');
    }
    
    function generateChartColors(count) {
        // Predefined color palette
        const palette = [
            '#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF',
            '#FF9F40', '#FF6384', '#C9CBCF', '#4BC0C0', '#FF6384',
            '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF', '#FF9F40',
            '#E7E9ED', '#71B37C', '#EC932F', '#BA68C8', '#4DD0E1'
        ];
        
        const colors = [];
        for (let i = 0; i < count; i++) {
            colors.push(palette[i % palette.length]);
        }
        
        // Make "Others" always gray
        if (count > 0) {
            colors[count - 1] = '#C9CBCF';
        }
        
        return colors;
    }
    
    function showTopNWarning(N) {
        const warningDiv = $('#topNWarning');
        if (N > 10) {
            warningDiv.removeClass('d-none');
        } else {
            warningDiv.addClass('d-none');
        }
    }
    
    // ============================================================================ //
    //                          RAM UNIT CONVERSION                                 //
    // ============================================================================ //
    
    function formatRAM(bytes, unit) {
        if (!bytes || bytes === 0) return '0';
        
        let value, unitLabel, decimals;
        
        switch(unit) {
            case 'bytes':
                value = bytes;
                unitLabel = 'B';
                decimals = 0;
                break;
            case 'kb':
                value = bytes / 1000;
                unitLabel = 'KB';
                decimals = 2;
                break;
            case 'kib':
                value = bytes / 1024;
                unitLabel = 'KiB';
                decimals = 2;
                break;
            case 'mb':
                value = bytes / (1000 * 1000);
                unitLabel = 'MB';
                decimals = 2;
                break;
            case 'mib':
                value = bytes / (1024 * 1024);
                unitLabel = 'MiB';
                decimals = 2;
                break;
            case 'gb':
                value = bytes / (1000 * 1000 * 1000);
                unitLabel = 'GB';
                decimals = 3;
                break;
            case 'gib':
                value = bytes / (1024 * 1024 * 1024);
                unitLabel = 'GiB';
                decimals = 3;
                break;
            case 'auto':
            default:
                // Auto select appropriate unit
                if (bytes >= 1024 * 1024 * 1024) {
                    value = bytes / (1024 * 1024 * 1024);
                    unitLabel = 'GiB';
                    decimals = 3;
                } else if (bytes >= 1024 * 1024) {
                    value = bytes / (1024 * 1024);
                    unitLabel = 'MiB';
                    decimals = 2;
                } else if (bytes >= 1024) {
                    value = bytes / 1024;
                    unitLabel = 'KiB';
                    decimals = 2;
                } else {
                    value = bytes;
                    unitLabel = 'B';
                    decimals = 0;
                }
                break;
        }
        
        return {
            value: value.toFixed(decimals),
            unit: unitLabel,
            formatted: `${value.toFixed(decimals)} ${unitLabel}`
        };
    }
    
    function updateRAMHeaders() {
        const unit = currentRamUnit;
        const unitLabels = {
            'auto': 'Auto',
            'bytes': 'B',
            'kb': 'KB',
            'kib': 'KiB',
            'mb': 'MB',
            'mib': 'MiB',
            'gb': 'GB',
            'gib': 'GiB'
        };
        const label = unitLabels[unit] || unit.toUpperCase();
        $('#ramHeader').text(`RAM (${label})`);
    }
    
    // ============================================================================ //
    //                      OTHERS DRILL-DOWN FUNCTIONALITY                         //
    // ============================================================================ //
    
    function initializeOthersChart() {
        const ctx = document.getElementById('othersDetailChart').getContext('2d');
        othersDetailChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: [],
                datasets: [{
                    label: 'Usage',
                    data: [],
                    backgroundColor: [],
                    borderColor: '#fff',
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false  // Hide legend for clean look
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                const label = context.label || '';
                                const value = context.parsed || 0;
                                const dataset = context.dataset.data;
                                const total = dataset.reduce((a, b) => a + b, 0);
                                const percentage = ((value / total) * 100).toFixed(1);
                                
                                if (currentOthersType === 'cpu') {
                                    return `${label}: ${value.toFixed(2)}% (${percentage}% of others)`;
                                } else {
                                    return `${label}: ${value.toFixed(2)} MB (${percentage}% of others)`;
                                }
                            }
                        }
                    }
                }
            }
        });
    }
    
    function setupOthersModalControls() {
        // Breadcrumb back to main
        $('#breadcrumbBack').on('click', function(e) {
            e.preventDefault();
            backToMainView();
        });
        
        // Top N selector for others
        $('#othersTopN').on('change', function() {
            const val = $(this).val();
            othersTopN = val === 'all' ? 9999 : parseInt(val);
            renderOthersChart();
        });
        
        // Chart type selector for others
        $('#othersChartType').on('change', function() {
            const newType = $(this).val();
            othersDetailChart.config.type = newType;
            othersDetailChart.update();
        });
    }
    
    function backToMainView() {
        // Hide others detail view
        $('#othersDetailView').addClass('d-none');
        $('#chartBreadcrumb').addClass('d-none');
        
        // Show main charts
        $('#mainChartsRow').removeClass('d-none');
        
        // Clear others data
        currentOthersData = null;
        currentOthersType = null;
    }
    
    function showOthersDetail(type, processes) {
        currentOthersType = type;
        currentOthersData = processes;
        
        // Update breadcrumb
        const typeLabel = type === 'cpu' ? 'CPU' : 'RAM';
        $('#breadcrumbCurrent').text(`${typeLabel} Others (${processes.length} processes)`);
        $('#othersChartTitle').text(`${typeLabel} Usage by Process (Others)`);
        
        // Update info bar
        $('#othersCount').text(processes.length);
        
        const totalUsage = processes.reduce((sum, p) => {
            return sum + (type === 'cpu' ? p.cpuUsage : p.ramUsage / (1024 * 1024));
        }, 0);
        
        if (type === 'cpu') {
            $('#othersTotalUsage').text(`${totalUsage.toFixed(2)}%`);
        } else {
            $('#othersTotalUsage').text(`${totalUsage.toFixed(2)} MB`);
        }
        
        // Render chart and table
        renderOthersChart();
        renderOthersTable();
        
        // Hide main charts, show others detail
        $('#mainChartsRow').addClass('d-none');
        $('#chartBreadcrumb').removeClass('d-none');
        $('#othersDetailView').removeClass('d-none');
    }
    
    function renderOthersChart() {
        if (!currentOthersData || currentOthersData.length === 0) {
            othersDetailChart.data.labels = [];
            othersDetailChart.data.datasets[0].data = [];
            othersDetailChart.data.datasets[0].backgroundColor = [];
            othersDetailChart.update();
            return;
        }
        
        // Sort processes
        const sorted = [...currentOthersData].sort((a, b) => {
            if (currentOthersType === 'cpu') {
                return b.cpuUsage - a.cpuUsage;
            } else {
                return b.ramUsage - a.ramUsage;
            }
        });
        
        // Get top N
        const displayCount = Math.min(othersTopN, sorted.length);
        const topProcesses = sorted.slice(0, displayCount);
        const remaining = sorted.slice(displayCount);
        
        // Prepare data
        const labels = topProcesses.map(p => p.name);
        const data = topProcesses.map(p => {
            if (currentOthersType === 'cpu') {
                return p.cpuUsage;
            } else {
                return p.ramUsage / (1024 * 1024);
            }
        });
        
        // Add "remaining" if needed
        if (remaining.length > 0) {
            labels.push(`Remaining (${remaining.length})`);
            const remainingSum = remaining.reduce((sum, p) => {
                if (currentOthersType === 'cpu') {
                    return sum + p.cpuUsage;
                } else {
                    return sum + (p.ramUsage / (1024 * 1024));
                }
            }, 0);
            data.push(remainingSum);
        }
        
        // Generate colors
        const colors = generateChartColors(labels.length);
        
        // Update chart
        othersDetailChart.data.labels = labels;
        othersDetailChart.data.datasets[0].data = data;
        othersDetailChart.data.datasets[0].backgroundColor = colors;
        othersDetailChart.data.datasets[0].label = currentOthersType === 'cpu' ? 'CPU Usage (%)' : 'RAM Usage (MB)';
        othersDetailChart.update('none');
    }
    
    function renderOthersTable() {
        if (!currentOthersData || currentOthersData.length === 0) {
            $('#othersTableBody').html('<tr><td colspan="5" class="text-center text-muted">No data</td></tr>');
            return;
        }
        
        // Sort processes
        const sorted = [...currentOthersData].sort((a, b) => {
            if (currentOthersType === 'cpu') {
                return b.cpuUsage - a.cpuUsage;
            } else {
                return b.ramUsage - a.ramUsage;
            }
        });
        
        // Build table rows
        const tbody = document.createElement('tbody');
        sorted.forEach((proc, index) => {
            const ramMB = (proc.ramUsage / (1024 * 1024)).toFixed(2);
            const cpuClass = proc.cpuUsage > 50 ? 'text-danger fw-bold' : '';
            const ramClass = ramMB > 500 ? 'text-warning fw-bold' : '';
            
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${index + 1}</td>
                <td>${proc.pid}</td>
                <td>${escapeHtml(proc.name)}</td>
                <td class="${cpuClass}">${proc.cpuUsage.toFixed(2)}%</td>
                <td class="${ramClass}">${ramMB}</td>
            `;
            tbody.appendChild(tr);
        });
        
        $('#othersTableBody').html(tbody.innerHTML);
    }
    
    function getChartOptions(title, minY, maxY, isRAMChart = false) {
        const options = {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: true,
                    position: 'top'
                },
                tooltip: {
                    mode: 'index',
                    intersect: false,
                    callbacks: {
                        title: function(context) {
                            // Format timestamp in tooltip
                            const timestamp = context[0].label;
                            return moment(parseInt(timestamp)).format('YYYY-MM-DD HH:mm:ss');
                        }
                    }
                }
            },
            scales: {
                x: {
                    display: true,
                    title: {
                        display: true,
                        text: 'Time'
                    },
                    ticks: {
                        callback: function(value, index, ticks) {
                            // Format timestamp on x-axis
                            const timestamp = this.getLabelForValue(value);
                            return moment(parseInt(timestamp)).format('HH:mm:ss');
                        },
                        maxRotation: 45,
                        minRotation: 0
                    }
                },
                y: {
                    display: true,
                    title: {
                        display: true,
                        text: title
                    },
                    min: minY,
                    max: maxY,
                    position: 'left'
                }
            },
            interaction: {
                mode: 'nearest',
                axis: 'x',
                intersect: false
            }
        };
        
        // Add secondary Y-axis for RAM chart showing actual GiB values
        if (isRAMChart && totalRamBytes > 0) {
            options.scales.y2 = {
                display: true,
                position: 'right',
                title: {
                    display: true,
                    text: 'RAM Usage (GiB)'
                },
                min: 0,
                max: totalRamBytes / (1024 * 1024 * 1024), // Convert bytes to GiB
                ticks: {
                    callback: function(value) {
                        return value.toFixed(2) + ' GiB';
                    }
                },
                grid: {
                    drawOnChartArea: false // Don't draw grid lines for secondary axis
                }
            };
            
            // Update tooltip for RAM chart to show both % and GiB
            options.plugins.tooltip.callbacks.label = function(context) {
                const percentage = context.parsed.y.toFixed(2);
                const gib = (totalRamBytes * percentage / 100 / (1024 * 1024 * 1024)).toFixed(2);
                return `RAM: ${percentage}% (${gib} GiB / ${(totalRamBytes / (1024 * 1024 * 1024)).toFixed(2)} GiB)`;
            };
        }
        
        return options;
    }
    
    // ============================================================================ //
    //                              LOAD CHART DATA                                 //
    // ============================================================================ //
    
    // Add flag to prevent concurrent chart data loading
    let isLoadingChartData = false;
    
    function loadChartData() {
        // Prevent concurrent requests
        if (isLoadingChartData) {
            console.log('Chart data loading already in progress, skipping...');
            return;
        }
        
        const params = {
            mac: macAddress
        };
        
        const slotConfig = CONFIG.timeSlotSizes[selectedSlotSize];
        let startTime, endTime;
        
        if (isRealTimeMode) {
            // Real-time mode: Get data for current time slot
            const now = Date.now();
            const currentSlot = getCurrentTimeSlot();
            startTime = currentSlot.startTime;
            endTime = now;
            
            updateChartTitle(`Real-time - ${currentSlot.label}`, true);
        } else {
            // Historical mode: Get data for selected time slot
            const slot = allTimeSlots[currentSlotIndex];
            startTime = slot.startTime;
            endTime = slot.endTime;
            
            updateChartTitle(slot.label, false);
        }
        
        params.startTime = startTime;
        params.endTime = endTime;
        params.limit = 10000; // Get all data, we'll sample/aggregate
        
        isLoadingChartData = true;
        showLoadingOverlay();
        
        $.ajax({
            url: CONFIG.apiEndpoints.sessions,
            method: 'GET',
            data: params,
            dataType: 'json',
            timeout: 10000, // 10 second timeout for chart data
            success: function(sessions) {
                hideLoadingOverlay();
                
                // Check if no data
                if (!sessions || sessions.length === 0) {
                    showNoDataMessage(startTime, endTime);
                    return;
                }
                
                // Sample/aggregate data to fit maxPoints
                const sampledData = sampleData(sessions, slotConfig);
                
                updateChartsWithData(sampledData);
                
                // Load processes for latest session
                if (sessions.length > 0) {
                    const latestSession = sessions[sessions.length - 1];
                    latestSessionId = latestSession.id;
                    loadProcesses(latestSession.id);
                }
            },
            error: function(xhr, status, error) {
                hideLoadingOverlay();
                console.error('Failed to load session data:', status, error);
                
                // Show user-friendly error message
                let errorMsg = 'Failed to load chart data';
                if (status === 'timeout') {
                    errorMsg = 'Request timed out. Server may be slow or unavailable.';
                } else if (xhr.status === 0) {
                    errorMsg = 'Network error. Please check your connection.';
                } else if (xhr.status >= 500) {
                    errorMsg = 'Server error. Please try again later.';
                }
                
                showError(errorMsg);
            },
            complete: function() {
                isLoadingChartData = false;
            }
        });
    }
    
    function showNoDataMessage(startTime, endTime) {
        const now = Date.now();
        const isFuture = startTime > now;
        const isPast = endTime < now;
        const isToday = isSameDay(new Date(startTime), new Date());
        
        let reasons = [];
        
        if (isFuture) {
            reasons.push('This time slot is in the future');
        } else if (isPast) {
            reasons.push('The agent may not have been running during this time period');
            reasons.push('Data may have been cleared or not recorded');
            if (!isToday) {
                reasons.push('Historical data might not be available for dates this far back');
            }
        } else {
            reasons.push('The agent may not be connected yet');
            reasons.push('No data has been received during this time slot');
        }
        
        const timeRange = `${moment(startTime).format('MMM D, HH:mm')} - ${moment(endTime).format('HH:mm')}`;
        
        const reasonsHtml = reasons.map(r => `<li>${r}</li>`).join('');
        
        const message = `
            <div class="alert alert-warning" role="alert">
                <h5><i class="fas fa-exclamation-triangle"></i> No Data Available</h5>
                <p class="mb-2"><strong>Time Range:</strong> ${timeRange}</p>
                <p class="mb-1"><strong>Possible reasons:</strong></p>
                <ul class="mb-0">
                    ${reasonsHtml}
                </ul>
            </div>
        `;
        
        // Clear charts and show message
        cpuChart.data.labels = [];
        cpuChart.data.datasets[0].data = [];
        cpuChart.update();
        
        ramChart.data.labels = [];
        ramChart.data.datasets[0].data = [];
        ramChart.update();
        
        // Show message in process table
        $('#processTableBody').html(`
            <tr>
                <td colspan="5" class="text-center">
                    ${message}
                </td>
            </tr>
        `);
        $('#processCount').text('0');
    }
    
    function sampleData(sessions, slotConfig) {
        if (sessions.length <= slotConfig.maxPoints) {
            return sessions;
        }
        
        // Sample data evenly to fit maxPoints
        const step = sessions.length / slotConfig.maxPoints;
        const sampled = [];
        
        for (let i = 0; i < slotConfig.maxPoints; i++) {
            const index = Math.floor(i * step);
            if (index < sessions.length) {
                sampled.push(sessions[index]);
            }
        }
        
        return sampled;
    }
    
    function updateChartsWithData(sessions) {
        if (!sessions || sessions.length === 0) {
            return;
        }
        
        // Get total RAM from first session if not already set
        if (totalRamBytes === 0 && sessions.length > 0 && sessions[0].totalRam) {
            totalRamBytes = sessions[0].totalRam;
            // Update RAM chart options with secondary axis
            ramChart.options = getChartOptions('RAM Usage (%)', 0, 100, true);
        }
        
        // Extract data
        const timestamps = sessions.map(s => s.timestamp);
        const cpuData = sessions.map(s => parseFloat(s.cpuUsage.toFixed(2)));
        const ramData = sessions.map(s => parseFloat(((s.ramUsage / s.totalRam) * 100).toFixed(2)));
        
        // Update CPU chart with animation disabled for better performance
        cpuChart.data.labels = timestamps;
        cpuChart.data.datasets[0].data = cpuData;
        cpuChart.update('none'); // 'none' for no animation
        
        // Update RAM chart
        ramChart.data.labels = timestamps;
        ramChart.data.datasets[0].data = ramData;
        ramChart.update('none');
    }
    
    // ============================================================================ //
    //                              REAL-TIME UPDATES                               //
    // ============================================================================ //
    
    function startRealTimeUpdates() {
        if (updateTimer) {
            clearInterval(updateTimer);
        }
        
        if (isRealTimeMode) {
            updateTimer = setInterval(function() {
                loadLatestSession();
            }, CONFIG.updateInterval);
        }
    }
    
    // Add flag to prevent concurrent AJAX requests
    let isLoadingSession = false;
    
    function stopRealTimeUpdates() {
        if (updateTimer) {
            clearInterval(updateTimer);
            updateTimer = null;
        }
    }
    
    function loadLatestSession() {
        // Prevent concurrent requests
        if (isLoadingSession) {
            return;
        }
        
        isLoadingSession = true;
        
        $.ajax({
            url: CONFIG.apiEndpoints.sessions,
            method: 'GET',
            data: {
                mac: macAddress,
                latest: true
            },
            dataType: 'json',
            timeout: 5000, // Add 5 second timeout
            success: function(session) {
                if (session) {
                    appendSessionToCharts(session);
                    
                    // Update processes if session changed
                    if (session.id !== latestSessionId) {
                        latestSessionId = session.id;
                        loadProcesses(session.id);
                    }
                }
            },
            error: function(xhr, status, error) {
                console.error('Failed to load latest session:', status, error);
                // Don't show error to user for polling failures
            },
            complete: function() {
                isLoadingSession = false;
            }
        });
    }
    
    function appendSessionToCharts(session) {
        // Only append in real-time mode
        if (!isRealTimeMode) {
            return;
        }
        
        const timestamp = session.timestamp;
        const cpuUsage = parseFloat(session.cpuUsage.toFixed(2));
        const ramUsage = parseFloat(((session.ramUsage / session.totalRam) * 100).toFixed(2));
        
        // Check if this timestamp already exists (prevent duplicates)
        const lastTimestamp = cpuChart.data.labels[cpuChart.data.labels.length - 1];
        if (lastTimestamp && timestamp <= lastTimestamp) {
            return; // Don't add duplicate data
        }
        
        // Check if we need to move to next time slot
        const currentSlot = getCurrentTimeSlot();
        if (timestamp >= currentSlot.endTime) {
            // Time slot changed, reload all data
            generateTimeSlots();
            loadChartData();
            return;
        }
        
        // Add new data point
        cpuChart.data.labels.push(timestamp);
        cpuChart.data.datasets[0].data.push(cpuUsage);
        
        ramChart.data.labels.push(timestamp);
        ramChart.data.datasets[0].data.push(ramUsage);
        
        // Remove old data points if exceeding max (scroll effect from right to left)
        const slotConfig = CONFIG.timeSlotSizes[selectedSlotSize];
        if (cpuChart.data.labels.length > slotConfig.maxPoints) {
            cpuChart.data.labels.shift();
            cpuChart.data.datasets[0].data.shift();
            
            ramChart.data.labels.shift();
            ramChart.data.datasets[0].data.shift();
        }
        
        // Update charts with animation disabled for performance
        cpuChart.update('none');
        ramChart.update('none');
    }
    
    // Add flag to prevent concurrent process loading
    let isLoadingProcesses = false;
    
    function loadProcesses(sessionId) {
        // Prevent concurrent requests
        if (isLoadingProcesses) {
            return;
        }
        
        isLoadingProcesses = true;
        
        $.ajax({
            url: CONFIG.apiEndpoints.processes,
            method: 'GET',
            data: {
                sessionId: sessionId
            },
            dataType: 'json',
            timeout: 5000, // Add 5 second timeout
            success: function(processes) {
                renderProcessTable(processes);
            },
            error: function(xhr, status, error) {
                console.error('Failed to load processes:', status, error);
                $('#processTableBody').html('<tr><td colspan="5" class="text-danger">Failed to load processes</td></tr>');
            },
            complete: function() {
                isLoadingProcesses = false;
            }
        });
    }
    
    function renderProcessTable(processes) {
        // Store all processes
        allProcesses = processes || [];
        
        // Check if we should render charts instead
        if (processViewMode === 'charts') {
            renderProcessCharts(allProcesses);
            return;
        }
        
        // Apply filter, search, and sort
        let filteredProcesses = filterProcesses(allProcesses);
        filteredProcesses = searchProcesses(filteredProcesses);
        filteredProcesses = sortProcesses(filteredProcesses);
        
        const $tbody = $('#processTableBody');
        
        if (filteredProcesses.length === 0) {
            $tbody.html('<tr><td colspan="6" class="text-center text-muted">No processes match criteria</td></tr>');
            $('#processCount').text(allProcesses.length > 0 ? `0/${allProcesses.length}` : '0');
            return;
        }
        
        // Limit display for performance (only show top N processes)
        const displayLimit = CONFIG.maxProcessDisplay;
        const limitedProcesses = filteredProcesses.slice(0, displayLimit);
        const hasMore = filteredProcesses.length > displayLimit;
        
        // Use DocumentFragment for better performance
        const fragment = document.createDocumentFragment();
        const tbody = document.createElement('tbody');
        
        limitedProcesses.forEach(function(proc) {
            const ramFormatted = formatRAM(proc.ramUsage, currentRamUnit);
            const ramPercent = totalRamBytes > 0 ? ((proc.ramUsage / totalRamBytes) * 100).toFixed(2) : '0.00';
            const cpuClass = proc.cpuUsage > 50 ? 'text-danger fw-bold' : '';
            const ramClass = parseFloat(ramPercent) > 10 ? 'text-warning fw-bold' : '';
            
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${proc.pid}</td>
                <td>${escapeHtml(proc.name)}</td>
                <td class="${cpuClass}">${proc.cpuUsage.toFixed(2)}%</td>
                <td class="${ramClass}">${ramPercent}%</td>
                <td class="${ramClass}">${ramFormatted.formatted}</td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-danger kill-process-btn" 
                            data-pid="${proc.pid}" 
                            data-name="${escapeHtml(proc.name)}"
                            title="Kill process">
                        <i class="fas fa-times-circle"></i>
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        });
        
        // Add "showing X of Y" message if truncated
        if (hasMore) {
            const tr = document.createElement('tr');
            tr.className = 'table-info';
            tr.innerHTML = `
                <td colspan="6" class="text-center">
                    <small><i class="fas fa-info-circle"></i> Showing top ${displayLimit} of ${filteredProcesses.length} processes. Use filters to narrow down results.</small>
                </td>
            `;
            tbody.appendChild(tr);
        }
        
        fragment.appendChild(tbody);
        
        // Replace entire tbody at once (faster than multiple appends)
        $tbody.html(tbody.innerHTML);
        
        // Update count display
        const countText = hasMore 
            ? `${displayLimit}/${filteredProcesses.length} (${allProcesses.length} total)`
            : (allProcesses.length > 0 ? `${filteredProcesses.length}/${allProcesses.length}` : '0');
        $('#processCount').text(countText);
    }
    
    // ============================================================================ //
    //                      PROCESS TABLE CONTROLS                                  //
    // ============================================================================ //
    
    function setupProcessTableControls() {
        // View mode toggle switch
        $('#processViewModeToggle').on('change', function() {
            const isChartsMode = $(this).is(':checked');
            processViewMode = isChartsMode ? 'charts' : 'table';
            
            if (isChartsMode) {
                // Charts mode
                $('#viewModeLabel').text('Charts View');
                $('#viewModeIcon').removeClass('fa-table').addClass('fa-chart-pie');
                $('#processTableView').addClass('d-none');
                $('#processChartsView').removeClass('d-none');
                $('#topNContainer').removeClass('d-none');
                $('#tableControls').addClass('d-none');
                
                // Render charts
                renderProcessCharts(allProcesses);
            } else {
                // Table mode
                $('#viewModeLabel').text('Table View');
                $('#viewModeIcon').removeClass('fa-chart-pie').addClass('fa-table');
                $('#processTableView').removeClass('d-none');
                $('#processChartsView').addClass('d-none');
                $('#topNContainer').addClass('d-none');
                $('#tableControls').removeClass('d-none');
                $('#topNWarning').addClass('d-none');
                
                // Re-render table
                renderProcessTable(allProcesses);
            }
        });
        
        // Top N selector
        $('#topNProcesses').on('change', function() {
            topNProcesses = $(this).val();
            renderProcessCharts(allProcesses);
        });
        
        // CPU chart type selector
        $('#processCpuChartType').on('change', function() {
            const newType = $(this).val();
            processCpuChart.config.type = newType;
            processCpuChart.update();
        });
        
        // RAM chart type selector
        $('#processRamChartType').on('change', function() {
            const newType = $(this).val();
            processRamChart.config.type = newType;
            processRamChart.update();
        });
        
        // Search input with debounce (increased to 500ms for better performance)
        let searchTimeout;
        $('#processSearch').on('input', function() {
            clearTimeout(searchTimeout);
            currentSearch = $(this).val().toLowerCase();
            searchTimeout = setTimeout(function() {
                renderProcessTable(allProcesses);
            }, 500); // Increased from 300ms to 500ms
        });
        
        // Sort dropdown
        $('#processSort').on('change', function() {
            currentSort = $(this).val();
            renderProcessTable(allProcesses);
        });
        
        // Filter dropdown
        $('#processFilter').on('change', function() {
            currentFilter = $(this).val();
            renderProcessTable(allProcesses);
        });
        
        // RAM Unit selector
        $('#ramUnit').on('change', function() {
            currentRamUnit = $(this).val();
            updateRAMHeaders();
            renderProcessTable(allProcesses);
        });
        
        // Table header click sort
        $('#processTable thead th[data-sort]').on('click', function() {
            const field = $(this).data('sort');
            const $icon = $(this).find('i');
            
            // Toggle sort direction
            if (currentSort.startsWith(field)) {
                currentSort = currentSort.endsWith('-asc') ? field + '-desc' : field + '-asc';
            } else {
                currentSort = field + '-desc';
            }
            
            // Update icons
            $('#processTable thead th i').removeClass('fa-sort-up fa-sort-down').addClass('fa-sort text-muted');
            $icon.removeClass('fa-sort text-muted').addClass(currentSort.endsWith('-asc') ? 'fa-sort-up' : 'fa-sort-down');
            
            // Update dropdown
            $('#processSort').val(currentSort);
            
            renderProcessTable(allProcesses);
        });
    }
    
    function filterProcesses(processes) {
        if (currentFilter === 'all') return processes;
        
        return processes.filter(function(proc) {
            switch(currentFilter) {
                case 'high-cpu':
                    return proc.cpuUsage > 50;
                case 'high-ram':
                    return (proc.ramUsage / (1024 * 1024)) > 500;
                case 'system':
                    return proc.name.toLowerCase().includes('system') || 
                           proc.name.toLowerCase().includes('kernel') ||
                           proc.name.toLowerCase().includes('windows') ||
                           proc.name.toLowerCase().includes('svchost');
                case 'user':
                    return !(proc.name.toLowerCase().includes('system') || 
                            proc.name.toLowerCase().includes('kernel') ||
                            proc.name.toLowerCase().includes('windows') ||
                            proc.name.toLowerCase().includes('svchost'));
                default:
                    return true;
            }
        });
    }
    
    function searchProcesses(processes) {
        if (!currentSearch) return processes;
        
        return processes.filter(function(proc) {
            return proc.name.toLowerCase().includes(currentSearch) ||
                   proc.pid.toString().includes(currentSearch);
        });
    }
    
    function sortProcesses(processes) {
        const [field, direction] = currentSort.split('-');
        const multiplier = direction === 'asc' ? 1 : -1;
        
        return processes.sort(function(a, b) {
            let aVal, bVal;
            
            switch(field) {
                case 'cpu':
                    aVal = a.cpuUsage;
                    bVal = b.cpuUsage;
                    break;
                case 'ram':
                    aVal = a.ramUsage;
                    bVal = b.ramUsage;
                    break;
                case 'name':
                    aVal = a.name.toLowerCase();
                    bVal = b.name.toLowerCase();
                    return multiplier * aVal.localeCompare(bVal);
                case 'pid':
                    aVal = a.pid;
                    bVal = b.pid;
                    break;
                default:
                    return 0;
            }
            
            return multiplier * (aVal - bVal);
        });
    }
    
    // ============================================================================ //
    //                          TIME SLOT CONTROLS                                  //
    // ============================================================================ //
    
    function setupTimeSlotControls() {
        // Date picker
        $('#trackingDate').on('change', function() {
            const dateStr = $(this).val();
            selectedDate = new Date(dateStr + ' 00:00:00');
            generateTimeSlots();
            
            // Check if selected date is today
            const today = new Date();
            const isToday = isSameDay(selectedDate, today);
            
            if (isToday && isRealTimeMode) {
                // Stay in real-time mode
                loadChartData();
            } else {
                // Switch to first slot
                isRealTimeMode = false;
                currentSlotIndex = 0;
                loadChartData();
            }
        });
        
        // Slot size selector
        $('#slotSize').on('change', function() {
            selectedSlotSize = $(this).val();
            generateTimeSlots();
            
            if (!isRealTimeMode && currentSlotIndex >= allTimeSlots.length) {
                currentSlotIndex = allTimeSlots.length - 1;
            }
            
            loadChartData();
        });
        
        // Real-time button
        $('#btnRealTime').on('click', function() {
            const today = new Date();
            if (!isSameDay(selectedDate, today)) {
                selectedDate = today;
                $('#trackingDate').val(formatDateForInput(today));
                generateTimeSlots();
            }
            
            isRealTimeMode = true;
            currentSlotIndex = null;
            loadChartData();
            startRealTimeUpdates();
            updateSlotNavigationUI();
        });
        
        // Previous slot button
        $('#btnPrevSlot').on('click', function() {
            if (isRealTimeMode) {
                // Switch to previous slot from current
                const currentSlot = getCurrentTimeSlot();
                const currentSlotIndexInArray = allTimeSlots.findIndex(s => s.startTime === currentSlot.startTime);
                
                if (currentSlotIndexInArray > 0) {
                    isRealTimeMode = false;
                    currentSlotIndex = currentSlotIndexInArray - 1;
                    stopRealTimeUpdates();
                    loadChartData();
                }
            } else if (currentSlotIndex > 0) {
                currentSlotIndex--;
                loadChartData();
            }
            
            updateSlotNavigationUI();
        });
        
        // Next slot button
        $('#btnNextSlot').on('click', function() {
            if (!isRealTimeMode && currentSlotIndex < allTimeSlots.length - 1) {
                currentSlotIndex++;
                
                // Check if moved to current slot
                const selectedSlot = allTimeSlots[currentSlotIndex];
                if (selectedSlot.isCurrent) {
                    isRealTimeMode = true;
                    currentSlotIndex = null;
                    startRealTimeUpdates();
                }
                
                loadChartData();
                updateSlotNavigationUI();
            }
        });
        
        // Set default date to today
        $('#trackingDate').val(formatDateForInput(selectedDate));
    }
    
    function generateTimeSlots() {
        const slotConfig = CONFIG.timeSlotSizes[selectedSlotSize];
        const slotMinutes = slotConfig.minutes;
        
        // Get start of day
        const startOfDay = new Date(selectedDate);
        startOfDay.setHours(0, 0, 0, 0);
        
        const now = Date.now();
        const isToday = isSameDay(selectedDate, new Date());
        
        allTimeSlots = [];
        
        // Generate slots for the day
        for (let i = 0; i < slotConfig.slotsPerDay; i++) {
            const startTime = startOfDay.getTime() + (i * slotMinutes * 60 * 1000);
            const endTime = startTime + (slotMinutes * 60 * 1000);
            
            // Skip future slots (only show past and current)
            if (isToday && startTime > now) {
                break;
            }
            
            const startMoment = moment(startTime);
            const endMoment = moment(endTime);
            
            // Mark if this is the current slot
            const isCurrent = isToday && now >= startTime && now < endTime;
            
            allTimeSlots.push({
                index: i,
                startTime: startTime,
                endTime: endTime,
                label: `${startMoment.format('HH:mm')} - ${endMoment.format('HH:mm')}`,
                isCurrent: isCurrent
            });
        }
        
        updateSlotSelector();
        updateSlotNavigationUI();
    }
    
    function updateSlotSelector() {
        const $select = $('#slotSelector');
        $select.empty();
        
        allTimeSlots.forEach((slot, index) => {
            const label = slot.label + (slot.isCurrent ? ' 🔴 Current' : '');
            $select.append(`<option value="${index}">${label}</option>`);
        });
        
        if (currentSlotIndex !== null) {
            $select.val(currentSlotIndex);
        }
        
        $select.off('change').on('change', function() {
            const selectedIndex = parseInt($(this).val());
            const selectedSlot = allTimeSlots[selectedIndex];
            
            // If selected slot is current, switch to real-time mode
            if (selectedSlot.isCurrent) {
                isRealTimeMode = true;
                currentSlotIndex = null;
                loadChartData();
                startRealTimeUpdates();
            } else {
                // Historical mode
                currentSlotIndex = selectedIndex;
                isRealTimeMode = false;
                stopRealTimeUpdates();
                loadChartData();
            }
            
            updateSlotNavigationUI();
        });
    }
    
    function updateSlotNavigationUI() {
        // Update button states
        $('#btnRealTime').toggleClass('btn-success', isRealTimeMode).toggleClass('btn-outline-success', !isRealTimeMode);
        
        // For Previous button: disable if at first slot or in real-time mode with no previous slot
        const canGoPrev = isRealTimeMode ? true : currentSlotIndex > 0;
        $('#btnPrevSlot').prop('disabled', !canGoPrev);
        
        // For Next button: disable if at last slot (never show future)
        const canGoNext = isRealTimeMode ? false : currentSlotIndex < allTimeSlots.length - 1;
        $('#btnNextSlot').prop('disabled', !canGoNext);
        
        // Update slot selector
        if (isRealTimeMode) {
            // Select current slot in dropdown
            const currentSlotIndex = allTimeSlots.findIndex(s => s.isCurrent);
            if (currentSlotIndex >= 0) {
                $('#slotSelector').val(currentSlotIndex);
            }
        } else if (currentSlotIndex !== null) {
            $('#slotSelector').val(currentSlotIndex);
        }
        
        // Update info display
        const today = new Date();
        const isToday = isSameDay(selectedDate, today);
        const dateStr = isToday ? 'Today' : moment(selectedDate).format('MMM D, YYYY');
        
        if (isRealTimeMode) {
            const currentSlot = getCurrentTimeSlot();
            $('#viewInfo').html(`
                <span class="badge bg-success">
                    <i class="fas fa-circle"></i> Real-time
                </span>
                <span class="text-muted ms-2">${dateStr} - ${currentSlot.label}</span>
            `);
        } else {
            const slot = allTimeSlots[currentSlotIndex];
            $('#viewInfo').html(`
                <span class="badge bg-primary">
                    <i class="fas fa-history"></i> Historical
                </span>
                <span class="text-muted ms-2">${dateStr} - ${slot.label}</span>
            `);
        }
    }
    
    function getCurrentTimeSlot() {
        const now = Date.now();
        const slotConfig = CONFIG.timeSlotSizes[selectedSlotSize];
        const slotMinutes = slotConfig.minutes;
        
        // Get start of day
        const startOfDay = new Date();
        startOfDay.setHours(0, 0, 0, 0);
        const startOfDayTime = startOfDay.getTime();
        
        // Calculate current slot
        const minutesSinceStartOfDay = (now - startOfDayTime) / (60 * 1000);
        const slotIndex = Math.floor(minutesSinceStartOfDay / slotMinutes);
        
        const startTime = startOfDayTime + (slotIndex * slotMinutes * 60 * 1000);
        const endTime = startTime + (slotMinutes * 60 * 1000);
        
        return {
            index: slotIndex,
            startTime: startTime,
            endTime: endTime,
            label: `${moment(startTime).format('HH:mm')} - ${moment(endTime).format('HH:mm')}`
        };
    }
    
    function updateChartTitle(label, isRealTime) {
        const icon = isRealTime ? '<i class="fas fa-circle text-success"></i>' : '<i class="fas fa-clock text-primary"></i>';
        $('#chartTimeLabel').html(`${icon} ${label}`);
    }
    
    function isSameDay(date1, date2) {
        return date1.getFullYear() === date2.getFullYear() &&
               date1.getMonth() === date2.getMonth() &&
               date1.getDate() === date2.getDate();
    }
    
    // ============================================================================ //
    //                              CHART TYPE HANDLERS                             //
    // ============================================================================ //
    
    function setupChartTypeHandlers() {
        $('#cpuChartType').on('change', function() {
            changeChartType(cpuChart, $(this).val());
        });
        
        $('#ramChartType').on('change', function() {
            changeChartType(ramChart, $(this).val());
        });
    }
    
    function changeChartType(chart, type) {
        if (type === 'area') {
            chart.config.type = 'line';
            chart.data.datasets[0].fill = true;
            // Keep hidden points for area chart
            chart.data.datasets[0].pointRadius = 0;
            chart.data.datasets[0].pointHoverRadius = 6;
        } else if (type === 'line') {
            chart.config.type = 'line';
            chart.data.datasets[0].fill = false;
            // Keep hidden points for line chart
            chart.data.datasets[0].pointRadius = 0;
            chart.data.datasets[0].pointHoverRadius = 6;
        } else if (type === 'bar') {
            chart.config.type = 'bar';
            chart.data.datasets[0].fill = false;
            // Bar chart doesn't use points
            chart.data.datasets[0].pointRadius = 0;
            chart.data.datasets[0].pointHoverRadius = 0;
        }
        chart.update();
    }
    
    // ============================================================================ //
    //                              HELPER FUNCTIONS                                //
    // ============================================================================ //
    
    function getURLParameter(name) {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get(name);
    }
    
    function getOSIcon(os) {
        const osLower = (os || '').toLowerCase();
        if (osLower.includes('windows')) return 'fab fa-windows';
        if (osLower.includes('mac') || osLower.includes('darwin')) return 'fab fa-apple';
        if (osLower.includes('linux')) return 'fab fa-linux';
        return 'fas fa-desktop';
    }
    
    function formatFrequency(hz) {
        if (!hz) return 'N/A';
        const ghz = (hz / 1000000000).toFixed(2);
        return `${ghz} GHz`;
    }
    
    function formatMAC(mac) {
        if (!mac) return 'N/A';
        return mac.replace(/(.{2})(?=.)/g, '$1:').toUpperCase();
    }
    
    function formatDateForInput(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }
    
    function showLoadingOverlay() {
        if ($('#chartLoadingOverlay').length === 0) {
            $('body').append(`
                <div id="chartLoadingOverlay" style="
                    position: fixed;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    background: rgba(0,0,0,0.5);
                    z-index: 9999;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                ">
                    <div class="spinner-border text-light" style="width: 3rem; height: 3rem;" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                </div>
            `);
        }
    }
    
    function hideLoadingOverlay() {
        $('#chartLoadingOverlay').remove();
    }
    
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
    
    function showError(message) {
        $('#agentInfo').html(`
            <div class="alert alert-danger" role="alert">
                <i class="fas fa-exclamation-triangle"></i> ${escapeHtml(message)}
            </div>
        `);
    }
    
    // ============================================================================ //
    //                          REMOTE COMMAND HANDLERS                             //
    // ============================================================================ //
    
    /**
     * Setup remote command buttons (Kill Process, Send Message, Shutdown)
     */
    function setupRemoteCommandButtons() {
        // Handle Kill Process button clicks (using event delegation for dynamically added buttons)
        $(document).on('click', '.kill-process-btn', function(e) {
            e.stopPropagation(); // Prevent row click event
            
            const pid = parseInt($(this).data('pid'));
            const processName = $(this).data('name');
            
            if (!pid || !macAddress) {
                alert('Unable to kill process: missing information');
                return;
            }
            
            RemoteCommands.killProcessWithConfirm(macAddress, pid, processName);
        });
        
        // Send Message button click
        $('#sendMessageBtn').on('click', function() {
            if (!macAddress) {
                alert('Agent MAC address not found');
                return;
            }
            
            RemoteCommands.sendMessageWithPrompt(macAddress);
        });
        
        // Shutdown button click
        $('#shutdownBtn').on('click', function() {
            if (!macAddress) {
                alert('Agent MAC address not found');
                return;
            }
            
            // Prompt for delay
            const delayStr = prompt('Enter shutdown delay in seconds (default 60):', '60');
            if (delayStr === null) {
                return; // User cancelled
            }
            
            const delay = parseInt(delayStr) || 60;
            RemoteCommands.shutdownWithConfirm(macAddress, delay);
        });
    }
    
    // Cleanup on page unload to prevent memory leaks
    $(window).on('beforeunload', function() {
        // Stop real-time updates
        if (updateTimer) {
            clearInterval(updateTimer);
            updateTimer = null;
        }
        
        // Destroy charts
        if (cpuChart) {
            cpuChart.destroy();
            cpuChart = null;
        }
        if (ramChart) {
            ramChart.destroy();
            ramChart = null;
        }
        
        // Clear large data structures
        allProcesses = [];
        allTimeSlots = [];
    });
    
})();
