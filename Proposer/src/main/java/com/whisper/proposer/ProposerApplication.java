package com.whisper.proposer;

import com.whisper.common.Constants;
import com.whisper.common.PrepareResponse;
import com.whisper.common.Proposal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;



import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@SpringBootApplication
@RestController
@RequestMapping("/proposer")
public class ProposerApplication {
    private final RestTemplate restTemplate = new RestTemplate();
    private final AtomicInteger proposalN = new AtomicInteger(1); // 自增提案编号
    private final int proposerId; // 提案者ID（环境变量注入）

    public ProposerApplication() {
        this.proposerId = Integer.parseInt(System.getenv().getOrDefault("PROPOSER_ID", "1"));
    }

    public static void main(String[] args) {
        SpringApplication.run(ProposerApplication.class, args);
    }

    /**
     * 发起提案接口
     * @param value 提案值
     * @return 最终被接受的提案
     */
    @PostMapping("/propose")
    public Proposal propose(@RequestParam String value) {
        int currentN = proposalN.get();
        // 1. Prepare阶段
        PrepareResult prepareResult = preparePhase(currentN);
        if (!prepareResult.isSuccess()) {
            proposalN.incrementAndGet(); // 编号自增重试
            return null;
        }

        // 2. 确定最终提案
        Proposal chosenProposal = prepareResult.getMaxNProposal() != null
                ? prepareResult.getMaxNProposal()
                : new Proposal(currentN, value);

        // 3. Accept阶段
        boolean acceptSuccess = acceptPhase(chosenProposal);
        return acceptSuccess ? chosenProposal : null;
    }

    // Prepare阶段：调用所有Acceptor的prepare接口
    private PrepareResult preparePhase(int n) {
        int promiseCount = 0;
        Proposal maxNProposal = null;

        log.info("Proposer %d start Prepare request，proposerId: {},num:{}", proposerId, n);
        for (int i = 1; i <= Constants.ACCEPTOR_COUNT; i++) {
            String url = Constants.ACCEPTOR_BASE_URL + i + ":8080/acceptor/prepare?n=" + n;
            try {
                PrepareResponse response = restTemplate.postForObject(url, null, PrepareResponse.class);
                if (response != null && response.isSuccess()) {
                    promiseCount++;
                    Proposal acceptedProposal = response.getAcceptedProposal();
                    log.info("Acceptor {} propose sussess,the propose have been accepted is: {}", i, acceptedProposal);
                    if (acceptedProposal != null && (maxNProposal == null || acceptedProposal.getN() > maxNProposal.getN())) {
                        maxNProposal = acceptedProposal;
                    }
                } else {
                    log.info("Acceptor {} reject", i);
                }
            } catch (Exception e) {
                log.info("Acceptor {} connect failed: {}", i, e.getMessage());
            }
        }

        boolean success = promiseCount >= Constants.MAJORITY_THRESHOLD;
        log.info("Proposer {} Prepare phase {}，the num of compose is : {}/{}",
                proposerId, success ? "success" : "failed", promiseCount, Constants.ACCEPTOR_COUNT);
        return new PrepareResult(success, maxNProposal);
    }

    // Accept阶段：调用所有Acceptor的accept接口
    private boolean acceptPhase(Proposal proposal) {
        int acceptCount = 0;

        log.info("Proposer {} commit Accept request，proposal: {}", proposerId, proposal);
        for (int i = 1; i <= Constants.ACCEPTOR_COUNT; i++) {
            String url = Constants.ACCEPTOR_BASE_URL + i + ":8080/acceptor/accept";
            try {
                Boolean success = restTemplate.postForObject(url, proposal, Boolean.class);
                if (success != null && success) {
                    acceptCount++;
                    log.info("Acceptor {} accept ", i);
                } else {
                    log.info("Acceptor {} reject", i);
                }
            } catch (Exception e) {
                log.info("Acceptor {} connect failed: {}", i, e.getMessage());
            }
        }

        boolean success = acceptCount >= Constants.MAJORITY_THRESHOLD;
        log.info("Proposer {} Accept phase {}，the num of compose is : {}/{}",
                proposerId, success ? "success" : "failed", acceptCount, Constants.ACCEPTOR_COUNT);
        return success;
    }

    // 内部结果类
    private static class PrepareResult {
        private boolean success;
        private Proposal maxNProposal;

        public PrepareResult(boolean success, Proposal maxNProposal) {
            this.success = success;
            this.maxNProposal = maxNProposal;
        }

        public boolean isSuccess() { return success; }
        public Proposal getMaxNProposal() { return maxNProposal; }
    }
}