package com.desafio.java.api.lancamentos.domain.model;

import com.desafio.java.api.lancamentos.domain.exception.SaldoInsuficienteException;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "contas",
		indexes = @Index(name = "idx_numero_conta", columnList = "numero_conta", unique = true))
public class Conta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "numero_conta", unique = true, nullable = false, length = 50)
	private String numeroConta; 

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal saldo;

	@Version
	private Long version;

	public BigDecimal getSaldo() {
		return saldo;
	}

	public String getNumeroConta() {
		return numeroConta;
	}

	public void debitar(BigDecimal valor) {
		if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Valor do débito deve ser positivo.");
		}
		
		if (this.saldo.compareTo(valor) < 0) {
			throw new SaldoInsuficienteException("Saldo insuficiente para debitar " + valor);
		}
		
		this.saldo = this.saldo.subtract(valor);
	}

	public void creditar(BigDecimal valor) {
		if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Valor do crédito deve ser positivo.");
		}
		
		this.saldo = this.saldo.add(valor);
	}
}
