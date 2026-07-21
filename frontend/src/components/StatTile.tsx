interface StatTileProps {
  rotulo: string;
  valor: string;
  legenda?: string;
}

/**
 * Cartão de estatística (KPI): rótulo em caixa de frase, valor semibold em
 * algarismos proporcionais e legenda opcional em tinta atenuada.
 */
export function StatTile({ rotulo, valor, legenda }: StatTileProps) {
  return (
    <div className="stat-tile">
      <div className="stat-rotulo">{rotulo}</div>
      <div className="stat-valor">{valor}</div>
      {legenda ? <div className="stat-legenda">{legenda}</div> : null}
    </div>
  );
}
