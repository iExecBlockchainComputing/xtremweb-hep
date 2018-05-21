package xtremweb.common;

public class EthereumWallet {
    private String address;
    public EthereumWallet(final String addr) {
        address = addr;
    }
    public String getAddress() {return address; }
}
