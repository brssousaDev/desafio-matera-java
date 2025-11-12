package com.desafio.java.api.lancamentos.api.exception;

import com.desafio.java.api.lancamentos.domain.exception.ContaNotFoundException;
import com.desafio.java.api.lancamentos.domain.exception.SaldoInsuficienteException;
import com.desafio.java.api.lancamentos.model.ErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.OffsetDateTime;


@ControllerAdvice
public class RestApiExceptionHandler extends ResponseEntityExceptionHandler {

	/**
	 * Captura exceções de regra de negócio (ex: Saldo Insuficiente).
	 * Retorna 422 UNPROCESSABLE_ENTITY.
	 */
	@ExceptionHandler({ SaldoInsuficienteException.class })
	public ResponseEntity<Object> handleNegocioException(
			Exception ex, WebRequest request) {

		ErrorResponse errorResponse = new ErrorResponse();
		errorResponse.setTimestamp(OffsetDateTime.now());
		errorResponse.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
		errorResponse.setError("Erro de Negócio");
		errorResponse.setMessage(ex.getMessage());
		errorResponse.setPath(((ServletWebRequest)request).getRequest().getRequestURI());

		return new ResponseEntity<>(
				errorResponse, new HttpHeaders(), HttpStatus.UNPROCESSABLE_ENTITY);
	}

	/**
	 * Captura a falha de 'retry' (concorrência).
	 * Se o @Retryable falhar 5 vezes, esta exceção será lançada.
	 * Retorna 409 CONFLICT.
	 */
	@ExceptionHandler({
			ObjectOptimisticLockingFailureException.class, 
			OptimisticLockingFailureException.class      
	})
	public ResponseEntity<Object> handleConcorrenciaException(
			Exception ex, WebRequest request) {

		ErrorResponse errorResponse = new ErrorResponse();
		errorResponse.setTimestamp(OffsetDateTime.now());
		errorResponse.setStatus(HttpStatus.CONFLICT.value());
		errorResponse.setError("Conflito de Concorrência");
		errorResponse.setMessage(
				"Não foi possível processar a transação devido a alta concorrência. " +
						"Nenhuma operação foi realizada. Por favor, tente novamente."
		);
		errorResponse.setPath(((ServletWebRequest)request).getRequest().getRequestURI());

		logger.warn("Falha de Lock Otimista após retries: " + ex.getMessage());

		return new ResponseEntity<>(
				errorResponse, new HttpHeaders(), HttpStatus.CONFLICT);
	}

	/**
	 * Captura exceções de Recurso Não Encontrado (ex: Conta).
	 * Retorna 404 NOT FOUND.
	 */
	@ExceptionHandler({ ContaNotFoundException.class })
	public ResponseEntity<Object> handleResourceNotFoundException(
			Exception ex, WebRequest request) {

		ErrorResponse errorResponse = new ErrorResponse();
		errorResponse.setTimestamp(OffsetDateTime.now());
		errorResponse.setStatus(HttpStatus.NOT_FOUND.value());
		errorResponse.setError("Recurso não encontrado");
		errorResponse.setMessage(ex.getMessage());
		errorResponse.setPath(((ServletWebRequest)request).getRequest().getRequestURI());

		return new ResponseEntity<>(
				errorResponse, new HttpHeaders(), HttpStatus.NOT_FOUND);
	}

	/**
	 * Captura exceções de argumentos inválidos (ex: valor negativo).
	 * Retorna 400 BAD REQUEST.
	 */
	@ExceptionHandler({ IllegalArgumentException.class })
	public ResponseEntity<Object> handleIllegalArgumentException(
			Exception ex, WebRequest request) {

		ErrorResponse errorResponse = new ErrorResponse();
		errorResponse.setTimestamp(OffsetDateTime.now());
		errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
		errorResponse.setError("Requisição inválida");
		errorResponse.setMessage(ex.getMessage());
		errorResponse.setPath(((ServletWebRequest)request).getRequest().getRequestURI());

		return new ResponseEntity<>(
				errorResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST);
	}

}
