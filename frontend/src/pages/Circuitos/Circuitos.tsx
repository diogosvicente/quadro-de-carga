import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
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
import { IconAlertTriangle, IconPlus } from '@tabler/icons-react';
import { listarCircuitos } from '../../api/circuitos';
import { obterReferencias } from '../../api/referencias';
import type { CircuitoResponse } from '../../types/circuito';
import type { TipoCircuito } from '../../types/comum';
import type { ReferenciasResponse } from '../../types/referencias';
import { mm2, num } from '../../utils/formato';

/** Lista de circuitos de um quadro. */
export function Circuitos() {
  const { id } = useParams();
  const quadroId = Number(id);
  const [circuitos, setCircuitos] = useState<CircuitoResponse[] | null>(null);
  const [referencias, setReferencias] = useState<ReferenciasResponse | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  const carregar = useCallback(async () => {
    try {
      setErro(null);
      const [lista, refs] = await Promise.all([listarCircuitos(quadroId), obterReferencias()]);
      setCircuitos(lista);
      setReferencias(refs);
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao carregar os circuitos.');
    }
  }, [quadroId]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  const rotuloTipo = (tipo: TipoCircuito): string =>
    referencias?.tiposCircuito.find((t) => t.codigo === tipo)?.rotulo ?? tipo;

  if (circuitos === null) {
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

  const ordenados = [...circuitos].sort((a, b) => a.numero - b.numero);

  return (
    <>
      <Group justify="space-between" align="flex-end" wrap="wrap" gap="sm" my="lg">
        <div>
          <Title order={1} fz={{ base: 'h2', sm: 'h1' }}>
            Circuitos Cadastrados
          </Title>
          <Text c="dimmed" size="sm" mt={4}>
            {circuitos.length === 1
              ? '1 circuito registrado'
              : `${circuitos.length} circuitos registrados`}
          </Text>
        </div>
        {circuitos.length > 0 ? (
          <Button component={Link} to={`/quadros/${quadroId}/novo`} leftSection={<IconPlus size={16} />}>
            Novo Circuito
          </Button>
        ) : null}
      </Group>

      {erro ? (
        <Alert color="red" icon={<IconAlertTriangle size={16} />} title="Erro" mb="md">
          <Group justify="space-between" wrap="wrap" gap="sm">
            <Text size="sm">{erro}</Text>
            <Button variant="light" color="red" size="xs" onClick={() => void carregar()}>
              Tentar novamente
            </Button>
          </Group>
        </Alert>
      ) : null}

      {ordenados.length === 0 ? (
        <Card padding="xl">
          <Stack align="center" gap="md">
            <Text c="dimmed">Nenhum circuito cadastrado neste quadro ainda.</Text>
            <Button
              component={Link}
              to={`/quadros/${quadroId}/novo`}
              leftSection={<IconPlus size={16} />}
            >
              Cadastrar o primeiro circuito
            </Button>
          </Stack>
        </Card>
      ) : (
        <Stack gap="md">
          {ordenados.map((c) => (
            <Link key={c.id} to={`/quadros/${quadroId}/circuitos/${c.id}`} className="link-cartao">
              <Card padding="lg">
                <Group justify="space-between" wrap="nowrap" gap="md" align="flex-start">
                  <div style={{ minWidth: 0 }}>
                    <Text fw={700} className="titulo-cartao">
                      Circuito {c.numero}
                      {c.descricao ? ` — ${c.descricao}` : ''}
                    </Text>
                    <Text size="sm" c="dimmed" mt={2} className="num-tab">
                      {c.tensaoV}V · {c.fases}P · {num(c.potenciaW, 0)}W · FP {num(c.fatorPotencia, 2)}
                    </Text>
                    <Badge variant="light" mt={8}>
                      {rotuloTipo(c.tipo)}
                    </Badge>
                  </div>
                  <Stack gap={2} align="flex-end" style={{ flexShrink: 0 }}>
                    <Text fw={600} size="sm" className="num-tab">
                      {c.resultado.disjuntor.rotulo}
                    </Text>
                    <Text size="sm" c="dimmed" className="num-tab">
                      {mm2(c.resultado.secaoFinalMm2)}
                    </Text>
                  </Stack>
                </Group>
              </Card>
            </Link>
          ))}
        </Stack>
      )}
    </>
  );
}
