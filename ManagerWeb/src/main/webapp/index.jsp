<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="util.Messages" %>
<!DOCTYPE html>
<html lang="<%= Messages.getLanguage() %>">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Messages.get("dashboard.title") %></title>
    
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    
    <!-- Font Awesome for icons -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    
    <!-- Custom CSS -->
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
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
                        <a class="nav-link active" href="<%= request.getContextPath() %>/">
                            <i class="fas fa-home"></i> <%= Messages.get("nav.dashboard") %>
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
        <!-- Control Panel -->
        <div class="row mb-4">
            <div class="col-12">
                <div class="card">
                    <div class="card-body">
                        <h5 class="card-title">
                            <i class="fas fa-radar"></i> <%= Messages.get("dashboard.network.scan") %>
                        </h5>
                        <button id="scanBtn" class="btn btn-success">
                            <i class="fas fa-sync-alt"></i> <%= Messages.get("dashboard.trigger.scan") %>
                        </button>
                        <span id="scanStatus" class="ms-3"></span>
                    </div>
                </div>
            </div>
        </div>

        <!-- Agent Grid -->
        <div class="row">
            <div class="col-12">
                <div class="card">
                    <div class="card-header bg-light">
                        <h5 class="mb-0">
                            <i class="fas fa-desktop"></i> <%= Messages.get("dashboard.agents.title") %>
                            <span id="agentCount" class="badge bg-primary ms-2">0</span>
                        </h5>
                    </div>
                    <div class="card-body">
                        <div id="agentGrid" class="agent-grid">
                            <!-- Agent cards will be dynamically loaded here -->
                            <div class="text-center py-5">
                                <div class="spinner-border text-primary" role="status">
                                    <span class="visually-hidden"><%= Messages.get("dashboard.loading.text") %></span>
                                </div>
                                <p class="mt-3 text-muted"><%= Messages.get("dashboard.loading") %></p>
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
    
    <!-- jQuery (for easier AJAX) -->
    <script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
    
    <!-- Remote Commands Module -->
    <script src="<%= request.getContextPath() %>/js/remote-commands.js"></script>
    
    <!-- Custom JS -->
    <script src="<%= request.getContextPath() %>/js/dashboard.js"></script>
</body>
</html>
