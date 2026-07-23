import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Center,
  Paper,
  PasswordInput,
  Stack,
  Text,
  TextInput,
  ThemeIcon,
  Title,
} from '@mantine/core';
import { IconBolt } from '@tabler/icons-react';
import { ToggleTema } from '../../components/ToggleTema';
import { entrar } from '../../auth';

/**
 * Tela de entrada ilustrativa (pulável): os campos são decorativos e não validam
 * nada — o botão Entrar sempre funciona (login fake). A autenticação real fica
 * para depois; ver src/auth.ts.
 */
export function Login() {
  const navigate = useNavigate();
  const [usuario, setUsuario] = useState('');
  const [senha, setSenha] = useState('');

  const submeter = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    entrar(usuario || 'Convidado');
    navigate('/', { replace: true });
  };

  return (
    <Box pos="relative" mih="100vh">
      <Box pos="absolute" top={0} right={0} p="md" style={{ zIndex: 1 }}>
        <ToggleTema />
      </Box>
      <Center mih="100vh" p="md">
        <Paper withBorder shadow="md" radius="md" p="xl" w="100%" maw={400}>
          <Stack gap="lg">
            <Stack gap="xs" align="center">
              <ThemeIcon variant="light" radius="md" size={56}>
                <IconBolt size={32} />
              </ThemeIcon>
              <Stack gap={2} align="center">
                <Title order={1} fz="h3" ta="center">
                  Dimensionamento NBR 5410
                </Title>
                <Text c="dimmed" size="sm" ta="center">
                  Departamento de Engenharia Elétrica — UERJ
                </Text>
              </Stack>
            </Stack>

            <form onSubmit={submeter} noValidate>
              <Stack gap="md">
                <TextInput
                  label="Usuário"
                  placeholder="seu.usuario"
                  autoComplete="username"
                  value={usuario}
                  onChange={(e) => setUsuario(e.currentTarget.value)}
                  data-autofocus
                />
                <PasswordInput
                  label="Senha"
                  placeholder="••••••••"
                  autoComplete="current-password"
                  value={senha}
                  onChange={(e) => setSenha(e.currentTarget.value)}
                />
                <Button type="submit" fullWidth>
                  Entrar
                </Button>
              </Stack>
            </form>

            <Text c="dimmed" size="xs" ta="center">
              Login ilustrativo — a autenticação será desenvolvida futuramente. Clique em Entrar para
              acessar.
            </Text>
          </Stack>
        </Paper>
      </Center>
    </Box>
  );
}
