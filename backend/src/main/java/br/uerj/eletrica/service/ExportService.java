package br.uerj.eletrica.service;

import br.uerj.eletrica.calc.ResultadoAlimentador;
import br.uerj.eletrica.dto.circuito.CircuitoResponse;
import br.uerj.eletrica.dto.quadro.ResumoQuadroResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Exportação da tabela do Quadro Elétrico em .xlsx (Apache POI) e .dxf (docs/CALCULOS.md §6c).
 *
 * <p>As duas exportações compartilham a mesma tabela: linhas são os circuitos em ordem de número e,
 * por último, o alimentador geral. Quando o quadro não tem circuitos (alimentador ausente), só o
 * cabeçalho é exportado — sem linha de alimentador e sem erro.
 */
@Service
public class ExportService {

    static final String[] CABECALHOS = {
            "Nº do circuito", "Descrição", "Tensão (V)", "Pólos", "Disjuntor", "Seção dos cabos"
    };

    /** Larguras (em nº de caracteres) das colunas do XLSX — a coluna de seção é a mais larga. */
    private static final int[] LARGURAS_CHARS = {14, 30, 12, 16, 14, 44};

    private final QuadroService quadroService;

    public ExportService(QuadroService quadroService) {
        this.quadroService = quadroService;
    }

    public byte[] xlsx(Long quadroId) {
        List<String[]> linhas = montar(quadroId);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Quadro Elétrico");

            Font negrito = wb.createFont();
            negrito.setBold(true);
            CellStyle estiloCabecalho = wb.createCellStyle();
            estiloCabecalho.setFont(negrito);
            bordasFinas(estiloCabecalho);
            CellStyle estiloCorpo = wb.createCellStyle();
            bordasFinas(estiloCorpo);

            Row cabecalho = sheet.createRow(0);
            for (int c = 0; c < CABECALHOS.length; c++) {
                Cell celula = cabecalho.createCell(c);
                celula.setCellValue(CABECALHOS[c]);
                celula.setCellStyle(estiloCabecalho);
            }
            for (int r = 0; r < linhas.size(); r++) {
                Row linha = sheet.createRow(r + 1);
                String[] celulas = linhas.get(r);
                for (int c = 0; c < CABECALHOS.length; c++) {
                    Cell celula = linha.createCell(c);
                    celula.setCellValue(c < celulas.length ? celulas[c] : "");
                    celula.setCellStyle(estiloCorpo);
                }
            }
            for (int c = 0; c < LARGURAS_CHARS.length; c++) {
                sheet.setColumnWidth(c, LARGURAS_CHARS[c] * 256);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar XLSX do quadro " + quadroId + ".", e);
        }
    }

    public byte[] dxf(Long quadroId) {
        return DxfTabelaWriter.gerar(CABECALHOS, montar(quadroId));
    }

    /** Monta as linhas da tabela (circuitos em ordem de número + alimentador por último). */
    private List<String[]> montar(Long quadroId) {
        ResumoQuadroResponse resumo = quadroService.resumo(quadroId);
        List<String[]> linhas = new ArrayList<>();
        for (CircuitoResponse c : resumo.circuitos()) {
            linhas.add(new String[]{
                    String.valueOf(c.numero()),
                    c.descricao() == null ? "" : c.descricao(),
                    String.valueOf(c.tensaoV()),
                    polos(c.fases()),
                    c.resultado().disjuntor().rotulo(),
                    c.resultado().rotuloCabo()
            });
        }
        ResultadoAlimentador a = resumo.alimentador();
        if (a != null) {
            linhas.add(new String[]{
                    "Geral",
                    "Alimentador geral",
                    String.valueOf(a.tensaoV()),
                    polos(a.fases()),
                    a.disjuntorGeral().rotulo(),
                    a.rotuloCabo()
            });
        }
        return linhas;
    }

    private static String polos(int fases) {
        return switch (fases) {
            case 1 -> "Monofásico";
            case 2 -> "Bifásico";
            case 3 -> "Trifásico";
            default -> fases + " fases";
        };
    }

    private static void bordasFinas(CellStyle estilo) {
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
    }
}
