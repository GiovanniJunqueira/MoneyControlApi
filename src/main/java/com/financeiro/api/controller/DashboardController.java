package com.financeiro.api.controller;

import com.financeiro.api.dto.dashboard.DevedoresDashboardResponse;
import com.financeiro.api.dto.dashboard.GastosDashboardResponse;
import com.financeiro.api.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // GET /dashboard/gastos?period=2026-08
    @GetMapping("/gastos")
    public GastosDashboardResponse gastos(@RequestParam(required = false) String period) {
        return dashboardService.gastos(period);
    }

    // GET /dashboard/devedores?period=2026-08
    @GetMapping("/devedores")
    public DevedoresDashboardResponse devedores(@RequestParam(required = false) String period) {
        return dashboardService.devedores(period);
    }
}
