# 04: Motor: Marcadores e preparação do texto do SMS

**Depende de:** 02
**Spec:** §5.5, §5.6 (corte do SMS)

## Escopo (TDD)
- Substituição de `{app}`, `{titulo}`, `{texto}`, `{data}` e `{hora}`. Marcadores desconhecidos ficam como estão; valores ausentes viram string vazia.
- Contexto de exemplo para o Teste.
- Corte do SMS em `limiteCaracteres`, terminando com "…".

## Critérios de aceite
- [ ] `"{app}: {titulo} - {texto} ({data} {hora})"` é renderizado corretamente.
- [ ] `{foo}` permanece literal.
- [ ] Um texto de 400 caracteres com limite 300 vira exatamente 300 caracteres, terminando em "…".
