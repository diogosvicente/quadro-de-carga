package br.uerj.eletrica.service;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Gera a tabela do Quadro Elétrico como DXF R12 ASCII (docs/CALCULOS.md §6c).
 *
 * <p>A tabela é desenhada como uma grade: entidades {@code LINE} (linhas horizontais e verticais)
 * mais uma entidade {@code TEXT} por célula, alinhada à esquerda com pequena margem. A origem fica
 * no canto superior esquerdo e o eixo Y decresce por linha (primeira linha no topo). Estrutura mínima:
 * SECTION HEADER (vazia) + SECTION ENTITIES + ENDSEC + EOF — abre em visualizadores DXF comuns
 * (AutoCAD, LibreCAD, etc.).
 *
 * <p>Compatibilidade de codepage: "mm²" é escrito como "mm2" e caracteres fora de Latin-1 são
 * substituídos (§6c).
 */
final class DxfTabelaWriter {

    private static final double ALTURA_LINHA = 8.0;   // altura de cada linha da tabela (unidades de desenho)
    private static final double ALTURA_TEXTO = 2.5;   // altura do texto
    private static final double MARGEM = 1.5;         // recuo do texto dentro da célula
    private static final double LARGURA_CHAR = 1.8;   // largura estimada por caractere (para dimensionar colunas)
    private static final double LARGURA_MIN_COLUNA = 12.0;

    private DxfTabelaWriter() {
    }

    /**
     * @param cabecalhos títulos das colunas
     * @param linhas     linhas de dados (cada uma com {@code cabecalhos.length} células)
     * @return bytes do arquivo DXF (Latin-1)
     */
    static byte[] gerar(String[] cabecalhos, List<String[]> linhas) {
        int nColunas = cabecalhos.length;
        int nLinhas = 1 + linhas.size(); // cabeçalho + dados

        double[] largura = larguraColunas(cabecalhos, linhas, nColunas);
        double[] x = new double[nColunas + 1];
        for (int c = 0; c < nColunas; c++) {
            x[c + 1] = x[c] + largura[c];
        }
        double larguraTotal = x[nColunas];
        double alturaTotal = nLinhas * ALTURA_LINHA;

        StringBuilder sb = new StringBuilder();
        // SECTION HEADER (vazia)
        par(sb, 0, "SECTION");
        par(sb, 2, "HEADER");
        par(sb, 0, "ENDSEC");
        // SECTION ENTITIES
        par(sb, 0, "SECTION");
        par(sb, 2, "ENTITIES");

        // grade: linhas horizontais (bordas de linha) e verticais (bordas de coluna)
        for (int i = 0; i <= nLinhas; i++) {
            double y = -i * ALTURA_LINHA;
            linha(sb, 0.0, y, larguraTotal, y);
        }
        for (int c = 0; c <= nColunas; c++) {
            linha(sb, x[c], 0.0, x[c], -alturaTotal);
        }

        // textos: cabeçalho na linha 0, dados nas linhas seguintes
        double baseline = (ALTURA_LINHA - ALTURA_TEXTO) / 2.0;
        for (int c = 0; c < nColunas; c++) {
            texto(sb, x[c] + MARGEM, -ALTURA_LINHA + baseline, cabecalhos[c]);
        }
        for (int r = 0; r < linhas.size(); r++) {
            String[] celulas = linhas.get(r);
            double y = -(r + 2) * ALTURA_LINHA + baseline;
            for (int c = 0; c < nColunas; c++) {
                texto(sb, x[c] + MARGEM, y, c < celulas.length ? celulas[c] : "");
            }
        }

        par(sb, 0, "ENDSEC");
        par(sb, 0, "EOF");
        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private static double[] larguraColunas(String[] cabecalhos, List<String[]> linhas, int nColunas) {
        double[] largura = new double[nColunas];
        for (int c = 0; c < nColunas; c++) {
            int maxChars = sanitizar(cabecalhos[c]).length();
            for (String[] celulas : linhas) {
                if (c < celulas.length) {
                    maxChars = Math.max(maxChars, sanitizar(celulas[c]).length());
                }
            }
            largura[c] = Math.max(LARGURA_MIN_COLUNA, maxChars * LARGURA_CHAR + 2 * MARGEM);
        }
        return largura;
    }

    private static void linha(StringBuilder sb, double x1, double y1, double x2, double y2) {
        par(sb, 0, "LINE");
        par(sb, 8, "0");
        par(sb, 10, x1);
        par(sb, 20, y1);
        par(sb, 30, 0.0);
        par(sb, 11, x2);
        par(sb, 21, y2);
        par(sb, 31, 0.0);
    }

    private static void texto(StringBuilder sb, double x, double y, String conteudo) {
        par(sb, 0, "TEXT");
        par(sb, 8, "0");
        par(sb, 10, x);
        par(sb, 20, y);
        par(sb, 30, 0.0);
        par(sb, 40, ALTURA_TEXTO);
        par(sb, 1, sanitizar(conteudo));
    }

    private static void par(StringBuilder sb, int codigo, String valor) {
        sb.append(codigo).append('\n').append(valor).append('\n');
    }

    private static void par(StringBuilder sb, int codigo, double valor) {
        sb.append(codigo).append('\n').append(String.format(Locale.US, "%.3f", valor)).append('\n');
    }

    /**
     * Transforma o texto em ASCII 7 bits para máxima compatibilidade do DXF R12 (evita mojibake de
     * acentos em Latin-1): NFKD decompõe acentos e formas de compatibilidade (ç→c, ã→a, ²→2, º→o),
     * removem-se os diacríticos e qualquer resíduo não-ASCII vira '?'.
     */
    private static String sanitizar(String s) {
        if (s == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(s, Normalizer.Form.NFKD).replaceAll("\\p{M}+", "");
        StringBuilder out = new StringBuilder(semAcento.length());
        for (int i = 0; i < semAcento.length(); i++) {
            char c = semAcento.charAt(i);
            out.append(c <= 0x7E ? c : '?');
        }
        return out.toString();
    }
}
