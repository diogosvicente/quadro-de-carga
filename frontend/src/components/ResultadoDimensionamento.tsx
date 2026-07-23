import { Card, Divider, Group, SimpleGrid, Stack, Text, Title } from '@mantine/core';
import type { ResultadoCircuito } from '../types/circuito';
import { StatTile } from './StatTile';
import { amp, mm2, num, pct } from '../utils/formato';

export interface ItemDefinicao {
  rotulo: string;
  valor: string;
  forte?: boolean;
}

/** Lista rótulo/valor (definições) com divisores entre as linhas. */
export function ListaDefinicao({ itens }: { itens: ItemDefinicao[] }) {
  return (
    <Stack gap={0}>
      {itens.map((item, indice) => (
        <div key={item.rotulo}>
          {indice > 0 ? <Divider /> : null}
          <Group justify="space-between" gap="md" wrap="nowrap" py="xs">
            <Text size="sm" c="dimmed">
              {item.rotulo}
            </Text>
            <Text
              size="sm"
              fw={600}
              ta="right"
              className="num-tab"
              c={item.forte ? 'brand.6' : undefined}
            >
              {item.valor}
            </Text>
          </Group>
        </div>
      ))}
    </Stack>
  );
}

interface ResultadoDimensionamentoProps {
  resultado: ResultadoCircuito;
  titulo?: string;
}

/**
 * Painel completo do resultado de um circuito: destaques, dimensionamento dos
 * condutores e fatores de correção. Usado no detalhe e na prévia do formulário.
 */
export function ResultadoDimensionamento({
  resultado,
  titulo = 'Resultado do Dimensionamento',
}: ResultadoDimensionamentoProps) {
  const r = resultado;

  const condutores: ItemDefinicao[] = [
    { rotulo: 'Seção por sobrecorrente', valor: mm2(r.secaoSobrecorrenteMm2) },
    { rotulo: 'Seção por queda de tensão (calculada)', valor: mm2(r.secaoQuedaCalculadaMm2) },
    { rotulo: 'Seção por queda de tensão (comercial)', valor: mm2(r.secaoQuedaMm2) },
    { rotulo: 'Seção mínima normativa', valor: mm2(r.secaoMinimaMm2) },
    { rotulo: 'Seção final (máximo)', valor: mm2(r.secaoFinalMm2), forte: true },
    ...(r.circuitosParalelos > 1
      ? [
          {
            rotulo: `Seção por fase (${r.circuitosParalelos} em paralelo)`,
            valor: mm2(r.secaoFaseParaleloMm2),
          },
        ]
      : []),
    { rotulo: 'Condutor neutro', valor: mm2(r.secaoNeutroMm2) },
    { rotulo: 'Condutor terra (PE)', valor: mm2(r.secaoTerraMm2) },
    { rotulo: 'Rótulo do cabo', valor: r.rotuloCabo },
    { rotulo: 'Queda de tensão calculada', valor: pct(r.quedaCalculadaPct) },
  ];

  const fatores: ItemDefinicao[] = [
    { rotulo: 'Fator de agrupamento', valor: num(r.fatorAgrupamento, 2) },
    { rotulo: 'Fator de temperatura', valor: num(r.fatorTemperatura, 2) },
    { rotulo: 'Fator total combinado', valor: num(r.fatorTotal, 2) },
  ];

  return (
    <Stack gap="md">
      <Card>
        <Title order={2} fz="h4" mb="md">
          {titulo}
        </Title>
        <SimpleGrid cols={{ base: 2, sm: 4 }} spacing="sm">
          <StatTile rotulo="Corrente de projeto" valor={amp(r.correnteProjetoA)} />
          <StatTile rotulo="Corrente corrigida" valor={amp(r.correnteCorrigidaA)} />
          <StatTile rotulo="Disjuntor recomendado" valor={r.disjuntor.rotulo} />
          <StatTile rotulo="Seção do cabo" valor={mm2(r.secaoFinalMm2)} />
        </SimpleGrid>
        <Text size="sm" c="dimmed" mt="md">
          Potência aparente: {num(r.potenciaVA, 0)} VA · Cabo: {r.rotuloCabo}
        </Text>
      </Card>

      <Card>
        <Title order={2} fz="h4" mb="md">
          Dimensionamento dos Condutores
        </Title>
        <ListaDefinicao itens={condutores} />
      </Card>

      <Card>
        <Title order={2} fz="h4" mb="md">
          Fatores de Correção
        </Title>
        <ListaDefinicao itens={fatores} />
      </Card>
    </Stack>
  );
}
