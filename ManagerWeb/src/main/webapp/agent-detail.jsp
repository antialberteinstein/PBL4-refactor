<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="util.Messages" %>
<!DOCTYPE html>
<html lang="<%= Messages.getLanguage() %>">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Messages.get("agent.details.title") %></title>
    
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    
    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    
    <!-- Chart.js -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/chartjs-plugin-zoom@2.0.1/dist/chartjs-plugin-zoom.min.js"></script>
    
    <!-- Custom CSS -->
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/toggle-switch.css">
</head>
<body>
    <!-- ============================================================================ -->
    <!--                                 HEADER                                       -->
    <!-- ============================================================================ -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
        <div class="container-fluid">
            <a class="navbar-brand" href="<%= request.getContextPath() %>/">
                <i class="fas fa-server"></i> <%= Messages.get("app.name") %>
            </a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarNav">
                <ul class="navbar-nav ms-auto">
                    <li class="nav-item">
                        <a class="nav-link" href="<%= request.getContextPath() %>/">
                            <i class="fas fa-arrow-left"></i> <%= Messages.get("agent.back.dashboard") %>
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="<%= request.getContextPath() %>/settings.jsp">
                            <i class="fas fa-cog"></i> <%= Messages.get("nav.settings") %>
                        </a>
                    </li>
                    <li class="nav-item">
                        <span class="nav-link">
                            <i class="fas fa-user"></i> <%= session.getAttribute("username") != null ? session.getAttribute("username") : Messages.get("nav.guest") %>
                        </span>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="<%= request.getContextPath() %>/logout">
                            <i class="fas fa-sign-out-alt"></i> <%= Messages.get("auth.logout") %>
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    </nav>

    <!-- ============================================================================ -->
    <!--                              MAIN CONTENT                                    -->
    <!-- ============================================================================ -->
    <div class="container-fluid mt-4">
        
        <!-- Agent Info Card -->
        <div class="row mb-4">
            <div class="col-12">
                <div class="card">
                    <div class="card-header bg-light d-flex justify-content-between align-items-center">
                        <h5 class="mb-0">
                            <i class="fas fa-info-circle"></i> Agent Information
                        </h5>
                        <div class="btn-group btn-group-sm" role="group">
                            <button type="button" class="btn btn-outline-primary" id="sendMessageBtn" 
                                    title="Send message to Agent">
                                <i class="fas fa-envelope"></i> Send Message
                            </button>
                            <button type="button" class="btn btn-outline-warning" id="shutdownBtn" 
                                    title="Shutdown Agent computer">
                                <i class="fas fa-power-off"></i> Shutdown
                            </button>
                        </div>
                    </div>
                    <div class="card-body">
                        <div id="agentInfo" class="agent-info">
                            <div class="spinner-border text-primary" role="status">
                                <span class="visually-hidden"><%= Messages.get("dashboard.loading.text") %></span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Time Slot Controls -->
        <div class="row mb-3">
            <div class="col-12">
                <div class="card">
                    <div class="card-body py-2">
                        <div class="row align-items-center g-2">
                            <!-- Date & Slot Size Selection -->
                            <div class="col-md-6">
                                <div class="d-flex align-items-center flex-wrap gap-2">
                                    <div class="d-flex align-items-center">
                                        <label class="me-2 mb-0 text-nowrap">
                                            <i class="fas fa-calendar"></i> <strong><%= Messages.get("agent.date") %>:</strong>
                                        </label>
                                        <input type="date" id="trackingDate" class="form-control form-control-sm" style="width: 150px;">
                                    </div>
                                    
                                    <div class="d-flex align-items-center">
                                        <label class="me-2 mb-0 text-nowrap">
                                            <i class="fas fa-clock"></i> <strong><%= Messages.get("agent.slot.size") %>:</strong>
                                        </label>
                                        <select id="slotSize" class="form-select form-select-sm" style="width: 120px;">
                                            <option value="30m">30 Minutes</option>
                                            <option value="1h" selected>1 Hour</option>
                                            <option value="2h">2 Hours</option>
                                            <option value="3h">3 Hours</option>
                                            <option value="6h">6 Hours</option>
                                        </select>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- View Info -->
                            <div class="col-md-6 text-end">
                                <div id="viewInfo">
                                    <span class="badge bg-success">
                                        <i class="fas fa-circle"></i> <%= Messages.get("agent.realtime") %>
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Time Slot Navigation -->
        <div class="row mb-3">
            <div class="col-12">
                <div class="card">
                    <div class="card-body py-2">
                        <div class="d-flex align-items-center justify-content-between flex-wrap gap-2">
                            <!-- Navigation Buttons -->
                            <div class="btn-group" role="group">
                                <button type="button" class="btn btn-sm btn-success" id="btnRealTime">
                                    <i class="fas fa-circle"></i> <%= Messages.get("agent.realtime") %>
                                </button>
                                <button type="button" class="btn btn-sm btn-outline-secondary" id="btnPrevSlot">
                                    <i class="fas fa-chevron-left"></i> <%= Messages.get("agent.previous") %>
                                </button>
                                <button type="button" class="btn btn-sm btn-outline-secondary" id="btnNextSlot">
                                    <%= Messages.get("agent.next") %> <i class="fas fa-chevron-right"></i>
                                </button>
                            </div>
                            
                            <!-- Slot Selector -->
                            <div class="d-flex align-items-center flex-grow-1 ms-3">
                                <label class="me-2 mb-0 text-nowrap">
                                    <strong><%= Messages.get("agent.time.slot") %>:</strong>
                                </label>
                                <select id="slotSelector" class="form-select form-select-sm flex-grow-1" style="max-width: 300px;">
                                    <!-- Options will be populated by JavaScript -->
                                </select>
                            </div>
                            
                            <!-- Chart Time Label -->
                            <div id="chartTimeLabel" class="text-muted">
                                <i class="fas fa-circle text-success"></i> Loading...
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Charts Section -->
        <div class="row mb-4">
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header bg-light d-flex justify-content-between align-items-center">
                        <h5 class="mb-0">
                            <i class="fas fa-microchip"></i> CPU Usage
                        </h5>
                        <select id="cpuChartType" class="form-select form-select-sm w-auto">
                            <option value="line">Line Chart</option>
                            <option value="bar">Histogram</option>
                            <option value="area">Area Chart</option>
                        </select>
                    </div>
                    <div class="card-body">
                        <canvas id="cpuChart"></canvas>
                    </div>
                </div>
            </div>
            
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header bg-light d-flex justify-content-between align-items-center">
                        <h5 class="mb-0">
                            <i class="fas fa-memory"></i> RAM Usage
                        </h5>
                        <select id="ramChartType" class="form-select form-select-sm w-auto">
                            <option value="line">Line Chart</option>
                            <option value="bar">Histogram</option>
                            <option value="area">Area Chart</option>
                        </select>
                    </div>
                    <div class="card-body">
                        <canvas id="ramChart"></canvas>
                    </div>
                </div>
            </div>
        </div>

        <!-- Process List -->
        <div class="row">
            <div class="col-12">
                <div class="card">
                    <div class="card-header bg-light">
                        <div class="row align-items-center">
                            <div class="col-md-3">
                                <h5 class="mb-0">
                                    <i class="fas fa-tasks"></i> <%= Messages.get("agent.running.processes") %>
                                    <span id="processCount" class="badge bg-primary ms-2">0</span>
                                </h5>
                            </div>
                            <div class="col-md-9">
                                <div class="row g-2 align-items-center">
                                    <!-- View Mode Toggle -->
                                    <div class="col-auto">
                                        <div class="view-mode-toggle">
                                            <input type="checkbox" id="processViewModeToggle" class="toggle-checkbox">
                                            <label for="processViewModeToggle" class="toggle-label">
                                                <span class="toggle-inner">
                                                    <span class="toggle-switch"></span>
                                                </span>
                                                <span class="toggle-text">
                                                    <i class="fas fa-table" id="viewModeIcon"></i>
                                                    <span id="viewModeLabel"><%= Messages.get("agent.table.view") %></span>
                                                </span>
                                            </label>
                                        </div>
                                    </div>
                                    
                                    <!-- Top N (only visible in chart mode) -->
                                    <div class="col-auto d-none" id="topNContainer">
                                        <label class="form-label mb-0 me-2 small">Show:</label>
                                        <select class="form-select form-select-sm d-inline-block" id="topNProcesses" style="width: auto;">
                                            <option value="5">Top 5</option>
                                            <option value="10" selected>Top 10</option>
                                            <option value="15">Top 15</option>
                                            <option value="20">Top 20</option>
                                        </select>
                                    </div>
                                    
                                    <!-- Table Controls (only visible in table mode) -->
                                    <div id="tableControls" class="col">
                                        <div class="row g-2">
                                            <!-- Search -->
                                            <div class="col-md-3">
                                                <input type="text" class="form-control form-control-sm" id="processSearch" 
                                                       placeholder="<%= Messages.get("agent.search") %>">
                                            </div>
                                            <!-- Sort -->
                                            <div class="col-md-3">
                                                <select class="form-select form-select-sm" id="processSort">
                                                    <option value="cpu-desc"><%= Messages.get("agent.cpu.high.low") %></option>
                                                    <option value="cpu-asc"><%= Messages.get("agent.cpu.low.high") %></option>
                                                    <option value="ram-desc"><%= Messages.get("agent.ram.high.low") %></option>
                                                    <option value="ram-asc"><%= Messages.get("agent.ram.low.high") %></option>
                                                    <option value="name-asc"><%= Messages.get("agent.name.asc") %></option>
                                                    <option value="name-desc"><%= Messages.get("agent.name.desc") %></option>
                                                </select>
                                            </div>
                                            <!-- Filter -->
                                            <div class="col-md-3">
                                                <select class="form-select form-select-sm" id="processFilter">
                                                    <option value="all">All</option>
                                                    <option value="high-cpu">High CPU</option>
                                                    <option value="high-ram">High RAM</option>
                                                </select>
                                            </div>
                                            <!-- RAM Unit -->
                                            <div class="col-md-3">
                                                <select class="form-select form-select-sm" id="ramUnit">
                                                    <option value="auto" selected>RAM: Auto</option>
                                                    <option value="bytes">Bytes</option>
                                                    <option value="kb">KB (1000)</option>
                                                    <option value="kib">KiB (1024)</option>
                                                    <option value="mb">MB (1000²)</option>
                                                    <option value="mib">MiB (1024²)</option>
                                                    <option value="gb">GB (1000³)</option>
                                                    <option value="gib">GiB (1024³)</option>
                                                </select>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="card-body">
                        <!-- Table View -->
                        <div id="processTableView">
                            <div class="table-responsive">
                            <table class="table table-striped table-hover" id="processTable">
                                <thead>
                                    <tr>
                                        <th style="cursor: pointer;" data-sort="pid">
                                            PID <i class="fas fa-sort text-muted"></i>
                                        </th>
                                        <th style="cursor: pointer;" data-sort="name">
                                            Name <i class="fas fa-sort text-muted"></i>
                                        </th>
                                        <th style="cursor: pointer;" data-sort="cpu">
                                            CPU Usage (%) <i class="fas fa-sort text-muted"></i>
                                        </th>
                                        <th style="cursor: pointer;" data-sort="ram">
                                            RAM Usage (%) <i class="fas fa-sort text-muted"></i>
                                        </th>
                                        <th style="cursor: pointer;" data-sort="ram">
                                            <span id="ramHeader">RAM</span> <i class="fas fa-sort text-muted"></i>
                                        </th>
                                        <th style="width: 80px; text-align: center;">Action</th>
                                    </tr>
                                </thead>
                                <tbody id="processTableBody">
                                    <tr>
                                        <td colspan="5" class="text-center">
                                            <div class="spinner-border text-primary" role="status">
                                                <span class="visually-hidden">Loading...</span>
                                            </div>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                            </div>
                        </div>
                        
                        <!-- Charts View -->
                        <div id="processChartsView" class="d-none">
                            <!-- Warning for large N -->
                            <div id="topNWarning" class="alert alert-warning d-none">
                                <i class="fas fa-exclamation-triangle"></i> 
                                <strong>Note:</strong> Displaying more than 10 processes may make the chart harder to read.
                            </div>
                            
                            <!-- Breadcrumb Navigation -->
                            <nav aria-label="breadcrumb" id="chartBreadcrumb" class="d-none mb-3">
                                <ol class="breadcrumb mb-0">
                                    <li class="breadcrumb-item">
                                        <a href="#" id="breadcrumbBack" class="text-decoration-none">
                                            <i class="fas fa-arrow-left"></i> Back to Main View
                                        </a>
                                    </li>
                                    <li class="breadcrumb-item active" aria-current="page">
                                        <span id="breadcrumbCurrent"></span>
                                    </li>
                                </ol>
                            </nav>
                            
                            <!-- Main Charts Row -->
                            <div class="row" id="mainChartsRow">
                                <div class="col-md-6">
                                    <div class="card bg-light">
                                        <div class="card-header d-flex justify-content-between align-items-center">
                                            <h6 class="mb-0">
                                                <i class="fas fa-microchip"></i> CPU Usage by Process
                                            </h6>
                                            <select id="processCpuChartType" class="form-select form-select-sm w-auto">
                                                <option value="doughnut" selected>Doughnut chart</option>
                                                <option value="pie">Pie chart</option>
                                            </select>
                                        </div>
                                        <div class="card-body" style="height: 400px;">
                                            <canvas id="processCpuChart"></canvas>
                                        </div>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <div class="card bg-light">
                                        <div class="card-header d-flex justify-content-between align-items-center">
                                            <h6 class="mb-0">
                                                <i class="fas fa-memory"></i> RAM Usage by Process
                                            </h6>
                                            <select id="processRamChartType" class="form-select form-select-sm w-auto">
                                                <option value="doughnut" selected>Doughnut Chart</option>
                                                <option value="pie">Pie Chart</option>
                                            </select>
                                        </div>
                                        <div class="card-body" style="height: 400px;">
                                            <canvas id="processRamChart"></canvas>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- Others Detail View (hidden by default) -->
                            <div id="othersDetailView" class="d-none">
                                <!-- Info Alert -->
                                <div class="alert alert-info mb-3">
                                    <div class="row align-items-center">
                                        <div class="col-md-6">
                                            <i class="fas fa-info-circle"></i>
                                            <strong>Viewing:</strong> <span id="othersCount">0</span> processes from "Others"
                                        </div>
                                        <div class="col-md-6 text-end">
                                            <strong>Total Usage:</strong> <span id="othersTotalUsage">0</span>
                                        </div>
                                    </div>
                                </div>
                                
                                <div class="row">
                                    <!-- Chart Column -->
                                    <div class="col-md-6">
                                        <div class="card bg-light">
                                            <div class="card-header d-flex justify-content-between align-items-center">
                                                <h6 class="mb-0">
                                                    <i class="fas fa-chart-pie"></i> <span id="othersChartTitle">Others Detail</span>
                                                </h6>
                                                <div class="d-flex gap-2">
                                                    <select class="form-select form-select-sm" id="othersTopN" style="width: auto;">
                                                        <option value="5">Top 5</option>
                                                        <option value="10" selected>Top 10</option>
                                                        <option value="15">Top 15</option>
                                                        <option value="20">Top 20</option>
                                                        <option value="all">All</option>
                                                    </select>
                                                    <select class="form-select form-select-sm" id="othersChartType" style="width: auto;">
                                                        <option value="doughnut" selected>Doughnut Chart</option>
                                                        <option value="pie">Pie Chart</option>
                                                        <option value="bar">Bar Chart</option>
                                                    </select>
                                                </div>
                                            </div>
                                            <div class="card-body" style="height: 400px;">
                                                <canvas id="othersDetailChart"></canvas>
                                            </div>
                                        </div>
                                    </div>
                                    
                                    <!-- Table Column -->
                                    <div class="col-md-6">
                                        <div class="card">
                                            <div class="card-header">
                                                <h6 class="mb-0">
                                                    <i class="fas fa-list"></i> Process List
                                                </h6>
                                            </div>
                                            <div class="card-body p-0" style="max-height: 400px; overflow-y: auto;">
                                                <table class="table table-sm table-striped table-hover mb-0">
                                                    <thead class="table-light sticky-top">
                                                        <tr>
                                                            <th>#</th>
                                                            <th>PID</th>
                                                            <th>Name</th>
                                                            <th>CPU %</th>
                                                            <th>RAM MB</th>
                                                        </tr>
                                                    </thead>
                                                    <tbody id="othersTableBody">
                                                        <tr>
                                                            <td colspan="5" class="text-center text-muted">No data</td>
                                                        </tr>
                                                    </tbody>
                                                </table>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </div>

    <!-- ============================================================================ -->
    <!--                                SCRIPTS                                       -->
    <!-- ============================================================================ -->
    
    <!-- Set context path for JavaScript -->
    <script>
        window.contextPath = '<%= request.getContextPath() %>';
    </script>
    
    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    
    <!-- jQuery -->
    <script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
    
    <!-- Moment.js for time formatting -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.29.4/moment.min.js"></script>
    
    <!-- Remote Commands Module -->
    <script src="<%= request.getContextPath() %>/js/remote-commands.js"></script>
    
    <!-- Custom JS -->
    <script src="<%= request.getContextPath() %>/js/agent-detail.js"></script>
</body>
</html>
