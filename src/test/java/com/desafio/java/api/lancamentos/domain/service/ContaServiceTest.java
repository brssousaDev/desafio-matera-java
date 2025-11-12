package com.desafio.java.api.lancamentos.domain.service;

import com.desafio.java.api.lancamentos.domain.exception.ContaNotFoundException;
import com.desafio.java.api.lancamentos.domain.exception.SaldoInsuficienteException;
import com.desafio.java.api.lancamentos.domain.model.Conta;
import com.desafio.java.api.lancamentos.domain.model.Transacao;
import com.desafio.java.api.lancamentos.infrastructure.repository.ContaRepository;
import com.desafio.java.api.lancamentos.infrastructure.repository.TransacaoRepository;
import com.desafio.java.api.lancamentos.model.TransacaoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

	@Mock
	private ContaRepository contaRepository;

	@Mock
	private TransacaoRepository transacaoRepository;

	@InjectMocks
	private ContaService contaService;

	private Conta contaMock;
	private final String NUMERO_CONTA = "12345-6";

	@BeforeEach
	void setUp() {
		contaMock = new Conta();
		ReflectionTestUtils.setField(contaMock, "id", 1L);
		ReflectionTestUtils.setField(contaMock, "numeroConta", NUMERO_CONTA);
		ReflectionTestUtils.setField(contaMock, "saldo", new BigDecimal("1000.00"));
		ReflectionTestUtils.setField(contaMock, "version", 0L);
	}

	@Test
	void deveProcessarTransacoesComSucesso() {
		when(contaRepository.findByNumeroConta(NUMERO_CONTA)).thenReturn(Optional.of(contaMock));

		TransacaoRequest debito = new TransacaoRequest()
				.tipo(TransacaoRequest.TipoEnum.DEBITO)
				.valor("100.00");
		TransacaoRequest credito = new TransacaoRequest()
				.tipo(TransacaoRequest.TipoEnum.CREDITO)
				.valor("50.00");
		List<TransacaoRequest> requests = List.of(debito, credito);

		BigDecimal saldoEsperado = new BigDecimal("950.00");

		Conta contaAtualizada = contaService.processarTransacoes(NUMERO_CONTA, requests);

		assertEquals(0, saldoEsperado.compareTo(contaAtualizada.getSaldo()));

		verify(contaRepository, times(1)).findByNumeroConta(NUMERO_CONTA);
		verify(transacaoRepository, times(2)).save(any(Transacao.class));
	}

	@Test
	void deveLancarContaNotFoundExceptionSeContaNaoExiste() {
		when(contaRepository.findByNumeroConta(anyString())).thenReturn(Optional.empty());

		TransacaoRequest debito = new TransacaoRequest()
				.tipo(TransacaoRequest.TipoEnum.DEBITO)
				.valor("100.00");

		assertThrows(ContaNotFoundException.class, () -> {
			contaService.processarTransacoes("CONTA_INEXISTENTE", List.of(debito));
		});

		verify(transacaoRepository, never()).save(any());
	}

	@Test
	void deveLancarSaldoInsuficienteExceptionEAtomicidade() {
		when(contaRepository.findByNumeroConta(NUMERO_CONTA)).thenReturn(Optional.of(contaMock));

		TransacaoRequest debitoOk = new TransacaoRequest()
				.tipo(TransacaoRequest.TipoEnum.DEBITO)
				.valor("100.00"); 
		TransacaoRequest debitoFalha = new TransacaoRequest()
				.tipo(TransacaoRequest.TipoEnum.DEBITO)
				.valor("901.00"); 

		List<TransacaoRequest> requests = List.of(debitoOk, debitoFalha);

		assertThrows(SaldoInsuficienteException.class, () -> {
			contaService.processarTransacoes(NUMERO_CONTA, requests);
		});

		verify(transacaoRepository, times(1)).save(any(Transacao.class));

		assertEquals(0, new BigDecimal("900.00").compareTo(contaMock.getSaldo()));
	}
}