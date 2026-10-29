package school.sptech.dto;

public enum Componente {
    CPU("CPU_PERCENT"),
    RAM("RAM_PERCENT"),
    SWAP("SWAP_PERCENT"),
    DISCO("DISCO_PERCENT");

    private final String coluna;

    Componente(String coluna) {
        this.coluna = coluna;
    }

    public String getColuna() {
        return coluna;
    }
}
