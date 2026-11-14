# SRooms 1.3.4

**Paper 1.20.1 · Java 17 · Maven · Sin dependencias obligatorias**

Plugin de salas 1v1 para BoxPvP, Practice y PvP. Máximo **2 participantes por arena**.

## SRooms 1.3.4 — Sin TP obligatorio ni cristal antes de los dos jugadores

Entra **caminando** a la región seleccionada de la room y pelea **en tu posición actual**. Ningún participante es teletransportado automáticamente cuando llega o termina el combate: `Teletransporte al entrar` empieza **DESACTIVADO**. Si deseas el formato tradicional de arenas con puntos de aparición, abre `/srooms roomconfig <room>` y activa **Teletransporte al entrar**; entonces se usan los dos spawns configurados y se devuelve al jugador a la ubicación anterior al finalizar.

**Funcionamiento del cristal:**

1. El jugador 1 entra en la room; se muestra `1/2`, pero **no se coloca ningún cristal**.
2. El jugador 2 entra andando; el contador pasa a `2/2`. El plugin confirma que ambos están físicamente dentro antes de colocar el cristal.
3. **Cuenta regresiva 0 segundos por defecto:** el combate se activa inmediatamente y el cristal **permanece puesto hasta terminar la pelea**, para cerrar la entrada y evitar que entren terceros. Si utilizas el cristal como **separador entre ambos jugadores**, activa **Abrir cristal al pelear** en `/srooms roomconfig <room>`, y se retirará al comenzar el combate (en modo instantáneo, un tick después de aparecer).
4. Al acabar, se restauran los bloques originales del cristal. Durante el tiempo de espera de un solo jugador y la reapertura **no se generan barreras nuevas**.

**Prevención de bucles:** sin TP, los participantes que terminaron un duelo deben salir de la room y volver a entrar para inscribirse otra vez. Al esperar rival puedes salir caminando. Los spawns son **opcionales** cuando el teletransporte está apagado.

**Compatibilidad con salas anteriores:** las rooms guardadas con la cuenta regresiva original de 5 segundos se migran a 0 una sola vez, salvo si ya usan el nuevo marcador de configuración. Los tiempos personalizados diferentes de 5 se conservan. El TP es opt-in incluso para salas antiguas; las rooms anteriores siguen guardadas.

Config global de las rooms nuevas:

```yaml
defaults:
  teleport-on-join: false
  open-crystal-on-fight: false
  countdown-seconds: 0
duel:
  countdown-preparation-ticks: 0
```

## Cambio principal de 1.3.3: entrar CAMINANDO

**Ya no hace falta un comando para entrar en una room.** Cuando un jugador con `srooms.use` atraviesa el borde de una arena disponible, SRooms lo registra automáticamente como participante 1/2 o 2/2, lo teletransporta al spawn que corresponda, muestra el título y prepara el 1v1. La entrada automática queda habilitada para **rooms nuevas y existentes**, incluso si antes tenían `walk-in: false`. El comando `/srooms join` sigue como alternativa, pero nunca es obligatorio.

**Todas las opciones adicionales de las nuevas rooms quedan desactivadas**: conservar inventario/XP, construir, hambre congelada, curar, limpiar efectos, bloquear comandos y perlas, congelar cuenta regresiva, kits, recompensas, Ranked y efectos de poción. El retardo de reapertura empieza en 0 segundos. La **room permanece habilitada** y la entrada caminando siempre está activa, porque son funciones básicas del plugin.

**Rooms dibujadas sobre el suelo:** cuando la selección vertical cubre solo uno o dos bloques, SRooms extiende automáticamente su altura a 12 bloques (configurable). También normaliza las rooms recientes guardadas con altura insuficiente al cargarlas. El perímetro horizontal se conserva.

**Salir sin comando:** si estás esperando rival, puedes salir de la room caminando fuera de sus límites; durante la cuenta regresiva y el combate sigues sujeto al bloqueo de salida. Los administradores pueden entrar **agachados** a una sala vacía para configurarla sin iniciar un combate.

**Configurable:** `access.walk-out-while-waiting`, `selection.minimum-room-height` y la sección `defaults`. Los ajustes opcionales de las rooms anteriores se conservan sin cambios; solo se activa el mecanismo de entrada por proximidad.

## Instalación

Compila con **mvn clean package** o descarga el JAR desde el artifact de GitHub Actions. Colócalo en plugins/ de tu servidor Paper 1.20.1 y reinicia. Configura los archivos en plugins/SRooms/.

## Comandos

| Comando | Función | Permiso |
| --- | --- | --- |
| /srooms gui | Menú público de salas con estados | srooms.use |
| /srooms list | Lista de rooms y ocupación | srooms.use |
| /srooms join <room> | Entrada alternativa, no necesaria: basta caminar dentro | srooms.use |
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

## Correcciones 1.3.2: selección del cristal y mensajes 1v1

- La room sigue usando **una única región**, seleccionada con dos esquinas (hacha de oro) y luego la zona del cristal (hacha de diamante).
- Cada selección requiere **dos clics distintos y en orden**: primero clic izquierdo en el punto 1, después clic derecho en **otro bloque** para el punto 2. Un único clic nunca termina el cristal.
- Se ignoran los eventos de clic duplicados demasiado rápidos y los clics generados justo durante el cambio automático de herramienta. La selección no se guarda hasta ejecutar `/srooms nameroom <nombre>`.
- Al unirse, cada jugador ve en chat y en título `¡ENTRASTE! Room <nombre> • 1/2` o `2/2`. Cuando llega un rival, el primero también ve `¡RIVAL ENCONTRADO!`.
- El contador de inicio deja una breve pausa configurable para leer los mensajes antes del countdown.
- Una arena con los dos spawns inválidos o superpuestos rechaza el intento de unión, en vez de dejar a un jugador esperando en un 1v1 imposible.
- Mensajes, títulos y protección contra clics duplicados configurables en `messages.yml` y `config.yml`.

```yaml
selection:
  min-click-interval-ms: 80
  stage-cooldown-ms: 160
duel:
  countdown-preparation-ticks: 40
ui:
  join-titles:
    enabled: true
```

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

**Build y pruebas:** mvn clean verify => target/SRooms-1.3.4.jar.

**Nota:** la compilación por CI valida que el proyecto genere el JAR; antes de producción comprueba los flujos en un servidor de prueba Paper 1.20.1.


**Al actualizar desde 1.2.0:** conserva una copia de tu carpeta plugins/SRooms/. La configuración y las arenas anteriores permanecen; el nuevo parámetro access.protect-empty-rooms se añade a config.yml de forma predeterminada.
