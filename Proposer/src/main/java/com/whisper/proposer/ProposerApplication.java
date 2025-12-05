package com.whisper.proposer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.paxos.common.Constants;
import com.paxos.common.Proposal;


import java.util.concurrent.atomic.AtomicInteger;

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

        System.out.printf("Proposer %d 发起Prepare请求，编号: %d%n", proposerId, n);
        for (int i = 1; i <= Constants.ACCEPTOR_COUNT; i++) {
            String url = Constants.ACCEPTOR_BASE_URL + i + ":8080/acceptor/prepare?n=" + n;
            try {
                PrepareResponse response = restTemplate.postForObject(url, null, PrepareResponse.class);
                if (response != null && response.isSuccess()) {
                    promiseCount++;
                    Proposal acceptedProposal = response.getAcceptedProposal();
                    System.out.printf("Acceptor %d 承诺成功，已接受提案: %s%n", i, acceptedProposal);
                    if (acceptedProposal != null && (maxNProposal == null || acceptedProposal.getN() > maxNProposal.getN())) {
                        maxNProposal = acceptedProposal;
                    }
                } else {
                    System.out.printf("Acceptor %d 拒绝承诺%n", i);
                }
            } catch (Exception e) {
                System.out.printf("Acceptor %d 通信失败: %s%n", i, e.getMessage());
            }
        }

        boolean success = promiseCount >= Constants.MAJORITY_THRESHOLD;
        System.out.printf("Proposer %d Prepare阶段%s，承诺数: %d/%d%n",
                proposerId, success ? "成功" : "失败", promiseCount, Constants.ACCEPTOR_COUNT);
        return new PrepareResult(success, maxNProposal);
    }

    // Accept阶段：调用所有Acceptor的accept接口
    private boolean acceptPhase(Proposal proposal) {
        int acceptCount = 0;

        System.out.printf("Proposer %d 发起Accept请求，提案: %s%n", proposerId, proposal);
        for (int i = 1; i <= Constants.ACCEPTOR_COUNT; i++) {
            String url = Constants.ACCEPTOR_BASE_URL + i + ":8080/acceptor/accept";
            try {
                Boolean success = restTemplate.postForObject(url, proposal, Boolean.class);
                if (success != null && success) {
                    acceptCount++;
                    System.out.printf("Acceptor %d 接受提案%n", i);
                } else {
                    System.out.printf("Acceptor %d 拒绝接受%n", i);
                }
            } catch (Exception e) {
                System.out.printf("Acceptor %d 通信失败: %s%n", i, e.getMessage());
            }
        }

        boolean success = acceptCount >= Constants.MAJORITY_THRESHOLD;
        System.out.printf("Proposer %d Accept阶段%s，接受数: %d/%d%n",
                proposerId, success ? "成功" : "失败", acceptCount, Constants.ACCEPTOR_COUNT);
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