package com.whisper.common;

// 内部响应类
public class PrepareResponse {
    private boolean success;
    private Proposal acceptedProposal;

    public PrepareResponse(boolean success, Proposal acceptedProposal) {
        this.success = success;
        this.acceptedProposal = acceptedProposal;
    }

    // Getter & Setter
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public Proposal getAcceptedProposal() { return acceptedProposal; }
    public void setAcceptedProposal(Proposal acceptedProposal) { this.acceptedProposal = acceptedProposal; }
}
