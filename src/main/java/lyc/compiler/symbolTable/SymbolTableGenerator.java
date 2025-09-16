package lyc.compiler.symbolTable;

import lyc.compiler.files.FileGenerator;
import lyc.compiler.model.CompilerException;
import lyc.compiler.model.DeclarationVariableException;
import lyc.compiler.model.DuplicateVariableException;
import lyc.compiler.model.InvalidTypeException;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CompletionException;

public class SymbolTableGenerator implements FileGenerator {
    private static SymbolTableGenerator symbolTable;

    private Map<String,SymbolTableData> symbols;
    private SymbolTableGenerator() {
        this.symbols = new HashMap<String,SymbolTableData>();
    }
    public static SymbolTableGenerator getInstance() {
        if(symbolTable == null) {
            symbolTable = new SymbolTableGenerator();
        }
        return symbolTable;
    }
    @Override
    /*public void generate(FileWriter fileWriter) throws IOException {
        String file = String.format("%-30s|%-30s|%-30s|%-30s\n","NOMBRE","TIPODATO","VALOR","LONGITUD");
        for (Map.Entry<String, SymbolTableData> entry : this.symbols.entrySet()) {
             file += String.format("%-30s", entry.getKey()) + "|" + entry.getValue().toString() + "\n";
        }
        fileWriter.write(file);
    }*/

    public void generate(FileWriter fw) throws IOException {
        fw.write(String.format("%-30s|%-30s|%-30s|%-30s%n", "NOMBRE","TIPODATO","VALOR","LONGITUD"));

        for (Map.Entry<String, SymbolTableData> entry : this.symbols.entrySet()) {
            String key = entry.getKey();
            SymbolTableData d = entry.getValue();

            String showName = key;
            if ("string".equalsIgnoreCase(d.getType())) {
                // Prefer canonical label derived from the literal value
                String val = d.getValue(); // includes quotes
                showName = toStringLabelForTable((val != null) ? val : key);
            }

            fw.write(String.format("%-30s|%30s|%30s|%30s%n",
                    showName,
                    d.getType(),
                    d.getValue(),   // keep quotes here
                    d.getLength()));
        }
    }

    private static String toStringLabelForTable(String op) {
            if (op != null && op.length() >= 2 && op.charAt(0) == '"' && op.charAt(op.length()-1) == '"') {
                String content = op.substring(1, op.length()-1);
                return "_" + content.replaceAll("[^A-Za-z0-9]+", "");
            }
            return op;
        }

    public void addToken(String token) {
        if(!this.symbols.containsKey(token)) {
            this.symbols.put(token,new SymbolTableData());
        }
    }

    public void addToken(String token,String dataType) {
        if(!this.symbols.containsKey(token)) {
            SymbolTableData data = new SymbolTableData(dataType,token,Integer.toString(token.length()-2));
            this.symbols.put("_" + token,data);
        }
    }

    public void addToken(String token,String dataType,String value) {
        if(!this.symbols.containsKey(token)) {
            SymbolTableData data = new SymbolTableData(dataType,token,value);
            this.symbols.put("_" + token,data);
        }
    }

    public void addDataType(String id, String dataType) throws CompilerException {
        SymbolTableData data = this.symbols.get(id);
        if(data.getType() != null){
            throw new DuplicateVariableException("Variable " + id + " ya definida");
        }
        data.setType(dataType);
    }

    public void verifyType(String id) throws CompilerException {
        SymbolTableData data = this.symbols.get(id);
        if(data.getType() == null){
            throw new DeclarationVariableException("Variable " + id + " sin declarar");
        }
    }

    public void addInternalVariable(String token, String dataType) {
        if(!this.symbols.containsKey(token)) {
            SymbolTableData data = new SymbolTableData(dataType,null,Integer.toString(token.length()));
            this.symbols.put(token,data);
        }
    }

    public void checkTypes(String id, Stack<String> asignados) throws CompilerException {
        if(!this.symbols.containsKey(id)) {
            throw new DeclarationVariableException("Variable " + id + " sin declarar");
        }
        SymbolTableData data_id = this.symbols.get(id);


        while(!asignados.isEmpty()){
            String asignado = asignados.pop();
            int flag_valor_asignado = 0;
            if(!this.symbols.containsKey(asignado)) {
                if(!this.symbols.containsKey("_" + asignado)) {
                    throw new DeclarationVariableException("Variable " + id + " sin declarar");
                }else{
                    flag_valor_asignado = 1;
                }
            }
            SymbolTableData data_asignado = flag_valor_asignado == 0 ? this.symbols.get(asignado) : this.symbols.get("_" + asignado);
            if(!data_id.getType().equals(data_asignado.getType())) {
                throw new InvalidTypeException("La variables '" + id + "' no puede ser asignado a una expresión de diferente tipo");
            }
        }
    }

    public String validarTipoExpresion(Stack<String> variables) throws CompilerException {
        String tipo = "";
        while(!variables.isEmpty()){
            String variable = variables.pop();
            int flag_valor_asignado = 0;
            if(!this.symbols.containsKey(variable)) {
                if(!this.symbols.containsKey("_" + variable)) {
                    throw new DeclarationVariableException("Variable " + variable + " sin declarar");
                }else{
                    flag_valor_asignado = 1;
                }
            }
            SymbolTableData data = flag_valor_asignado == 0 ? this.symbols.get(variable) : this.symbols.get("_" + variable);
            if(!tipo.isEmpty() && !tipo.equals(data.getType())) {
                throw new InvalidTypeException("No se puede operar entre variables de distintos tipos");
            }
            tipo = data.getType();
        }

        return tipo;
    }

    public void compararTipos(String tipo1, String tipo2) throws CompilerException {
        if(!tipo1.equals(tipo2)) {
            throw new InvalidTypeException("No se puede comparar entre dos variables de distintos tipos");
        }
    }

    public Map<String,SymbolTableData> getSymbols(){
        return this.symbols;
    }
}
