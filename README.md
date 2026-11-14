# SRooms 1.3.5

**Paper 1.20.1 · Java 17 · Salas BoxPvP 1v1 sin dependencias obligatorias**

SRooms funciona directamente al crear una room. Los jugadores **entran caminando** con su propio equipamiento, no necesitan comandos para participar, kits, spawns manuales ni clasificaciones Ranked.

## Crear una room

1. Ejecuta `/srooms createroom` como administrador.
2. Con el **hacha dorada**, clic izquierdo en el punto 1 de la **arena completa** y clic derecho en el punto 2.
3. Con el **hacha de diamante** que recibes después, clic izquierdo en el punto 1 y clic derecho en el punto 2 de la **zona del cristal** (debe quedar dentro de la arena).
4. Ejecuta `/srooms nameroom MiRoom`. **Ya está lista para jugar.** No necesitas configurar nada más.

Las selecciones hechas sobre el suelo se extienden automáticamente hasta la altura mínima de la arena para detectar jugadores. Las rooms previamente creadas siguen siendo compatibles.

## Jugabilidad por defecto

- El jugador 1 **entra caminando** en la room y espera; el cristal **NO** se coloca todavía.
- Cuando el jugador 2 entra, comienza inmediatamente el combate (cuenta regresiva predeterminada **0 segundos**).
- El cristal se coloca **solo cuando hay dos participantes dentro** para cerrar el acceso, siempre que sea seguro colocar bloques en esa zona.
- Cuando termina la pelea, el cristal se retira y los bloques originales se restauran.
- Los jugadores pelean **con su inventario propio** y donde están parados; no hay TP obligatorio.
- No se aceptan nuevos combates hasta que los participantes anteriores hayan salido caminando.
- El jugador que aún espera rival puede salir caminando de la arena.

**La opción de teletransporte sigue disponible**, apagada por defecto, en `/srooms roomconfig MiRoom`. Si la activas, SRooms **calcula automáticamente** dos ubicaciones seguras dentro de la arena; no tienes que guardar Spawn jugador 1 o Spawn jugador 2. Si la distribución del mapa no permite encontrar puntos seguros, desactiva el TP.

El cristal puede funcionar como separador activando `Abrir cristal al pelear`; de lo contrario, cierra el acceso hasta que el combate termina.

## Configuración

Abre `/srooms roomconfig MiRoom` para activar las opciones adicionales que quieras. Por defecto, la arena está **habilitada**, la entrada caminando está activa y **todos los modificadores opcionales están desactivados**, como efectos, construcción, congelar jugadores, bloquear comandos y teletransporte.

Se **eliminaron** los kits por room, los spawns configurables de jugador 1/jugador 2 y la opción de Combate Ranked. El plugin conserva automáticamente la información histórica necesaria para recuperar inventarios de la antigua implementación de kits, pero ya **no aplica ni genera kits nuevos**. Las victorias, derrotas y el historial normal continúan disponibles.

## Comandos

| Comando | Función |
| --- | --- |
| `/srooms createroom` | Iniciar la selección de room + cristal (admin) |
| `/srooms nameroom <nombre>` | Crear la room seleccionada (admin) |
| `/srooms roomconfig <nombre>` | Modificar opciones de la room (admin) |
| `/srooms check <nombre>` | Validar el diseño de una arena (admin) |
| `/srooms deleteroom <nombre>` | Eliminar una arena libre (admin) |
| `/srooms cancel` | Cancelar selección (admin) |
| `/srooms reset <nombre>` | Cancelar una partida bloqueada (admin) |
| `/srooms repair <nombre>` | Restaurar cristal desde respaldo (admin) |
| `/srooms setexit` | Guardar punto de salida fuera de rooms (admin) |
| `/srooms gui` | Ver las arenas |
| `/srooms stats` | Estadísticas de victorias y derrotas |
| `/srooms top` | Clasificación por victorias |
| `/srooms history` | Historial de combates |
| `/srooms leave` | Abandonar voluntariamente la partida |

No hace falta ejecutar ningún comando para **entrar**: basta con cruzar caminando el borde de la room.

## Actualizar desde 1.3.4

1. Guarda una copia de `plugins/SRooms/`.
2. Sustituye el JAR y reinicia Paper.
3. Comprueba con dos jugadores: entra el primero, luego el segundo, verifica la colocación del cristal y pelea.
4. Si una room antigua tenía un kit o Ranked activo, **esos modos dejan de utilizarse**. Los datos antiguos se mantienen solo cuando son necesarios para recuperar inventarios, y el guardado de `rooms.yml` elimina las opciones retiradas.

**Pruebas:** compilación Maven + pruebas unitarias en GitHub Actions. Los eventos y la colocación de cristal en un servidor real deben verificarse también con un ensayo multijugador.
