import { useState } from 'react';
import type { FormEvent } from 'react';
import {
  Alert,
  Button,
  Card,
  Group,
  NumberInput,
  Select,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { IconAlertTriangle } from '@tabler/icons-react';
import { ApiError } from '../api/client';
import { atualizarQuadro, criarQuadro } from '../api/quadros';
import type { MetodoInstalacao, SistemaTensao } from '../types/comum';
import type { QuadroRequest, QuadroResponse } from '../types/quadro';
import type { ReferenciasResponse } from '../types/referencias';

interface FormQuadro {
  nome: string;
  local: string;
  sistemaTensao: SistemaTensao;
  fasesAlimentador: string;
  fatorDemanda: string;
  cargaReservaVA: string;
  comprimentoM: string;
  metodoInstalacao: MetodoInstalacao;
  quedaAdmissivelPct: string;
  temperaturaC: string;
}

// Defaults do contrato: V127_220, 3, 1, 0, 10, "B1", 2, 30.
const FORM_PADRAO: FormQuadro = {
  nome: '',
  local: '',
  sistemaTensao: 'V127_220',
  fasesAlimentador: '3',
  fatorDemanda: '1',
  cargaReservaVA: '0',
  comprimentoM: '10',
  metodoInstalacao: 'B1',
  quedaAdmissivelPct: '2',
  temperaturaC: '30',
};

function deResposta(quadro: QuadroResponse): FormQuadro {
  return {
    nome: quadro.nome,
    local: quadro.local ?? '',
    sistemaTensao: quadro.sistemaTensao,
    fasesAlimentador: String(quadro.fasesAlimentador),
    fatorDemanda: String(quadro.fatorDemanda),
    cargaReservaVA: String(quadro.cargaReservaVA),
    comprimentoM: String(quadro.comprimentoM),
    metodoInstalacao: quadro.metodoInstalacao,
    quedaAdmissivelPct: String(quadro.quedaAdmissivelPct),
    temperaturaC: String(quadro.temperaturaC),
  };
}

function paraNumero(texto: string): number {
  return Number(texto.trim().replace(',', '.'));
}

interface QuadroFormProps {
  /** Quando presente, o formulário edita (PUT); caso contrário, cria (POST). */
  inicial?: QuadroResponse;
  referencias: ReferenciasResponse;
  aoSalvar: (quadro: QuadroResponse) => void;
  aoCancelar: () => void;
  /**
   * 'simples' mostra só identidade (nome/local) — os parâmetros do alimentador
   * ficam com valores padrão e são ajustados depois na tela do Quadro Elétrico.
   * 'completo' mostra tudo (usado ao ajustar o alimentador no Quadro Elétrico).
   */
  variante?: 'completo' | 'simples';
}

/** Formulário de quadro (alimentador geral) — criação e edição. */
export function QuadroForm({
  inicial,
  referencias,
  aoSalvar,
  aoCancelar,
  variante = 'completo',
}: QuadroFormProps) {
  const [form, setForm] = useState<FormQuadro>(() => (inicial ? deResposta(inicial) : FORM_PADRAO));
  const [erros, setErros] = useState<Record<string, string>>({});
  const [mensagem, setMensagem] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  const alterar = <K extends keyof FormQuadro>(campo: K, valor: FormQuadro[K]) => {
    setForm((atual) => ({ ...atual, [campo]: valor }) as FormQuadro);
  };

  const montar = (): QuadroRequest | null => {
    const locais: Record<string, string> = {};
    const nome = form.nome.trim();
    if (nome === '') locais.nome = 'Informe o nome do quadro.';
    const fatorDemanda = paraNumero(form.fatorDemanda);
    if (Number.isNaN(fatorDemanda) || fatorDemanda <= 0 || fatorDemanda > 1) {
      locais.fatorDemanda = 'Fator de demanda deve estar entre 0 e 1.';
    }
    const cargaReservaVA = paraNumero(form.cargaReservaVA);
    if (Number.isNaN(cargaReservaVA) || cargaReservaVA < 0) {
      locais.cargaReservaVA = 'Informe a carga de reserva em VA (0 ou mais).';
    }
    const comprimentoM = paraNumero(form.comprimentoM);
    if (Number.isNaN(comprimentoM) || comprimentoM <= 0) {
      locais.comprimentoM = 'Informe o comprimento em metros (maior que zero).';
    }
    const quedaAdmissivelPct = paraNumero(form.quedaAdmissivelPct);
    if (Number.isNaN(quedaAdmissivelPct) || quedaAdmissivelPct <= 0) {
      locais.quedaAdmissivelPct = 'Informe a queda admissível em % (maior que zero).';
    }
    const temperaturaC = paraNumero(form.temperaturaC);
    if (Number.isNaN(temperaturaC)) {
      locais.temperaturaC = 'Informe a temperatura em °C.';
    }
    if (Object.keys(locais).length > 0) {
      setErros(locais);
      setMensagem('Corrija os campos destacados.');
      return null;
    }
    setErros({});
    setMensagem(null);
    const local = form.local.trim();
    return {
      nome,
      local: local === '' ? undefined : local,
      sistemaTensao: form.sistemaTensao,
      fasesAlimentador: paraNumero(form.fasesAlimentador) as QuadroRequest['fasesAlimentador'],
      fatorDemanda,
      cargaReservaVA,
      comprimentoM,
      metodoInstalacao: form.metodoInstalacao,
      quedaAdmissivelPct,
      temperaturaC,
    };
  };

  const enviar = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    const corpo = montar();
    if (!corpo) return;
    setSalvando(true);
    const chamada = inicial ? atualizarQuadro(inicial.id, corpo) : criarQuadro(corpo);
    chamada
      .then((quadro) => {
        notifications.show({
          message: inicial ? 'Quadro atualizado.' : 'Quadro criado.',
          color: 'teal',
        });
        aoSalvar(quadro);
      })
      .catch((erro: unknown) => {
        if (erro instanceof ApiError) {
          setMensagem(erro.mensagem);
          setErros(erro.erros ?? {});
        } else {
          setMensagem(erro instanceof Error ? erro.message : 'Erro inesperado ao salvar.');
          setErros({});
        }
        setSalvando(false);
      });
  };

  const sistemas = referencias.sistemasTensao.map((s) => ({ value: s.codigo, label: s.rotulo }));
  const fases = referencias.fases.map((f) => ({ value: String(f.valor), label: f.rotulo }));
  const metodos = referencias.metodosInstalacao.map((m) => ({
    value: m.codigo,
    label: `${m.codigo} — ${m.descricao}`,
  }));

  return (
    <Card component="section">
      <Title order={2} fz="h4" mb="md">
        {inicial ? 'Editar Quadro' : 'Novo Quadro'}
      </Title>
      {mensagem ? (
        <Alert color="red" icon={<IconAlertTriangle size={16} />} mb="md">
          {mensagem}
        </Alert>
      ) : null}
      <form onSubmit={enviar} noValidate>
        <Stack gap="md">
          <TextInput
            label="Nome do Quadro"
            required
            value={form.nome}
            error={erros.nome}
            placeholder="Ex: QDC Bloco D"
            maxLength={120}
            onChange={(e) => alterar('nome', e.currentTarget.value)}
          />
          <TextInput
            label="Local"
            value={form.local}
            error={erros.local}
            placeholder="Ex: Dept. Engenharia Elétrica"
            maxLength={120}
            onChange={(e) => alterar('local', e.currentTarget.value)}
          />
          {variante === 'completo' ? (
            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
              <Select
                label="Sistema de Tensão"
                data={sistemas}
                value={form.sistemaTensao}
                error={erros.sistemaTensao}
                allowDeselect={false}
                onChange={(v) => {
                  if (v) alterar('sistemaTensao', v as SistemaTensao);
                }}
              />
              <Select
                label="Fases do Alimentador"
                data={fases}
                value={form.fasesAlimentador}
                error={erros.fasesAlimentador}
                allowDeselect={false}
                onChange={(v) => {
                  if (v) alterar('fasesAlimentador', v);
                }}
              />
              <NumberInput
                label="Fator de Demanda"
                value={form.fatorDemanda}
                error={erros.fatorDemanda}
                step={0.01}
                allowNegative={false}
                onChange={(v) => alterar('fatorDemanda', String(v))}
              />
              <NumberInput
                label="Carga de Reserva (VA)"
                value={form.cargaReservaVA}
                error={erros.cargaReservaVA}
                suffix=" VA"
                step={1}
                allowNegative={false}
                onChange={(v) => alterar('cargaReservaVA', String(v))}
              />
              <NumberInput
                label="Comprimento do Alimentador (m)"
                value={form.comprimentoM}
                error={erros.comprimentoM}
                suffix=" m"
                step={0.1}
                allowNegative={false}
                onChange={(v) => alterar('comprimentoM', String(v))}
              />
              <Select
                label="Método de Instalação"
                data={metodos}
                value={form.metodoInstalacao}
                error={erros.metodoInstalacao}
                allowDeselect={false}
                onChange={(v) => {
                  if (v) alterar('metodoInstalacao', v as MetodoInstalacao);
                }}
              />
              <NumberInput
                label="Queda Admissível (%)"
                value={form.quedaAdmissivelPct}
                error={erros.quedaAdmissivelPct}
                suffix=" %"
                step={0.1}
                allowNegative={false}
                onChange={(v) => alterar('quedaAdmissivelPct', String(v))}
              />
              <NumberInput
                label="Temperatura Ambiente (°C)"
                value={form.temperaturaC}
                error={erros.temperaturaC}
                suffix=" °C"
                step={1}
                onChange={(v) => alterar('temperaturaC', String(v))}
              />
            </SimpleGrid>
          ) : null}
          {variante === 'simples' ? (
            <Text size="sm" c="dimmed">
              Os parâmetros do alimentador (tensão, comprimento, método, temperatura…) você ajusta
              depois na tela do <strong>Quadro Elétrico</strong>.
            </Text>
          ) : null}
        </Stack>
        <Group justify="flex-end" mt="lg">
          <Button variant="default" onClick={aoCancelar} disabled={salvando}>
            Cancelar
          </Button>
          <Button type="submit" loading={salvando}>
            {inicial ? 'Salvar Alterações' : 'Criar Quadro'}
          </Button>
        </Group>
      </form>
    </Card>
  );
}
