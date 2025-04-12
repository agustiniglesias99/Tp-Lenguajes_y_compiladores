package lyc.compiler.symbolTable;

import lyc.compiler.files.FileGenerator;
import lyc.compiler.model.CompilerException;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    public void generate(FileWriter fileWriter) throws IOException {
        String file = String.format("%-30s|%-30s|%-30s|%-30s\n","NOMBRE","TIPODATO","VALOR","LONGITUD");
        for (Map.Entry<String, SymbolTableData> entry : this.symbols.entrySet()) {
             file += String.format("%-30s", entry.getKey()) + "|" + entry.getValue().toString() + "\n";
        }
        fileWriter.write(file);
    }

    public void addToken(String token) {
        if(!this.symbols.containsKey(token)) {
            this.symbols.put(token,new SymbolTableData());
        }
    }

    public void addToken(String token,String dataType) {
        if(!this.symbols.containsKey(token)) {
            SymbolTableData data = new SymbolTableData(dataType,token,Integer.toString(token.length()-1));
            this.symbols.put("_" + token,data);
        }
    }

    public void addInternalVariable(String token, String dataType) {
        if(!this.symbols.containsKey(token)) {
            SymbolTableData data = new SymbolTableData(dataType,null,Integer.toString(token.length()));
            this.symbols.put(token,data);
        }
    }

    public Map<String,SymbolTableData> getSymbols(){
        return this.symbols;
    }
}
