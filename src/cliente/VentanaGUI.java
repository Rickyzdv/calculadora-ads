package cliente;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// Ventana de la celula GUI, en JavaFX.
//
// Diferencias con Swing:
//   - Ya no hacemos "new VentanaGUI()" y setVisible(true). JavaFX arranca solo
//     (Application.launch) y nos llama a start(Stage). Ahi armamos la ventana.
//   - JFrame -> Stage + Scene, JLabel -> Label, JTextArea -> TextArea,
//     GridLayout -> GridPane, y los paneles se apilan con VBox.
//   - SwingUtilities.invokeLater -> Platform.runLater (misma idea: solo el hilo
//     de la interfaz puede tocar la ventana).
public class VentanaGUI extends Application {

    private Label pantalla = new Label("0");
    private Label etiquetaEstado = new Label(" ");
    private Label etiquetaServidor = new Label(" ");
    private TextArea area = new TextArea();

    private String primerNumero = "";
    private String segundoNumero = "";
    private String operador = "";

    private static final String NARANJA = "#ff9500";
    private static final String GRIS_NUMERO = "#fafafa";
    private static final String GRIS_ACCION = "#d2d2d2";

    // Simbolos con escapes unicode para evitar problemas de codificacion: x y division
    private static final String POR = "\u00D7";
    private static final String ENTRE = "\u00F7";

    @Override
    public void start(Stage stage) {

        // ---------- PANTALLA ----------
        pantalla.setStyle("-fx-font-size: 34px; -fx-font-weight: bold;");
        etiquetaEstado.setStyle("-fx-font-size: 12px; -fx-text-fill: #828282;");
        etiquetaServidor.setStyle("-fx-font-size: 11px; -fx-text-fill: #2e7d32;");

        VBox panelPantalla = new VBox(2, etiquetaEstado, pantalla, etiquetaServidor);
        panelPantalla.setAlignment(Pos.CENTER_RIGHT);
        panelPantalla.setPadding(new Insets(12, 16, 12, 16));
        panelPantalla.setStyle("-fx-background-color: white; -fx-border-color: #c8c8c8;");

        // ---------- BOTONES ----------
        GridPane panelBotones = new GridPane();
        panelBotones.setHgap(6);
        panelBotones.setVgap(6);
        panelBotones.setPadding(new Insets(8, 10, 8, 10));

        String[] etiquetas = {
                "C", "", "", ENTRE,
                "7", "8", "9", POR,
                "4", "5", "6", "-",
                "1", "2", "3", "+",
                "0", ".", "", "="
        };

        for (int i = 0; i < etiquetas.length; i++) {
            if (etiquetas[i].equals("")) {
                continue; // hueco vacio, igual que antes
            }
            int columna = i % 4;
            int fila = i / 4;
            panelBotones.add(crearBoton(etiquetas[i]), columna, fila);
        }

        // ---------- BITACORA ----------
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(8);
        area.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
        Label tituloBitacora = new Label("Bitacora de mensajes (broadcast del Campo de Datos)");
        VBox panelBitacora = new VBox(4, tituloBitacora, area);
        panelBitacora.setPadding(new Insets(0, 10, 10, 10));
        VBox.setVgrow(area, Priority.ALWAYS);

        // ---------- TODO JUNTO (de arriba hacia abajo) ----------
        panelBotones.setAlignment(Pos.CENTER);
        VBox raiz = new VBox(panelPantalla, panelBotones, panelBitacora);
        VBox.setVgrow(panelBitacora, Priority.ALWAYS); // la bitacora se queda con el espacio que sobre
        raiz.setStyle("-fx-background-color: #f5f5f5;");

        stage.setTitle("Calculadora ADS - " + ClienteMain.miId + " (nodo en puerto " + ClienteMain.puertoNodo + ")");
        stage.setScene(new Scene(raiz, 500, 600));
        stage.setOnCloseRequest(e -> System.exit(0));
        stage.show();

        try {
            ClienteMain.conectar(this);
        } catch (Exception e) {
            mostrarLinea("No se pudo conectar al nodo en el puerto " + ClienteMain.puertoNodo);
        }
    }

    private Button crearBoton(String etiqueta) {
        Button boton = new Button(etiqueta);
        boton.setPrefSize(90, 44);
        String color = GRIS_NUMERO;
        String texto = "black";
        if (esOperador(etiqueta) || etiqueta.equals("=")) {
            color = NARANJA;
            texto = "white";
        } else if (etiqueta.equals("C")) {
            color = GRIS_ACCION;
        }
        boton.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-background-color: " + color
                + "; -fx-text-fill: " + texto + "; -fx-border-color: #d7d7d7;");
        boton.setOnAction(e -> presionar(etiqueta));
        return boton;
    }

    private boolean esOperador(String t) {
        return t.equals("+") || t.equals("-") || t.equals(POR) || t.equals(ENTRE);
    }

    // Boton de la calculadora -> Codigo de Contenido del servicio
    private String servicioDe(String operador) {
        if (operador.equals("+")) {
            return "SUM";
        } else if (operador.equals("-")) {
            return "SUB";
        } else if (operador.equals(POR)) {
            return "MUL";
        } else {
            return "DIV";
        }
    }

    private void presionar(String tecla) {

        if (tecla.equals("C")) {
            primerNumero = "";
            segundoNumero = "";
            operador = "";
            pantalla.setText("0");
            etiquetaEstado.setText(" ");
            etiquetaServidor.setText(" ");
            return;
        }

        if (esOperador(tecla)) {
            if (primerNumero.equals("")) {
                return;
            }
            operador = tecla;
            etiquetaEstado.setText(primerNumero + " " + operador);
            pantalla.setText("0");
            return;
        }

        if (tecla.equals("=")) {
            if (primerNumero.equals("") || operador.equals("") || segundoNumero.equals("")) {
                mostrarLinea("Falta completar la operacion.");
                return;
            }
            try {
                double a = Double.parseDouble(primerNumero);
                double b = Double.parseDouble(segundoNumero);
                etiquetaEstado.setText(primerNumero + " " + operador + " " + segundoNumero + " =");
                pantalla.setText("esperando...");
                etiquetaServidor.setText(" ");
                ClienteMain.enviarOperacion(a, servicioDe(operador), b);
            } catch (NumberFormatException ex) {
                mostrarLinea("Numero invalido.");
            }
            primerNumero = "";
            segundoNumero = "";
            operador = "";
            return;
        }

        if (operador.equals("")) {
            primerNumero = primerNumero + tecla;
            pantalla.setText(primerNumero);
        } else {
            segundoNumero = segundoNumero + tecla;
            pantalla.setText(segundoNumero);
        }
    }

    // Se llama desde ClienteMain (hilo de red) cuando llega un resultado propio
    public void mostrarResultado(String resultado, String servidor) {
        Platform.runLater(() -> {
            if (resultado.startsWith("ERROR")) {
                // El texto completo del error no cabe en letra grande: va abajo, en chiquito
                pantalla.setText("Error");
                etiquetaServidor.setText(resultado + " (" + servidor + ")");
            } else {
                pantalla.setText(resultado);
                etiquetaServidor.setText("Atendido por " + servidor);
            }
        });
    }

    public void mostrarLinea(String texto) {
        Platform.runLater(() -> area.appendText(texto + "\n"));
    }
}
