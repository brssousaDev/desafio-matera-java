package com.desafio.java.api.lancamentos.api;

import com.desafio.java.api.lancamentos.domain.model.Conta;
import com.desafio.java.api.lancamentos.infrastructure.repository.ContaRepository;
import com.desafio.java.api.lancamentos.infrastructure.repository.TransacaoRepository;
import com.desafio.java.api.lancamentos.model.TransacaoRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContaControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ContaRepository contaRepository;

	@Autowired
	private TransacaoRepository transacaoRepository;

	private final String NUMERO_CONTA = "0001-123456-7";

	@BeforeEach
	void setUp() {
		transacaoRepository.deleteAll();
		contaRepository.deleteAll();

		Conta conta = new Conta();
		ReflectionTestUtils.setField(conta, "numeroConta", NUMERO_CONTA);
		ReflectionTestUtils.setField(conta, "saldo", new BigDecimal("500.00"));
		contaRepository.save(conta);
	}

	@Test
	void deveRealizarDebitoEConfirmarSaldoComSucesso() throws Exception {
		TransacaoRequest debito = new TransacaoRequest()
				.tipo(TransacaoRequest.TipoEnum.DEBITO)
				.valor("100.00");
		List<TransacaoRequest> requests = List.of(debito);

		mockMvc.perform(post("/api/v1/contas/{numeroConta}/transacoes", NUMERO_CONTA)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(requests)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.numeroConta", is(NUMERO_CONTA)))
				.andExpect(jsonPath("$.saldo", is("400.00")));

		mockMvc.perform(get("/api/v1/contas/{numeroConta}/saldo", NUMERO_CONTA)
						.accept(MediaType.valueOf("application/hal+json")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.numeroConta", is(NUMERO_CONTA)))
				.andExpect(jsonPath("$.saldo", is("400.00")))
				.andExpect(jsonPath("$._links.self.href", containsString("/api/v1/contas/" + NUMERO_CONTA + "/saldo")));
	}

	@Test
	void deveRetornar422UnprocessableEntityParaSaldoInsuficiente() throws Exception {
		TransacaoRequest debito = new TransacaoRequest()
				.tipo(TransacaoRequest.TipoEnum.DEBITO)
				.valor("9999.00"); 
		List<TransacaoRequest> requests = List.of(debito);

		mockMvc.perform(post("/api/v1/contas/{numeroConta}/transacoes", NUMERO_CONTA)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(requests)))
				.andExpect(status().isUnprocessableEntity()) 
				.andExpect(jsonPath("$.error", is("Erro de Negócio")))
				.andExpect(jsonPath("$.message", containsString("Saldo insuficiente")));

		mockMvc.perform(get("/api/v1/contas/{numeroConta}/saldo", NUMERO_CONTA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.saldo", is("500.00")));
	}

	@Test
	void deveRetornar404NotFoundParaContaInexistente() throws Exception {
		mockMvc.perform(get("/api/v1/contas/CONTA-INEXISTENTE/saldo"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", containsString("Conta não encontrada")));

		TransacaoRequest debito = new TransacaoRequest()
				.tipo(TransacaoRequest.TipoEnum.DEBITO)
				.valor("10.00");

		mockMvc.perform(post("/api/v1/contas/CONTA-INEXISTENTE/transacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(List.of(debito))))
				.andExpect(status().isNotFound());
	}
}