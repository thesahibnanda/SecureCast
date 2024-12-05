package org.captaintree.blocktree;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.captaintree.pojo.Block;
import org.captaintree.pojo.User;
import org.captaintree.utils.MailUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
public class BlockTree {
    @Getter
    private final Block genesisBlock;
    private final Map<String, Block> blocksByHash;
    private final Map<String, Set<Block>> adjacencyList;
    private final Map<String, User> usersByEmail;
    private final Map<String, User> usersByAadhar;
    private final Map<String, Integer> partyVotes;
    private final Map<String, String> userVotes;

    private final ExecutorService executorService;

    public BlockTree(User genesisData) {
        log.info("Initializing BlockTree with genesis data");
        Block.setTarget("0");
        this.executorService = Executors.newCachedThreadPool();
        this.blocksByHash = new ConcurrentHashMap<>();
        this.adjacencyList = new ConcurrentHashMap<>();
        this.usersByEmail = new ConcurrentHashMap<>();
        this.usersByAadhar = new ConcurrentHashMap<>();
        this.userVotes = new ConcurrentHashMap<>();
        this.partyVotes = new ConcurrentHashMap<>();
        this.genesisBlock = new Block(0, genesisData, "0");
        this.blocksByHash.put(genesisBlock.getHash(), genesisBlock);
        this.adjacencyList.put(genesisBlock.getHash(), ConcurrentHashMap.newKeySet());
        log.info("Genesis Block created with ID: {}, Hash: {}", genesisBlock.getId(), genesisBlock.getHash());
    }

    public boolean isUserRegisteredByEmail(String email) {
        boolean registered = usersByEmail.containsKey(email);
        log.info("Checking if user is registered by email {}: {}", email, registered);
        return registered;
    }

    public boolean isUserRegisteredByAadhar(String aadhar) {
        boolean registered = usersByAadhar.containsKey(aadhar);
        log.info("Checking if user is registered by Aadhaar {}: {}", aadhar, registered);
        return registered;
    }

    public synchronized Future<Block> addBlockAsync(User data) {
        if (isUserRegisteredByEmail(data.getEmail())) {
            log.error("Duplicate registration attempt for user with email {}", data.getEmail());
            throw new IllegalArgumentException("User with this email is already registered.");
        }
        if (isUserRegisteredByAadhar(data.getAadharCardNumber())) {
            log.error("Duplicate registration attempt for user with Aadhaar {}", data.getAadharCardNumber());
            throw new IllegalArgumentException("User with this Aadhaar number is already registered.");
        }
        usersByEmail.put(data.getEmail(), data);
        usersByAadhar.put(data.getAadharCardNumber(), data);
        log.info("Registering new user: {}", data);

        return executorService.submit(() -> {
            Block parentBlock = getLatestBlock();
            Block newBlock = new Block(blocksByHash.size(), data, parentBlock.getHash());
            blocksByHash.put(newBlock.getHash(), newBlock);
            adjacencyList.get(parentBlock.getHash()).add(newBlock);
            adjacencyList.put(newBlock.getHash(), ConcurrentHashMap.newKeySet());
            log.info("New Block added - ID: {}, Hash: {}, Parent Hash: {}", newBlock.getId(), newBlock.getHash(), parentBlock.getHash());
            return newBlock;
        });
    }

    private Block getLatestBlock() {
        Block latestBlock = blocksByHash.values()
                .stream()
                .max(Comparator.comparingLong(Block::getTimeStamp))
                .orElse(genesisBlock);
        log.info("Latest block selected for new addition - ID: {}, Hash: {}", latestBlock.getId(), latestBlock.getHash());
        return latestBlock;
    }

    public Map<String, Object> getTreeStructure() {
        Map<String, Object> treeStructure = buildTreeMap(genesisBlock);
        log.info("Generated tree structure for visualization");
        return treeStructure;
    }

    private Map<String, Object> buildTreeMap(Block block) {
        Map<String, Object> treeMap = new HashMap<>();
        treeMap.put("blockId", block.getId());
        treeMap.put("hash", block.getHash());
        treeMap.put("data", block.getData());

        List<Map<String, Object>> children = getChildren(block.getHash())
                .stream()
                .map(this::buildTreeMap)
                .toList();

        if (!children.isEmpty()) {
            treeMap.put("children", children);
        }
        return treeMap;
    }

    public Set<Block> getChildren(String hash) {
        Set<Block> children = adjacencyList.getOrDefault(hash, Collections.emptySet());
        log.info("Retrieved children of block with hash {}: {}", hash, children.size());
        return children;
    }

    public Block getBlockByHash(String hash) {
        Block block = blocksByHash.get(hash);
        log.info("Retrieved block by hash {}: {}", hash, block != null ? "found" : "not found");
        return block;
    }

    public void shutdown() {
        executorService.shutdown();
        log.info("Shutting down BlockTree executor service");
    }

    public boolean verifyTreeIntegrity() {
        log.info("Verifying integrity of the block tree");
        for (Block block : blocksByHash.values()) {
            if (!block.equals(genesisBlock)) {
                Block parentBlock = blocksByHash.get(block.getPreviousHash());
                if (parentBlock == null || !block.getPreviousHash().equals(parentBlock.getHash())) {
                    log.error("Tree integrity check failed at block ID: {}, Hash: {}", block.getId(), block.getHash());
                    return false;
                }
            }
        }
        log.info("Tree integrity verified successfully");
        return true;
    }

    public List<Block> getBlocksByUser(User user) {
        List<Block> userBlocks = blocksByHash.values().stream()
                .filter(block -> block.getData().equals(user))
                .toList();
        log.info("Retrieved blocks associated with user {}: {}", user, userBlocks.size());
        return userBlocks;
    }

    public boolean updateUser(User updatedUser) {
        String email = updatedUser.getEmail();
        String aadhar = updatedUser.getAadharCardNumber();
        if (isUserRegisteredByEmail(email) || isUserRegisteredByAadhar(aadhar)) {
            usersByEmail.put(email, updatedUser);
            usersByAadhar.put(aadhar, updatedUser);
            log.info("User updated: {}", updatedUser);
            return true;
        }
        log.warn("User not found for update with email: {}", email);
        return false;
    }

    public int getTotalUsers() {
        return usersByEmail.size();
    }

    public int getTotalBlocks() {
        return blocksByHash.size();
    }

    public synchronized String voteForParty(String email, String partyName) {
        if (!isUserRegisteredByEmail(email)) {
            log.error("User with email {} is not registered", email);
            return "User not registered";
        }
        if (userVotes.containsKey(email)) {
            log.error("User with email {} has already voted", email);
            return "User has already voted";
        }
        userVotes.put(email, partyName);
        partyVotes.merge(partyName, 1, Integer::sum);
        log.info("User {} voted for party {}", email, partyName);
        MailUtils.sendEmail(email, "Your Vote Is Casted Successfully", String.format("Thank You For Voting To %s", partyName));
        return "Vote cast successfully";
    }

    public int getPartyVoteCount(String partyName) {
        return partyVotes.getOrDefault(partyName, 0);
    }

    public Map<String, Object> checkUserVote(String email) {
        if (!isUserRegisteredByEmail(email)) {
            return Map.of("error", true, "message", "User is not registered");
        }
        String votedParty = userVotes.get(email);
        if (votedParty == null) {
            return Map.of("error", true, "message", "User has not voted yet");
        }
        return Map.of("error", false, "party", votedParty);
    }

    public User getUserByEmailOrAadhar(String identifier) {
        User user = usersByEmail.getOrDefault(identifier, usersByAadhar.get(identifier));
        log.info("User retrieved by identifier {}: {}", identifier, user != null ? "found" : "not found");
        return user;
    }
}
