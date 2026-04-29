package com.docollab.shared.packet;

import com.docollab.shared.algorithm.CRDTNode;

public class Delta {

    public enum Operation {
        INSERT,
        DELETE
    }

    private Operation op;

    private CRDTNode node;

    private String userId;

    private long timestamp;

    public Delta() {
    }

    public Delta(Operation op, CRDTNode node, String userId, long timestamp) {
        this.op = op;
        this.node = node;
        this.userId = userId;
        this.timestamp = timestamp;
    }


    public Operation getOp() { return op; }
    public void setOp(Operation op) { this.op = op; }

    public CRDTNode getNode() { return node; }
    public void setNode(CRDTNode node) { this.node = node; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}