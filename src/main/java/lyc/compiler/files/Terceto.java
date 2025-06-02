package lyc.compiler.files;

public class Terceto {
    private int index;
    private String operador;
    private String operando1;
    private String operando2;

    public Terceto(int index, String operador, String operando1, String operando2) {
        this.index = index;
        this.operador = operador;
        this.operando1 = operando1;
        this.operando2 = operando2;
    }

    public String toString() {
        return "[" + index + "] (" + operador + ", " + operando1 + ", " + operando2 + ")";
    }
}
