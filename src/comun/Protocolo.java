package comun;

import java.util.Random;

// Todo lo que el nodo y las celulas necesitan acordar sobre los mensajes.
//
// Formato de un mensaje de datos (5 o 6 campos separados por |):
//
//   CODIGO | ID_MENSAJE | ORIGEN | ID_SOLICITUD | DATO1 | DATO2
//
//   Solicitud: SUM_REQ|gui1-3fa2-7|gui1|-|5.0|3.0
//   Respuesta: SUM_RES|srv-suma3-91bc-4|srv-suma3|gui1-3fa2-7|8.0
//
// En una solicitud ID_SOLICITUD va como "-" porque no responde a nada.
// En una respuesta ID_SOLICITUD es el ID_MENSAJE de la solicitud original.
public class Protocolo {

    // Posicion de cada campo al hacer split("\\|")
    public static final int CODIGO = 0;
    public static final int ID_MENSAJE = 1;
    public static final int ORIGEN = 2;
    public static final int ID_SOLICITUD = 3;
    public static final int DATO1 = 4;
    public static final int DATO2 = 5;

    // Sesion: 4 caracteres al azar que se eligen al arrancar el proceso.
    // Sirve para que si gui1 se cierra y se vuelve a abrir, sus mensajes
    // no repitan los IDs de la vez anterior (el contador vuelve a empezar en 1).
    private static String sesion = String.format("%04x", new Random().nextInt(65536));
    private static int contador = 0;

    // ID unico sin que nadie lo reparta: quien soy + mi sesion + mi contador.
    // Ejemplo: gui1-3fa2-7
    public static synchronized String nuevoId(String origen) {
        contador++;
        return origen + "-" + sesion + "-" + contador;
    }

    public static boolean esSolicitud(String codigo) {
        return codigo.endsWith("_REQ");
    }

    public static boolean esRespuesta(String codigo) {
        return codigo.endsWith("_RES");
    }

    // SUM -> "+", SUB -> "-", etc. Solo para mostrarlo bonito en pantalla.
    public static String simbolo(String servicio) {
        switch (servicio) {
            case "SUM":
                return "+";
            case "SUB":
                return "-";
            case "MUL":
                return "*";
            case "DIV":
                return "/";
            default:
                return "?";
        }
    }
}
