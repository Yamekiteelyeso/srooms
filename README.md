# SRooms 1.3.1

**Paper 1.20.1 · Java 17 · Maven · Sin dependencias obligatorias**

Plugin de salas 1v1 para BoxPvP, Practice y PvP. Máximo **2 participantes por arena**.

## Instalación

Compila con **mvn clean package** o descarga el JAR desde el artifact de GitHub Actions. Colócalo en plugins/ de tu servidor Paper 1.20.1 y reinicia. Configura los archivos en plugins/SRooms/.

## Comandos

| Comando | Función | Permiso |
| --- | --- | --- |
| /srooms gui | Menú público de salas con estados | srooms.use |
| /srooms list | Lista de rooms y ocupación | srooms.use |
| /srooms join <room> | Entrar a combatir | srooms.use |
| /srooms leave | Abandonar combate | srooms.use |
| /srooms info <room> | Datos de la room | srooms.use |
| /srooms stats [jugador] | Victorias, derrotas y rachas | srooms.use |
| /srooms top | Top 10 por victorias | srooms.use |
| /srooms createroom | Selección con hachas | srooms.admin |
| /srooms nameroom <nombre> | Guardar creación | srooms.admin |
| /srooms roomconfig <room> | Menú de ajustes | srooms.admin |
| /srooms deleteroom <room> | Borrar room libre | srooms.admin |
| /srooms reopen <room> <0-300> | Segundos de reapertura | srooms.admin |
| /srooms duration <room> <30-1800> | Duración máxima del combate | srooms.admin |
| /srooms check <room> | Validar la room, el cristal y los spawns | srooms.admin |
| /srooms repair <room> | Restaurar la barrera desde la copia de recuperación (solo sala libre) | srooms.admin |
| /srooms reload | Recargar ajustes y mensajes | srooms.admin |
| /srooms cancel | Cancelar selección | srooms.admin |

## SRooms 1.3 — Sistemas competitivos y administración avanzada

**Seguridad de inventarios:** cada arena puede usar un kit específico (armadura, manos e inventario). Al entrar se almacena una copia de recuperación **antes** de sustituir los objetos; al salir, morir o reconectarse tras un apagado inesperado se intenta restaurar el estado original. El kit se administra con los comandos indicados abajo. Los objetos del kit no se pueden tirar ni guardar en contenedores mientras juegas. Los kits están **desactivados por defecto**.

**Ranking opcional:** activa el botón "Combate Ranked" en `/srooms roomconfig <room>` para que victorias, derrotas y empates modifiquen el ELO. Cada jugador comienza con 1000 puntos en la primera versión donde se usa el sistema. El factor K por defecto es 32 y se modifica en `ranked.k-factor`. Combates casuales siguen contando para estadísticas pero **no** modifican ELO.

**Historial:** registra las últimas partidas completadas en `history.yml`, incluyendo arena, jugadores, ganador o empate y marca de tiempo. Nunca se registran como victorias los duelos cancelados antes del inicio.

**Tiempo de espera:** el primer jugador dispone de 120 segundos predeterminados para encontrar rival; puedes cambiar `duel.waiting-timeout-seconds` o configurarlo como 0 para desactivar el límite.

**Herramientas de administración:** una posición de salida segura configurable con `/srooms setexit`, reinicio instantáneo de una room mediante `/srooms reset` (sin regalar victorias), restauración de proyectiles y protección contra pistones, fuego y líquidos que podrían dañar una arena.

### Comandos nuevos de 1.3

| Comando | Función | Permiso |
| --- | --- | --- |
| `/srooms kit save <room>` | Guarda el inventario y la armadura actuales como kit de la arena | srooms.admin |
| `/srooms kit clear <room>` | Elimina y desactiva el kit de la room | srooms.admin |
| `/srooms kit info <room>` | Estado del kit | srooms.admin |
| `/srooms setexit` | Guarda tu ubicación como salida global segura | srooms.admin |
| `/srooms reset <room>` | Cancela un 1v1 atascado sin premiar a nadie | srooms.admin |
| `/srooms top elo` | Top 10 por rating ELO | srooms.use |
| `/srooms history [room]` | Últimos combates, globales o de una arena | srooms.use |

### Configuración rápida

```yaml
duel:
  waiting-timeout-seconds: 120
ranked:
  k-factor: 32
kits:
  block-containers: true
history:
  max-entries: 200
```

**Activar un kit:** equipa al administrador con los objetos deseados, ejecuta `/srooms kit save Box1` y activa el botón del kit en `/srooms roomconfig Box1`. Nunca es obligatorio usar kits. La partida con kit intenta restaurar inventario, experiencia, salud, modo de juego, vuelo, comida y efectos anteriores al finalizar, incluso cuando `keep-inventory` de la arena está desactivado.

**Datos y migración:** los archivos nuevos son `kits.yml`, `kit-recovery.yml` y `history.yml`. Los `rooms.yml` y `stats.yml` existentes conservan sus valores, se agregan solamente claves nuevas; haz una copia de `plugins/SRooms/` antes de actualizar. Si el servidor se apaga durante una partida con kit, el inventario se recupera al volver a entrar usando el respaldo si todavía existe; por seguridad, cada respaldo solo se consume una vez.

**Seguridad:** no prometemos restauración perfecta ante cualquier fallo físico del disco, mods que reemplacen inventarios ni plugins que modifiquen eventos a posteriori. Prueba los kits y la interacción con otros plugins en un servidor de ensayo antes de activarlos para todos.

## Crear una room

1. Ejecuta **/srooms createroom**. Recibes el hacha de selección de la room.
2. **Clic izquierdo** en el bloque del **punto 1** de la room completa.
3. **Clic derecho** en el bloque del **punto 2** de la misma room. No existe selección de zona A ni B.
4. El plugin retira esa hacha y te entrega la segunda para el cristal.
5. Con la segunda hacha, **clic izquierdo** para el punto 1 del cristal y **clic derecho** para su punto 2. El cristal debe quedar dentro de la room.
6. Escribe **/srooms nameroom Box1** para guardar.
7. El plugin intenta calcular dos spawns automáticos, uno a cada lado del cristal. Si no encuentra dos posiciones seguras, usa **/srooms roomconfig Box1** y establece "Spawn jugador 1" y "Spawn jugador 2" en lugares diferentes, dentro de la room y fuera del cristal.

**Compatibilidad:** las rooms creadas con A/B en versiones antiguas conservan esas coordenadas internamente. Las rooms nuevas utilizan exclusivamente una región `region` y una región `gate` en `rooms.yml`.

## Cambio de selección en 1.3.1

- Dos selecciones, no tres: **room completa** y, después, **cristal**.
- No se piden las zonas A/B; el sistema de 1v1 sigue usando dos spawns separados.
- Las posiciones iniciales se intentan calcular automáticamente según la ubicación del cristal.
- Se rechaza guardar un cristal seleccionado fuera de los límites de la room.
- Los formatos de las rooms antiguas siguen siendo compatibles y no cambian de tamaño al actualizar.

## Correcciones 1.3.1

- Un jugador que abandona en la cuenta regresiva cancela la pelea. Ya no se anuncia un ganador falso, ni se dan recompensas o estadísticas antes de comenzar.
- La entrada a regiones vacías queda protegida: usuarios que no estén inscritos no pueden pasearse por una room (configurable con access.protect-empty-rooms). Los administradores pueden entrar a rooms libres.
- El acceso caminando respeta el permiso srooms.use y la opción walk-in.
- /srooms check <room> permite comprobar regiones y spawns antes de permitir combates; /srooms repair <room> recupera el respaldo del cristal cuando hay uno pendiente.
- Los mensajes de reapertura respetan announcements.broadcast-ready también cuando finaliza el temporizador.
- Pruebas unitarias JUnit 5 de las condiciones de victoria y empate: mvn clean verify ejecuta las pruebas y compila.

## Mejoras 1.2

- **Walk-in opcional:** al entrar andando desde fuera de la región de una room, el plugin une al jugador al primer lado disponible (desactivado por defecto).
- **Menú /srooms gui:** explorador paginado de salas y entrada con un clic.
- **Tiempo de pelea:** de 30 a 1800 segundos con BossBar; empate si el contador se agota.
- **Estadísticas persistentes:** victorias, derrotas, rachas y leaderboard en stats.yml.
- **Configuración por room:** keep inventory y XP, efectos de pociones, hambre, curación, bloqueo de comandos, bloqueo de perlas/chorus, congelación en cuenta atrás, activar/desactivar room, tiempo de pelea y reapertura.
- **Cristal por sala:** siete materiales/colores en la GUI.
- **Sonidos y anuncios** configurables desde config.yml y messages.yml.
- **Recompensas opcionales al ganador:** habilita rewards.enabled y la opción de recompensa en la room. Comandos configurables con %winner%, %loser% y %room%; solo en victorias de combates iniciados, no empates.
- **Protección del mapa:** permite restaurar bloques modificados durante duelos cuando allow-build está activo, limita la cantidad de modificaciones y evita drops duplicados. Los bloques colocados por participantes NO se reembolsan.
- **Estado de los jugadores:** restaura localización, modo de juego, vuelo, salud, hambre y efectos, respetando los ajustes de keep.
- **Seguridad:** tercero rechazado, sin acceso externo durante combate, cierre del cristal en countdown y hasta que se cumpla el tiempo de reapertura.

## Archivos

- config.yml — ajustes globales, defaults para arenas nuevas, sonidos, permisos de comandos, recompensas y protección.
- messages.yml — mensajes editables y títulos.
- rooms.yml — configuración independiente y persistente por arena.
- stats.yml — victorias/derrotas por UUID.
- gate-recovery.yml — respaldo de bloques del cristal ante reinicios inesperados.

Las arenas previas se cargan sin borrarse. Los archivos globales se amplían con valores predeterminados de nuevas versiones.

## Pruebas

Comprueba creación, selección, reapertura, duelos con dos y tres jugadores, entrada automática, empate por timeout, BossBar, stats, protección de construcción, restitución de jugador, premios opt-in y persistencia después del reinicio.

**Build y pruebas:** mvn clean verify => target/SRooms-1.3.1.jar.

**Nota:** la compilación por CI valida que el proyecto genere el JAR; antes de producción comprueba los flujos en un servidor de prueba Paper 1.20.1.


**Al actualizar desde 1.2.0:** conserva una copia de tu carpeta plugins/SRooms/. La configuración y las arenas anteriores permanecen; el nuevo parámetro access.protect-empty-rooms se añade a config.yml de forma predeterminada.
