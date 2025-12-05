// paxos-common/src/main/java/com/paxos/common/Proposal.java
package com.whisper.common;

import java.io.Serializable;

public class Proposal implements Serializable {
    private int n;
    private Object v;

    public Proposal() {}
    public Proposal(int n, Object v) {
        this.n = n;
        this.v = v;
    }

    // Getter & Setter & toString
    public int getN() { return n; }
    public void setN(int n) { this.n = n; }
    public Object getV() { return v; }
    public void setV(Object v) { this.v = v; }

    @Override
    public String toString() {
        return "Proposal{n=" + n + ", v='" + v + "'}";
    }
}
