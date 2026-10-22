# Calculadora distribuida ADS

Proyecto del segundo parcial de Cómputo Distribuido (UP, agosto-noviembre 2026). Es la calculadora del primer parcial adaptada a una **arquitectura ADS**. El middleware desaparece. En su lugar hay un **Campo de Datos** formado por nodos en malla completa, y la comunicación entre células es por **broadcast** con **Códigos de Contenido**.

Versión del primer parcial (cliente / middleware / servidor): https://github.com/Rickyzdv/calculadora-distribuida

## Programas

| Programa | Clase principal | Qué hace |
|---|---|---|
| Nodo del Campo de Datos | `nodo.NodoMain` | Forma la malla con otros nodos, acepta células y retransmite todo por broadcast, descartando lo que ya vio |
| Célula servidor | `servidor.ServidorMain` | Atiende **un** servicio (SUM, SUB, MUL o DIV), elegido al arrancar |
| Célula cliente (GUI) | `cliente.ClienteMain` | Calculadora en JavaFX: genera solicitudes y muestra su respuesta |

```
src/
  nodo/      NodoMain, ManejadorConexion
  servidor/  ServidorMain, Calculadora
  cliente/   ClienteMain (red), VentanaGUI (JavaFX)
  comun/     Protocolo (formato de mensajes e IDs), Historial (CSV)
```

## Requisitos

- JDK 25 (Temurin)
- JavaFX SDK 25 o más reciente (probado con 27.0.1) para Windows x64 ([gluonhq.com/products/javafx](https://gluonhq.com/products/javafx/)), descomprimido junto al proyecto (`C:\dev\javafx-sdk-27.0.1`). Los scripts lo buscan solos.

## Cómo correrlo

```powershell
.\compilar.ps1
.\iniciar.ps1          # 4 nodos + 16 servidores + 4 GUIs = 24 procesos
.\detener.ps1          # cierra todo
```

`iniciar.ps1 -Nodos 6` levanta cualquier otra cantidad sin cambiar el código.

También se puede arrancar cada programa a mano:

```powershell
java -cp out nodo.NodoMain nodo1 5001               # primer nodo
java -cp out nodo.NodoMain nodo2 5002 5001          # conoce a nodo1; al resto lo descubre
java -cp out servidor.ServidorMain srv-suma1 SUM 5001
java --module-path C:\dev\javafx-sdk-27.0.1\lib --add-modules javafx.controls -cp out cliente.ClienteMain gui1 5001
```

Un contacto puede ser solo el puerto (`5001`, en la misma máquina) o `host:puerto` (otra máquina).

## Protocolo de conexión

Toda conexión empieza con un saludo que identifica el tipo de entidad:

| Quién se conecta | Manda | El nodo contesta |
|---|---|---|
| Célula GUI | `HELLO\|GUI\|gui1` | nada |
| Célula servidor | `HELLO\|SERVER\|srv-suma1\|SUM` | nada |
| Otro nodo | `HELLO\|NODE\|nodo4\|5004` (su puerto de escucha) | `WELCOME\|nodo3\|nodo1@127.0.0.1:5001,nodo2@127.0.0.1:5002` |

Con el `WELCOME`, el nodo nuevo conoce a todos los nodos que ya están en la malla y se conecta a los que le faltan. Por eso basta con darle **un** contacto, cualquiera. La regla para no duplicar conexiones es que *el que llega se conecta a los que ya están*. Si aun así llegaran dos conexiones con el mismo nodo, se rechaza la segunda.

Cada nodo lleva dos listas de conexiones activas, `nodos` y `celulas`, y las imprime cada vez que alguien entra o sale.

## Formato de los mensajes de datos

```
CODIGO | ID_MENSAJE | ORIGEN | ID_SOLICITUD | DATO1 | DATO2
```

| Campo | Solicitud | Respuesta |
|---|---|---|
| CODIGO | `SUM_REQ`, `SUB_REQ`, `MUL_REQ`, `DIV_REQ` | `SUM_RES`, `SUB_RES`, `MUL_RES`, `DIV_RES` |
| ID_MENSAJE | ID único de la solicitud | ID único **nuevo** de la respuesta |
| ORIGEN | GUI que la generó | servidor que la atendió |
| ID_SOLICITUD | `-` | ID_MENSAJE de la solicitud original |
| DATO1, DATO2 | operandos | resultado (o `ERROR: ...`), sin DATO2 |

Ejemplo:

```
SUM_REQ|gui1-3fa2-7|gui1|-|5.0|3.0
SUM_RES|srv-suma3-91bc-4|srv-suma3|gui1-3fa2-7|8.0
```

**ID único sin coordinador:** `origen-sesion-contador`. El origen es único porque se configura al arrancar. La sesión son 4 caracteres hexadecimales al azar, elegidos al iniciar el proceso, para que una GUI que se reinicia no repita IDs anteriores. El contador avanza en cada mensaje. Nadie reparte los IDs.

## Broadcast y prevención de ciclos

Cuando un nodo recibe un mensaje de datos:

1. Si su `ID_MENSAJE` ya está en `vistos`, lo descarta y no lo retransmite (`DESCARTADO (ya visto)`).
2. Si es nuevo, lo guarda en `vistos` y lo manda a **todos** sus vecinos (nodos y células) **menos al que se lo mandó**.

Los IDs se olvidan después de 60 s (un hilo los limpia cada 10 s), así que el registro es temporal.

En una malla de 4 nodos, cada nodo recibe cada mensaje una vez "de verdad" y dos copias más, que descarta. Esas copias son el costo de la inundación. A cambio, si se cae un enlace o un nodo, el mensaje llega por otro camino.

## Estrategia contra respuestas duplicadas

Hay un servidor de cada tipo en cada nodo, y por el broadcast los cuatro reciben la misma solicitud. La estrategia es **supresión por espera aleatoria**:

1. Al recibir una solicitud de su servicio, cada servidor espera un tiempo al azar entre 0 y 500 ms.
2. Mientras espera, sigue escuchando el broadcast. Si ve pasar una respuesta (`X_RES`) con ese mismo `ID_SOLICITUD`, otro servidor ya respondió y él no responde.
3. Si termina la espera y nadie respondió, responde él.

Nadie coordina y no hay mensajes extra: cada servidor decide con lo que ve pasar por la red. Si un servidor se cae, los demás siguen atendiendo, y la carga se reparte al azar entre todos.

Como **última defensa**, la GUI guarda sus solicitudes pendientes. Muestra solo la primera respuesta a cada una e ignora cualquier repetida, que queda anotada en la bitácora. Si en 5 s nadie responde (por ejemplo, porque no hay servidores de ese tipo), avisa "sin respuesta".

Limitación conocida: si dos servidores sacan tiempos de espera casi iguales (más cerca entre sí que lo que tarda un mensaje en cruzar la malla), ambos responden. En las pruebas, con 4 GUIs pidiendo al mismo tiempo, no hubo duplicados. Solo aparecieron al mandar ráfagas artificiales de cientos de solicitudes en milisegundos, y la GUI las filtró.

**`setTcpNoDelay(true)`:** TCP junta mensajes pequeños antes de enviarlos (algoritmo de Nagle), lo que puede retrasar un mensaje de 40 a 200 ms. Ese retraso es del mismo orden que la espera aleatoria, y en las primeras pruebas hacía que casi todos los servidores respondieran. Por eso todos los sockets lo desactivan.

## Descentralización

- No hay nodo maestro. Todos los nodos corren el mismo programa con las mismas reglas. El contacto inicial solo sirve para entrar, y después cualquier nodo puede caerse.
- Probado: con `nodo1` y `srv-suma2` apagados, las GUIs de los nodos 2, 3 y 4 siguieron resolviendo las cuatro operaciones. Al reiniciar `nodo1` (contactando a `nodo4`) y agregar un `nodo5` al mismo tiempo, se rehízo la malla completa de 5 nodos.
- Si se cae un nodo, sus células se quedan sin acceso, pero el resto del sistema sigue funcionando.

## Persistencia

No la pide este parcial, pero se conserva la del primero:

- `datos/resultados_<servidor>.csv`: lo que atendió cada servidor
- `datos/historial_<gui>.csv`: lo que envió y recibió cada GUI
