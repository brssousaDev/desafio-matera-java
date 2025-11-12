package com.desafio.java.api.lancamentos.infrastructure.repository;

import com.desafio.java.api.lancamentos.domain.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContaRepository extends JpaRepository<Conta, Long> {
	Optional<Conta> findByNumeroConta(String numeroConta);
}
