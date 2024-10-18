package org.captaintree.pojo;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.captaintree.utils.HashUtils;

import java.util.Date;

@Slf4j
@Getter
public class Block {
    private int id;
    private User data;
    private long timeStamp;
    private String previousHash;
    private String hash;
    private int nonce=0;
    private static String target;

    public static void setTarget(String target) {
        Block.target = target;
        log.info("Setting the target {}", target);
    }

    private void calculateHash() {
        nonce = 0;
        String dataToHash;
        do{
            nonce++;
            dataToHash = id+data.toString()+timeStamp+previousHash+nonce;
            hash = HashUtils.applySHA3_512(dataToHash);
        } while(!hash.startsWith(target));
        log.info("Hash For Block Generated For Block {} with nonce {}", id, nonce);
    }

    public Block(int id, User data, String previousHash) {
        this.id = id;
        this.data = data;
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.calculateHash();
    }

    public void setId(int id) {
        this.id = id;
        this.timeStamp = new Date().getTime();
        this.calculateHash();
    }

    public void setData(User data) {
        this.data = data;
        this.timeStamp = new Date().getTime();
        this.calculateHash();
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.calculateHash();
    }
}
