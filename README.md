# SRooms 1.1.0

Plugin de **rooms PvP 1v1** para **Paper 1.20.1** y Java 17. Sin dependencias externas.

## Instalación

1. Compila con \`mvn clean package\` (o descarga el JAR generado en GitHub Actions).
2. Copia \`target/SRooms-1.1.0.jar\` a \`plugins/\`.
3. Reinicia Paper 1.20.1.
4. Edita \`plugins/SRooms/config.yml\`, \`messages.yml\` o las rooms desde el menú si lo necesitas.

## Comandos (único comando base: /srooms)

| Comando | Permiso | Uso |
|---|---|---|
| \`/srooms help\` | srooms.use | Ayuda |
| \`/srooms list\` | srooms.use | Lista ocupación (0/2, 1/2, 2/2) y estado |
| \`/srooms join <room>\` | srooms.use | Entrar a una arena |
| \`/srooms leave\` | srooms.use | Salir / abandonar combate |
| \`/srooms info <room>\` | srooms.use | Ver room, keep, tiempo de reapertura |
| \`/srooms createroom\` | srooms.admin | Iniciar selección de regiones |
| \`/srooms nameroom <nombre>\` | srooms.admin | Guardar la selección completa |
| \`/srooms roomconfig <nombre>\` | srooms.admin | Menú gráfico de ajustes |
| \`/srooms deleteroom <nombre>\` | srooms.admin | Eliminar la room vacía |
| \`/srooms reopen <nombre> <0-300>\` | srooms.admin | Ajustar segundos de reapertura |
| \`/srooms cancel\` | srooms.admin | Cancelar creación |

Los comandos antiguos (\`/createroom\`, \`/roomconfig\`, \`/room join\`...) fueron sustituidos por los subcomandos de \`/srooms\`.

## Creación de rooms

1. Ejecuta \`/srooms createroom\`.
2. Con el hacha inicial, **clic izquierdo = primer punto**, **clic derecho = segundo punto** de la **zona A**.
3. Repite para la **zona B**.
4. Con la segunda hacha, selecciona las dos esquinas del **cristal de cierre**.
5. Las hachas se retiran al terminar: escribe \`/srooms nameroom <nombre>\`.
6. En \`/srooms roomconfig <nombre>\` configura inventario, experiencia, efectos, hambre, construcción, preparación, **reapertura**, spawn A y spawn B.

Si no hay suelo seguro en el centro de una zona, marca manualmente su spawn desde el menú mientras estés dentro de ella. No coloques spawns en la barrera.

## Combates y seguridad

- Cada room tiene **máximo dos participantes**. El primero ocupa A; el segundo, B.
- No se admite un tercero aunque se intente usar \`/srooms join\`, caminar o teletransportarse dentro de una arena ocupada. Se rechaza a quien entra último, **sin expulsarlo del servidor** ni sacar a los participantes.
- La barrera se cierra antes del combate; se abre cuando termina la cuenta regresiva.
- El ganador se determina por muerte o abandono, con mensaje de resultado global, título de victoria y derrota, y nombres de ambos jugadores.
- **Después del resultado**, se vuelve a cerrar la barrera; la room espera el tiempo configurado antes de restaurarla y admitir a otros dos jugadores. Predeterminado: **10 segundos**, configurable de **0 a 300** por arena.
- Las rooms que están ocupadas o en reapertura no pueden modificarse ni eliminarse.
- El inventario y los efectos previos de los jugadores se conservan/restauran de acuerdo con las opciones de la room; ten en cuenta que \`keep-inventory: false\` permite pérdida de objetos al morir.
- Las rooms se guardan en \`rooms.yml\`; las barreras en \`gate-recovery.yml\` para restauración tras un apagado inesperado.
- Mensajes completamente configurables en \`messages.yml\`. Los textos de la versión anterior se migran **solo si no fueron personalizados**.

## Pruebas recomendadas

- Crea una room con dos spawns y un cristal; comprueba selección, guardado y reinicio.
- Entrar con 2 jugadores y probar un tercero por comando, caminando y por teleport; el tercero nunca debe entrar.
- Verifica ganador por muerte, abandono y desconexión; lee el anuncio para todo el servidor.
- Ajusta la reapertura a 0, 5 y 30 segundos, tanto desde el menú como con \`/srooms reopen\`.
- Verifica que durante la reapertura no admite jugadores y que restaura los bloques originales después de la espera.

**Compilación:** \`mvn clean package\` → \`target/SRooms-1.1.0.jar\`. El proyecto tiene GitHub Actions para compilar y generar el archivo ZIP descargable con el JAR.
