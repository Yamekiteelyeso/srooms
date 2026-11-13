# SRooms

Plugin de arenas 1v1 para Paper 1.20.1, Java 17.

## Creación
1. /createroom: selecciona zona A con clic izquierdo/derecho del hacha.
2. Selecciona zona B con la misma hacha.
3. Selecciona la barrera con la nueva hacha.
4. /nameroom <nombre>: guarda la room.
5. /roomconfig <nombre>: abre el menú de configuración.

## Comandos
- /createroom, /nameroom <nombre>, /roomconfig <nombre>, /deleteroom <nombre> (srooms.admin)
- /room list, /room join <nombre>, /room leave, /room info <nombre> (srooms.use)

El primer jugador espera en zona A y el segundo aparece en zona B. La barrera se cierra mientras esperan, se abre tras la cuenta regresiva y se restaura al finalizar. La arena usa el inventario del jugador; la opción keepinventory determina si se conserva al morir.

Los datos se guardan en plugins/SRooms/rooms.yml y la recuperación de barreras en gate-recovery.yml.

Compila con: mvn clean package
Resultado: target/SRooms-1.0.0.jar
