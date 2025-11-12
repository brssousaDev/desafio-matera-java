package com.desafio.java.api.lancamentos.api;

import com.desafio.java.api.lancamentos.domain.model.Conta;
import com.desafio.java.api.lancamentos.domain.service.ContaService;
import com.desafio.java.api.lancamentos.model.Links;
import com.desafio.java.api.lancamentos.model.LinksSelf;
import com.desafio.java.api.lancamentos.model.SaldoResponse;
import com.desafio.java.api.lancamentos.model.TransacaoRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Service
public class ContasApiDelegateImpl implements ContasApiDelegate {

	private final ContaService contaService;

	public ContasApiDelegateImpl(ContaService contaService) {
		this.contaService = contaService;
	}

	@Override
	public ResponseEntity<SaldoResponse> getSaldo(String numeroConta) {
		Conta conta = contaService.getContaParaSaldo(numeroConta);

		SaldoResponse response = mapToSaldoResponse(conta);

		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<SaldoResponse> realizarTransacoes(String numeroConta, List<TransacaoRequest> transacaoRequest) {
		Conta contaAtualizada = contaService.processarTransacoes(numeroConta, transacaoRequest);

		SaldoResponse response = mapToSaldoResponse(contaAtualizada);

		return ResponseEntity.ok(response);
	}

	private SaldoResponse mapToSaldoResponse(Conta conta) {
		SaldoResponse response = new SaldoResponse();
		response.setNumeroConta(conta.getNumeroConta());
		response.setSaldo(conta.getSaldo().toPlainString());

		// Manually construct the URI to avoid issues with property placeholders in @RequestMapping
		URI selfUri = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/v1/contas/{numeroConta}/saldo")
				.buildAndExpand(conta.getNumeroConta())
				.toUri();

		LinksSelf selfLink = new LinksSelf();
		selfLink.setHref(selfUri);

		Links linksContainer = new Links();
		linksContainer.setSelf(selfLink);

		response.setLinks(linksContainer);

		return response;
	}
}
