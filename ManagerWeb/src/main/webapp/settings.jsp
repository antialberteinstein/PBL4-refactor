<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="util.Messages" %>
<!DOCTYPE html>
<html lang="<%= Messages.getLanguage() %>">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Messages.get("settings.title") %> - <%= Messages.get("app.name") %></title>
    
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
                        <a class="nav-link" href="<%= request.getContextPath() %>/">
                            <i class="fas fa-home"></i> <%= Messages.get("nav.dashboard") %>
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link active" href="<%= request.getContextPath() %>/settings.jsp">
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
    <div class="container mt-5">
        <div class="row justify-content-center">
            <div class="col-md-8 col-lg-6">
                <div class="card shadow">
                    <div class="card-header bg-primary text-white">
                        <h4 class="mb-0">
                            <i class="fas fa-cog"></i> <%= Messages.get("settings.title") %>
                        </h4>
                    </div>
                    <div class="card-body">
                        <% if (request.getParameter("saved") != null) { %>
                        <div class="alert alert-success alert-dismissible fade show" role="alert">
                            <i class="fas fa-check-circle"></i> <%= Messages.get("settings.language.changed") %>
                            <br>
                            <small><%= Messages.get("settings.restart.required") %></small>
                            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                        </div>
                        <% } %>
                        
                        <% if (request.getParameter("error") != null) { %>
                        <div class="alert alert-danger alert-dismissible fade show" role="alert">
                            <i class="fas fa-exclamation-circle"></i> Invalid input. Please check your values.
                            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                        </div>
                        <% } %>
                        
                        <form method="post" action="<%= request.getContextPath() %>/settings">
                            <!-- General Settings Section -->
                            <h5 class="mb-3">
                                <i class="fas fa-sliders-h"></i> General Settings
                            </h5>
                            
                            <div class="mb-4">
                                <label for="language" class="form-label">
                                    <i class="fas fa-language"></i> <%= Messages.get("settings.language") %>
                                </label>
                                <select class="form-select" id="language" name="language" required>
                                    <option value="en" <%= "en".equals(Messages.getLanguage()) ? "selected" : "" %>>English</option>
                                    <option value="vi" <%= "vi".equals(Messages.getLanguage()) ? "selected" : "" %>>Tiếng Việt</option>
                                </select>
                                <div class="form-text">
                                    <%= Messages.get("settings.select.language") %>
                                </div>
                            </div>
                            
                            <!-- Resource Monitoring Thresholds Section -->
                            <h5 class="mb-3 mt-4">
                                <i class="fas fa-chart-line"></i> Resource Monitoring Thresholds
                            </h5>
                            
                            <%
                                config.AppConfig appConfig = (config.AppConfig) application.getAttribute("appConfig");
                                double cpuThreshold = appConfig != null ? appConfig.getCpuThresholdPercent() : 90.0;
                                double ramThreshold = appConfig != null ? appConfig.getRamThresholdPercent() : 90.0;
                            %>
                            
                            <div class="mb-3">
                                <label for="cpuThreshold" class="form-label">
                                    <i class="fas fa-microchip"></i> CPU Threshold (%)
                                </label>
                                <input type="number" class="form-control" id="cpuThreshold" name="cpuThreshold" 
                                       value="<%= cpuThreshold %>" min="0" max="100" step="1" required>
                                <div class="form-text">
                                    Agents will receive warnings when CPU usage exceeds this threshold.
                                </div>
                            </div>
                            
                            <div class="mb-4">
                                <label for="ramThreshold" class="form-label">
                                    <i class="fas fa-memory"></i> RAM Threshold (%)
                                </label>
                                <input type="number" class="form-control" id="ramThreshold" name="ramThreshold" 
                                       value="<%= ramThreshold %>" min="0" max="100" step="1" required>
                                <div class="form-text">
                                    Agents will receive warnings when RAM usage exceeds this threshold.
                                </div>
                            </div>
                            
                            <div class="d-grid gap-2 d-md-flex justify-content-md-end">
                                <a href="<%= request.getContextPath() %>/" class="btn btn-secondary">
                                    <i class="fas fa-times"></i> <%= Messages.get("common.cancel") %>
                                </a>
                                <button type="submit" class="btn btn-primary">
                                    <i class="fas fa-save"></i> <%= Messages.get("common.save") %>
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
                
                <!-- Current Settings Info -->
                <div class="card mt-4 shadow-sm">
                    <div class="card-body">
                        <h6 class="card-subtitle mb-2 text-muted">
                            <i class="fas fa-info-circle"></i> <%= Messages.get("common.info") %>
                        </h6>
                        <ul class="list-unstyled mb-0">
                            <li><strong><%= Messages.get("settings.language") %>:</strong> 
                                <%= "vi".equals(Messages.getLanguage()) ? "Tiếng Việt" : "English" %>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Bootstrap JS Bundle -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
