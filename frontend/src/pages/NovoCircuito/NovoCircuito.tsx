import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Accordion,
  Alert,
  Button,
  Center,
  Fieldset,
  Group,
  Loader,
  NumberInput,
  SegmentedControl,
  Select,
  SimpleGrid,
  Stack,
  Switch,
  Text,
  TextInput,
  Title,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { IconAlertTriangle } from '@tabler/icons-react';
import { ApiError } from '../../api/client';
import {
  atualizarCircuito,
  buscarCircuito,
  calcularCircuito,
  criarCircuito,
} from '../../api/circuitos';
import { obterReferencias } from '../../api/referencias';
import { ResultadoDimensionamento } from '../../components/ResultadoDimensionamento';
import type { CircuitoRequest, CircuitoResponse, ResultadoCircuito } from '../../types/circuito';
import type { Isolante, MetodoInstalacao, TipoCircuito } from '../../types/comum';
import type { ReferenciasResponse } from '../../types/referencias';
import { num, watts } from '../../utils/formato';

interface FormCircuito {
  numero: string;
  descricao: string;
  tipo: TipoCircuito;
  tensaoV: string;
  fases: string;
  potenciaW: string;
  modoEntrada: 'potencia' | 'corrente';
  correnteA: string;
  fatorPotencia: string;
  comprimentoM: string;
  circuitosAgrupados: string;
  temperaturaC: string;
  metodoInstalacao: MetodoInstalacao;
  isolante: Isolante;
  formaAgrupamentoRef: string;
  fatorDemanda: string;
  quedaAdmissivelPct: string;
  circuitosParalelos: string;
  linhaSubterranea: boolean;
}

// Defaults do contrato (únicos valores fixos no código; opções vêm de /api/referencias).
const FORM_PADRAO: FormCircuito = {
  numero: '',
  descricao: '',
  tipo: 'FORCA',
  tensaoV: '127',
  fases: '1',
  potenciaW: '',
  modoEntrada: 'potencia',
  correnteA: '',
  fatorPotencia: '0.92',
  comprimentoM: '',
  circuitosAgrupados: '1',
  temperaturaC: '30',
  metodoInstalacao: 'B1',
  isolante: 'PVC',
  formaAgrupamentoRef: '1',
  fatorDemanda: '1',
  quedaAdmissivelPct: '4',
  circuitosParalelos: '1',
  linhaSubterranea: false,
};

const CAMPOS_AVANCADOS = [
  'metodoInstalacao',
  'isolante',
  'formaAgrupamentoRef',
  'fatorDemanda',
  'quedaAdmissivelPct',
  'circuitosParalelos',
  'linhaSubterranea',
];

function deResposta(c: CircuitoResponse): FormCircuito {
  return {
    numero: String(c.numero),
    descricao: c.descricao ?? '',
    tipo: c.tipo,
    tensaoV: String(c.tensaoV),
    fases: String(c.fases),
    potenciaW: String(c.potenciaW),
    modoEntrada: 'potencia',
    correnteA: '',
    fatorPotencia: String(c.fatorPotencia),
    comprimentoM: String(c.comprimentoM),
    circuitosAgrupados: String(c.circuitosAgrupados),
    temperaturaC: String(c.temperaturaC),
    metodoInstalacao: c.metodoInstalacao,
    isolante: c.isolante,
    formaAgrupamentoRef: String(c.formaAgrupamentoRef),
    fatorDemanda: String(c.fatorDemanda),
    quedaAdmissivelPct: String(c.quedaAdmissivelPct),
    circuitosParalelos: String(c.circuitosParalelos),
    linhaSubterranea: c.linhaSubterranea,
  };
}

function paraNumero(texto: string): number {
  return Number(texto.trim().replace(',', '.'));
}

const RAIZ_TRES = Math.sqrt(3);

/**
 * Deriva a potência (W) a partir da corrente informada, conforme o nº de fases:
 * monofásico/bifásico → P = V·I·FP; trifásico → P = √3·V·I·FP. Arredonda a 2 casas.
 */
function potenciaDeCorrente(
  tensaoV: number,
  correnteA: number,
  fatorPotencia: number,
  fases: number,
): number {
  const base = tensaoV * correnteA * fatorPotencia;
  const potencia = fases === 3 ? RAIZ_TRES * base : base;
  return Math.round(potencia * 100) / 100;
}

/** Formulário de circuito — criação ("/quadros/:id/novo") e edição ("…/:cid/editar"). */
export function NovoCircuito() {
  const { id, cid } = useParams();
  const quadroId = Number(id);
  const circuitoId = cid !== undefined ? Number(cid) : null;
  const editando = circuitoId !== null;
  const navigate = useNavigate();

  const [referencias, setReferencias] = useState<ReferenciasResponse | null>(null);
  const [form, setForm] = useState<FormCircuito>(FORM_PADRAO);
  const [carregando, setCarregando] = useState(true);
  const [erroCarga, setErroCarga] = useState<string | null>(null);
  const [erros, setErros] = useState<Record<string, string>>({});
  const [mensagem, setMensagem] = useState<string | null>(null);
  const [previa, setPrevia] = useState<ResultadoCircuito | null>(null);
  const [ocupado, setOcupado] = useState<'previa' | 'salvar' | null>(null);
  const [avancadoAberto, setAvancadoAberto] = useState<string | null>(null);

  useEffect(() => {
    let ativo = true;
    setCarregando(true);
    setErroCarga(null);
    Promise.all([
      obterReferencias(),
      circuitoId !== null
        ? buscarCircuito(quadroId, circuitoId)
        : Promise.resolve<CircuitoResponse | null>(null),
    ])
      .then(([refs, circuito]) => {
        if (!ativo) return;
        setReferencias(refs);
        // reset ao alternar entre editar ↔ novo: o componente é reaproveitado pelas duas rotas
        setForm(circuito ? deResposta(circuito) : FORM_PADRAO);
        setPrevia(null);
        setErros({});
        setMensagem(null);
        setCarregando(false);
      })
      .catch((e: unknown) => {
        if (!ativo) return;
        setErroCarga(e instanceof Error ? e.message : 'Falha ao carregar o formulário.');
        setCarregando(false);
      });
    return () => {
      ativo = false;
    };
  }, [quadroId, circuitoId]);

  const alterar = <K extends keyof FormCircuito>(campo: K, valor: FormCircuito[K]) => {
    setForm((atual) => ({ ...atual, [campo]: valor }) as FormCircuito);
  };

  const montar = (): CircuitoRequest | null => {
    const locais: Record<string, string> = {};
    const numero = paraNumero(form.numero);
    if (form.numero.trim() === '' || !Number.isInteger(numero) || numero <= 0) {
      locais.numero = 'Informe um número inteiro maior que zero.';
    }
    let potenciaW: number;
    if (form.modoEntrada === 'corrente') {
      const correnteA = paraNumero(form.correnteA);
      if (form.correnteA.trim() === '' || Number.isNaN(correnteA) || correnteA <= 0) {
        locais.correnteA = 'Informe a corrente em ampères (maior que zero).';
        potenciaW = Number.NaN;
      } else {
        potenciaW = potenciaDeCorrente(
          paraNumero(form.tensaoV),
          correnteA,
          paraNumero(form.fatorPotencia),
          paraNumero(form.fases),
        );
      }
    } else {
      potenciaW = paraNumero(form.potenciaW);
      if (form.potenciaW.trim() === '' || Number.isNaN(potenciaW) || potenciaW <= 0) {
        locais.potenciaW = 'Informe a potência em watts (maior que zero).';
      }
    }
    const comprimentoM = paraNumero(form.comprimentoM);
    if (form.comprimentoM.trim() === '' || Number.isNaN(comprimentoM) || comprimentoM <= 0) {
      locais.comprimentoM = 'Informe o comprimento em metros (maior que zero).';
    }
    const fatorPotencia = paraNumero(form.fatorPotencia);
    if (Number.isNaN(fatorPotencia) || fatorPotencia <= 0 || fatorPotencia > 1) {
      locais.fatorPotencia = 'Fator de potência deve estar entre 0 e 1.';
    }
    const circuitosAgrupados = paraNumero(form.circuitosAgrupados);
    if (!Number.isInteger(circuitosAgrupados) || circuitosAgrupados < 1) {
      locais.circuitosAgrupados = 'Informe um inteiro maior ou igual a 1.';
    }
    const temperaturaC = paraNumero(form.temperaturaC);
    if (form.temperaturaC.trim() === '' || Number.isNaN(temperaturaC)) {
      locais.temperaturaC = 'Informe a temperatura em °C.';
    }
    const fatorDemanda = paraNumero(form.fatorDemanda);
    if (Number.isNaN(fatorDemanda) || fatorDemanda <= 0 || fatorDemanda > 1) {
      locais.fatorDemanda = 'Fator de demanda deve estar entre 0 e 1.';
    }
    const quedaAdmissivelPct = paraNumero(form.quedaAdmissivelPct);
    if (Number.isNaN(quedaAdmissivelPct) || quedaAdmissivelPct <= 0) {
      locais.quedaAdmissivelPct = 'Informe a queda admissível em % (maior que zero).';
    }
    const circuitosParalelos = paraNumero(form.circuitosParalelos);
    if (!Number.isInteger(circuitosParalelos) || circuitosParalelos < 1) {
      locais.circuitosParalelos = 'Informe um inteiro maior ou igual a 1.';
    }
    if (Object.keys(locais).length > 0) {
      setErros(locais);
      setMensagem('Corrija os campos destacados.');
      return null;
    }
    setErros({});
    setMensagem(null);
    const descricao = form.descricao.trim();
    return {
      numero,
      descricao: descricao === '' ? undefined : descricao,
      tipo: form.tipo,
      tensaoV: paraNumero(form.tensaoV) as CircuitoRequest['tensaoV'],
      fases: paraNumero(form.fases) as CircuitoRequest['fases'],
      potenciaW,
      fatorPotencia,
      comprimentoM,
      circuitosAgrupados,
      temperaturaC,
      metodoInstalacao: form.metodoInstalacao,
      isolante: form.isolante,
      formaAgrupamentoRef: paraNumero(form.formaAgrupamentoRef),
      fatorDemanda,
      quedaAdmissivelPct,
      circuitosParalelos,
      linhaSubterranea: form.linhaSubterranea,
    };
  };

  const tratarErro = (e: unknown) => {
    if (e instanceof ApiError) {
      setMensagem(e.mensagem);
      setErros(e.erros ?? {});
    } else {
      setMensagem(e instanceof Error ? e.message : 'Erro inesperado.');
      setErros({});
    }
  };

  const calcularPrevia = () => {
    const corpo = montar();
    if (!corpo) return;
    setOcupado('previa');
    calcularCircuito(corpo)
      .then((resultado) => setPrevia(resultado))
      .catch((e: unknown) => {
        setPrevia(null);
        tratarErro(e);
      })
      .finally(() => setOcupado(null));
  };

  const salvar = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    const corpo = montar();
    if (!corpo) return;
    setOcupado('salvar');
    const chamada =
      circuitoId !== null
        ? atualizarCircuito(quadroId, circuitoId, corpo)
        : criarCircuito(quadroId, corpo);
    chamada
      .then((circuito) => {
        notifications.show({
          message: circuitoId !== null ? 'Circuito atualizado.' : 'Circuito cadastrado.',
          color: 'teal',
        });
        navigate(`/quadros/${quadroId}/circuitos/${circuito.id}`);
      })
      .catch((e: unknown) => {
        tratarErro(e);
        setOcupado(null);
      });
  };

  if (carregando) {
    return (
      <Center py="xl">
        <Loader />
      </Center>
    );
  }
  if (erroCarga !== null || referencias === null) {
    return (
      <Alert color="red" icon={<IconAlertTriangle size={16} />} title="Erro" mt="lg">
        {erroCarga ?? 'Falha ao carregar o formulário.'}
      </Alert>
    );
  }

  const tipos = referencias.tiposCircuito.map((t) => ({
    value: t.codigo,
    label: `${t.rotulo} (seção mín. ${num(t.secaoMinimaMm2)} mm²)`,
  }));
  const tensoes = referencias.tensoes.map((t) => ({ value: String(t), label: `${t} V` }));
  const fases = referencias.fases.map((f) => ({ value: String(f.valor), label: f.rotulo }));
  const metodos = referencias.metodosInstalacao.map((m) => ({
    value: m.codigo,
    label: `${m.codigo} — ${m.descricao}`,
  }));
  const isolantes = referencias.isolantes.map((i) => ({ value: i.codigo, label: i.rotulo }));
  const formas = referencias.formasAgrupamento.map((f) => ({
    value: String(f.ref),
    label: `${f.ref} — ${f.descricao}`,
  }));

  const temErroAvancado = CAMPOS_AVANCADOS.some((campo) => campo in erros);

  const correnteInformada = paraNumero(form.correnteA);
  const fatorPotenciaAtual = paraNumero(form.fatorPotencia);
  const potenciaDerivada =
    form.modoEntrada === 'corrente' &&
    form.correnteA.trim() !== '' &&
    Number.isFinite(correnteInformada) &&
    correnteInformada > 0 &&
    Number.isFinite(fatorPotenciaAtual)
      ? potenciaDeCorrente(
          paraNumero(form.tensaoV),
          correnteInformada,
          fatorPotenciaAtual,
          paraNumero(form.fases),
        )
      : null;

  return (
    <Stack gap="md">
      <div style={{ marginTop: 'var(--mantine-spacing-lg)' }}>
        <Title order={1} fz={{ base: 'h2', sm: 'h1' }}>
          {editando ? `Editar Circuito ${form.numero}` : 'Novo Circuito'}
        </Title>
        <Text c="dimmed" size="sm" mt={4}>
          Preencha os dados para dimensionamento conforme NBR 5410
        </Text>
      </div>

      {mensagem ? (
        <Alert color="red" icon={<IconAlertTriangle size={16} />} title="Erro">
          {mensagem}
        </Alert>
      ) : null}

      <form onSubmit={salvar} noValidate>
        <Stack gap="md">
          <Fieldset legend="Identificação" radius="md">
            <Stack gap="md">
              <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
                <NumberInput
                  label="Número do Circuito"
                  required
                  value={form.numero}
                  error={erros.numero}
                  placeholder="Ex: 1"
                  allowDecimal={false}
                  allowNegative={false}
                  onChange={(v) => alterar('numero', String(v))}
                />
                <TextInput
                  label="Descrição"
                  value={form.descricao}
                  error={erros.descricao}
                  placeholder="Ex: Iluminação Sala"
                  maxLength={120}
                  onChange={(e) => alterar('descricao', e.currentTarget.value)}
                />
              </SimpleGrid>
              <Select
                label="Tipo de Circuito"
                required
                data={tipos}
                value={form.tipo}
                error={erros.tipo}
                allowDeselect={false}
                onChange={(v) => {
                  if (v) alterar('tipo', v as TipoCircuito);
                }}
              />
            </Stack>
          </Fieldset>

          <Fieldset legend="Dados Elétricos" radius="md">
            <Stack gap="md">
              <SegmentedControl
                fullWidth
                aria-label="Modo de entrada dos dados elétricos"
                value={form.modoEntrada}
                data={[
                  { label: 'Potência (W)', value: 'potencia' },
                  { label: 'Corrente (A)', value: 'corrente' },
                ]}
                onChange={(v) => alterar('modoEntrada', v as FormCircuito['modoEntrada'])}
              />
              <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
                <Select
                  label="Tensão (V)"
                  required
                  data={tensoes}
                  value={form.tensaoV}
                  error={erros.tensaoV}
                  allowDeselect={false}
                  onChange={(v) => {
                    if (v) alterar('tensaoV', v);
                  }}
                />
                <Select
                  label="Fases"
                  required
                  data={fases}
                  value={form.fases}
                  error={erros.fases}
                  allowDeselect={false}
                  onChange={(v) => {
                    if (v) alterar('fases', v);
                  }}
                />
                {form.modoEntrada === 'corrente' ? (
                  <NumberInput
                    label="Corrente (A)"
                    required
                    value={form.correnteA}
                    error={erros.correnteA}
                    placeholder="Ex: 100"
                    suffix=" A"
                    min={0}
                    step={0.1}
                    allowNegative={false}
                    inputWrapperOrder={['label', 'input', 'description', 'error']}
                    description={
                      potenciaDerivada !== null ? `≈ ${watts(potenciaDerivada)}` : undefined
                    }
                    onChange={(v) => alterar('correnteA', String(v))}
                  />
                ) : (
                  <NumberInput
                    label="Potência Total (W)"
                    required
                    value={form.potenciaW}
                    error={erros.potenciaW}
                    placeholder="Ex: 1200"
                    suffix=" W"
                    step={1}
                    allowNegative={false}
                    onChange={(v) => alterar('potenciaW', String(v))}
                  />
                )}
                <NumberInput
                  label="Fator de Potência"
                  value={form.fatorPotencia}
                  error={erros.fatorPotencia}
                  step={0.01}
                  allowNegative={false}
                  onChange={(v) => alterar('fatorPotencia', String(v))}
                />
                <NumberInput
                  label="Comprimento do Fio (m)"
                  required
                  value={form.comprimentoM}
                  error={erros.comprimentoM}
                  placeholder="Ex: 30"
                  suffix=" m"
                  step={0.1}
                  allowNegative={false}
                  onChange={(v) => alterar('comprimentoM', String(v))}
                />
              </SimpleGrid>
            </Stack>
          </Fieldset>

          <Fieldset legend="Condições de Instalação" radius="md">
            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
              <NumberInput
                label="Circuitos Agrupados"
                value={form.circuitosAgrupados}
                error={erros.circuitosAgrupados}
                allowDecimal={false}
                allowNegative={false}
                onChange={(v) => alterar('circuitosAgrupados', String(v))}
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
          </Fieldset>

          <Accordion
            variant="separated"
            value={temErroAvancado ? 'avancado' : avancadoAberto}
            onChange={setAvancadoAberto}
          >
            <Accordion.Item value="avancado">
              <Accordion.Control>Condições Avançadas</Accordion.Control>
              <Accordion.Panel>
                <Stack gap="md">
                  <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
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
                    <Select
                      label="Isolante"
                      data={isolantes}
                      value={form.isolante}
                      error={erros.isolante}
                      allowDeselect={false}
                      onChange={(v) => {
                        if (v) alterar('isolante', v as Isolante);
                      }}
                    />
                  </SimpleGrid>
                  <Select
                    label="Forma de Agrupamento"
                    data={formas}
                    value={form.formaAgrupamentoRef}
                    error={erros.formaAgrupamentoRef}
                    allowDeselect={false}
                    onChange={(v) => {
                      if (v) alterar('formaAgrupamentoRef', v);
                    }}
                  />
                  <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
                    <NumberInput
                      label="Fator de Demanda"
                      value={form.fatorDemanda}
                      error={erros.fatorDemanda}
                      step={0.01}
                      allowNegative={false}
                      onChange={(v) => alterar('fatorDemanda', String(v))}
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
                      label="Circuitos em paralelo"
                      description="cabos em paralelo por fase"
                      value={form.circuitosParalelos}
                      error={erros.circuitosParalelos}
                      min={1}
                      step={1}
                      allowDecimal={false}
                      allowNegative={false}
                      onChange={(v) => alterar('circuitosParalelos', String(v))}
                    />
                  </SimpleGrid>
                  <Switch
                    label="Linha subterrânea"
                    checked={form.linhaSubterranea}
                    error={erros.linhaSubterranea}
                    onChange={(e) => alterar('linhaSubterranea', e.currentTarget.checked)}
                  />
                </Stack>
              </Accordion.Panel>
            </Accordion.Item>
          </Accordion>

          <Group gap="sm" grow>
            <Button variant="default" onClick={calcularPrevia} loading={ocupado === 'previa'}>
              Calcular Prévia
            </Button>
            <Button type="submit" loading={ocupado === 'salvar'}>
              {editando ? 'Salvar Alterações' : 'Cadastrar Circuito'}
            </Button>
          </Group>
        </Stack>
      </form>

      {previa ? (
        <div aria-live="polite">
          <ResultadoDimensionamento
            resultado={previa}
            titulo="Prévia do Dimensionamento (não salva)"
          />
        </div>
      ) : null}
    </Stack>
  );
}
