package br.uerj.eletrica.web;

import br.uerj.eletrica.service.ExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Download da tabela do Quadro Elétrico em .xlsx e .dxf (docs/CALCULOS.md §6c).
 * Quadro inexistente → 404 (RecursoNaoEncontradoException, via QuadroService.resumo).
 */
@RestController
@RequestMapping("/api/quadros/{id}")
public class ExportController {

    private static final MediaType TIPO_XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final MediaType TIPO_DXF = MediaType.parseMediaType("application/dxf");

    private final ExportService service;

    public ExportController(ExportService service) {
        this.service = service;
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> xlsx(@PathVariable Long id) {
        byte[] corpo = service.xlsx(id);
        return ResponseEntity.ok()
                .headers(anexo("quadro-" + id + ".xlsx", TIPO_XLSX, corpo.length))
                .body(corpo);
    }

    @GetMapping("/export.dxf")
    public ResponseEntity<byte[]> dxf(@PathVariable Long id) {
        byte[] corpo = service.dxf(id);
        return ResponseEntity.ok()
                .headers(anexo("quadro-" + id + ".dxf", TIPO_DXF, corpo.length))
                .body(corpo);
    }

    private static HttpHeaders anexo(String nomeArquivo, MediaType tipo, int tamanho) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(tipo);
        headers.setContentDisposition(ContentDisposition.attachment().filename(nomeArquivo).build());
        headers.setContentLength(tamanho);
        return headers;
    }
}
