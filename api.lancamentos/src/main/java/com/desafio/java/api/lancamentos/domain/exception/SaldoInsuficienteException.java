package com.desafio.java.api.lancamentos.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção lançada quando uma operação (ex: débito) falha
 * por falta de saldo na conta.
 *
 * É uma RuntimeException (unchecked) pois representa uma
 * falha de regra de negócio (um estado inválido solicitado pelo cliente),
 * não um erro técnico que precisa ser checado.
 *
 * @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY) faz com que o Spring
 * automaticamente retorne um código HTTP 422 (Unprocessable Entity)
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class SaldoInsuficienteException extends RuntimeException {

	public SaldoInsuficienteException(String message) {
		super(message);
	}
}