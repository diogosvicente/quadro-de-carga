import { useCallback, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import {
  Alert,
  AppShell,
  Badge,
  Box,
  Button,
  Card,
  Center,
  Container,
  Divider,
  Group,
  Loader,
  SimpleGrid,
  Stack,
  Text,
  Title,
} from '@mantine/core';
import { modals } from '@mantine/modals';
import { notifications } from '@mantine/notifications';
import { IconAlertTriangle, IconPlus } from '@tabler/icons-react';
import { listarQuadros, removerQuadro } from '../../api/quadros';
import { obterReferencias } from '../../api/referencias';
import { AppHeader } from '../../components/AppHeader';
import { QuadroForm } from '../../components/QuadroForm';
import type { QuadroResponse } from '../../types/quadro';
import type { ReferenciasResponse } from '../../types/referencias';
import { kw } from '../../utils/formato';

/** Lista de quadros de distribuição + criar/editar/excluir quadro. */
export function Quadros() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [quadros, setQuadros] = useState<QuadroResponse[] | null>(null);
  const [referencias, setReferencias] = useState<ReferenciasResponse | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [formAberto, setFormAberto] = useState<'novo' | number | null>(null);

  const carregar = useCallback(async () => {
    try {
      setErro(null);
      const [listaQuadros, refs] = await Promise.all([listarQuadros(), obterReferencias()]);
      setQuadros(listaQuadros);
      setReferencias(refs);
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao carregar os quadros.');
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  const confirmarExclusao = async (quadro: QuadroResponse) => {
    try {
      await removerQuadro(quadro.id);
      notifications.show({ message: `Quadro "${quadro.nome}" excluído.`, color: 'teal' });
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao excluir o quadro.');
    }
  };

  const excluir = (quadro: QuadroResponse) => {
    modals.openConfirmModal({
      title: 'Excluir quadro',
      children: (
        <Text size="sm">
          Excluir o quadro <strong>{quadro.nome}</strong> e todos os seus circuitos?
        </Text>
      ),
      labels: { confirm: 'Excluir', cancel: 'Cancelar' },
      confirmProps: { color: 'red' },
      onConfirm: () => {
        void confirmarExclusao(quadro);
      },
    });
  };

  const rotuloSistema = (quadro: QuadroResponse): string =>
    referencias?.sistemasTensao.find((s) => s.codigo === quadro.sistemaTensao)?.rotulo ??
    quadro.sistemaTensao;

  const alertaErro = (mensagem: string) => (
    <Alert color="red" icon={<IconAlertTriangle size={16} />} title="Erro" mb="md">
      <Group justify="space-between" wrap="wrap" gap="sm">
        <Text size="sm">{mensagem}</Text>
        <Button variant="light" color="red" size="xs" onClick={() => void carregar()}>
          Tentar novamente
        </Button>
      </Group>
    </Alert>
  );

  // Usuário típico tem um único quadro: entra direto nele.
  // O link "Quadros" do layout usa /?lista=1 para não redirecionar de volta.
  if (
    quadros !== null &&
    quadros.length === 1 &&
    !params.has('lista') &&
    formAberto === null &&
    erro === null
  ) {
    return <Navigate to={`/quadros/${quadros[0].id}/circuitos`} replace />;
  }

  let corpo: ReactNode;
  if (quadros === null) {
    corpo = erro ? (
      alertaErro(erro)
    ) : (
      <Center py="xl">
        <Loader />
      </Center>
    );
  } else {
    corpo = (
      <>
        {erro ? alertaErro(erro) : null}
        {formAberto !== null && referencias ? (
          <Box mb="md">
            <QuadroForm
              key={formAberto === 'novo' ? 'novo' : `quadro-${formAberto}`}
              variante="simples"
              inicial={
                typeof formAberto === 'number'
                  ? quadros.find((q) => q.id === formAberto)
                  : undefined
              }
              referencias={referencias}
              aoSalvar={(quadro) => {
                if (formAberto === 'novo') {
                  navigate(`/quadros/${quadro.id}/circuitos`);
                  return;
                }
                setFormAberto(null);
                void carregar();
              }}
              aoCancelar={() => setFormAberto(null)}
            />
          </Box>
        ) : null}
        {quadros.length === 0 ? (
          formAberto === null ? (
            <Card padding="xl">
              <Stack align="center" gap="md">
                <Text c="dimmed">Nenhum quadro de distribuição cadastrado.</Text>
                <Button leftSection={<IconPlus size={16} />} onClick={() => setFormAberto('novo')}>
                  Criar o primeiro quadro
                </Button>
              </Stack>
            </Card>
          ) : null
        ) : (
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
            {quadros.map((quadro) => (
              <Card key={quadro.id} padding="lg">
                <Link to={`/quadros/${quadro.id}/circuitos`} className="link-cartao">
                  <Group justify="space-between" wrap="nowrap" gap="sm" align="flex-start">
                    <Text fw={700} fz="lg" className="titulo-cartao">
                      {quadro.nome}
                    </Text>
                    <Badge variant="light" style={{ flexShrink: 0 }}>
                      {rotuloSistema(quadro)}
                    </Badge>
                  </Group>
                  {quadro.local ? (
                    <Text size="sm" c="dimmed" mt={2}>
                      {quadro.local}
                    </Text>
                  ) : null}
                  <Text size="sm" c="dimmed" mt={6}>
                    {quadro.totalCircuitos === 1
                      ? '1 circuito'
                      : `${quadro.totalCircuitos} circuitos`}{' '}
                    · {kw(quadro.potenciaTotalW)}
                  </Text>
                </Link>
                <Divider my="md" />
                <Group gap="xs">
                  <Button variant="light" size="xs" onClick={() => setFormAberto(quadro.id)}>
                    Editar
                  </Button>
                  <Button
                    variant="light"
                    color="red"
                    size="xs"
                    onClick={() => excluir(quadro)}
                  >
                    Excluir
                  </Button>
                </Group>
              </Card>
            ))}
          </SimpleGrid>
        )}
      </>
    );
  }

  return (
    <AppShell header={{ height: 56 }} padding="md">
      <AppHeader marca="Dimensionamento NBR 5410 — UERJ" />

      <AppShell.Main>
        <Container size="lg" px={0}>
          <Group justify="space-between" align="flex-end" wrap="wrap" gap="sm" my="lg">
            <div>
              <Title order={1} fz={{ base: 'h2', sm: 'h1' }}>
                Quadros de Distribuição
              </Title>
              <Text c="dimmed" size="sm" mt={4}>
                Dimensionamento de circuitos elétricos conforme NBR 5410
              </Text>
            </div>
            {quadros !== null && quadros.length > 0 && formAberto === null ? (
              <Button leftSection={<IconPlus size={16} />} onClick={() => setFormAberto('novo')}>
                Novo Quadro
              </Button>
            ) : null}
          </Group>
          {corpo}
        </Container>
      </AppShell.Main>
    </AppShell>
  );
}
