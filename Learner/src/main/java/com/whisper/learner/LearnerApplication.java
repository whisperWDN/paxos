package com.whisper.learner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.paxos.common.Constants;
import com.paxos.common.Proposal;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RestController
@RequestMapping("/learner")
public class LearnerApplication {
    private final RestTemplate restTemplate = new RestTemplate();
    private Proposal learnedProposal; // 最终学习到的提案

    public static void main(String[] args) {
        SpringApplication.run(LearnerApplication.class, args);
    }

    /**
     * 学习接口：收集所有Acceptor的提案结果
     */
    @GetMapping("/learn")
    public Proposal learn() {
        System.out.println("Learner 开始收集Acceptor结果...");
        Map<Proposal, Integer> proposalCount = new HashMap<>();
        int maxCount = 0;
        Proposal finalProposal = null;

        for (int i = 1; i <= Constants.ACCEPTOR_COUNT; i++) {
            String url = Constants.ACCEPTOR_BASE_URL + i + ":8080/acceptor/accepted";
            try {
                Proposal proposal = restTemplate.getForObject(url, Proposal.class);
                if (proposal != null) {
                    proposalCount.put(proposal, proposalCount.getOrDefault(proposal, 0) + 1);
                    int count = proposalCount.get(proposal);
                    if (count > maxCount) {
                        maxCount = count;
                        finalProposal = proposal;
                    }
                    System.out.printf("Acceptor %d 已接受提案: %s%n", i, proposal);
                } else {
                    System.out.printf("Acceptor %d 未接受任何提案%n", i);
                }
            } catch (Exception e) {
                System.out.printf("Acceptor %d 通信失败: %s%n", i, e.getMessage());
            }
        }

        // 检查多数派
        if (maxCount >= Constants.MAJORITY_THRESHOLD && finalProposal != null) {
            this.learnedProposal = finalProposal;
            System.out.printf("Learner 学习到最终提案: %s%n", learnedProposal);
        } else {
            System.out.println("Learner 未达成一致提案");
            finalProposal = null;
        }
        return finalProposal;
    }

    /**
     * 获取已学习到的提案
     */
    @GetMapping("/getLearned")
    public Proposal getLearnedProposal() {
        return learnedProposal;
    }
}