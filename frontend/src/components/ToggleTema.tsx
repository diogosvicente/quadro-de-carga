import { ActionIcon, useComputedColorScheme, useMantineColorScheme } from '@mantine/core';
import { IconMoon, IconSun } from '@tabler/icons-react';

/** Alterna claro/escuro a partir do esquema efetivo (respeita 'auto' do sistema). */
export function ToggleTema() {
  const { setColorScheme } = useMantineColorScheme();
  const computado = useComputedColorScheme('light', { getInitialValueInEffect: true });
  const escuro = computado === 'dark';

  return (
    <ActionIcon
      variant="default"
      size="lg"
      radius="md"
      aria-label={escuro ? 'Ativar tema claro' : 'Ativar tema escuro'}
      onClick={() => setColorScheme(escuro ? 'light' : 'dark')}
    >
      {escuro ? <IconSun size={18} stroke={1.6} /> : <IconMoon size={18} stroke={1.6} />}
    </ActionIcon>
  );
}
