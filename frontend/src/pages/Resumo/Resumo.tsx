import { useCallback, useEffect, useState } from 'react';
import { Link, useOutletContext, useParams } from 'react-router-dom';
import {
  Alert,
  Anchor,
  Button,
  Card,
  Center,
  Group,
  Loader,
  SimpleGrid,
  Stack,
  Table,
  Text,
  Title,
} from '@mantine/core';
import { IconAlertTriangle, IconPlus, IconSettings } from '@tabler/icons-react';
import type { ContextoQuadro } from '../../App';
import { buscarResumoQuadro } from '../../api/quadros';
import { obterReferencias } from '../../api/referencias';
import { QuadroForm } from '../../components/QuadroForm';
import { StatTile } from '../../components/StatTile';
import type { TipoCircuito } from '../../types/comum';
import type { ResumoQuadroResponse } from '../../types/quadro';
import type { ReferenciasResponse } from '../../types/referencias';
import { amp, kva, kw, mm2, num, pct, watts } from '../../utils/formato';

/** Resumo do quadro: alimentador geral (tiles) + tabela dos circuitos. */
export function Resumo() {
  const { id } = useParams();
  const quadroId = Number(id);
  const { recarregarQuadro } = useOutletContext<ContextoQuadro>();
  const [resumo, setResumo] = useState<ResumoQuadroResponse | null>(null);
  const [referencias, setReferencias] = useState<ReferenciasResponse | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [editandoQuadro, setEditandoQuadro] = useState(false);

  const carregar = useCallback(async () => {
    try {
      setErro(null);
      const [r, refs] = await Promise.all([buscarResumoQuadro(quadroId), obterReferencias()]);
      setResumo(r);
      setReferencias(refs);
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao carregar o resumo.');
    }
  }, [quadroId]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  const rotuloTipo = (tipo: TipoCircuito): string =>
    referencias?.tiposCircuito.find((t) => t.codigo === tipo)?.rotulo ?? tipo;

  if (resumo === null) {
    return erro ? (
      <Alert color="red" icon={<IconAlertTriangle size={16} />} title="Erro" mt="lg">
        <Group justify="space-between" wrap="wrap" gap="sm">
          <Text size="sm">{erro}</Text>
          <Button variant="light" color="red" size="xs" onClick={() => void carregar()}>
            Tentar novamente
          </Button>
        </Group>
      </Alert>
    ) : (
      <Center py="xl">
        <Loader />
      </Center>
    );
  }

  const alimentador = resumo.alimentador;
  const circuitos = [...resumo.circuitos].sort((a, b) => a.numero - b.numero);

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-end" wrap="wrap" gap="sm" mt="lg">
        <div style={{ minWidth: 0 }}>
          <Title order={1} fz={{ base: 'h2', sm: 'h1' }}>
            Quadro Elétrico
          </Title>
          <Text c="dimmed" size="sm" mt={4}>
            {resumo.quadro.nome} — disjuntor geral e cabo alimentador (NBR 5410)
          </Text>
        </div>
        <Button
          variant="default"
          leftSection={<IconSettings size={16} />}
          onClick={() => setEditandoQuadro((atual) => !atual)}
        >
          {editandoQuadro ? 'Fechar Configurações' : 'Configurações do Alimentador'}
        </Button>
      </Group>

      {erro ? (
        <Alert color="red" icon={<IconAlertTriangle size={16} />} title="Erro">
          <Group justify="space-between" wrap="wrap" gap="sm">
            <Text size="sm">{erro}</Text>
            <Button variant="light" color="red" size="xs" onClick={() => void carregar()}>
              Tentar novamente
            </Button>
          </Group>
        </Alert>
      ) : null}

      {editandoQuadro && referencias ? (
        <QuadroForm
          key={`quadro-${resumo.quadro.id}`}
          inicial={resumo.quadro}
          referencias={referencias}
          aoSalvar={() => {
            setEditandoQuadro(false);
            void carregar();
            recarregarQuadro(); // cabeçalho pode exibir o nome antigo após renomear
          }}
          aoCancelar={() => setEditandoQuadro(false)}
        />
      ) : null}

      {alimentador === null ? (
        <Card padding="xl">
          <Stack align="center" gap="md">
            <Text c="dimmed">
              O quadro ainda não tem circuitos — cadastre o primeiro para ver o resumo.
            </Text>
            <Button
              component={Link}
              to={`/quadros/${quadroId}/novo`}
              leftSection={<IconPlus size={16} />}
            >
              Novo Circuito
            </Button>
          </Stack>
        </Card>
      ) : (
        <>
          <Card>
            <Title order={2} fz="h4" mb="md">
              Alimentador Geral
            </Title>
            <SimpleGrid cols={{ base: 2, sm: 3 }} spacing="sm">
              <StatTile
                rotulo="Corrente total"
                valor={amp(alimentador.correnteTotalA)}
                legenda={`calculada: ${amp(alimentador.correnteCalculadaA)}`}
              />
              <StatTile
                rotulo="Potência demandada"
                valor={kva(alimentador.cargaDemandadaVA)}
                legenda={`instalada: ${kw(alimentador.potenciaTotalW)}`}
              />
              <StatTile
                rotulo="Disjuntor geral"
                valor={alimentador.disjuntorGeral.rotulo}
                legenda={
                  alimentador.maiorDisjuntorCircuitoA !== null
                    ? `maior circuito: ${num(alimentador.maiorDisjuntorCircuitoA, 0)}A`
                    : undefined
                }
              />
              <StatTile
                rotulo="Cabo alimentador"
                valor={mm2(alimentador.secaoAlimentadorMm2)}
                legenda={
                  alimentador.maiorSecaoCircuitoMm2 !== null
                    ? `maior circuito: ${mm2(alimentador.maiorSecaoCircuitoMm2)}`
                    : undefined
                }
              />
              <StatTile
                rotulo="Queda de tensão calculada"
                valor={pct(alimentador.quedaCalculadaPct)}
                legenda={`admissível: ${pct(resumo.quadro.quedaAdmissivelPct)}`}
              />
              <StatTile
                rotulo="Capacidade do quadro"
                valor={kva(alimentador.capacidadeQuadroVA)}
                legenda={`${alimentador.tensaoV}V · ${alimentador.fases}P`}
              />
            </SimpleGrid>
            <Text size="sm" c="dimmed" mt="md">
              Alimentador: {alimentador.rotuloCabo} · Neutro {mm2(alimentador.secaoNeutroMm2)} ·
              Terra {mm2(alimentador.secaoTerraMm2)}
            </Text>
          </Card>

          <Card>
            <Title order={2} fz="h4" mb="md">
              Circuitos
            </Title>
            <Table.ScrollContainer minWidth={760}>
              <Table striped highlightOnHover verticalSpacing="xs" horizontalSpacing="md">
                <Table.Caption style={{ captionSide: 'bottom' }}>
                  {circuitos.length === 1 ? '1 circuito' : `${circuitos.length} circuitos`} · Total:{' '}
                  {kw(resumo.quadro.potenciaTotalW)}
                </Table.Caption>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Nº</Table.Th>
                    <Table.Th>Descrição</Table.Th>
                    <Table.Th>Tipo</Table.Th>
                    <Table.Th>Tensão</Table.Th>
                    <Table.Th ta="right">Potência</Table.Th>
                    <Table.Th ta="right">Ip (A)</Table.Th>
                    <Table.Th ta="right">Ic (A)</Table.Th>
                    <Table.Th>Disjuntor</Table.Th>
                    <Table.Th ta="right">Cabo</Table.Th>
                    <Table.Th ta="right">N / PE</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {circuitos.map((c) => (
                    <Table.Tr key={c.id}>
                      <Table.Td className="num-tab">{c.numero}</Table.Td>
                      <Table.Td>
                        <Anchor component={Link} to={`/quadros/${quadroId}/circuitos/${c.id}`} size="sm">
                          {c.descricao ?? `Circuito ${c.numero}`}
                        </Anchor>
                      </Table.Td>
                      <Table.Td>{rotuloTipo(c.tipo)}</Table.Td>
                      <Table.Td className="num-tab">
                        {c.tensaoV}V · {c.fases}P
                      </Table.Td>
                      <Table.Td ta="right" className="num-tab">
                        {watts(c.potenciaW)}
                      </Table.Td>
                      <Table.Td ta="right" className="num-tab">
                        {num(c.resultado.correnteProjetoA, 1)}
                      </Table.Td>
                      <Table.Td ta="right" className="num-tab">
                        {num(c.resultado.correnteCorrigidaA, 1)}
                      </Table.Td>
                      <Table.Td className="num-tab">{c.resultado.disjuntor.rotulo}</Table.Td>
                      <Table.Td ta="right" className="num-tab">
                        {mm2(c.resultado.secaoFinalMm2)}
                      </Table.Td>
                      <Table.Td ta="right" className="num-tab">
                        {num(c.resultado.secaoNeutroMm2)}/{num(c.resultado.secaoTerraMm2)} mm²
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          </Card>
        </>
      )}
    </Stack>
  );
}
