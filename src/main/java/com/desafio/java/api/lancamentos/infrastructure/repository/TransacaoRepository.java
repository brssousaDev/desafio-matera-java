package com.desafio.java.api.lancamentos.infrastructure.repository;

import com.desafio.java.api.lancamentos.domain.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
}
