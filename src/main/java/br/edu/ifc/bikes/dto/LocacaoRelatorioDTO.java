package br.edu.ifc.bikes.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Expõe propriedades JavaBean já formatadas para o datasource do JasperReports. */
@Getter
@RequiredArgsConstructor
public class LocacaoRelatorioDTO {
    private final String nomeCliente;
    private final String codigoBicicleta;
    private final String dataRetirada;
    private final String dataDevolucao;
    private final String duracao;
    private final String status;
}
