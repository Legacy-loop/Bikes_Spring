package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.mapper.LocacaoMapper;
import br.edu.ifc.bikes.repository.LocacaoRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/** Compila o template Jasper e gera o PDF atualizado das locações e devoluções. */
@Service
@RequiredArgsConstructor
public class RelatorioService {
    private final LocacaoRepository locacaoRepository;
    private final LocacaoMapper locacaoMapper;
    private JasperReport relatorioCompilado;

    @PostConstruct
    public void compilarTemplate() {
        try (InputStream template = new ClassPathResource("reports/locacoes.jrxml").getInputStream()) {
            // Compila o JRXML uma única vez na inicialização em vez de recompilar a cada solicitação.
            relatorioCompilado = JasperCompileManager.compileReport(template);
        } catch (JRException | IOException ex) {
            throw new IllegalStateException("Não foi possível compilar o template do relatório de locações", ex);
        }
    }

    @Transactional(readOnly = true)
    public byte[] gerarRelatorio() {
        try {
            var linhas = locacaoRepository.findTodasComClienteEBicicleta().stream()
                    .map(locacaoMapper::toRelatorio)
                    .toList();
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("dataEmissao", new Date());
            // Preenche o relatório compilado com os dados JavaBean retornados pela consulta.
            JasperPrint relatorioPreenchido = JasperFillManager.fillReport(
                    relatorioCompilado,
                    parametros,
                    new JRBeanCollectionDataSource(linhas)
            );
            // Exporta o documento preenchido para bytes PDF que o controller envia ao navegador.
            return JasperExportManager.exportReportToPdf(relatorioPreenchido);
        } catch (JRException ex) {
            throw new IllegalStateException("Não foi possível gerar o PDF do relatório de locações", ex);
        }
    }
}
