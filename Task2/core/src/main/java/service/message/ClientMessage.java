package service.message;
import service.core.ClientInfo;

public class ClientMessage implements java.io.Serializable {
    private long token;
    private ClientInfo info;
    public ClientMessage(long token, ClientInfo info) {
        this.token = token;
        this.info = info;
    }
    public long getToken() {
        return token;
    }
    public ClientInfo getInfo() {
        return info;
    }
}
