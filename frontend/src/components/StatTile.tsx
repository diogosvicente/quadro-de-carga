import { Paper, Text } from '@mantine/core';

interface StatTileProps {
  rotulo: string;
  valor: string;
  legenda?: string;
}

/**
 * Cartão de estatística (KPI): rótulo em caixa de frase (tinta dimmed), valor
 * grande em algarismos tabulares e legenda opcional dimmed. Wrapper fino sobre
 * Paper — não reestilizar tile a tile.
 */
export function StatTile({ rotulo, valor, legenda }: StatTileProps) {
  return (
    <Paper withBorder p="md">
      <Text size="xs" c="dimmed">
        {rotulo}
      </Text>
      <Text fw={600} fz="1.5rem" lh={1.2} className="num-tab" style={{ overflowWrap: 'anywhere' }}>
        {valor}
      </Text>
      {legenda ? (
        <Text size="xs" c="dimmed" mt={4}>
          {legenda}
        </Text>
      ) : null}
    </Paper>
  );
}
