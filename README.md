# SRooms 1.2.0

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
| /srooms reload | Recargar ajustes y mensajes | srooms.admin |
| /srooms cancel | Cancelar selección | srooms.admin |

## Crear una room

1. /srooms createroom — hacha: dos esquinas de zona A, luego de zona B.
2. Segunda hacha: dos puntos de la barrera de cristal.
3. /srooms nameroom Box1
4. /srooms roomconfig Box1 — define spawn A/B desde cada zona si no hay suelo seguro en el centro.

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

**Build:** mvn clean package => target/SRooms-1.2.0.jar.

**Nota:** la compilación por CI valida que el proyecto genere el JAR; antes de producción comprueba los flujos en un servidor de prueba Paper 1.20.1.
