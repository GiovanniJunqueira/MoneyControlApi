package com.financeiro.api.controller;

import com.financeiro.api.dto.dashboard.DevedorGeralResponse;
import com.financeiro.api.dto.dashboard.DevedoresDashboardResponse;
import com.financeiro.api.dto.dashboard.GastosDashboardResponse;
import com.financeiro.api.dto.dashboard.VisaoGeralResponse;
import com.financeiro.api.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // GET /tabs/{tabId}/dashboard/gastos?period=2026-08
    @GetMapping("/tabs/{tabId}/dashboard/gastos")
    public GastosDashboardResponse gastos(@PathVariable UUID tabId, @RequestParam(required = false) String period) {
        return dashboardService.gastos(tabId, period);
    }

    // GET /tabs/{tabId}/dashboard/devedores?period=2026-08
    @GetMapping("/tabs/{tabId}/dashboard/devedores")
    public DevedoresDashboardResponse devedores(@PathVariable UUID tabId, @RequestParam(required = false) String period) {
        return dashboardService.devedores(tabId, period);
    }

    // GET /dashboard/visao-geral?period=2026-08 - soma todas as abas do usuario
    @GetMapping("/dashboard/visao-geral")
    public VisaoGeralResponse visaoGeral(@RequestParam(required = false) String period) {
        return dashboardService.visaoGeral(period);
    }

    // GET /dashboard/devedores-geral - devedores de todas as abas, sem merge (mostra de qual aba e cada um)
    @GetMapping("/dashboard/devedores-geral")
    public List<DevedorGeralResponse> devedoresGeral() {
        return dashboardService.devedoresGeral();
    }
}
