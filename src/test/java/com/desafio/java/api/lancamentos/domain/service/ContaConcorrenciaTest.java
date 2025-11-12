package com.desafio.java.api.lancamentos.domain.service;

import com.desafio.java.api.lancamentos.domain.model.Conta;
import com.desafio.java.api.lancamentos.infrastructure.repository.ContaRepository;
import com.desafio.java.api.lancamentos.infrastructure.repository.TransacaoRepository;
import com.desafio.java.api.lancamentos.model.TransacaoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@ActiveProfiles("test")
class ContaConcorrenciaTest {

	private static final Logger log = LoggerFactory.getLogger(ContaConcorrenciaTest.class);

	@Autowired
	private ContaService contaService;

	@Autowired
	private ContaRepository contaRepository;

	@Autowired
	private TransacaoRepository transacaoRepository;

	private final String NUMERO_CONTA = "CONCORRENCIA-1";
	private final BigDecimal SALDO_INICIAL = new BigDecimal("1000.00");
	private final BigDecimal VALOR_DEBITO = new BigDecimal("1.00");
	private final int NUMERO_THREADS = 20;

	@BeforeEach
	@Transactional
	void setUp() {
		transacaoRepository.deleteAll();
		contaRepository.deleteAll();

		Conta conta = new Conta();
		ReflectionTestUtils.setField(conta, "numeroConta", NUMERO_CONTA);
		ReflectionTestUtils.setField(conta, "saldo", SALDO_INICIAL);
		contaRepository.saveAndFlush(conta);
	}

	@Test
	void deveManterConsistenciaDoSaldoSobAltaConcorrencia() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(NUMERO_THREADS);

		CountDownLatch startGate = new CountDownLatch(1);
		CountDownLatch finishGate = new CountDownLatch(NUMERO_THREADS);

		AtomicInteger falhas = new AtomicInteger(0);

		TransacaoRequest request = new TransacaoRequest()
				.tipo(TransacaoRequest.TipoEnum.DEBITO)
				.valor(VALOR_DEBITO.toPlainString());
		List<TransacaoRequest> requests = List.of(request);

		for (int i = 0; i < NUMERO_THREADS; i++) {
			int threadNum = i + 1;
			executor.submit(() -> {
				try {
					startGate.await();

					log.info("Thread {} iniciando débito...", threadNum);
					contaService.processarTransacoes(NUMERO_CONTA, requests);

					log.info("Thread {} concluiu o débito.", threadNum);

				} catch (Exception e) {
					log.warn("Thread {} falhou permanentemente após retentativas: {}", threadNum, e.getMessage());
					falhas.incrementAndGet();
				} finally {
					finishGate.countDown();
				}
			});
		}

		log.info("Disparando {} threads...", NUMERO_THREADS);
		startGate.countDown();

		if (!finishGate.await(20, TimeUnit.SECONDS)) {
			fail("Timeout - Nem todas as threads terminaram.");
		}

		executor.shutdown();
		log.info("Todas as threads terminaram.");

		int numFalhas = falhas.get();
		int transacoesBemSucedidas = NUMERO_THREADS - numFalhas;

		log.warn("--- RELATÓRIO DE CONCORRÊNCIA ---");
		log.warn("Transações com Sucesso: {}/{}", transacoesBemSucedidas, NUMERO_THREADS);
		log.warn("Falhas Permanentes (ex: 409 Conflict): {}/{}", numFalhas, NUMERO_THREADS);
		log.warn("---------------------------------");

		BigDecimal debitosComputados = VALOR_DEBITO.multiply(new BigDecimal(transacoesBemSucedidas));
		BigDecimal saldoEsperado = SALDO_INICIAL.subtract(debitosComputados);

		Conta contaFinal = contaRepository.findByNumeroConta(NUMERO_CONTA)
				.orElseThrow(() -> new RuntimeException("Conta desapareceu do DB"));

		log.info("Saldo Inicial: {}. Saldo Final (Real): {}", SALDO_INICIAL, contaFinal.getSaldo());
		log.info("Saldo Esperado (Com base nas falhas): {}", saldoEsperado);

		assertEquals(0, saldoEsperado.compareTo(contaFinal.getSaldo()),
				"O saldo final não reflete o número de transações que tiveram sucesso.");

		long numTransacoes = transacaoRepository.count();
		log.info("Número de transações registradas: {}", numTransacoes);

		assertEquals(transacoesBemSucedidas, numTransacoes,
				"O número de transações salvas no DB não reflete o número de transações que tiveram sucesso.");
	}
}