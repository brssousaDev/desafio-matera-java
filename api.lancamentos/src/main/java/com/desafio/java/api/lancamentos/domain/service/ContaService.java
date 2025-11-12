package com.desafio.java.api.lancamentos.domain.service;

import com.desafio.java.api.lancamentos.domain.exception.ContaNotFoundException;
import com.desafio.java.api.lancamentos.domain.model.Conta;
import com.desafio.java.api.lancamentos.domain.model.TipoTransacao;
import com.desafio.java.api.lancamentos.domain.model.Transacao;
import com.desafio.java.api.lancamentos.infrastructure.repository.ContaRepository;
import com.desafio.java.api.lancamentos.infrastructure.repository.TransacaoRepository;
import com.desafio.java.api.lancamentos.model.TransacaoRequest;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Retryable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;


@Service
public class ContaService {

	private ContaRepository contaRepository;
	private TransacaoRepository transacaoRepository;

	public ContaService(ContaRepository contaRepository, TransacaoRepository transacaoRepository) {
		this.contaRepository = contaRepository;
		this.transacaoRepository = transacaoRepository;
	}

	@Transactional
	@Retryable(retryFor = {
			ObjectOptimisticLockingFailureException.class, 
			OptimisticLockingFailureException.class      
	}, maxAttempts = 5) 
	public Conta processarTransacoes(String numeroConta, List<TransacaoRequest> requests) {

		Conta conta = buscarContaPorNumero(numeroConta);

		for (TransacaoRequest req : requests) {
			BigDecimal valor = new BigDecimal(req.getValor()); 
			
			TipoTransacao tipoTransacaoRequest = validarTipoTransacao(req.getTipo());

			if (tipoTransacaoRequest.equals(TipoTransacao.DEBITO)) {
				conta.debitar(valor); 
			} else {
				conta.creditar(valor);
			}

			transacaoRepository.save(
					new Transacao(conta, tipoTransacaoRequest, valor)
			);
		}

		//return contaRepository.save(conta);
		return conta;
	}

	@Transactional(readOnly = true)
	public Conta getContaParaSaldo(String numeroConta) {
		return buscarContaPorNumero(numeroConta);
	}

	private Conta buscarContaPorNumero(String numeroConta) {
		return contaRepository.findByNumeroConta(numeroConta)
				.orElseThrow(() -> new ContaNotFoundException("Conta não encontrada: " + numeroConta));
	}

	private TipoTransacao validarTipoTransacao(TransacaoRequest.@NotNull TipoEnum tipo) {
		try {
			return TipoTransacao.valueOf(tipo.name());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Tipo de transação inválido: " + tipo);
		}
	}
}
