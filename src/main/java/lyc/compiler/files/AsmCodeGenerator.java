package lyc.compiler.files;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lyc.compiler.symbolTable.SymbolTableData;
import lyc.compiler.symbolTable.SymbolTableGenerator;

public class AsmCodeGenerator implements FileGenerator {

    private int MAX_TEXT_SIZE = 50;

    private ArrayList<Terceto> tercetos;
    private int cont;

    private Map<String,String> comparadores = Map.ofEntries(
            Map.entry("BNE","JNE"),
            Map.entry("BLT","JNAE"),
            Map.entry("BLE","JNA"),
            Map.entry("BGT","JA"),
            Map.entry("BGE","JAE"),
            Map.entry("BEQ","JE"));

    private Map<String,String> comparadoresNegados = Map.ofEntries(
            Map.entry("==","JE"),
            Map.entry(">=","JAE"),
            Map.entry(">","JA"),
            Map.entry("<=","JNA"),
            Map.entry("<","JNAE"),
            Map.entry("!=","JNE"));

    private Map<String,String> operadores = Map.ofEntries(
            Map.entry("+","FADD"),
            Map.entry("-","FSUB"),
            Map.entry("*","FMUL"),
            Map.entry("/","FDIV")
    );

    private Map<Integer, String> etiquetasIf = new HashMap<>();

    public AsmCodeGenerator(ArrayList<Terceto> tercetos) {
        this.tercetos = tercetos;
        this.cont = 0;
    }

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        String asm = "include macros2.asm\ninclude number.asm\n .MODEL  LARGE\n.386\n.STACK 200h\n\n";
        asm += generarDATA();
        String ci = generarAsmTercetos();
        fileWriter.write(asm + generarCabecera() + ci + "\nmov ax, 4C00h\nint 21h\nEND START");
    }

    public String generarAsmTercetos() {
        String asm = "";
        for (int i = 0; i < tercetos.size(); i++) {
            Terceto tc = tercetos.get(i);
            String primer = tc.getOperando();
            String segundo = tc.getOperando1();
            String tercero = tc.getOperando2();

            if(etiquetasIf.get(i) != null){
                asm += etiquetasIf.get(i) + ":" + "\n";
                etiquetasIf.remove(i);
            }

            if(primer.equalsIgnoreCase("cmp")){
                asm += generarCodigoIf();
            } else if(comparadores.get(primer) != null) {
                // Manejar saltos - verificar que el destino existe
                if (segundo.startsWith("[") && segundo.endsWith("]")) {
                    String tercetoRef = segundo.substring(1, segundo.length() - 1);
                    Integer tercetoIndex = Integer.parseInt(tercetoRef);

                    String etiqueta = "";
                    //Genero la etiqueta SI NO EXISTE YA UNA A ESE NRO DE TERCETO
                    if(etiquetasIf.get(tercetoIndex) != null){
                        etiqueta = etiquetasIf.get(tercetoIndex);
                    }else{
                        etiqueta = generarEtiqueta();
                    }

                    etiquetasIf.put(tercetoIndex, etiqueta);

                    //Genero el jump
                    asm += comparadores.get(primer) + " " + etiqueta + "\n";
                }
            } else if(primer.equals("=")) {
                asm += generarAsignacion(tc);
            }else if(operadores.get(primer) != null) {
                asm += operadores.get(primer) + "\n";
            }else if(primer.equals("read")){
                asm += generarScanf(tc);
            }else if(primer.equals("write")){
                asm += generarPrintf(tc);
            }else {
                if (!primer.equals("_")) {
                    asm += resolverExpresion(primer);
                }
                if (!segundo.equals("_")) {
                    asm += resolverExpresion(segundo);
                }
                if (!tercero.equals("_")) {
                    asm += resolverExpresion(tercero);
                }
            }
        }
        System.out.println(etiquetasIf);
        if(!etiquetasIf.isEmpty()){
            for(Map.Entry<Integer, String> entry : etiquetasIf.entrySet()){
                asm += entry.getValue() + ":\n";
            }
        }
        return asm;
    }

    private String generarCodigoIf() {
        String asm_if = "";

        asm_if += "FXCH\n";
        asm_if += "FCOM\n";
        asm_if += "FSTSW AX\n";
        asm_if += "SAHF\n";

        return asm_if;
    }

    private String resolverExpresion(String operando) {
        String asm = "";
        
        // Si es una referencia a un terceto [X]
        if (operando.startsWith("[") && operando.endsWith("]")) {
            String tercetoRef = operando.substring(1, operando.length() - 1);
            int tercetoIndex = Integer.parseInt(tercetoRef);
            Terceto terceto = tercetos.get(tercetoIndex);
            
            // Resolver recursivamente los operandos del terceto
            if(!terceto.getOperando().equals("_")){
                asm += resolverExpresion(terceto.getOperando());
            }
            if(!terceto.getOperando1().equals("_")){
                asm += resolverExpresion(terceto.getOperando1());
            }
            if(!terceto.getOperando2().equals("_")){
                asm += resolverExpresion(terceto.getOperando2());
            }
        } else {
            // Es una constante o variable directa
            if(!operando.equals("_")){
                asm += "FLD " + operando + "\n";
            }
        }
        
        return asm;
    }

    private String generarEtiqueta() {
        return "etiq_" + cont++;
    }

    public String generarCabecera(){
        return ".CODE\nSTART:\nmov AX,@DATA\nmov DS,AX\nmov es,ax\n\n";
    }


    private String generarAsignacion(Terceto tc) {
        String asm = "";
        String segundo = tc.getOperando1();
        asm += "FSTP " + segundo + "\n";
        return asm;
    }

    private String generarPrintf(Terceto tc){
        String asm_scanf = "";

        String segundo = tc.getOperando1();
        Map<String, SymbolTableData> symbols = SymbolTableGenerator.getInstance().getSymbols();

        Boolean finded = false;
        for (Map.Entry<String, SymbolTableData> entry : symbols.entrySet()) {
            if(entry.getKey().equals(segundo)){
                if(entry.getValue().getType().equals("string")){
                    asm_scanf += "DisplayString ";
                }else{
                    asm_scanf += "DisplayFloat ";
                }
            }
        }
        if(!finded){
            asm_scanf += "DisplayString ";
        }

        return asm_scanf + segundo + "\n";
    }

    private String generarScanf(Terceto tc){
        String asm_scanf = "";

        String segundo = tc.getOperando1();
        Map<String, SymbolTableData> symbols = SymbolTableGenerator.getInstance().getSymbols();

        for (Map.Entry<String, SymbolTableData> entry : symbols.entrySet()) {
            if(entry.getKey().equals(segundo)){
                if(entry.getValue().getType().equals("string")){
                    asm_scanf += "GetString ";
                }else{
                    asm_scanf += "GetFloat ";
                }
            }
        }

        return asm_scanf + segundo + "\n";
    }

    private String generarDATA(){
        Map<String, SymbolTableData> symbols = SymbolTableGenerator.getInstance().getSymbols();
        String asm_DATA = ".DATA\n";

        for (Map.Entry<String, SymbolTableData> entry : symbols.entrySet()) {
            SymbolTableData data = entry.getValue();
            if(!data.getType().equals("string")){
                asm_DATA += entry.getKey().replace(".","_") + "\t" + "dd ";
                if(data.getValue() == null)
                    asm_DATA += "? \n";
                else{
                    String value = "";
                    if(data.getType().equals("int"))
                        value = data.getValue() + ".0";
                    else
                        value = data.getValue();
                    asm_DATA += value + "\n";
                }
            }
            else{
                if(entry.getKey().startsWith("_")){
                    int largo = this.MAX_TEXT_SIZE - Integer.valueOf(data.getLength());
                    asm_DATA += entry.getKey().replaceAll("[^a-zA-z0-9]*","") + "\t" + "db " + data.getValue() + ",'$'," + largo + " dup (?)\n";
                }
                else{
                    asm_DATA += entry.getKey().replaceAll("[^a-zA-z0-9]*","") + "\t" + "db " + MAX_TEXT_SIZE + " dup (?),'$'\n";
                }
            }
        }

        return asm_DATA + "\n";
    }

}
