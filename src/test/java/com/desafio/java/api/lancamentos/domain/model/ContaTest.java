package com.desafio.java.api.lancamentos.domain.model;

import com.desafio.java.api.lancamentos.domain.exception.SaldoInsuficienteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ContaTest {

	private Conta conta;

	@BeforeEach
	void setUp() {
		conta = new Conta();
		ReflectionTestUtils.setField(conta, "saldo", new BigDecimal("100.00"));
	}

	@Test
	void deveDebitarComSucessoQuandoHaSaldoSuficiente() {
		BigDecimal valorDebito = new BigDecimal("40.00");
		BigDecimal saldoEsperado = new BigDecimal("60.00");

		conta.debitar(valorDebito);

		assertEquals(0, saldoEsperado.compareTo(conta.getSaldo()));
	}

	@Test
	void deveLancarSaldoInsuficienteExceptionAoDebitarValorMaiorQueSaldo() {
		BigDecimal valorDebito = new BigDecimal("110.00");

		SaldoInsuficienteException exception = assertThrows(
				SaldoInsuficienteException.class,
				() -> conta.debitar(valorDebito)
		);

		assertTrue(exception.getMessage().contains("Saldo insuficiente"));

		assertEquals(0, new BigDecimal("100.00").compareTo(conta.getSaldo()));
	}

	@Test
	void deveLancarIllegalArgumentExceptionParaDebitoNegativoOuZero() {
		assertThrows(IllegalArgumentException.class, () -> conta.debitar(BigDecimal.ZERO));
		assertThrows(IllegalArgumentException.class, () -> conta.debitar(new BigDecimal("-50.00")));
	}

	@Test
	void deveCreditarValorComSucesso() {
		BigDecimal valorCredito = new BigDecimal("50.50");
		BigDecimal saldoEsperado = new BigDecimal("150.50");

		conta.creditar(valorCredito);

		assertEquals(0, saldoEsperado.compareTo(conta.getSaldo()));
	}

	@Test
	void deveLancarIllegalArgumentExceptionParaCreditoNegativoOuZero() {
		assertThrows(IllegalArgumentException.class, () -> conta.creditar(BigDecimal.ZERO));
		assertThrows(IllegalArgumentException.class, () -> conta.creditar(new BigDecimal("-50.00")));
	}
}
