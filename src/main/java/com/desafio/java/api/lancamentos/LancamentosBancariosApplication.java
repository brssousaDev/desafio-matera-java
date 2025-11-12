package com.desafio.java.api.lancamentos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class LancamentosBancariosApplication {

	public static void main(String[] args) {
		SpringApplication.run(LancamentosBancariosApplication.class, args);
	}

}
