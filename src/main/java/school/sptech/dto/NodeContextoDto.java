package school.sptech.dto;

/** Hierarquia do node (empresa > HPC > cluster > node) e quem é o dono dele. */
public record NodeContextoDto(String empresa, String hpc, String cluster, String node,
                              String proprietarioNome, String proprietarioEmail) {

    /** Node que não está cadastrado no banco não pode esconder um problema real. */
    public static NodeContextoDto desconhecido(String idNode) {
        return new NodeContextoDto("?", "?", "?", idNode, "Sem proprietário", "-");
    }
}
