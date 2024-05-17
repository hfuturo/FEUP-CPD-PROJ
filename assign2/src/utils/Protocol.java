package utils;

public class Protocol {
    public final static String INFO = "INFO|"; // recebe texto do server
    public final static String REQUEST = "REQUEST|"; // pede para mandar texto para o server
    public final static String TERMINATE = "TERMINATE|"; // termina ligação
    public final static String EMPTY = " "; // quando é necessário fazer uma linha nova no terminal
    public final static String PING = "PING|"; // server pinga clients para verificar se estão conectados
    public final static String ACK = "ACK|"; // client envia para confirmar que está conectado
    private Protocol() {}
}
