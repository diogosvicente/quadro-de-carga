package br.uerj.eletrica.service;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import java.util.Locale;

/**
 * Gera a tabela do Quadro Elétrico como DXF R12 ASCII (docs/CALCULOS.md §6c).
 *
 * <p>A tabela é desenhada como uma grade: entidades {@code LINE} (linhas horizontais e verticais)
 * mais uma entidade {@code TEXT} por célula, alinhada à esquerda com pequena margem. A origem fica
 * no canto superior esquerdo e o eixo Y decresce por linha (primeira linha no topo).
 *
 * <p>Estrutura: SECTION HEADER (com $ACADVER e extensões do desenho, para o "zoom extents" abrir
 * enquadrado) + SECTION TABLES (LTYPE/LAYER/STYLE) + SECTION ENTITIES + EOF. Definir o STYLE
 * explicitamente evita que o CAD substitua a fonte por uma proporcional mais larga, o que
 * desconfigurava a tabela (texto invadindo a célula vizinha).
 *
 * <p>Acentuação e "mm²": o arquivo declara {@code $DWGCODEPAGE = ANSI_1252} e é gravado em
 * Windows-1252, e o STYLE usa uma fonte TrueType (Arial), que tem os glifos de "²" e dos acentos.
 * Caracteres fora da Windows-1252 são transliterados para ASCII.
 */
final class DxfTabelaWriter {

    private static final double ALTURA_TEXTO = 2.5;   // altura do texto
    private static final double ALTURA_LINHA = 8.0;   // altura de cada linha da tabela
    private static final double MARGEM = 2.0;         // recuo do texto dentro da célula

    /**
     * Fator de largura do texto (grupo 41 do TEXT e do STYLE): 1.0 = fonte sem compressão.
     * Mantido em 1.0 para que a largura renderizada case com a estimativa abaixo.
     */
    private static final double FATOR_LARGURA_TEXTO = 1.0;

    /**
     * Avanço horizontal estimado por caractere, como fração da altura do texto. Medido na
     * renderização real do AutoCAD (~0,86 × altura para a fonte txt); usa-se 0,95 para dar folga
     * e cobrir substituição de fonte. Subestimar isto faz o texto transbordar da célula.
     */
    private static final double AVANCO_CHAR = 0.95;
    private static final double LARGURA_CHAR = ALTURA_TEXTO * AVANCO_CHAR;
    private static final double LARGURA_MIN_COLUNA = 14.0;

    /** Codepage declarada no arquivo e usada na gravação — cobre acentos e "²". */
    private static final Charset CODEPAGE = Charset.forName("windows-1252");

    /** Fonte TrueType: tem "²" e acentos (a SHX padrão do CAD não tem). */
    private static final String FONTE = "arial.ttf";

    private DxfTabelaWriter() {
    }

    /**
     * @param cabecalhos títulos das colunas
     * @param linhas     linhas de dados (cada uma com {@code cabecalhos.length} células)
     * @return bytes do arquivo DXF (ASCII)
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
        cabecalhoDxf(sb, larguraTotal, alturaTotal);
        tabelasDxf(sb);

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
        return sb.toString().getBytes(CODEPAGE);
    }

    /** HEADER com versão e extensões do desenho (para abrir enquadrado no CAD). */
    private static void cabecalhoDxf(StringBuilder sb, double larguraTotal, double alturaTotal) {
        par(sb, 0, "SECTION");
        par(sb, 2, "HEADER");
        par(sb, 9, "$ACADVER");
        par(sb, 1, "AC1009");
        par(sb, 9, "$DWGCODEPAGE");
        par(sb, 3, "ANSI_1252");
        par(sb, 9, "$EXTMIN");
        par(sb, 10, 0.0);
        par(sb, 20, -alturaTotal);
        par(sb, 30, 0.0);
        par(sb, 9, "$EXTMAX");
        par(sb, 10, larguraTotal);
        par(sb, 20, 0.0);
        par(sb, 30, 0.0);
        par(sb, 0, "ENDSEC");
    }

    /** TABLES mínimas (LTYPE, LAYER e STYLE) — o STYLE fixa a fonte e o fator de largura. */
    private static void tabelasDxf(StringBuilder sb) {
        par(sb, 0, "SECTION");
        par(sb, 2, "TABLES");

        par(sb, 0, "TABLE");
        par(sb, 2, "LTYPE");
        par(sb, 70, 1);
        par(sb, 0, "LTYPE");
        par(sb, 2, "CONTINUOUS");
        par(sb, 70, 0);
        par(sb, 3, "Solid line");
        par(sb, 72, 65);
        par(sb, 73, 0);
        par(sb, 40, 0.0);
        par(sb, 0, "ENDTAB");

        par(sb, 0, "TABLE");
        par(sb, 2, "LAYER");
        par(sb, 70, 1);
        par(sb, 0, "LAYER");
        par(sb, 2, "0");
        par(sb, 70, 0);
        par(sb, 62, 7);
        par(sb, 6, "CONTINUOUS");
        par(sb, 0, "ENDTAB");

        par(sb, 0, "TABLE");
        par(sb, 2, "STYLE");
        par(sb, 70, 1);
        par(sb, 0, "STYLE");
        par(sb, 2, "STANDARD");
        par(sb, 70, 0);
        par(sb, 40, 0.0);                      // altura fixa 0 = definida por entidade
        par(sb, 41, FATOR_LARGURA_TEXTO);      // fator de largura
        par(sb, 50, 0.0);                      // ângulo de inclinação
        par(sb, 71, 0);
        par(sb, 42, ALTURA_TEXTO);             // última altura usada
        par(sb, 3, FONTE);                     // TrueType: tem "²" e acentos
        par(sb, 4, "");
        par(sb, 0, "ENDTAB");

        par(sb, 0, "ENDSEC");
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
        par(sb, 41, FATOR_LARGURA_TEXTO);
        par(sb, 7, "STANDARD");
    }

    private static void par(StringBuilder sb, int codigo, String valor) {
        sb.append(codigo).append('\n').append(valor).append('\n');
    }

    private static void par(StringBuilder sb, int codigo, int valor) {
        sb.append(codigo).append('\n').append(valor).append('\n');
    }

    private static void par(StringBuilder sb, int codigo, double valor) {
        sb.append(codigo).append('\n').append(String.format(Locale.US, "%.3f", valor)).append('\n');
    }

    /**
     * Mantém o texto como está quando os caracteres existem na Windows-1252 — o caso de "mm²",
     * "Descrição", "Tensão", "Pólos" e "Seção" — já que o arquivo declara essa codepage e usa
     * fonte TrueType. Só o que estiver fora dela cai para um equivalente ASCII (ex.: travessão
     * "—" vira "-"), evitando byte inválido no arquivo.
     */
    private static String sanitizar(String s) {
        if (s == null) {
            return "";
        }
        CharsetEncoder encoder = CODEPAGE.newEncoder();
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(encoder.canEncode(c) ? String.valueOf(c) : substitutoAscii(c));
        }
        return out.toString();
    }

    private static String substitutoAscii(char c) {
        return switch (c) {
            case '—', '–', '‑' -> "-";
            case '“', '”' -> "\"";
            case '‘', '’' -> "'";
            case '…' -> "...";
            case '³' -> "3";
            default -> "?";
        };
    }
}
