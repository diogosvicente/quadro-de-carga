import type { ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Avatar,
  AppShell,
  Container,
  Group,
  Menu,
  Text,
  ThemeIcon,
  UnstyledButton,
} from '@mantine/core';
import { IconBolt, IconLogout } from '@tabler/icons-react';
import { sair, usuarioAtual } from '../auth';
import { ToggleTema } from './ToggleTema';

/** Menu do usuário no canto direito: avatar + nome e a ação Sair (login fake). */
function MenuUsuario() {
  const navigate = useNavigate();
  const usuario = usuarioAtual() || 'Convidado';
  const inicial = usuario.trim().charAt(0).toUpperCase() || 'U';

  const sairDoApp = () => {
    sair();
    navigate('/login', { replace: true });
  };

  return (
    <Menu shadow="md" width={200} position="bottom-end">
      <Menu.Target>
        <UnstyledButton aria-label="Menu do usuário">
          <Group gap="xs" wrap="nowrap">
            <Avatar radius="xl" size={30} color="brand">
              {inicial}
            </Avatar>
            <Text size="sm" fw={600} visibleFrom="sm" maw={140} truncate>
              {usuario}
            </Text>
          </Group>
        </UnstyledButton>
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Label>{usuario}</Menu.Label>
        <Menu.Item leftSection={<IconLogout size={16} />} onClick={sairDoApp}>
          Sair
        </Menu.Item>
      </Menu.Dropdown>
    </Menu>
  );
}

interface AppHeaderProps {
  /**
   * Texto da marca ao lado do raio, dentro do link clicável que leva à home.
   * Usado na landing (nome do app); omitido dentro de um quadro (só o raio).
   */
  marca?: ReactNode;
  /** Conteúdo ao lado da marca, fora do link — ex.: o breadcrumb dentro de um quadro. */
  children?: ReactNode;
  /** Navegação à direita, antes do ToggleTema — ex.: o SegmentedControl de seções. */
  navegacao?: ReactNode;
}

/**
 * Cabeçalho compartilhado (altura 56) das telas do app: marca clicável (leva aos
 * Quadros = início) à esquerda, breadcrumb/navegação no meio e, à direita,
 * ToggleTema + menu do usuário. Usado tanto pela landing quanto pelo quadro.
 */
export function AppHeader({ marca, children, navegacao }: AppHeaderProps) {
  return (
    <AppShell.Header>
      <Container size="lg" h="100%">
        <Group h="100%" justify="space-between" wrap="nowrap" gap="sm">
          <Group gap="sm" wrap="nowrap" style={{ minWidth: 0 }}>
            <UnstyledButton
              component={Link}
              to="/"
              aria-label="Ir para os Quadros (início)"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 'var(--mantine-spacing-sm)',
                minWidth: 0,
              }}
            >
              <ThemeIcon variant="light" radius="md" size="lg">
                <IconBolt size={20} />
              </ThemeIcon>
              {marca ? (
                <Text fw={700} size="sm" truncate>
                  {marca}
                </Text>
              ) : null}
            </UnstyledButton>
            {children}
          </Group>
          <Group gap="xs" wrap="nowrap">
            {navegacao}
            <ToggleTema />
            <MenuUsuario />
          </Group>
        </Group>
      </Container>
    </AppShell.Header>
  );
}
