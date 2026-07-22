import { createTheme, type MantineColorsTuple } from '@mantine/core';

/**
 * Azul de engenharia (confiança) — acento único da marca, do claro (0) ao
 * escuro (9). Shade primário 6 no tema claro e 8 no escuro (padrão do Mantine).
 */
const brand: MantineColorsTuple = [
  '#edf2ff',
  '#dbe4fb',
  '#b7c9f8',
  '#90a9f3',
  '#6d8fef',
  '#4a7bee',
  '#2f6fed', // 6 — base da marca
  '#1f5fd8',
  '#184fb5',
  '#123f91',
];

/**
 * Fonte única dos tokens visuais do app (cor da marca, raio, tipografia).
 * Nenhum componente deve hardcodar cor/raio/fonte — sempre via estes tokens.
 */
export const theme = createTheme({
  primaryColor: 'brand',
  colors: { brand },
  fontFamily: 'Inter, system-ui, sans-serif',
  fontFamilyMonospace: 'ui-monospace, SFMono-Regular, Menlo, monospace',
  headings: { fontFamily: 'Inter, system-ui, sans-serif', fontWeight: '700' },
  defaultRadius: 'md',
  autoContrast: true,
  components: {
    Card: {
      defaultProps: { withBorder: true, shadow: 'sm', radius: 'md', padding: 'lg' },
    },
    Paper: {
      defaultProps: { radius: 'md' },
    },
  },
});
