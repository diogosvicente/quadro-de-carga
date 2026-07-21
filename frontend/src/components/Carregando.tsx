interface CarregandoProps {
  texto?: string;
}

export function Carregando({ texto = 'Carregando…' }: CarregandoProps) {
  return (
    <p className="carregando" role="status">
      {texto}
    </p>
  );
}
