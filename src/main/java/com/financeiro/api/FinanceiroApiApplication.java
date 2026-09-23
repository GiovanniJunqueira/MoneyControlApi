package com.financeiro.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class FinanceiroApiApplication {
    public static void main(String[] args) {
        // o Render roda o container em UTC - sem isso, todo LocalDate.now()/LocalDateTime.now()
        // (usado em todo o modulo Bets pra decidir "qual é o dia de hoje") virava o dia 3h mais
        // cedo que o horário de Brasília (ex: 21h em SP já contava como meia-noite do dia seguinte).
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        SpringApplication.run(FinanceiroApiApplication.class, args);
    }
}
