import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../../api/client';
import {
  atualizarCircuito,
  buscarCircuito,
  calcularCircuito,
  criarCircuito,
} from '../../api/circuitos';
import { obterReferencias } from '../../api/referencias';
import { Alerta } from '../../components/Alerta';
import { Campo } from '../../components/Campo';
import { Carregando } from '../../components/Carregando';
import { ResultadoDimensionamento } from '../../components/ResultadoDimensionamento';
import type { CircuitoRequest, CircuitoResponse, ResultadoCircuito } from '../../types/circuito';
import type { Isolante, MetodoInstalacao, TipoCircuito } from '../../types/comum';
import type { ReferenciasResponse } from '../../types/referencias';
import { num } from '../../utils/formato';

interface FormCircuito {
  numero: string;
  descricao: string;
  tipo: TipoCircuito;
  tensaoV: string;
  fases: string;
  potenciaW: string;
  fatorPotencia: string;
  comprimentoM: string;
  circuitosAgrupados: string;
  temperaturaC: string;
  metodoInstalacao: MetodoInstalacao;
  isolante: Isolante;
  formaAgrupamentoRef: string;
  fatorDemanda: string;
  quedaAdmissivelPct: string;
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
  fatorPotencia: '0.92',
  comprimentoM: '',
  circuitosAgrupados: '1',
  temperaturaC: '30',
  metodoInstalacao: 'B1',
  isolante: 'PVC',
  formaAgrupamentoRef: '1',
  fatorDemanda: '1',
  quedaAdmissivelPct: '4',
  linhaSubterranea: false,
};

function deResposta(c: CircuitoResponse): FormCircuito {
  return {
    numero: String(c.numero),
    descricao: c.descricao ?? '',
    tipo: c.tipo,
    tensaoV: String(c.tensaoV),
    fases: String(c.fases),
    potenciaW: String(c.potenciaW),
    fatorPotencia: String(c.fatorPotencia),
    comprimentoM: String(c.comprimentoM),
    circuitosAgrupados: String(c.circuitosAgrupados),
    temperaturaC: String(c.temperaturaC),
    metodoInstalacao: c.metodoInstalacao,
    isolante: c.isolante,
    formaAgrupamentoRef: String(c.formaAgrupamentoRef),
    fatorDemanda: String(c.fatorDemanda),
    quedaAdmissivelPct: String(c.quedaAdmissivelPct),
    linhaSubterranea: c.linhaSubterranea,
  };
}

function paraNumero(texto: string): number {
  return Number(texto.trim().replace(',', '.'));
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
    const potenciaW = paraNumero(form.potenciaW);
    if (form.potenciaW.trim() === '' || Number.isNaN(potenciaW) || potenciaW <= 0) {
      locais.potenciaW = 'Informe a potência em watts (maior que zero).';
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
      .then((circuito) => navigate(`/quadros/${quadroId}/circuitos/${circuito.id}`))
      .catch((e: unknown) => {
        tratarErro(e);
        setOcupado(null);
      });
  };

  if (carregando) {
    return <Carregando texto="Carregando formulário…" />;
  }
  if (erroCarga !== null || referencias === null) {
    return <Alerta>{erroCarga ?? 'Falha ao carregar o formulário.'}</Alerta>;
  }

  return (
    <>
      <header className="pagina-cabeca">
        <div>
          <h1>{editando ? `Editar Circuito ${form.numero}` : 'Novo Circuito'}</h1>
          <p className="texto-mudo">
            Preencha os dados para dimensionamento conforme NBR 5410
          </p>
        </div>
      </header>

      {mensagem ? <Alerta>{mensagem}</Alerta> : null}

      <form onSubmit={salvar} noValidate>
        <fieldset className="cartao">
          <legend>Identificação</legend>
          <div className="form-grade">
            <Campo id="numero" rotulo="Número do Circuito *" erro={erros.numero}>
              <input
                id="numero"
                type="number"
                inputMode="numeric"
                step="1"
                min="1"
                placeholder="Ex: 1"
                value={form.numero}
                onChange={(e) => alterar('numero', e.target.value)}
              />
            </Campo>
            <Campo id="descricao" rotulo="Descrição" erro={erros.descricao}>
              <input
                id="descricao"
                type="text"
                placeholder="Ex: Iluminação Sala"
                maxLength={120}
                value={form.descricao}
                onChange={(e) => alterar('descricao', e.target.value)}
              />
            </Campo>
            <Campo id="tipo" rotulo="Tipo de Circuito *" erro={erros.tipo} cheio>
              <select
                id="tipo"
                value={form.tipo}
                onChange={(e) => alterar('tipo', e.target.value as TipoCircuito)}
              >
                {referencias.tiposCircuito.map((t) => (
                  <option key={t.codigo} value={t.codigo}>
                    {t.rotulo} (seção mín. {num(t.secaoMinimaMm2)} mm²)
                  </option>
                ))}
              </select>
            </Campo>
          </div>
        </fieldset>

        <fieldset className="cartao">
          <legend>Dados Elétricos</legend>
          <div className="form-grade">
            <Campo id="tensaoV" rotulo="Tensão (V) *" erro={erros.tensaoV}>
              <select
                id="tensaoV"
                value={form.tensaoV}
                onChange={(e) => alterar('tensaoV', e.target.value)}
              >
                {referencias.tensoes.map((t) => (
                  <option key={t} value={String(t)}>
                    {t} V
                  </option>
                ))}
              </select>
            </Campo>
            <Campo id="fases" rotulo="Fases *" erro={erros.fases}>
              <select
                id="fases"
                value={form.fases}
                onChange={(e) => alterar('fases', e.target.value)}
              >
                {referencias.fases.map((f) => (
                  <option key={f.valor} value={String(f.valor)}>
                    {f.rotulo}
                  </option>
                ))}
              </select>
            </Campo>
            <Campo id="potenciaW" rotulo="Potência Total (W) *" erro={erros.potenciaW}>
              <input
                id="potenciaW"
                type="number"
                inputMode="decimal"
                step="1"
                min="1"
                placeholder="Ex: 1200"
                value={form.potenciaW}
                onChange={(e) => alterar('potenciaW', e.target.value)}
              />
            </Campo>
            <Campo id="fatorPotencia" rotulo="Fator de Potência" erro={erros.fatorPotencia}>
              <input
                id="fatorPotencia"
                type="number"
                inputMode="decimal"
                step="0.01"
                min="0.01"
                max="1"
                value={form.fatorPotencia}
                onChange={(e) => alterar('fatorPotencia', e.target.value)}
              />
            </Campo>
            <Campo id="comprimentoM" rotulo="Comprimento do Fio (m) *" erro={erros.comprimentoM}>
              <input
                id="comprimentoM"
                type="number"
                inputMode="decimal"
                step="0.1"
                min="0.1"
                placeholder="Ex: 30"
                value={form.comprimentoM}
                onChange={(e) => alterar('comprimentoM', e.target.value)}
              />
            </Campo>
          </div>
        </fieldset>

        <fieldset className="cartao">
          <legend>Condições de Instalação</legend>
          <div className="form-grade">
            <Campo id="circuitosAgrupados" rotulo="Circuitos Agrupados" erro={erros.circuitosAgrupados}>
              <input
                id="circuitosAgrupados"
                type="number"
                inputMode="numeric"
                step="1"
                min="1"
                value={form.circuitosAgrupados}
                onChange={(e) => alterar('circuitosAgrupados', e.target.value)}
              />
            </Campo>
            <Campo id="temperaturaC" rotulo="Temperatura Ambiente (°C)" erro={erros.temperaturaC}>
              <input
                id="temperaturaC"
                type="number"
                inputMode="decimal"
                step="1"
                value={form.temperaturaC}
                onChange={(e) => alterar('temperaturaC', e.target.value)}
              />
            </Campo>
          </div>
        </fieldset>

        <details
          className="cartao avancado"
          open={
            ['metodoInstalacao', 'isolante', 'formaAgrupamentoRef', 'fatorDemanda', 'quedaAdmissivelPct', 'linhaSubterranea']
              .some((campo) => campo in erros) || undefined
          }
        >
          <summary>Condições Avançadas</summary>
          <div className="avancado-corpo">
            <div className="form-grade">
              <Campo id="metodoInstalacao" rotulo="Método de Instalação" erro={erros.metodoInstalacao}>
                <select
                  id="metodoInstalacao"
                  value={form.metodoInstalacao}
                  onChange={(e) => alterar('metodoInstalacao', e.target.value as MetodoInstalacao)}
                >
                  {referencias.metodosInstalacao.map((m) => (
                    <option key={m.codigo} value={m.codigo}>
                      {m.codigo} — {m.descricao}
                    </option>
                  ))}
                </select>
              </Campo>
              <Campo id="isolante" rotulo="Isolante" erro={erros.isolante}>
                <select
                  id="isolante"
                  value={form.isolante}
                  onChange={(e) => alterar('isolante', e.target.value as Isolante)}
                >
                  {referencias.isolantes.map((i) => (
                    <option key={i.codigo} value={i.codigo}>
                      {i.rotulo}
                    </option>
                  ))}
                </select>
              </Campo>
              <Campo
                id="formaAgrupamentoRef"
                rotulo="Forma de Agrupamento"
                erro={erros.formaAgrupamentoRef}
                cheio
              >
                <select
                  id="formaAgrupamentoRef"
                  value={form.formaAgrupamentoRef}
                  onChange={(e) => alterar('formaAgrupamentoRef', e.target.value)}
                >
                  {referencias.formasAgrupamento.map((f) => (
                    <option key={f.ref} value={String(f.ref)}>
                      {f.ref} — {f.descricao}
                    </option>
                  ))}
                </select>
              </Campo>
              <Campo id="fatorDemanda" rotulo="Fator de Demanda" erro={erros.fatorDemanda}>
                <input
                  id="fatorDemanda"
                  type="number"
                  inputMode="decimal"
                  step="0.01"
                  min="0.01"
                  max="1"
                  value={form.fatorDemanda}
                  onChange={(e) => alterar('fatorDemanda', e.target.value)}
                />
              </Campo>
              <Campo id="quedaAdmissivelPct" rotulo="Queda Admissível (%)" erro={erros.quedaAdmissivelPct}>
                <input
                  id="quedaAdmissivelPct"
                  type="number"
                  inputMode="decimal"
                  step="0.1"
                  min="0.1"
                  value={form.quedaAdmissivelPct}
                  onChange={(e) => alterar('quedaAdmissivelPct', e.target.value)}
                />
              </Campo>
              <div className="campo campo-check">
                <label htmlFor="linhaSubterranea">
                  <input
                    id="linhaSubterranea"
                    type="checkbox"
                    checked={form.linhaSubterranea}
                    onChange={(e) => alterar('linhaSubterranea', e.target.checked)}
                  />
                  Linha subterrânea
                </label>
                {erros.linhaSubterranea ? (
                  <p className="campo-msg" role="alert">
                    {erros.linhaSubterranea}
                  </p>
                ) : null}
              </div>
            </div>
          </div>
        </details>

        <div className="form-acoes">
          <button
            type="button"
            className="botao-sec"
            onClick={calcularPrevia}
            disabled={ocupado !== null}
          >
            {ocupado === 'previa' ? 'Calculando…' : 'Calcular Prévia'}
          </button>
          <button type="submit" className="botao" disabled={ocupado !== null}>
            {ocupado === 'salvar'
              ? 'Salvando…'
              : editando
                ? 'Salvar Alterações'
                : 'Cadastrar Circuito'}
          </button>
        </div>
      </form>

      {previa ? (
        <section className="previa" aria-live="polite">
          <ResultadoDimensionamento
            resultado={previa}
            titulo="Prévia do Dimensionamento (não salva)"
          />
        </section>
      ) : null}
    </>
  );
}
