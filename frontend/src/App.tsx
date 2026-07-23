import { useCallback, useEffect, useState } from 'react';
import {
  BrowserRouter,
  Link,
  Navigate,
  Outlet,
  Route,
  Routes,
  useLocation,
  useNavigate,
  useParams,
} from 'react-router-dom';
import {
  Anchor,
  AppShell,
  Box,
  Breadcrumbs,
  Container,
  Group,
  SegmentedControl,
  Stack,
  Text,
  UnstyledButton,
} from '@mantine/core';
import { IconLayoutGrid, IconListDetails, IconPlus } from '@tabler/icons-react';
import type { Icon } from '@tabler/icons-react';
import { buscarQuadro } from './api/quadros';
import type { QuadroResponse } from './types/quadro';
import { estaAutenticado } from './auth';
import { AppHeader } from './components/AppHeader';
import { RequireAuth } from './components/RequireAuth';
import { Login } from './pages/Login/Login';
import { Quadros } from './pages/Quadros/Quadros';
import { NovoCircuito } from './pages/NovoCircuito/NovoCircuito';
import { Circuitos } from './pages/Circuitos/Circuitos';
import { DetalheCircuito } from './pages/DetalheCircuito/DetalheCircuito';
import { Resumo } from './pages/Resumo/Resumo';

/**
 * Layout das telas de um quadro: cabeçalho compartilhado (AppHeader) com a marca
 * de retorno à home, breadcrumb "Quadros / <nome>" e navegação Novo Circuito |
 * Circuitos | Quadro Elétrico — barra fixa embaixo no celular (alcance do polegar)
 * e SegmentedControl no topo em telas largas.
 */
export interface ContextoQuadro {
  /** Recarrega o quadro exibido no cabeçalho (ex.: após renomear nas configurações). */
  recarregarQuadro: () => void;
}

interface ItemNav {
  valor: string;
  rotulo: string;
  icone: Icon;
}

const ITENS_NAV: ItemNav[] = [
  { valor: 'novo', rotulo: 'Novo Circuito', icone: IconPlus },
  { valor: 'circuitos', rotulo: 'Circuitos', icone: IconListDetails },
  { valor: 'resumo', rotulo: 'Quadro Elétrico', icone: IconLayoutGrid },
];

function secaoAtual(pathname: string): string {
  if (pathname.includes('/novo')) return 'novo';
  if (pathname.includes('/resumo')) return 'resumo';
  return 'circuitos';
}

function QuadroLayout() {
  const { id } = useParams();
  const quadroId = Number(id);
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [quadro, setQuadro] = useState<QuadroResponse | null>(null);

  const recarregarQuadro = useCallback(() => {
    buscarQuadro(quadroId)
      .then((q) => setQuadro(q))
      .catch(() => {
        // As páginas internas exibem seus próprios erros; o cabeçalho degrada para o id.
      });
  }, [quadroId]);

  useEffect(() => {
    setQuadro(null);
    recarregarQuadro();
  }, [recarregarQuadro]);

  const secao = secaoAtual(pathname);
  const irPara = (valor: string) => navigate(`/quadros/${quadroId}/${valor}`);

  return (
    <AppShell header={{ height: 56 }} padding="md">
      <AppHeader
        navegacao={
          <SegmentedControl
            visibleFrom="sm"
            value={secao}
            onChange={irPara}
            data={ITENS_NAV.map((it) => ({ value: it.valor, label: it.rotulo }))}
          />
        }
      >
        <Breadcrumbs separator="/" style={{ minWidth: 0 }}>
          <Anchor component={Link} to="/?lista=1" fw={600} size="sm">
            Quadros
          </Anchor>
          <Text fw={700} size="sm" truncate maw={{ base: 150, sm: 340 }}>
            {quadro ? quadro.nome : `Quadro ${quadroId}`}
          </Text>
        </Breadcrumbs>
      </AppHeader>

      <AppShell.Main>
        <Container size="lg" px={0} pb={{ base: 72, sm: 'md' }}>
          <Outlet context={{ recarregarQuadro } satisfies ContextoQuadro} />
        </Container>

        <Box
          hiddenFrom="sm"
          className="barra-inferior"
          pos="fixed"
          bottom={0}
          left={0}
          right={0}
          bg="var(--mantine-color-body)"
          style={{
            zIndex: 200,
            borderTop: '1px solid var(--mantine-color-default-border)',
          }}
        >
          <Group gap={0} grow>
            {ITENS_NAV.map((it) => {
              const ativo = secao === it.valor;
              const Icone = it.icone;
              const cor = ativo ? 'var(--mantine-color-brand-6)' : 'var(--mantine-color-dimmed)';
              return (
                <UnstyledButton
                  key={it.valor}
                  onClick={() => irPara(it.valor)}
                  py={8}
                  style={{ borderTop: `2px solid ${ativo ? 'var(--mantine-color-brand-6)' : 'transparent'}` }}
                >
                  <Stack gap={2} align="center">
                    <Icone size={22} stroke={1.6} color={cor} />
                    <Text size="xs" fw={600} c={ativo ? 'brand.6' : 'dimmed'}>
                      {it.rotulo}
                    </Text>
                  </Stack>
                </UnstyledButton>
              );
            })}
          </Group>
        </Box>
      </AppShell.Main>
    </AppShell>
  );
}

/** Rota desconhecida: leva à home se autenticado, senão para o login. */
function RotaDesconhecida() {
  return <Navigate to={estaAutenticado() ? '/' : '/login'} replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route element={<RequireAuth />}>
          <Route path="/" element={<Quadros />} />
          <Route path="/quadros/:id" element={<QuadroLayout />}>
            <Route index element={<Navigate to="circuitos" replace />} />
            <Route path="novo" element={<NovoCircuito />} />
            <Route path="circuitos" element={<Circuitos />} />
            <Route path="circuitos/:cid" element={<DetalheCircuito />} />
            <Route path="circuitos/:cid/editar" element={<NovoCircuito />} />
            <Route path="resumo" element={<Resumo />} />
          </Route>
        </Route>
        <Route path="*" element={<RotaDesconhecida />} />
      </Routes>
    </BrowserRouter>
  );
}
