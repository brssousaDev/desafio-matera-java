package com.desafio.java.api.lancamentos.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Transacao {
	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "conta_id")
	private Conta conta;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoTransacao tipo;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal valor;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime dataHoraProcessamento;

	public Transacao() {
	}


	public Transacao(Conta conta, TipoTransacao tipoTransacaoRequest, BigDecimal valor) {
		this.conta = conta;
		this.tipo = tipoTransacaoRequest;
		this.valor = valor;
	}
}
