import type { ReactNode } from 'react';

interface CampoProps {
  /** id do controle interno — o label usa htmlFor com este id. */
  id: string;
  rotulo: string;
  erro?: string;
  cheio?: boolean;
  children: ReactNode;
}

/** Rótulo + controle + mensagem de erro do envelope da API. */
export function Campo({ id, rotulo, erro, cheio = false, children }: CampoProps) {
  const classes = ['campo'];
  if (erro) classes.push('campo-com-erro');
  if (cheio) classes.push('campo-cheio');
  return (
    <div className={classes.join(' ')}>
      <label htmlFor={id}>{rotulo}</label>
      {children}
      {erro ? (
        <p className="campo-msg" role="alert">
          {erro}
        </p>
      ) : null}
    </div>
  );
}
