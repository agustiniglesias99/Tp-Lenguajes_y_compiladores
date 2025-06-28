package lyc.compiler.files;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class IntermediateCodeGenerator implements FileGenerator {

    private static IntermediateCodeGenerator intermediateCodeGenerator;
    private ArrayList<Terceto> tercetos;
    private HashMap<String, Integer> indices = new HashMap<>();

    public static IntermediateCodeGenerator getInstance(){
        if(intermediateCodeGenerator == null){
            intermediateCodeGenerator = new IntermediateCodeGenerator();
        }
        return intermediateCodeGenerator;
    }

    private IntermediateCodeGenerator() {
        this.tercetos = new ArrayList<>();
    }

    public int addTerceto(String op1, String op2, String op3) {
        int indexTerceto = this.tercetos.size();
        this.tercetos.add(new Terceto(op1, op2, op3));
        return indexTerceto;
    }

    public Terceto getTercetoByIdx(int idx){
        return this.tercetos.get(idx);
    }

    public void addIndex(String index, Integer nroTerceto) {
        this.indices.put(index, nroTerceto);
    }
    public String getIndexDraw(String idx){
        Integer nroTerceto = this.indices.get(idx);
        return "[" + nroTerceto + "]";
    }

    public Integer getIndex(String idx){
        return this.indices.get(idx);
    }

    public Integer getNextIndex(){
        return this.tercetos.size();
    }

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        for (int i = 0; i < tercetos.size(); i++) {
            Terceto t = tercetos.get(i);
            fileWriter.write("[" + i + "] " + t.toString() + "\n");
        }
    }

    public ArrayList<Terceto> getIntermediateCode(){
        return this.tercetos;
    }
}
