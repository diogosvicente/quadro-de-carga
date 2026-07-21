import type { ReactNode } from 'react';

interface AlertaProps {
  children: ReactNode;
  aoTentarNovamente?: () => void;
}

/** Alerta de erro (mensagem do envelope da API ou falha de rede). */
export function Alerta({ children, aoTentarNovamente }: AlertaProps) {
  return (
    <div className="alerta" role="alert">
      <span>{children}</span>
      {aoTentarNovamente ? (
        <button type="button" className="botao-sec botao-mini" onClick={aoTentarNovamente}>
          Tentar novamente
        </button>
      ) : null}
    </div>
  );
}
