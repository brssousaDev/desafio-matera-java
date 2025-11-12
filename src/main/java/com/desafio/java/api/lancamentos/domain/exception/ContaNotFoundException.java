package com.desafio.java.api.lancamentos.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção lançada quando uma Conta não é encontrada pelo seu número.
 *
 * @ResponseStatus(HttpStatus.NOT_FOUND) instrui o Spring a retornar
 * um código HTTP 404 (Not Found) automaticamente.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ContaNotFoundException extends RuntimeException {

	public ContaNotFoundException(String message) {
		super(message);
	}
}