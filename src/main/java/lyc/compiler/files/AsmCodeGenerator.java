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
        ci = ci.replaceAll("@", "");
        ci = ci.replaceAll("_-", "_n");
        fileWriter.write(asm + generarCabecera() + ci + "\nmov ax, 4C00h\nint 21h\nEND START");
    }

    public String generarAsmTercetos() {
        String asm = "";
        for (int i = 0; i < tercetos.size(); i++) {
            Terceto tc = tercetos.get(i);
            String primer = tc.getOperando();
            String segundo = tc.getOperando1();
            String tercero = tc.getOperando2();

            segundo = convertirConstante(segundo);
            tercero = convertirConstante(tercero);

            if(etiquetasIf.get(i) != null){
                asm += etiquetasIf.get(i) + ":" + "\n";
                etiquetasIf.remove(i);
            }

            if(primer.equalsIgnoreCase("cmp")){
                asm += generarCodigoIf(tc);
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
                if(!(segundo.startsWith("[") && segundo.endsWith("]"))){
                    asm += "FLD " + segundo + "\n";
                }

                if(!(tercero.startsWith("[") && tercero.endsWith("]"))){
                    asm += "FLD " + tercero + "\n";
                }

                asm += operadores.get(primer) + "\n";
            }else if(primer.equals("read")){
                asm += generarScanf(tc);
            }else if(primer.equals("write")){
                asm += generarPrintf(tc);
            }else if(primer.equals("ADDLABEL")){
                String etiqueta = generarEtiqueta();
                etiquetasIf.put(i, etiqueta);
                asm += etiqueta + ":\n";
            }else if(primer.equalsIgnoreCase("bi")){
                String tercetoRef = segundo.substring(1, segundo.length() - 1);
                int tercetoIndex = Integer.parseInt(tercetoRef);

                String etiqueta = "";
                //Si es mayor, no pasa nada creo etiqueta para el futuro
                if(tercetoIndex > i){

                    //Genero la etiqueta SI NO EXISTE YA UNA A ESE NRO DE TERCETO
                    if(etiquetasIf.get(tercetoIndex) != null){
                        etiqueta = etiquetasIf.get(tercetoIndex);
                    }else{
                        etiqueta = generarEtiqueta();
                        etiquetasIf.put(tercetoIndex, etiqueta);
                    }
                }else{
                    //Si es un salto al pasado, CREO QUE ES SOLO EN EL WHILE tengo que ir a la etiqueta que se creo ahi
                    etiqueta = etiquetasIf.get(tercetoIndex);
                    etiquetasIf.remove(tercetoIndex);
                }

                asm += "JMP " + etiqueta + "\n";

            }else if(primer.equalsIgnoreCase("mod")){
                if(! (segundo.startsWith("[") && segundo.endsWith("]")) ){
                    asm += "FLD " + segundo + "\n";
                }
                if(! (tercero.startsWith("[") && tercero.endsWith("]")) ){
                    asm += "FLD " + tercero + "\n";
                }

                asm += "FPREM \n";
                asm += "FSTP ST(1) \n";
            }else if(primer.equalsIgnoreCase("SUBSTRING")){
                asm += generarSubstring(tc);
            }else if(primer.equalsIgnoreCase("CONCAT")){
                asm += generarConcat(tc);
            }else{
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

    private String convertirConstante(String cte){
        boolean flag_number;

        try{
            Float.parseFloat(cte);
            flag_number = true;
        }catch(NumberFormatException excepcion){
            flag_number = false;
        }

        String final_cte = cte;
        if(flag_number){
            final_cte = final_cte.replaceAll("\\.", "_");
            System.out.println(final_cte);
        }

        return flag_number ? "_" + final_cte : cte;
    }

    private String generarCodigoIf(Terceto tc) {
        String asm_if = "";

        String segundo = tc.getOperando1();
        String tercero = tc.getOperando2();

        segundo = convertirConstante(segundo);
        tercero = convertirConstante(tercero);

        if( !(segundo.startsWith("[") && segundo.endsWith("]")) ){
            asm_if += "FLD " + segundo + "\n";
        }

        if( !(tercero.startsWith("[") && tercero.endsWith("]")) ){
            asm_if += "FLD " + tercero + "\n";
        }

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

                operando = convertirConstante(operando);
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
        String tercero = tc.getOperando2();

        segundo = convertirConstante(segundo);
        tercero = convertirConstante(tercero);

        boolean is_string = false;

        if( !(tercero.startsWith("[") && tercero.endsWith("]")) ) {
            Map<String, SymbolTableData> symbols = SymbolTableGenerator.getInstance().getSymbols();
            for (Map.Entry<String, SymbolTableData> entry : symbols.entrySet()) {
                if(entry.getKey().equals(segundo) || entry.getKey().equals("_"+segundo)){
                    if(entry.getValue().getType().equals("string")){
                        is_string = true;
                        break;
                    }
                }
            }
            if(is_string){
                asm += "STRCPY " + segundo + " " + tercero + "\n";
            }else{

                asm += "FLD " + tercero + "\n";
            }
        }

        if(!is_string){
            asm += "FSTP " + segundo + "\n";

            if(segundo.startsWith("@res")){
                asm += "FLD " + segundo + "\n";
            }
        }


        return asm;
    }

    private String generarPrintf(Terceto tc){
        String asm_scanf = "";

        String segundo = tc.getOperando1();
        Map<String, SymbolTableData> symbols = SymbolTableGenerator.getInstance().getSymbols();

        // ¿Es string?
        boolean isString = false;
        SymbolTableData sd = symbols.get(segundo);
        if (sd != null) {
            isString = "string".equals(sd.getType());
        } else if (segundo.length() > 1 && segundo.startsWith("\"") && segundo.endsWith("\"")) {
            isString = true;
        }

        if (isString) {
            asm_scanf += "DisplayString ";

            String label = null;

            // 1) Si segundo es un literal con comillas, busco en la TS por valor exacto
            if (segundo.startsWith("\"") && segundo.endsWith("\"")) {
                for (Map.Entry<String, SymbolTableData> e : symbols.entrySet()) {
                    SymbolTableData d = e.getValue();
                    if ("string".equals(d.getType()) && segundo.equals(d.getValue())) {
                        // IMPORTANT: canonicalize even if the table key has quotes/spaces
                        label = normalizeStringLabelForAsm(e.getKey());
                        break;
                    }
                }
                if (label == null) {
                    // Fallback: construyo etiqueta como en .DATA (prefijo "_" y solo alfanuméricos)
                    label = normalizeStringLabelForAsm(segundo);
                }
            } else {
                // 2) Si ya viene como identificador, lo normalizo igual que en .DATA
                label = normalizeStringLabelForAsm(segundo);
            }

            // Misma sanitización que usás al final del generador
            label = label.replaceAll("@", "").replaceAll("_-", "_n");

            return asm_scanf + label + "\n";
        } else {
            // No string: imprimir número/var float
            asm_scanf += "DisplayFloat ";
            return asm_scanf + segundo + "\n";
        }
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

    private String generarSubstring(Terceto tc) {
        String destino = "@subcadena";
        String origen = tc.getOperando1();

        return "SUBSTRING " + destino + ", " + origen + ", [ST1], [ST0]\n";
    }

    private String generarConcat(Terceto tc) {
        String cadena1 = tc.getOperando1();
        String cadena2 = tc.getOperando2();
        String destino = "@resParcial";

        return "CONCAT " + destino + ", " + cadena1 + ", " + cadena2 + "\n";
    }

    private String generarDATA(){
        Map<String, SymbolTableData> symbols = SymbolTableGenerator.getInstance().getSymbols();
        String asm_DATA = ".DATA\n";

        for (Map.Entry<String, SymbolTableData> entry : symbols.entrySet()) {
            SymbolTableData data = entry.getValue();
            if(!data.getType().equals("string")){
                String posible = entry.getKey().replace(".","_");

                if(posible.contains("@")){
                    posible = posible.replaceAll("_", "");
                }

                asm_DATA += (posible + "\t" + "dd ");
                if(data.getValue() == null)
                    asm_DATA += "? \n";
                else{
                    String value = "";
                    if(data.getType().equals("int")){
                        if(data.getValue().startsWith("@")){
                            if(data.getValue().equals("@mulParcial")){
                                value = "1.0";
                            }else{
                                value = "0.0";
                            }
                        }else{
                            value = data.getValue() + ".0";
                        }
                    }else{
                        value = data.getValue();
                    }


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

        asm_DATA = asm_DATA.replaceAll("@", "");
        asm_DATA = asm_DATA.replaceAll("_-", "_n");

        return asm_DATA + "\n";
    }

    private String normalizeStringLabelForAsm(String op) {
        if (op == null) return "_str";
        String s = op;

        // If it is a quoted literal, drop quotes
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length()-1) == '"') {
            s = s.substring(1, s.length()-1);
        }

        // If it looks like _"text", drop leading underscores and quotes
        s = s.replace("\"", "");
        s = s.replace(".", "_");
        while (s.startsWith("_")) s = s.substring(1);

        // Keep only letters/digits, collapse everything else
        s = s.replaceAll("[^A-Za-z0-9]+", "");
        if (s.isEmpty()) s = "str";
        return "_" + s;
    }

}
