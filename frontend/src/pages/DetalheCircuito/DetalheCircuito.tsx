import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Alert,
  Badge,
  Button,
  Card,
  Center,
  Group,
  Loader,
  Stack,
  Text,
  Title,
} from '@mantine/core';
import { modals } from '@mantine/modals';
import { notifications } from '@mantine/notifications';
import { IconAlertTriangle, IconPencil, IconTrash } from '@tabler/icons-react';
import { buscarCircuito, removerCircuito } from '../../api/circuitos';
import { obterReferencias } from '../../api/referencias';
import {
  ListaDefinicao,
  ResultadoDimensionamento,
} from '../../components/ResultadoDimensionamento';
import type { ItemDefinicao } from '../../components/ResultadoDimensionamento';
import type { CircuitoResponse } from '../../types/circuito';
import type { ReferenciasResponse } from '../../types/referencias';
import { num, pct, watts } from '../../utils/formato';

/** Detalhe de um circuito: resultado do dimensionamento + dados de entrada. */
export function DetalheCircuito() {
  const { id, cid } = useParams();
  const quadroId = Number(id);
  const circuitoId = Number(cid);
  const navigate = useNavigate();

  const [circuito, setCircuito] = useState<CircuitoResponse | null>(null);
  const [referencias, setReferencias] = useState<ReferenciasResponse | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [excluindo, setExcluindo] = useState(false);

  const carregar = useCallback(async () => {
    try {
      setErro(null);
      const [c, refs] = await Promise.all([
        buscarCircuito(quadroId, circuitoId),
        obterReferencias(),
      ]);
      setCircuito(c);
      setReferencias(refs);
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao carregar o circuito.');
    }
  }, [quadroId, circuitoId]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  const confirmarExclusao = async () => {
    setExcluindo(true);
    try {
      await removerCircuito(quadroId, circuitoId);
      notifications.show({ message: 'Circuito excluído.', color: 'teal' });
      navigate(`/quadros/${quadroId}/circuitos`);
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao excluir o circuito.');
      setExcluindo(false);
    }
  };

  const excluir = () => {
    if (!circuito) return;
    modals.openConfirmModal({
      title: 'Excluir circuito',
      children: <Text size="sm">Excluir o circuito {circuito.numero}?</Text>,
      labels: { confirm: 'Excluir', cancel: 'Cancelar' },
      confirmProps: { color: 'red' },
      onConfirm: () => {
        void confirmarExclusao();
      },
    });
  };

  if (circuito === null || referencias === null) {
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

  const c = circuito;
  const rotuloTipo = referencias.tiposCircuito.find((t) => t.codigo === c.tipo)?.rotulo ?? c.tipo;
  const rotuloFases =
    referencias.fases.find((f) => f.valor === c.fases)?.rotulo ?? `${c.fases} fases`;
  const rotuloIsolante =
    referencias.isolantes.find((i) => i.codigo === c.isolante)?.rotulo ?? c.isolante;
  const metodo = referencias.metodosInstalacao.find((m) => m.codigo === c.metodoInstalacao);
  const rotuloMetodo = metodo ? `${metodo.codigo} — ${metodo.descricao}` : c.metodoInstalacao;
  const forma = referencias.formasAgrupamento.find((f) => f.ref === c.formaAgrupamentoRef);
  const rotuloForma = forma ? `${forma.ref} — ${forma.descricao}` : String(c.formaAgrupamentoRef);

  const entradas: ItemDefinicao[] = [
    { rotulo: 'Tipo de circuito', valor: rotuloTipo },
    { rotulo: 'Tensão (V)', valor: `${c.tensaoV} V` },
    { rotulo: 'Fases', valor: rotuloFases },
    { rotulo: 'Potência total (W)', valor: watts(c.potenciaW) },
    { rotulo: 'Fator de potência', valor: num(c.fatorPotencia, 2) },
    { rotulo: 'Comprimento do fio (m)', valor: `${num(c.comprimentoM)} m` },
    { rotulo: 'Circuitos agrupados', valor: num(c.circuitosAgrupados, 0) },
    { rotulo: 'Temperatura ambiente (°C)', valor: `${num(c.temperaturaC, 0)} °C` },
    { rotulo: 'Método de instalação', valor: rotuloMetodo },
    { rotulo: 'Isolante', valor: rotuloIsolante },
    { rotulo: 'Forma de agrupamento', valor: rotuloForma },
    { rotulo: 'Fator de demanda', valor: num(c.fatorDemanda, 2) },
    { rotulo: 'Queda admissível (%)', valor: pct(c.quedaAdmissivelPct) },
    { rotulo: 'Linha subterrânea', valor: c.linhaSubterranea ? 'Sim' : 'Não' },
  ];

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" wrap="wrap" gap="sm" mt="lg">
        <div style={{ minWidth: 0 }}>
          <Title order={1} fz={{ base: 'h2', sm: 'h1' }}>
            Circuito {c.numero}
            {c.descricao ? ` — ${c.descricao}` : ''}
          </Title>
          <Group gap="xs" mt="xs">
            <Badge variant="light">{rotuloTipo}</Badge>
            <Badge variant="light" color="gray">
              {c.tensaoV}V · {c.fases}P
            </Badge>
            <Badge variant="light" color="gray">
              {num(c.potenciaW, 0)}W
            </Badge>
          </Group>
        </div>
        <Group gap="xs">
          <Button
            variant="default"
            leftSection={<IconPencil size={16} />}
            onClick={() => navigate(`/quadros/${quadroId}/circuitos/${circuitoId}/editar`)}
          >
            Editar
          </Button>
          <Button
            variant="light"
            color="red"
            leftSection={<IconTrash size={16} />}
            onClick={excluir}
            loading={excluindo}
          >
            Excluir
          </Button>
        </Group>
      </Group>

      {erro ? (
        <Alert color="red" icon={<IconAlertTriangle size={16} />} title="Erro">
          {erro}
        </Alert>
      ) : null}

      <ResultadoDimensionamento resultado={c.resultado} />

      <Card>
        <Title order={2} fz="h4" mb="md">
          Dados de Entrada
        </Title>
        <ListaDefinicao itens={entradas} />
      </Card>
    </Stack>
  );
}
